package com.gzasc.aishopping.chat.dao.impl;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChromaEmbeddingStorageDaoImplTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @InjectMocks
    private ChromaEmbeddingStorageDaoImpl dao;

    @Captor
    private ArgumentCaptor<EmbeddingSearchRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<Collection<String>> collectionCaptor;

    // ---- search ----

    @Test
    @DisplayName("search - 正常检索返回匹配列表")
    void search_normal_returnsMatches() {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{0.1f, 0.2f}))
                .maxResults(3)
                .build();
        List<EmbeddingMatch<TextSegment>> expected = List.of(
                new EmbeddingMatch<>(0.95, "id1", Embedding.from(new float[]{0.1f, 0.2f}), TextSegment.from("result1")),
                new EmbeddingMatch<>(0.85, "id2", Embedding.from(new float[]{0.3f, 0.4f}), TextSegment.from("result2"))
        );
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(expected));

        List<EmbeddingMatch<TextSegment>> result = dao.search(request);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("result1", result.get(0).embedded().text());
        assertEquals(0.95, result.get(0).score());
        verify(embeddingStore).search(request);
    }

    @Test
    @DisplayName("search - 跨参数透传到 embeddingStore")
    void search_passesRequestThrough() {
        Embedding embedding = Embedding.from(new float[]{0.5f, 0.6f, 0.7f});
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(5)
                .minScore(0.7)
                .build();
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of()));

        dao.search(request);

        verify(embeddingStore).search(requestCaptor.capture());
        EmbeddingSearchRequest captured = requestCaptor.getValue();
        assertArrayEquals(new float[]{0.5f, 0.6f, 0.7f}, captured.queryEmbedding().vector());
        assertEquals(5, captured.maxResults());
        assertEquals(0.7, captured.minScore());
    }

    @Test
    @DisplayName("search - 空结果返回空列表")
    void search_emptyResult_returnsEmptyList() {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(Embedding.from(new float[]{0.1f, 0.2f}))
                .maxResults(10)
                .build();
        when(embeddingStore.search(any())).thenReturn(new EmbeddingSearchResult<>(List.of()));

        List<EmbeddingMatch<TextSegment>> result = dao.search(request);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("search - null 请求抛异常")
    void search_nullRequest_throwsException() {
        assertThrows(NullPointerException.class, () -> dao.search(null));
    }

    // ---- remove ----

    @Test
    @DisplayName("remove - 委托到 embeddingStore.remove")
    void remove_delegatesToStore() {
        dao.remove("abc-123");

        verify(embeddingStore).remove("abc-123");
    }

    @Test
    @DisplayName("remove - null id 透传")
    void remove_nullId_passesThrough() {
        dao.remove(null);

        verify(embeddingStore).remove(null);
    }

    // ---- removeAll ----

    @Test
    @DisplayName("removeAll - 委托到 embeddingStore.removeAll")
    void removeAll_delegatesToStore() {
        List<String> ids = List.of("id1", "id2", "id3");

        dao.removeAll(ids);

        verify(embeddingStore).removeAll(collectionCaptor.capture());
        assertEquals(List.of("id1", "id2", "id3"), collectionCaptor.getValue());
    }

    @Test
    @DisplayName("removeAll - 空集合透传")
    void removeAll_emptyCollection_passesThrough() {
        Collection<String> empty = Collections.emptyList();

        dao.removeAll(empty);

        verify(embeddingStore).removeAll(empty);
    }

    @Test
    @DisplayName("removeAll - null 透传")
    void removeAll_null_passesThrough() {
        dao.removeAll(null);

        verify(embeddingStore).removeAll((Collection<String>) null);
    }
}
