// Load data when page loads
document.addEventListener('DOMContentLoaded', () => {
    loadAmbulances();
    loadPendingRequests();
    loadAllRequests();
});

async function loadAmbulances() {
    try {
        const response = await fetch('/api/ambulances');
        const ambulances = await response.json();
        
        const container = document.getElementById('ambulanceList');
        let html = '<table class="data-table"><thead><tr><th>ID</th><th>Location</th><th>Status</th></tr></thead><tbody>';
        
        ambulances.forEach(amb => {
            html += `<tr>
                <td>${amb.id}</td>
                <td>${amb.currentLocation}</td>
                <td class="${amb.availability ? 'status-available' : 'status-busy'}">
                    ${amb.availability ? 'Available' : 'On Duty'}
                </td>
            </tr>`;
        });
        
        html += '</tbody></table>';
        container.innerHTML = html;
        container.classList.remove('loading');
    } catch (error) {
        document.getElementById('ambulanceList').innerHTML = `<p class="error">Error loading ambulances: ${error.message}</p>`;
    }
}

async function loadPendingRequests() {
    try {
        const response = await fetch('/api/requests/pending');
        const requests = await response.json();
        
        const container = document.getElementById('pendingRequests');
        
        if (requests.length === 0) {
            container.innerHTML = '<p>No pending requests at the moment.</p>';
        } else {
            let html = '<table class="data-table"><thead><tr><th>ID</th><th>Name</th><th>Contact</th><th>Location</th><th>Request Time</th><th>Action</th></tr></thead><tbody>';
            
            requests.forEach(req => {
                html += `<tr>
                    <td>${req.id}</td>
                    <td>${req.userName}</td>
                    <td>${req.userContact}</td>
                    <td>${req.location}</td>
                    <td>${new Date(req.requestTime).toLocaleString()}</td>
                    <td>
                        <button class="btn btn-dispatch" onclick="dispatchAmbulance(${req.id})">
                            Dispatch
                        </button>
                    </td>
                </tr>`;
            });
            
            html += '</tbody></table>';
            container.innerHTML = html;
        }
        container.classList.remove('loading');
    } catch (error) {
        document.getElementById('pendingRequests').innerHTML = `<p class="error">Error loading pending requests: ${error.message}</p>`;
    }
}

async function loadAllRequests() {
    try {
        const response = await fetch('/api/requests');
        const requests = await response.json();
        
        const container = document.getElementById('allRequests');
        let html = '<table class="data-table"><thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Request Time</th><th>Dispatch Time</th><th>Ambulance</th><th>Status</th></tr></thead><tbody>';
        
        requests.forEach(req => {
            html += `<tr>
                <td>${req.id}</td>
                <td>${req.userName}</td>
                <td>${req.location}</td>
                <td>${new Date(req.requestTime).toLocaleString()}</td>
                <td>${req.dispatchTime ? new Date(req.dispatchTime).toLocaleString() : '-'}</td>
                <td>${req.ambulance ? req.ambulance.id : '-'}</td>
                <td class="status-${req.status.toLowerCase()}">${req.status}</td>
            </tr>`;
        });
        
        html += '</tbody></table>';
        container.innerHTML = html;
        container.classList.remove('loading');
    } catch (error) {
        document.getElementById('allRequests').innerHTML = `<p class="error">Error loading requests: ${error.message}</p>`;
    }
}

async function dispatchAmbulance(requestId) {
    if (!confirm('Are you sure you want to dispatch an ambulance for this request?')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/dispatch/${requestId}`, {
            method: 'POST'
        });
        
        const messageDiv = document.getElementById('message');
        
        if (response.ok) {
            messageDiv.className = 'alert alert-success';
            messageDiv.textContent = 'Ambulance dispatched successfully!';
            messageDiv.style.display = 'block';
            
            // Reload all data
            loadAmbulances();
            loadPendingRequests();
            loadAllRequests();
        } else {
            const error = await response.json();
            messageDiv.className = 'alert alert-error';
            messageDiv.textContent = `Error: ${error.error}`;
            messageDiv.style.display = 'block';
        }
        
        // Hide message after 5 seconds
        setTimeout(() => {
            messageDiv.style.display = 'none';
        }, 5000);
    } catch (error) {
        alert(`Error dispatching ambulance: ${error.message}`);
    }
}