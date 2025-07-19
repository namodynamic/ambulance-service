        // Check authentication and role on page load
        document.addEventListener('DOMContentLoaded', () => {
            const token = localStorage.getItem('token');
            const role = localStorage.getItem('role');
            const username = localStorage.getItem('username');

            // Set username if available
            if (username) {
                document.getElementById('username').textContent = username;
            }

            // Redirect to login if not authenticated
            if (!token) {
                window.location.href = '/login.html';
                return;
            }

            // Check if user has admin or dispatcher role
            if (role !== 'ROLE_ADMIN' && role !== 'ROLE_DISPATCHER') {
                document.getElementById('unauthorized-message').style.display = 'block';
                document.querySelector('.container').style.display = 'none';

                // Redirect to appropriate page after delay
                setTimeout(() => {
                    window.location.href = role === 'ROLE_USER' ? '/request-ambulance.html' : '/login.html';
                }, 3000);
                return;
            }

            // Load data if authorized
            loadAmbulances();
            loadPendingRequests();
            loadAllRequests();

            // Set up auto-refresh every 30 seconds
            setInterval(() => {
                loadAmbulances();
                loadPendingRequests();
                loadAllRequests();
            }, 30000);
        });

        function logout() {
            // Clear local storage
            localStorage.removeItem('token');
            localStorage.removeItem('username');
            localStorage.removeItem('role');

            // Redirect to login page
            window.location.href = '/login.html';
        }
