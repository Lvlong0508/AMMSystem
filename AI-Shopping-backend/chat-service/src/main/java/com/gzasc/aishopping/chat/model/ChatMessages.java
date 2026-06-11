package com.gzasc.aishopping.chat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("chat_message")
public class ChatMessages {

    @Id
    private ObjectId messageId;

    private String memoryId;

    private String content;
}
