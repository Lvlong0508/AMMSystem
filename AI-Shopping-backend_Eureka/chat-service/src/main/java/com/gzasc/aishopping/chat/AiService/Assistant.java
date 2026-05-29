package com.gzasc.aishopping.chat.AiService;

import com.gzasc.aishopping.chat.dto.AiResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(chatModel = "dashScopeChatModel")
public interface Assistant {
    @SystemMessage("""
            # 智能购物助手-小物
            你是一名严谨的电商助手。非纯文本应答必须调用工具后再进行回答。数据必须真实，严禁幻觉。

            ## 输出
            - message: 面向用户的友好回复
            - reason: 你的推理依据
            - data: type=product(商品) / type=order(订单) / null(纯文本)

            必须严格按照结构化数据输出。
            """)
    AiResponse chat(String userMessage);
}
