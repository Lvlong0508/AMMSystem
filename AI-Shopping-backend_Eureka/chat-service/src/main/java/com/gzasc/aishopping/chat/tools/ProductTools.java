package com.gzasc.aishopping.chat.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.chat.exception.AiToolException;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductTools {

    private final ProductFeignClient productFeignClient;
    private final ObjectMapper objectMapper;

    @Tool("获取商品列表（分页，每页20条）。返回商品信息，包含 shopName")
    public List<Map<String, Object>> getAllProducts(@P("页码，从0开始，每页20条") int page) {
        ApiResponse<Map<String, Object>> response = productFeignClient.getAllProducts(page);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            return Collections.emptyList();
        }
        Map<String, Object> data = response.getData();
        Object rawData = data.get("products");
        if (!(rawData instanceof List)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> rawList = (List<Map<String, Object>>) rawData;
        return rawList.stream().map(item -> {
            Map<String, Object> result = new HashMap<>(item);
            if (item.get("shop") instanceof Map) {
                Map<String, Object> shop = (Map<String, Object>) item.get("shop");
                result.put("shopName", shop.get("name"));
            }
            result.remove("shop");
            return result;
        }).collect(Collectors.toList());
    }

    @Tool("获取指定商品详情。返回商品完整信息，包含 shopName")
    public Map<String, Object> getProductDetails(@P("商品ID") String productId) {
        Long id;
        try {
            id = Long.valueOf(productId);
        } catch (NumberFormatException e) {
            throw new AiToolException("商品ID格式不正确");
        }
        ApiResponse<Map<String, Object>> response = productFeignClient.getProductByIdExternal(id);
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new AiToolException("id不存在或商品已下架");
        }
        Map<String, Object> result = new HashMap<>(response.getData());
        if (result.get("shop") instanceof Map) {
            Map<String, Object> shop = (Map<String, Object>) result.get("shop");
            result.put("shopName", shop.get("name"));
        }
        result.remove("shop");
        return result;
    }
}
