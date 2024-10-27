package com.example.collabcode.handler;

import com.example.collabcode.model.Room;
import com.example.collabcode.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
@Service
public class WebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private RoomRepository roomRepository;

    private static final Map<String, Set<WebSocketSession>> roomSessions = new HashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String roomId = getRoomId(session);
            Room room = (Room) roomRepository.findByUuid(roomId);
            if (room != null) {
                roomSessions.computeIfAbsent(roomId, k -> new HashSet<>()).add(session);
            } else {
                session.close(CloseStatus.NOT_ACCEPTABLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            String roomId = getRoomId(session);
            String username = getUsername(session);

            Room room = (Room) roomRepository.findByUuid(roomId);
            if (room != null && hasEditPermissions(room, username)) {
                Set<WebSocketSession> sessions = roomSessions.get(roomId);
                for (WebSocketSession webSocketSession : sessions) {
                    if (webSocketSession.isOpen()) {
                        webSocketSession.sendMessage(message);
                    }
                }
            } else {
                session.sendMessage(new TextMessage("You do not have permission to edit."));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String roomId = getRoomId(session);
        if (roomId != null) {
            Set<WebSocketSession> sessions = roomSessions.get(roomId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    roomSessions.remove(roomId);
                }
            }
        }
    }
    private String getRoomId(WebSocketSession session) {
        String uri = session.getUri().toString();
        String[] parts = uri.split("/");
        return parts.length > 1 ? parts[parts.length - 1] : null;
    }

    private boolean hasEditPermissions(Room room, String username) {
        String role = room.getUserRoles().get(username);
        return "EDITOR".equals(role) || "ADMIN".equals(role);
    }

    private String getUsername(WebSocketSession session) {
        return session.getPrincipal().getName();
    }
}