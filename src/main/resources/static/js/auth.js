// auth.js - Optimized Authentication Module
class AuthManager {
  constructor() {
    this.token = localStorage.getItem("token");
    this.user = this.parseUser();
    this.originalShowPage = null;
    this.initialized = false;
  }

  parseUser() {
    try {
      const userStr = localStorage.getItem("user");
      return userStr ? JSON.parse(userStr) : null;
    } catch (error) {
      console.error("Error parsing user data:", error);
      localStorage.removeItem("user");
      return null;
    }
  }

  init() {
    if (this.initialized) return;

    console.log("Initializing auth manager...");

    // Store the original showPage function
    // this.originalShowPage = window.showPage;
    if (!window._originalShowPage) {
      window._originalShowPage = window.showPage;
    }

    // Override showPage with auth checks
    window.showPage = this.createAuthenticatedShowPage();

    // Set up event listeners
    this.setupEventListeners();

    // Update UI based on auth status
    this.updateUI();

    this.initialized = true;
    console.log("Auth manager initialized");
  }

  createAuthenticatedShowPage() {
    return (pageId) => {
      console.log(`Auth: Showing page ${pageId}`);

      // Check for protected pages
      const protectedPages = ["admin-dashboard", "user-dashboard", "profile"];
      const adminPages = ["admin-dashboard"];
      const userPages = ["user-dashboard"];

      const isProtected = protectedPages.includes(pageId);
      const isAdminPage = adminPages.includes(pageId);
      const isUserPage = userPages.includes(pageId);

      // Check authentication status
      const isAuthenticated = this.checkAuthStatus();

      if (isProtected && !isAuthenticated) {
        console.log("Auth: Redirecting to login - not authenticated");
        // Redirect to appropriate login page
        if (isAdminPage) {
          window._originalShowPage?.("admin-login");
        } else {
          window._originalShowPage?.("user-login");
        }
        return;
      }

      // Check role-based access
      if (isAuthenticated && isProtected) {
        const userRole = this.user?.role || "";

        if (isAdminPage && !this.hasAdminAccess(userRole)) {
          console.error("Access denied: Admin role required");
          this.showNotification(
            "Access denied: Admin privileges required",
            "error"
          );
          this.originalShowPage?.("user-dashboard");
          return;
        }

        if (isUserPage && !this.hasUserAccess(userRole)) {
          console.error("Access denied: User role required");
          this.showNotification(
            "Access denied: User privileges required",
            "error"
          );
          this.originalShowPage?.("admin-dashboard");
          return;
        }
      }

      // Handle dashboard initialization
      if (pageId === "admin-dashboard" && isAuthenticated) {
        // Call the original showPage first
        this.originalShowPage?.(pageId);

        // Initialize admin dashboard
        setTimeout(() => {
          if (typeof dashboard !== "undefined" && dashboard.initialize) {
            dashboard.initialize();
          } else if (typeof initializeAdminDashboard === "function") {
            initializeAdminDashboard();
          } else {
            console.error("Admin dashboard initialization function not found");
          }
        }, 0);
        return;
      }

      if (pageId === "user-dashboard" && isAuthenticated) {
        // Call the original showPage first
        this.originalShowPage?.(pageId);

        // Initialize user dashboard
        setTimeout(() => {
          if (typeof dashboard !== "undefined" && dashboard.initialize) {
            dashboard.initialize();
          } else if (typeof initializeUserDashboard === "function") {
            initializeUserDashboard();
          } else {
            console.error("User dashboard initialization function not found");
          }
        }, 0);
        return;
      }

      // Handle login pages when already authenticated
      if (
        (pageId === "admin-login" || pageId === "user-login") &&
        isAuthenticated
      ) {
        console.log(
          "Already authenticated, redirecting to appropriate dashboard"
        );
        const userRole = this.user?.role || "";

        if (this.hasAdminAccess(userRole)) {
          this.originalShowPage?.("admin-dashboard");
        } else {
          this.originalShowPage?.("user-dashboard");
        }
        return;
      }

      // For all other pages, use the original showPage
      this.originalShowPage?.(pageId);
    };
  }

  checkAuthStatus() {
    this.token = localStorage.getItem("token");
    this.user = this.parseUser();

    if (!this.token || !this.user) {
      return false;
    }

    // TODO: Add token validation/expiration check here
    // For now, just check if token exists
    return true;
  }

  hasAdminAccess(role) {
    if (!role) return false;
    return role.includes("ADMIN") || role.includes("DISPATCHER");
  }

  hasUserAccess(role) {
    if (!role) return false;
    return role.includes("USER") || this.hasAdminAccess(role); // Admins can access user features
  }

  setupEventListeners() {
    // Handle login forms
    document.addEventListener("submit", (e) => {
      if (e.target.id === "admin-login-form") {
        e.preventDefault();
        this.handleAdminLogin(e.target);
      } else if (e.target.id === "user-login-form") {
        e.preventDefault();
        this.handleUserLogin(e.target);
      } else if (e.target.id === "register-form") {
        e.preventDefault();
        this.handleUserRegistration(e.target);
      }
    });

    // Handle logout buttons
    document.addEventListener("click", (e) => {
      if (e.target.matches('.logout-btn, [onclick*="logout"]')) {
        e.preventDefault();
        this.handleLogout();
      }
    });
  }

  async handleAdminLogin(form) {
    const formData = new FormData(form);
    const messageDiv = document.getElementById("admin-login-message");

    const credentials = {
      username: formData.get("username"),
      password: formData.get("password"),
    };

    console.log(`Attempting admin login for: ${credentials.username}`);

    try {
      this.clearMessage(messageDiv);
      this.showLoading("Logging in...");

      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credentials),
      });

      const data = await response.json();

      if (response.ok) {
        console.log("Admin login successful:", data);

        // Store authentication data
        this.setAuthData(data.token, {
          username: data.username,
          role: data.role,
        });

        this.showNotification("Login successful!", "success");

        // Redirect to admin dashboard
        setTimeout(() => {
          if (typeof showPage === "function") {
            showPage("admin-dashboard");
            // Immediately initialize dashboard after showing page
            if (typeof dashboard !== "undefined" && dashboard.initialize) {
              dashboard.initialize();
            }
          }
        }, 1000);
      } else {
        console.error("Admin login failed:", data);
        this.showMessage(
          messageDiv,
          data.message || "Login failed. Please check your credentials.",
          "error"
        );
      }
    } catch (error) {
      console.error("Error during admin login:", error);
      this.showMessage(
        messageDiv,
        "An error occurred during login. Please try again.",
        "error"
      );
    } finally {
      this.hideLoading();
    }
  }

  async handleUserLogin(form) {
    const formData = new FormData(form);
    const messageDiv = document.getElementById("user-login-message");

    const credentials = {
      username: formData.get("username"),
      password: formData.get("password"),
    };

    console.log(`Attempting user login for: ${credentials.username}`);

    try {
      this.clearMessage(messageDiv);
      this.showLoading("Logging in...");

      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(credentials),
      });

      const data = await response.json();

      if (response.ok) {
        console.log("User login successful:", data);

        // Store authentication data
        this.setAuthData(data.token, {
          username: data.username,
          role: data.role,
        });

        this.showMessage(
          messageDiv,
          "Login successful! Redirecting...",
          "success"
        );

        // Redirect based on role
        setTimeout(() => {
          if (this.hasAdminAccess(data.role)) {
            showPage("admin-dashboard");
            if (typeof dashboard !== "undefined" && dashboard.initialize) {
              dashboard.initialize();
            }
          } else {
            showPage("user-dashboard");
            if (typeof dashboard !== "undefined" && dashboard.initialize) {
              dashboard.initialize();
            }
          }
        }, 1000);
      } else {
        console.error("User login failed:", data);
        this.showMessage(
          messageDiv,
          data.message || "Login failed. Please check your credentials.",
          "error"
        );
      }
    } catch (error) {
      console.error("Error during user login:", error);
      this.showMessage(
        messageDiv,
        "An error occurred during login. Please try again.",
        "error"
      );
    } finally {
      this.hideLoading();
    }
  }

  async handleUserRegistration(form) {
    const formData = new FormData(form);
    const messageDiv = document.getElementById("register-message");

    const password = formData.get("password");
    const confirmPassword = formData.get("confirmPassword");

    if (password !== confirmPassword) {
      this.showMessage(messageDiv, "Passwords do not match", "error");
      return;
    }

    if (password.length < 6) {
      this.showMessage(
        messageDiv,
        "Password must be at least 6 characters long",
        "error"
      );
      return;
    }

    const registerData = {
      username: formData.get("username"),
      email: formData.get("email"),
      password: password,
      firstName: formData.get("firstName"),
      lastName: formData.get("lastName"),
      phoneNumber: formData.get("phoneNumber"),
      role: "ROLE_USER",
    };

    try {
      this.clearMessage(messageDiv);
      this.showLoading("Creating account...");

      const response = await fetch("/api/auth/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(registerData),
      });

      if (response.ok) {
        const data = await response.json();
        console.log("Registration successful:", data);

        this.showMessage(
          messageDiv,
          "Registration successful! You can now login.",
          "success"
        );
        form.reset();

        setTimeout(() => {
          if (typeof showPage === "function") {
            showPage("user-login");
          }
        }, 2000);
      } else {
        const errorText = await response.text();
        let error = {};
        try {
          error = JSON.parse(errorText);
        } catch (e) {
          error = { error: errorText };
        }

        console.error("Registration failed:", error);
        this.showMessage(
          messageDiv,
          error.error || error.message || "Registration failed",
          "error"
        );
      }
    } catch (error) {
      console.error("Registration error:", error);
      this.showMessage(
        messageDiv,
        "Registration failed. Please try again.",
        "error"
      );
    } finally {
      this.hideLoading();
    }
  }

  handleLogout() {
    if (confirm("Are you sure you want to logout?")) {
      console.log("Logging out user");

      // Clear authentication data
      this.clearAuthData();

      // Update UI
      this.updateUI();

      // Redirect to landing page
      if (typeof showPage === "function") {
        showPage("landing-page");
      } else {
        window.location.href = "index.html";
      }
    }
  }

  setAuthData(token, user) {
    this.token = token;
    this.user = user;

    localStorage.setItem("token", token);
    localStorage.setItem("user", JSON.stringify(user));

    this.updateUI();
  }

  clearAuthData() {
    this.token = null;
    this.user = null;

    localStorage.removeItem("token");
    localStorage.removeItem("user");
    localStorage.removeItem("username"); // Legacy cleanup
    localStorage.removeItem("role"); // Legacy cleanup
  }

  updateUI() {
    // Update user display elements
    const userElements = document.querySelectorAll(
      ".user-name, [data-user-name]"
    );
    userElements.forEach((el) => {
      el.textContent = this.user?.username || "User";
    });

    // Update role display elements
    const roleElements = document.querySelectorAll(
      ".user-role, [data-user-role]"
    );
    roleElements.forEach((el) => {
      el.textContent = this.formatRole(this.user?.role) || "Guest";
    });

    // Show/hide elements based on auth status
    const authElements = document.querySelectorAll("[data-auth-required]");
    authElements.forEach((el) => {
      el.style.display = this.checkAuthStatus() ? "block" : "none";
    });

    const noAuthElements = document.querySelectorAll("[data-no-auth-required]");
    noAuthElements.forEach((el) => {
      el.style.display = this.checkAuthStatus() ? "none" : "block";
    });

    // Show/hide elements based on role
    const adminElements = document.querySelectorAll("[data-admin-only]");
    adminElements.forEach((el) => {
      el.style.display = this.hasAdminAccess(this.user?.role)
        ? "block"
        : "none";
    });

    const userOnlyElements = document.querySelectorAll("[data-user-only]");
    userOnlyElements.forEach((el) => {
      el.style.display =
        this.checkAuthStatus() && !this.hasAdminAccess(this.user?.role)
          ? "block"
          : "none";
    });
  }

  formatRole(role) {
    if (!role) return "";
    return role
      .replace(/^ROLE_/, "")
      .toLowerCase()
      .split("_")
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
      .join(" ");
  }

  // UI Helper methods
  showMessage(messageDiv, message, type = "info") {
    if (!messageDiv) return;

    messageDiv.textContent = message;
    messageDiv.className = `alert alert-${type}`;
    messageDiv.style.display = "block";
  }

  clearMessage(messageDiv) {
    if (messageDiv) {
      messageDiv.style.display = "none";
    }
  }

  showLoading(message = "Loading...") {
    let loadingDiv = document.getElementById("auth-loading");
    if (!loadingDiv) {
      loadingDiv = document.createElement("div");
      loadingDiv.id = "auth-loading";
      loadingDiv.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        background: white;
        padding: 2rem;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
        z-index: 1000;
        text-align: center;
        min-width: 200px;
      `;
      document.body.appendChild(loadingDiv);
    }

    loadingDiv.innerHTML = `
      <div class="spinner" style="margin-bottom: 1rem;"></div>
      <p>${message}</p>
    `;
    loadingDiv.style.display = "block";
  }

  hideLoading() {
    const loadingDiv = document.getElementById("auth-loading");
    if (loadingDiv) {
      loadingDiv.style.display = "none";
    }
  }

  showNotification(message, type = "info") {
    // Use the global notification system if available
    if (typeof showNotification === "function") {
      showNotification(message, type);
      return;
    }

    // Fallback notification system
    const notification = document.createElement("div");
    notification.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      padding: 1rem 1.5rem;
      border-radius: 6px;
      color: white;
      font-weight: 500;
      z-index: 1001;
      max-width: 400px;
      animation: slideIn 0.3s ease-out;
    `;

    switch (type) {
      case "success":
        notification.style.background = "#16a34a";
        break;
      case "error":
        notification.style.background = "#dc2626";
        break;
      case "warning":
        notification.style.background = "#d97706";
        break;
      default:
        notification.style.background = "#2563eb";
    }

    notification.textContent = message;
    document.body.appendChild(notification);

    setTimeout(() => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    }, 5000);

    notification.addEventListener("click", () => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    });
  }

  // Public API methods
  getToken() {
    return this.token;
  }

  getUser() {
    return this.user;
  }

  isAuthenticated() {
    return this.checkAuthStatus();
  }

  isAdmin() {
    return this.hasAdminAccess(this.user?.role);
  }

  isUser() {
    return this.hasUserAccess(this.user?.role);
  }
}

// Create global auth manager instance
const authManager = new AuthManager();

// Legacy compatibility functions
function checkAuthStatus() {
  return authManager.checkAuthStatus();
}

function handleLogin(event) {
  event.preventDefault();
  const form = event.target;

  if (form.id === "admin-login-form") {
    authManager.handleAdminLogin(form);
  } else {
    authManager.handleUserLogin(form);
  }
}

function handleLogout() {
  authManager.handleLogout();
}

// Auto-initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", function () {
  console.log("DOM loaded, initializing auth...");
  authManager.init();
});

// Export for module use
if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    AuthManager,
    authManager,
    checkAuthStatus,
    handleLogin,
    handleLogout,
  };
}

console.log("Auth module loaded successfully");
