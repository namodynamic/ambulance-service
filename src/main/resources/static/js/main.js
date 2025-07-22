// Page navigation functionality with backend integration - FIXED AUTH VERSION
let currentPage = "landing-page"
let authToken = localStorage.getItem('token')
let currentUser = localStorage.getItem('user')
let userRole = null

if (currentUser) {
  currentUser = JSON.parse(currentUser)
  userRole = currentUser.role
}

// API base URL - adjust for your deployment
const API_BASE = '/api'

function showPage(pageId) {
  console.log(`Switching to page: ${pageId}`)

  // Hide all pages
  document.querySelectorAll(".page").forEach((page) => {
    page.classList.remove("active")
  })

  // Show target page
  const targetPage = document.getElementById(pageId)
  if (targetPage) {
    targetPage.classList.add("active")
    currentPage = pageId

    // Initialize page-specific functionality
    if (pageId === "admin-dashboard") {
      // Check authentication before loading dashboard
      if (!authToken || (!userRole || (!userRole.includes('ADMIN') && !userRole.includes('DISPATCHER')))) {
        console.log('No admin access, redirecting to login')
        showPage('admin-login')
        return
      }
      console.log('Loading admin dashboard...')
      // Ensure admin.js is loaded before calling initializeAdminDashboard
      if (typeof initializeAdminDashboard === 'function') {
        initializeAdminDashboard()
      } else {
        console.error('initializeAdminDashboard function not found - check admin.js')
      }
    } else if (pageId === "admin-login") {
      // Check if already authenticated
      if (authToken && (userRole === 'ROLE_ADMIN' || userRole === 'ROLE_DISPATCHER')) {
        console.log('Already authenticated, redirecting to dashboard')
        showPage('admin-dashboard')
        return
      }
    }
      else if (pageId === "user-dashboard") {
          if (!authToken) {
            showPage('user-login');
            return;
          }
          console.log('Loading user dashboard...');
          if (typeof initializeUserDashboard === 'function') {
            initializeUserDashboard();
          }
        }
  }
}

// Enhanced API utility functions with better error handling
async function apiCall(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`
  console.log(`Making API call to: ${url}`)

  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(authToken && { 'Authorization': `Bearer ${authToken}` })
    },
    ...options
  }

  console.log('API call config:', { url, headers: config.headers, method: config.method || 'GET' })

  try {
    const response = await fetch(url, config)
    console.log(`API Response status: ${response.status}`)

    if (response.status === 401 || response.status === 403) {
      console.log('Authentication failed, clearing tokens')
      // Token expired or invalid
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      authToken = null
      currentUser = null
      userRole = null

      if (currentPage === 'admin-dashboard') {
        showPage('admin-login')
      }
      throw new Error('Authentication required')
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
           console.error('Error parsing error response:', e);
         }

         const error = new Error(errorData.message || `HTTP ${response.status}`);
         error.status = response.status;
         error.response = errorData;
         throw error;
       }

    // Handle empty responses
    if (response.status === 204) {
      return null
    }

    const data = await response.json()
    console.log(`API Response data:`, data)
    return data
  } catch (error) {
    console.error(`API call failed: ${endpoint}`, error)
    throw error
  }
}

// Form submission handlers
document.addEventListener("DOMContentLoaded", () => {
  console.log('DOM loaded, initializing app...')

  // Set app container styling
  const appContainer = document.querySelector(".app-container")
  if (appContainer) {
    appContainer.style.display = "flex"
    appContainer.style.flexDirection = "column"
    appContainer.style.minHeight = "100vh"
  }

  // Set current year
  const yearElement = document.getElementById("current-year")
  if (yearElement) {
    yearElement.textContent = new Date().getFullYear()
  }

  // Update auth variables from localStorage
  authToken = localStorage.getItem('token')
  currentUser = localStorage.getItem('user')
  if (currentUser) {
    currentUser = JSON.parse(currentUser)
    userRole = currentUser.role
  }

  // Check authentication status
  if (authToken) {
    console.log('Found existing token, verifying...')
    // Test token validity with a simple API call
    apiCall('/debug/health').then(() => {
      console.log('Token is valid')
    }).catch((error) => {
      console.log('Token invalid, clearing auth:', error.message)
      localStorage.clear()
      authToken = null
      currentUser = null
      userRole = null
    })
  }

  // Ambulance request form
  const ambulanceForm = document.getElementById("ambulance-request-form")
  if (ambulanceForm) {
    ambulanceForm.addEventListener("submit", (e) => {
      e.preventDefault()
      submitAmbulanceRequest()
    })
  }

  // Admin login form
  const adminLoginForm = document.getElementById("admin-login-form")
  if (adminLoginForm) {
    adminLoginForm.addEventListener("submit", handleAdminLogin)
  }

  // User login form
  const userLoginForm = document.getElementById("user-login-form")
  if (userLoginForm) {
    userLoginForm.addEventListener("submit", (e) => {
      e.preventDefault()
      handleUserLogin()
    })
  }

  // Register form
  const registerForm = document.getElementById("register-form")
  if (registerForm) {
    registerForm.addEventListener("submit", (e) => {
      e.preventDefault()
      handleUserRegistration()
    })
  }

  // Initialize app data
  initializeApp()
})

async function submitAmbulanceRequest() {
  const form = document.getElementById("ambulance-request-form")
  const formData = new FormData(form)
  const phoneInput = document.getElementById("phone")

  // Remove any non-digit and plus characters for validation
  const phoneNumber = phoneInput.value.replace(/[^\d+]/g, '')

  // Validate phone number format (10-15 digits, optional + at start)
  const phoneRegex = /^[+]?[0-9]{10,15}$/
  if (!phoneRegex.test(phoneNumber)) {
      showError("Please enter a valid phone number (10-15 digits, optional + at start)")
      phoneInput.focus()
      phoneInput.classList.add('error')
      return
  }

  // Update the form data with the cleaned phone number
  formData.set("phone", phoneNumber)

  // Prepare request data matching your DTO structure
  const requestData = {
    userName: formData.get("callerName"),
    userContact: formData.get("phone"),
    location: `${formData.get("address")}, ${formData.get("city")}, ${formData.get("zipcode")}`,
    emergencyDescription: `${formData.get("emergencyType")}: ${formData.get("description")}`
  }

  console.log('Submitting ambulance request:', requestData)

  try {
    showLoading("Submitting emergency request...")

    const response = await apiCall('/requests', {
      method: 'POST',
      body: JSON.stringify(requestData)
    })

    console.log("Request submitted successfully:", response)

    // Reset any error states
    phoneInput.classList.remove('error')

    // Update confirmation page with real data
    const confirmationId = document.getElementById("confirmation-request-id")
    if (confirmationId) {
      confirmationId.textContent = `#${response.id}`
    }

    // Show confirmation page
    showPage("confirmation-page")

    // Start status tracking
    if (response.id) {
      trackRequestStatus(response.id)
    }

    // Reset form
    form.reset()

  } catch (error) {
    console.error("Error submitting request:", error)

   // Check if this is a "no ambulance available" error (either 503 or 500 with specific message)
      const isNoAmbulanceError =
        error.status === 503 ||
        error.status === 500 ||
        (error.message && (
          error.message.includes('No ambulance available') ||
          error.message.includes('NoAvailableAmbulanceException') ||
          (error.response && (
            error.response.message && error.response.message.includes('No ambulance available at the moment')
          ))
        ));

      if (isNoAmbulanceError) {
        // Show a friendly message that the request is queued
        const loadingMessage =
          "All our ambulances are currently busy. " +
          "Your request has been queued and an ambulance will be assigned as soon as one becomes available. " +
          "You can close this message - we'll process your request shortly.";

        showLoading(loadingMessage);

        // Still show the confirmation page since the request was queued
        const confirmationId = document.getElementById("confirmation-request-id");
        if (confirmationId) {
          confirmationId.textContent = "#QUEUED";
        }

        showPage("confirmation-page");
      } else {
        // For all other errors, show the error message
        const errorMessage = error.response?.message || error.message || 'An unknown error occurred';
        showError(`Failed to submit request: ${errorMessage}`);
        hideLoading();
      }
    }
}

async function handleAdminLogin(event) {
  event.preventDefault();

  const form = event.target;
  const formData = new FormData(form);
  const username = formData.get('username');
  const password = formData.get('password');

  console.log(`Attempting admin login for: ${username}`);

  try {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ username, password })
    });

    console.log('Login response status:', response.status);

    if (response.ok) {
      const data = await response.json();
      console.log('Login successful:', data);

      // Store the received token and user info
      localStorage.setItem('token', data.token);

      // Store user info including role
      localStorage.setItem('user', JSON.stringify({
        username: data.username,
        role: data.role,
        // Add any other user data you need
      }));

      // Update global variables
      authToken = data.token;
      currentUser = { username: data.username, role: data.role };
      userRole = data.role;

      // Show success message
      showNotification('Login successful!', 'success');

      // Redirect to admin dashboard
      console.log('Redirecting to admin dashboard...');
      showPage('admin-dashboard');
    } else {
      const errorData = await response.json();
      console.error('Login failed:', errorData);
      showNotification(errorData.message || 'Login failed. Please check your credentials.', 'error');
    }
  } catch (error) {
    console.error('Error during login:', error);
    showNotification('An error occurred during login. Please try again.', 'error');
  }
}

async function handleUserLogin() {
  const form = document.getElementById("user-login-form")
  const formData = new FormData(form)
  const messageDiv = document.getElementById("user-login-message")

  const loginData = {
    username: formData.get("username"),
    password: formData.get("password")
  }

  console.log('Attempting user login for:', loginData.username)

  try {
    if (messageDiv) {
      messageDiv.style.display = "none"
    }

    const response = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(loginData)
    })
    if (response.ok) {
      const data = await response.json();
      localStorage.setItem('token', data.token);
      localStorage.setItem('user', JSON.stringify({
        username: data.username,
        role: data.role,
      }));

      // Update global variables
      authToken = data.token;
      currentUser = JSON.parse(localStorage.getItem('user'));
      userRole = data.role;  // Set userRole from the response

      // Redirect based on role
      setTimeout(() => {
        if (userRole.includes('ADMIN') || userRole.includes('DISPATCHER')) {
          showPage('admin-dashboard');
        } else {
          showPage('user-dashboard');
        }
      }, 1000);
    } else {
      const error = await response.json();
      showUserError(error.message || 'Login failed. Please try again.');
    }
  } catch (error) {
    console.error('Error during login:', error);
    showUserError('An error occurred during login. Please try again.');
  }

  function showUserError(message) {
    if (messageDiv) {
      messageDiv.textContent = message
      messageDiv.className = "alert alert-error"
      messageDiv.style.display = "block"
    }
  }

  function showUserSuccess(message) {
    if (messageDiv) {
      messageDiv.textContent = message
      messageDiv.className = "alert alert-success"
      messageDiv.style.display = "block"
    }
  }
}

async function handleUserRegistration() {
  const form = document.getElementById("register-form")
  const formData = new FormData(form)
  const messageDiv = document.getElementById("register-message")

  const password = formData.get("password")
  const confirmPassword = formData.get("confirmPassword")

  if (password !== confirmPassword) {
    showRegisterError("Passwords do not match")
    return
  }

  const registerData = {
    username: formData.get("username"),
    email: formData.get("email"),
    password: password,
    role: "ROLE_USER"
  }

  try {
    if (messageDiv) {
      messageDiv.style.display = "none"
    }

    const response = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(registerData)
    })

    if (response.ok) {
      const data = await response.json()
      showRegisterSuccess("Registration successful! You can now login.")
      form.reset()
      setTimeout(() => {
        showPage("user-login")
      }, 2000)
    } else {
      const errorText = await response.text()
      let error = {}
      try {
        error = JSON.parse(errorText)
      } catch (e) {
        error = { error: errorText }
      }
      showRegisterError(error.error || "Registration failed")
    }
  } catch (error) {
    console.error("Registration error:", error)
    showRegisterError("Registration failed. Please try again.")
  }

  function showRegisterError(message) {
    if (messageDiv) {
      messageDiv.textContent = message
      messageDiv.className = "alert alert-error"
      messageDiv.style.display = "block"
    }
  }

  function showRegisterSuccess(message) {
    if (messageDiv) {
      messageDiv.textContent = message
      messageDiv.className = "alert alert-success"
      messageDiv.style.display = "block"
    }
  }
}

async function trackRequestStatus(requestId) {
    const statusText = document.getElementById("current-status")
    const progressBar = document.getElementById("progress-bar")
    const etaDisplay = document.getElementById("eta-display")
    const etaTime = document.getElementById("eta-time")
    const statusIcons = document.querySelectorAll(".status-icon")

    if (!statusText || !progressBar) {
        console.error('Status elements not found')
        return
    }

    // Reset all icons
    statusIcons.forEach(icon => icon.classList.remove('active'))

    try {
        // Poll for request status updates
        const checkStatus = async () => {
            try {
                const request = await apiCall(`/api/requests/${requestId}`)
                console.log('Request status update:', request)

                // Update status based on current state
                switch (request.status) {
                    case 'PENDING':
                        statusText.textContent = "Searching for available ambulance..."
                        progressBar.style.width = "20%"
                        document.querySelector(".status-icon[data-status='PENDING']")?.classList.add('active')
                        break

                    case 'ASSIGNED':
                        statusText.textContent = "Ambulance assigned and preparing for dispatch"
                        progressBar.style.width = "40%"
                        document.querySelector(".status-icon[data-status='ASSIGNED']")?.classList.add('active')
                        break

                    case 'DISPATCHED':
                        const eta = request.eta ? ` (ETA: ${request.eta} min)` : ''
                        statusText.textContent = `Ambulance #${request.ambulance?.id || ''} is on the way${eta}`
                        progressBar.style.width = "60%"
                        if (etaDisplay) etaDisplay.style.display = "block"
                        if (etaTime) etaTime.textContent = `${request.eta || '5-10'} minutes`
                        document.querySelector(".status-icon[data-status='DISPATCHED']")?.classList.add('active')
                        break

                    case 'ARRIVED':
                        statusText.textContent = `Ambulance has arrived at the location`
                        progressBar.style.width = "80%"
                        if (etaTime) etaTime.textContent = "Arrived"
                        document.querySelector(".status-icon[data-status='ARRIVED']")?.classList.add('active')
                        break

                    case 'IN_PROGRESS':
                        statusText.textContent = "Patient is being attended to"
                        progressBar.style.width = "90%"
                        document.querySelector(".status-icon[data-status='IN_PROGRESS']")?.classList.add('active')
                        break

                    case 'COMPLETED':
                        statusText.textContent = "Service completed successfully"
                        progressBar.style.width = "100%"
                        progressBar.style.backgroundColor = "#10b981"
                        if (etaDisplay) etaDisplay.style.display = "none"
                        document.querySelector(".status-icon[data-status='COMPLETED']")?.classList.add('active')
                        return true

                    case 'CANCELLED':
                        statusText.textContent = "Request has been cancelled"
                        progressBar.style.width = "100%"
                        progressBar.style.backgroundColor = "#ef4444"
                        if (etaDisplay) etaDisplay.style.display = "none"
                        return true
                }

                // Continue polling if not in final state
                if (!['COMPLETED', 'CANCELLED'].includes(request.status)) {
                    setTimeout(checkStatus, 10000) // Check every 10 seconds
                }
                return false

            } catch (error) {
                console.error('Error checking request status:', error)
                // If it's a 404, the request might not be processed yet, keep trying
                if (error.status === 404) {
                    setTimeout(checkStatus, 5000)
                } else {
                    statusText.textContent = "Error checking status. Please refresh the page."
                }
                return false
            }
        }

        // Start polling
        const isComplete = await checkStatus()

        // If completed immediately, show success message
        if (isComplete) {
            showSuccess("Request processing complete!")
        }

    } catch (error) {
        console.error('Error tracking request status:', error)
        statusText.textContent = "Error tracking request status. Please check back later."
    }
}

function getCurrentLocation() {
  if (navigator.geolocation) {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        // In a real implementation, you would use reverse geocoding
        // For demo purposes, we'll use placeholder values
        const addressField = document.getElementById("address")
        const cityField = document.getElementById("city")
        const zipcodeField = document.getElementById("zipcode")

        if (addressField) addressField.value = "Current Location (GPS)"
        if (cityField) cityField.value = "Auto-detected"
        if (zipcodeField) zipcodeField.value = "00000"

        showSuccess("Location detected and filled automatically")
      },
      (error) => {
        console.error("Geolocation error:", error)
        showError("Unable to get your location. Please enter manually.")
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 60000
      }
    )
  } else {
    showError("Geolocation is not supported by this browser.")
  }
}

function contactDispatch() {
  // In a real implementation, this could open a direct communication channel
  const phone = "08002255372"
  const message = `Emergency dispatch contact:\n\nPhone: ${phone}\n\nThis would normally:\n- Open your phone dialer\n- Connect to dispatch center\n- Provide direct communication with emergency services`

  alert(message)

  // Attempt to open phone dialer on mobile devices
  if (/Android|iPhone|iPad|iPod|BlackBerry|IEMobile/i.test(navigator.userAgent)) {
    window.location.href = `tel:${phone}`
  }
}

function showLoginPrompt() {
  const message = `Account Benefits:\n\n• Track your emergency requests in real-time\n• View complete request history\n• Save personal and medical information\n• Faster service with pre-filled forms\n• SMS/Email notifications\n\nWould you like to create an account or login?`

  if (confirm(message)) {
    showPage('user-login')
  }
}

function logout() {
  if (confirm("Are you sure you want to logout?")) {
    console.log('Logging out user')
    // Clear authentication data
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    authToken = null
    currentUser = null
    userRole = null

    // Return to landing page
    showPage("landing-page")
  }
}

// Utility functions for user feedback
function showLoading(message) {
  // Create or update loading indicator
  let loadingDiv = document.getElementById('loading-indicator')
  if (!loadingDiv) {
    loadingDiv = document.createElement('div')
    loadingDiv.id = 'loading-indicator'
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
    `
    document.body.appendChild(loadingDiv)
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
  `
  loadingDiv.style.display = 'block'

  // Add click event for close button
  document.getElementById('close-loading').addEventListener('click', hideLoading)
}

function hideLoading() {
  const loadingDiv = document.getElementById('loading-indicator')
  if (loadingDiv) {
    loadingDiv.style.display = 'none'
  }
}

function showError(message) {
  hideLoading()
  showNotification(message, 'error')
}

function showSuccess(message) {
  hideLoading()
  showNotification(message, 'success')
}

function showNotification(message, type = 'info') {
  const notification = document.createElement('div')
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
  `

  switch (type) {
    case 'success':
      notification.style.background = '#16a34a'
      break
    case 'error':
      notification.style.background = '#dc2626'
      break
    case 'warning':
      notification.style.background = '#d97706'
      break
    default:
      notification.style.background = '#2563eb'
  }

  notification.textContent = message
  document.body.appendChild(notification)

  // Auto-remove after 5 seconds
  setTimeout(() => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  }, 5000)

  // Remove on click
  notification.addEventListener('click', () => {
    if (notification.parentNode) {
      notification.parentNode.removeChild(notification)
    }
  })
}

// Enhanced data management functions integrated with backend
function getRequests() {
  return apiCall('/requests').catch(() => [])
}

function getAmbulances() {
  return apiCall('/ambulances').catch(() => [])
}

function getPendingRequests() {
  return apiCall('/requests/pending').catch(() => [])
}

// Initialize sample data on first load
async function initializeApp() {
  try {
    console.log('Initializing app...')

    // Initialize sample localStorage data if none exists
    initializeLocalStorageData()

    // Try to check backend connectivity
    try {
      const response = await fetch('/api/debug/health')
      if (response.ok) {
        console.log('Backend is available')
      }
    } catch (error) {
      console.log('Backend not available, using localStorage only')
    }

  } catch (error) {
    console.log('App initialization error:', error.message)
  }
}

function initializeLocalStorageData() {
  // Initialize ambulances if none exist
  const existingAmbulances = localStorage.getItem('ambulance_ambulances')
  if (!existingAmbulances) {
    const sampleAmbulances = [
      { id: 1, currentLocation: 'Central Station', availability: 'AVAILABLE' },
      { id: 2, currentLocation: 'North Station', availability: 'AVAILABLE' },
      { id: 3, currentLocation: 'South Station', availability: 'MAINTENANCE' },
      { id: 4, currentLocation: 'East Station', availability: 'AVAILABLE' },
      { id: 5, currentLocation: 'West Station', availability: 'DISPATCHED' }
    ]
    localStorage.setItem('ambulance_ambulances', JSON.stringify(sampleAmbulances))
    console.log('Initialized sample ambulance data')
  }

  // Initialize requests if none exist
  const existingRequests = localStorage.getItem('ambulance_requests')
  if (!existingRequests) {
    const sampleRequests = [
      {
        id: 1001,
        userName: 'John Doe',
        userContact: '+1234567890',
        location: '123 Main Street, Emergency City',
        emergencyDescription: 'Cardiac Emergency: Patient experiencing chest pain',
        requestTime: new Date(Date.now() - 3600000).toISOString(), // 1 hour ago
        status: 'COMPLETED',
        ambulanceId: 1,
        dispatchTime: new Date(Date.now() - 3300000).toISOString()
      },
      {
        id: 1002,
        userName: 'Jane Smith',
        userContact: '+1987654321',
        location: '456 Oak Avenue, Emergency City',
        emergencyDescription: 'Traffic Accident: Multiple injuries reported',
        requestTime: new Date(Date.now() - 1800000).toISOString(), // 30 minutes ago
        status: 'DISPATCHED',
        ambulanceId: 5,
        dispatchTime: new Date(Date.now() - 1500000).toISOString()
      },
      {
        id: 1003,
        userName: 'Bob Johnson',
        userContact: '+1122334455',
        location: '789 Pine Road, Emergency City',
        emergencyDescription: 'Respiratory Emergency: Difficulty breathing',
        requestTime: new Date(Date.now() - 300000).toISOString(), // 5 minutes ago
        status: 'PENDING'
      }
    ]
    localStorage.setItem('ambulance_requests', JSON.stringify(sampleRequests))
    console.log('Initialized sample request data')
  }

  // Initialize patients if none exist
  const existingPatients = localStorage.getItem('ambulance_patients')
  if (!existingPatients) {
    const samplePatients = [
      {
        id: 1,
        name: 'John Doe',
        contact: '+1234567890',
        medicalNotes: 'History of heart disease',
        createdAt: new Date(Date.now() - 86400000).toISOString(),
        updatedAt: new Date(Date.now() - 3600000).toISOString()
      },
      {
        id: 2,
        name: 'Jane Smith',
        contact: '+1987654321',
        medicalNotes: 'No known allergies',
        createdAt: new Date(Date.now() - 7200000).toISOString(),
        updatedAt: new Date(Date.now() - 1800000).toISOString()
      }
    ]
    localStorage.setItem('ambulance_patients', JSON.stringify(samplePatients))
    console.log('Initialized sample patient data')
  }
}

// Format utility functions
function formatDateTime(dateString) {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleString()
}

function formatTime(dateString) {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// Add styles for animations and loading
const style = document.createElement('style')
style.textContent = `
  @keyframes slideIn {
    from {
      transform: translateX(100%);
      opacity: 0;
    }
    to {
      transform: translateX(0);
      opacity: 1;
    }
  }

  .spinner {
    display: inline-block;
    width: 20px;
    height: 20px;
    border: 2px solid #f3f3f3;
    border-radius: 50%;
    border-top-color: #dc2626;
    animation: spin 1s ease-in-out infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }

  .alert-error {
    background-color: #fee2e2;
    color: #991b1b;
    border: 1px solid #fecaca;
  }

  .alert-success {
    background-color: #d1fae5;
    color: #065f46;
    border: 1px solid #a7f3d0;
  }

  .alert {
    padding: 0.75rem 1rem;
    border-radius: 6px;
    margin-bottom: 1rem;
    font-size: 0.875rem;
  }
`
document.head.appendChild(style)