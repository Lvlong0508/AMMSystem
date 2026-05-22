# Product-Service 对齐 Auth-Service 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 product-service 的代码风格、异常体系、响应格式、DTO 规范、项目配置对齐 auth-service 的成熟模式

**Architecture:** 共 8 个独立任务，按依赖顺序执行。common-api 作为公共响应格式层，product-service 作为主要修改对象。所有变更仅涉及 Java 源文件、pom.xml 和 application.yml，不改 API 契约。

**Tech Stack:** Spring Boot 3.x, MyBatis, Lombok, Maven, common-api 模块

---

### Task 1: ApiResponse 迁移到 common-api

**Files:**
- Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\common-api\src\main\java\com\gzasc\aishopping\common\response\ApiResponse.java`
- Delete: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\common\ApiResponse.java`

- [ ] **Step 1: 在 common-api 中创建 ApiResponse**

```java
package com.gzasc.aishopping.common.response;

import java.io.Serializable;

public class ApiResponse<T> implements Serializable {
    private int code;
    private String message;
    private T data;

    public ApiResponse() {}

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
```

- [ ] **Step 2: 删除 product-service 旧版 ApiResponse**

Delete: `product-service/src/main/java/com/gzasc/aishopping/product/common/ApiResponse.java`
Also delete the `common` package directory if it becomes empty.

- [ ] **Step 3: Commit**

```bash
git add common-api/src/main/java/com/gzasc/aishopping/common/response/ApiResponse.java
git rm product-service/src/main/java/com/gzasc/aishopping/product/common/ApiResponse.java
git commit -m "refactor(product-service): 迁移 ApiResponse 到 common-api 模块"
```

---

### Task 2: pom.xml + application.yml 补齐

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\pom.xml`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\resources\application.yml`

- [ ] **Step 1: pom.xml 添加 HikariCP、测试依赖**

在 `</dependencies>` 结束前，`lombok` 依赖之后，添加：

```xml
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
```

- [ ] **Step 2: application.yml 添加 mybatis 日志和 executor 配置**

在 `map-underscore-to-camel-case: true` 后添加两行：

```yaml
    default-executor-type: simple
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 3: Commit**

```bash
git add product-service/pom.xml product-service/src/main/resources/application.yml
git commit -m "chore(product-service): 补齐 pom 和 yml 配置对齐 auth"
```

---

### Task 3: DTO 统一为 @Data（4 个文件）

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\dto\CreateProductRequest.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\dto\UpdateProductRequest.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\dto\StockRequest.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\dto\ListProductRequest.java`

- [ ] **Step 1: CreateProductRequest.java**

替换全部内容为 Lombok @Data 风格：

```java
package com.gzasc.aishopping.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private String description;

    @NotNull(message = "商品价格不能为空")
    @Positive(message = "商品价格必须大于0")
    private BigDecimal price;

    @NotNull(message = "商品库存不能为空")
    @Min(value = 0, message = "商品库存不能小于0")
    private Integer stock;

    @NotBlank(message = "商品图片不能为空")
    private String imageUrl;
}
```

- [ ] **Step 2: UpdateProductRequest.java**

替换全部内容为 Lombok @Data：

```java
package com.gzasc.aishopping.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
}
```

- [ ] **Step 3: StockRequest.java**

替换全部内容为 Lombok @Data：

```java
package com.gzasc.aishopping.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class StockRequest {

    @NotBlank(message = "商品ID不能为空")
    private String productId;

    @NotNull(message = "库存数量不能为空")
    @Positive(message = "库存数量必须大于0")
    private Integer quantity;
}
```

- [ ] **Step 4: ListProductRequest.java**

替换全部内容为 Lombok @Data：

```java
package com.gzasc.aishopping.product.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ListProductRequest {
    private Integer page = 0;
    private Integer size = 10;
    private String category;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
```

- [ ] **Step 5: Commit**

```bash
git add product-service/src/main/java/com/gzasc/aishopping/product/dto/
git commit -m "refactor(product-service): DTO 统一为 Lombok @Data 风格"
```

---

### Task 4: InternalProductController 使用 common-api StockDeductRequest

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\controller\internal\InternalProductController.java`

- [ ] **Step 1: 替换内联类为 common-api 的 StockDeductRequest**

当前 InternalProductController 内部定义了 `StockDeductRequest` 静态内部类，但 common-api 已有完全相同的 `com.gzasc.aishopping.common.dto.product.StockDeductRequest`。删除内部类，改用 common-api 的。

修改后的完整文件：

```java
package com.gzasc.aishopping.product.controller.internal;

import com.gzasc.aishopping.common.dto.product.StockDeductRequest;
import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/product")
@RequiredArgsConstructor
public class InternalProductController {

    private final ProductService productService;

    @GetMapping("/{productId}")
    public ProductWithImageDetailDTO getProductById(@PathVariable("productId") String productId) {
        return productService.getProductById(productId);
    }

    @GetMapping("/batch")
    public List<ProductWithImageAbstractDTO> getProductsByIds(@RequestParam("ids") String ids) {
        List<String> idList = Arrays.asList(ids.split(","));
        return productService.getAbstractProductsForBuyer(idList);
    }

    @PostMapping("/deduct-stock")
    public Map<String, Object> deductStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.deductStock(request.getProductId(), request.getQuantity());
        return Map.of("success", success, "message", success ? "扣减成功" : "扣减失败：库存不足");
    }

    @PostMapping("/restore-stock")
    public Map<String, Object> restoreStock(@RequestBody StockDeductRequest request) {
        boolean success = productService.restoreStock(request.getProductId(), request.getQuantity());
        return Map.of("success", success, "message", success ? "恢复成功" : "恢复失败");
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add product-service/src/main/java/com/gzasc/aishopping/product/controller/internal/InternalProductController.java
git commit -m "refactor(product-service): 改用 common-api 的 StockDeductRequest"
```

---

### Task 5: 异常体系简化（4 类 → 1 类）

**Files:**
- Delete: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\exception\ProductServiceException.java`
- Delete: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\exception\ProductNotFoundException.java`
- Delete: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\exception\ProductOnSaleException.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\service\impl\ProductServiceImpl.java`

- [ ] **Step 1: 删除 3 个多余异常文件**

Git rm 以下文件：
- `product-service/.../exception/ProductServiceException.java`
- `product-service/.../exception/ProductNotFoundException.java`
- `product-service/.../exception/ProductOnSaleException.java`

- [ ] **Step 2: 更新 ProductServiceImpl 中的引用**

`ProductServiceImpl.java` 中：
- 删除 import `ProductOnSaleException` 和 `ProductNotFoundException`
- 将 `throw new ProductNotFoundException(productId)` → `throw new ProductException(404, "商品不存在: " + productId)`
- 将 `throw new ProductOnSaleException(productId)` → `throw new ProductException(400, "商品在上架中，请先下架: " + productId)`

修改点汇总：

| 原代码 | 改为 |
|--------|------|
| `throw new ProductNotFoundException(productId)` (2处) | `throw new ProductException(404, "商品不存在: " + productId)` |
| `throw new ProductOnSaleException(productId)` (1处) | `throw new ProductException(400, "商品在上架中，请先下架: " + productId)` |
| import `ProductNotFoundException` | 删除 |
| import `ProductOnSaleException` | 删除 |

- [ ] **Step 3: Commit**

```bash
git rm product-service/src/main/java/com/gzasc/aishopping/product/exception/ProductServiceException.java
git rm product-service/src/main/java/com/gzasc/aishopping/product/exception/ProductNotFoundException.java
git rm product-service/src/main/java/com/gzasc/aishopping/product/exception/ProductOnSaleException.java
git add product-service/src/main/java/com/gzasc/aishopping/product/service/impl/ProductServiceImpl.java
git commit -m "refactor(product-service): 简化异常体系为单一 ProductException"
```

---

### Task 6: Controller 层 import 更新 + GlobalExceptionHandler 对齐

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\controller\GlobalExceptionHandler.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\controller\ProductSellerController.java`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\controller\ProductUserController.java`

- [ ] **Step 1: 更新 GlobalExceptionHandler**

将 `import com.gzasc.aishopping.product.common.ApiResponse` 改为 `import com.gzasc.aishopping.common.response.ApiResponse`
删除 `ProductServiceException` 的 import 和 handler 方法。
删除 `import com.gzasc.aishopping.product.exception.ProductServiceException;`

修改后的完整文件：

```java
package com.gzasc.aishopping.product.controller;

import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.product.exception.ProductException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ProductException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleProductException(ProductException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数验证失败";
        log.warn("参数验证失败: {}", message);
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统错误", e);
        return ApiResponse.error("系统错误，请稍后重试");
    }
}
```

- [ ] **Step 2: 更新 ProductSellerController 和 ProductUserController 的 import**

两个文件中将：
```java
import com.gzasc.aishopping.product.common.ApiResponse;
```
改为：
```java
import com.gzasc.aishopping.common.response.ApiResponse;
```

同时在 `ProductSellerController.java` 中，将以下 import 删除（因为 ProductServiceException 已被删除，没有其他类使用它）：
```java
import com.gzasc.aishopping.product.exception.ProductException;
```
（注意：ProductException 仍然被使用，不要删这个。但 ProductSellerController 中没有 import ProductServiceException，所以不需要改。）

检查 `ProductUserController.java` — 没有 import ProductServiceException，也不需要改。

- [ ] **Step 3: Commit**

```bash
git add product-service/src/main/java/com/gzasc/aishopping/product/controller/
git commit -m "refactor(product-service): 更新 controller 层 ApiResponse import 路径"
```

---

### Task 7: Converter 简化

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\product-service\src\main\java\com\gzasc\aishopping\product\converter\ProductConverter.java`

- [ ] **Step 1: 删除未使用的 toDetailDTOList 方法**

`toDetailDTOList(List<Product>)` 方法在 ProductService 中没有任何调用链。删除该方法：

```java
    // 删除整个方法:
    public List<ProductDetailDTO> toDetailDTOList(List<Product> products) {
        if (products == null) return List.of();
        return products.stream().map(this::toDetailDTO).collect(Collectors.toList());
    }
```

同时检查 `import java.util.stream.Collectors` — 如果删掉的方法后 stream/Collectors 还在其他方法中使用，则保留 import；否则删除。

检查：`toAbstractDTOList`, `toImageDTOList`, `toAbstractWithImageDTOList`, `toDetailWithImageDTOList` 都使用了 `.stream()` 和 `collect(Collectors.toList())`，所以 import 保留。

- [ ] **Step 2: Commit**

```bash
git add product-service/src/main/java/com/gzasc/aishopping/product/converter/ProductConverter.java
git commit -m "refactor(product-service): 简化 ProductConverter 删除未使用方法"
```

---

### Task 8: 编译验证

- [ ] **Step 1: 先编译 common-api**

```bash
mvn clean compile -pl common-api -am -f F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 编译 product-service**

```bash
mvn clean compile -pl product-service -am -f F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\pom.xml
```

Expected: BUILD SUCCESS

- [ ] **Step 3: 如果有编译错误，逐个修复并重新编译**

可能的问题：
1. ApiResponse import 路径错误 → 修正 import
2. ProductServiceImpl 中仍有对已删除异常类的引用 → 改为 ProductException

- [ ] **Step 4: 最终确认无 diff 问题**

```bash
git status
git diff --stat
```

Expected: 改动的文件数符合预期，无多余修改。
