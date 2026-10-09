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

function initSellerProfileMenu() {
  var toggle = document.getElementById("sellerProfileToggle");
  var menu = document.getElementById("sellerProfileMenu");
  if (!toggle || !menu) return;
  toggle.addEventListener("click", function () {
    var isOpen = toggle.getAttribute("aria-expanded") === "true";
    toggle.setAttribute("aria-expanded", String(!isOpen));
    menu.hidden = isOpen;
  });
}

function initLoginPage() {
  var identifier = document.getElementById("otpIdentifier");
  if (!identifier) return;
  var inputError = document.getElementById("otpInputError");
  var sendButton = document.getElementById("sendOtpButton");
  var verifyPanel = document.getElementById("otpVerifyPanel");
  var verifyButton = document.getElementById("verifyOtpButton");
  var resendButton = document.getElementById("resendOtpButton");
  var codeInput = document.getElementById("otpCode");
  var message = document.getElementById("otpMessage");
  var latestIdentifier = "";
  var timer;

  function validValue(value) {
    return /^\d{10}$/.test(value) || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
  }

  function validate() {
    var value = identifier.value.trim();
    if (!value) { inputError.textContent = ""; sendButton.disabled = true; return false; }
    if (!validValue(value)) { inputError.textContent = "Enter a valid 10-digit phone number or email address."; sendButton.disabled = true; return false; }
    inputError.textContent = ""; sendButton.disabled = false; return true;
  }

  function startTimer(seconds) {
    clearInterval(timer);
    var remaining = seconds;
    resendButton.disabled = true;
    function tick() {
      resendButton.textContent = remaining > 0 ? "Resend OTP in " + remaining + "s" : "Resend OTP";
      if (remaining <= 0) { clearInterval(timer); resendButton.disabled = false; return; }
      remaining -= 1;
    }
    tick(); timer = setInterval(tick, 1000);
  }

  identifier.addEventListener("input", validate);
  sendButton.addEventListener("click", function () {
    if (!validate()) return;
    latestIdentifier = identifier.value.trim();
    sendButton.disabled = true;
    fetch("/api/auth/otp/request", { method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: latestIdentifier }) })
      .then(function (response) { return response.json().then(function (body) { if (!response.ok) throw new Error(body.message || "Could not send OTP."); return body; }); })
      .then(function (body) {
        verifyPanel.hidden = false; message.className = "auth-message auth-message-success";
        message.textContent = "OTP sent to " + body.maskedIdentifier + "." + (body.developmentOtp ? " Development OTP: " + body.developmentOtp : "");
        startTimer(body.resendAfterSeconds || 60); codeInput.focus();
      })
      .catch(function (error) { inputError.textContent = error.message; sendButton.disabled = false; });
  });

  verifyButton.addEventListener("click", function () {
    message.className = "auth-message"; message.textContent = "Verifying...";
    fetch("/api/auth/otp/verify", { method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ identifier: latestIdentifier, otp: codeInput.value.trim() }) })
      .then(function (response) { return response.json().then(function (body) { if (!response.ok) throw new Error(body.message || "Invalid OTP."); return body; }); })
      .then(function (body) { window.location.href = body.role === "SELLER" ? "/seller" : body.role === "ADMIN" ? "/admin" : "/products"; })
      .catch(function (error) { message.className = "auth-message auth-message-error"; message.textContent = error.message; });
  });

  resendButton.addEventListener("click", function () { sendButton.click(); });
  document.querySelectorAll("[data-auth-tab]").forEach(function (tab) {
    tab.addEventListener("click", function () {
      document.querySelectorAll("[data-auth-tab]").forEach(function (item) { item.classList.remove("auth-tab-active"); });
      tab.classList.add("auth-tab-active");
      document.getElementById("otpPanel").hidden = tab.dataset.authTab !== "otp";
      document.getElementById("passwordPanel").hidden = tab.dataset.authTab !== "password";
    });
  });
}

document.addEventListener("DOMContentLoaded", function () {
  initLoginPage();
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
