document.getElementById('requestForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    
    const formData = {
        userName: document.getElementById('userName').value,
        userContact: document.getElementById('userContact').value,
        location: document.getElementById('location').value
    };
    
    try {
        const response = await fetch('/api/requests', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(formData)
        });
        
        const messageDiv = document.getElementById('message');
        
        if (response.ok) {
            const data = await response.json();
            messageDiv.className = 'alert alert-success';
            messageDiv.textContent = `Ambulance request submitted successfully! Request ID: ${data.id}`;
            messageDiv.style.display = 'block';
            document.getElementById('requestForm').reset();
        } else {
            const error = await response.json();
            messageDiv.className = 'alert alert-error';
            messageDiv.textContent = `Error: ${JSON.stringify(error)}`;
            messageDiv.style.display = 'block';
        }
    } catch (error) {
        const messageDiv = document.getElementById('message');
        messageDiv.className = 'alert alert-error';
        messageDiv.textContent = `Error: ${error.message}`;
        messageDiv.style.display = 'block';
    }
});