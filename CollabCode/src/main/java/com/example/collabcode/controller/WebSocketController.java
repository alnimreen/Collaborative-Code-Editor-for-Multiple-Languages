package com.example.collabcode.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@CrossOrigin(origins = "https://3.86.42.230:3000")
@Controller
public class WebSocketController {
    private final SimpMessagingTemplate messagingTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Lock> fileLocks = new ConcurrentHashMap<>(); // Locks for file concurrency

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    @MessageMapping("/code")
    @SendTo("/topic/code")

    public void handleTextMessage(String message) {
        try {
            Map<String, Object> parsedMessage = objectMapper.readValue(message, Map.class);
            String action = (String) parsedMessage.get("action");

            switch (action) {
                case "EDIT":
                    handleEdit(parsedMessage);
                    break;
                case "SAVE":
                    handleSave(parsedMessage);
                    break;
                case "EXECUTE":
                    handleExecute(parsedMessage);
                    break;
                case "COMMENT":
                    handleComment(parsedMessage);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEdit(Map<String, Object> message) {
        String fileId = (String) message.get("fileId");
        String content = (String) message.get("content");
        String username = (String) message.get("username");
        String timestamp = (String) message.get("timestamp");
        Lock lock = fileLocks.computeIfAbsent(fileId, k -> new ReentrantLock()); // Lock per file
        lock.lock();
        try {
            System.out.println("Editing content for file: " + fileId);
            broadcastChange(fileId, "EDIT", content);
        } finally {
            lock.unlock();
        }
    }

    private void handleSave(Map<String, Object> message) {
        String fileId = (String) message.get("fileId");
        String username = (String) message.get("username");

        Lock lock = fileLocks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            System.out.println("Saving file: " + fileId);
            broadcastChange(fileId, "SAVE", username);
        } finally {
            lock.unlock();
        }
    }

    private void handleExecute(Map<String, Object> message) {
        String fileId = (String) message.get("fileId");
        String language = (String) message.get("language");
        String username = (String) message.get("username");
        String timestamp = (String) message.get("timestamp");
        System.out.println(username + " executed file " + fileId + " in " + language + " at " + timestamp);
    }

    private void handleComment(Map<String, Object> message) {
        String fileId = (String) message.get("fileId");
        String comment = (String) message.get("comment");
        String username = (String) message.get("username");
        String timestamp = (String) message.get("timestamp");
        Lock lock = fileLocks.computeIfAbsent(fileId, k -> new ReentrantLock());
        lock.lock();
        try {
            System.out.println("Adding comment to file: " + fileId);
            broadcastChange(fileId, "COMMENT", comment);
        } finally {
            lock.unlock();
        }
    }
        public void broadcastChange(String roomId, String action, Object payload) {
            Map<String, Object> message = Map.of("action", action, "payload", payload);
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, message);
        }
}