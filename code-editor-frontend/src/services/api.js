import axios from 'axios';

const API_URL = 'https://3.86.42.230:8082/api';

const api = {
  getUser: () => axios.get(`${API_URL}/auth/user`),
  registerUser: (user) => axios.post(`${API_URL}/register`, user),
  loginUser: (user) => axios.post(`${API_URL}/login`, user),

   getRoomsForUser: (username) => axios.get(`${API_URL}/rooms?username=${username}`, {
        withCredentials: true  
    }),
      createRoom: (room) => axios.post(`${API_URL}/createRoom`, room),
  getFilesInRoom: (roomId) => axios.get(`${API_URL}/rooms/${roomId}/files`, {
    withCredentials: true, 
  }),  
  createFile: ( roomId,file, username) => 
    axios.post(`${API_URL}/rooms/${roomId}/files/create?username=${username}`, file),
  getFileContent: (roomId, fileId, username) => 
    axios.get(`${API_URL}/rooms/${roomId}/files/${fileId}`, {
        params: { username } ,
        withCredentials: true, 

      }),
  getUserRoleInRoom: (roomId, username) => 
    axios.get(`${API_URL}/rooms/${roomId}/user-role`, {
      params: { username },
      withCredentials: true, 
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    }),
    executeCode: (roomId, fileId, code, language, username) => 
    axios.post(`${API_URL}/rooms/${roomId}/files/${fileId}/exec`, {
      code,
      language,
      username ,   
         withCredentials: true, 

    }),
  revertVersion: (roomId, fileId, versionId) => 
    axios.post(`${API_URL}/rooms/${roomId}/files/${fileId}/revert/${versionId}`, {
      withCredentials: true, 
    }),  
  listVersions: (roomId, fileId) => 
    axios.get(`${API_URL}/rooms/${roomId}/files/${fileId}/versions`, {
      withCredentials: true,  
    }),  

  saveFileAndVersion: (roomId, fileId, data) => 
    axios.post(`${API_URL}/rooms/${roomId}/files/${fileId}/saveAndVersion`, data, {
      withCredentials: true, 
    }),  

  addComment: (roomId, fileId, comment, username) => 
    axios.post(`${API_URL}/rooms/${roomId}/files/${fileId}/comments?username=${username}`, comment, {
      withCredentials: true,  }),
  
  getComments: (roomId, fileId) => 
    axios.get(`${API_URL}/rooms/${roomId}/files/${fileId}/comments`, {
      withCredentials: true,  }),
    
  deleteComment: (roomId, fileId, commentId, username) => 
    axios.delete(`${API_URL}/rooms/${roomId}/files/${fileId}/comments/${commentId}?username=${username}`, {
      withCredentials: true,  }),
  
  uploadFile: (formData, roomId, username) => axios.post(`${API_URL}/files/upload/${roomId}`, formData, {
  headers: {
    'Content-Type': 'multipart/form-data',
  },
  params: { username } 
}),

downloadFile: (roomId, fileId) => axios.get(`${API_URL}/files/download/${roomId}/${fileId}`, {
  responseType: 'blob' 
}),
deleteFile: (roomId, fileId, username) => 
  axios.delete(`${API_URL}/files/delete/${roomId}/${fileId}`, {
      params: { username } 
  }),
 cloneRoom: (roomId, newRoomId, newRoomName, username) => 
  axios.post(`${API_URL}/rooms/${roomId}/clone`, null, {
    params: { newRoomId, newRoomName, username } 
  }),

forkRoom: (roomId, newRoomId, newRoomName, username) => 
  axios.post(`${API_URL}/rooms/${roomId}/fork`, null, {
    params: { newRoomId, newRoomName, username }
  }),
mergeFiles: (sourceRoomName, sourceFileName, targetRoomName, targetFileName, username) => 
  axios.post(`${API_URL}/rooms/merge?sourceRoomName=${sourceRoomName}&sourceFileName=${sourceFileName}&targetRoomName=${targetRoomName}&targetFileName=${targetFileName}&username=${username}`),

getAllUsers: () => axios.get(`${API_URL}/users`, {
  withCredentials: true, 
}),

assignRoleToUser: (roomId, userData) => 
  axios.post(`${API_URL}/rooms/${roomId}/assignRole`, userData, {
    headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` } 
  }),

  getRoomParticipants: (roomId) => 
   axios.get(`${API_URL}/rooms/${roomId}/participants`, {
    withCredentials: true, 
  }),
  };

export default api;
