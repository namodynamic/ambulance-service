// Dashboard functionality
let currentPage = 1;
const ITEMS_PER_PAGE = 10;

// Initialize dashboard when page loads
document.addEventListener('DOMContentLoaded', () => {
    // Check if we're on the dashboard page
    if (document.getElementById('user-dashboard')) {
        initializeDashboard();
    }
});

// Initialize dashboard
function initializeDashboard() {
    updateUserGreeting();
    loadActiveRequests();
    loadRequestHistory();

    // Set up pagination event listeners
    document.getElementById('prev-page')?.addEventListener('click', () => {
        if (currentPage > 1) {
            currentPage--;
            loadRequestHistory();
        }
    });

    document.getElementById('next-page')?.addEventListener('click', () => {
        currentPage++;
        loadRequestHistory();
    });
}

// Update the greeting with the user's name
function updateUserGreeting() {
    const userData = JSON.parse(localStorage.getItem('user') || '{}');
    const greetingElement = document.getElementById('user-greeting');
    if (greetingElement && userData.username) {
        // Capitalize first letter of username
        const displayName = userData.username.charAt(0).toUpperCase() + userData.username.slice(1);
        greetingElement.textContent = displayName;
    }
}

// Load active requests for the current user
async function loadActiveRequests() {
    const container = document.getElementById('active-requests');
    if (!container) return;

    try {
        // Show loading state
        container.innerHTML = '<div class="loading">Loading active requests...</div>';

        // Get user's active requests from the API
        const response = await apiCall('/api/requests/active');

        if (response.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <p>No active requests</p>
                    <button class="btn btn-primary" onclick="showPage('request-page')">
                        Request Emergency Service
                    </button>
                </div>
            `;
            return;
        }

        // Render active requests
        container.innerHTML = response.map(request => `
            <div class="request-card" data-request-id="${request.id}">
                <div class="request-header">
                    <h3>Request #${request.id}</h3>
                    <span class="status-badge ${request.status.toLowerCase()}">
                        ${request.status.replace('_', ' ')}
                    </span>
                </div>
                <div class="request-details">
                    <div class="detail">
                        <span class="label">Type:</span>
                        <span class="value">${request.emergencyType || 'Emergency'}</span>
                    </div>
                    <div class="detail">
                        <span class="label">Location:</span>
                        <span class="value">${request.address || 'N/A'}</span>
                    </div>
                    <div class="detail">
                        <span class="label">Requested:</span>
                        <span class="value">${new Date(request.createdAt).toLocaleString()}</span>
                    </div>
                </div>
                <div class="request-actions">
                    <button class="btn btn-sm btn-primary"
                            onclick="showRequestDetails('${request.id}')">
                        Track Status
                    </button>
                </div>
            </div>
        `).join('');

    } catch (error) {
        console.error('Error loading active requests:', error);
        container.innerHTML = `
            <div class="error-state">
                <p>Error loading active requests. Please try again later.</p>
                <button class="btn btn-ghost" onclick="loadActiveRequests()">
                    Retry
                </button>
            </div>
        `;
    }
}

// Load request history with pagination
async function loadRequestHistory() {
    const tbody = document.querySelector('#request-history tbody');
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');
    const pageInfo = document.getElementById('page-info');
    const filter = document.getElementById('history-filter')?.value || 'all';

    if (!tbody) return;

    try {
        // Show loading state
        tbody.innerHTML = '<tr><td colspan="5" class="loading">Loading request history...</td></tr>';

        // Build query parameters
        const params = new URLSearchParams({
            page: currentPage,
            limit: ITEMS_PER_PAGE,
            filter
        });

        // Get request history from API
        const response = await apiCall(`/api/requests/history?${params}`);

        if (response.requests.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="5" class="empty-state">
                        No request history found
                    </td>
                </tr>
            `;
            return;
        }

        // Render request history
        tbody.innerHTML = response.requests.map(request => {
            const statusClass = request.status.toLowerCase().replace('_', '-');
            const formattedDate = new Date(request.createdAt).toLocaleDateString();

            return `
                <tr>
                    <td>#${request.id}</td>
                    <td>${formattedDate}</td>
                    <td>
                        <span class="status-badge ${statusClass}">
                            ${request.status.replace('_', ' ')}
                        </span>
                    </td>
                    <td>${request.emergencyType || 'Emergency'}</td>
                    <td class="actions">
                        <button class="btn btn-sm btn-ghost"
                                onclick="showRequestDetails('${request.id}')">
                            View Details
                        </button>
                    </td>
                </tr>
            `;
        }).join('');

        // Update pagination controls
        updatePaginationControls(response.total, currentPage, ITEMS_PER_PAGE);

    } catch (error) {
        console.error('Error loading request history:', error);
        tbody.innerHTML = `
            <tr>
                <td colspan="5" class="error-state">
                    Error loading request history.
                    <button class="btn btn-xs" onclick="loadRequestHistory()">
                        Retry
                    </button>
                </td>
            </tr>
        `;
    }
}

// Update pagination controls
function updatePaginationControls(totalItems, currentPage, itemsPerPage) {
    const totalPages = Math.ceil(totalItems / itemsPerPage);
    const prevBtn = document.getElementById('prev-page');
    const nextBtn = document.getElementById('next-page');
    const pageInfo = document.getElementById('page-info');

    if (prevBtn) prevBtn.disabled = currentPage <= 1;
    if (nextBtn) nextBtn.disabled = currentPage >= totalPages;
    if (pageInfo) pageInfo.textContent = `Page ${currentPage} of ${totalPages || 1}`;
}

// Show request details in a modal or redirect to details page
function showRequestDetails(requestId) {
    // Store the request ID in session storage
    sessionStorage.setItem('currentRequestId', requestId);

    // Show the tracking page (you'll need to implement this page)
    showPage('confirmation-page');

    // Start tracking the request status
    trackRequestStatus(requestId);
}

// Export functions to the global scope
window.loadRequestHistory = loadRequestHistory;
window.showRequestDetails = showRequestDetails;
