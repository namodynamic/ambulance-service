// user-dashboard.js
// Global variables
let currentPage = 0;
const pageSize = 10;

// DOM Elements
const activeRequestsContainer = document.getElementById('active-requests-container');
const requestHistoryContainer = document.getElementById('request-history-container');
const paginationContainer = document.getElementById('pagination-controls');
const refreshButton = document.getElementById('refresh-requests');
const userEmailElement = document.getElementById('user-email');

// Initialize the dashboard when the page loads
document.addEventListener('DOMContentLoaded', function() {
    // Check if user is logged in
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = 'index.html#login';
        return;
    }

    // Set up event listeners
    refreshButton.addEventListener('click', loadDashboardData);

    // Load initial data
    loadDashboardData();

    // Set up periodic refresh every 30 seconds
    setInterval(loadDashboardData, 30000);
});

// Load dashboard data
async function loadDashboardData() {
    try {
        // Show loading state
        activeRequestsContainer.innerHTML = '<div class="loading">Loading active requests...</div>';
        requestHistoryContainer.innerHTML = '<div class="loading">Loading request history...</div>';

        // Load active requests and history in parallel
        await Promise.all([
            loadActiveRequests(),
            loadRequestHistory(currentPage)
        ]);
    } catch (error) {
        console.error('Error loading dashboard data:', error);
        showError('Failed to load dashboard data. Please try again.');
    }
}

// Load active requests for the logged-in user
async function loadActiveRequests() {
    try {
        const response = await fetch('/api/requests/user/active', {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        displayActiveRequests(data.content);
    } catch (error) {
        console.error('Error loading active requests:', error);
        activeRequestsContainer.innerHTML = `
            <div class="error">
                Failed to load active requests.
                <button onclick="loadActiveRequests()">Retry</button>
            </div>`;
    }
}

// Load request history with pagination
async function loadRequestHistory(page) {
    try {
        const response = await fetch(`/api/requests/user/history?page=${page}&size=${pageSize}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        displayRequestHistory(data.content, data.totalPages, data.number);
    } catch (error) {
        console.error('Error loading request history:', error);
        requestHistoryContainer.innerHTML = `
            <div class="error">
                Failed to load request history.
                <button onclick="loadRequestHistory(${currentPage})">Retry</button>
            </div>`;
    }
}

// Display active requests in the UI
function displayActiveRequests(requests) {
    if (!requests || requests.length === 0) {
        activeRequestsContainer.innerHTML = '<div class="no-requests">No active requests found.</div>';
        return;
    }

    const html = `
        <div class="requests-grid">
            ${requests.map(request => `
                <div class="request-card" data-id="${request.id}">
                    <h4>${request.userName || 'Unknown User'}</h4>
                    <p><strong>Status:</strong> <span class="status ${request.status.toLowerCase()}">${formatStatus(request.status)}</span></p>
                    <p><strong>Location:</strong> ${request.location}</p>
                    <p><strong>Requested:</strong> ${formatDate(request.requestTime)}</p>
                    <button class="btn btn-view" onclick="viewRequestDetails(${request.id})">View Details</button>
                </div>
            `).join('')}
        </div>`;

    activeRequestsContainer.innerHTML = html;
}

// Display request history with pagination
function displayRequestHistory(requests, totalPages, currentPage) {
    if (!requests || requests.length === 0) {
        requestHistoryContainer.innerHTML = '<div class="no-requests">No request history found.</div>';
        paginationContainer.innerHTML = '';
        return;
    }

    // Display requests
    const html = `
        <div class="requests-list">
            ${requests.map(request => `
                <div class="request-item" data-id="${request.id}">
                    <div class="request-header">
                        <span class="request-id">#${request.id}</span>
                        <span class="status ${request.status.toLowerCase()}">${formatStatus(request.status)}</span>
                    </div>
                    <div class="request-details">
                        <p><strong>Location:</strong> ${request.location}</p>
                        <p><strong>Requested:</strong> ${formatDate(request.requestTime)}</p>
                        ${request.completionTime ? `<p><strong>Completed:</strong> ${formatDate(request.completionTime)}</p>` : ''}
                    </div>
                    <button class="btn btn-view" onclick="viewRequestDetails(${request.id})">View</button>
                </div>
            `).join('')}
        </div>`;

    requestHistoryContainer.innerHTML = html;

    // Update pagination controls
    updatePagination(totalPages, currentPage);
}

// Update pagination controls
function updatePagination(totalPages, currentPage) {
    let paginationHTML = '<div class="pagination">';

    // Previous button
    if (currentPage > 0) {
        paginationHTML += `<button onclick="changePage(${currentPage - 1})">&laquo; Previous</button>`;
    }

    // Page numbers
    for (let i = 0; i < totalPages; i++) {
        if (i === currentPage) {
            paginationHTML += `<span class="current-page">${i + 1}</span>`;
        } else {
            paginationHTML += `<button onclick="changePage(${i})">${i + 1}</button>`;
        }
    }

    // Next button
    if (currentPage < totalPages - 1) {
        paginationHTML += `<button onclick="changePage(${currentPage + 1})">Next &raquo;</button>`;
    }

    paginationHTML += '</div>';
    paginationContainer.innerHTML = paginationHTML;
}

// Change page in pagination
function changePage(page) {
    currentPage = page;
    loadRequestHistory(currentPage);
}

// View detailed request information
async function viewRequestDetails(requestId) {
    try {
        const response = await fetch(`/api/requests/user/${requestId}`, {
            headers: {
                'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const request = await response.json();
        showRequestModal(request);
    } catch (error) {
        console.error('Error loading request details:', error);
        showError('Failed to load request details. Please try again.');
    }
}

// Show request details in a modal
function showRequestModal(request) {
    // Format status history
    const statusHistory = request.statusHistory
        ? request.statusHistory.map(history => `
            <div class="status-history-item">
                <span class="status ${history.newStatus.toLowerCase()}">${formatStatus(history.newStatus)}</span>
                <span class="status-time">${formatDate(history.timestamp)}</span>
                <p class="status-notes">${history.notes || 'No notes'}</p>
                <p class="status-changed-by">Changed by: ${history.changedBy}</p>
            </div>
        `).join('')
        : '<p>No status history available</p>';

    // Create modal HTML
    const modalHTML = `
        <div class="modal-overlay" id="requestModal">
            <div class="modal-content">
                <div class="modal-header">
                    <h3>Request #${request.id}</h3>
                    <button class="close-modal" onclick="closeModal()">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="request-details">
                        <p><strong>Status:</strong> <span class="status ${request.status.toLowerCase()}">${formatStatus(request.status)}</span></p>
                        <p><strong>Name:</strong> ${request.userName || 'N/A'}</p>
                        <p><strong>Contact:</strong> ${request.userContact || 'N/A'}</p>
                        <p><strong>Location:</strong> ${request.location}</p>
                        <p><strong>Request Time:</strong> ${formatDate(request.requestTime)}</p>
                        ${request.dispatchTime ? `<p><strong>Dispatch Time:</strong> ${formatDate(request.dispatchTime)}</p>` : ''}
                        ${request.emergencyDescription ? `<p><strong>Description:</strong> ${request.emergencyDescription}</p>` : ''}

                        <div class="status-history">
                            <h4>Status History</h4>
                            ${statusHistory}
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-secondary" onclick="closeModal()">Close</button>
                </div>
            </div>
        </div>`;

    // Add modal to the page
    document.body.insertAdjacentHTML('beforeend', modalHTML);

    // Add event listener for clicking outside modal
    document.getElementById('requestModal').addEventListener('click', function(e) {
        if (e.target === this) {
            closeModal();
        }
    });
}

// Close modal
function closeModal() {
    const modal = document.getElementById('requestModal');
    if (modal) {
        modal.remove();
    }
}

// Helper function to format status for display
function formatStatus(status) {
    if (!status) return '';
    return status
        .toLowerCase()
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

// Helper function to format date
function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const options = {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    return new Date(dateString).toLocaleString(undefined, options);
}

// Show error message
function showError(message) {
    const errorDiv = document.createElement('div');
    errorDiv.className = 'alert alert-error';
    errorDiv.textContent = message;

    // Add to the top of the page
    const mainContent = document.querySelector('main');
    if (mainContent) {
        mainContent.insertBefore(errorDiv, mainContent.firstChild);
    } else {
        document.body.insertBefore(errorDiv, document.body.firstChild);
    }

    // Remove after 5 seconds
    setTimeout(() => {
        errorDiv.remove();
    }, 5000);
}

// Logout function
function logout() {
    localStorage.removeItem('token');
    window.location.href = 'index.html';
}