import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import api from '../services/api';
import styles from './styles/roomStyle.module.css';
import { useUser } from './UserContext';

function Rooms() {
  const [ownedRooms, setOwnedRooms] = useState([]);
  const [participantRooms, setParticipantRooms] = useState([]);
  const [roomName, setRoomName] = useState('');
  const [uuid, setUuid] = useState('');
  const { user } = useUser();
  const navigate = useNavigate();
  const [errorMessage, setErrorMessage] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const location = useLocation();
  const { setUser } = useUser(); 

  const clearMessages = () => {
    setTimeout(() => {
      setErrorMessage('');
      setSuccessMessage('');
    }, 3000);
  };

  useEffect(() => {
    const urlParams = new URLSearchParams(location.search);
    const userId = urlParams.get('userId');
    const username = urlParams.get('username');
    console.log('Current user:', { userId, username }); 
    if (userId && username) {
      setUser({ userId, username });    
      const fetchRooms = async () => {
        try {
          console.log("Fetching rooms for user:", username);
    const response = await api.getRoomsForUser(username);
    console.log("Response received:", response);
          if (response.status === 200) {
            const data = response.data;
            setOwnedRooms(data.ownedRooms);
            setParticipantRooms(data.participantRooms);
          } else {
            setErrorMessage('Error fetching rooms');
            clearMessages();
          }
        }catch (error) {
          console.error('Error details:', error);
          if (error.response) {
              console.error('Response data:', error.response.data);
              console.error('Response status:', error.response.status);
          }
          setErrorMessage('Error fetching rooms: ' + (error.response ? error.response.data : error.message));
          clearMessages();
      }
      };
      fetchRooms();
    } else {
      setErrorMessage('User not logged in');
      clearMessages();
    }
  }, [location.search, setUser]);

  const handleCreateRoom = async () => {
    if (!roomName || !uuid) {
      setErrorMessage('Please provide both room name and user ID');
      clearMessages();
      return;
    }
    const newRoom = {
      name: roomName,
      uuid: uuid,
      owner: user.username,
    };

    try {
      const response = await api.createRoom(newRoom);
      if (response.status === 200) {
        const updatedRoomsResponse = await api.getRoomsForUser(user.username);
        if (updatedRoomsResponse.status === 200) {
            const data = updatedRoomsResponse.data;
            setOwnedRooms(data.ownedRooms);
            setParticipantRooms(data.participantRooms); 
        }
                setRoomName(''); 
        setUuid('');
        setSuccessMessage('Room created successfully!');
        clearMessages();
      }
    } catch (error) {
      setErrorMessage(`Error creating room: ${error.response?.data || error.message}`);
      clearMessages();
    }
  };

  const handleJoinRoom = (roomId) => {
    if (roomId) {
      navigate(`/rooms/${roomId}`);
    } else {
      setErrorMessage('Room ID is undefined');
      clearMessages();
    }
  };

  return (
    <div className={styles.container}>
      {errorMessage && <div className={styles.errorMessage}>{errorMessage}</div>}
      {successMessage && <div className={styles.successMessage}>{successMessage}</div>}

      <h1>Welcome {user.username} </h1>
      <h3>Your Id {user.userId && <span>{user.userId}</span>}</h3>

      <h2>Owned Rooms:</h2>
      <ul className={styles.roomList}>
        {ownedRooms.map((room) => (
          <li key={room.id} onClick={() => handleJoinRoom(room.id)}>
            Room Name: {room.name}
          </li>
        ))}
      </ul>

      <h2>Shared With You:</h2>
      <ul className={styles.roomList}>
        {participantRooms.map((room) => (
          <li key={room.id} onClick={() => handleJoinRoom(room.id)}>
            Room Name: {room.name}
          </li>
        ))}
      </ul>

      <div className={styles.createRoomForm}>
        <input
          type="text"
          placeholder="Room Name"
          value={roomName}
          onChange={(e) => setRoomName(e.target.value)}
        />
        <input
          type="text"
          placeholder="User Id"
          value={uuid}
          onChange={(e) => setUuid(e.target.value)}
        />
        <button onClick={handleCreateRoom}>Create Room</button>
      </div>
    </div>
  );
}

export default Rooms;
