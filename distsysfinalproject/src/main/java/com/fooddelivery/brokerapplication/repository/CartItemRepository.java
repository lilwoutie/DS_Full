package com.fooddelivery.brokerapplication.repository;

import com.fooddelivery.brokerapplication.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {}
