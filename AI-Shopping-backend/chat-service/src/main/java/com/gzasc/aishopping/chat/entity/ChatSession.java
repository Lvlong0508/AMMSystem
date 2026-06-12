package com.gzasc.aishopping.chat.entity;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document("chat_session")
public class ChatSession {
    @Id
    private ObjectId id;
    private Long userId;
    private String title;
    private Date createdAt;
    private Date updatedAt;
}
