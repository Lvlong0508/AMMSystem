package com.gzasc.aishopping.chat.dao;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;

import java.util.Collection;
import java.util.List;

public interface ChromaEmbeddingStorageDao {

    /**
     * 根据 query embedding 检索最相似的文本段
     *
     * @param request 检索请求（含 query embedding、返回数量等）
     * @return 匹配结果列表，按相似度降序排列
     */
    List<EmbeddingMatch<TextSegment>> search(EmbeddingSearchRequest request);

    /**
     * 根据 ID 删除向量库中的记录
     */
    void remove(String id);

    /**
     * 批量删除向量库中的记录
     */
    void removeAll(Collection<String> ids);
}
