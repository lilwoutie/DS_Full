package com.example.supplier.helper;

import com.example.supplier.model.Product;

public class StagedProduct {
    private final Long originalProductId;
    private final Product stagedProduct;

    public StagedProduct(Long originalProductId, Product stagedProduct) {
        this.originalProductId = originalProductId;
        this.stagedProduct = stagedProduct;
    }

    public Long getOriginalProductId() {
        return originalProductId;
    }

    public Product getStagedProduct() {
        return stagedProduct;
    }
}
