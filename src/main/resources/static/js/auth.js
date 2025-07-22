    // Auth module for handling authentication

// Store the original showPage function
const originalShowPage = window.showPage;

// Override showPage to handle auth checks
window.showPage = function(pageId) {
  console.log(`Auth: Showing page ${pageId}`);

  // Check for protected pages
  const protectedPages = ['admin-dashboard', 'profile'];
  const isProtected = protectedPages.includes(pageId);

  // Check authentication status
  const isAuthenticated = checkAuthStatus();

  if (isProtected && !isAuthenticated) {
    console.log('Auth: Redirecting to login');
    // Redirect to login page
    if (originalShowPage) {
      originalShowPage('login');
    } else {
      console.error('Original showPage function not found');
    }
    return;
  }

  // Handle admin dashboard initialization
  if (pageId === 'admin-dashboard' && isAuthenticated) {
    // Check if user has admin role
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const userRole = user.role || '';

    // Check for both 'ADMIN' and 'ROLE_ADMIN' role formats
    if (userRole !== 'ADMIN' && !userRole.endsWith('_ADMIN')) {
      console.error('Access denied: Admin role required');
      showNotification('Access denied: Admin privileges required', 'error');
      return;
    }

    // Initialize admin dashboard if functions are available
    if (typeof initializeAdminDashboard === 'function') {
      // Call the original showPage first
      if (originalShowPage) {
        originalShowPage(pageId);
      }

      // Then initialize the admin dashboard
      setTimeout(() => {
        initializeAdminDashboard();
      }, 0);
      return;
    } else {
      console.error('Admin functions not loaded');
      showNotification('Error loading admin interface', 'error');
      return;
    }
  }

  // For all other pages, use the original showPage
  if (originalShowPage) {
    originalShowPage(pageId);
  } else {
    console.error('Original showPage function not found');
  }
};

// Check if user is authenticated
function checkAuthStatus() {
  const token = localStorage.getItem('token');
  if (!token) return false;

  // TODO: Add token validation/expiration check
  return true;
}

// Handle login form submission
function handleLogin(event) {
  event.preventDefault();

  const form = event.target;
  const formData = new FormData(form);
  const credentials = {
    email: formData.get('email'),
    password: formData.get('password')
  };

  // Call your authentication API here
  console.log('Login attempt with:', credentials);

  // For demo purposes, simulate successful login
  setTimeout(() => {
    const mockUser = {
      id: '123',
      email: credentials.email,
      name: 'Admin User',
      role: 'ADMIN'
    };

    localStorage.setItem('token', 'mock-jwt-token');
    localStorage.setItem('user', JSON.stringify(mockUser));

    showNotification('Login successful!', 'success');

    // Redirect to dashboard
    if (typeof showPage === 'function') {
      showPage('admin-dashboard');
    } else {
      window.location.href = 'index.html#admin-dashboard';
    }
  }, 500);
}

// Handle logout
function handleLogout() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');

  // Redirect to home page
  if (typeof showPage === 'function') {
    showPage('home');
  } else {
    window.location.href = 'index.html';
  }
}

// Initialize auth module
function initAuth() {
  console.log('Auth module initialized');

  // Add event listeners
  const loginForm = document.getElementById('login-form');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
  }

  const logoutButtons = document.querySelectorAll('.logout-btn');
  logoutButtons.forEach(button => {
    button.addEventListener('click', handleLogout);
  });

  // Check auth status on page load
  if (checkAuthStatus()) {
    console.log('User is authenticated');
    // Update UI for authenticated user
    const user = JSON.parse(localStorage.getItem('user') || '{}');
    const userElements = document.querySelectorAll('.user-name');
    userElements.forEach(el => {
      el.textContent = user.name || 'User';
    });
  } else {
    console.log('User is not authenticated');
  }
}

// Initialize auth when DOM is loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initAuth);
} else {
  initAuth();
}