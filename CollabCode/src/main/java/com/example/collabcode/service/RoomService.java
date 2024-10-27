package com.example.collabcode.service;
import com.example.collabcode.dto.RoleAssignmentRequest;
import com.example.collabcode.model.Room;
import com.example.collabcode.model.User;
import com.example.collabcode.repository.RoomRepository;
import com.example.collabcode.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;

    public List<User> getUsersInRoom(String roomId) {
        Optional<Room> roomOptional = roomRepository.findById(roomId);

        if (!roomOptional.isPresent()) {
            return new ArrayList<>();
        }

        Room room = roomOptional.get();
        List<User> users = new ArrayList<>();

        for (String participantId : room.getParticipants()) {
            Optional<User> user = userRepository.findById(participantId);
            user.ifPresent(users::add);
        }

        return users;
    }

    public void assignRole(String roomId, RoleAssignmentRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        if (room.getParticipants().contains(request.getUserId())) {
            room.getUserRoles().put(request.getUserId(), request.getRole());
            roomRepository.save(room);
        } else {
            throw new RuntimeException("User is not part of the room");
        }
    }
}
