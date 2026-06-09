package com.gzasc.aishopping.chat.internal;

import dev.langchain4j.internal.Json;
import dev.langchain4j.spi.json.JsonCodecFactory;

public class JacksonJsonCodecFactory implements JsonCodecFactory {

    @Override
    public Json.JsonCodec create() {
        return new JacksonJsonCodec();
    }
}
