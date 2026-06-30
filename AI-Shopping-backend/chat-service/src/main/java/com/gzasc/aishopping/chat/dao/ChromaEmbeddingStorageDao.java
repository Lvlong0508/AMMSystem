package com.gzasc.aishopping.chat.dao;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

import java.util.Collection;
import java.util.List;

public interface ChromaEmbeddingStorageDao {

    List<EmbeddingMatch<TextSegment>> search(EmbeddingSearchRequest request);

    void remove(String id);

    void removeAll(Collection<String> ids);
}
