package com.gzasc.aishopping.tools;

import com.gzasc.aishopping.model.Product;
import com.gzasc.aishopping.service.ProductService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductTools {

    private final ProductService productService;

    @Tool("""
            主要作用:获取20条可用的商品列表，包含 ID、名称、价格、标签、库存。
            分页说明:page参数表示页码，从0开始，每页20条商品。如需换页查看其他商品，增加page数值即可。
            特殊情况:如果能获取到商品列表，就按JSON格式返回给用户；否则提示用户'稍后再试'。
            """)
    public String getAllProducts(@P("页码，从0开始，每页20条商品，用于分页查看商品列表") int page) {
        List<Product> products = productService.getAllProducts(page);
        return products.stream()
                .map(p -> String.format("ID: %s, 名称: %s, 价格: %.2f, 标签: %s, 库存: %d",
                        p.getId(), p.getName(), p.getPrice(), p.getTags(), p.getStock()))
                .collect(Collectors.joining("\n"));
    }

    @Tool("""
            主要作用:获取指定id商品的详细信息，包含 ID、名称、价格、标签、描述、库存。
            特殊情况:如果能获取到商品信息，就按JSON格式返回给用户；否则提示用户'id不存在或商品已下架'。
            """)
    public String getProductDetails(@P("商品ID，必须由用户提供方可使用该方法") String productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return "id不存在或商品已下架";
        }
        return String.format("ID: %s, 名称: %s, 价格: %.2f, 标签: %s, 描述: %s, 库存: %d",
                product.getId(), product.getName(), product.getPrice(), product.getTags(), product.getDescription(), product.getStock());
    }
}