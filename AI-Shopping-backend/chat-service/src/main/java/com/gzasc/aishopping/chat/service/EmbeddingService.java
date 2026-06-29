package com.gzasc.aishopping.chat.service;

import java.util.List;
import java.util.Map;

public interface EmbeddingService {

    /**
     * 导入文件到向量库
     *
     * @param fileNames 文件名列表（upload 目录下的文件名）
     * @return 失败列表，每项含 fileName 和 error；空列表表示全部成功
     */
    List<Map<String, String>> input(List<String> fileNames);
}
