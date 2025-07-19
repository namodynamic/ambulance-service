// Check if user is logged in and has the correct role for the current page
function checkAuth() {
    const token = localStorage.getItem('token');
    const role = localStorage.getItem('role');
    const currentPath = window.location.pathname;

    // If no token and not on login/register page, redirect to login
    if (!token && !['/login.html', '/register.html', '/'].includes(currentPath)) {
        window.location.href = '/login.html';
        return false;
    }

    // If user is on admin dashboard but not an admin/dispatcher, redirect
    if (currentPath === '/admin-dashboard.html' && !['ROLE_ADMIN', 'ROLE_DISPATCHER'].includes(role)) {
        window.location.href = role ? '/request-ambulance.html' : '/login.html';
        return false;
    }

    // If user is on request-ambulance page but not logged in, redirect to login
    if (currentPath === '/request-ambulance.html' && !token) {
        window.location.href = '/login.html';
        return false;
    }

    // If token exists and user is on login/register page, redirect based on role
    if (token && ['/login.html', '/register.html', '/', '/index.html'].includes(currentPath)) {
        if (role === 'ROLE_ADMIN' || role === 'ROLE_DISPATCHER') {
            window.location.href = '/admin-dashboard.html';
        } else {
            window.location.href = '/request-ambulance.html';
        }
    }

    return !!token;
}

// Add auth header to API calls
function getAuthHeader() {
    const token = localStorage.getItem('token');
    if (!token) {
        window.location.href = '/login.html';
        return {};
    }
    return { 'Authorization': 'Bearer ' + token };
}

// Handle login form
if (document.getElementById('loginForm')) {
    document.getElementById('loginForm').addEventListener('submit', async (e) => {
        e.preventDefault();

        const loginData = {
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(loginData)
            });

            const messageDiv = document.getElementById('message');

            if (response.ok) {
                const data = await response.json();

                // Store token and user info
                localStorage.setItem('token', data.token);
                localStorage.setItem('username', data.username);
                localStorage.setItem('role', data.role);

                messageDiv.className = 'alert alert-success';
                messageDiv.textContent = 'Login successful! Redirecting...';
                messageDiv.style.display = 'block';

                // Redirect based on role
                setTimeout(() => {
                    if (data.role === 'ROLE_ADMIN' || data.role === 'ROLE_DISPATCHER') {
                        window.location.href = '/admin-dashboard.html';
                    } else {
                        window.location.href = '/request-ambulance.html';
                    }
                }, 1000);
            } else {
                const error = await response.json();
                messageDiv.className = 'alert alert-error';
                messageDiv.textContent = error.error || 'Login failed';
                messageDiv.style.display = 'block';
            }
        } catch (error) {
            const messageDiv = document.getElementById('message');
            messageDiv.className = 'alert alert-error';
            messageDiv.textContent = 'Network error: ' + error.message;
            messageDiv.style.display = 'block';
        }
    });
}

// Logout function
function logout() {
    // Clear all auth data
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');

    // Redirect to login page
    window.location.href = '/login.html';
}

// Run checkAuth when the page loads
document.addEventListener('DOMContentLoaded', checkAuth);