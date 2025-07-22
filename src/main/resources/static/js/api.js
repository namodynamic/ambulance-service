// API utility functions for Spring Boot backend integration
class AmbulanceAPI {
  constructor() {
    this.baseURL = '/api'
    this.token = localStorage.getItem('token')
  }

  // Update token when authentication changes
  setToken(token) {
    this.token = token
    if (token) {
      localStorage.setItem('token', token)
    } else {
      localStorage.removeItem('token')
    }
  }

  // Get authentication headers
  getHeaders() {
    const headers = {
      'Content-Type': 'application/json'
    }

    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`
    }

    return headers
  }

  // Generic API call method
  async call(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`
    const config = {
      headers: this.getHeaders(),
      ...options
    }

    try {
      const response = await fetch(url, config)

      // Handle authentication errors
      if (response.status === 401 || response.status === 403) {
        this.handleAuthError()
        throw new Error('Authentication required')
      }

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}))
        throw new Error(errorData.error || errorData.message || `HTTP ${response.status}`)
      }

      // Handle empty responses
      if (response.status === 204) {
        return null
      }

      return await response.json()
    } catch (error) {
      console.error(`API call failed: ${endpoint}`, error)
      throw error
    }
  }

  // Handle authentication errors
  handleAuthError() {
    this.setToken(null)
    localStorage.removeItem('username')
    localStorage.removeItem('role')

    // Redirect to login if not already there
    if (currentPage !== 'admin-login' && currentPage !== 'landing-page') {
      showPage('admin-login')
    }
  }

  // Authentication endpoints
  async login(credentials) {
    const response = await this.call('/auth/login', {
      method: 'POST',
      body: JSON.stringify(credentials)
    })

    if (response.token) {
      this.setToken(response.token)
      localStorage.setItem('username', response.username)
      localStorage.setItem('role', response.role)
    }

    return response
  }

  async register(userData) {
    return await this.call('/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData)
    })
  }

  // Request endpoints
  async createRequest(requestData) {
    return await this.call('/requests', {
      method: 'POST',
      body: JSON.stringify(requestData)
    })
  }

  async getRequests() {
    return await this.call('/requests')
  }

  async getRequest(id) {
    return await this.call(`/requests/${id}`)
  }

  async updateRequestStatus(id, status) {
    return await this.call(`/requests/${id}/status?status=${status}`, {
      method: 'PUT'
    })
  }

  async getPendingRequests() {
    return await this.call('/requests/pending')
  }

  async getRequestsByStatus(status) {
    return await this.call(`/requests/status/${status}`)
  }

  // Ambulance endpoints
  async getAmbulances() {
    return await this.call('/ambulances')
  }

  async getAmbulance(id) {
    return await this.call(`/ambulances/${id}`)
  }

  async createAmbulance(ambulanceData) {
    return await this.call('/ambulances', {
      method: 'POST',
      body: JSON.stringify(ambulanceData)
    })
  }

  async updateAmbulance(id, ambulanceData) {
    return await this.call(`/ambulances/${id}`, {
      method: 'PUT',
      body: JSON.stringify(ambulanceData)
    })
  }

  async updateAmbulanceStatus(id, status) {
    return await this.call(`/ambulances/${id}/status?status=${status}`, {
      method: 'PUT'
    })
  }

  async getAvailableAmbulances() {
    return await this.call('/ambulances/available')
  }

  // Dispatch endpoints
  async dispatchAmbulance(requestId) {
    return await this.call(`/dispatch/${requestId}`, {
      method: 'POST'
    })
  }

  // Patient endpoints
  async getPatients() {
    return await this.call('/patients')
  }

  async getPatient(id) {
    return await this.call(`/patients/${id}`)
  }

  async createPatient(patientData) {
    return await this.call('/patients', {
      method: 'POST',
      body: JSON.stringify(patientData)
    })
  }

  async getPatientByContact(contact) {
    return await this.call(`/patients/contact/${encodeURIComponent(contact)}`)
  }

  // Service History endpoints
  async getServiceHistory() {
    return await this.call('/service-history')
  }

  async getServiceHistoryById(id) {
    return await this.call(`/service-history/${id}`)
  }

  async updateServiceHistory(id, updates) {
    const params = new URLSearchParams()
    if (updates.arrivalTime) params.append('arrivalTime', updates.arrivalTime)
    if (updates.completionTime) params.append('completionTime', updates.completionTime)
    if (updates.status) params.append('status', updates.status)
    if (updates.notes) params.append('notes', updates.notes)

    return await this.call(`/service-history/${id}?${params.toString()}`, {
      method: 'PUT'
    })
  }

  async getServiceHistoryByStatus(status) {
    return await this.call(`/service-history/status/${status}`)
  }

  async getServiceHistoryByDateRange(startDate, endDate) {
    const params = new URLSearchParams({
      start: startDate,
      end: endDate
    })
    return await this.call(`/service-history/date-range?${params.toString()}`)
  }

  // Utility methods for common operations
  async getDashboardStats() {
    try {
      const [requests, ambulances] = await Promise.all([
        this.getRequests(),
        this.getAmbulances()
      ])

      const stats = {
        activeRequests: requests.filter(r =>
          ['PENDING', 'DISPATCHED', 'IN_PROGRESS'].includes(r.status)
        ).length,
        availableAmbulances: ambulances.filter(a => a.availability === 'AVAILABLE').length,
        totalRequests: requests.length,
        todayRequests: requests.filter(r => {
          const requestDate = new Date(r.requestTime)
          const today = new Date()
          return requestDate.toDateString() === today.toDateString()
        }).length
      }

      // Calculate average response time
      const completedRequests = requests.filter(r =>
        r.status === 'COMPLETED' && r.dispatchTime
      )
      stats.avgResponseTime = completedRequests.length > 0
        ? Math.round(
            completedRequests.reduce((sum, r) => {
              const requestTime = new Date(r.requestTime)
              const dispatchTime = new Date(r.dispatchTime)
              return sum + (dispatchTime - requestTime) / (1000 * 60)
            }, 0) / completedRequests.length
          )
        : 0

      return stats
    } catch (error) {
      console.error('Error getting dashboard stats:', error)
      return {
        activeRequests: 0,
        availableAmbulances: 0,
        totalRequests: 0,
        todayRequests: 0,
        avgResponseTime: 0
      }
    }
  }

  // Real-time status polling
  async pollRequestStatus(requestId, callback, interval = 5000) {
    const poll = async () => {
      try {
        const request = await this.getRequest(requestId)
        callback(request)

        // Continue polling if request is not final state
        if (!['COMPLETED', 'CANCELLED'].includes(request.status)) {
          setTimeout(poll, interval)
        }
      } catch (error) {
        console.error('Error polling request status:', error)
        // Stop polling on error
      }
    }

    // Start polling after initial delay
    setTimeout(poll, 2000)
  }

  // Validation helpers
  validatePhoneNumber(phone) {
    const phoneRegex = /^[+]?[0-9]{10,15}$/
    return phoneRegex.test(phone)
  }

  validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  // Format helpers
  formatDateTime(dateString) {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  }

  formatTime(dateString) {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  formatPhoneNumber(phone) {
    // Format phone number for display
    if (!phone) return 'N/A'
    const cleaned = phone.replace(/\D/g, '')
    const match = cleaned.match(/^(\d{3})(\d{3})(\d{4})$/)
    if (match) {
      return `(${match[1]}) ${match[2]}-${match[3]}`
    }
    return phone
  }

  // Error handling helpers
  handleError(error, context = '') {
    console.error(`${context} error:`, error)

    if (error.message.includes('Authentication required')) {
      return 'Please log in to continue'
    } else if (error.message.includes('No ambulance available')) {
      return 'No ambulances currently available. Please call emergency services directly.'
    } else if (error.message.includes('Invalid phone number')) {
      return 'Please enter a valid phone number'
    } else if (error.message.includes('Network')) {
      return 'Network error. Please check your connection and try again.'
    } else {
      return error.message || 'An unexpected error occurred'
    }
  }
}

// Create global API instance
const ambulanceAPI = new AmbulanceAPI()

// Legacy compatibility - replace existing apiCall function
async function apiCall(endpoint, options = {}) {
  return await ambulanceAPI.call(endpoint, options)
}

// Export for use in modules if needed
if (typeof module !== 'undefined' && module.exports) {
  module.exports = { AmbulanceAPI, ambulanceAPI }
}