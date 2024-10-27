package com.example.collabcode.service.strategy;

import com.example.collabcode.model.Code;
import com.example.collabcode.model.ExecResult;


public interface CodeExecutionStrategy {
    ExecResult execCode(Code code);
}
