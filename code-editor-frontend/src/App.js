import React, { useEffect } from 'react';
import { BrowserRouter as Router, Route, Routes, useNavigate } from 'react-router-dom';
import Register from './components/AuthPage';
import Rooms from './components/Rooms';
import RoomDetails from './components/RoomDetails';
import { useUser, UserProvider } from './components/UserContext';
import CodeEditor from './components/CodeEditor';

function ProtectedRoute({ element }) {
  const { user } = useUser();
  const navigate = useNavigate();

  useEffect(() => {
    if (!user) {
      const storedUser = localStorage.getItem('user');
      if (!storedUser) {
        navigate('/');  
      }
    }
  }, [user, navigate]);

  return user ? element : null;
}

function App() {
  return (
    <UserProvider>
      <Router>
        <Routes>
          <Route path="/" element={<Register />} />
          <Route path="/rooms" element={<ProtectedRoute element={<Rooms />} />} />
          <Route path="/rooms/:roomId" element={<ProtectedRoute element={<RoomDetails />} />} />
          <Route path="/rooms/:roomId/files/:fileId" element={<ProtectedRoute element={<CodeEditor />} />} />
        </Routes>
      </Router>
    </UserProvider>
  );
}

export default App;
