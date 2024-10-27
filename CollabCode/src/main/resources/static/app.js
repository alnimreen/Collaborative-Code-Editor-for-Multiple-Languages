const createRoomBtn = document.getElementById('create-room');
const joinRoomBtn = document.getElementById('join-room');
const saveCodeBtn = document.getElementById('save-code');
const runCodeBtn = document.getElementById('run-code');
let roomId = null;
let roomKey = null;
let currentFileId = null; // Keep track of the currently opened file
let socket = null;
// API base URL
const apiUrl = 'http://localhost:8081/api';

// Room creation
createRoomBtn.addEventListener('click', async () => {
    const owner = document.getElementById('owner').value; // Corrected ID
    // Ensure the owner is provided
    if (!owner) {
        alert('Owner must be provided.');
        return;
    }

    console.log('Creating room with owner:', owner); // Debugging log

    const response = await fetch(`${apiUrl}/createRoom`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ owner, userRoles: {} }) // Make sure to send an empty userRoles object
    });

    if (response.ok) {
        const message = await response.text();
        console.log(message); // "Room Created!"

        // You can also alert the message
        alert(message);
        roomId = data.uuid;
        roomKey = data.roomKey;

        alert(`Room Created! ID: ${roomId}, Key: ${roomKey}`);
        document.getElementById('files-section').style.display = 'block';
        setupWebSocket(roomId); // Set up WebSocket connection
    } else {
        const errorMessage = await response.text();
        alert(`Error: ${errorMessage}`);    }
});

// Join Room
joinRoomBtn.addEventListener('click', async () => {
    roomId = document.getElementById('id').value; // Corrected ID
    roomuuid= document.getElementById('uuid').value; // Corrected ID

    roomKey = document.getElementById('roomKey').value; // Corrected ID
    // Ensure both fields are filled
    if (!roomId || !roomKey||!roomuuid) {
        alert('Room ID and Room Key must be provided.');
        return;
    }
    console.log('Joining room with ID:', roomId, 'and Key:', roomKey); // Debugging log

    // Use GET method for joining room
    const response = await fetch(`${apiUrl}/joinRoom?uuid=${roomuuid}&roomKey=${roomKey}`, {
        method: 'GET' // Ensure method is GET
    });

    if (response.status === 200) {
        document.getElementById('files-section').style.display = 'block';
    } else {
        alert('Failed to join the room!');
    }
});

// Load existing files
async function loadFiles() {
    const response = await fetch(`/api/rooms/${roomId}/files`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });

    if (response.ok) {
        const files = await response.json();
        const fileList = document.getElementById('file-list');
        fileList.innerHTML = ''; // Clear previous file list

        files.forEach(file => {
            const fileItem = document.createElement('div');
            fileItem.textContent = file.name;
            fileItem.addEventListener('click', () => {
                openFile(file.id); // Open file when clicked
            });
            fileList.appendChild(fileItem);
        });
    } else {
        alert('Failed to load files.');
    }
}

// File creation
document.getElementById('create-file').addEventListener('click', async () => {
    const fileName = document.getElementById('fileName').value;
    const fileContent = document.getElementById('code-editor').value || ''; // Default to empty content if none is provided

    if (!fileName) {
        alert('File name cannot be empty.');
        return;
    }

    const response = await fetch(`/api/rooms/${roomId}/files/create`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            fileName,
            content: fileContent
        })
    });

    if (response.ok) {
        alert('File created!');
        loadFiles(); // Refresh the file list
    } else {
        alert('Failed to create file!');
    }
});


// Open file
async function openFile(fileId) {
    const response = await fetch(`/api/rooms/${roomId}/files/${fileId}`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        }
    });

    if (response.ok) {
        const file = await response.json();
        document.getElementById('code-editor').value = file.content; // Display file content in editor
        currentFileId = fileId; // Set the current file ID for saving later
    } else {
        alert('Failed to open file.');
    }
}

// Save code to the current file
saveCodeBtn.addEventListener('click', async () => {
    const code = document.getElementById('code-editor').value;

    if (!currentFileId) {
        alert('No file selected.');
        return;
    }

    const response = await fetch(`api/rooms/${roomId}/files/${currentFileId}/state`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code })
    });

    if (response.ok) {
        alert('Code saved!');
    } else {
        alert('Failed to save code!');
    }
});

// WebSocket connection
function setupWebSocket(roomId) {
    socket = new WebSocket(`ws://localhost:8081/websocket/${roomId}`);

    socket.onmessage = function(event) {
        const editor = document.getElementById('code-editor');
        editor.value = event.data;
    };

    socket.onopen = function() {
        console.log('Connected to WebSocket');
    };

    socket.onclose = function() {
        console.log('Disconnected from WebSocket');
    };
}

// Running the code
runCodeBtn.addEventListener('click', async () => {
    const code = document.getElementById('code-editor').value;

    const response = await fetch(`${apiUrl}/exec`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code, lang: 'java' }) // Ensure language is correctly sent
    });

    if (response.ok) {
        const result = await response.json();
        document.getElementById('terminal-output').innerText = result.out; // Display output
    } else {
        alert('Failed to run code!');
    }
});

