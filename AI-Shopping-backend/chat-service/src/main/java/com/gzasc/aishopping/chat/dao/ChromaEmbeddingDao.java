package com.gzasc.aishopping.chat.dao;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;
import java.util.Map;

/**
 * Chroma 向量存储 DAO。
 * <p>
 * Embedding API（search/remove/removeAll）继承自 {@link EmbeddingStore}，
 * 管理 API（count/getDocuments/deleteBySource）供知识库文件管理使用。
 */
public interface ChromaEmbeddingDao extends EmbeddingStore<TextSegment> {

    /**
     * 集合中文档总数。
     *
     * @return 文档数
     */
    long count();

    /**
     * 获取文档列表，按 source 元数据分组。
     *
     * @return [{fileName, chunkCount}, ...]，按文件名排序
     */
    List<Map<String, Object>> getDocuments();

    /**
     * 按文件名删除所有 chunks。
     *
     * @param fileName 文件名（source 元数据）
     * @return 删除的 chunk 数量
     */
    int deleteBySource(String fileName);
}
