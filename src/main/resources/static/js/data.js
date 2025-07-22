// Data management functions integrated with Spring Boot backend
// This file provides both local storage fallback and backend integration

class DataManager {
  constructor() {
    this.useBackend = true
    this.localStoragePrefix = 'ambulance_'
  }

  // Toggle between backend and local storage
  setBackendMode(useBackend) {
    this.useBackend = useBackend
  }

  // Request management
  async getRequests() {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.getRequests()
      } catch (error) {
        console.warn('Backend unavailable, falling back to local storage:', error)
        return this.getLocalRequests()
      }
    }
    return this.getLocalRequests()
  }

  async createRequest(requestData) {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.createRequest(requestData)
      } catch (error) {
        console.warn('Backend unavailable, saving locally:', error)
        return this.saveLocalRequest(requestData)
      }
    }
    return this.saveLocalRequest(requestData)
  }

  async updateRequestStatus(requestId, status) {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.updateRequestStatus(requestId, status)
      } catch (error) {
        console.warn('Backend unavailable, updating locally:', error)
        return this.updateLocalRequestStatus(requestId, status)
      }
    }
    return this.updateLocalRequestStatus(requestId, status)
  }

  async getPendingRequests() {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.getPendingRequests()
      } catch (error) {
        console.warn('Backend unavailable, filtering locally:', error)
        const requests = this.getLocalRequests()
        return requests.filter(r => r.status === 'PENDING')
      }
    }
    const requests = this.getLocalRequests()
    return requests.filter(r => r.status === 'PENDING')
  }

  // Ambulance management
  async getAmbulances() {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.getAmbulances()
      } catch (error) {
        console.warn('Backend unavailable, falling back to local storage:', error)
        return this.getLocalAmbulances()
      }
    }
    return this.getLocalAmbulances()
  }

  async createAmbulance(ambulanceData) {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.createAmbulance(ambulanceData)
      } catch (error) {
        console.warn('Backend unavailable, saving locally:', error)
        return this.saveLocalAmbulance(ambulanceData)
      }
    }
    return this.saveLocalAmbulance(ambulanceData)
  }

  async updateAmbulanceStatus(ambulanceId, status) {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.updateAmbulanceStatus(ambulanceId, status)
      } catch (error) {
        console.warn('Backend unavailable, updating locally:', error)
        return this.updateLocalAmbulanceStatus(ambulanceId, status)
      }
    }
    return this.updateLocalAmbulanceStatus(ambulanceId, status)
  }

  async getAvailableAmbulances() {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.getAvailableAmbulances()
      } catch (error) {
        console.warn('Backend unavailable, filtering locally:', error)
        const ambulances = this.getLocalAmbulances()
        return ambulances.filter(a => a.availability === 'AVAILABLE')
      }
    }
    const ambulances = this.getLocalAmbulances()
    return ambulances.filter(a => a.availability === 'AVAILABLE')
  }

  // Patient management
  async getPatients() {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.getPatients()
      } catch (error) {
        console.warn('Backend unavailable, falling back to local storage:', error)
        return this.getLocalPatients()
      }
    }
    return this.getLocalPatients()
  }

  async findOrCreatePatient(name, contact) {
    if (this.useBackend) {
      try {
        // Try to find existing patient
        const existingPatient = await ambulanceAPI.getPatientByContact(contact)
        if (existingPatient) {
          return existingPatient
        }

        // Create new patient
        return await ambulanceAPI.createPatient({
          name: name,
          contact: contact,
          medicalNotes: ''
        })
      } catch (error) {
        console.warn('Backend unavailable, managing locally:', error)
        return this.findOrCreateLocalPatient(name, contact)
      }
    }
    return this.findOrCreateLocalPatient(name, contact)
  }

  // Dispatch operations
  async dispatchAmbulance(requestId) {
    if (this.useBackend) {
      try {
        return await ambulanceAPI.dispatchAmbulance(requestId)
      } catch (error) {
        console.warn('Backend unavailable, dispatching locally:', error)
        return this.dispatchLocalAmbulance(requestId)
      }
    }
    return this.dispatchLocalAmbulance(requestId)
  }

  // Local storage implementations (fallback)
  getLocalRequests() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'requests')
    return stored ? JSON.parse(stored) : []
  }

  saveLocalRequests(requests) {
    localStorage.setItem(this.localStoragePrefix + 'requests', JSON.stringify(requests))
  }

  saveLocalRequest(requestData) {
    const requests = this.getLocalRequests()
    const newRequest = {
      id: this.generateId(),
      ...requestData,
      requestTime: new Date().toISOString(),
      status: 'PENDING'
    }
    requests.push(newRequest)
    this.saveLocalRequests(requests)
    return newRequest
  }

  updateLocalRequestStatus(requestId, status) {
    const requests = this.getLocalRequests()
    const request = requests.find(r => r.id == requestId)
    if (request) {
      request.status = status
      if (status === 'DISPATCHED' && !request.dispatchTime) {
        request.dispatchTime = new Date().toISOString()
      }
      this.saveLocalRequests(requests)
      return request
    }
    throw new Error('Request not found')
  }

  getLocalAmbulances() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'ambulances')
    if (stored) {
      return JSON.parse(stored)
    }

    // Initialize with sample data if none exists
    const sampleAmbulances = [
      { id: 1, currentLocation: 'Central Station', availability: 'AVAILABLE' },
      { id: 2, currentLocation: 'North Station', availability: 'AVAILABLE' },
      { id: 3, currentLocation: 'South Station', availability: 'MAINTENANCE' },
      { id: 4, currentLocation: 'East Station', availability: 'AVAILABLE' },
    ]
    this.saveLocalAmbulances(sampleAmbulances)
    return sampleAmbulances
  }

  saveLocalAmbulances(ambulances) {
    localStorage.setItem(this.localStoragePrefix + 'ambulances', JSON.stringify(ambulances))
  }

  saveLocalAmbulance(ambulanceData) {
    const ambulances = this.getLocalAmbulances()
    const newAmbulance = {
      id: this.generateId(),
      ...ambulanceData
    }
    ambulances.push(newAmbulance)
    this.saveLocalAmbulances(ambulances)
    return newAmbulance
  }

  updateLocalAmbulanceStatus(ambulanceId, status) {
    const ambulances = this.getLocalAmbulances()
    const ambulance = ambulances.find(a => a.id == ambulanceId)
    if (ambulance) {
      ambulance.availability = status
      this.saveLocalAmbulances(ambulances)
      return ambulance
    }
    throw new Error('Ambulance not found')
  }

  getLocalPatients() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'patients')
    return stored ? JSON.parse(stored) : []
  }

  saveLocalPatients(patients) {
    localStorage.setItem(this.localStoragePrefix + 'patients', JSON.stringify(patients))
  }

  findOrCreateLocalPatient(name, contact) {
    const patients = this.getLocalPatients()
    let patient = patients.find(p => p.contact === contact)

    if (!patient) {
      patient = {
        id: this.generateId(),
        name: name,
        contact: contact,
        medicalNotes: '',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
      patients.push(patient)
      this.saveLocalPatients(patients)
    }

    return patient
  }

  dispatchLocalAmbulance(requestId) {
    const requests = this.getLocalRequests()
    const ambulances = this.getLocalAmbulances()

    const request = requests.find(r => r.id == requestId)
    if (!request) {
      throw new Error('Request not found')
    }

    const availableAmbulance = ambulances.find(a => a.availability === 'AVAILABLE')
    if (!availableAmbulance) {
      throw new Error('No ambulance available')
    }

    // Update request
    request.status = 'DISPATCHED'
    request.ambulanceId = availableAmbulance.id
    request.dispatchTime = new Date().toISOString()

    // Update ambulance
    availableAmbulance.availability = 'DISPATCHED'

    // Save changes
    this.saveLocalRequests(requests)
    this.saveLocalAmbulances(ambulances)

    return { request, ambulance: availableAmbulance }
  }

  // Utility functions
  generateId() {
    return Date.now() + Math.floor(Math.random() * 1000)
  }

  // Statistics and analytics
  async getStatistics() {
    try {
      const [requests, ambulances] = await Promise.all([
        this.getRequests(),
        this.getAmbulances()
      ])

      const today = new Date()
      today.setHours(0, 0, 0, 0)

      const stats = {
        total: {
          requests: requests.length,
          ambulances: ambulances.length,
          patients: await this.getPatients().then(p => p.length).catch(() => 0)
        },
        today: {
          requests: requests.filter(r => new Date(r.requestTime) >= today).length,
          completed: requests.filter(r =>
            r.status === 'COMPLETED' && new Date(r.requestTime) >= today
          ).length
        },
        current: {
          activeRequests: requests.filter(r =>
            ['PENDING', 'DISPATCHED', 'IN_PROGRESS'].includes(r.status)
          ).length,
          availableAmbulances: ambulances.filter(a => a.availability === 'AVAILABLE').length,
          busyAmbulances: ambulances.filter(a => a.availability === 'DISPATCHED').length,
          maintenanceAmbulances: ambulances.filter(a => a.availability === 'MAINTENANCE').length
        },
        performance: {
          avgResponseTime: this.calculateAverageResponseTime(requests),
          completionRate: this.calculateCompletionRate(requests)
        }
      }

      return stats
    } catch (error) {
      console.error('Error getting statistics:', error)
      return this.getDefaultStats()
    }
  }

  calculateAverageResponseTime(requests) {
    const completedRequests = requests.filter(r =>
      r.status === 'COMPLETED' && r.dispatchTime && r.requestTime
    )

    if (completedRequests.length === 0) return 0

    const totalTime = completedRequests.reduce((sum, request) => {
      const requestTime = new Date(request.requestTime)
      const dispatchTime = new Date(request.dispatchTime)
      return sum + (dispatchTime - requestTime)
    }, 0)

    return Math.round(totalTime / completedRequests.length / (1000 * 60)) // Convert to minutes
  }

  calculateCompletionRate(requests) {
    if (requests.length === 0) return 0
    const completedRequests = requests.filter(r => r.status === 'COMPLETED')
    return Math.round((completedRequests.length / requests.length) * 100)
  }

  getDefaultStats() {
    return {
      total: { requests: 0, ambulances: 0, patients: 0 },
      today: { requests: 0, completed: 0 },
      current: { activeRequests: 0, availableAmbulances: 0, busyAmbulances: 0, maintenanceAmbulances: 0 },
      performance: { avgResponseTime: 0, completionRate: 0 }
    }
  }

  // Search and filtering
  async searchRequests(query) {
    const requests = await this.getRequests()
    const lowercaseQuery = query.toLowerCase()

    return requests.filter(request =>
      request.id.toString().includes(query) ||
      request.userName?.toLowerCase().includes(lowercaseQuery) ||
      request.userContact?.includes(query) ||
      request.location?.toLowerCase().includes(lowercaseQuery) ||
      request.emergencyDescription?.toLowerCase().includes(lowercaseQuery)
    )
  }

  async searchPatients(query) {
    const patients = await this.getPatients()
    const lowercaseQuery = query.toLowerCase()

    return patients.filter(patient =>
      patient.name?.toLowerCase().includes(lowercaseQuery) ||
      patient.contact?.includes(query) ||
      patient.medicalNotes?.toLowerCase().includes(lowercaseQuery)
    )
  }

  async filterRequestsByStatus(status) {
    const requests = await this.getRequests()
    if (status === 'all') return requests
    return requests.filter(r => r.status === status)
  }

  async filterRequestsByDateRange(startDate, endDate) {
    const requests = await this.getRequests()
    const start = new Date(startDate)
    const end = new Date(endDate)
    end.setHours(23, 59, 59, 999)

    return requests.filter(r => {
      const requestDate = new Date(r.requestTime)
      return requestDate >= start && requestDate <= end
    })
  }

  // Data validation
  validateRequest(requestData) {
    const required = ['userName', 'userContact', 'location', 'emergencyDescription']
    const missing = required.filter(field => !requestData[field] || requestData[field].trim() === '')

    if (missing.length > 0) {
      return {
        valid: false,
        errors: missing.map(field => `${this.getFieldDisplayName(field)} is required`)
      }
    }

    // Phone number validation
    if (!ambulanceAPI.validatePhoneNumber(requestData.userContact)) {
      return {
        valid: false,
        errors: ['Phone number must be 10-15 digits']
      }
    }

    return { valid: true, errors: [] }
  }

  validateAmbulance(ambulanceData) {
    const required = ['currentLocation', 'availability']
    const missing = required.filter(field => !ambulanceData[field] || ambulanceData[field].trim() === '')

    if (missing.length > 0) {
      return {
        valid: false,
        errors: missing.map(field => `${this.getFieldDisplayName(field)} is required`)
      }
    }

    const validStatuses = ['AVAILABLE', 'DISPATCHED', 'MAINTENANCE', 'OUT_OF_SERVICE']
    if (!validStatuses.includes(ambulanceData.availability)) {
      return {
        valid: false,
        errors: ['Status must be one of: ' + validStatuses.join(', ')]
      }
    }

    return { valid: true, errors: [] }
  }

  getFieldDisplayName(field) {
    const displayNames = {
      userName: 'Patient Name',
      userContact: 'Phone Number',
      location: 'Location',
      emergencyDescription: 'Emergency Description',
      currentLocation: 'Current Location',
      availability: 'Status'
    }
    return displayNames[field] || field
  }

  // Data export/import (for admin use)
  async exportData() {
    try {
      const [requests, ambulances, patients] = await Promise.all([
        this.getRequests(),
        this.getAmbulances(),
        this.getPatients()
      ])

      const exportData = {
        timestamp: new Date().toISOString(),
        version: '1.0',
        data: {
          requests,
          ambulances,
          patients
        },
        stats: await this.getStatistics()
      }

      return exportData
    } catch (error) {
      console.error('Error exporting data:', error)
      throw error
    }
  }

  downloadDataAsJSON() {
    this.exportData().then(data => {
      const dataStr = JSON.stringify(data, null, 2)
      const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr)

      const exportFileDefaultName = `ambulance-service-data-${new Date().toISOString().split('T')[0]}.json`

      const linkElement = document.createElement('a')
      linkElement.setAttribute('href', dataUri)
      linkElement.setAttribute('download', exportFileDefaultName)
      linkElement.click()
    }).catch(error => {
      console.error('Error downloading data:', error)
      alert('Error exporting data: ' + error.message)
    })
  }

  // Cache management
  clearLocalCache() {
    const keys = ['requests', 'ambulances', 'patients']
    keys.forEach(key => {
      localStorage.removeItem(this.localStoragePrefix + key)
    })
  }

  async syncWithBackend() {
    if (!this.useBackend) return

    try {
      // Force refresh from backend
      const [requests, ambulances, patients] = await Promise.all([
        ambulanceAPI.getRequests(),
        ambulanceAPI.getAmbulances(),
        ambulanceAPI.getPatients()
      ])

      // Update local cache
      this.saveLocalRequests(requests)
      this.saveLocalAmbulances(ambulances)
      this.saveLocalPatients(patients)

      console.log('Data synchronized with backend')
      return true
    } catch (error) {
      console.error('Error syncing with backend:', error)
      return false
    }
  }

  // Real-time data updates
  async pollForUpdates(callback, interval = 30000) {
    const poll = async () => {
      try {
        const stats = await this.getStatistics()
        callback(stats)
      } catch (error) {
        console.error('Error polling for updates:', error)
      }
    }

    // Initial call
    poll()

    // Set up interval
    return setInterval(poll, interval)
  }

  stopPolling(intervalId) {
    if (intervalId) {
      clearInterval(intervalId)
    }
  }
}

// Create global data manager instance
const dataManager = new DataManager()

// Legacy compatibility functions
async function getRequests() {
  return await dataManager.getRequests()
}

async function getAmbulances() {
  return await dataManager.getAmbulances()
}

async function getPendingRequests() {
  return await dataManager.getPendingRequests()
}

async function createRequest(requestData) {
  return await dataManager.createRequest(requestData)
}

async function updateRequestStatus(requestId, status) {
  return await dataManager.updateRequestStatus(requestId, status)
}

async function dispatchAmbulance(requestId) {
  return await dataManager.dispatchAmbulance(requestId)
}

// Utility functions
function generateRequestId() {
  return 'REQ-' + Date.now().toString().slice(-6)
}

function generateAmbulanceId() {
  return 'AMB-' + Date.now().toString().slice(-6)
}

function formatDateTime(dateString) {
  return ambulanceAPI.formatDateTime(dateString)
}

function formatTime(dateString) {
  return ambulanceAPI.formatTime(dateString)
}

// Export for module use
if (typeof module !== 'undefined' && module.exports) {
  module.exports = {
    DataManager,
    dataManager,
    getRequests,
    getAmbulances,
    getPendingRequests,
    createRequest,
    updateRequestStatus,
    dispatchAmbulance,
    generateRequestId,
    generateAmbulanceId,
    formatDateTime,
    formatTime
  }
}