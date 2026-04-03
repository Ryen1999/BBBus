package com.bbbus.contentservice.dto;

import lombok.Data;

@Data
public class BaseObject {
    private String id;

    public BaseObject() {
        this.id = java.util.UUID.randomUUID().toString();
    }
}