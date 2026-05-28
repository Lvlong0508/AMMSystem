# product-service 测试用例文档

> 版本: 1.0 | 日期: 2026-05-28

---

## 1. 概述

本文档定义 product-service（端口 8081，数据库 eureka_product）的测试用例。覆盖用户端商品查询、商家端商品管理、内部库存操作、库存预占确认释放、上架下架生命周期、定时过期清理、异常处理等核心业务流程。

---

## 2. 测试环境

| 项目 | 配置 |
|------|------|
| 数据库 | MySQL 5.7+，数据库 eureka_product |
| 缓存 | Caffeine（店铺信息缓存 10min/最大1000条） |
| 注册中心 | Eureka Server |
| 服务端口 | 8081 |
| ID 生成 | 雪花算法 (Snowflake ID) |

---

## 3. 测试用例表

### 3.1 商品查询（用户端）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-001 | GET /api/user/product/all - 正常分页 | 数据库有 20 条可售商品 (is_sale=true)，10 条不可售 | 1. 调用 GET /api/user/product/all?page=0<br>2. 调用 GET /api/user/product/all?page=1 | 1. page=0 返回第一页数据，products 不为空，page=0（size 不可配置，固定每页 20 条）<br>2. page=1 返回第二页或无更多数据 | P0 |
| PR-002 | GET /api/user/product/all - 空数据分页 | 无可售商品 | 调用 GET /api/user/product/all?page=0 | 返回 ApiResponse，products 为空列表，page=0 | P1 |
| PR-003 | GET /api/user/product/all - size 参数不生效 | 有足够可售商品 | 调用 GET /api/user/product/all?page=0（size 参数不被接受，分页大小硬编码为 20） | 返回 20 条商品记录（size 参数无效，固定每页 20 条） | P1 |
| PR-004 | GET /api/user/product/{productId} - 查询详情 | 商品 ID=1001 存在且为可售状态 | 调用 GET /api/user/product/1001 | 返回 ApiResponse\<ProductWithImageDetailDTO\>，包含完整商品信息及图片 | P0 |
| PR-005 | GET /api/user/product/{productId} - 商品不存在 | 商品 ID=99999 不存在 | 调用 GET /api/user/product/99999 | 返回 400，ProductException，提示商品不存在 | P1 |
| PR-006 | GET /api/user/product/{productId} - 商品已下架（无 is_sale 过滤） | 商品 ID=1002 is_sale=false | 调用 GET /api/user/product/1002 | 返回 200，ApiResponse\<ProductWithImageDetailDTO\>，包含完整商品信息（用户端查询无 is_sale 过滤，任何商品均可查询） | P1 |
| PR-007 | GET /api/user/product/search - 模糊搜索有结果 | 商品名包含 "手机" 的记录有 3 条 | 调用 GET /api/user/product/search?name=手机 | 返回 ApiResponse，products 包含匹配商品，total=3 | P0 |
| PR-008 | GET /api/user/product/search - 模糊搜索无结果 | 无商品名匹配 "不存在商品xxx" | 调用 GET /api/user/product/search?name=不存在商品xxx | 返回 ApiResponse，products 为空列表，total=0 | P1 |
| PR-009 | GET /api/user/product/search - 搜索关键词为空 | — | 调用 GET /api/user/product/search?name= | 返回所有商品（LIKE '%%' 匹配全部，无空关键词校验） | P2 |
| PR-010 | GET /api/user/product/price-range - 正常区间 | 价格 50-200 的商品有 5 条 | 调用 GET /api/user/product/price-range?minPrice=50&maxPrice=200&page=0 | 返回 price 在 [50,200] 内的商品分页，包含所选页面数据 | P0 |
| PR-011 | GET /api/user/product/price-range - 价格区间无交集 | 无价格 >1000 的商品 | 调用 GET /api/user/product/price-range?minPrice=1000&maxPrice=10000&page=0 | 返回 ApiResponse，products 为空列表 | P1 |
| PR-012 | GET /api/user/product/price-range - minPrice > maxPrice | — | 调用 GET /api/user/product/price-range?minPrice=200&maxPrice=50&page=0 | 返回空列表（无参数校验，BETWEEN 空区间返回空结果） | P2 |
| PR-013 | GET /api/user/product/price-range - 边界值测试 | 商品价格恰好等于 100 和 200 | 调用 GET /api/user/product/price-range?minPrice=100&maxPrice=200&page=0 | 包含 price=100 和 price=200 的商品（闭区间） | P2 |

### 3.2 商品管理（商家端）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-014 | POST /api/seller/product/create - 创建商品（含图片） | 商家已登录，图片 URL 有效 | 1. 构造 CreateProductRequest：name="测试商品A"，description="描述"，price=99.99，stock=100，imageUrl="http://img.test/a.jpg"<br>2. 调用 POST /api/seller/product/create | 返回 ApiResponse，productId 为雪花ID（非空数值），products 表插入记录，product_image_info 表插入图片记录 | P0 |
| PR-015 | POST /api/seller/product/create - 创建商品（无图片被拦截） | 商家已登录，imageUrl 为空 | 构造 CreateProductRequest，imageUrl=""，调用 POST | 返回 400，MethodArgumentNotValidException，提示"商品图片不能为空"（imageUrl 为 @NotBlank 必填） | P1 |
| PR-016 | POST /api/seller/product/create - 请求体缺少必填字段（name 为空） | — | 构造 CreateProductRequest：name=""（或缺失），其余字段合法 | 返回 400，MethodArgumentNotValidException，提示 name 不能为空 | P1 |
| PR-017 | POST /api/seller/product/create - price 为负数 | — | 构造 CreateProductRequest：price=-10，其余合法 | 返回 400，MethodArgumentNotValidException | P1 |
| PR-018 | POST /api/seller/product/create - stock 为负数 | — | 构造 CreateProductRequest：stock=-1，其余合法 | 返回 400，MethodArgumentNotValidException | P2 |
| PR-019 | POST /api/seller/product/create - name 超长 | — | 构造 CreateProductRequest：name 超过字段最大长度 | 返回 400，MethodArgumentNotValidException | P2 |
| PR-020 | PUT /api/seller/product/{productId} - 更新商品名称和价格 | 商品 ID=2001 存在 | 1. 构造 UpdateProductRequest：name="新名称"，price=199<br>2. 调用 PUT /api/seller/product/2001 | 返回 ApiResponse\<Void\>，products 表对应记录 name 和 price 已更新 | P0 |
| PR-021 | PUT /api/seller/product/{productId} - 更新商品图片（替换） | 商品已有关联图片记录 | 构造 UpdateProductRequest 含新 imageUrl | 返回成功，product_image_info 表更新图片 URL | P1 |
| PR-022 | PUT /api/seller/product/{productId} - 更新商品图片（新增不受支持） | 商品原无图片（imageId=0） | 构造 UpdateProductRequest 含 imageUrl | 返回成功，但 product_image_info 不新增记录（当前仅支持替换已有图片，不支持从无到有新增） | P1 |
| PR-023 | PUT /api/seller/product/{productId} - 商品不存在 | productId=99999 不存在 | 构造合法请求体，调用 PUT | 返回 400，ProductException 提示商品不存在 | P1 |
| PR-024 | PUT /api/seller/product/{productId} - 更新全部字段 | 商品 ID=2002 存在 | 构造 UpdateProductRequest 含 name, description, price, stock, imageUrl 全部字段 | 返回成功，数据库全部字段已更新 | P1 |
| PR-025 | DELETE /api/seller/product/{productId} - 已下架商品可删除 | 商品 ID=3001 is_sale=false | 1. 确认商品已下架<br>2. 调用 DELETE /api/seller/product/3001 | 返回 ApiResponse\<Void\>，200 OK，products 表记录被删除 | P0 |
| PR-026 | DELETE /api/seller/product/{productId} - 未下架商品不可删除 | 商品 ID=3002 is_sale=true | 调用 DELETE /api/seller/product/3002 | 返回 400，ProductException 提示"商品在上架中，请先下架: {productId}" | P1 |
| PR-027 | DELETE /api/seller/product/{productId} - 商品不存在 | productId=99999 不存在 | 调用 DELETE /api/seller/product/99999 | 返回 400，ProductException 提示商品不存在 | P1 |
| PR-028 | GET /api/seller/product/{productId} - 商家查询商品详情 | 商品 ID=4001 存在（可售或不可售均可） | 调用 GET /api/seller/product/4001 | 返回 ApiResponse\<ProductWithImageDetailDTO\>，包含完整信息，不受 is_sale 限制 | P0 |
| PR-029 | GET /api/seller/product/{productId} - 商家查询不存在的商品 | productId=99999 | 调用 GET /api/seller/product/99999 | 返回 400，ProductException | P1 |
| PR-030 | GET /api/seller/product/batch - 批量查询 | ids=1001,1002,1003 存在且有效 | 调用 GET /api/seller/product/batch?ids=1001,1002,1003 | 返回 ApiResponse\<List\<ProductWithImageAbstractDTO\>\>，包含 3 条商品摘要 | P0 |
| PR-031 | GET /api/seller/product/batch - 批量查询含无效 ID | ids=1001,99999，其中 99999 不存在 | 调用 GET /api/seller/product/batch?ids=1001,99999 | 返回列表仅包含 ID=1001 的记录，ID=99999 被忽略 | P1 |
| PR-032 | GET /api/seller/product/batch - 空 ID 列表 | ids 为空 | 调用 GET /api/seller/product/batch?ids= | 返回空列表或 400 错误 | P2 |

### 3.3 内部接口（基础操作）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-033 | GET /internal/product/{productId} - 内部查询商品详情 | 商品 ID=5001 存在 | 调用 GET /internal/product/5001 | 返回 ProductWithImageDetailDTO，包含完整信息 | P1 |
| PR-034 | GET /internal/product/{productId} - 商品不存在 | ID=99999 | 调用 GET /internal/product/99999 | 返回 null 或 404（内部接口行为） | P1 |
| PR-035 | GET /internal/product/batch - 批量查询 | ids=5001,5002,5003 | 调用 GET /internal/product/batch?ids=5001,5002,5003 | 返回 List\<ProductWithImageAbstractDTO\>，3 条 | P1 |
| PR-036 | POST /internal/product/create - 内部创建商品 | 提供完整 Product 对象 | 调用 POST /internal/product/create | 返回 ApiResponse，商品创建成功 | P1 |
| PR-037 | GET /internal/product/by-shop/{shopId} - 按店铺分页查询 | shopId=1 有 15 条商品 | 调用 GET /internal/product/by-shop/1?page=1&size=10 | 返回 ApiResponse\<List\>，第一页 10 条记录（page 从 1 开始） | P1 |
| PR-038 | GET /internal/product/by-shop/{shopId} - 店铺无商品 | shopId=999 | 调用 GET /internal/product/by-shop/999?page=1&size=10 | 返回空列表 | P1 |
| PR-039 | GET /internal/product/by-shop/{shopId} - 分页第 N 页 | shopId=1 有 25 条 | 调用 page=3，size=10 | 返回第三页，5 条记录（page 从 1 开始） | P2 |

### 3.4 库存预占 / 确认 / 释放

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-040 | POST /internal/product/reserve-stock - 正常预占 | 商品 stock=100，无其他预占 | 1. 构造 StockReserveRequest：productId=6001，quantity=3，orderId=ORDER001<br>2. 调用 POST /internal/product/reserve-stock | 返回 Map，成功标记 true，预占记录写入 product_reservation，状态=RESERVED | P0 |
| PR-041 | POST /internal/product/reserve-stock - 库存足够但接近上限 | 商品 stock=5，预占 quantity=5 | 调用预占 5 件 | 返回成功，可用量刚好够 | P1 |
| PR-042 | POST /internal/product/reserve-stock - 预占数量 > 库存 | 商品 stock=3，预占 quantity=5 | 调用预占 | 返回失败标记，提示库存不足，不写入预占记录 | P1 |
| PR-043 | POST /internal/product/reserve-stock - 预占数量 > 可用量（已有预占占用） | 商品 stock=10，已有预占 RESERVED=8，本次预占 quantity=5（可用量=2） | 调用预占 5 件 | 返回失败，可用量不足提示 | P1 |
| PR-044 | POST /internal/product/reserve-stock - 预占数量为 0 | quantity=0 | 调用预占 | 返回 400 或业务校验失败 | P2 |
| PR-045 | POST /internal/product/reserve-stock - 预占数量为负数 | quantity=-1 | 调用预占 | 返回 400 或业务校验失败 | P2 |
| PR-046 | POST /internal/product/reserve-stock - 商品不存在 | productId=99999 | 调用预占 | 返回失败标记或 400 | P1 |
| PR-047 | POST /internal/product/reserve-stock - 并发预占同一商品 | 商品 stock=10，两个订单各预占 6 | 1. 线程 A 预占 6<br>2. 线程 B 并发预占 6 | 一个成功一个失败（SELECT FOR UPDATE 互斥） | P0 |
| PR-048 | POST /internal/product/confirm-reservation - 正常确认 | 预占记录 ORDER002 存在且状态=RESERVED | 调用 POST /internal/product/confirm-reservation?orderId=ORDER002 | 返回 Map（成功），预占状态改为 CONFIRMED，products.stock 扣减对应数量 | P0 |
| PR-049 | POST /internal/product/confirm-reservation - 订单不存在 | orderId=NOT_EXIST | 调用确认 | 返回失败标记，提示预占记录不存在 | P1 |
| PR-050 | POST /internal/product/confirm-reservation - 重复确认 | ORDER003 已为 CONFIRMED 状态 | 再次调用确认 | 返回失败，提示"预占状态已变更，无法确认" | P1 |
| PR-051 | POST /internal/product/confirm-reservation - 确认已释放的预占 | ORDER004 状态=RELEASED | 调用确认 | 返回失败，提示"预占状态已变更，无法确认" | P2 |
| PR-052 | POST /internal/product/release-reservation - 正常释放 | ORDER005 状态=RESERVED | 调用 POST /internal/product/release-reservation?orderId=ORDER005 | 返回 Map（成功），预占状态改为 RELEASED，不扣减 stock | P0 |
| PR-053 | POST /internal/product/release-reservation - 释放已确认的预占 | ORDER006 状态=CONFIRMED | 调用释放 | 返回 400 或失败，已确认的预占不可释放 | P2 |
| PR-054 | POST /internal/product/release-reservation - 订单不存在 | orderId=NOT_EXIST | 调用释放 | 返回成功（静默跳过，release() 查不到记录时直接 return 不抛异常） | P1 |
| PR-055 | POST /internal/product/deduct-stock - 直接扣减库存 | 商品 stock=100，扣减 quantity=10 | 调用 POST /internal/product/deduct-stock | 返回 Map，成功标记 true，products.stock=90 | P0 |
| PR-056 | POST /internal/product/deduct-stock - 库存不足扣减 | 商品 stock=3，扣减 quantity=5 | 调用扣减（CAS UPDATE ... WHERE stock >= quantity） | 返回失败标记，库存未变更 | P1 |
| PR-057 | POST /internal/product/deduct-stock - 并发扣减到零 | 商品 stock=1，两个请求各扣 1 | 两线程同时扣减 | 一个成功 stock=0，一个失败（CAS 条件不满足） | P1 |
| PR-058 | POST /internal/product/restore-stock - 正常恢复 | 商品 stock=90，恢复 quantity=5 | 调用 POST /internal/product/restore-stock | 返回成功，products.stock=95 | P1 |
| PR-059 | POST /internal/product/restore-stock - 恢复超量（非负检查） | 商品 stock=90，恢复 quantity=999 | 调用恢复 | stock 变为 999+90，或服务端做上限保护 | P2 |

### 3.5 上架 / 下架

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-060 | POST /api/seller/product/{productId}/list - 上架商品 | 商品 ID=7001 is_sale=false | 1. 调用 POST /api/seller/product/7001/list<br>2. 查询数据库 | 返回 ApiResponse\<Void\>，products.is_sale=true，salable_products 表新增记录 | P0 |
| PR-061 | POST /api/seller/product/{productId}/list - 重复上架（无幂等） | 商品已为 is_sale=true | 调用上架 | 返回 500 错误（salable_products 唯一约束冲突，无幂等处理） | P1 |
| PR-062 | POST /api/seller/product/{productId}/list - 商品不存在 | productId=99999 | 调用上架 | 返回 400，ProductException | P1 |
| PR-063 | POST /api/seller/product/{productId}/unlist - 下架商品 | 商品 ID=7002 is_sale=true | 1. 调用 POST /api/seller/product/7002/unlist<br>2. 查询数据库 | 返回 ApiResponse\<Void\>，products.is_sale=false，salable_products 表移除记录 | P0 |
| PR-064 | POST /api/seller/product/{productId}/unlist - 重复下架 | 商品已为 is_sale=false | 调用下架 | 返回成功（无幂等检查，DELETE 操作本身幂等，静默重复执行） | P1 |
| PR-065 | POST /api/seller/product/{productId}/unlist - 商品不存在 | productId=99999 | 调用下架 | 返回 400，ProductException | P1 |
| PR-066 | 上架后用户端可查询到 | 1. 商品 7003 原下架<br>2. 执行上架 | 1. 调用上架<br>2. 调用 GET /api/user/product/7003 | 用户端可成功查询到该商品详情 | P0 |
| PR-067 | 下架后用户端可查询到（但不在 /all 列表） | 1. 商品 7004 原可售<br>2. 执行下架 | 1. 调用下架<br>2. 调用 GET /api/user/product/7004 | 用户端仍可查询到商品详情（getProductById 无 is_sale 过滤），但不出现在 /all 列表中 | P0 |
| PR-068 | 下架商品不出现在 /all 列表中 | 商品 7005 原可售，执行下架 | 1. 下架 7005<br>2. 调用 GET /api/user/product/all | /all 返回列表中不包含 7005 | P1 |

### 3.6 定时任务（过期预占清理）

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-069 | @Scheduled 清理 - 清理过期 RESERVED 预占 | 预占记录 ORDER8001 创建于 35 分钟前，状态=RESERVED | 1. 等待定时任务触发（fixedRate=120000ms）或手动触发<br>2. 检查数据库 | ORDER8001 状态改为 RELEASED，products.stock 不变（原未扣减） | P1 |
| PR-070 | @Scheduled 清理 - 未过期预占不被清理 | ORDER8002 创建于 5 分钟前，状态=RESERVED | 等待定时任务触发 | ORDER8002 状态仍为 RESERVED，未被影响 | P1 |
| PR-071 | @Scheduled 清理 - CONFIRMED 状态的预占不受影响 | ORDER8003 创建于 40 分钟前，状态=CONFIRMED | 等待定时任务触发 | ORDER8003 状态保持 CONFIRMED，不被清理 | P1 |
| PR-072 | @Scheduled 清理 - 清理后库存释放可用量 | 商品 stock=10，过期预占占用了 3 件 | 1. 等待清理<br>2. 尝试预占 8 件 | 预占成功（可用量恢复为 10） | P1 |
| PR-073 | @Scheduled 清理 - 大量过期记录并发清理 | 有 1000 条过期 RESERVED 预占 | 等待定时任务触发 | 全部改为 RELEASED，无死锁或性能异常 | P2 |

### 3.7 异常处理

| 编号 | 测试项 | 前置条件 | 测试步骤 | 预期结果 | 优先级 |
|------|--------|----------|----------|----------|--------|
| PR-074 | ProductException - 业务异常返回 400 | — | 触发任意 ProductException（如查询不存在商品） | 返回 HTTP 400，响应体为 ApiResponse，含错误 code 和 message | P0 |
| PR-075 | MethodArgumentNotValidException - 参数校验失败 | — | 发送非法参数请求（如 price 为负数） | 返回 HTTP 400，包含具体字段校验失败提示 | P1 |
| PR-076 | Exception - 未预期异常返回 500 | — | 模拟空指针等未捕获异常 | 返回 HTTP 500，ApiResponse 含通用错误信息，不泄露堆栈详情 | P1 |
| PR-077 | Caffeine 缓存 - 店铺信息缓存命中 | 同一店铺已查询过 | 1. 首次查询店铺信息（查库）<br>2. 再次查询同一店铺 | 第二次查询命中缓存，不访问数据库 | P1 |
| PR-078 | Caffeine 缓存 - 店铺信息缓存过期 | 缓存已存在且超过 10 分钟 | 1. 等待 10 分钟以上<br>2. 再次查询同一店铺 | 缓存过期，重新从数据库加载 | P2 |
| PR-079 | Caffeine 缓存 - 最大 1000 条限制 | 缓存已存满 1000 条不同店铺 | 查询第 1001 个不同店铺 | 正常加载数据，淘汰最早或最久未访问的缓存条目 | P2 |
| PR-080 | 雪花 ID - 商品创建后 ID 全局唯一 | 多次调用创建 | 1. 创建商品 A<br>2. 创建商品 B | A.ID ≠ B.ID，且 ID 为数字格式 | P1 |
| PR-081 | 事务回滚 - 商品创建时图片写入失败 | 模拟 product_image_info 写入异常 | 构造含 imageUrl 的创建请求，mock 图片 Mapper 抛出异常 | products 表不插入（事务回滚），商品未创建 | P1 |
| PR-082 | 事务回滚 - 商品更新时图片更新失败 | 模拟图片更新异常 | 调用更新含新图片，mock 图片 Mapper 更新抛出异常 | products 表不更新（事务回滚），商品数据不变 | P1 |
| PR-083 | DELETE 已下架商品后 salable_products 未同步清理 | 商品 ID=9001 已下架且存在于 salable_products | 执行删除 | product 记录删除，但 salable_products 对应记录未被清理（需手动处理） | P2 |

---

## 4. 测试要点总结

### 4.1 核心流程（P0，共 13 条）

- PR-001 用户端商品分页
- PR-004 用户端商品详情
- PR-007 用户端模糊搜索
- PR-010 用户端价格区间查询
- PR-014 商家创建商品（含图片）
- PR-020 商家更新商品
- PR-025 商家删除已下架商品
- PR-028 商家查询商品详情
- PR-030 商家批量查询
- PR-040 库存预占（正常流程）
- PR-048 库存确认
- PR-052 库存释放
- PR-055 直接扣减库存
- PR-060 上架 / PR-063 下架
- PR-066 上架下架用户端可见性验证
- PR-074 业务异常返回 400

### 4.2 关键边界与并发

- **库存预占并发** (PR-047)：SELECT FOR UPDATE 悲观锁必须互斥
- **库存扣减 CAS** (PR-057)：UPDATE ... WHERE stock >= quantity 保证原子性
- **价格区间边界** (PR-013)：确认闭区间包含端点
- **预占数量等于库存极限** (PR-041)：恰好用完的边界

### 4.3 定时任务验证

- 每 2 分钟扫描并释放超时 (30min) RESERVED 预占 (PR-069~PR-072)
- CONFIRMED 状态的预占不被定时任务干扰 (PR-071)
- 大量过期记录并发清理无死锁 (PR-073)

### 4.4 事务一致性与异常路径

- **创建事务** (PR-081)：商品 + 图片必须全部成功或全部回滚
- **更新事务** (PR-082)：商品 + 图片更新原子性
- **删除前置条件** (PR-026)：未下架商品拒绝删除
- **重复操作防御** (PR-050, PR-061, PR-064)：重复确认有防御（RESERVED→CONFIRMED 状态检查），重复上架无幂等（唯一约束冲突），重复下架静默成功

### 4.5 缓存验证

- Caffeine 店铺缓存命中 (PR-077)
- 10 分钟过期 (PR-078)
- 1000 条 LRU 淘汰 (PR-079)
