package com.gzasc.aishopping.chat.service;

import java.util.List;
import java.util.Map;

public interface EmbeddingService {

    /**
     * 将文件列表导入向量库：解析文档 → 文本切分 → 生成 embedding → 持久化，
     * 成功后异步将文件移至 finish 目录。
     *
     * @param fileNames upload 目录下的文件名列表
     * @return 失败项列表 [{fileName, error}]，全部成功则返回空列表
     */
    List<Map<String, String>> ingest(List<String> fileNames, Long userId);

    /**
     * 为给定文本生成 embedding 向量。
     */
    float[] embed(String text);

    /**
     * 返回集合级别统计信息：总文档数、总文本段数。
     */
    Map<String, Object> getCollectionStats();

    /**
     * 按源文件名分组列出向量库中的所有文档。
     */
    List<Map<String, Object>> getDocuments();

    /**
     * 获取最近导入的文档列表，按导入时间降序排列。
     */
    List<Map<String, Object>> getRecentDocuments(int limit);

    /**
     * 概览页统一数据：并发获取集合统计和最近导入文档。
     */
    Map<String, Object> getOverview();

    /**
     * 语义搜索：文本向量化 → 向量检索 → 返回排序结果。
     *
     * @param query 自然语言查询字符串
     * @param topK  返回结果数量上限
     */
    List<Map<String, Object>> search(String query, int topK);

    /**
     * 删除指定源文件在向量库中的所有关联记录。
     *
     * @param fileName Chroma metadata 中的 source 字段值
     * @return 删除的记录数
     */
    int deleteFromVector(String fileName);
}
