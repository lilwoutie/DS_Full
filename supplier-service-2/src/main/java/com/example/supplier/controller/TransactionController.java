package com.example.supplier.controller;

import com.example.supplier.dto.OrderRequest;
import com.example.supplier.dto.TransactionPrepareRequest;
import com.example.supplier.model.Product;
import com.example.supplier.model.StagedProduct;
import com.example.supplier.model.TransactionLogEntry;
import com.example.supplier.service.ProductService;
import com.example.supplier.util.TransactionLogger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    private final ProductService productService;
    private final TransactionLogger logger = new TransactionLogger();

    // transactionId -> Map<productId, stagedProduct>
    private final Map<String, Map<Long, StagedProduct>> stagedItems = new ConcurrentHashMap<>();

    public TransactionController(ProductService productService) {
        this.productService = productService;

        for (TransactionLogEntry entry : logger.readPendingTransactions()) {
            Map<Long, StagedProduct> staged = new HashMap<>();
            for (Long productId : entry.getProductIds()) {
                Optional<Product> product = productService.getProductById(productId);
                product.ifPresent(p -> staged.put(productId, new StagedProduct(productId, p)));
            }
            stagedItems.put(entry.getTransactionId(), staged);
        }
    }

    private int getTotalStagedQuantity(Long productId) {
        return stagedItems.values().stream()
                .flatMap(txnMap -> txnMap.values().stream())
                .filter(staged -> staged.productId().equals(productId))
                .mapToInt(staged -> staged.product().getQuantity())
                .sum();
    }

    @PostMapping("/begin")
    public ResponseEntity<String> begin() {
        return ResponseEntity.ok("txn-id");
    }

    @PostMapping("/prepare/{transactionId}")
    public ResponseEntity<String> prepare(@PathVariable String transactionId,
                                          @RequestBody TransactionPrepareRequest request) {
        if (stagedItems.containsKey(transactionId)) {
            return ResponseEntity.status(409).body("Transaction ID already used");
        }

        List<OrderRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body("No items provided");
        }

        Map<Long, StagedProduct> stagedForTxn = new HashMap<>();

        for (OrderRequest item : items) {
            Long productId = (long) item.getProductId();
            int requestedQty = item.getQuantity();

            Optional<Product> optProduct = productService.getProductById(productId);
            if (optProduct.isEmpty()) {
                return ResponseEntity.status(404).body("Product ID " + productId + " not found");
            }

            Product product = optProduct.get();
            int alreadyStaged = getTotalStagedQuantity(productId);
            int available = product.getQuantity() - alreadyStaged;

            if (available < requestedQty) {
                return ResponseEntity.badRequest().body("Insufficient stock for product ID " + productId);
            }

            Product stagedProduct = new Product(
                    product.getName(),
                    product.getDescription(),
                    product.getPrice(),
                    product.isAvailable(),
                    product.getQuantity() - requestedQty
            );

            stagedForTxn.put(productId, new StagedProduct(productId, stagedProduct));
        }

        // Only put staged map once
        stagedItems.put(transactionId, stagedForTxn);

        // Log the PREPARE

        List<Long> productIds = new ArrayList<>(stagedForTxn.keySet());
        TransactionLogEntry logEntry = new TransactionLogEntry(transactionId, "PREPARE", productIds);
        System.out.println("About to log transaction...");
        logger.logTransaction(logEntry);
        logger.logPending(logEntry);
        System.out.println("Finished logging transaction.");
        return ResponseEntity.ok("Prepared transaction " + transactionId + " with " + items.size() + " item(s)");
    }

    @PostMapping("/commit/{transactionId}")
    public ResponseEntity<String> commit(@PathVariable String transactionId) {
        Map<Long, StagedProduct> stagedMap = stagedItems.remove(transactionId);
        if (stagedMap == null) {
            return ResponseEntity.status(404).body("No prepared transaction with ID " + transactionId);
        }

        for (StagedProduct staged : stagedMap.values()) {
            productService.updateProduct(staged.productId(), staged.product());
        }

        List<Long> productIds = new ArrayList<>(stagedMap.keySet());
        TransactionLogEntry logEntry = new TransactionLogEntry(transactionId, "COMMIT", productIds);
        logger.logTransaction(logEntry);
        logger.removePending(transactionId);

        return ResponseEntity.ok("Committed transaction " + transactionId);
    }

    @PostMapping("/rollback/{transactionId}")
    public ResponseEntity<String> rollback(@PathVariable String transactionId) {
        Map<Long, StagedProduct> rolledBack = stagedItems.remove(transactionId);
        if (rolledBack != null) {
            List<Long> productIds = new ArrayList<>(rolledBack.keySet());
            TransactionLogEntry logEntry = new TransactionLogEntry(transactionId, "ROLLBACK", productIds);
            logger.logTransaction(logEntry);
            logger.removePending(transactionId);

            return ResponseEntity.ok("Rolled back transaction " + transactionId);
        } else {
            return ResponseEntity.status(404).body("No transaction to rollback with ID " + transactionId);
        }
    }

    @GetMapping("/staged")
    public ResponseEntity<Map<String, Map<Long, StagedProduct>>> getStaged() {
        return ResponseEntity.ok(stagedItems);
    }
}
