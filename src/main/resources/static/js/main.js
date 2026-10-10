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

/* ---------- Dark / Light Mode ---------- */
function toggleTheme() {
  var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  if (isDark) {
    document.documentElement.removeAttribute('data-theme');
    localStorage.setItem('ss_theme', 'light');
  } else {
    document.documentElement.setAttribute('data-theme', 'dark');
    localStorage.setItem('ss_theme', 'dark');
  }
  updateThemeIcons();
}

function updateThemeIcons() {
  var isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  document.querySelectorAll('.theme-toggle-btn, .themeToggleBtn, #themeToggleBtn, #sellerThemeToggleBtn').forEach(function (btn) {
    var icon = btn.querySelector('.theme-icon, .theme-toggle-icon');
    if (icon) {
      icon.innerHTML = isDark ? '&#9728;' : '&#9790;';
    }
  });
}

/* ---------- Password Visibility Toggle ---------- */
function togglePasswordVisibility(inputId, btn) {
  var input = document.getElementById(inputId);
  if (!input) return;
  if (input.type === 'password') {
    input.type = 'text';
    btn.innerHTML = '&#128064;';
    btn.setAttribute('aria-label', 'Hide password');
    btn.setAttribute('title', 'Hide password');
  } else {
    input.type = 'password';
    btn.innerHTML = '&#128065;';
    btn.setAttribute('aria-label', 'Show password');
    btn.setAttribute('title', 'Show password');
  }
}

/* ---------- AI Chatbot Assistant with Chat History ---------- */
var AI_CHAT_STORAGE_KEY = 'ss_ai_chat_history_v1';

function getAiChatHistory() {
  try {
    var stored = localStorage.getItem(AI_CHAT_STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch (e) {
    return [];
  }
}

function saveAiChatHistory(history) {
  try {
    if (history.length > 40) {
      history = history.slice(history.length - 40);
    }
    localStorage.setItem(AI_CHAT_STORAGE_KEY, JSON.stringify(history));
  } catch (e) {}
}

function clearAiChatHistory() {
  try {
    localStorage.removeItem(AI_CHAT_STORAGE_KEY);
  } catch (e) {}
  var container = document.getElementById('aiChatMessages');
  if (container) {
    container.innerHTML =
      '<div class="ai-message bot">' +
      '<div class="ai-msg-bubble">' +
      '👋 Hello! I am your SS Supermarket Assistant. Chat history has been cleared! How can I help you today?' +
      '</div></div>';
  }
}

function initAiChat() {
  var history = getAiChatHistory();
  var container = document.getElementById('aiChatMessages');
  if (container && history.length > 0) {
    container.innerHTML = '';
    history.forEach(function (item) {
      appendAiMessageElement(item.role, item.content, item.isHtml);
    });
    container.scrollTop = container.scrollHeight;
  }
  setupContextualAiChips();
}

function setupContextualAiChips() {
  var chipsContainer = document.getElementById('aiQuickPrompts');
  if (!chipsContainer) return;
  var urlMatch = window.location.pathname.match(/\/products\/(\d+)/);
  if (urlMatch) {
    chipsContainer.innerHTML =
      '<button type="button" class="ai-chip" onclick="sendAiPrompt(\'How to use this product?\')">&#128214; How to Use</button>' +
      '<button type="button" class="ai-chip" onclick="sendAiPrompt(\'Explain this product in detail\')">&#128161; Explain Product</button>' +
      '<button type="button" class="ai-chip" onclick="sendAiPrompt(\'What is the rating and stock for this product?\')">&#11088; Rating &amp; Stock</button>' +
      '<button type="button" class="ai-chip" onclick="sendAiPrompt(\'Help me navigate the whole webpage\')">&#129517; Site Navigation</button>';
  }
}

function toggleAiChat() {
  var win = document.getElementById('aiChatWindow');
  if (!win) return;
  var isHidden = win.style.display === 'none' || win.style.display === '';
  if (isHidden) {
    win.style.display = 'flex';
    win.setAttribute('aria-hidden', 'false');
    var input = document.getElementById('aiChatInput');
    if (input) input.focus();
    var container = document.getElementById('aiChatMessages');
    if (container) container.scrollTop = container.scrollHeight;
  } else {
    win.style.display = 'none';
    win.setAttribute('aria-hidden', 'true');
  }
}

function sendAiPrompt(promptText) {
  var input = document.getElementById('aiChatInput');
  if (input) {
    input.value = promptText;
  }
  handleAiSubmit();
}

function handleAiSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();
  var input = document.getElementById('aiChatInput');
  if (!input) return;
  var message = input.value.trim();
  if (!message) return;

  renderAiMessage('user', message, false, true);
  input.value = '';

  var productId = null;
  var urlMatch = window.location.pathname.match(/\/products\/(\d+)/);
  if (urlMatch) {
    productId = parseInt(urlMatch[1], 10);
  }

  // Pass recent history for contextual follow-up
  var fullHistory = getAiChatHistory();
  var payloadHistory = fullHistory.slice(-8).map(function (m) {
    return { role: m.role, content: m.rawContent || m.content };
  });

  var loadingMsg = appendAiMessageElement('bot', 'Thinking...', false);

  fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message: message, productId: productId, history: payloadHistory })
  })
    .then(function (res) {
      if (!res.ok) throw new Error('AI service response error');
      return res.json();
    })
    .then(function (data) {
      if (loadingMsg) loadingMsg.remove();
      var formatted = formatAiMarkdown(data.reply);
      renderAiMessage('bot', formatted, true, true, data.reply);
    })
    .catch(function () {
      if (loadingMsg) loadingMsg.remove();
      renderAiMessage('bot', 'Sorry, I had trouble answering that. Please try asking again!', false, false);
    });
}

function renderAiMessage(sender, text, isHtml, persist, rawContent) {
  appendAiMessageElement(sender, text, isHtml);
  if (persist) {
    var history = getAiChatHistory();
    history.push({
      role: sender,
      content: text,
      isHtml: !!isHtml,
      rawContent: rawContent || text
    });
    saveAiChatHistory(history);
  }
}

function appendAiMessageElement(sender, text, isHtml) {
  var container = document.getElementById('aiChatMessages');
  if (!container) return null;
  var msg = document.createElement('div');
  msg.className = 'ai-message ' + sender;
  var bubble = document.createElement('div');
  bubble.className = 'ai-msg-bubble';
  if (isHtml) {
    bubble.innerHTML = text;
  } else {
    bubble.textContent = text;
  }
  msg.appendChild(bubble);
  container.appendChild(msg);
  container.scrollTop = container.scrollHeight;
  return msg;
}

function formatAiMarkdown(text) {
  if (!text) return '';
  var escaped = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  return escaped
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br>');
}

document.addEventListener("DOMContentLoaded", function () {
  updateThemeIcons();
  initLoginPage();
  initAiChat();
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

