package com.example.collabcode.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.nio.ByteBuffer;
import java.util.*;

@Document(collection ="Rooms")
public class Room {

    @Id
    private String id;
    private String name;
    private String uuid;
    private List<String> fileIds=new ArrayList<>();
    private List<String> participants =new ArrayList<>();
    private Map<String, String> userRoles;
    private String owner;

    public Room(String id,String uuid) {
        this.id=id;
        this.uuid = uuid;
        this.userRoles = new HashMap<>();
        this.userRoles.put(uuid, "Admin");
    }
    public Room() {
        this.userRoles = new HashMap<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Room room = (Room) obj;
        return id.equals(room.id);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<String> fileIds) {
        this.fileIds = fileIds;
    }

    public List<String> getParticipants() {
        return participants;
    }

    public void setParticipants(List<String> participants) {
        this.participants = participants;
    }

    public Map<String, String> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(Map<String, String> userRoles) {
        this.userRoles = userRoles;
    }
    @Override
    public String toString() {
        return "Room{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", uuid='" + uuid + '\'' +
                ", userRoles=" + userRoles +
                ", fileIds=" + fileIds +
                '}';
    }

}