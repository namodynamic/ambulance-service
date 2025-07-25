class ApplicationManager {
  constructor() {
    this.currentPage = "landing-page";
    this.authToken = localStorage.getItem("token");
    this.currentUser = this.parseUser();
    this.userRole = this.currentUser?.role || null;
    this.API_BASE = "/api";
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

    console.log("Initializing application...");

    // Set up core functionality
    this.setupGlobalShowPage();
    this.setupEventListeners();
    this.initializeLocalStorageData();
    this.updateCurrentYear();
    this.testBackendConnectivity();

    // Handle initial page routing
    this.handleInitialRouting();

    this.initialized = true;
    console.log("Application initialized successfully");
  }

  setupGlobalShowPage() {
    window.showPage = (pageId) => {
      if (typeof authManager !== "undefined" && authManager.originalShowPage) {
        authManager.createAuthenticatedShowPage().call(authManager, pageId);
        return;
      }
      console.log(`Switching to page: ${pageId}`);

      window.location.hash = pageId;

      document.querySelectorAll(".page").forEach((page) => {
        page.classList.remove("active");
      });

      // Show target page
      const targetPage = document.getElementById(pageId);
      if (targetPage) {
        targetPage.classList.add("active");
        this.currentPage = pageId;

        console.log(`Page switched to: ${pageId}`);
      } else {
        console.error(`Page element not found: ${pageId}`);
      }
    };
  }

  setupEventListeners() {
    // Ambulance request form
    const ambulanceForm = document.getElementById("ambulance-request-form");
    if (ambulanceForm) {
      ambulanceForm.addEventListener("submit", (e) => {
        e.preventDefault();
        this.submitAmbulanceRequest();
      });
    }

    // Set up app container styling
    const appContainer = document.querySelector(".app-container");
    if (appContainer) {
      appContainer.style.display = "flex";
      appContainer.style.flexDirection = "column";
      appContainer.style.minHeight = "100vh";
    }

    // Handle hash changes for routing
    window.addEventListener("hashchange", () => {
      const hash = window.location.hash.substring(1);
      if (hash && hash !== this.currentPage) {
        window.showPage(hash);
      }
    });

    // Handle initial page load
    window.addEventListener("load", () => {
      this.handleInitialRouting();
    });
  }

  updateCurrentYear() {
    const yearElement = document.getElementById("current-year");
    if (yearElement) {
      yearElement.textContent = new Date().getFullYear();
    }
  }

  async testBackendConnectivity() {
    try {
      const response = await fetch("/api/debug/health");
      if (response.ok) {
        console.log("Backend is available");
      }
    } catch (error) {
      console.log("Backend not available, using localStorage fallback");
    }
  }

  handleInitialRouting() {
    // Update auth variables from localStorage
    this.authToken = localStorage.getItem("token");
    this.currentUser = this.parseUser();
    this.userRole = this.currentUser?.role || null;

    // Check for hash on load
    const hash = window.location.hash.substring(1);
    if (hash) {
      window.showPage(hash);
    } else if (this.authToken && this.currentUser) {
      // If logged in but no hash, go to appropriate dashboard
      if (
        this.userRole &&
        (this.userRole.includes("ADMIN") ||
          this.userRole.includes("DISPATCHER"))
      ) {
        window.showPage("admin-dashboard");
      } else {
        window.showPage("user-dashboard");
      }
    } else {
      window.showPage("landing-page");
    }
  }

  async submitAmbulanceRequest() {
    const form = document.getElementById("ambulance-request-form");
    const formData = new FormData(form);
    const phoneInput = document.getElementById("phone");

    // Validate phone number
    const phoneNumber = phoneInput.value.replace(/[^\d+]/g, "");
    const phoneRegex = /^[+]?[0-9]{10,15}$/;

    if (!phoneRegex.test(phoneNumber)) {
      this.showError(
        "Please enter a valid phone number (10-15 digits, optional + at start)"
      );
      phoneInput.focus();
      phoneInput.classList.add("error");
      return;
    }

    // Validate required fields
    const requiredFields = [
      "callerName",
      "phone",
      "address",
      "city",
      "emergencyType",
      "description",
    ];
    const missingFields = requiredFields.filter(
      (field) => !formData.get(field)?.trim()
    );

    if (missingFields.length > 0) {
      this.showError(
        `Please fill in all required fields: ${missingFields.join(", ")}`
      );
      return;
    }

    // Prepare request data
    const requestData = {
      userName: formData.get("callerName"),
      userContact: phoneNumber,
      location: `${formData.get("address")}, ${formData.get(
        "city"
      )}, ${formData.get("zipcode")}`,
      emergencyDescription: `${formData.get("emergencyType")}: ${formData.get(
        "description"
      )}`,
    };

    console.log("Submitting ambulance request:", requestData);

    try {
      this.showLoading("Submitting emergency request...");

      const response = await this.apiCall("/requests", {
        method: "POST",
        body: JSON.stringify(requestData),
      });

      console.log("Request submitted successfully:", response);

      // Reset any error states
      phoneInput.classList.remove("error");

      // Update confirmation page with real data
      const confirmationId = document.getElementById("confirmation-request-id");
      if (confirmationId) {
        confirmationId.textContent = `#${response.id}`;
      }

      // Show confirmation page
      window.showPage("confirmation-page");

      // Start status tracking
      if (response.id) {
        this.trackRequestStatus(response.id);
      }

      // Reset form
      form.reset();
    } catch (error) {
      console.error("Error submitting request:", error);

      // Check if this is a "no ambulance available" error
      const isNoAmbulanceError =
        error.status === 503 ||
        error.status === 500 ||
        (error.message &&
          (error.message.includes("No ambulance available") ||
            error.message.includes("NoAvailableAmbulanceException") ||
            (error.response &&
              error.response.message &&
              error.response.message.includes(
                "No ambulance available at the moment"
              ))));

      if (isNoAmbulanceError) {
        // Show a friendly message that the request is queued
        const loadingMessage =
          "All our ambulances are currently busy. " +
          "Your request has been queued and an ambulance will be assigned as soon as one becomes available. " +
          "You can close this message - we'll process your request shortly.";

        this.showLoading(loadingMessage);

        // Still show the confirmation page since the request was queued
        const confirmationId = document.getElementById(
          "confirmation-request-id"
        );
        if (confirmationId) {
          confirmationId.textContent = "#QUEUED";
        }

        window.showPage("confirmation-page");
      } else {
        // For all other errors, show the error message
        const errorMessage =
          error.response?.message ||
          error.message ||
          "An unknown error occurred";
        this.showError(`Failed to submit request: ${errorMessage}`);
        this.hideLoading();
      }
    }
  }

  async handleUserRegistration() {
    const form = document.getElementById("register-form");
    const formData = new FormData(form);
    const messageDiv = document.getElementById("register-message");

    const password = formData.get("password");
    const confirmPassword = formData.get("confirmPassword");

    if (password !== confirmPassword) {
      showRegisterError("Passwords do not match");
      return;
    }

    const registerData = {
      username: formData.get("username"),
      email: formData.get("email"),
      password: password,
      role: "ROLE_USER",
    };

    try {
      if (messageDiv) {
        messageDiv.style.display = "none";
      }

      const response = await fetch(`${API_BASE}/auth/register`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(registerData),
      });

      if (response.ok) {
        const data = await response.json();
        showRegisterSuccess("Registration successful! You can now login.");
        form.reset();
        setTimeout(() => {
          showPage("user-login");
        }, 2000);
      } else {
        const errorText = await response.text();
        let error = {};
        try {
          error = JSON.parse(errorText);
        } catch (e) {
          error = { error: errorText };
        }
        showRegisterError(error.error || "Registration failed");
      }
    } catch (error) {
      console.error("Registration error:", error);
      showRegisterError("Registration failed. Please try again.");
    }

    function showRegisterError(message) {
      if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = "alert alert-error";
        messageDiv.style.display = "block";
      }
    }

    function showRegisterSuccess(message) {
      if (messageDiv) {
        messageDiv.textContent = message;
        messageDiv.className = "alert alert-success";
        messageDiv.style.display = "block";
      }
    }
  }

  async trackRequestStatus(requestId) {
    const statusText = document.getElementById("current-status");
    const progressBar = document.getElementById("progress-bar");
    const etaDisplay = document.getElementById("eta-display");
    const etaTime = document.getElementById("eta-time");
    const statusIcons = document.querySelectorAll(".status-icon");

    if (!statusText || !progressBar) {
      console.error("Status elements not found");
      return;
    }

    // Reset all icons
    statusIcons.forEach((icon) => icon.classList.remove("active"));

    try {
      // Poll for request status updates
      const checkStatus = async () => {
        try {
          const request = await this.apiCall(`/requests/${requestId}`);
          console.log("Request status update:", request);

          // Update status based on current state
          const statusConfig = {
            PENDING: {
              text: "Searching for available ambulance...",
              progress: 20,
              icon: "PENDING",
            },
            ASSIGNED: {
              text: "Ambulance assigned and preparing for dispatch",
              progress: 40,
              icon: "ASSIGNED",
            },
            DISPATCHED: {
              text: `Ambulance #${request.ambulance?.id || ""} is on the way${
                request.eta ? ` (ETA: ${request.eta} min)` : ""
              }`,
              progress: 60,
              icon: "DISPATCHED",
              showEta: true,
            },
            ARRIVED: {
              text: "Ambulance has arrived at the location",
              progress: 80,
              icon: "ARRIVED",
              etaText: "Arrived",
            },
            IN_PROGRESS: {
              text: "Patient is being attended to",
              progress: 90,
              icon: "IN_PROGRESS",
            },
            COMPLETED: {
              text: "Service completed successfully",
              progress: 100,
              icon: "COMPLETED",
              color: "#10b981",
              final: true,
            },
            CANCELLED: {
              text: "Request has been cancelled",
              progress: 100,
              icon: "CANCELLED",
              color: "#ef4444",
              final: true,
            },
          };

          const config =
            statusConfig[request.status] || statusConfig["PENDING"];

          statusText.textContent = config.text;
          progressBar.style.width = `${config.progress}%`;

          if (config.color) {
            progressBar.style.backgroundColor = config.color;
          }

          if (config.showEta && etaDisplay) {
            etaDisplay.style.display = "block";
            if (etaTime)
              etaTime.textContent = `${request.eta || "5-10"} minutes`;
          }

          if (config.etaText && etaTime) {
            etaTime.textContent = config.etaText;
          }

          if (config.final && etaDisplay) {
            etaDisplay.style.display = "none";
          }

          // Activate status icon
          const statusIcon = document.querySelector(
            `.status-icon[data-status='${config.icon}']`
          );
          if (statusIcon) {
            statusIcon.classList.add("active");
          }

          // Return true if in final state
          return config.final || false;
        } catch (error) {
          console.error("Error checking request status:", error);

          // If it's a 404, the request might not be processed yet, keep trying
          if (error.status === 404) {
            setTimeout(checkStatus, 5000);
          } else {
            statusText.textContent =
              "Error checking status. Please refresh the page.";
          }
          return false;
        }
      };

      // Start polling
      const isComplete = await checkStatus();

      // Continue polling if not completed
      if (!isComplete) {
        const pollInterval = setInterval(async () => {
          const isComplete = await checkStatus();
          if (isComplete) {
            clearInterval(pollInterval);
            this.showSuccess("Request processing complete!");
          }
        }, 10000); // Check every 10 seconds

        // Stop polling after 30 minutes to prevent infinite loops
        setTimeout(() => {
          clearInterval(pollInterval);
        }, 30 * 60 * 1000);
      } else {
        this.showSuccess("Request processing complete!");
      }
    } catch (error) {
      console.error("Error tracking request status:", error);
      statusText.textContent =
        "Error tracking request status. Please check back later.";
    }
  }

  // Location and utility functions
  getCurrentLocation() {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          // In a real implementation, you would use reverse geocoding
          const addressField = document.getElementById("address");
          const cityField = document.getElementById("city");
          const zipcodeField = document.getElementById("zipcode");

          if (addressField) addressField.value = "Current Location (GPS)";
          if (cityField) cityField.value = "Auto-detected";
          if (zipcodeField) zipcodeField.value = "00000";

          this.showSuccess("Location detected and filled automatically");
        },
        (error) => {
          console.error("Geolocation error:", error);
          this.showError("Unable to get your location. Please enter manually.");
        },
        {
          enableHighAccuracy: true,
          timeout: 10000,
          maximumAge: 60000,
        }
      );
    } else {
      this.showError("Geolocation is not supported by this browser.");
    }
  }

  contactDispatch() {
    const phone = "08002255372";
    const message = `Emergency dispatch contact:\n\nPhone: ${phone}\n\nThis would normally:\n- Open your phone dialer\n- Connect to dispatch center\n- Provide direct communication with emergency services`;

    alert(message);

    // Attempt to open phone dialer on mobile devices
    if (
      /Android|iPhone|iPad|iPod|BlackBerry|IEMobile/i.test(navigator.userAgent)
    ) {
      window.location.href = `tel:${phone}`;
    }
  }

  showLoginPrompt() {
    const message = `Account Benefits:\n\n• Track your emergency requests in real-time\n• View complete request history\n• Save personal and medical information\n• Faster service with pre-filled forms\n• SMS/Email notifications\n\nWould you like to create an account or login?`;

    if (confirm(message)) {
      window.showPage("user-login");
    }
  }

  logout() {
    if (typeof authManager !== "undefined" && authManager.handleLogout) {
      authManager.handleLogout();
    } else {
      // Fallback logout
      localStorage.removeItem("token");
      localStorage.removeItem("user");
      this.authToken = null;
      this.currentUser = null;
      this.userRole = null;
      window.showPage("landing-page");
    }
  }

  // API and data management
  async apiCall(endpoint, options = {}) {
    const url = `${this.API_BASE}${endpoint}`;
    const token = localStorage.getItem("token");

    const config = {
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      ...options,
    };

    console.log(`Making API call to: ${url}`);

    try {
      const response = await fetch(url, config);
      console.log(`API Response status: ${response.status}`);

      if (response.status === 401 || response.status === 403) {
        console.log("Authentication failed, clearing tokens");
        localStorage.removeItem("token");
        localStorage.removeItem("user");
        this.authToken = null;
        this.currentUser = null;
        this.userRole = null;

        if (
          this.currentPage === "admin-dashboard" ||
          this.currentPage === "user-dashboard"
        ) {
          window.showPage("landing-page");
        }
        throw new Error("Authentication required");
      }

      // Handle error responses with JSON body
      if (!response.ok) {
        let errorData = { message: `HTTP ${response.status}` };
        try {
          const errorText = await response.text();
          console.error(`API Error: ${response.status} - ${errorText}`);
          try {
            errorData = JSON.parse(errorText);
          } catch (e) {
            errorData.message = errorText || errorData.message;
          }
        } catch (e) {
          console.error("Error parsing error response:", e);
        }

        const error = new Error(errorData.message || `HTTP ${response.status}`);
        error.status = response.status;
        error.response = errorData;
        throw error;
      }

      // Handle empty responses
      if (response.status === 204) {
        return null;
      }

      const data = await response.json();
      console.log(`API Response data:`, data);
      return data;
    } catch (error) {
      console.error(`API call failed: ${endpoint}`, error);
      throw error;
    }
  }

  initializeLocalStorageData() {
    // Initialize ambulances if none exist
    const existingAmbulances = localStorage.getItem("ambulance_ambulances");
    if (!existingAmbulances) {
      const sampleAmbulances = [
        {
          id: 1,
          currentLocation: "Central Station",
          availability: "AVAILABLE",
        },
        { id: 2, currentLocation: "North Station", availability: "AVAILABLE" },
        {
          id: 3,
          currentLocation: "South Station",
          availability: "MAINTENANCE",
        },
        { id: 4, currentLocation: "East Station", availability: "AVAILABLE" },
        { id: 5, currentLocation: "West Station", availability: "DISPATCHED" },
      ];
      localStorage.setItem(
        "ambulance_ambulances",
        JSON.stringify(sampleAmbulances)
      );
      console.log("Initialized sample ambulance data");
    }

    // Initialize requests if none exist
    const existingRequests = localStorage.getItem("ambulance_requests");
    if (!existingRequests) {
      const sampleRequests = [
        {
          id: 1001,
          userName: "John Doe",
          userContact: "+1234567890",
          location: "123 Main Street, Emergency City",
          emergencyDescription:
            "Cardiac Emergency: Patient experiencing chest pain",
          requestTime: new Date(Date.now() - 3600000).toISOString(),
          status: "COMPLETED",
          ambulanceId: 1,
          dispatchTime: new Date(Date.now() - 3300000).toISOString(),
        },
        {
          id: 1002,
          userName: "Jane Smith",
          userContact: "+1987654321",
          location: "456 Oak Avenue, Emergency City",
          emergencyDescription: "Traffic Accident: Multiple injuries reported",
          requestTime: new Date(Date.now() - 1800000).toISOString(),
          status: "DISPATCHED",
          ambulanceId: 5,
          dispatchTime: new Date(Date.now() - 1500000).toISOString(),
        },
        {
          id: 1003,
          userName: "Bob Johnson",
          userContact: "+1122334455",
          location: "789 Pine Road, Emergency City",
          emergencyDescription: "Respiratory Emergency: Difficulty breathing",
          requestTime: new Date(Date.now() - 300000).toISOString(),
          status: "PENDING",
        },
      ];
      localStorage.setItem(
        "ambulance_requests",
        JSON.stringify(sampleRequests)
      );
      console.log("Initialized sample request data");
    }

    // Initialize patients if none exist
    const existingPatients = localStorage.getItem("ambulance_patients");
    if (!existingPatients) {
      const samplePatients = [
        {
          id: 1,
          name: "John Doe",
          contact: "+1234567890",
          medicalNotes: "History of heart disease",
          createdAt: new Date(Date.now() - 86400000).toISOString(),
          updatedAt: new Date(Date.now() - 3600000).toISOString(),
        },
        {
          id: 2,
          name: "Jane Smith",
          contact: "+1987654321",
          medicalNotes: "No known allergies",
          createdAt: new Date(Date.now() - 7200000).toISOString(),
          updatedAt: new Date(Date.now() - 1800000).toISOString(),
        },
      ];
      localStorage.setItem(
        "ambulance_patients",
        JSON.stringify(samplePatients)
      );
      console.log("Initialized sample patient data");
    }
  }

  // UI Utility functions
  showLoading(message) {
    let loadingDiv = document.getElementById("loading-indicator");
    if (!loadingDiv) {
      loadingDiv = document.createElement("div");
      loadingDiv.id = "loading-indicator";
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
        min-width: 300px;
        max-width: 90%;
      `;
      document.body.appendChild(loadingDiv);
    }

    loadingDiv.innerHTML = `
      <button id="close-loading" style="
        position: absolute;
        top: 8px;
        right: 8px;
        background: none;
        border: none;
        font-size: 1.2rem;
        cursor: pointer;
        color: #666;
      ">×</button>
      <div class="spinner" style="margin-bottom: 1rem;"></div>
      <p>${message}</p>
    `;
    loadingDiv.style.display = "block";

    // Add click event for close button
    const closeButton = document.getElementById("close-loading");
    if (closeButton) {
      closeButton.addEventListener("click", () => this.hideLoading());
    }
  }

  hideLoading() {
    const loadingDiv = document.getElementById("loading-indicator");
    if (loadingDiv) {
      loadingDiv.style.display = "none";
    }
  }

  showError(message) {
    this.hideLoading();
    this.showNotification(message, "error");
  }

  showSuccess(message) {
    this.hideLoading();
    this.showNotification(message, "success");
  }

  showNotification(message, type = "info") {
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

    // Auto-remove after 5 seconds
    setTimeout(() => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    }, 5000);

    // Remove on click
    notification.addEventListener("click", () => {
      if (notification.parentNode) {
        notification.parentNode.removeChild(notification);
      }
    });
  }

  // Format utility functions
  formatDateTime(dateString) {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleString();
  }

  formatTime(dateString) {
    if (!dateString) return "N/A";
    return new Date(dateString).toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    });
  }
}

// Create global application manager instance
const app = new ApplicationManager();

// Legacy compatibility functions
function showPage(pageId) {
  // This will be overridden by app.init()
  console.log("showPage called before initialization");
}

function submitAmbulanceRequest() {
  return app.submitAmbulanceRequest();
}

function getCurrentLocation() {
  return app.getCurrentLocation();
}

function contactDispatch() {
  return app.contactDispatch();
}

function showLoginPrompt() {
  return app.showLoginPrompt();
}

function logout() {
  return app.logout();
}

function trackRequestStatus(requestId) {
  return app.trackRequestStatus(requestId);
}

function showLoading(message) {
  return app.showLoading(message);
}

function hideLoading() {
  return app.hideLoading();
}

function showError(message) {
  return app.showError(message);
}

function showSuccess(message) {
  return app.showSuccess(message);
}

function showNotification(message, type) {
  return app.showNotification(message, type);
}

function formatDateTime(dateString) {
  return app.formatDateTime(dateString);
}

function formatTime(dateString) {
  return app.formatTime(dateString);
}


// Auto-initialize when DOM is ready
document.addEventListener("DOMContentLoaded", function () {
  console.log("DOM loaded, initializing application...");
  app.init();
});

// Export for module use
if (typeof module !== "undefined" && module.exports) {
  module.exports = {
    ApplicationManager,
    app,
    showPage,
    submitAmbulanceRequest,
    getCurrentLocation,
    contactDispatch,
    showLoginPrompt,
    logout,
    trackRequestStatus,
    showLoading,
    hideLoading,
    showError,
    showSuccess,
    showNotification,
    formatDateTime,
    formatTime,
  };
}

console.log("Main application loaded successfully");
