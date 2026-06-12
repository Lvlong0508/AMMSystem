package com.gzasc.aishopping.chat.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_session")
public class ChatSession {

    @Id
    private ObjectId sessionId;
    private Long userId;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}
