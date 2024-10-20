package com.example.collabcode.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection ="ExecResult")
public class ExecResult {
    @Id
    private String id; // Optional: if you want an ID for the exec result
    private String out;
    private float tte;
    private String fileName; // Add fileName field

    public ExecResult() {
    }

    public ExecResult(String out, float tte, String fileName) {
        this.out = out;
        this.tte = tte;
        this.fileName = fileName; // Initialize fileName
    }

    public ExecResult(String s, float v) {
    }

    // Getters and Setters
    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public float getTte() {
        return tte;
    }

    public void setTte(float tte) {
        this.tte = tte;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
