  document.getElementById('registerForm').addEventListener('submit', async (e) => {
            e.preventDefault();

            const username = document.getElementById('username').value;
            const email = document.getElementById('email').value;
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const role = document.getElementById('role').value;

            const messageDiv = document.getElementById('message');

            // Basic client-side validation
            if (password !== confirmPassword) {
                messageDiv.textContent = 'Passwords do not match';
                messageDiv.className = 'alert alert-error';
                messageDiv.style.display = 'block';
                return;
            }

            if (password.length < 6) {
                messageDiv.textContent = 'Password must be at least 6 characters long';
                messageDiv.className = 'alert alert-error';
                messageDiv.style.display = 'block';
                return;
            }

            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        username,
                        email,
                        password,
                        role
                    })
                });

                const result = await response.json();

                if (response.ok) {
                    messageDiv.textContent = 'Registration successful! Redirecting to login...';
                    messageDiv.className = 'alert alert-success';
                    messageDiv.style.display = 'block';

                    // Redirect to login after successful registration
                    setTimeout(() => {
                        window.location.href = '/login.html';
                    }, 1500);
                } else {
                    messageDiv.textContent = result.error || 'Registration failed';
                    messageDiv.className = 'alert alert-error';
                    messageDiv.style.display = 'block';
                }
            } catch (error) {
                console.error('Error during registration:', error);
                messageDiv.textContent = 'An error occurred during registration';
                messageDiv.className = 'alert alert-error';
                messageDiv.style.display = 'block';
            }
        });