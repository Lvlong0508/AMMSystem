package com.gzasc.aishopping.chat.assistant;

import com.gzasc.aishopping.chat.AiService.Assistant;
import com.gzasc.aishopping.chat.dto.AiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class testChat {

    @Autowired
    private Assistant assistant;

    @Test
    public void testChatMemory1(){
        AiResponse response1 = assistant.chat(1L,"我是李华");
        System.out.println("message:"+response1.getMessage() +
                "\nreason:"+response1.getReason() +
                "\ndata:"+response1.getData()
        );
        AiResponse response2 = assistant.chat(1L,"我是谁");
        System.out.println("message:"+response2.getMessage() +
                "\nreason:"+response2.getReason() +
                "\ndata:"+response2.getData()
        );

        AiResponse response3 = assistant.chat(2L,"我是谁");
        System.out.println("message:"+response3.getMessage() +
                "\nreason:"+response3.getReason() +
                "\ndata:"+response3.getData()
        );
    }
}
