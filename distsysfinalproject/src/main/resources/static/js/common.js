function updateCartCount() {
    fetch("/cart/count")
        .then(res => res.text())
        .then(count => {
            const badge = document.getElementById("cart-count");
            const countInt = parseInt(count, 10);
            if (badge) {
                if (countInt > 0) {
                    badge.textContent = countInt;
                    badge.style.display = "inline-block";
                } else {
                    badge.style.display = "none";
                }
            }
        });
}

document.addEventListener("DOMContentLoaded", () => {
    updateCartCount();

    // Intercept update form submissions
    document.querySelectorAll(".update-form").forEach(form => {
        form.addEventListener("submit", function (e) {
            e.preventDefault(); // prevent page reload

            const formData = new FormData(form);
            const action = form.getAttribute("action");

            fetch(action, {
                method: "POST",
                body: formData
            }).then(res => {
                if (res.ok) {
                    updateCartCount();
                    // Optionally reload the row or page if needed
                    window.location.reload(); // this ensures cart totals update
                }
            });
        });
    });
});

