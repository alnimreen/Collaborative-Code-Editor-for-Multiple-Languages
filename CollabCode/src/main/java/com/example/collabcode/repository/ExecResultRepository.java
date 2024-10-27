package com.example.collabcode.repository;

import com.example.collabcode.model.ExecResult;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface ExecResultRepository extends MongoRepository<ExecResult, String> {
}
