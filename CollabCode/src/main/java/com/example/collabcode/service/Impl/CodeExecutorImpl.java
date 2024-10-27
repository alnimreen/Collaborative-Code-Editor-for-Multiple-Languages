package com.example.collabcode.service.Impl;

import com.example.collabcode.model.Code;
import com.example.collabcode.model.ExecResult;
import com.example.collabcode.service.strategy.CodeExecutionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CodeExecutorImpl {

    @Autowired
    private Map<String, CodeExecutionStrategy> executors;

    public ExecResult codeExecutor(Code code) {
        String lang = code.getLang();
        CodeExecutionStrategy executor = executors.get(lang);
        if (executor != null) {
            return executor.execCode(code);
        } else {
            return new ExecResult("Unsupported Language: " + code.getLang(), 0.00F);
        }
    }
}