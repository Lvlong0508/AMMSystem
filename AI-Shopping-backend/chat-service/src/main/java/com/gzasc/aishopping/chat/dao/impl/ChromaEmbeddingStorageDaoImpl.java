package com.gzasc.aishopping.chat.dao.impl;

import com.gzasc.aishopping.chat.dao.ChromaEmbeddingStorageDao;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChromaEmbeddingStorageDaoImpl implements ChromaEmbeddingStorageDao {

    private final EmbeddingStore<TextSegment> embeddingStore;

    @Override
    public List<EmbeddingMatch<TextSegment>> search(EmbeddingSearchRequest request) {
        return embeddingStore.search(request).matches();
    }

    @Override
    public void remove(String id) {
        embeddingStore.remove(id);
    }

    @Override
    public void removeAll(Collection<String> ids) {
        embeddingStore.removeAll(ids);
    }
}
