// Load data when page loads
document.addEventListener('DOMContentLoaded', () => {
    loadAmbulances();
    loadPendingRequests();
    loadAllRequests();
});

async function loadAmbulances() {
    try {
        const response = await fetch('/api/ambulances', {
            headers: getAuthHeader()
        });

        console.log('Ambulances response status:', response.status);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const text = await response.text();

        if (!text) {
            throw new Error('Empty response from server');
        }

        const ambulances = JSON.parse(text);

        const container = document.getElementById('ambulanceList');
        let html = '<table class="data-table"><thead><tr><th>ID</th><th>Location</th><th>Status</th></tr></thead><tbody>';

        if (Array.isArray(ambulances) && ambulances.length > 0) {
            ambulances.forEach(amb => {
                html += `<tr>
                    <td>${amb.id || 'N/A'}</td>
                    <td>${amb.currentLocation || 'N/A'}</td>
                    <td class="${amb.availability ? 'status-available' : 'status-busy'}">
                        ${amb.availability ? 'Available' : 'On Duty'}
                    </td>
                </tr>`;
            });
        } else {
            html += '<tr><td colspan="3">No ambulances found</td></tr>';
        }

        html += '</tbody></table>';
        container.innerHTML = html;
        container.classList.remove('loading');
    } catch (error) {
        console.error('Error loading ambulances:', error);
        document.getElementById('ambulanceList').innerHTML =
            `<p class="error">Error loading ambulances: ${error.message}. Please check the console for details.</p>`;
    }
}

async function loadPendingRequests() {
    try {
        const response = await fetch('/api/requests/pending', {
            headers: getAuthHeader()
        });

        console.log('Pending requests response status:', response.status);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const text = await response.text();

        if (!text) {
            throw new Error('Empty response from server');
        }

        const requests = JSON.parse(text);
        const container = document.getElementById('pendingRequests');

        if (Array.isArray(requests) && requests.length > 0) {
            let html = '<table class="data-table"><thead><tr><th>ID</th><th>Name</th><th>Contact</th><th>Location</th><th>Request Time</th><th>Action</th></tr></thead><tbody>';

            requests.forEach(req => {
                html += `<tr>
                    <td>${req.id || 'N/A'}</td>
                    <td>${req.userName || 'N/A'}</td>
                    <td>${req.userContact || 'N/A'}</td>
                    <td>${req.location || 'N/A'}</td>
                    <td>${req.requestTime ? new Date(req.requestTime).toLocaleString() : 'N/A'}</td>
                    <td>
                        <button class="btn btn-dispatch" onclick="dispatchAmbulance(${req.id})">
                            Dispatch
                        </button>
                    </td>
                </tr>`;
            });

            html += '</tbody></table>';
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p>No pending requests at the moment.</p>';
        }

        container.classList.remove('loading');
    } catch (error) {
        console.error('Error loading pending requests:', error);
        document.getElementById('pendingRequests').innerHTML =
            `<p class="error">Error loading pending requests: ${error.message}. Please check the console for details.</p>`;
    }
}

async function loadAllRequests() {
    try {
        const response = await fetch('/api/requests', {
            headers: getAuthHeader()
        });

        console.log('All requests response status:', response.status);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const text = await response.text();

        if (!text) {
            throw new Error('Empty response from server');
        }

        const requests = JSON.parse(text);
        const container = document.getElementById('allRequests');

        if (Array.isArray(requests) && requests.length > 0) {
            let html = '<table class="data-table"><thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Request Time</th><th>Dispatch Time</th><th>Ambulance</th><th>Status</th></tr></thead><tbody>';

            requests.forEach(req => {
                const statusClass = req.status ? `status-${req.status.toLowerCase()}` : '';
                html += `<tr>
                    <td>${req.id || 'N/A'}</td>
                    <td>${req.userName || 'N/A'}</td>
                    <td>${req.location || 'N/A'}</td>
                    <td>${req.requestTime ? new Date(req.requestTime).toLocaleString() : 'N/A'}</td>
                    <td>${req.dispatchTime ? new Date(req.dispatchTime).toLocaleString() : '-'}</td>
                    <td>${req.ambulance ? req.ambulance.id : '-'}</td>
                    <td class="${statusClass}">${req.status || 'N/A'}</td>
                </tr>`;
            });

            html += '</tbody></table>';
            container.innerHTML = html;
        } else {
            container.innerHTML = '<p>No request history found.</p>';
        }

        container.classList.remove('loading');
    } catch (error) {
        console.error('Error loading all requests:', error);
        document.getElementById('allRequests').innerHTML =
            `<p class="error">Error loading request history: ${error.message}. Please check the console for details.</p>`;
    }
}

function getAuthHeader() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return {};
    }
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
}

async function dispatchAmbulance(requestId) {
    if (!confirm('Are you sure you want to dispatch an ambulance for this request?')) {
        return;
    }

    try {
        console.log(`Dispatching ambulance for request ${requestId}...`);
        const response = await fetch(`/api/dispatch/${requestId}`, {
            method: 'POST',
            headers: getAuthHeader()
        });

        console.log('Dispatch response status:', response.status);

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Failed to dispatch ambulance');
        }

        const result = await response.json();

        const messageDiv = document.getElementById('message');
        messageDiv.className = 'alert alert-success';
        messageDiv.textContent = 'Ambulance dispatched successfully!';
        messageDiv.style.display = 'block';

        // Reload all data
        loadAmbulances();
        loadPendingRequests();
        loadAllRequests();

        // Hide message after 5 seconds
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    } catch (error) {
        console.error('Error dispatching ambulance:', error);
        const messageDiv = document.getElementById('message');
        messageDiv.className = 'alert alert-error';
        messageDiv.textContent = `Error: ${error.message}`;
        messageDiv.style.display = 'block';

        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    }
}