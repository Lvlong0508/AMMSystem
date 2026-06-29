package com.gzasc.aishopping.chat.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {

    @Value("${chroma.base-url}")
    private String baseUrl;

    // 默认tenant：default_tenant；和默认database：default_database
    @Value("${chroma.collection-name}")
    private String collectionName;

    @Bean
    public EmbeddingStore<TextSegment> chromaEmbeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(baseUrl)
                .collectionName(collectionName)
                .build();
    }
}
