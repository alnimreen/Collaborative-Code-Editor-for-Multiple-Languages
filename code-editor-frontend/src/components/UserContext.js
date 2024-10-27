import React, { createContext, useContext, useState, useEffect } from 'react';

const UserContext = createContext();

export const UserProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  
  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    console.log('Stored user in localStorage:', storedUser);

    if (storedUser) {
      setUser(JSON.parse(storedUser)); 
    }
  }, []);

  return (
    <UserContext.Provider value={{ user, setUser }}>
      {children}
    </UserContext.Provider>
  );
};

export const useUser = () => useContext(UserContext);
