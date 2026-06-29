package com.gzasc.aishopping.chat.service;

import java.util.List;
import java.util.Map;

public interface RAGService {

    /**
     * 批量导入文件到向量库
     *
     * @param filePaths 文件路径列表
     * @return 失败映射，key=文件名，value=失败原因；空 map 表示全部成功
     */
    Map<String, String> input(List<String> filePaths);
}
