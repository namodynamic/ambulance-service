// Admin dashboard functionality with backend integration

// Global admin state
const AdminState = {
  currentSection: 'dashboard',
  isInitialized: false
};

// Add required CSS for animations and styling
const adminStyle = document.createElement('style');
adminStyle.textContent = `
  /* Admin Dashboard Layout */
  .admin-container {
    display: flex;
    min-height: 100vh;
    background-color: #f8f9fa;
  }

  /* Sidebar Styles */
  .admin-sidebar {
    width: 250px;
    background: #1a237e;
    color: white;
    padding: 1.5rem 0;
    transition: all 0.3s ease;
  }

  .admin-main {
    flex: 1;
    padding: 2rem;
    overflow-y: auto;
  }

  /* Navigation */
  .admin-nav-item {
    padding: 0.75rem 1.5rem;
    color: rgba(255, 255, 255, 0.8);
    text-decoration: none;
    display: block;
    transition: all 0.2s ease;
  }

  .admin-nav-item:hover,
  .admin-nav-item.active {
    background: rgba(255, 255, 255, 0.1);
    color: white;
  }

  /* Section Management */
  .admin-section {
    display: none;
    animation: fadeIn 0.3s ease;
  }

  .admin-section.active {
    display: block;
  }

  /* Animations */
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
  }

  @keyframes slideIn {
    from { transform: translateX(20px); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
  }

  /* Cards */
  .card {
    background: white;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
    margin-bottom: 1.5rem;
    overflow: hidden;
    transition: transform 0.2s, box-shadow 0.2s;
  }

  .card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  }

  .card-header {
    padding: 1rem 1.5rem;
    border-bottom: 1px solid #e5e7eb;
    font-weight: 600;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .card-body {
    padding: 1.5rem;
  }

  /* Buttons */
  .btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0.5rem 1rem;
    border-radius: 4px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s;
    border: 1px solid transparent;
  }

  .btn-primary {
    background: #2563eb;
    color: white;
  }

  .btn-primary:hover {
    background: #1d4ed8;
  }

  .btn-danger {
    background: #dc2626;
    color: white;
  }

  .btn-danger:hover {
    background: #b91c1c;
  }

  /* Status Badges */
  .status-badge {
    display: inline-block;
    padding: 0.25rem 0.5rem;
    border-radius: 9999px;
    font-size: 0.75rem;
    font-weight: 600;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .status-pending { background: #fef3c7; color: #92400e; }
  .status-dispatched { background: #dbeafe; color: #1e40af; }
  .status-arrived { background: #dcfce7; color: #166534; }
  .status-completed { background: #e5e7eb; color: #374151; }
  .status-cancelled { background: #fee2e2; color: #991b1b; }

  /* Forms */
  .form-group {
    margin-bottom: 1rem;
  }

  .form-label {
    display: block;
    margin-bottom: 0.5rem;
    font-weight: 500;
    color: #374151;
  }

  .form-control {
    width: 100%;
    padding: 0.5rem 0.75rem;
    border: 1px solid #d1d5db;
    border-radius: 4px;
    font-size: 1rem;
    transition: border-color 0.2s;
  }

  .form-control:focus {
    outline: none;
    border-color: #3b82f6;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  }

  /* Modal */
  .modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    opacity: 0;
    visibility: hidden;
    transition: all 0.3s;
  }

  .modal.active {
    opacity: 1;
    visibility: visible;
  }

  .modal-content {
    background: white;
    border-radius: 8px;
    width: 90%;
    max-width: 500px;
    max-height: 90vh;
    overflow-y: auto;
    transform: translateY(20px);
    transition: transform 0.3s;
  }

  .modal.active .modal-content {
    transform: translateY(0);
  }

  .modal-header {
    padding: 1rem 1.5rem;
    border-bottom: 1px solid #e5e7eb;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .modal-title {
    margin: 0;
    font-size: 1.25rem;
    font-weight: 600;
  }

  .modal-close {
    background: none;
    border: none;
    font-size: 1.5rem;
    cursor: pointer;
    color: #6b7280;
    padding: 0.25rem;
    line-height: 1;
  }

  .modal-body {
    padding: 1.5rem;
  }

  .modal-footer {
    padding: 1rem 1.5rem;
    border-top: 1px solid #e5e7eb;
    display: flex;
    justify-content: flex-end;
    gap: 0.5rem;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .admin-container {
      flex-direction: column;
    }

    .admin-sidebar {
      width: 100%;
      padding: 0.5rem 0;
    }

    .admin-main {
      padding: 1rem;
    }
  }

  /* Detail View */
  .detail-item {
    padding: 0.75rem;
    background: #f9fafb;
    border-radius: 6px;
    margin-bottom: 0.75rem;
  }

  .detail-item:last-child {
    margin-bottom: 0;
  }

  .detail-item strong {
    display: block;
    color: #374151;
    font-size: 0.875rem;
    margin-bottom: 0.25rem;
  }

  .detail-item span {
    color: #4b5563;
    font-size: 0.9375rem;
  }

  /* Request Status */
  .request-status {
    padding: 0.25rem 0.75rem;
    border-radius: 12px;
    font-size: 0.75rem;
    font-weight: bold;
    text-transform: uppercase;
  }

  .request-status.pending {
    background: #fef3c7;
    color: #92400e;
  }

  .request-status.dispatched {
    background: #dbeafe;
    color: #1e40af;
  }

  .request-status.completed {
    background: #d1fae5;
    color: #065f46;
  }

  .request-status.in_progress {
    background: #e0e7ff;
    color: #3730a3;
  }

  /* Activity Item */
  .activity-item {
    padding: 1rem;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-bottom: 0.5rem;
  }

  .activity-item:hover {
    background: #f9fafb;
  }

  /* Ambulance Card */
  .ambulance-card {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
  }

  .ambulance-card.available {
    border-top: 4px solid #10b981;
  }

  .ambulance-card.dispatched {
    border-top: 4px solid #f59e0b;
  }

  .ambulance-card.maintenance {
    border-top: 4px solid #6b7280;
  }

  /* Patient Card */
  .patient-card {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
  }

  /* History Item */
  .history-item {
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
  }

  .history-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
  }
`;

document.head.appendChild(adminStyle);

// Initialize admin dashboard
function initializeAdminDashboard() {
  console.log('Initializing admin dashboard...');

  // Check if we're on the admin dashboard
  const adminDashboard = document.getElementById('admin-dashboard');
  if (!adminDashboard) {
    console.log('Not on admin dashboard, skipping initialization');
    return;
  }

  // Initialize the dashboard
  AdminState.isInitialized = true;
  console.log('Admin dashboard initialized');

  // Load initial data
  loadDashboardData();

  // Set up event listeners
  setupAdminEventListeners();
}

function setupAdminEventListeners() {
  // Add any global event listeners here
  document.addEventListener('click', (e) => {
    // Handle navigation clicks
    if (e.target.matches('[data-section]')) {
      e.preventDefault();
      const sectionId = e.target.getAttribute('data-section');
      showAdminSection(sectionId, e);
    }
  });
}

// Make sure all functions are available globally
// Core functions
function showAdminSection(sectionId, event) {
  console.log(`Switching to admin section: ${sectionId}`);

  try {
    // Update navigation
    const navButtons = document.querySelectorAll('.nav-btn');
    if (navButtons.length > 0) {
      navButtons.forEach(btn => btn.classList.remove('active'));

      // Find and activate the clicked button
      const clickedButton = event ? event.currentTarget :
        document.querySelector(`[onclick*="${sectionId}"]`);
      if (clickedButton) {
        clickedButton.classList.add('active');
      }
    }

    // Hide all sections
    const sections = document.querySelectorAll('.admin-section');
    sections.forEach(section => section.classList.remove('active'));

    // Show target section
    const targetSection = document.getElementById(`${sectionId}-section`);
    if (targetSection) {
      targetSection.classList.add('active');
      AdminState.currentSection = sectionId;

      // Load section data
      const loadFunction = {
        'dashboard': loadDashboardData,
        'dispatch': loadDispatchData,
        'ambulances': loadAmbulancesData,
        'patients': loadPatientsData,
        'history': loadHistoryData
      }[sectionId];

      if (loadFunction) {
        loadFunction().catch(error => {
          console.error(`Error loading ${sectionId} data:`, error);
          showNotification(`Error loading ${sectionId} data: ${error.message}`, 'error');
        });
      }
    }
  } catch (error) {
    console.error('Error in showAdminSection:', error);
    showNotification('Error changing section', 'error');
  }
}

/**
 * Initialize the admin dashboard
 */
async function initializeAdminDashboard() {
  if (AdminState.isInitialized) {
    console.log('Admin dashboard already initialized');
    return;
  }

  console.log('Initializing admin dashboard...');

  try {
    // Show dashboard by default
    showAdminSection('dashboard');

    // Set up auto-refresh
    setInterval(() => {
      const refreshFunction = {
        'dashboard': loadDashboardData,
        'dispatch': loadDispatchData,
        'ambulances': loadAmbulancesData,
        'patients': loadPatientsData,
        'history': loadHistoryData
      }[AdminState.currentSection];

      if (refreshFunction) {
        refreshFunction().catch(console.error);
      }
    }, 30000); // Refresh every 30 seconds

    AdminState.isInitialized = true;
    console.log('Admin dashboard initialized successfully');
  } catch (error) {
    console.error('Error initializing admin dashboard:', error);
    showNotification('Error initializing dashboard', 'error');
  }
}

// Data retrieval functions that work with localStorage
function getLocalStorageRequests() {
  const stored = localStorage.getItem('ambulance_requests') || localStorage.getItem('ambulanceRequests')
  return stored ? JSON.parse(stored) : []
}

function getLocalStorageAmbulances() {
  const stored = localStorage.getItem('ambulance_ambulances') || localStorage.getItem('ambulances')
  if (stored) {
    return JSON.parse(stored)
  }

  // If no ambulances in localStorage, create sample data
  const sampleAmbulances = [
    { id: 1, currentLocation: 'Central Station', availability: 'AVAILABLE' },
    { id: 2, currentLocation: 'North Station', availability: 'AVAILABLE' },
    { id: 3, currentLocation: 'South Station', availability: 'MAINTENANCE' },
    { id: 4, currentLocation: 'East Station', availability: 'AVAILABLE' },
    { id: 5, currentLocation: 'West Station', availability: 'DISPATCHED' }
  ]

  localStorage.setItem('ambulance_ambulances', JSON.stringify(sampleAmbulances))
  return sampleAmbulances
}

function saveLocalStorageAmbulances(ambulances) {
  localStorage.setItem('ambulance_ambulances', JSON.stringify(ambulances))
}

function getLocalStoragePatients() {
  const stored = localStorage.getItem('ambulance_patients') || localStorage.getItem('patients')
  return stored ? JSON.parse(stored) : []
}

// Enhanced data loading with both API and localStorage fallback
async function getDataWithFallback(apiEndpoint, localStorageGetter) {
  try {
    // Try API first
    const data = await apiCall(apiEndpoint)
    console.log(`Loaded ${data.length} items from API: ${apiEndpoint}`)
    return data
  } catch (error) {
    console.warn(`API failed for ${apiEndpoint}, using localStorage:`, error.message)
    // Fallback to localStorage
    return localStorageGetter()
  }
}

// Dashboard functions with proper error handling
async function loadDashboardData() {
  console.log('Loading dashboard data...')
  try {
    const [requests, ambulances] = await Promise.all([
      getDataWithFallback('/requests', getLocalStorageRequests),
      getDataWithFallback('/ambulances', getLocalStorageAmbulances)
    ])

    console.log('Dashboard data loaded:', {
      requests: requests.length,
      ambulances: ambulances.length
    })

    // Update statistics
    const activeRequests = requests.filter(r =>
      r.status === 'PENDING' || r.status === 'DISPATCHED' || r.status === 'IN_PROGRESS'
    ).length

    const availableAmbulances = ambulances.filter(a =>
      a.availability === 'AVAILABLE' || a.status === 'available'
    ).length

    const todayRequests = requests.filter(r => {
      if (!r.requestTime) return false
      const requestDate = new Date(r.requestTime)
      const today = new Date()
      return requestDate.toDateString() === today.toDateString()
    }).length

    // Calculate average response time
    const completedRequests = requests.filter(r =>
      r.status === 'COMPLETED' && r.dispatchTime
    )
    const avgResponseTime = completedRequests.length > 0
      ? Math.round(
          completedRequests.reduce((sum, r) => {
            const requestTime = new Date(r.requestTime)
            const dispatchTime = new Date(r.dispatchTime)
            return sum + (dispatchTime - requestTime) / (1000 * 60) // minutes
          }, 0) / completedRequests.length
        )
      : 0

    // Update DOM elements safely
    updateElementSafe("active-requests", activeRequests)
    updateElementSafe("available-ambulances", availableAmbulances)
    updateElementSafe("avg-response-time", avgResponseTime)
    updateElementSafe("total-requests", todayRequests)

    // Load recent activity
    loadRecentActivity(requests)

  } catch (error) {
    console.error('Error loading dashboard data:', error)
    showNotification('Error loading dashboard data: ' + error.message, 'error')

    // Set default values
    updateElementSafe("active-requests", 0)
    updateElementSafe("available-ambulances", 0)
    updateElementSafe("avg-response-time", 0)
    updateElementSafe("total-requests", 0)
  }
}

// History functions
async function loadHistoryData() {
  console.log('Loading history data...')
  try {
    // Use requests data for history since service history might not be available
    const requests = await getDataWithFallback('/requests', getLocalStorageRequests)
    const completedRequests = requests.filter(r => r.status === 'COMPLETED')
    displayHistoryFromRequests(completedRequests)
  } catch (error) {
    console.error('Error loading history:', error)
    showNotification('Error loading service history: ' + error.message, 'error')
  }
}

function displayHistoryFromRequests(requests) {
  const container = document.getElementById("history-list")
  if (!container) {
    console.warn('History list container not found')
    return
  }

  container.innerHTML = ""

  if (requests.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No completed requests found</p>'
    return
  }

  // Sort by request time (newest first)
  const sortedRequests = requests
    .filter(r => r.requestTime)
    .sort((a, b) => new Date(b.requestTime) - new Date(a.requestTime))

  sortedRequests.forEach((request) => {
    const item = createHistoryItemFromRequest(request)
    container.appendChild(item)
  })
}

function createHistoryItemFromRequest(request) {
  const item = document.createElement("div")
  item.className = "history-item"
  item.style.cssText = `
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
  `

  const responseTime = request.dispatchTime
    ? Math.round((new Date(request.dispatchTime) - new Date(request.requestTime)) / (1000 * 60))
    : 'N/A'

  item.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
      <div>
        <strong>Request #${request.id}</strong> - ${request.userName || request.patientName || 'Unknown'}
        <br>
        <span style="color: #6b7280; font-size: 0.875rem;">${request.emergencyDescription || request.emergencyType || 'No description'}</span>
      </div>
      <span style="padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: bold; background: #d1fae5; color: #065f46;">
        ${request.status || 'Completed'}
      </span>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
      <div><strong>Request Time:</strong> ${formatDateTime(request.requestTime)}</div>
      <div><strong>Response Time:</strong> ${responseTime} min</div>
      <div><strong>Location:</strong> ${request.location || request.address || 'N/A'}</div>
      <div><strong>Ambulance:</strong> ${request.ambulanceId || 'N/A'}</div>
    </div>

    <div style="font-size: 0.875rem; color: #6b7280;">
      ${request.emergencyDescription || request.description || 'No description available'}
    </div>
  `

  return item
}

function filterHistory() {
  const dateFrom = document.getElementById("date-from")
  const dateTo = document.getElementById("date-to")
  const status = document.getElementById("history-status")

  if (!dateFrom || !dateTo || !status) {
    console.warn('History filter elements not found')
    return
  }

  getDataWithFallback('/requests', getLocalStorageRequests).then(requests => {
    let filtered = requests.filter(r => r.status === 'COMPLETED')

    if (dateFrom.value) {
      filtered = filtered.filter(r => new Date(r.requestTime) >= new Date(dateFrom.value))
    }
    if (dateTo.value) {
      filtered = filtered.filter(r => new Date(r.requestTime) <= new Date(dateTo.value + 'T23:59:59'))
    }

    displayHistoryFromRequests(filtered)
  }).catch(error => {
    showNotification('Error filtering history: ' + error.message, 'error')
  })
}

// Utility functions
function showNotification(message, type = "info") {
  console.log(`Notification: ${message} (${type})`)

  const notification = document.createElement("div")
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 1rem 1.5rem;
    border-radius: 8px;
    color: white;
    font-weight: 500;
    z-index: 1001;
    animation: slideIn 0.3s ease-out;
    max-width: 400px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  `

  switch (type) {
    case "success":
      notification.style.background = "#10b981"
      break
    case "error":
      notification.style.background = "#ef4444"
      break
    case "warning":
      notification.style.background = "#f59e0b"
      break
    default:
      notification.style.background = "#3b82f6"
  }

  notification.textContent = message
  document.body.appendChild(notification)

  setTimeout(() => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  }, 5000)

  // Click to dismiss
  notification.addEventListener('click', () => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  })
}

function viewRequestDetails(requestId) {
  getDataWithFallback('/requests', getLocalStorageRequests).then(requests => {
    const request = requests.find(r => r.id == requestId)
    if (request) {
      const detailsModal = createRequestDetailsModal(request)
      document.body.appendChild(detailsModal)
      detailsModal.classList.add('active')
    } else {
      showNotification('Request not found', 'error')
    }
  }).catch(error => {
    console.error('Error loading request details:', error)
    showNotification('Error loading request details: ' + error.message, 'error')
  })
}

function createRequestDetailsModal(request) {
  const modal = document.createElement('div')
  modal.className = 'modal'
  modal.style.cssText = `
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  `

  modal.innerHTML = `
    <div style="background: white; border-radius: 12px; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1); max-width: 600px; width: 90%; max-height: 90vh; overflow-y: auto;">
      <div style="padding: 1.5rem 1.5rem 0; display: flex; justify-content: space-between; align-items: center;">
        <h3 style="font-size: 1.25rem; font-weight: 600; color: #111827;">Request Details - #${request.id}</h3>
        <button onclick="this.closest('.modal').remove()" style="background: none; border: none; font-size: 1.5rem; color: #6b7280; cursor: pointer; padding: 0; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;">×</button>
      </div>
      <div style="padding: 1.5rem;">
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
            <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Status</strong>
            <span style="color: #111827; font-weight: 500;">${request.status || 'Pending'}</span>
          </div>
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
            <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Patient</strong>
            <span style="color: #111827; font-weight: 500;">${request.userName || request.patientName || 'Unknown'}</span>
          </div>
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
            <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Contact</strong>
            <span style="color: #111827; font-weight: 500;">${request.userContact || request.phone || 'N/A'}</span>
          </div>
          <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
            <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Request Time</strong>
            <span style="color: #111827; font-weight: 500;">${formatDateTime(request.requestTime)}</span>
          </div>
          ${request.dispatchTime ? `
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Dispatch Time</strong>
              <span style="color: #111827; font-weight: 500;">${formatDateTime(request.dispatchTime)}</span>
            </div>
          ` : ''}
          ${request.ambulanceId ? `
            <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
              <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Assigned Ambulance</strong>
              <span style="color: #111827; font-weight: 500;">#${request.ambulanceId}</span>
            </div>
          ` : ''}
        </div>

        <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444; margin-bottom: 1rem;">
          <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Location</strong>
          <span style="color: #111827; font-weight: 500;">${request.location || request.address || 'N/A'}</span>
        </div>

        <div style="padding: 0.75rem; background: #f9fafb; border-radius: 6px; border-left: 3px solid #ef4444;">
          <strong style="display: block; color: #374151; font-size: 0.875rem; margin-bottom: 0.25rem;">Emergency Description</strong>
          <span style="color: #111827; font-weight: 500;">${request.emergencyDescription || request.emergencyType || 'N/A'}</span>
        </div>
      </div>
      <div style="padding: 0 1.5rem 1.5rem; display: flex; gap: 1rem; justify-content: flex-end;">
        <button onclick="this.closest('.modal').remove()" style="background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">Close</button>
      </div>
    </div>
  `

  return modal
}

// Enhanced utility functions
function formatDateTime(dateString) {
  if (!dateString) return 'N/A'
  try {
    const date = new Date(dateString)
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  } catch (error) {
    return 'Invalid Date'
  }
}

function formatTime(dateString) {
  if (!dateString) return 'N/A'
  try {
    const date = new Date(dateString)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } catch (error) {
    return 'Invalid Time'
  }
}

// Safe API call wrapper that handles both success and failure
async function apiCall(endpoint, options = {}) {
  const url = `/api${endpoint}`
  const token = localStorage.getItem('token')

  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    },
    ...options
  }

  try {
    const response = await fetch(url, config)

    if (response.status === 401 || response.status === 403) {
      // Token expired or invalid
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      throw new Error('Authentication required')
    }

    if (!response.ok) {
      const errorText = await response.text()
      let errorData = {}
      try {
        errorData = JSON.parse(errorText)
      } catch (e) {
        errorData = { error: errorText || `HTTP ${response.status}` }
      }
      throw new Error(errorData.error || errorData.message || `HTTP ${response.status}`)
    }

    return await response.json()
  } catch (error) {
    console.error(`API call failed: ${endpoint}`, error)
    throw error
  }
}

// Add required CSS for animations and styling
// const adminStyle = document.createElement('style')
// adminStyle.textContent = `
//   @keyframes slideIn {
//     from {
//       transform: translateX(100%);
//       opacity: 0;
//     }
//     to {
//       transform: translateX(0);
//       opacity: 1;
//     }
//   }

//   .modal.active {
//     display: flex !important;
//   }

//   .request-status {
//     padding: 0.25rem 0.75rem;
//     border-radius: 12px;
//     font-size: 0.75rem;
//     font-weight: bold;
//     text-transform: uppercase;
//   }

//   .request-status.pending {
//     background: #fef3c7;
//     color: #92400e;
//   }

//   .request-status.dispatched {
//     background: #dbeafe;
//     color: #1e40af;
//   }

//   .request-status.completed {
//     background: #d1fae5;
//     color: #065f46;
//   }

//   .request-status.in_progress {
//     background: #e0e7ff;
//     color: #3730a3;
//   }

//   .btn {
//     display: inline-flex;
//     align-items: center;
//     justify-content: center;
//     gap: 0.5rem;
//     padding: 0.5rem 1rem;
//     border: none;
//     border-radius: 4px;
//     font-size: 0.875rem;
//     font-weight: 600;
//     text-decoration: none;
//     cursor: pointer;
//     transition: all 0.2s ease-in-out;
//     white-space: nowrap;
//   }

//   .btn:hover {
//     transform: translateY(-1px);
//     box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
//   }

//   .btn-primary {
//     background: #3b82f6;
//     color: white;
//   }

//   .btn-primary:hover {
//     background: #2563eb;
//   }

//   .btn-success {
//     background: #10b981;
//     color: white;
//   }

//   .btn-success:hover {
//     background: #059669;
//   }

//   .btn-secondary {
//     background: #6b7280;
//     color: white;
//   }

//   .btn-secondary:hover {
//     background: #4b5563;
//   }

//   .activity-item:hover {
//     background: #f9fafb;
//   }

//   .ambulance-card:hover,
//   .request-card:hover,
//   .patient-card:hover,
//   .history-item:hover {
//     transform: translateY(-2px);
//     box-shadow: 0 8px 15px rgba(0, 0, 0, 0.1);
//   }
// `

// document.head.appendChild(adminStyle)

// Add function check to ensure all functions are properly defined
console.log('=== Function Check ===');

// Check if all required functions exist
const requiredFunctions = [
  'initializeAdminDashboard',
  'showAdminSection',
  'loadDashboardData',
  'loadDispatchData',
  'loadAmbulancesData',
  'loadPatientsData',
  'loadHistoryData',
  'dispatchRequest',
  'updateRequestStatus',
  'updateAmbulanceStatus',
  'addAmbulance',
  'editAmbulance',
  'showAddAmbulanceModal',
  'closeAddAmbulanceModal',
  'refreshRequests',
  'filterRequests',
  'filterHistory',
  'searchPatients',
  'viewRequestDetails'
];

const missingFunctions = [];
const availableFunctions = [];

requiredFunctions.forEach(funcName => {
  if (typeof window[funcName] === 'function') {
    availableFunctions.push(funcName);
  } else {
    missingFunctions.push(funcName);
  }
});

console.log('✅ Available functions:', availableFunctions);
if (missingFunctions.length > 0) {
  console.error('❌ Missing functions:', missingFunctions);
} else {
  console.log('🎉 All admin functions are properly defined!');
}

// Test data loading functions
console.log('=== Data Check ===');
try {
  const ambulances = getLocalStorageAmbulances();
  const requests = getLocalStorageRequests();
  const patients = getLocalStoragePatients();

  console.log(`📊 Data available: ${ambulances.length} ambulances, ${requests.length} requests, ${patients.length} patients`);
} catch (error) {
  console.error('❌ Data loading error:', error);
}

console.log('=== Admin.js loaded successfully ===');

// Safe DOM update
function updateElementSafe(elementId, value) {
  const element = document.getElementById(elementId)
  if (element) {
    element.textContent = value
    console.log(`Updated ${elementId}: ${value}`)
  } else {
    console.warn(`Element with id '${elementId}' not found`)
  }
}

function loadRecentActivity(requests) {
  const recentRequests = requests
    .filter(r => r.requestTime) // Only requests with valid time
    .sort((a, b) => new Date(b.requestTime) - new Date(a.requestTime))
    .slice(0, 5)

  const container = document.getElementById("recent-requests")
  if (!container) {
    console.warn('Recent requests container not found')
    return
  }

  container.innerHTML = ""

  if (recentRequests.length === 0) {
    container.innerHTML = '<p style="color: #6b7280; padding: 1rem;">No recent activity</p>'
    return
  }

  recentRequests.forEach((request) => {
    const item = document.createElement("div")
    item.className = "activity-item"
    item.innerHTML = `
      <div style="display: flex; justify-content: space-between; align-items: center; padding: 1rem; border: 1px solid #e5e7eb; border-radius: 8px; margin-bottom: 0.5rem;">
        <div>
          <strong>#${request.id}</strong> - ${request.userName || request.callerName || 'Unknown'}
          <br>
          <small style="color: #6b7280;">${request.location || request.address || 'No location'}</small>
        </div>
        <div style="text-align: right;">
          <span class="request-status ${(request.status || 'pending').toLowerCase()}">${request.status || 'Pending'}</span>
          <br>
          <small style="color: #6b7280;">${formatTime(request.requestTime)}</small>
        </div>
      </div>
    `
    container.appendChild(item)
  })
}

// Dispatch functions
async function loadDispatchData() {
  console.log('Loading dispatch data...')
  try {
    const requests = await getDataWithFallback('/requests', getLocalStorageRequests)
    displayRequests(requests)
  } catch (error) {
    console.error('Error loading dispatch data:', error)
    showNotification('Error loading requests: ' + error.message, 'error')
  }
}

function displayRequests(requests) {
  const container = document.getElementById("requests-list")
  if (!container) {
    console.warn('Requests list container not found')
    return
  }

  container.innerHTML = ""

  if (requests.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No requests found</p>'
    return
  }

  // Sort by request time (newest first)
  const sortedRequests = requests
    .filter(r => r.requestTime) // Filter out invalid requests
    .sort((a, b) => new Date(b.requestTime) - new Date(a.requestTime))

  console.log(`Displaying ${sortedRequests.length} requests`)

  sortedRequests.forEach((request) => {
    const card = createRequestCard(request)
    container.appendChild(card)
  })
}

function createRequestCard(request) {
  const card = document.createElement("div")

  // Normalize status field
  const status = request.status || 'PENDING'

  card.className = `request-card ${status.toLowerCase()}`
  card.style.cssText = `
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
    border-left: 4px solid ${status === 'PENDING' ? '#3b82f6' : status === 'DISPATCHED' ? '#f59e0b' : '#6b7280'};
  `

  // Calculate response time if available
  const responseTime = request.dispatchTime
    ? Math.round((new Date(request.dispatchTime) - new Date(request.requestTime)) / (1000 * 60))
    : null

  card.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
      <div style="font-weight: bold; font-size: 1.1rem;">#${request.id}</div>
      <span style="padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: bold; background: ${status === 'PENDING' ? '#dbeafe' : status === 'DISPATCHED' ? '#fef3c7' : '#f3f4f6'}; color: ${status === 'PENDING' ? '#1e40af' : status === 'DISPATCHED' ? '#92400e' : '#374151'};">
        ${status}
      </span>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
      <div><strong>Patient:</strong> ${request.userName || request.patientName || 'Unknown'}</div>
      <div><strong>Contact:</strong> ${request.userContact || request.phone || 'N/A'}</div>
      <div><strong>Location:</strong> ${request.location || request.address || 'N/A'}</div>
      <div><strong>Emergency:</strong> ${request.emergencyDescription || request.emergencyType || 'N/A'}</div>
      <div><strong>Request Time:</strong> ${formatDateTime(request.requestTime)}</div>
      ${request.ambulanceId ? `<div><strong>Ambulance:</strong> #${request.ambulanceId}</div>` : ''}
      ${responseTime ? `<div><strong>Response Time:</strong> ${responseTime} minutes</div>` : ''}
    </div>

    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
      ${status === 'PENDING' ? `
        <button class="btn btn-primary" onclick="dispatchRequest(${request.id})" style="background: #3b82f6; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Dispatch Ambulance
        </button>
      ` : ''}

      ${status === 'DISPATCHED' ? `
        <button class="btn btn-success" onclick="updateRequestStatus(${request.id}, 'IN_PROGRESS')" style="background: #10b981; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer; margin-right: 0.5rem;">
          Mark Arrived
        </button>
        <button class="btn btn-success" onclick="updateRequestStatus(${request.id}, 'COMPLETED')" style="background: #10b981; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Mark Complete
        </button>
      ` : ''}

      ${status === 'IN_PROGRESS' ? `
        <button class="btn btn-success" onclick="updateRequestStatus(${request.id}, 'COMPLETED')" style="background: #10b981; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Mark Complete
        </button>
      ` : ''}

      <button class="btn btn-secondary" onclick="viewRequestDetails(${request.id})" style="background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
        View Details
      </button>
    </div>
  `

  return card
}

async function dispatchRequest(requestId) {
  if (!confirm('Dispatch an ambulance for this request?')) {
    return
  }

  console.log(`Dispatching ambulance for request ${requestId}`)

  try {
    // Try API first
    try {
      await apiCall(`/dispatch/${requestId}`, { method: 'POST' })
      showNotification('Ambulance dispatched successfully!', 'success')
    } catch (apiError) {
      console.warn('API dispatch failed, updating locally:', apiError.message)

      // Fallback to local dispatch
      const requests = getLocalStorageRequests()
      const ambulances = getLocalStorageAmbulances()

      const request = requests.find(r => r.id == requestId)
      const availableAmbulance = ambulances.find(a =>
        a.availability === 'AVAILABLE' || a.status === 'available'
      )

      if (request && availableAmbulance) {
        request.status = 'DISPATCHED'
        request.ambulanceId = availableAmbulance.id
        request.dispatchTime = new Date().toISOString()

        availableAmbulance.availability = 'DISPATCHED'
        availableAmbulance.status = 'dispatched'

        localStorage.setItem('ambulance_requests', JSON.stringify(requests))
        saveLocalStorageAmbulances(ambulances)

        showNotification('Ambulance dispatched successfully (locally)!', 'success')
      } else {
        throw new Error('No available ambulance found')
      }
    }

    loadDispatchData() // Refresh the dispatch view

    // Also refresh dashboard if it's the current section
    if (AdminState.currentSection === 'dashboard') {
      loadDashboardData()
    }
  } catch (error) {
    console.error('Error dispatching ambulance:', error)
    showNotification(`Error dispatching ambulance: ${error.message}`, 'error')
  }
}

async function updateRequestStatus(requestId, newStatus) {
  try {
    // Try API first
    try {
      await apiCall(`/requests/${requestId}/status?status=${newStatus}`, {
        method: 'PUT'
      })
      showNotification(`Request status updated to ${newStatus}`, 'success')
    } catch (apiError) {
      console.warn('API status update failed, updating locally:', apiError.message)

      // Fallback to local update
      const requests = getLocalStorageRequests()
      const request = requests.find(r => r.id == requestId)

      if (request) {
        request.status = newStatus
        if (newStatus === 'COMPLETED' && request.ambulanceId) {
          // Free up the ambulance
          const ambulances = getLocalStorageAmbulances()
          const ambulance = ambulances.find(a => a.id == request.ambulanceId)
          if (ambulance) {
            ambulance.availability = 'AVAILABLE'
            ambulance.status = 'available'
            saveLocalStorageAmbulances(ambulances)
          }
        }

        localStorage.setItem('ambulance_requests', JSON.stringify(requests))
        showNotification(`Request status updated to ${newStatus} (locally)`, 'success')
      }
    }

    loadDispatchData() // Refresh the dispatch view

    // Also refresh dashboard if it's the current section
    if (AdminState.currentSection === 'dashboard') {
      loadDashboardData()
    }
  } catch (error) {
    console.error('Error updating request status:', error)
    showNotification(`Error updating status: ${error.message}`, 'error')
  }
}

function refreshRequests() {
  loadDispatchData()
  showNotification("Requests refreshed", "info")
}

function filterRequests() {
  const filterSelect = document.getElementById("filter-status")
  if (!filterSelect) return

  const filterValue = filterSelect.value

  if (filterValue === "all") {
    loadDispatchData()
  } else {
    getDataWithFallback('/requests', getLocalStorageRequests).then(requests => {
      const filtered = requests.filter(r => (r.status || 'PENDING') === filterValue)
      displayRequests(filtered)
    }).catch(error => {
      console.error('Error filtering requests:', error)
      showNotification('Error filtering requests: ' + error.message, 'error')
    })
  }
}

// Ambulances functions
async function loadAmbulancesData() {
  console.log('Loading ambulances data...')
  try {
    const ambulances = await getDataWithFallback('/ambulances', getLocalStorageAmbulances)
    console.log('Loaded ambulances:', ambulances)
    displayAmbulances(ambulances)
  } catch (error) {
    console.error('Error loading ambulances:', error)
    showNotification('Error loading ambulances: ' + error.message, 'error')
  }
}

function displayAmbulances(ambulances) {
  const container = document.getElementById("ambulances-grid")
  if (!container) {
    console.warn('Ambulances grid container not found')
    return
  }

  container.innerHTML = ""

  if (ambulances.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No ambulances found</p>'
    return
  }

  console.log(`Displaying ${ambulances.length} ambulances`)

  ambulances.forEach((ambulance) => {
    const card = createAmbulanceCard(ambulance)
    container.appendChild(card)
  })
}

function createAmbulanceCard(ambulance) {
  const card = document.createElement("div")

  // Normalize status field
  const status = ambulance.availability || ambulance.status || 'UNKNOWN'

  card.className = `ambulance-card ${status.toLowerCase()}`
  card.style.cssText = `
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
    border-top: 4px solid ${status === 'AVAILABLE' ? '#10b981' : status === 'DISPATCHED' ? '#f59e0b' : '#6b7280'};
  `

  const statusDisplayMap = {
    'AVAILABLE': 'Available',
    'DISPATCHED': 'Dispatched',
    'MAINTENANCE': 'Maintenance',
    'OUT_OF_SERVICE': 'Out of Service',
    'available': 'Available',
    'dispatched': 'Dispatched',
    'maintenance': 'Maintenance'
  }

  card.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
      <div style="font-weight: bold; font-size: 1.1rem;">Ambulance #${ambulance.id}</div>
      <span style="padding: 0.25rem 0.75rem; border-radius: 12px; font-size: 0.75rem; font-weight: bold; background: ${status === 'AVAILABLE' ? '#d1fae5' : status === 'DISPATCHED' ? '#fef3c7' : '#f3f4f6'}; color: ${status === 'AVAILABLE' ? '#065f46' : status === 'DISPATCHED' ? '#92400e' : '#374151'};">
        ${statusDisplayMap[status] || status}
      </span>
    </div>

    <div style="margin-bottom: 1rem;">
      <strong>Location:</strong> ${ambulance.currentLocation || ambulance.location || 'Unknown'}
    </div>

    <div style="display: flex; gap: 0.5rem; flex-wrap: wrap;">
      ${status === 'AVAILABLE' || status === 'available' ? `
        <button onclick="updateAmbulanceStatus(${ambulance.id}, 'MAINTENANCE')" style="background: #f59e0b; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Maintenance
        </button>
      ` : ''}

      ${status === 'MAINTENANCE' || status === 'maintenance' ? `
        <button onclick="updateAmbulanceStatus(${ambulance.id}, 'AVAILABLE')" style="background: #10b981; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Mark Available
        </button>
      ` : ''}

      ${status === 'DISPATCHED' || status === 'dispatched' ? `
        <button onclick="updateAmbulanceStatus(${ambulance.id}, 'AVAILABLE')" style="background: #10b981; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
          Mark Available
        </button>
      ` : ''}

      <button onclick="editAmbulance(${ambulance.id})" style="background: #6b7280; color: white; padding: 0.5rem 1rem; border: none; border-radius: 4px; cursor: pointer;">
        Edit Location
      </button>
    </div>
  `

  return card
}

async function updateAmbulanceStatus(ambulanceId, newStatus) {
  try {
    // Try API first
    try {
      await apiCall(`/ambulances/${ambulanceId}/status?status=${newStatus}`, {
        method: 'PUT'
      })
      showNotification(`Ambulance status updated to ${newStatus}`, 'success')
    } catch (apiError) {
      console.warn('API ambulance update failed, updating locally:', apiError.message)

      // Fallback to local update
      const ambulances = getLocalStorageAmbulances()
      const ambulance = ambulances.find(a => a.id == ambulanceId)

      if (ambulance) {
        ambulance.availability = newStatus
        ambulance.status = newStatus.toLowerCase()
        saveLocalStorageAmbulances(ambulances)
        showNotification(`Ambulance status updated to ${newStatus} (locally)`, 'success')
      }
    }

    loadAmbulancesData()

    // Also refresh dashboard
    if (AdminState.currentSection === 'dashboard') {
      loadDashboardData()
    }
  } catch (error) {
    console.error('Error updating ambulance status:', error)
    showNotification(`Error updating ambulance: ${error.message}`, 'error')
  }
}

function editAmbulance(ambulanceId) {
  const newLocation = prompt("Enter new location for the ambulance:")
  if (newLocation && newLocation.trim()) {
    // Try API first, then fallback to local
    apiCall(`/ambulances/${ambulanceId}`, {
      method: 'PUT',
      body: JSON.stringify({ currentLocation: newLocation.trim() })
    }).then(() => {
      showNotification('Ambulance location updated!', 'success')
      loadAmbulancesData()
    }).catch(error => {
      console.warn('API location update failed, updating locally:', error.message)

      // Fallback to local update
      const ambulances = getLocalStorageAmbulances()
      const ambulance = ambulances.find(a => a.id == ambulanceId)

      if (ambulance) {
        ambulance.currentLocation = newLocation.trim()
        ambulance.location = newLocation.trim()
        saveLocalStorageAmbulances(ambulances)
        showNotification('Ambulance location updated (locally)!', 'success')
        loadAmbulancesData()
      }
    })
  }
}

function showAddAmbulanceModal() {
  const modal = document.getElementById("add-ambulance-modal")
  if (modal) {
    modal.classList.add("active")
  }
}

function closeAddAmbulanceModal() {
  const modal = document.getElementById("add-ambulance-modal")
  if (modal) {
    modal.classList.remove("active")
  }
  const form = document.getElementById("add-ambulance-form")
  if (form) {
    form.reset()
  }
}

async function addAmbulance() {
  const form = document.getElementById("add-ambulance-form")
  if (!form) return

  const formData = new FormData(form)

  const newAmbulance = {
    currentLocation: formData.get("location"),
    availability: formData.get("status"),
    id: Date.now() // Generate new ID
  }

  try {
    // Try API first
    try {
      await apiCall('/ambulances', {
        method: 'POST',
        body: JSON.stringify(newAmbulance)
      })
      showNotification('Ambulance added successfully!', 'success')
    } catch (apiError) {
      console.warn('API add ambulance failed, adding locally:', apiError.message)

      // Fallback to local addition
      const ambulances = getLocalStorageAmbulances()
      ambulances.push(newAmbulance)
      saveLocalStorageAmbulances(ambulances)
      showNotification('Ambulance added successfully (locally)!', 'success')
    }

    closeAddAmbulanceModal()
    loadAmbulancesData()
  } catch (error) {
    console.error('Error adding ambulance:', error)
    showNotification(`Error adding ambulance: ${error.message}`, 'error')
  }
}

// Patients functions
async function loadPatientsData() {
  console.log('Loading patients data...')
  try {
    const patients = await getDataWithFallback('/patients', getLocalStoragePatients)
    displayPatients(patients)
  } catch (error) {
    console.error('Error loading patients:', error)
    showNotification('Error loading patients: ' + error.message, 'error')
  }
}

function displayPatients(patients) {
  const container = document.getElementById("patients-list")
  if (!container) {
    console.warn('Patients list container not found')
    return
  }

  container.innerHTML = ""

  if (patients.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No patient records found</p>'
    return
  }

  patients.forEach((patient) => {
    const card = createPatientCard(patient)
    container.appendChild(card)
  })
}

function createPatientCard(patient) {
  const card = document.createElement("div")
  card.className = "patient-card"
  card.style.cssText = `
    background: white;
    padding: 1.5rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    margin-bottom: 1rem;
  `

  card.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
      <div style="font-weight: bold; font-size: 1.1rem;">${patient.name || 'Unknown'}</div>
      <div style="color: #6b7280; font-size: 0.875rem;">
        ID: ${patient.id}
      </div>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;">
      <div><strong>Contact:</strong> ${patient.contact || 'N/A'}</div>
      <div><strong>Created:</strong> ${formatDateTime(patient.createdAt)}</div>
      <div><strong>Updated:</strong> ${formatDateTime(patient.updatedAt)}</div>
    </div>

    ${patient.medicalNotes ? `
      <div style="margin-top: 1rem; padding: 1rem; background: #f9fafb; border-radius: 6px;">
        <strong>Medical Notes:</strong> ${patient.medicalNotes}
      </div>
    ` : ''}
  `

  return card
}

function searchPatients() {
  const searchInput = document.getElementById("patient-search")
  if (!searchInput) return

  const searchTerm = searchInput.value.toLowerCase()

  if (searchTerm.length === 0) {
    loadPatientsData()
    return
  }

  getDataWithFallback('/patients', getLocalStoragePatients).then(patients => {
    const filtered = patients.filter(patient =>
      (patient.name || '').toLowerCase().includes(searchTerm) ||
      (patient.contact || '').includes(searchTerm)
    )
    displayPatients(filtered)
  }).catch(error => {
    console.error('Error searching patients:', error)
    showNotification('Error searching patients: ' + error.message, 'error')
  })
}

// History functions
async function loadHistoryData() {
  try {
    const history = await apiCall('/service-history')
    displayHistory(history)
  } catch (error) {
    console.error('Error loading history:', error)
    // Fallback to requests if service history endpoint doesn't exist
    try {
      const requests = await apiCall('/requests')
      const completedRequests = requests.filter(r => r.status === 'COMPLETED')
      displayHistoryFromRequests(completedRequests)
    } catch (requestError) {
      console.error('Error loading request history:', requestError)
      showNotification('Error loading service history', 'error')
    }
  }
}

function displayHistory(history) {
  const container = document.getElementById("history-list")
  container.innerHTML = ""

  if (history.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No service history found</p>'
    return
  }

  // Sort by creation time (newest first)
  const sortedHistory = history.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

  sortedHistory.forEach((item) => {
    const historyItem = createHistoryItem(item)
    container.appendChild(historyItem)
  })
}

function displayHistoryFromRequests(requests) {
  const container = document.getElementById("history-list")
  container.innerHTML = ""

  if (requests.length === 0) {
    container.innerHTML = '<p style="text-align: center; color: #6b7280; padding: 2rem;">No completed requests found</p>'
    return
  }

  requests.forEach((request) => {
    const item = createHistoryItemFromRequest(request)
    container.appendChild(item)
  })
}

function createHistoryItem(historyItem) {
  const item = document.createElement("div")
  item.className = "history-item"

  item.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
      <div>
        <strong>Service #${historyItem.id}</strong>
        <br>
        <span style="color: #6b7280; font-size: 0.875rem;">
          ${formatDateTime(historyItem.createdAt)}
        </span>
      </div>
      <span class="request-status ${historyItem.status.toLowerCase()}">${historyItem.status}</span>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
      <div><strong>Patient:</strong> ${historyItem.patient?.name || 'N/A'}</div>
      <div><strong>Ambulance:</strong> ${historyItem.ambulance?.id || 'N/A'}</div>
      <div><strong>Arrival:</strong> ${formatDateTime(historyItem.arrivalTime)}</div>
      <div><strong>Completion:</strong> ${formatDateTime(historyItem.completionTime)}</div>
    </div>

    ${historyItem.notes ? `
      <div style="font-size: 0.875rem; color: #6b7280;">
        <strong>Notes:</strong> ${historyItem.notes}
      </div>
    ` : ''}
  `

  return item
}

function createHistoryItemFromRequest(request) {
  const item = document.createElement("div")
  item.className = "history-item"

  const responseTime = request.dispatchTime
    ? Math.round((new Date(request.dispatchTime) - new Date(request.requestTime)) / (1000 * 60))
    : 'N/A'

  item.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 1rem;">
      <div>
        <strong>Request #${request.id}</strong> - ${request.userName}
        <br>
        <span style="color: #6b7280; font-size: 0.875rem;">${request.emergencyDescription}</span>
      </div>
      <span class="request-status ${request.status.toLowerCase()}">${request.status}</span>
    </div>

    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1rem;">
      <div><strong>Request Time:</strong> ${formatDateTime(request.requestTime)}</div>
      <div><strong>Response Time:</strong> ${responseTime} min</div>
      <div><strong>Location:</strong> ${request.location}</div>
      <div><strong>Ambulance:</strong> ${request.ambulance?.id || 'N/A'}</div>
    </div>

    <div style="font-size: 0.875rem; color: #6b7280;">
      ${request.emergencyDescription}
    </div>
  `

  return item
}

function filterHistory() {
  const dateFrom = document.getElementById("date-from")
  const dateTo = document.getElementById("date-to")
  const status = document.getElementById("history-status")

  if (!dateFrom || !dateTo || !status) {
    console.warn('History filter elements not found')
    return
  }

  const params = new URLSearchParams()
  if (dateFrom.value) params.append('start', new Date(dateFrom.value).toISOString())
  if (dateTo.value) params.append('end', new Date(dateTo.value + 'T23:59:59').toISOString())

  let endpoint = '/service-history'
  if (dateFrom.value && dateTo.value) {
    endpoint += `/date-range?${params.toString()}`
  } else if (status.value !== 'all') {
    endpoint += `/status/${status.value}`
  }

  apiCall(endpoint).then(history => {
    displayHistory(history)
  }).catch(error => {
    console.error('Error filtering history:', error)
    // Fallback to client-side filtering if server endpoints don't exist
    apiCall('/requests').then(requests => {
      let filtered = requests.filter(r => r.status === 'COMPLETED')

      if (dateFrom.value) {
        filtered = filtered.filter(r => new Date(r.requestTime) >= new Date(dateFrom.value))
      }
      if (dateTo.value) {
        filtered = filtered.filter(r => new Date(r.requestTime) <= new Date(dateTo.value + 'T23:59:59'))
      }

      displayHistoryFromRequests(filtered)
    })
  })
}

// Utility functions
function showNotification(message, type = "info") {
  const notification = document.createElement("div")
  notification.style.cssText = `
    position: fixed;
    top: 20px;
    right: 20px;
    padding: 1rem 1.5rem;
    border-radius: 8px;
    color: white;
    font-weight: 500;
    z-index: 1001;
    animation: slideIn 0.3s ease-out;
    max-width: 400px;
  `

  switch (type) {
    case "success":
      notification.style.background = "#16a34a"
      break
    case "error":
      notification.style.background = "#dc2626"
      break
    case "warning":
      notification.style.background = "#d97706"
      break
    default:
      notification.style.background = "#2563eb"
  }

  notification.textContent = message
  document.body.appendChild(notification)

  setTimeout(() => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  }, 5000)

  // Click to dismiss
  notification.addEventListener('click', () => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  })
}

function viewRequestDetails(requestId) {
  apiCall(`/requests/${requestId}`).then(request => {
    // Create a detailed view modal or redirect to details page
    const detailsModal = createRequestDetailsModal(request)
    document.body.appendChild(detailsModal)
    detailsModal.classList.add('active')
  }).catch(error => {
    console.error('Error loading request details:', error)
    showNotification('Error loading request details', 'error')
  })
}

function createRequestDetailsModal(request) {
  const modal = document.createElement('div')
  modal.className = 'modal'
  modal.style.display = 'flex'

  modal.innerHTML = `
    <div class="modal-content" style="max-width: 600px; width: 90%;">
      <div class="modal-header">
        <h3>Request Details - #${request.id}</h3>
        <button class="modal-close" onclick="this.closest('.modal').remove()">×</button>
      </div>
      <div class="modal-body">
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; margin-bottom: 1.5rem;">
          <div class="detail-item">
            <strong>Status</strong>
            <span class="request-status ${request.status.toLowerCase()}">${request.status}</span>
          </div>
          <div class="detail-item">
            <strong>Patient</strong>
            <span>${request.userName}</span>
          </div>
          <div class="detail-item">
            <strong>Contact</strong>
            <span>${request.userContact}</span>
          </div>
          <div class="detail-item">
            <strong>Request Time</strong>
            <span>${formatDateTime(request.requestTime)}</span>
          </div>
          ${request.dispatchTime ? `
            <div class="detail-item">
              <strong>Dispatch Time</strong>
              <span>${formatDateTime(request.dispatchTime)}</span>
            </div>
          ` : ''}
          ${request.ambulance ? `
            <div class="detail-item">
              <strong>Assigned Ambulance</strong>
              <span>#${request.ambulance.id} - ${request.ambulance.currentLocation}</span>
            </div>
          ` : ''}
        </div>

        <div class="detail-item" style="margin-bottom: 1rem;">
          <strong>Location</strong>
          <span>${request.location}</span>
        </div>

        <div class="detail-item">
          <strong>Emergency Description</strong>
          <span>${request.emergencyDescription}</span>
        </div>
      </div>
      <div class="modal-actions">
        <button class="btn btn-secondary" onclick="this.closest('.modal').remove()">Close</button>
      </div>
    </div>
  `

  return modal
}

// Enhanced utility functions
function formatDateTime(dateString) {
  if (!dateString) return 'N/A'
  const date = new Date(dateString)
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

function formatTime(dateString) {
  if (!dateString) return 'N/A'
  const date = new Date(dateString)
  return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// API utility function
async function apiCall(endpoint, options = {}) {
  const url = `/api${endpoint}`
  const token = localStorage.getItem('token')

  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    },
    ...options
  }

  try {
    const response = await fetch(url, config)

    if (response.status === 401) {
      // Token expired or invalid
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
      showPage('admin-login')
      throw new Error('Authentication required')
    }

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}))
      throw new Error(errorData.error || `HTTP ${response.status}`)
    }

    return await response.json()
  } catch (error) {
    console.error(`API call failed: ${endpoint}`, error)
    throw error
  }
}

// Expose all admin functions to the window object
Object.assign(window, {
  // Core functions
  showAdminSection,
  initializeAdminDashboard,
  loadDashboardData,
  loadDispatchData,
  loadAmbulancesData,
  loadPatientsData,
  loadHistoryData,
  updateAmbulanceStatus,
  dispatchRequest,
  updateRequestStatus,
  filterRequests,
  filterHistory,
  searchPatients,
  viewRequestDetails,
  showAddAmbulanceModal,
  closeAddAmbulanceModal,
  addAmbulance,
  editAmbulance,
  formatDateTime,
  formatTime,
  showNotification,
  updateElementSafe
});

console.log('Admin functions exposed to window object');

// Initialize the admin dashboard if we're on an admin page
if (document.getElementById('admin-dashboard')) {
  initializeAdminDashboard();
}