function toggleNav() {
  var links = document.getElementById("navLinks");
  if (links) {
    links.classList.toggle("open");
  }
}

function showQuantityWarning() {
  var input = document.getElementById("qtyInput");
  var max = input ? parseInt(input.max, 10) : 0;
  var value = input ? parseInt(input.value, 10) : 0;
  if (input && (isNaN(value) || value < 1)) {
    input.value = 1;
  }
  if (input && value > max) {
    input.value = max;
  }
}

document.addEventListener("DOMContentLoaded", function () {
  var qtyInput = document.getElementById("qtyInput");
  if (qtyInput) {
    qtyInput.addEventListener("change", showQuantityWarning);
  }

  var alerts = document.querySelectorAll(".alert");
  alerts.forEach(function (alert) {
    setTimeout(function () {
      alert.style.transition = "opacity 0.4s";
      alert.style.opacity = "0";
      setTimeout(function () {
        alert.remove();
      }, 450);
    }, 4500);
  });
});
