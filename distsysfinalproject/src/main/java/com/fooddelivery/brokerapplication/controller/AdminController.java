package com.fooddelivery.brokerapplication.controller;

import com.fooddelivery.brokerapplication.model.Order;
import com.fooddelivery.brokerapplication.model.TransactionLog;
import com.fooddelivery.brokerapplication.model.ParticipantLog;
import com.fooddelivery.brokerapplication.service.OrderService;
import com.fooddelivery.brokerapplication.repository.TransactionLogRepository;
import com.fooddelivery.brokerapplication.repository.ParticipantLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private OrderService orderService;

    // --- Added: Repositories for transaction logs ---
    @Autowired
    private TransactionLogRepository transactionLogRepository;

    @Autowired
    private ParticipantLogRepository participantLogRepository;

    /**
     * GET /admin/orders
     * Fetches all persisted orders and also the 2PC transaction logs with participant details.
     */
    @GetMapping("/orders")
    public String viewOrders(Model model) {
        // 1) Fetch all orders and add to model
        List<Order> orders = orderService.getAllOrders();
        model.addAttribute("orders", orders);

        // 2) Fetch all transaction logs and add to model
        List<TransactionLog> transactionLogs = transactionLogRepository.findAll();
        model.addAttribute("transactionLogs", transactionLogs);

        // 3) Build a map: transactionId -> List<ParticipantLog>
        Map<String, List<ParticipantLog>> participantLogsMap = new HashMap<>();
        for (TransactionLog tx : transactionLogs) {
            String txId = tx.getId();  // assuming getId() returns the transaction ID string
            List<ParticipantLog> participants = participantLogRepository.findByTransactionId(txId);
            participantLogsMap.put(txId, participants);
        }
        model.addAttribute("participantLogsMap", participantLogsMap);

        // 4) Return the template name. Ensure you have admin-orders.html updated to show logs.
        return "admin-orders";
    }
}
