package com.example.collabcode.repository;


import com.example.collabcode.model.File;
import com.example.collabcode.model.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends MongoRepository<File, String> {
    List<File> findByRoomId(String roomId);

    File findByRoomIdAndName(String roomId, String filename);
}
