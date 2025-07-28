class UnifiedDashboard {
  constructor() {
    this.currentSection = "dashboard";
    this.isInitialized = false;
    this.userRole = null;
    this.currentUser = null;
    this.currentPage = 0;
    this.pageSize = 10;
    this.refreshInterval = null;
  }

  // ===== Utility Methods =====
  showNotification(message, type = "info") {
    if (window.showNotification) {
      window.showNotification(message, type);
      return;
    }
    const notification = document.createElement("div");
    notification.style.cssText = `
      position: fixed; top: 20px; right: 20px; padding: 1rem 1.5rem;
      border-radius: 8px; color: white; font-weight: 500; z-index: 1001;
      animation: slideIn 0.3s ease-out; max-width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.15);
    `;
    switch (type) {
      case "success":
        notification.style.background = "#10b981";
        break;
      case "error":
        notification.style.background = "#ef4444";
        break;
      case "warning":
        notification.style.background = "#f59e0b";
        break;
      default:
        notification.style.background = "#3b82f6";
    }
    notification.textContent = message;
    document.body.appendChild(notification);
    setTimeout(() => notification.remove(), 5000);
    notification.addEventListener("click", () => notification.remove());
  }

  showError(message) {
    this.showNotification(message, "error");
  }

  updateElementSafe(elementId, value) {
    const el = document.getElementById(elementId);
    if (el) el.textContent = value;
  }

  async apiCall(endpoint, options = {}) {
    if (window.apiCall) return await window.apiCall(endpoint, options);
    const url = `/api${endpoint}`;
    const token = localStorage.getItem("token");
    const config = {
      headers: {
        "Content-Type": "application/json",
        ...(token && { Authorization: `Bearer ${token}` }),
      },
      ...options,
    };
    try {
      const response = await fetch(url, config);
      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        throw new Error("Authentication required");
      }
      if (!response.ok) {
        const errorText = await response.text();
        let errorData = {};
        try {
          errorData = JSON.parse(errorText);
        } catch (e) {
          errorData = { error: errorText || `HTTP ${response.status}` };
        }
        throw new Error(
          errorData.error || errorData.message || `HTTP ${response.status}`
        );
      }
      return await response.json();
    } catch (error) {
      console.error(`API call failed: ${endpoint}`, error);
      throw error;
    }
  }

  formatDateTime(dateString) {
    if (!dateString) return "N/A";
    try {
      const date = new Date(dateString);
      return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      })}`;
    } catch {
      return "Invalid Date";
    }
  }

  formatTime(dateString) {
    if (!dateString) return "N/A";
    try {
      const date = new Date(dateString);
      return date.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return "Invalid Time";
    }
  }

  formatStatus(status) {
    if (!status) return "Unknown";
    return status.replace(/_/g, " ").replace(/\b\w/g, (c) => c.toUpperCase());
  }

  // ===== Initialization =====
  async initialize() {
    if (this.isInitialized) return;
    const token = localStorage.getItem("token");
    const userStr = localStorage.getItem("user");
    if (!token || !userStr) return;
    this.currentUser = JSON.parse(userStr);
    this.userRole = this.currentUser.role;
    const adminDashboard = document.getElementById("admin-dashboard");
    const userDashboard = document.getElementById("user-dashboard");
    if (
      adminDashboard &&
      (this.userRole.includes("ADMIN") || this.userRole.includes("DISPATCHER"))
    ) {
      await this.initializeAdminDashboard();
    } else if (userDashboard && this.userRole.includes("USER")) {
      await this.initializeUserDashboard();
    }
    this.isInitialized = true;
  }

  // ===== Admin Dashboard =====
  async initializeAdminDashboard() {
    this.showAdminSection("dashboard");
    this.setupAdminEventListeners();
    this.refreshInterval = setInterval(() => {
      const refreshFunction = {
        dashboard: () => this.loadDashboardData(),
        dispatch: () => this.loadDispatchData(),
        ambulances: () => this.loadAmbulancesData(),
        patients: () => this.loadPatientsData(),
        history: () => this.loadHistoryData(),
      }[this.currentSection];
      if (refreshFunction) refreshFunction().catch(console.error);
    }, 30000);
  }

  setupAdminEventListeners() {
    document.addEventListener("click", (e) => {
      if (e.target.matches("[data-section]")) {
        e.preventDefault();
        const sectionId = e.target.getAttribute("data-section");
        this.showAdminSection(sectionId, e);
      }
    });
    const refreshButton = document.getElementById("refresh-requests");
    if (refreshButton)
      refreshButton.addEventListener("click", () => this.refreshRequests());
  }

  showAdminSection(sectionId, event) {
    const navButtons = document.querySelectorAll(".nav-btn");
    navButtons.forEach((btn) => btn.classList.remove("active"));
    const clickedButton = event
      ? event.currentTarget
      : document.querySelector(`[onclick*="${sectionId}"]`);
    if (clickedButton) clickedButton.classList.add("active");
    const sections = document.querySelectorAll(".admin-section");
    sections.forEach((section) => {
      section.classList.remove("active");
      section.style.display = "none";
    });
    const targetSection = document.getElementById(`${sectionId}-section`);
    if (targetSection) {
      targetSection.classList.add("active");
      targetSection.style.display = "block";
      this.currentSection = sectionId;
      const loadFunction = {
        dashboard: () => this.loadDashboardData(),
        dispatch: () => this.loadDispatchData(),
        ambulances: () => this.loadAmbulancesData(),
        patients: () => this.loadPatientsData(),
        history: () => this.loadHistoryData(),
      }[sectionId];
      if (loadFunction)
        loadFunction().catch((error) =>
          this.showError(`Error loading ${sectionId}: ${error.message}`)
        );
    }
  }

  // ===== Data Retrieval with Fallback =====
  getLocalStorageRequests() {
    const stored =
      localStorage.getItem("ambulance_requests") ||
      localStorage.getItem("ambulanceRequests");
    return stored ? JSON.parse(stored) : [];
  }

  getLocalStorageAmbulances() {
    const stored =
      localStorage.getItem("ambulance_ambulances") ||
      localStorage.getItem("ambulances");
    return stored ? JSON.parse(stored) : [];
  }

  saveLocalStorageAmbulances(ambulances) {
    localStorage.setItem("ambulance_ambulances", JSON.stringify(ambulances));
  }

  getLocalStoragePatients() {
    const stored =
      localStorage.getItem("ambulance_patients") ||
      localStorage.getItem("patients");
    return stored ? JSON.parse(stored) : [];
  }

  async getDataWithFallback(apiEndpoint, localStorageGetter) {
    try {
      const data = await this.apiCall(apiEndpoint);
      return data;
    } catch (error) {
      return localStorageGetter();
    }
  }

  // ===== Admin Dashboard Data Loading =====
  async loadDashboardData() {
    try {
      const [requests, ambulances] = await Promise.all([
        this.getDataWithFallback("/requests", () =>
          this.getLocalStorageRequests()
        ),
        this.getDataWithFallback("/ambulances", () =>
          this.getLocalStorageAmbulances()
        ),
      ]);
      const activeRequests = requests.filter((r) =>
        ["PENDING", "DISPATCHED", "IN_PROGRESS"].includes(r.status)
      ).length;
      const availableAmbulances = ambulances.filter(
        (a) => a.availability === "AVAILABLE" || a.status === "available"
      ).length;
      const todayRequests = requests.filter((r) => {
        if (!r.requestTime) return false;
        const requestDate = new Date(r.requestTime);
        const today = new Date();
        return requestDate.toDateString() === today.toDateString();
      }).length;
      const completedRequests = requests.filter(
        (r) => r.status === "COMPLETED" && r.dispatchTime
      );
      const avgResponseTime =
        completedRequests.length > 0
          ? Math.round(
              completedRequests.reduce((sum, r) => {
                const requestTime = new Date(r.requestTime);
                const dispatchTime = new Date(r.dispatchTime);
                return sum + (dispatchTime - requestTime) / (1000 * 60);
              }, 0) / completedRequests.length
            )
          : 0;
      this.updateElementSafe("active-requests", activeRequests);
      this.updateElementSafe("available-ambulances", availableAmbulances);
      this.updateElementSafe("avg-response-time", avgResponseTime);
      this.updateElementSafe("total-requests", todayRequests);
      this.loadRecentActivity(requests);
    } catch (error) {
      this.showError("Error loading dashboard data: " + error.message);
      this.updateElementSafe("active-requests", 0);
      this.updateElementSafe("available-ambulances", 0);
      this.updateElementSafe("avg-response-time", 0);
      this.updateElementSafe("total-requests", 0);
    }
  }

  loadRecentActivity(requests) {
    const recentRequests = requests
      .filter((r) => r.requestTime)
      .sort((a, b) => new Date(b.requestTime) - new Date(a.requestTime))
      .slice(0, 5);
    const container = document.getElementById("recent-requests");
    if (!container) return;
    container.innerHTML = "";
    if (recentRequests.length === 0) {
      container.innerHTML =
        '<p style="color: #6b7280; padding: 1rem;">No recent activity</p>';
      return;
    }
    recentRequests.forEach((request) => {
      const item = document.createElement("div");
      item.className = "activity-item";
      item.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; padding: 1rem; border: 1px solid #e5e7eb; border-radius: 8px; margin-bottom: 0.5rem;">
          <div>
            <strong>#${request.id}</strong> - ${
        request.userName || request.callerName || "Unknown"
      }
            <br>
            <small style="color: #6b7280;">${
              request.location || request.address || "No location"
            }</small>
          </div>
          <div style="text-align: right;">
            <span class="request-status ${(
              request.status || "pending"
            ).toLowerCase()}">${request.status || "Pending"}</span>
            <br>
            <small style="color: #6b7280;">${this.formatTime(
              request.requestTime
            )}</small>
          </div>
        </div>
      `;
      container.appendChild(item);
    });
  }

  // ===== History Data =====
  async loadHistoryData() {
    const historyContainer = document.getElementById("history-container");
    if (!historyContainer) return;
    try {
      historyContainer.innerHTML =
        '<div class="text-center py-4">Loading history...</div>';
      const history = await this.apiCall("/service-history");
      if (history && Array.isArray(history)) {
        this.displayHistory(history);
      } else {
        await this.loadFallbackHistory();
      }
    } catch (error) {
      historyContainer.innerHTML = `
        <div class="alert alert-danger">
          <p>Error loading service history: ${
            error.message || "Unknown error"
          }</p>
          <button class="btn btn-primary btn-sm" onclick="dashboard.loadHistoryData()">Retry</button>
        </div>
      `;
      await this.loadFallbackHistory();
    }
  }

  async loadFallbackHistory() {
    const historyContainer = document.getElementById("history-container");
    try {
      const requests = await this.apiCall("/requests");
      if (requests && Array.isArray(requests)) {
        const completedRequests = requests.filter(
          (r) => r.status === "COMPLETED"
        );
        if (completedRequests.length > 0) {
          this.displayHistoryFromRequests(completedRequests);
        } else {
          this.showNoHistoryMessage();
        }
      } else {
        this.showNoHistoryMessage();
      }
    } catch (error) {
      if (historyContainer.innerHTML.includes("Error loading service history"))
        return;
      historyContainer.innerHTML = `
        <div class="alert alert-warning">
          <p>No service history available. Please try again later.</p>
          <button class="btn btn-primary btn-sm" onclick="dashboard.loadHistoryData()">Retry</button>
        </div>
      `;
    }
  }

  showNoHistoryMessage() {
    const historyContainer = document.getElementById("history-container");
    historyContainer.innerHTML = `<div class="alert alert-info">No service history found.</div>`;
  }

  displayHistory(history) {
    const container = document.getElementById("history-container");
    if (!container) return;
    if (!history || !Array.isArray(history) || history.length === 0) {
      container.innerHTML =
        '<div class="alert alert-info">No service history found.</div>';
      return;
    }
    container.innerHTML = `
      <div style="width: 100%; overflow-x: auto;">
        <table style="width: 100%; border-collapse: collapse; min-width: 800px;">
          <thead style="background-color: #f8f9fa;">
            <tr>
              <th>ID</th>
              <th>Request ID</th>
              <th>Patient ID</th>
              <th>Ambulance ID</th>
              <th>Status</th>
              <th>Created</th>
              <th>Arrival Time</th>
              <th>Completion Time</th>
            </tr>
          </thead>
          <tbody>
            ${history
              .sort(
                (a, b) =>
                  new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
              )
              .map((item) => {
                const status = item.status || "UNKNOWN";
                return `
                  <tr>
                    <td>${item.id || "N/A"}</td>
                    <td>${item.requestId || "N/A"}</td>
                    <td>${item.patientId || "N/A"}</td>
                    <td>${item.ambulanceId || "N/A"}</td>
                    <td>${status.replace(/_/g, " ")}</td>
                    <td>${
                      item.createdAt
                        ? this.formatDateTime(item.createdAt)
                        : "N/A"
                    }</td>
                    <td>${
                      item.arrivalTime
                        ? this.formatDateTime(item.arrivalTime)
                        : "N/A"
                    }</td>
                    <td>${
                      item.completionTime
                        ? this.formatDateTime(item.completionTime)
                        : "N/A"
                    }</td>
                  </tr>
                `;
              })
              .join("")}
          </tbody>
        </table>
      </div>
    `;
  }

  displayHistoryFromRequests(requests) {
    const container = document.getElementById("history-container");
    if (!container) return;
    if (requests.length === 0) {
      container.innerHTML =
        '<p style="text-align: center; color: #6b7280; padding: 2rem;">No completed requests found</p>';
      return;
    }
    let html = `
      <table class="history-table">
        <thead>
          <tr>
            <th>Request ID</th>
            <th>Patient</th>
            <th>Emergency Type</th>
            <th>Status</th>
            <th>Request Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
    `;
    const sortedRequests = [...requests].sort(
      (a, b) => new Date(b.requestTime || 0) - new Date(a.requestTime || 0)
    );
    sortedRequests.forEach((request) => {
      html += `
        <tr>
          <td>#${request.id || "N/A"}</td>
          <td>${request.patientName || "N/A"}</td>
          <td>${request.emergencyType || "N/A"}</td>
          <td>${request.status || "UNKNOWN"}</td>
          <td>${
            request.requestTime
              ? this.formatDateTime(request.requestTime)
              : "N/A"
          }</td>
          <td>
            <button class="btn btn-sm btn-outline" onclick="dashboard.viewRequestDetails('${
              request.id
            }')">View</button>
          </td>
        </tr>
      `;
    });
    html += `</tbody></table>`;
    container.innerHTML = html;
  }

  // ===== Dispatch & Requests =====
  async loadDispatchData() {
    try {
      const requests = await this.getDataWithFallback("/requests", () =>
        this.getLocalStorageRequests()
      );
      this.displayRequests(requests);
    } catch (error) {
      this.showError("Error loading requests: " + error.message);
    }
  }

  displayRequests(requests) {
    const container = document.getElementById("requests-list");
    if (!container) return;
    container.innerHTML = "";
    if (requests.length === 0) {
      container.innerHTML =
        '<p style="text-align: center; color: #6b7280; padding: 2rem;">No requests found</p>';
      return;
    }
    const sortedRequests = requests
      .filter((r) => r.requestTime)
      .sort((a, b) => new Date(b.requestTime) - new Date(a.requestTime));
    sortedRequests.forEach((request) => {
      const card = this.createRequestCard(request);
      container.appendChild(card);
    });
  }

  createRequestCard(request) {
    const card = document.createElement("div");
    const status = request.status || "PENDING";
    card.className = `request-card ${status.toLowerCase()}`;
    card.style.cssText = `
      background: white; padding: 1.5rem; border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 1rem;
      border-left: 4px solid ${
        status === "PENDING"
          ? "#3b82f6"
          : status === "DISPATCHED"
          ? "#f59e0b"
          : "#6b7280"
      };
    `;
    const responseTime = request.dispatchTime
      ? Math.round(
          (new Date(request.dispatchTime) - new Date(request.requestTime)) /
            (1000 * 60)
        )
      : null;
    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
        <div style="font-weight: bold; font-size: 1.1rem;">#${request.id}</div>
        <span style="padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: bold; background: ${
          status === "PENDING"
            ? "#dbeafe"
            : status === "DISPATCHED"
            ? "#fef3c7"
            : "#f3f4f6"
        }; color: ${
      status === "PENDING"
        ? "#1e40af"
        : status === "DISPATCHED"
        ? "#92400e"
        : "#374151"
    };">${status}</span>
      </div>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
        <div><strong>Patient:</strong> ${
          request.userName || request.patientName || "Unknown"
        }</div>
        <div><strong>Contact:</strong> ${
          request.userContact || request.phone || "N/A"
        }</div>
        <div><strong>Location:</strong> ${
          request.location || request.address || "N/A"
        }</div>
        <div><strong>Emergency:</strong> ${
          request.emergencyDescription || request.emergencyType || "N/A"
        }</div>
        <div><strong>Request Time:</strong> ${this.formatDateTime(
          request.requestTime
        )}</div>
        ${
          request.ambulanceId
            ? `<div><strong>Ambulance:</strong> #${request.ambulanceId}</div>`
            : ""
        }
        ${
          responseTime
            ? `<div><strong>Response Time:</strong> ${responseTime} minutes</div>`
            : ""
        }
      </div>
      <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
        ${
          status === "PENDING"
            ? `<button class="btn btn-primary" onclick="dashboard.dispatchRequest(${request.id})">Dispatch Ambulance</button>`
            : ""
        }
        ${
          status === "DISPATCHED"
            ? `
          <button class="btn btn-success" onclick="dashboard.updateRequestStatus(${request.id}, 'IN_PROGRESS')">Mark Arrived</button>
          <button class="btn btn-success" onclick="dashboard.updateRequestStatus(${request.id}, 'COMPLETED')">Mark Complete</button>
        `
            : ""
        }
        ${
          status === "IN_PROGRESS"
            ? `<button class="btn btn-success" onclick="dashboard.updateRequestStatus(${request.id}, 'COMPLETED')">Mark Complete</button>`
            : ""
        }
        <button class="btn btn-secondary" onclick="dashboard.viewRequestDetails(${
          request.id
        })">View Details</button>
      </div>
    `;
    return card;
  }

  async dispatchRequest(requestId) {
    if (!confirm("Dispatch an ambulance for this request?")) return;
    try {
      await this.apiCall(`/dispatch/${requestId}`, { method: "POST" });
      this.showNotification("Ambulance dispatched successfully!", "success");
    } catch (apiError) {
      const requests = this.getLocalStorageRequests();
      const ambulances = this.getLocalStorageAmbulances();
      const request = requests.find((r) => r.id == requestId);
      const availableAmbulance = ambulances.find(
        (a) => a.availability === "AVAILABLE" || a.status === "available"
      );
      if (request && availableAmbulance) {
        request.status = "DISPATCHED";
        request.ambulanceId = availableAmbulance.id;
        request.dispatchTime = new Date().toISOString();
        availableAmbulance.availability = "DISPATCHED";
        availableAmbulance.status = "dispatched";
        localStorage.setItem("ambulance_requests", JSON.stringify(requests));
        this.saveLocalStorageAmbulances(ambulances);
        this.showNotification(
          "Ambulance dispatched successfully (locally)!",
          "success"
        );
      } else {
        this.showError("No available ambulance found");
      }
    }
    this.loadDispatchData();
    if (this.currentSection === "dashboard") this.loadDashboardData();
  }

  async updateRequestStatus(requestId, newStatus) {
    try {
      await this.apiCall(`/requests/${requestId}/status?status=${newStatus}`, {
        method: "PUT",
      });
      this.showNotification(
        `Request status updated to ${newStatus}`,
        "success"
      );
    } catch (apiError) {
      const requests = this.getLocalStorageRequests();
      const request = requests.find((r) => r.id == requestId);
      if (request) {
        request.status = newStatus;
        if (newStatus === "COMPLETED" && request.ambulanceId) {
          const ambulances = this.getLocalStorageAmbulances();
          const ambulance = ambulances.find((a) => a.id == request.ambulanceId);
          if (ambulance) {
            ambulance.availability = "AVAILABLE";
            ambulance.status = "available";
            this.saveLocalStorageAmbulances(ambulances);
          }
        }
        localStorage.setItem("ambulance_requests", JSON.stringify(requests));
        this.showNotification(
          `Request status updated to ${newStatus} (locally)`,
          "success"
        );
      }
    }
    this.loadDispatchData();
    if (this.currentSection === "dashboard") this.loadDashboardData();
  }

  refreshRequests() {
    this.loadDispatchData();
    this.showNotification("Requests refreshed", "info");
  }

  filterRequests() {
    const filterSelect = document.getElementById("filter-status");
    if (!filterSelect) return;
    const filterValue = filterSelect.value;
    if (filterValue === "all") {
      this.loadDispatchData();
    } else {
      this.getDataWithFallback("/requests", () =>
        this.getLocalStorageRequests()
      )
        .then((requests) => {
          const filtered = requests.filter(
            (r) => (r.status || "PENDING") === filterValue
          );
          this.displayRequests(filtered);
        })
        .catch((error) =>
          this.showError("Error filtering requests: " + error.message)
        );
    }
  }

  // ===== Ambulances =====
  async loadAmbulancesData() {
    try {
      const ambulances = await this.getDataWithFallback("/ambulances", () =>
        this.getLocalStorageAmbulances()
      );
      this.displayAmbulances(ambulances);
    } catch (error) {
      this.showError("Error loading ambulances: " + error.message);
    }
  }

  displayAmbulances(ambulances) {
    const container = document.getElementById("ambulances-grid");
    if (!container) return;
    container.innerHTML = "";
    if (ambulances.length === 0) {
      container.innerHTML =
        '<p style="text-align: center; color: #6b7280; padding: 2rem;">No ambulances found</p>';
      return;
    }
    ambulances.forEach((ambulance) => {
      const card = this.createAmbulanceCard(ambulance);
      container.appendChild(card);
    });
  }

  createAmbulanceCard(ambulance) {
    const card = document.createElement("div");
    const status = ambulance.availability || ambulance.status || "UNKNOWN";
    card.className = `ambulance-card ${status.toLowerCase()}`;
    card.style.cssText = `
      background: white; padding: 1.5rem; border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 1rem;
      border-top: 4px solid ${
        status === "AVAILABLE"
          ? "#10b981"
          : status === "DISPATCHED"
          ? "#f59e0b"
          : "#6b7280"
      };
    `;
    const statusDisplayMap = {
      AVAILABLE: "Available",
      DISPATCHED: "Dispatched",
      MAINTENANCE: "Maintenance",
      OUT_OF_SERVICE: "Out of Service",
      available: "Available",
      dispatched: "Dispatched",
      maintenance: "Maintenance",
    };
    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
        <div style="font-weight: bold; font-size: 1.1rem;">Ambulance #${
          ambulance.id
        }</div>
        <span style="padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: bold; background: ${
          status === "AVAILABLE"
            ? "#d1fae5"
            : status === "DISPATCHED"
            ? "#fef3c7"
            : "#f3f4f6"
        }; color: ${
      status === "AVAILABLE"
        ? "#065f46"
        : status === "DISPATCHED"
        ? "#92400e"
        : "#374151"
    };">${statusDisplayMap[status] || status}</span>
      </div>
      <div style="margin-bottom: 1rem;"><strong>Location:</strong> ${
        ambulance.currentLocation || ambulance.location || "Unknown"
      }</div>
      <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
        ${
          status === "AVAILABLE" || status === "available"
            ? `<button onclick="dashboard.updateAmbulanceStatus(${ambulance.id}, 'MAINTENANCE')">Maintenance</button>`
            : ""
        }
        ${
          status === "MAINTENANCE" || status === "maintenance"
            ? `<button onclick="dashboard.updateAmbulanceStatus(${ambulance.id}, 'AVAILABLE')">Mark Available</button>`
            : ""
        }
        ${
          status === "DISPATCHED" || status === "dispatched"
            ? `<button onclick="dashboard.updateAmbulanceStatus(${ambulance.id}, 'AVAILABLE')">Mark Available</button>`
            : ""
        }
        <button onclick="dashboard.editAmbulance(${
          ambulance.id
        })">Edit Location</button>
      </div>
    `;
    return card;
  }

  async updateAmbulanceStatus(ambulanceId, newStatus) {
    try {
      await this.apiCall(
        `/ambulances/${ambulanceId}/status?status=${newStatus}`,
        { method: "PUT" }
      );
      this.showNotification(
        `Ambulance status updated to ${newStatus}`,
        "success"
      );
    } catch (apiError) {
      const ambulances = this.getLocalStorageAmbulances();
      const ambulance = ambulances.find((a) => a.id == ambulanceId);
      if (ambulance) {
        ambulance.availability = newStatus;
        ambulance.status = newStatus.toLowerCase();
        this.saveLocalStorageAmbulances(ambulances);
        this.showNotification(
          `Ambulance status updated to ${newStatus} (locally)`,
          "success"
        );
      }
    }
    this.loadAmbulancesData();
    if (this.currentSection === "dashboard") this.loadDashboardData();
  }

  editAmbulance(ambulanceId) {
    const newLocation = prompt("Enter new location for the ambulance:");
    if (newLocation && newLocation.trim()) {
      this.apiCall(`/ambulances/${ambulanceId}`, {
        method: "PUT",
        body: JSON.stringify({ currentLocation: newLocation.trim() }),
      })
        .then(() => {
          this.showNotification("Ambulance location updated!", "success");
          this.loadAmbulancesData();
        })
        .catch(() => {
          const ambulances = this.getLocalStorageAmbulances();
          const ambulance = ambulances.find((a) => a.id == ambulanceId);
          if (ambulance) {
            ambulance.currentLocation = newLocation.trim();
            ambulance.location = newLocation.trim();
            this.saveLocalStorageAmbulances(ambulances);
            this.showNotification(
              "Ambulance location updated (locally)!",
              "success"
            );
            this.loadAmbulancesData();
          }
        });
    }
  }

  // ===== Patients =====
  async loadPatientsData() {
    try {
      const patients = await this.getDataWithFallback("/patients", () =>
        this.getLocalStoragePatients()
      );
      this.displayPatients(patients);
    } catch (error) {
      this.showError("Error loading patients: " + error.message);
    }
  }

  displayPatients(patients) {
    const container = document.getElementById("patients-list");
    if (!container) return;
    container.innerHTML = "";
    if (patients.length === 0) {
      container.innerHTML =
        '<p style="text-align: center; color: #6b7280; padding: 2rem;">No patient records found</p>';
      return;
    }
    patients.forEach((patient) => {
      const card = this.createPatientCard(patient);
      container.appendChild(card);
    });
  }

  createPatientCard(patient) {
    const card = document.createElement("div");
    card.className = "patient-card";
    card.style.cssText = `
      background: white; padding: 1.5rem; border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 1rem;
    `;
    card.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
        <div style="font-weight: bold; font-size: 1.1rem;">${
          patient.name || "Unknown"
        }</div>
        <div style="color: #6b7280; font-size: 0.875rem;">ID: ${
          patient.id
        }</div>
      </div>
      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;">
        <div><strong>Contact:</strong> ${patient.contact || "N/A"}</div>
        <div><strong>Created:</strong> ${this.formatDateTime(
          patient.createdAt
        )}</div>
        <div><strong>Updated:</strong> ${this.formatDateTime(
          patient.updatedAt
        )}</div>
      </div>
      <div style="margin-bottom: 1rem;"><strong>Medical Notes:</strong> ${
        patient.medicalNotes || "N/A"
      }</div>
      <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
        <button onclick="dashboard.editPatient(${patient.id})">Edit</button>
      </div>
    `;
    return card;
  }

  editPatient(patientId) {
    // Implement as needed
    this.showNotification("Edit patient feature coming soon.", "info");
  }

  searchPatients() {
    const searchInput = document.getElementById("patient-search");
    if (!searchInput) return;
    const searchTerm = searchInput.value.toLowerCase();
    if (searchTerm.length === 0) {
      this.loadPatientsData();
      return;
    }
    this.getDataWithFallback("/patients", () => this.getLocalStoragePatients())
      .then((patients) => {
        const filtered = patients.filter(
          (patient) =>
            (patient.name || "").toLowerCase().includes(searchTerm) ||
            (patient.contact || "").includes(searchTerm)
        );
        this.displayPatients(filtered);
      })
      .catch((error) =>
        this.showError("Error searching patients: " + error.message)
      );
  }

  // ===== Request Details Modal =====
  viewRequestDetails(requestId) {
    this.getDataWithFallback("/requests", () => this.getLocalStorageRequests())
      .then((requests) => {
        const request = requests.find((r) => r.id == requestId);
        if (request) {
          const detailsModal = this.createRequestDetailsModal(request);
          document.body.appendChild(detailsModal);
          detailsModal.classList.add("active");
        } else {
          this.showError("Request not found");
        }
      })
      .catch((error) =>
        this.showError("Error loading request details: " + error.message)
      );
  }

  createRequestDetailsModal(request) {
    const modal = document.createElement("div");
    modal.className = "modal";
    modal.style.cssText = `
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000;
    `;
    modal.innerHTML = `
      <div style="background: white; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1); max-width: 600px; width: 90%; max-height: 90vh; overflow-y: auto;">
        <div style="padding: 1.5rem 1.5rem 0; display: flex; justify-content: space-between; align-items: center;">
          <h3 style="font-size: 1.25rem; font-weight: 600; color: #111827;">Request Details - #${
            request.id
          }</h3>
          <button onclick="this.closest('.modal').remove()" style="background: none; border: none; font-size: 1.5rem; color: #6b7280; cursor: pointer; padding: 0; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">×</button>
        </div>
        <div style="padding: 1.5rem;">
          <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong>Status</strong>
              <span>${request.status || "Pending"}</span>
            </div>
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong>Patient</strong>
              <span>${
                request.userName || request.patientName || "Unknown"
              }</span>
            </div>
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong>Contact</strong>
              <span>${request.userContact || request.phone || "N/A"}</span>
            </div>
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong>Request Time</strong>
              <span>${this.formatDateTime(request.requestTime)}</span>
            </div>
            ${
              request.dispatchTime
                ? `
              <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
                <strong>Dispatch Time</strong>
                <span>${this.formatDateTime(request.dispatchTime)}</span>
              </div>
            `
                : ""
            }
            ${
              request.ambulanceId
                ? `
              <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
                <strong>Assigned Ambulance</strong>
                <span>#${request.ambulanceId}</span>
              </div>
            `
                : ""
            }
          </div>
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444; margin-bottom: 1rem;">
            <strong>Location</strong>
            <span>${request.location || request.address || "N/A"}</span>
          </div>
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
            <strong>Emergency Description</strong>
            <span>${
              request.emergencyDescription || request.emergencyType || "N/A"
            }</span>
          </div>
        </div>
        <div style="padding: 0 1.5rem 1.5rem; display: flex; gap: 1rem; justify-content: flex-end;">
          <button onclick="this.closest('.modal').remove()" style="background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">Close</button>
        </div>
      </div>
    `;
    return modal;
  }

  // ===== User Dashboard =====
  async initializeUserDashboard() {
    this.setupUserEventListeners();
    await this.loadUserDashboardData();
    this.refreshInterval = setInterval(
      () => this.loadUserDashboardData(),
      30000
    );
  }

  setupUserEventListeners() {
    const refreshButton = document.getElementById("refresh-requests");
    if (refreshButton)
      refreshButton.addEventListener("click", () =>
        this.loadUserDashboardData()
      );
  }

  async loadUserDashboardData() {
    try {
      const activeRequestsContainer = document.getElementById(
        "active-requests-container"
      );
      const requestHistoryContainer = document.getElementById(
        "request-history-container"
      );
      if (activeRequestsContainer)
        activeRequestsContainer.innerHTML =
          '<div class="loading">Loading active requests...</div>';
      if (requestHistoryContainer)
        requestHistoryContainer.innerHTML =
          '<div class="loading">Loading request history...</div>';
      await Promise.all([
        this.loadActiveRequests(),
        this.loadRequestHistory(this.currentPage),
      ]);
    } catch (error) {
      this.showError("Failed to load dashboard data. Please try again.");
    }
  }

  async loadActiveRequests() {
    const activeRequestsContainer = document.getElementById(
      "active-requests-container"
    );
    if (!activeRequestsContainer) return;
    try {
      const response = await fetch("/api/requests/user/active", {
        headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
      });
      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);
      const data = await response.json();
      this.displayActiveRequests(data.content || data);
    } catch (error) {
      activeRequestsContainer.innerHTML = `
        <div class="error">
          Failed to load active requests.
          <button onclick="dashboard.loadActiveRequests()">Retry</button>
        </div>`;
    }
  }

  async loadRequestHistory(page) {
    const requestHistoryContainer = document.getElementById(
      "request-history-container"
    );
    if (!requestHistoryContainer) return;
    try {
      const response = await fetch(
        `/api/requests/user/history?page=${page}&size=${this.pageSize}`,
        {
          headers: { Authorization: `Bearer ${localStorage.getItem("token")}` },
        }
      );
      if (!response.ok)
        throw new Error(`HTTP error! status: ${response.status}`);
      const data = await response.json();
      this.displayRequestHistory(
        data.content || data,
        data.totalPages,
        data.number
      );
    } catch (error) {
      requestHistoryContainer.innerHTML = `
        <div class="error">
          Failed to load request history.
          <button onclick="dashboard.loadRequestHistory(${this.currentPage})">Retry</button>
        </div>`;
    }
  }

  displayActiveRequests(requests) {
    const activeRequestsContainer = document.getElementById(
      "active-requests-container"
    );
    if (!activeRequestsContainer) return;
    if (!requests || requests.length === 0) {
      activeRequestsContainer.innerHTML =
        '<div class="no-requests">No active requests found.</div>';
      return;
    }
    const html = `
      <div class="requests-grid">
        ${requests
          .map(
            (request) => `
          <div class="request-card" data-id="${request.id}">
            <h4>${request.userName || "Unknown User"}</h4>
            <p><strong>Status:</strong> <span class="status ${request.status.toLowerCase()}">${this.formatStatus(
              request.status
            )}</span></p>
            <p><strong>Location:</strong> ${request.location}</p>
            <p><strong>Requested:</strong> ${this.formatDate(
              request.requestTime
            )}</p>
            <button class="btn btn-view" onclick="dashboard.viewRequestDetails(${
              request.id
            })">View Details</button>
          </div>
        `
          )
          .join("")}
      </div>`;
    activeRequestsContainer.innerHTML = html;
  }

  displayRequestHistory(requests, totalPages, currentPage) {
    const requestHistoryContainer = document.getElementById(
      "request-history-container"
    );
    const paginationContainer = document.getElementById("pagination-controls");
    if (!requestHistoryContainer) return;
    if (!requests || requests.length === 0) {
      requestHistoryContainer.innerHTML =
        '<div class="no-requests">No request history found.</div>';
      if (paginationContainer) paginationContainer.innerHTML = "";
      return;
    }
    const html = `
      <div class="requests-list">
        ${requests
          .map(
            (request) => `
          <div class="request-item" data-id="${request.id}">
            <div class="request-header">
              <span class="request-id">#${request.id}</span>
              <span class="status ${request.status.toLowerCase()}">${this.formatStatus(
              request.status
            )}</span>
            </div>
            <div class="request-details">
              <p><strong>Location:</strong> ${request.location}</p>
              <p><strong>Requested:</strong> ${this.formatDate(
                request.requestTime
              )}</p>
              ${
                request.completionTime
                  ? `<p><strong>Completed:</strong> ${this.formatDate(
                      request.completionTime
                    )}</p>`
                  : ""
              }
            </div>
            <button class="btn btn-view" onclick="dashboard.viewRequestDetails(${
              request.id
            })">View</button>
          </div>
        `
          )
          .join("")}
      </div>`;
    requestHistoryContainer.innerHTML = html;
    if (paginationContainer && totalPages > 1)
      this.updatePagination(totalPages, currentPage);
  }

  updatePagination(totalPages, currentPage) {
    const paginationContainer = document.getElementById("pagination-controls");
    if (!paginationContainer) return;
    let paginationHTML = '<div class="pagination">';
    if (currentPage > 0)
      paginationHTML += `<button onclick="dashboard.changePage(${
        currentPage - 1
      })">&laquo; Previous</button>`;
    for (let i = 0; i < totalPages; i++) {
      if (i === currentPage) {
        paginationHTML += `<span class="current-page">${i + 1}</span>`;
      } else {
        paginationHTML += `<button onclick="dashboard.changePage(${i})">${
          i + 1
        }</button>`;
      }
    }
    if (currentPage < totalPages - 1)
      paginationHTML += `<button onclick="dashboard.changePage(${
        currentPage + 1
      })">Next &raquo;</button>`;
    paginationHTML += "</div>";
    paginationContainer.innerHTML = paginationHTML;
  }

  changePage(page) {
    this.currentPage = page;
    this.loadRequestHistory(this.currentPage);
  }
}

// Expose dashboard instance and navigation globally
window.dashboard = new UnifiedDashboard();
window.showAdminSection = function (sectionId, event) {
  dashboard.showAdminSection(sectionId, event);
};
