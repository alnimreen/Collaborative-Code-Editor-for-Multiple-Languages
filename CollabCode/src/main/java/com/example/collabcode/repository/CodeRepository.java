package com.example.collabcode.repository;

import com.example.collabcode.model.Code;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface CodeRepository extends MongoRepository<Code, String> {
}
