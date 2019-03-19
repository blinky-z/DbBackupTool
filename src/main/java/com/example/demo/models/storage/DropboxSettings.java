package com.example.demo.models.storage;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;

@Entity
@Table(name = "dropbox_settings")
public class DropboxSettings implements StorageSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String accessToken;

    @Column(insertable = false)
    private Date date;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.DROPBOX;
    }

    @Override
    public HashMap<String, String> getProperties() {
        HashMap<String, String> properties = new HashMap<>();

        properties.put("access token", accessToken);

        return properties;
    }
}
