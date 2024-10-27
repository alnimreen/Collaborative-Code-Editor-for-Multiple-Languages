import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { useUser } from './UserContext';
import styles from './styles/authStyle.module.css';


function AuthPage() {
    const [userId, setUserId] = useState('');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [isLogin, setIsLogin] = useState(true);
    const navigate = useNavigate();
    const { setUser } = useUser(); 
    const [errorMessage, setErrorMessage] = useState('');
   
    const clearMessages = () => {
        setTimeout(() => {
          setErrorMessage('');
        }, 3000);
      };
      

    const handleGitHubLogin = () => {
        const clientId = "Ov23lie8ji0oOMTGFXTg"; 
        const scope = "user:email read:user"; 
        const redirectUri = encodeURIComponent("https://3.86.42.230:8082/login/oauth2/code/github"); 
        const authorizationUrl = `http://github.com/login/oauth/authorize?client_id=${clientId}&scope=${scope}&redirect_uri=${redirectUri}`;
        window.location.href = authorizationUrl; 
    };

    useEffect(() => {
        const urlParams = new URLSearchParams(window.location.search);
        console.log('urlParams', urlParams)
        const userIdFromUrl = urlParams.get('userId'); 
        const userNameFromUrl = urlParams.get('username'); 
        console.log(userIdFromUrl)

        if (userIdFromUrl) {
            const fetchUserData = async () => {
                try {
                    const response = await api.getUser(); 
                    if (response.status === 200) {
                        console.log("User data fetched successfully:", response.data); 
                        setUser(response.data); 
                        navigate(`/rooms?username=${userNameFromUrl}`);
                    }
                } catch (error) {
                    setErrorMessage('Error fetching user data: ' + (error.response?.data || error.message));
                    clearMessages(); 
                }
            };

            fetchUserData(); 
        }
    }, [setUser, navigate]);

    const handleRegister = async () => {
        if (!userId || !username || !password) {
            setErrorMessage('All fields (User ID, Username, and Password) are required');
            clearMessages(); 
            return;
        }
        const user = { userId, username, password };
        try {
            const response = await api.registerUser(user);
            if (response.status === 200) {
                setUser({ userId, username });
                localStorage.setItem('user', JSON.stringify({ userId, username })); 
                navigate(`/rooms?userId=${userId}&username=${username}`); 
            } else {
                setErrorMessage('Registration failed');
                clearMessages(); 
            }
        } catch (error) {
            setErrorMessage('Registration failed: ' + (error.response?.data || error.message));
            clearMessages(); 
        }
    };

    const handleLogin = async () => {
        try {
            console.log("username: ", username, "password: ",password)
            const response = await api.loginUser({ username, password });
            console.log("Login response:", response);  

            if (response.status === 200) {
                const { userId, username } = response.data;

               if (userId && username) {
                setUser({ userId, username });
                localStorage.setItem('user', JSON.stringify({ userId, username }));
                navigate(`/rooms?userId=${userId}&username=${username}`);
            } else {
                setErrorMessage('Login failed: User data is incomplete');
            }  } else {
                setErrorMessage('Login failed');
                clearMessages(); 
            }
        } catch (error) {
            console.log("Error response:", error.response); 

            setErrorMessage('Login failed: ' + (error.response?.data || error.message));
            clearMessages(); 
        }
    };

    return (
       
        <div className={styles.container}>
            {errorMessage && <div className={styles.errorMessage}>{errorMessage}</div>}
            <h2>{isLogin ? 'Login' : 'Register'}</h2>
            {!isLogin && (
                <input
                    type="text"
                    placeholder="User ID"
                    value={userId}
                    onChange={(e) => setUserId(e.target.value)}
                />
            )}
            <input
                type="text"
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
            />
            <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
            />
            <button onClick={isLogin ? handleLogin : handleRegister}>
                {isLogin ? 'Login' : 'Register'}
            </button>
            <button onClick={() => setIsLogin(!isLogin)}>
                {isLogin ? 'Switch to Register' : 'Switch to Login'}
            </button>
            <button onClick={handleGitHubLogin} className={styles.githubButton}>
                <img src="/GitHub-Symbol.png" alt="GitHub logo" className={styles.githubLogo} />
                Log in with GitHub
            </button>      
              </div>
    );
}

export default AuthPage;
