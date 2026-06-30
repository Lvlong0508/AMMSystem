package com.gzasc.aishopping.chat.dao;

import java.util.List;
import java.util.Map;

public interface ChromaAdminDao {

    long count();

    List<Map<String, Object>> getDocuments();

    int deleteBySource(String fileName);
}
