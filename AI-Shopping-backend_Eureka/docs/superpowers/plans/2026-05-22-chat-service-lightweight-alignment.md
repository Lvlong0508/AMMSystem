# Chat Service 轻量对齐实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以最小改动对齐 Auth Service 的响应格式、异常处理、参数校验和安全性规范

**架构:** 保持 Chat Service 扁平结构不变（无 Service 层），仅增加 DTO、Exception、GlobalExceptionHandler，修改 Controller 返回 `ApiResponse`，API Key 改环境变量读取

**Tech Stack:** Java 17, Spring Boot 3.2.3, LangChain4j 0.35.0, DashScope, JUnit 5

---

### Task 1: pom.xml — 添加测试依赖

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\pom.xml`

- [ ] **Step 1: 添加 spring-boot-starter-test 依赖**

  在 `chat-service/pom.xml` 的 dependencies 末尾（`<artifactId>common-api</artifactId>` 之后），添加：

  ```xml
          <dependency>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-test</artifactId>
              <scope>test</scope>
          </dependency>
  ```

- [ ] **Step 2: 验证 pom.xml 格式正确**

  Run: `cd AI-Shopping-backend_Eureka && mvn validate -pl chat-service -q`
  Expected: BUILD SUCCESS

- [ ] **Step 3: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/pom.xml
  git commit -m "chore(chat): add spring-boot-starter-test dependency"
  ```

---

### Task 2: ChatException — 自定义异常类

**Files:**
- Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\main\java\com\gzasc\aishopping\chat\exception\ChatException.java`

- [ ] **Step 1: 创建 ChatException**

  ```java
  package com.gzasc.aishopping.chat.exception;

  public class ChatException extends RuntimeException {
      private int code = 500;

      public ChatException(String message) {
          super(message);
      }

      public ChatException(int code, String message) {
          super(message);
          this.code = code;
      }

      public int getCode() {
          return code;
      }
  }
  ```

- [ ] **Step 2: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/main/java/com/gzasc/aishopping/chat/exception/ChatException.java
  git commit -m "feat(chat): add ChatException"
  ```

---

### Task 3: ChatRequest DTO — 参数校验

**Files:**
- Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\main\java\com\gzasc\aishopping\chat\dto\ChatRequest.java`

- [ ] **Step 1: 创建 ChatRequest DTO**

  ```java
  package com.gzasc.aishopping.chat.dto;

  import jakarta.validation.constraints.NotBlank;
  import lombok.Data;

  @Data
  public class ChatRequest {
      @NotBlank(message = "消息内容不能为空")
      private String message;
  }
  ```

- [ ] **Step 2: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/main/java/com/gzasc/aishopping/chat/dto/ChatRequest.java
  git commit -m "feat(chat): add ChatRequest DTO with validation"
  ```

---

### Task 4: GlobalExceptionHandler — 统一异常处理

**Files:**
- Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\main\java\com\gzasc\aishopping\chat\controller\GlobalExceptionHandler.java`

- [ ] **Step 1: 创建 GlobalExceptionHandler**

  ```java
  package com.gzasc.aishopping.chat.controller;

  import com.gzasc.aishopping.chat.exception.ChatException;
  import com.gzasc.aishopping.common.response.ApiResponse;
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

      @ExceptionHandler(ChatException.class)
      @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
      public ApiResponse<Void> handleChatException(ChatException e) {
          log.error("聊天服务异常: {}", e.getMessage());
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
          return ApiResponse.error(500, "系统错误，请稍后重试");
      }
  }
  ```

- [ ] **Step 2: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/main/java/com/gzasc/aishopping/chat/controller/GlobalExceptionHandler.java
  git commit -m "feat(chat): add GlobalExceptionHandler"
  ```

---

### Task 5: ChatController — 响应格式 + 参数校验

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\main\java\com\gzasc\aishopping\chat\controller\ChatController.java`

- [ ] **Step 1: 修改 ChatController**

  ```java
  package com.gzasc.aishopping.chat.controller;

  import com.gzasc.aishopping.chat.AiService.Assistant;
  import com.gzasc.aishopping.chat.dto.ChatRequest;
  import com.gzasc.aishopping.common.response.ApiResponse;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.web.bind.annotation.*;

  @Slf4j
  @RestController
  @RequestMapping("/chat")
  @RequiredArgsConstructor
  public class ChatController {

      private final Assistant assistant;

      @PostMapping("/chat")
      public ApiResponse<String> chat(@RequestBody @Valid ChatRequest request) {
          String reply = assistant.chat(request.getMessage());
          return ApiResponse.success(reply);
      }
  }
  ```

- [ ] **Step 2: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/main/java/com/gzasc/aishopping/chat/controller/ChatController.java
  git commit -m "refactor(chat): align response format to ApiResponse + add validation"
  ```

---

### Task 6: application.yml — API Key 环境变量化

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\main\resources\application.yml`

- [ ] **Step 1: 替换 api-key 为环境变量引用**

  ```yaml
  langchain4j:
    dashscope:
      chat-model:
        api-key: ${DASHSCOPE_API_KEY}
        model-name: glm-5.1
        enable-search: false
        temperature: 0.7
  ```

- [ ] **Step 2: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/main/resources/application.yml
  git commit -m "security(chat): move DashScope API key to environment variable"
  ```

---

### Task 7: 测试 — SDK 配置读取验证

**Files:**
- Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\test\java\com\gzasc\aishopping\chat\ChatServiceApplicationTests.java`

- [ ] **Step 1: 创建测试配置文件**

  Create: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\chat-service\src\test\resources\application-test.yml`

  ```yaml
  langchain4j:
    dashscope:
      chat-model:
        api-key: test-sk-mock-key
        model-name: glm-5.1
        temperature: 0.7
  ```

- [ ] **Step 2: 创建测试类**

  ```java
  package com.gzasc.aishopping.chat;

  import org.junit.jupiter.api.Test;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.boot.test.context.SpringBootTest;
  import org.springframework.test.context.ActiveProfiles;

  import static org.junit.jupiter.api.Assertions.*;

  @SpringBootTest
  @ActiveProfiles("test")
  class ChatServiceApplicationTests {

      @Value("${langchain4j.dashscope.chat-model.api-key}")
      private String apiKey;

      @Value("${langchain4j.dashscope.chat-model.model-name}")
      private String modelName;

      @Value("${langchain4j.dashscope.chat-model.temperature}")
      private Double temperature;

      @Test
      void apiKeyShouldBeReadable() {
          assertNotNull(apiKey, "API Key should not be null");
          assertEquals("test-sk-mock-key", apiKey);
      }

      @Test
      void modelNameShouldBeGlm5_1() {
          assertEquals("glm-5.1", modelName);
      }

      @Test
      void temperatureShouldBe0_7() {
          assertEquals(0.7, temperature, 0.001);
      }
  }
  ```

- [ ] **Step 3: 运行测试确认通过**

  Run: `cd AI-Shopping-backend_Eureka && mvn test -pl chat-service -Dtest=ChatServiceApplicationTests -q`
  Expected: BUILD SUCCESS, 3 tests pass

- [ ] **Step 4: 提交**

  ```bash
  git add AI-Shopping-backend_Eureka/chat-service/src/test/
  git commit -m "test(chat): verify DashScope SDK configuration loading"
  ```
