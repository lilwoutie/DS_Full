// src/main/resources/static/js/restaurant.js

// Track whether items have been submitted to cart before:
let cartSubmitted = false;

/**
 * Adjusts quantity and updates per-meal amount, grand total, and button state.
 */
function adjustQty(mealId, delta) {
    const id = parseInt(mealId, 10);
    const qtyInput = document.getElementById(`qty-${id}`);
    let current = parseInt(qtyInput.value, 10);

    // Update quantity without going below 0
    let updated = current + delta;
    if (updated < 0) {
        updated = 0;
    }
    qtyInput.value = updated;

    // Update this meal’s amount = price × updated
    updateMealAmount(id);

    // Update grand total
    calculateTotal();

    // Enable “Add/Update Cart” button if any qty > 0
    toggleCartButton();

    // If user changes quantities after first submission, change button text
    if (cartSubmitted) {
        document.getElementById('add-cart-btn').textContent = 'Update Cart';
    }
}

/**
 * Calculates and updates the “amount” for a single meal row.
 */
function updateMealAmount(mealId) {
    const id = parseInt(mealId, 10);
    const qtyInput = document.getElementById(`qty-${id}`);
    const qty = parseInt(qtyInput.value, 10);

    const priceCell = document.querySelector(`#meal-${id} .col-price`);
    if (!priceCell) return;

    const priceValue = parseFloat(priceCell.textContent.trim());
    const amtSpan = document.getElementById(`amt-${id}`);

    let amount = 0.0;
    if (!isNaN(priceValue)) {
        amount = priceValue * qty;
    }
    amtSpan.textContent = amount.toFixed(2);
}

/**
 * Recalculates grand total across all meal rows.
 */
function calculateTotal() {
    let total = 0.0;
    document.querySelectorAll('.meal-row').forEach((row) => {
        const id = row.id.split('-')[1];
        const amtSpan = document.getElementById(`amt-${id}`);
        const amtValue = parseFloat(amtSpan.textContent.trim());
        if (!isNaN(amtValue)) {
            total += amtValue;
        }
    });
    document.getElementById('restaurant-total').textContent = total.toFixed(2);
}

/**
 * Enables the Add/Update Cart button if any quantity > 0.
 */
function toggleCartButton() {
    const btn = document.getElementById('add-cart-btn');
    const anyQtyPositive = Array.from(document.querySelectorAll('.qty-input'))
        .some(input => parseInt(input.value, 10) > 0);

    btn.disabled = !anyQtyPositive;
}

/**
 * When “Add to Cart” or “Update Cart” button is clicked:
 *   - Collect all mealIds with qty > 0
 *   - Send them to server (placeholder; implement in future)
 *   - Set cartSubmitted = true, change button text to “Update Cart”
 */
function submitCart() {
    const restaurantId = parseInt(document.querySelector('main').getAttribute('data-restaurant-id'), 10);
    const items = [];
    document.querySelectorAll('.meal-row').forEach((row) => {
        const id = row.id.split('-')[1];
        const qty = parseInt(document.getElementById(`qty-${id}`).value, 10);
        if (qty > 0) {
            const price = parseFloat(document.querySelector(`#meal-${id} .col-price`).textContent.trim());
            const name = document.querySelector(`#meal-${id} .meal-name`).textContent.trim();
            items.push({
                mealId: parseInt(id, 10),
                quantity: qty,
                price: price,
                name: name,
                restaurantId: restaurantId
            });
        }
    });

    fetch("/cart/add", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(items)
    }).then(res => {
        if (res.ok) {
            cartSubmitted = true;
            document.getElementById('add-cart-btn').textContent = 'Update Cart';
            updateCartCount();

            // Reset all quantities to 0
            document.querySelectorAll('.qty-input').forEach(input => {
                input.value = 0;
            });

// Reset amounts per meal
            document.querySelectorAll('.meal-row').forEach((row) => {
                const id = row.id.split('-')[1];
                const amtSpan = document.getElementById(`amt-${id}`);
                if (amtSpan) amtSpan.textContent = '0.00';
            });

// Reset total
            calculateTotal();

// Disable button again
            toggleCartButton();

        } else {
            alert("Failed to update cart. Please try again.");
        }
    });
}