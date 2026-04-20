package com.gzasc.aishopping.chat.tools;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gzasc.aishopping.common.dto.product.ProductDTO;
import com.gzasc.aishopping.common.feign.product.ProductFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductTools {

    private final ProductFeignClient productFeignClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool("""
            主要作用:获取20条可用的商品列表，包含 ID、名称、价格、标签、库存。
            分页说明:page参数表示页码，从0开始，每页20条商品。如需换页查看其他商品，增加page数值即可。
            特殊情况:如果能获取到商品列表，就按JSON格式返回给用户；否则提示用户'稍后再试'。
            """)
    public String getAllProducts(@P("页码，从0开始，每页20条商品，用于分页查看商品列表") int page) {
        try {
            Map<String, Object> response = productFeignClient.getAllProducts(page);
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object data = response.get("data");
                List<ProductDTO> products = objectMapper.convertValue(data, new TypeReference<List<ProductDTO>>() {});
                return products.stream()
                        .map(p -> String.format("ID: %s, 名称: %s, 价格: %.2f, 标签: %s, 库存: %d",
                                p.getId(), p.getName(), p.getPrice(), p.getTags(), p.getStock()))
                        .collect(Collectors.joining("\n"));
            }
            return "稍后再试";
        } catch (Exception e) {
            return "稍后再试：" + e.getMessage();
        }
    }

    @Tool("""
            主要作用:获取指定id商品的详细信息，包含 ID、名称、价格、标签、描述、库存。
            特殊情况:如果能获取到商品信息，就按JSON格式返回给用户；否则提示用户'id不存在或商品已下架'。
            """)
    public String getProductDetails(@P("商品ID，必须由用户提供方可使用该方法") String productId) {
        try {
            Map<String, Object> response = productFeignClient.getProductById(productId);
            if (response != null && "查询成功".equals(response.get("message"))) {
                Object data = response.get("data");
                ProductDTO product = objectMapper.convertValue(data, ProductDTO.class);
                return String.format("ID: %s, 名称: %s, 价格: %.2f, 标签: %s, 描述: %s, 库存: %d",
                        product.getId(), product.getName(), product.getPrice(), product.getTags(), product.getDescription(), product.getStock());
            }
            return "id不存在或商品已下架";
        } catch (Exception e) {
            return "id不存在或商品已下架：" + e.getMessage();
        }
    }
}