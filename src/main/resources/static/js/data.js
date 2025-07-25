// data.js - Optimized Data Management with Backend Integration
class DataManager {
  constructor() {
    this.useBackend = true;
    this.localStoragePrefix = 'ambulance_';
    this.apiBase = '/api';
    this.cache = new Map();
    this.cacheTimeout = 30000; // 30 seconds
  }

  // Configuration methods
  setBackendMode(useBackend) {
    this.useBackend = useBackend;
    console.log(`Backend mode set to: ${useBackend}`);
  }

  setApiBase(apiBase) {
    this.apiBase = apiBase;
  }

  // Cache management
  getCacheKey(key) {
    return `${Date.now()}_${key}`;
  }

  setCache(key, data) {
    this.cache.set(key, {
      data,
      timestamp: Date.now()
    });
  }

  getCache(key) {
    const cached = this.cache.get(key);
    if (!cached) return null;

    // Check if cache is still valid
    if (Date.now() - cached.timestamp > this.cacheTimeout) {
      this.cache.delete(key);
      return null;
    }

    return cached.data;
  }

  clearCache() {
    this.cache.clear();
  }

  // Core API wrapper with fallback
  async apiCall(endpoint, options = {}) {
    const url = `${this.apiBase}${endpoint}`;
    const token = localStorage.getItem('token');

    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` })
      },
      ...options
    };

    try {
      const response = await fetch(url, config);

      if (response.status === 401 || response.status === 403) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        throw new Error('Authentication required');
      }

      if (!response.ok) {
        const errorText = await response.text();
        let errorData = {};
        try {
          errorData = JSON.parse(errorText);
        } catch (e) {
          errorData = { error: errorText || `HTTP ${response.status}` };
        }
        throw new Error(errorData.error || errorData.message || `HTTP ${response.status}`);
      }

      // Handle empty responses
      if (response.status === 204) {
        return null;
      }

      return await response.json();
    } catch (error) {
      console.error(`API call failed: ${endpoint}`, error);
      throw error;
    }
  }

  // Generic data fetcher with fallback
  async getDataWithFallback(apiEndpoint, localStorageGetter, cacheKey = null) {
    // Check cache first
    if (cacheKey) {
      const cached = this.getCache(cacheKey);
      if (cached) {
        console.log(`Returning cached data for ${cacheKey}`);
        return cached;
      }
    }

    if (this.useBackend) {
      try {
        const data = await this.apiCall(apiEndpoint);
        console.log(`Loaded ${Array.isArray(data) ? data.length : 'data'} items from API: ${apiEndpoint}`);

        // Cache the result
        if (cacheKey) {
          this.setCache(cacheKey, data);
        }

        return data;
      } catch (error) {
        console.warn(`API failed for ${apiEndpoint}, using localStorage:`, error.message);
        const localData = localStorageGetter();

        // Cache the local data too
        if (cacheKey) {
          this.setCache(cacheKey, localData);
        }

        return localData;
      }
    }

    const localData = localStorageGetter();
    if (cacheKey) {
      this.setCache(cacheKey, localData);
    }
    return localData;
  }

  // Request management
  async getRequests() {
    return this.getDataWithFallback(
      '/requests',
      () => this.getLocalRequests(),
      'requests'
    );
  }

  async getRequestById(requestId) {
    try {
      if (this.useBackend) {
        return await this.apiCall(`/requests/${requestId}`);
      }
    } catch (error) {
      console.warn('API request failed, checking local storage:', error.message);
    }

    const requests = this.getLocalRequests();
    return requests.find(r => r.id == requestId);
  }

  async getUserRequests(userId) {
    return this.getDataWithFallback(
      `/requests/user/${userId}`,
      () => {
        const requests = this.getLocalRequests();
        return requests.filter(r => r.userId == userId);
      },
      `user_requests_${userId}`
    );
  }

  async getPendingRequests() {
    return this.getDataWithFallback(
      '/requests/pending',
      () => {
        const requests = this.getLocalRequests();
        return requests.filter(r => r.status === 'PENDING');
      },
      'pending_requests'
    );
  }

  async createRequest(requestData) {
    // Validate request data
    const validation = this.validateRequest(requestData);
    if (!validation.valid) {
      throw new Error(`Validation failed: ${validation.errors.join(', ')}`);
    }

    if (this.useBackend) {
      try {
        const newRequest = await this.apiCall('/requests', {
          method: 'POST',
          body: JSON.stringify(requestData)
        });

        // Clear cache
        this.clearCache();

        return newRequest;
      } catch (error) {
        console.warn('API create request failed, saving locally:', error.message);
        return this.saveLocalRequest(requestData);
      }
    }

    return this.saveLocalRequest(requestData);
  }

  async updateRequestStatus(requestId, status, notes = '') {
    if (this.useBackend) {
      try {
        const updatedRequest = await this.apiCall(`/requests/${requestId}/status`, {
          method: 'PUT',
          body: JSON.stringify({ status, notes })
        });

        // Clear cache
        this.clearCache();

        return updatedRequest;
      } catch (error) {
        console.warn('API status update failed, updating locally:', error.message);
        return this.updateLocalRequestStatus(requestId, status, notes);
      }
    }

    return this.updateLocalRequestStatus(requestId, status, notes);
  }

  // Ambulance management
  async getAmbulances() {
    return this.getDataWithFallback(
      '/ambulances',
      () => this.getLocalAmbulances(),
      'ambulances'
    );
  }

  async getAvailableAmbulances() {
    return this.getDataWithFallback(
      '/ambulances/available',
      () => {
        const ambulances = this.getLocalAmbulances();
        return ambulances.filter(a => a.availability === 'AVAILABLE');
      },
      'available_ambulances'
    );
  }

  async createAmbulance(ambulanceData) {
    // Validate ambulance data
    const validation = this.validateAmbulance(ambulanceData);
    if (!validation.valid) {
      throw new Error(`Validation failed: ${validation.errors.join(', ')}`);
    }

    if (this.useBackend) {
      try {
        const newAmbulance = await this.apiCall('/ambulances', {
          method: 'POST',
          body: JSON.stringify(ambulanceData)
        });

        // Clear cache
        this.clearCache();

        return newAmbulance;
      } catch (error) {
        console.warn('API create ambulance failed, saving locally:', error.message);
        return this.saveLocalAmbulance(ambulanceData);
      }
    }

    return this.saveLocalAmbulance(ambulanceData);
  }

  async updateAmbulanceStatus(ambulanceId, status) {
    if (this.useBackend) {
      try {
        const updatedAmbulance = await this.apiCall(`/ambulances/${ambulanceId}/status`, {
          method: 'PUT',
          body: JSON.stringify({ status })
        });

        // Clear cache
        this.clearCache();

        return updatedAmbulance;
      } catch (error) {
        console.warn('API ambulance update failed, updating locally:', error.message);
        return this.updateLocalAmbulanceStatus(ambulanceId, status);
      }
    }

    return this.updateLocalAmbulanceStatus(ambulanceId, status);
  }

  async updateAmbulanceLocation(ambulanceId, location) {
    if (this.useBackend) {
      try {
        const updatedAmbulance = await this.apiCall(`/ambulances/${ambulanceId}`, {
          method: 'PUT',
          body: JSON.stringify({ currentLocation: location })
        });

        // Clear cache
        this.clearCache();

        return updatedAmbulance;
      } catch (error) {
        console.warn('API ambulance location update failed, updating locally:', error.message);
        return this.updateLocalAmbulanceLocation(ambulanceId, location);
      }
    }

    return this.updateLocalAmbulanceLocation(ambulanceId, location);
  }

  // Patient management
  async getPatients() {
    return this.getDataWithFallback(
      '/patients',
      () => this.getLocalPatients(),
      'patients'
    );
  }

  async getPatientByContact(contact) {
    try {
      if (this.useBackend) {
        return await this.apiCall(`/patients/contact/${encodeURIComponent(contact)}`);
      }
    } catch (error) {
      console.warn('API patient lookup failed, checking local storage:', error.message);
    }

    const patients = this.getLocalPatients();
    return patients.find(p => p.contact === contact);
  }

  async findOrCreatePatient(name, contact) {
    // Try to find existing patient first
    let patient = await this.getPatientByContact(contact);

    if (patient) {
      return patient;
    }

    // Create new patient
    const patientData = {
      name: name,
      contact: contact,
      medicalNotes: ''
    };

    if (this.useBackend) {
      try {
        patient = await this.apiCall('/patients', {
          method: 'POST',
          body: JSON.stringify(patientData)
        });

        // Clear cache
        this.clearCache();

        return patient;
      } catch (error) {
        console.warn('API patient creation failed, creating locally:', error.message);
        return this.findOrCreateLocalPatient(name, contact);
      }
    }

    return this.findOrCreateLocalPatient(name, contact);
  }

  // Dispatch operations
  async dispatchAmbulance(requestId) {
    if (this.useBackend) {
      try {
        const result = await this.apiCall(`/dispatch/${requestId}`, {
          method: 'POST'
        });

        // Clear cache
        this.clearCache();

        return result;
      } catch (error) {
        console.warn('API dispatch failed, dispatching locally:', error.message);
        return this.dispatchLocalAmbulance(requestId);
      }
    }

    return this.dispatchLocalAmbulance(requestId);
  }

  // Service History
  async getServiceHistory() {
    return this.getDataWithFallback(
      '/service-history',
      () => {
        // Fallback: generate history from completed requests
        const requests = this.getLocalRequests();
        return requests
          .filter(r => r.status === 'COMPLETED')
          .map(r => ({
            id: r.id,
            requestId: r.id,
            patientId: r.patientId || null,
            ambulanceId: r.ambulanceId,
            status: r.status,
            createdAt: r.requestTime,
            arrivalTime: r.arrivalTime || null,
            completionTime: r.completionTime || r.dispatchTime
          }));
      },
      'service_history'
    );
  }

  // Local storage implementations (fallback)
  getLocalRequests() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'requests');
    return stored ? JSON.parse(stored) : [];
  }

  saveLocalRequests(requests) {
    localStorage.setItem(this.localStoragePrefix + 'requests', JSON.stringify(requests));
  }

  saveLocalRequest(requestData) {
    const requests = this.getLocalRequests();
    const newRequest = {
      id: this.generateId(),
      ...requestData,
      requestTime: new Date().toISOString(),
      status: 'PENDING'
    };
    requests.push(newRequest);
    this.saveLocalRequests(requests);
    return newRequest;
  }

  updateLocalRequestStatus(requestId, status, notes = '') {
    const requests = this.getLocalRequests();
    const request = requests.find(r => r.id == requestId);

    if (request) {
      request.status = status;

      // Add timestamp for status changes
      if (status === 'DISPATCHED' && !request.dispatchTime) {
        request.dispatchTime = new Date().toISOString();
      } else if (status === 'ARRIVED' && !request.arrivalTime) {
        request.arrivalTime = new Date().toISOString();
      } else if (status === 'COMPLETED' && !request.completionTime) {
        request.completionTime = new Date().toISOString();

        // Free up the ambulance if completed
        if (request.ambulanceId) {
          this.updateLocalAmbulanceStatus(request.ambulanceId, 'AVAILABLE');
        }
      }

      // Add notes if provided
      if (notes) {
        if (!request.statusHistory) {
          request.statusHistory = [];
        }
        request.statusHistory.push({
          status: status,
          notes: notes,
          timestamp: new Date().toISOString(),
          changedBy: 'System'
        });
      }

      this.saveLocalRequests(requests);
      return request;
    }

    throw new Error('Request not found');
  }

  getLocalAmbulances() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'ambulances');
    if (stored) {
      return JSON.parse(stored);
    }

    // Initialize with sample data if none exists
    const sampleAmbulances = [
      { id: 1, currentLocation: 'Central Station', availability: 'AVAILABLE' },
      { id: 2, currentLocation: 'North Station', availability: 'AVAILABLE' },
      { id: 3, currentLocation: 'South Station', availability: 'MAINTENANCE' },
      { id: 4, currentLocation: 'East Station', availability: 'AVAILABLE' },
      { id: 5, currentLocation: 'West Station', availability: 'DISPATCHED' }
    ];
    this.saveLocalAmbulances(sampleAmbulances);
    return sampleAmbulances;
  }

  saveLocalAmbulances(ambulances) {
    localStorage.setItem(this.localStoragePrefix + 'ambulances', JSON.stringify(ambulances));
  }

  saveLocalAmbulance(ambulanceData) {
    const ambulances = this.getLocalAmbulances();
    const newAmbulance = {
      id: this.generateId(),
      ...ambulanceData
    };
    ambulances.push(newAmbulance);
    this.saveLocalAmbulances(ambulances);
    return newAmbulance;
  }

  updateLocalAmbulanceStatus(ambulanceId, status) {
    const ambulances = this.getLocalAmbulances();
    const ambulance = ambulances.find(a => a.id == ambulanceId);

    if (ambulance) {
      ambulance.availability = status;
      ambulance.status = status.toLowerCase();
      this.saveLocalAmbulances(ambulances);
      return ambulance;
    }

    throw new Error('Ambulance not found');
  }

  updateLocalAmbulanceLocation(ambulanceId, location) {
    const ambulances = this.getLocalAmbulances();
    const ambulance = ambulances.find(a => a.id == ambulanceId);

    if (ambulance) {
      ambulance.currentLocation = location;
      ambulance.location = location;
      this.saveLocalAmbulances(ambulances);
      return ambulance;
    }

    throw new Error('Ambulance not found');
  }

  getLocalPatients() {
    const stored = localStorage.getItem(this.localStoragePrefix + 'patients');
    return stored ? JSON.parse(stored) : [];
  }

  saveLocalPatients(patients) {
    localStorage.setItem(this.localStoragePrefix + 'patients', JSON.stringify(patients));
  }

  findOrCreateLocalPatient(name, contact) {
    const patients = this.getLocalPatients();
    let patient = patients.find(p => p.contact === contact);

    if (!patient) {
      patient = {
        id: this.generateId(),
        name: name,
        contact: contact,
        medicalNotes: '',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
      patients.push(patient);
      this.saveLocalPatients(patients);
    }

    return patient;
  }

  dispatchLocalAmbulance(requestId) {
    const requests = this.getLocalRequests();
    const ambulances = this.getLocalAmbulances();

    const request = requests.find(r => r.id == requestId);
    if (!request) {
      throw new Error('Request not found');
    }

    const availableAmbulance = ambulances.find(a => a.availability === 'AVAILABLE');
    if (!availableAmbulance) {
      throw new Error('No ambulance available');
    }

    // Update request
    request.status = 'DISPATCHED';
    request.ambulanceId = availableAmbulance.id;
    request.dispatchTime = new Date().toISOString();

    // Update ambulance
    availableAmbulance.availability = 'DISPATCHED';
    availableAmbulance.status = 'dispatched';

    // Save changes
    this.saveLocalRequests(requests);
    this.saveLocalAmbulances(ambulances);

    return { request, ambulance: availableAmbulance };
  }

  // Statistics and analytics
  async getStatistics() {
    try {
      const [requests, ambulances, patients] = await Promise.all([
        this.getRequests(),
        this.getAmbulances(),
        this.getPatients().catch(() => [])
      ]);

      const today = new Date();
      today.setHours(0, 0, 0, 0);

      const stats = {
        total: {
          requests: requests.length,
          ambulances: ambulances.length,
          patients: patients.length
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
      };

      return stats;
    } catch (error) {
      console.error('Error getting statistics:', error);
      return this.getDefaultStats();
    }
  }

  calculateAverageResponseTime(requests) {
    const completedRequests = requests.filter(r =>
      r.status === 'COMPLETED' && r.dispatchTime && r.requestTime
    );

    if (completedRequests.length === 0) return 0;

    const totalTime = completedRequests.reduce((sum, request) => {
      const requestTime = new Date(request.requestTime);
      const dispatchTime = new Date(request.dispatchTime);
      return sum + (dispatchTime - requestTime);
    }, 0);

    return Math.round(totalTime / completedRequests.length / (1000 * 60)); // Convert to minutes
  }

  calculateCompletionRate(requests) {
    if (requests.length === 0) return 0;
    const completedRequests = requests.filter(r => r.status === 'COMPLETED');
    return Math.round((completedRequests.length / requests.length) * 100);
  }

  getDefaultStats() {
    return {
      total: { requests: 0, ambulances: 0, patients: 0 },
      today: { requests: 0, completed: 0 },
      current: { activeRequests: 0, availableAmbulances: 0, busyAmbulances: 0, maintenanceAmbulances: 0 },
      performance: { avgResponseTime: 0, completionRate: 0 }
    };
  }

  // Search and filtering
  async searchRequests(query) {
    const requests = await this.getRequests();
    const lowercaseQuery = query.toLowerCase();

    return requests.filter(request =>
      request.id.toString().includes(query) ||
      request.userName?.toLowerCase().includes(lowercaseQuery) ||
      request.userContact?.includes(query) ||
      request.location?.toLowerCase().includes(lowercaseQuery) ||
      request.emergencyDescription?.toLowerCase().includes(lowercaseQuery)
    );
  }

  async searchPatients(query) {
    const patients = await this.getPatients();
    const lowercaseQuery = query.toLowerCase();

    return patients.filter(patient =>
      patient.name?.toLowerCase().includes(lowercaseQuery) ||
      patient.contact?.includes(query) ||
      patient.medicalNotes?.toLowerCase().includes(lowercaseQuery)
    );
  }

  async filterRequestsByStatus(status) {
    const requests = await this.getRequests();
    if (status === 'all') return requests;
    return requests.filter(r => r.status === status);
  }

  async filterRequestsByDateRange(startDate, endDate) {
    const requests = await this.getRequests();
    const start = new Date(startDate);
    const end = new Date(endDate);
    end.setHours(23, 59, 59, 999);

    return requests.filter(r => {
      const requestDate = new Date(r.requestTime);
      return requestDate >= start && requestDate <= end;
    });
  }

  // Data validation
  validateRequest(requestData) {
    const required = ['userName', 'userContact', 'location', 'emergencyDescription'];
    const missing = required.filter(field => !requestData[field] || requestData[field].trim() === '');

    if (missing.length > 0) {
      return {
        valid: false,
        errors: missing.map(field => `${this.getFieldDisplayName(field)} is required`)
      };
    }

    // Phone number validation
    if (!this.validatePhoneNumber(requestData.userContact)) {
      return {
        valid: false,
        errors: ['Phone number must be 10-15 digits']
      };
    }

    return { valid: true, errors: [] };
  }

  validateAmbulance(ambulanceData) {
    const required = ['currentLocation', 'availability'];
    const missing = required.filter(field => !ambulanceData[field] || ambulanceData[field].trim() === '');

    if (missing.length > 0) {
      return {
        valid: false,
        errors: missing.map(field => `${this.getFieldDisplayName(field)} is required`)
      };
    }

    const validStatuses = ['AVAILABLE', 'DISPATCHED', 'MAINTENANCE', 'OUT_OF_SERVICE'];
    if (!validStatuses.includes(ambulanceData.availability)) {
      return {
        valid: false,
        errors: ['Status must be one of: ' + validStatuses.join(', ')]
      };
    }

    return { valid: true, errors: [] };
  }

  validatePhoneNumber(phone) {
    const phoneRegex = /^[+]?[0-9]{10,15}$/;
    return phoneRegex.test(phone.replace(/[^\d+]/g, ''));
  }

  getFieldDisplayName(field) {
    const displayNames = {
      userName: 'Patient Name',
      userContact: 'Phone Number',
      location: 'Location',
      emergencyDescription: 'Emergency Description',
      currentLocation: 'Current Location',
      availability: 'Status'
    };
    return displayNames[field] || field;
  }

  // Data export/import
  async exportData() {
    try {
      const [requests, ambulances, patients] = await Promise.all([
        this.getRequests(),
        this.getAmbulances(),
        this.getPatients().catch(() => [])
      ]);

      const exportData = {
        timestamp: new Date().toISOString(),
        version: '1.0',
        data: {
          requests,
          ambulances,
          patients
        },
        stats: await this.getStatistics()
      };

      return exportData;
    } catch (error) {
      console.error('Error exporting data:', error);
      throw error;
    }
  }

  async downloadDataAsJSON() {
    try {
      const data = await this.exportData();
      const dataStr = JSON.stringify(data, null, 2);
      const dataUri = 'data:application/json;charset=utf-8,'+ encodeURIComponent(dataStr);

      const exportFileDefaultName = `ambulance-service-data-${new Date().toISOString().split('T')[0]}.json`;

      const linkElement = document.createElement('a');
      linkElement.setAttribute('href', dataUri);
      linkElement.setAttribute('download', exportFileDefaultName);
      linkElement.click();

      console.log('Data export initiated');
    } catch (error) {
      console.error('Error downloading data:', error);
      throw new Error('Failed to export data: ' + error.message);
    }
  }

  // Cache and sync management
  clearLocalCache() {
    const keys = ['requests', 'ambulances', 'patients'];
    keys.forEach(key => {
      localStorage.removeItem(this.localStoragePrefix + key);
    });
    this.clearCache();
    console.log('Local cache cleared');
  }

  async syncWithBackend() {
    if (!this.useBackend) {
      console.log('Backend sync skipped - backend mode disabled');
      return false;
    }

    try {
      console.log('Syncing with backend...');

      // Force refresh from backend
      const [requests, ambulances, patients] = await Promise.all([
        this.apiCall('/requests'),
        this.apiCall('/ambulances'),
        this.apiCall('/patients').catch(() => [])
      ]);

      // Update local cache
      this.saveLocalRequests(requests);
      this.saveLocalAmbulances(ambulances);
      this.saveLocalPatients(patients);

      // Clear memory cache to force fresh data
      this.clearCache();

      console.log('Data synchronized with backend successfully');
      return true;
    } catch (error) {
      console.error('Error syncing with backend:', error);
      return false;
    }
  }

  // Real-time updates
  async pollForUpdates(callback, interval = 30000) {
    const poll = async () => {
      try {
        const stats = await this.getStatistics();
        callback(stats);
      } catch (error) {
        console.error('Error polling for updates:', error);
      }
    };

    // Initial call
    poll();

    // Set up interval
    const intervalId = setInterval(poll, interval);
    console.log(`Started polling for updates every ${interval}ms`);

    return intervalId;
  }

  stopPolling(intervalId) {
    if (intervalId) {
      clearInterval(intervalId);
      console.log('Stopped polling for updates');
    }
  }

  // Utility functions
  generateId() {
    return Date.now() + Math.floor(Math.random() * 1000);
  }

  formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleString();
    } catch (error) {
      return 'Invalid Date';
    }
  }

  formatTime(dateString) {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch (error) {
      return 'Invalid Time';
    }
  }
}

// Create global data manager instance
const dataManager = new DataManager();

// Legacy compatibility functions
async function getRequests() {
  return await dataManager.getRequests();
}

async function getAmbulances() {
  return await dataManager.getAmbulances();
}

async function getPendingRequests() {
  return await dataManager.getPendingRequests();
}

async function createRequest(requestData) {
  return await dataManager.createRequest(requestData);
}

async function updateRequestStatus(requestId, status) {
  return await dataManager.updateRequestStatus(requestId, status);
}

async function dispatchAmbulance(requestId) {
  return await dataManager.dispatchAmbulance(requestId);
}

async function getPatients() {
  return await dataManager.getPatients();
}

async function getStatistics() {
  return await dataManager.getStatistics();
}

// Utility functions
function generateRequestId() {
  return 'REQ-' + Date.now().toString().slice(-6);
}

function generateAmbulanceId() {
  return 'AMB-' + Date.now().toString().slice(-6);
}

function formatDateTime(dateString) {
  return dataManager.formatDateTime(dateString);
}

function formatTime(dateString) {
  return dataManager.formatTime(dateString);
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
    getPatients,
    getStatistics,
    generateRequestId,
    generateAmbulanceId,
    formatDateTime,
    formatTime
  };
}

console.log('Data manager loaded successfully');