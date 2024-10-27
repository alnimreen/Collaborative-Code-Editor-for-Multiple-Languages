package com.example.collabcode.controller;

import com.example.collabcode.model.File;
import com.example.collabcode.model.Room;
import com.example.collabcode.model.Version;
import com.example.collabcode.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "https://3.86.42.230:3000")
public class FileController {
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
   private  FileRepository fileRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    WebSocketController websocketController;

    private final String baseDirectory  = "uploads";

    @PostMapping("/upload/{roomId}")
    public ResponseEntity<Map<String, String>> uploadFile(@PathVariable String roomId, @RequestParam("file") MultipartFile file, @RequestParam String username) throws IOException {
        String userRole = getUserRole(roomId, username);

        if (!userRole.equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You do not have permission to upload files in this room."));
        }

        Path roomDir = Paths.get(baseDirectory, roomId);
        if (!Files.exists(roomDir)) {
            Files.createDirectories(roomDir);
        }
        Path filePath = roomDir.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        File newFile = new File();
        newFile.setRoomId(roomId);
        newFile.setName(file.getOriginalFilename());
        newFile.setContent(new String(file.getBytes()));
        newFile.setLastModified(System.currentTimeMillis());
        newFile.setOwner(username);

        File savedFile = fileRepository.save(newFile);

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            room.getFileIds().add(savedFile.getId());
            roomRepository.save(room);
            websocketController.broadcastChange(roomId, "FILE_UPLOADED", savedFile);

        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Room not found."));
        }

        Version version = new Version();
        version.setFileId(savedFile.getId());
        version.setCode(savedFile.getContent());
        version.setTimestamp(System.currentTimeMillis());
        version.setAuthor(username);
        versionRepository.save(version);

        return ResponseEntity.ok(Map.of("id", savedFile.getId(), "message", "File uploaded and version saved successfully in room " + roomId));
    }

    @GetMapping("/download/{roomId}/{fileId}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String roomId, @PathVariable String fileId) {
        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        byte[] fileContent = file.getContent().getBytes();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(fileContent);
    }

    @DeleteMapping("/delete/{roomId}/{fileId}")
    public ResponseEntity<String> deleteFile(@PathVariable String roomId, @PathVariable String fileId, @RequestParam String username) throws IOException {
        String userRole = getUserRole(roomId, username);
        System.out.println(userRole);
        if (!userRole.equals("ADMIN") ) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You do not have permission to delete files in this room.");
        }

        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        }
        Path filePath = Paths.get(baseDirectory, roomId, file.getName());
        Files.deleteIfExists(filePath);
        versionRepository.deleteByFileId(file.getId());
        fileRepository.delete(file);
        websocketController.broadcastChange(roomId, "FILE_DELETED", fileId);

        return ResponseEntity.ok("File and its versions deleted successfully: " + file.getName());
    }

    private String getUserRole(String roomId, String username) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null && room.getUserRoles() != null) {
            return room.getUserRoles().get(username);
        }
        return null;
    }
}
