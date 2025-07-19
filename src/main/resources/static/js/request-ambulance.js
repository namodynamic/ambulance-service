        document.addEventListener('DOMContentLoaded', function() {
            // Check authentication
            const token = localStorage.getItem('token');
            const role = localStorage.getItem('role');

            if (!token) {
                window.location.href = '/login.html';
                return;
            }

            // Set username
            const username = localStorage.getItem('username');
            if (username) {
                document.getElementById('username-display').textContent = username;
            }

            // Setup logout button
            document.getElementById('logoutBtn').addEventListener('click', function() {
                // Clear auth data
                localStorage.removeItem('token');
                localStorage.removeItem('username');
                localStorage.removeItem('role');

                // Redirect to login
                window.location.href = '/login.html';
            });

            // Setup form submission
            const requestForm = document.getElementById('requestForm');
            if (requestForm) {
                requestForm.addEventListener('submit', async function(e) {
                    e.preventDefault();

                    const statusMessage = document.getElementById('statusMessage');
                    statusMessage.style.display = 'none';

                    // Get form values
                    const userName = document.getElementById('patientName').value;
                    const userContact = document.getElementById('phoneNumber').value;
                    const location = document.getElementById('location').value;
                    const emergencyType = document.getElementById('emergencyType').value;
                    const notes = document.getElementById('notes').value;

                    // Basic validation
                    if (!userName || !userContact || !location) {
                        showError('Please fill in all required fields');
                        return;
                    }

                    // Validate phone number format (10 digits)
                    const phoneRegex = /^\d{10}$/;
                    if (!phoneRegex.test(userContact)) {
                        showError('Please enter a valid 10-digit phone number');
                        return;
                    }

                    try {
                        const response = await fetch('/api/requests', {
                            method: 'POST',
                            headers: {
                                'Content-Type': 'application/json',
                                ...getAuthHeader()
                            },
                            body: JSON.stringify({
                                userName: userName,
                                userContact: userContact,
                                location: location,
                                emergencyType: emergencyType,
                                notes: notes
                            })
                        });

                        if (response.status === 401 || response.status === 403) {
                            window.location.href = '/login.html';
                            return;
                        }

                        const result = await response.json();

                        if (response.ok) {
                            // Show success message
                            statusMessage.textContent = 'Your ambulance request has been received and is being processed.';
                            statusMessage.className = 'status-message alert-success';
                            statusMessage.style.display = 'block';

                            // Reset form
                            requestForm.reset();

                            // Show request status
                            updateRequestStatus(result);
                        } else {
                            throw new Error(result.error || 'Failed to submit request');
                        }
                    } catch (error) {
                        console.error('Error submitting request:', error);
                        showError('Error: ' + (error.message || 'Failed to submit request'));
                    }
                });
            }

            // Helper function to show error messages
            function showError(message) {
                const statusMessage = document.getElementById('statusMessage');
                statusMessage.textContent = message;
                statusMessage.className = 'status-message alert-error';
                statusMessage.style.display = 'block';

                // Scroll to the error message
                statusMessage.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }

            // Load any existing requests
            loadUserRequests();
        });

        function getAuthHeader() {
            const token = localStorage.getItem('token');
            if (!token) {
                window.location.href = '/login.html';
                return {};
            }
            return { 'Authorization': 'Bearer ' + token };
        }

        async function loadUserRequests() {
            try {
                const response = await fetch('/api/requests/my-requests', {
                    headers: getAuthHeader()
                });

                if (response.status === 200) {
                    const requests = await response.json();
                    if (requests.length > 0) {
                        updateRequestStatus(requests[0]); // Show the most recent request
                    }
                }
            } catch (error) {
                console.error('Error loading user requests:', error);
            }
        }

        function updateRequestStatus(request) {
            const statusSection = document.getElementById('requestStatus');
            const statusContent = document.getElementById('statusContent');

            if (!request) {
                statusSection.style.display = 'none';
                return;
            }

            let statusHtml = `
                <div class="status-info">
                    <p><strong>Request ID:</strong> ${request.id}</p>
                    <p><strong>Status:</strong> <span class="status-badge ${request.status.toLowerCase()}">${request.status}</span></p>
                    <p><strong>Requested At:</strong> ${new Date(request.createdAt).toLocaleString()}</p>
                </div>
            `;

            if (request.assignedAmbulance) {
                statusHtml += `
                    <div class="ambulance-info">
                        <h3>Assigned Ambulance</h3>
                        <p><strong>Ambulance ID:</strong> ${request.assignedAmbulance.id}</p>
                        <p><strong>Driver:</strong> ${request.assignedAmbulance.driverName || 'N/A'}</p>
                        <p><strong>ETA:</strong> ${request.assignedAmbulance.eta || 'Calculating...'}</p>
                    </div>
                `;
            }

            statusContent.innerHTML = statusHtml;
            statusSection.style.display = 'block';
        }