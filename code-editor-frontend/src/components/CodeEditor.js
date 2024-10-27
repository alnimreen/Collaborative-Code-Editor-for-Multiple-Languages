import React, { useState, useEffect, useRef } from 'react';  
import { useParams, useNavigate } from 'react-router-dom'; 
import api from '../services/api';
import { useUser } from './UserContext';
import styles from './styles/codeEditorStyle.module.css';
import { Mutex } from 'async-mutex';

function CodeEditor() {
  const { roomId, fileId } = useParams(); 
  const { user } = useUser();
  const [file, setFile] = useState(null);
  const [code, setCode] = useState('');
  const [language, setLanguage] = useState('python'); 
  const [languages] = useState(['python', 'java']); 
  const [versions, setVersions] = useState([]); 
  const [isVersionControlVisible, setIsVersionControlVisible] = useState(false); 
  const [comments, setComments] = useState([]);
  const [newComment, setNewComment] = useState('');
  const [lineNumber, setLineNumber] = useState(1); 
  const [participants, setParticipants] = useState([]); 
  const [executionOutput, setExecutionOutput] = useState(''); 
  const [userRole, setUserRole] = useState(null);
  const navigate = useNavigate();
  const ws = useRef(null);  
  const [errorMessage, setErrorMessage] = useState(''); 
  const [successMessage, setSuccessMessage] = useState(''); 
  

  const clearMessages = () => {
    setTimeout(() => {
      setErrorMessage('');
      setSuccessMessage('');
    }, 3000);
  };
  const editMutex = useRef(new Mutex());
  const commentMutex = useRef(new Mutex());

  useEffect(() => {
    const fetchUserRole = async () => {
      if (user && user.username) {
        console.log('Fetching role for user:', user.userId);
        console.log('Current user:', user);
  
        try {
          const roomResponse = await api.getUserRoleInRoom(roomId, user.username);
          if (roomResponse.status === 200) {
            setUserRole(roomResponse.data.role);
          }
        } catch (error) {
          setErrorMessage('Error fetching user role in room:', error.response ? error.response.data : error.message);
          clearMessages();
        }
      } else {
        console.warn('User is not logged in or username is missing.');
      }
    };
  
    fetchUserRole();
  }, [user, roomId]);
  useEffect(() => {
    if (roomId) {
      console.log('role', user);

      const socket = new WebSocket(`wss://3.86.42.230:8082/ws/${roomId}`);
      ws.current = socket;

      socket.onopen = () => {
        console.log('Connected to WebSocket');
      };

      socket.onmessage = (event) => {
        const message = JSON.parse(event.data);
        handleWebSocketMessage(message);
      };

      socket.onclose = () => {
        console.log('WebSocket connection closed');
      };

      return () => {
        if (ws.current) {
          ws.current.close();
        }
      };
    }
  }, [roomId]);

  const handleWebSocketMessage = (message) => {
    if (!userRole) return;

    if (message.type === 'CODE_UPDATE' && (userRole === 'ADMIN' || userRole === 'EDITOR')) {
    switch (message.type) {
      case 'CODE_UPDATE':
        editMutex.current.runExclusive(() => {
        setCode(message.payload.code);});
        break;
      case 'COMMENT_ADDED':
        editMutex.current.runExclusive(() => {
        setComments((prevComments) => [...prevComments, message.payload.comment]);});
        break;
      case 'COMMENT_DELETED':
        commentMutex.current.runExclusive(() => {
          setComments((prevComments) =>
          prevComments.filter((comment) => comment.id !== message.payload.commentId)
        );});
        break;
      case 'PARTICIPANT_UPDATE':
        editMutex.current.runExclusive(() => {
        setParticipants(message.payload.participants);});
        break;
      default:
        break;
    }}
  };

  const broadcastCodeChange = (newCode) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      const message = {
        type: 'CODE_UPDATE',
        payload: {
          roomId,
          fileId,
          code: newCode,
        },
      };
      editMutex.current.runExclusive(() => {
        ws.current.send(JSON.stringify(message));
      });
    }
  };
   const handleCodeChange = (e) => {
    setCode(e.target.value);
    broadcastCodeChange(e.target.value); 
  };

    useEffect(() => {
      const fetchParticipants = async () => {
        try {
          const response = await api.getRoomParticipants(roomId);
          if (response.status === 200) {
            setParticipants(response.data);
          }
        } catch (error) {
          setErrorMessage('Error fetching participants:', error.message);
          clearMessages();
        }
      };
  
      fetchParticipants();
    }, [roomId]);
  useEffect(() => {
    const fetchFileContent = async () => {
      if (roomId && fileId) {
        try {
          const response = await api.getFileContent(roomId, fileId, user.username);
          if (response.status === 200) {
            console.log('File content fetched:', response.data);
            setFile(response.data.file); 
            setCode(response.data.file.content); 
            setLanguage(response.data.file.lang || 'python'); 
          }
        } catch (error) {
          setErrorMessage('Error fetching file content:', error.response ? error.response.data : error.message);
          clearMessages();
        }
      }
    };
    
    fetchFileContent();
  }, [roomId, fileId, user.username]);


  useEffect(() => {
    const fetchComments = async () => {
      if (roomId && fileId) {
        try {
          const response = await api.getComments(roomId, fileId);
          setComments(response.data);
          console.log('Comments state updated:', response.data); 
        } catch (error) {
          setErrorMessage('Error fetching comments:', error);
          clearMessages();
        }
      }
    };

    fetchComments();
  }, [roomId, fileId]);

  
  const handleAddComment = async () => {
    await commentMutex.current.runExclusive(async () => {

    if (!newComment) return;

    try {
      const comment = {
        content: newComment,
        author: user.username,
        lineNumber: lineNumber,
        fileId: file.id, 
        roomId: roomId,
      };

      const response = await api.addComment(roomId, fileId, comment, user.username);
      const savedComment = response.data; 
      if (response.status === 200) {
        setComments([...comments, savedComment]);
        broadcastComment(response.data); 
      }

      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        const message = {
          type: 'COMMENT_ADDED',
          payload: {
            comment: savedComment,
          },
        };
        ws.current.send(JSON.stringify(message));
      }

      setNewComment('');
    } catch (error) {
      setErrorMessage('Error adding comment:', error);
      clearMessages();
    }
  });
  };
  const broadcastComment = (comment) => {
    if (ws.current && ws.current.readyState === WebSocket.OPEN) {
      const message = {
        type: 'COMMENT_ADDED',
        payload: { comment },
      };
      ws.current.send(JSON.stringify(message));
    }
  };


  const handleDeleteComment = async (commentId) => {

    if (!commentId) {
      setErrorMessage('Comment ID is missing');
      clearMessages();
      return;
    }
  
    try {
      await api.deleteComment(roomId, fileId, commentId, user.username);
      setComments(comments.filter(comment => comment.id !== commentId));
    } catch (error) {
      setErrorMessage('Error deleting comment:', error);
      clearMessages();
    }
  };
  
  const handleExecute = async () => {
    if (!roomId || !fileId) {
      setErrorMessage("Room ID or File ID is undefined.");
      clearMessages();
      return;
    }
    try {
      const response = await api.executeCode(roomId, fileId, code, language, user.username); 
      console.log("response: ",response)
      if (response.status === 200) {
        setExecutionOutput(response.data); 
      } else {
        setErrorMessage('Error executing code');
        clearMessages();
      }
    } catch (error) {
      setErrorMessage('Execution error:', error);
      clearMessages();
    }
  };
  
  const handleSaveVersion = async () => {
    console.log('Current file state:', file);

    if (!file) {
      setErrorMessage('File is not defined. Please select a file first.');
      clearMessages();  
      return;
    }

    const { id } = file;

    if (!id || !user.username) {
        setErrorMessage('File ID or author is undefined. Please check if you are logged in and have a valid file.');
        clearMessages();
        return;
    }

    if (!code || !language) {
        setErrorMessage("Please ensure you have code and language defined before saving.");
        clearMessages();
        return;
    }

    try {
        const response = await api.saveFileAndVersion(roomId, id, {
            code: code,
            lang: language,
            author: user.username,
            username: user.username,
            roomId: roomId,
        });

        if (response.status === 200) {
            setSuccessMessage('File and version saved successfully.');
            clearMessages();
          } else {
          setErrorMessage('Error saving file and version.');
          clearMessages();
        }
    } catch (error) {
        setErrorMessage('Error saving file and version: ' + error.response?.data || error.message);
        clearMessages();
      }
};



const handleListVersions = async () => {
  if (!roomId || !fileId) {
    setErrorMessage('Room ID or File ID is undefined. Please select a file.');
    clearMessages();
    return;
  }
  try {
    const response = await api.listVersions(roomId, fileId);
    if (response.status === 200) {
      setVersions(response.data);
      console.log('Versions state updated:', response.data);
      setIsVersionControlVisible(true);
    } else {
      setErrorMessage('Error fetching versions');
      clearMessages();
    }
  } catch (error) {
    setErrorMessage('Error fetching versions:', error.response ? error.response.data : error.message);
    clearMessages();
  }
};

  const handleRevertVersion = async (versionId) => {
    if (userRole !== 'ADMIN' && userRole !== 'EDITOR') {
      setErrorMessage('You do not have permission to Revert Version.');
      clearMessages();
      return;
    }
    try {
      const response = await api.revertVersion(roomId, fileId, versionId);
      if (response.status === 200) {
        setCode(response.data.code);
        setSuccessMessage("Code Reverted successfuly!")
        clearMessages();
      } else {
        setErrorMessage('Error reverting to the selected version.');
        clearMessages();
      }
    } catch (error) {
      setErrorMessage('Error reverting version:', error.response ? error.response.data : error.message);
      clearMessages();
    }
  };

  return (
    <div className={`${styles.editorPage} dark-code-editor`}>
  <h2>Editing File: {file?.name}</h2>
  {errorMessage && <div className={styles.errorMessage}>{errorMessage}</div>}
      {successMessage && <div className={styles.successMessage}>{successMessage}</div>}

  {/* Main Layout Section */}
  <div className={styles.mainContainer}>
    
    {/* Execution Output Section on the left */}
    <div className={styles.outputSection}>
      <h4>Execution Output:</h4>
      <pre>{executionOutput}</pre>
    </div>

    {/* Code Editor Section in the middle */}
    <div className={styles.editorSection}>
      <h2>Editor</h2>
      <select value={language} onChange={(e) => setLanguage(e.target.value)}>
        <option value="python">Python</option>
        <option value="java">Java</option>
      </select>

      {/* Code editor (textarea) */}
      <textarea value={code} onChange={handleCodeChange} rows={20} cols={70} />

      
      {/* Action buttons */}
      <div className={styles.actionButtons}>
        <button onClick={handleExecute}>Execute</button>
        <button onClick={handleSaveVersion}>Save Version</button>
      </div>
    </div>
  
    {/* Participants Section on the far right */}
    <div className={styles.participantsSection}>
      <h2>Participants</h2>
      <ul>
        {participants.map(participant => (
          <li key={participant.userId}>
            <strong>{participant.username}</strong> ({participant.role})
          </li>
        ))}
      </ul>
    </div>
  </div>
  <button onClick={handleListVersions} className="listVersions">List Versions</button>
  {isVersionControlVisible && (
        <div>
          <h4>Versions</h4>
          <ul>
            {versions.map((version) => (
              <li key={version.id}>
                Version by {version.author} at {new Date(version.timestamp).toLocaleString()}
                <pre>{version.code}</pre>
                <button onClick={() => handleRevertVersion(version.id)}>Revert</button>
              </li>
            ))}
          </ul>
        </div>
      )}
  {/* Add Comments Section below */}
  <div className={styles.editorSection}>
    <h4>Add Comments</h4>
    <textarea
      value={newComment}
      onChange={(e) => setNewComment(e.target.value)}
      placeholder="Add a comment..."
    />
    <input
      type="number"
      value={lineNumber}
      onChange={(e) => setLineNumber(Number(e.target.value))}
      placeholder="Line number"
    />
  <button onClick={handleAddComment}>Add Comment</button>
  
</div>

  {/* Comments Section below */}
  <div className={styles.commentsSection}>
    <h4>Comments</h4>
    <ul>
      {comments.map((comment) => (
        <li key={comment.id} className={styles.commentItem}>
          <div>
            <strong>{comment.author}</strong> (Line {comment.lineNumber}): {comment.content}
          </div>
      <button onClick={() => handleDeleteComment(comment.id)}>Delete Comment</button>
  
            </li>
      ))}
    </ul>
  </div>
</div>
  )
}

export default CodeEditor;    