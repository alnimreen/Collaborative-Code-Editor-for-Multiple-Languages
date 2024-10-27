package com.example.collabcode.model;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection ="Code")
public class Code {


    private String code;
    private String lang;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}
