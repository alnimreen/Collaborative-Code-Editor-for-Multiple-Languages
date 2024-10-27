package com.example.collabcode.controller;

import com.example.collabcode.model.*;
import com.example.collabcode.repository.*;
import com.example.collabcode.service.Impl.CodeExecutorImpl;
import com.example.collabcode.service.RoomService;
import com.example.collabcode.service.UserService;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;

@CrossOrigin(origins = "https://3.86.42.230:3000")
@RestController
@RequestMapping("/api")
public class RoomController {
    @Autowired
    private VersionRepository versionRepository;
    @Autowired
    private CodeRepository codeRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    FileRepository fileRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private CodeExecutorImpl codeExecutor;
    @Autowired
    private UserService userService;
    @Autowired
    private  ExecResultRepository execResultRepository;
    @Autowired
    private RoomService roomService;
    private final Lock fileLock = new ReentrantLock();
    private CookieCsrfTokenRepository jwtTokenProvider;
    @Autowired
    WebSocketController websocketController;

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        try {

            String result = userService.registerUser(user);

            if ("User registered successfully".equals(result)) {
                List<GrantedAuthority> authorities = new ArrayList<>();
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user.getUsername(), user.getPassword(), authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                return ResponseEntity.status(HttpStatus.OK).body(result);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }
        } catch (Exception e) {
            log.error("Error during registration: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error during registration: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<User> loginUser(@RequestBody User user) {
        User loggedInUser = userService.loginUser(user.getUsername(), user.getPassword());

        if (loggedInUser != null) {

            List<GrantedAuthority> authorities = new ArrayList<>();
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    loggedInUser.getUsername(), user.getPassword(), authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return ResponseEntity.status(HttpStatus.OK).body(loggedInUser);
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, List<Room>>> getRoomsForUser(@RequestParam String username) {
        try {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Set<Room> ownedRooms = new HashSet<>(roomRepository.findByOwner(user.getUsername()));
            List<Room> participantRooms = roomRepository.findByParticipants(user.getUserId());
            participantRooms.removeAll(ownedRooms);

            Map<String, List<Room>> response = new HashMap<>();
            response.put("ownedRooms", new ArrayList<>(ownedRooms));
            response.put("participantRooms", participantRooms);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching rooms for user: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PostMapping("/rooms/{roomId}/files/{fileId}/comments")
    public ResponseEntity<Comment> addComment(@PathVariable String roomId,
                                              @PathVariable String fileId,
                                              @RequestBody Comment comment,
                                              @RequestParam String username) {

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        String role = room.getUserRoles().get(username);
        if (role == null || role.equals("VIEWER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        comment.setFileId(fileId);
        comment.setRoomId(roomId);

        if (comment.getContent() == null || comment.getContent().isEmpty()) {
            return ResponseEntity.badRequest().body(null); // Return 400 if content is missing
        }
        commentRepository.save(comment);
        websocketController.broadcastChange(roomId, "Comment Added", comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/rooms/{roomId}/files/{fileId}/comments")
    public ResponseEntity<List<Comment>> getComments(@PathVariable String roomId,
                                                     @PathVariable String fileId) {
        List<Comment> comments = commentRepository.findByRoomIdAndFileId(roomId, fileId);
        return ResponseEntity.ok(comments);
    }

    @DeleteMapping("/rooms/{roomId}/files/{fileId}/comments/{commentId}")
    public ResponseEntity<String> deleteComment(@PathVariable String roomId,
                                                @PathVariable String fileId,
                                                @PathVariable String commentId,
                                                @RequestParam String username) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found");
        }

        String role = room.getUserRoles().get(username);
        if (role == null || role.equals("VIEWER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to delete comments");
        }

        Optional<Comment> comment = commentRepository.findById(commentId);
        if (!comment.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Comment not found");
        }

        try {
            commentRepository.deleteById(commentId);
            websocketController.broadcastChange(roomId, "Comment Deleted", comment);

            return ResponseEntity.ok("Comment deleted successfully");
        } catch (Exception e) {
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting comment: " + e.getMessage());
        }
    }

    @GetMapping("/rooms/{roomId}/user-role")
    public ResponseEntity<Map<String, String>> getUserRoleInRoom(@PathVariable String roomId, @RequestParam String username) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        String role = room.getUserRoles().get(username);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Map<String, String> response = new HashMap<>();
        response.put("role", role);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rooms/{roomId}/files/create")
    public ResponseEntity<Map<String, Object>> createNewFile(
            @PathVariable String roomId,
            @RequestBody File newFile,
            @RequestParam String username) {

        Map<String, Object> response = new HashMap<>();

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            response.put("message", "Room not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        String role = room.getUserRoles().get(username);
        if (role == null || !"ADMIN".equals(role)) {
            response.put("message", "You do not have permission to create a file.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        String fileId = UUID.randomUUID().toString();
        newFile.setId(fileId);
        newFile.setOwner(username);
        newFile.setRoomId(roomId);
        newFile.setLastModified(System.currentTimeMillis());

        if (newFile.getLang() == null || newFile.getLang().isEmpty()) {
            newFile.setLang("python");
        }

        if (newFile.getContent() == null) {
            newFile.setContent("");
        }
        File savedFile = fileRepository.save(newFile);
        room.getFileIds().add(savedFile.getId());
        roomRepository.save(room);
        response.put("file", savedFile);
        websocketController.broadcastChange(roomId, "fileCreated", savedFile);

        return ResponseEntity.ok(response);
    }

    private final String baseDirectory = "uploads";

    @PutMapping("/rooms/{roomId}/files/{fileId}")
    public ResponseEntity<String> editFile(@PathVariable String roomId, @PathVariable String fileId,
                                           @RequestBody MultipartFile newFile, @RequestParam String username) throws IOException {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }

        String role = room.getUserRoles().get(username);
        if (!"EDITOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to edit this file.");
        }
        fileLock.lock();
        try {
            File file = fileRepository.findById(fileId).orElse(null);
            if (file == null || !file.getRoomId().equals(roomId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
            }

            Version version = new Version();
            version.setFileId(file.getId());
            version.setCode(new String(newFile.getBytes()));
            version.setTimestamp(System.currentTimeMillis());
            version.setAuthor(username);
            versionRepository.save(version);

            file.setContent(new String(newFile.getBytes()));
            file.setLastModified(System.currentTimeMillis());
            fileRepository.save(file);
            Path filePath = Paths.get(baseDirectory, roomId, file.getName());
            Files.copy(newFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return ResponseEntity.ok("File edited and version updated successfully.");
        } finally {
            fileLock.unlock();
        }
    }

    @GetMapping("/rooms/{roomId}/files/{fileId}")
    public ResponseEntity<Map<String, Object>> getFileContent(@PathVariable String roomId,
                                                              @PathVariable String fileId, @RequestParam String username) {

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        String role = room.getUserRoles().get(username);
        if (role == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        File file = fileRepository.findById(fileId).orElse(null);
        System.out.println("filename; "+file.getName());
        if (file == null || !file.getRoomId().equals(roomId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("file", file);
        response.put("role", role);
        return ResponseEntity.ok(response);
    }
    @PostMapping(value = "/createRoom", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Object> createRoom(@RequestBody Room room) {
        if (room.getName() == null || room.getUuid() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Room name and UUID are required.");
        }

        User user = userRepository.findById(room.getUuid()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found");
        }
        if (user.getRoomIds().contains(room.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You already have a room with this id.");
        }
        room.setOwner(user.getUsername());

        if (room.getUserRoles() == null) {
            room.setUserRoles(new HashMap<>());
        }
        room.getUserRoles().put(user.getUsername(), "ADMIN");

        if (room.getParticipants() == null) {
            room.setParticipants(new ArrayList<>());
        }
        room.getParticipants().add(user.getUserId());

        roomRepository.save(room);
        user.getRoomIds().add(room.getId());
        userRepository.save(user);
        websocketController.broadcastChange(room.getId(), "ROOM_CREATED", room);

        return ResponseEntity.status(HttpStatus.OK).body("Room Created!");
    }

    @GetMapping(value = "/joinRoom", produces = "application/json")
    public ResponseEntity<Object> joinRoom(@RequestParam String uuid, @RequestParam String username) {
        List<Room> roomOptional = roomRepository.findByUuid(uuid);
        if (roomOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid room UUID.");
        }
        Room room = null;
        for (Room r : roomOptional) {
            if (r.getOwner().equals(username) || r.getParticipants().contains(username)) {
                room = r;
                break;
            }
        }

        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not part of this room.");
        }
        Map<String, Object> response = new HashMap<>();
        response.put("room", room);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/rooms/{roomId}/files")
    public ResponseEntity<List<File>> getFilesInRoom(@PathVariable String roomId) {

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        Optional<User> userOptional = userRepository.findByUsername(room.getOwner());
        if (userOptional.isEmpty()) {
            log.error("User not found for room owner: {}", room.getOwner());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        User user = userOptional.get();

        if (!room.getUserRoles().containsKey(user.getUsername())) {
            log.error("User {} does not have access to room ID: {}", user.getUsername(), roomId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        List<File> files = fileRepository.findByRoomId(roomId);
        log.info("Files found for room ID {}: {}", roomId, files);

        if (files.isEmpty()) {
            log.warn("No files found for room ID: {}", roomId);
        }

        return ResponseEntity.ok(files);
    }

    @PostMapping("/rooms/{roomId}/files/{fileId}/exec")
    public ResponseEntity<String> executeFile(@PathVariable String roomId, @PathVariable String fileId,
                                              @RequestBody Map<String, Object> requestData) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }
        String username = (String) requestData.get("username");
        String language = (String) requestData.get("language");
        String role = room.getUserRoles().get(username);

        if (!"EDITOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to execute this file.");
        }
        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found.");
        }
        if (file.getContent() == null || file.getLang() == null) {
            return ResponseEntity.badRequest().body("File content or language is not defined.");
        }
        if (!file.getLang().equalsIgnoreCase(language)) {
            return ResponseEntity.badRequest().body("Language mismatch. Please ensure the language is correct.");
        }
        Code code = new Code();
        code.setCode(file.getContent());
        code.setLang(language);
        ExecResult result = codeExecutor.codeExecutor(code);
        ExecResult execResult = new ExecResult(result.getOut(), result.getTte(),file.getName());
        execResultRepository.save(execResult); // Save to MongoDB
        websocketController.broadcastChange(roomId, "FILE_EXECUTED", Map.of("fileId", fileId, "output", execResult.getOut()));

        return ResponseEntity.ok(execResult.getOut());
    }

    @PostMapping("/rooms/{roomId}/files/{fileId}/saveAndVersion")
    public ResponseEntity<String> saveFileAndVersion(@PathVariable String roomId,
                                                     @PathVariable String fileId,
                                                     @RequestBody Map<String, String> requestBody) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }
        String username = requestBody.get("username");
        String role = room.getUserRoles().get(username);

        if (!"EDITOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to save this file.");
        }
        String code = requestBody.get("code");
        String lang = requestBody.get("lang");
        String author = requestBody.get("author");

        File file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }
        file.setContent(code);
        file.setLang(lang);
        file.setLastModified(Instant.now().toEpochMilli());

        fileRepository.save(file);

        Version version = new Version();
        version.setFileId(fileId);
        version.setRoomId(roomId);
        version.setCode(code);
        version.setTimestamp(Instant.now().toEpochMilli());
        version.setAuthor(author);
        versionRepository.save(version);
        websocketController.broadcastChange(roomId, "fileSavedAndVersioned", file);

        return ResponseEntity.ok("File and version saved successfully.");
    }
    @PostMapping("/rooms/{roomId}/files/{fileId}/revert/{versionId}")
    public ResponseEntity<Version> revertVersion(@PathVariable String roomId,
                                                 @PathVariable String fileId,
                                                 @PathVariable String versionId) {

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userRepository.findById(room.getUuid()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String role = room.getUserRoles().get(user.getUsername());
        if (!"EDITOR".equals(role) && !"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Version revertedVersion = versionRepository.findById(versionId).orElse(null);
        if (revertedVersion == null) {
            return ResponseEntity.notFound().build();
        }
        File file = fileRepository.findById(fileId).orElse(null);
        if (file != null) {
            file.setContent(revertedVersion.getCode());
            fileRepository.save(file);
        }
        return ResponseEntity.ok(revertedVersion);
    }

    @GetMapping("/rooms/{roomId}/files/{fileId}/versions")
    public ResponseEntity<List<Version>> getFileVersions(@PathVariable String roomId,
                                                         @PathVariable String fileId) {

        List<Version> versions = versionRepository.findByRoomIdAndFileId(roomId, fileId);
        return ResponseEntity.ok(versions);
    }

    public String getLoggedInUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authenticated user: " + authentication);
        if (authentication != null) {
            System.out.println("Is Authenticated: " + authentication.isAuthenticated());
            Object principal = authentication.getPrincipal();

            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            } else {
                return principal.toString();
            }
        }
        return null;
}
    private static final Logger log = LoggerFactory.getLogger(RoomController.class);
    private String generateNewFileId() {
        return UUID.randomUUID().toString();
    }
    @PostMapping("/rooms/{roomId}/clone")
    public ResponseEntity<Object> cloneRoom(
            @PathVariable String roomId,
            @RequestParam String newRoomId,
            @RequestParam String newRoomName,
            @RequestParam String username) {

        if (!hasEditPermission(roomId, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You do not have permission to clone this room."));
        }

        Room originalRoom = roomRepository.findById(roomId).orElse(null);
        if (originalRoom == null) {
            log.error("Original room with ID {} not found", roomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Room not found"));
        }

        Room clonedRoom = new Room();
        clonedRoom.setId(newRoomId);
        clonedRoom.setName(newRoomName);
        clonedRoom.setUuid(originalRoom.getUuid());
        clonedRoom.setOwner(originalRoom.getOwner());
        clonedRoom.setUserRoles(originalRoom.getUserRoles() != null ?
                new HashMap<>(originalRoom.getUserRoles()) : new HashMap<>());

        clonedRoom.getUserRoles().put(username, "EDITOR");

        List<File> originalFiles = fileRepository.findByRoomId(roomId);
        List<File> clonedFiles = new ArrayList<>();

        if (originalFiles == null || originalFiles.isEmpty()) {
            log.warn("No files found for the original room ID: {}", roomId);
        } else {
            for (File originalFile : originalFiles) {
                File clonedFile = new File();
                clonedFile.setId(UUID.randomUUID().toString());
                clonedFile.setName(originalFile.getName());
                clonedFile.setContent(originalFile.getContent());
                clonedFile.setRoomId(newRoomId);
                clonedFile.setLastModified(originalFile.getLastModified());
                clonedFile.setOwner(originalFile.getOwner());
                clonedFile.setLang(originalFile.getLang());
                clonedFile.setVersions(originalFile.getVersions() != null ?
                        new ArrayList<>(originalFile.getVersions()) : new ArrayList<>());
                clonedFile.setComments(originalFile.getComments() != null ?
                        new ArrayList<>(originalFile.getComments()) : new ArrayList<>());
                clonedFiles.add(clonedFile);
            }
        }

        fileRepository.saveAll(clonedFiles);

        List<String> clonedFileIds = clonedFiles.stream().map(File::getId).collect(Collectors.toList());
        clonedRoom.setFileIds(clonedFileIds);

        roomRepository.save(clonedRoom);
        websocketController.broadcastChange(roomId, "roomCloned", clonedRoom);
        websocketController.broadcastChange(newRoomId, "roomCloned", clonedRoom);

        return ResponseEntity.ok(clonedRoom);
    }

    @PostMapping("/rooms/{roomId}/fork")
    public ResponseEntity<Object> forkRoom(@PathVariable String roomId,
                                           @RequestParam String newRoomId,
                                           @RequestParam String newRoomName,
                                           @RequestParam String username) {
        if (!hasEditPermission(roomId, username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You do not have permission to fork this room."));
        }
        Room originalRoom = roomRepository.findById(roomId).orElse(null);
        if (originalRoom == null) {
            log.error("Original room with ID {} not found", roomId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Room not found"));
        }

        Room forkedRoom = new Room();
        forkedRoom.setId(newRoomId);
        forkedRoom.setName(newRoomName);
        forkedRoom.setUuid(originalRoom.getUuid());
        forkedRoom.setOwner(originalRoom.getOwner());
        forkedRoom.setUserRoles(new HashMap<>(originalRoom.getUserRoles()));

        forkedRoom.getUserRoles().put(username, "EDITOR");
        List<String> originalFileIds = originalRoom.getFileIds();
        List<File> clonedFiles = new ArrayList<>();

        for (String fileId : originalFileIds) {
            File originalFile = fileRepository.findById(fileId).orElse(null);
            if (originalFile != null) {
                File clonedFile = new File();
                clonedFile.setId(UUID.randomUUID().toString());
                clonedFile.setName(originalFile.getName());
                clonedFile.setContent(originalFile.getContent());
                clonedFile.setLang(originalFile.getLang());
                clonedFile.setLastModified(originalFile.getLastModified());
                clonedFile.setOwner(originalFile.getOwner());
                clonedFile.setRoomId(newRoomId);
                clonedFile.setComments(originalFile.getComments() != null ?
                        new ArrayList<>(originalFile.getComments()) : new ArrayList<>());
                clonedFile.setVersions(originalFile.getVersions() != null ?
                        new ArrayList<>(originalFile.getVersions()) : new ArrayList<>());
                clonedFiles.add(clonedFile);
            }
        }
        fileRepository.saveAll(clonedFiles);

        List<String> clonedFileIds = clonedFiles.stream().map(File::getId).collect(Collectors.toList());
        forkedRoom.setFileIds(clonedFileIds);
        roomRepository.save(forkedRoom);
        websocketController.broadcastChange(roomId, "roomForked", forkedRoom);
        websocketController.broadcastChange(newRoomId, "roomForked", forkedRoom);

        return ResponseEntity.ok(forkedRoom);
    }

    @PostMapping("/rooms/merge")
    public ResponseEntity<String> mergeFiles(@RequestParam String sourceRoomName,
                                             @RequestParam String sourceFileName,
                                             @RequestParam String targetRoomName,
                                             @RequestParam String targetFileName,
                                             @RequestParam String username) {
        Room sourceRoom = roomRepository.findByName(sourceRoomName);
        Room targetRoom = roomRepository.findByName(targetRoomName);

        if (sourceRoom == null || targetRoom == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Source or target room not found.");
        }
        File sourceFile = fileRepository.findByRoomIdAndName(sourceRoom.getId(), sourceFileName);
        File targetFile = fileRepository.findByRoomIdAndName(targetRoom.getId(), targetFileName);

        if (sourceFile != null && targetFile != null) {
            String mergedContent = mergeFileContents(sourceFile.getContent(), targetFile.getContent());
            targetFile.setContent(mergedContent);
            sourceFile.setContent(mergedContent);
            fileRepository.save(targetFile);
            fileRepository.save(sourceFile);

            return ResponseEntity.ok("Files merged successfully");
        }
        websocketController.broadcastChange(sourceRoom.getId(), "filesMerged", Map.of("sourceFile", sourceFile, "targetFile", targetFile));
        websocketController.broadcastChange(targetRoom.getId(), "filesMerged", Map.of("sourceFile", sourceFile, "targetFile", targetFile));

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Files not found");
    }

    private String mergeFileContents(String sourceContent, String targetContent) {
        return sourceContent + "\n" + targetContent;
    }
    private boolean hasEditPermission(String roomId, String username) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return false;
        }
        String role = room.getUserRoles().get(username);
        return "ADMIN".equals(role);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users);
    }

    @PostMapping("/rooms/{roomId}/assignRole")
    public ResponseEntity<String> assignRoleToUser(
            @PathVariable String roomId,
            @RequestBody Map<String, String> request) {

        String username = request.get("username");
        String role = request.get("role");

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Room not found.");
        }
        String currentAdminRole = room.getUserRoles().get(request.get("assignerUsername"));
        if (!"ADMIN".equals(currentAdminRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You do not have permission to assign roles.");
        }
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not found.");
        }

        User user = optionalUser.get();
        room.getUserRoles().put(username, role);
        if (!room.getParticipants().contains(user.getUserId())) {
            room.getParticipants().add(user.getUserId());
            System.out.println("User added to participants: " + username);
        } else {
            System.out.println("User already a participant: " + username);
        }
        roomRepository.save(room);

        return ResponseEntity.ok("Role assigned successfully to user: " + username);
    }
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<Map<String, String>>> getRoomParticipants(@PathVariable String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Map<String, String>> participantsWithRoles = new ArrayList<>();

        for (String userId : room.getParticipants()) {
            Optional<User> optionalUser = userRepository.findById(userId);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                String role = room.getUserRoles().get(user.getUsername());
                Map<String, String> participant = new HashMap<>();
                participant.put("username", user.getUsername());
                participant.put("role", role);
                participantsWithRoles.add(participant);
            }
        }
        return ResponseEntity.ok(participantsWithRoles);
    }  @GetMapping("/favicon.ico")
    public void favicon() {
    }
}
