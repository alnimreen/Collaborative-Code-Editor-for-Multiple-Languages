package com.example.collabcode.repository;

import com.example.collabcode.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends MongoRepository<Room, String> {
  Optional<Room> findById(String id);
    List<Room> findByUuid(String uuid);
    List<Room> findByOwner(String username);

  Room findByName(String roomName);

  List<Room> findByParticipants(String username);
}
