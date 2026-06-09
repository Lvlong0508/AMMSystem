# 订单展示重构设计

## 背景

现有订单服务的列表 DTO 信息过少：用户端只能看到订单 ID、商品 ID、店铺 ID、数量、状态、总价；商家端只能看到订单 ID、商品 ID、联系人 ID、数量、状态。前端需要再理解 ID 或自行拼装信息，不利于用户快速定位订单，也不利于商家处理发货。

本次重构目标是让订单服务直接返回面向页面展示的订单视图 DTO，降低前端拼装复杂度，同时保留订单核心表结构不变。

## 现状

订单模型 `Order` 已有字段：

- `orderId`
- `userId`
- `shopId`
- `productId`
- `quantity`
- `totalPrice`
- `orderStatus`
- `orderDate`
- `contactId`

订单服务可通过 Feign 获取：

- 商品信息：`ProductDTO`，包含 `name`、`price`、`tags`、`stock`、`shopId`、`imageUrl`
- 店铺信息：`ShopInfoDTO`，包含 `name`、`logoUrl`
- 联系人信息：`ContactDTO`，包含姓名、电话、地址
- 物流信息：`LogisticsFeignClient#getLatestLogistics`

## 设计目标

1. 用户能在订单列表中快速定位订单。
2. 商家订单管理页展示商家关心的信息，不复用用户订单卡片字段。
3. 商家发货页只展示 `PAID` 待发货订单，并使用专用发货卡片。
4. 前端不直接依赖商品、店铺、联系人服务字段结构，只依赖订单服务返回的展示 DTO。
5. 不修改订单表结构，避免引入订单快照表带来的迁移复杂度。

## 非目标

1. 不支持一个订单包含多个商品项；当前系统仍是一个订单绑定一个商品。
2. 不新增订单快照表。
3. 不在本次重构中优化批量聚合性能到极致，可先保持逐条聚合，后续再批量优化。
4. 不设计完整前端交互，只定义后端 DTO 和接口语义；发货按钮属于前端卡片操作区。

## DTO 设计

### 用户订单卡片 DTO

建议新增 `UserOrderCardDTO`。

字段：

- `orderId`
- `shopLogoUrl`
- `shopName`
- `productImageUrl`
- `productName`
- `quantity`
- `productType`
- `orderStatus`
- `totalPrice`

说明：

- `productType` 是订单展示语义字段，不直接命名为 `tags`。
- 当前可由 `ProductDTO.tags` 填充，后续如果商品服务有更明确的类目字段，只需要调整订单服务聚合逻辑，不影响前端契约。

### 商家订单卡片 DTO

建议新增 `SellerOrderCardDTO`。

字段：

- `orderId`
- `productImageUrl`
- `productName`
- `quantity`
- `orderStatus`
- `totalPrice`
- `contactName`
- `contactPhone`
- `contactAddress`

说明：

- 不展示 `userId`，减少噪音。
- 用于商家订单管理页。
- 该 DTO 关注“这笔订单是什么、多少钱、发给谁”。

### 订单详情 DTO

建议扩展现有 `OrderDetailDTO`，作为用户端和商家端详情弹窗共用的后端 DTO。

字段：

- `orderId`
- `shopId`
- `shopLogoUrl`
- `shopName`
- `productId`
- `productImageUrl`
- `productName`
- `quantity`
- `productType`
- `totalPrice`
- `orderStatus`
- `orderDate`
- `contactId`
- `contactName`
- `contactPhone`
- `contactAddress`
- `trackingNumber`

说明：

- 用户详情弹窗展示：卡片基础信息 + 订单 ID、下单时间、收货地址、发货物流。
- 商家详情弹窗可复用该 DTO，但前端可以选择不展示不需要的信息。
- 不建议将 DTO 字段命名为 `tags`，仍使用 `productType`。

### 发货订单卡片 DTO

建议新增 `ShipmentOrderCardDTO`。

字段：

- `orderId`
- `productImageUrl`
- `productName`
- `quantity`
- `productType`
- `orderStatus`
- `orderDate`
- `contactName`
- `contactPhone`
- `contactAddress`

说明：

- 只用于商家发货页面。
- 查询范围仅限 `PAID` 待发货订单。
- 前端在卡片底部增加操作区，至少包含“发货”按钮。
- 后端不需要返回按钮配置，按钮是页面行为，不属于订单数据。

## 接口设计

### 用户订单列表

现有：

- `GET /api/user/order/list`
- 返回 `List<OrderAbstractUserDTO>`

调整为：

- 返回 `List<UserOrderCardDTO>`

### 用户订单详情

现有：

- `GET /api/user/order/{orderId}`
- 返回 `OrderDetailDTO`

调整为：

- 返回扩展后的 `OrderDetailDTO`

### 商家订单管理列表

现有：

- `GET /api/seller/order/shop/{shopId}/list`
- 返回 `List<OrderAbstractSellerDTO>`

调整为：

- 返回 `List<SellerOrderCardDTO>`

### 商家订单详情

现有：

- `GET /api/seller/order/shop/{shopId}/{orderId}`
- 返回 `OrderDetailDTO`

调整为：

- 返回扩展后的 `OrderDetailDTO`

### 商家发货页列表

建议新增：

- `GET /api/seller/order/shop/{shopId}/shipment-list`
- 返回 `List<ShipmentOrderCardDTO>`
- 只查询 `orderStatus = PAID` 的订单

保留现有发货接口：

- `PUT /api/seller/order/{orderId}/ship?shopId={shopId}`

## 聚合策略

订单服务作为订单展示聚合层：

1. 从订单表查出订单列表。
2. 对每个订单获取商品信息。
3. 对用户订单卡片和详情，再获取店铺信息。
4. 对商家订单卡片、发货卡片和详情，获取联系人信息。
5. 对详情获取最新发货物流。

字段来源：

- 商品图片、商品名、商品类型：`ProductDTO.imageUrl`、`ProductDTO.name`、`ProductDTO.tags`
- 商家 logo、商家名：`ShopInfoDTO.logoUrl`、`ShopInfoDTO.name`
- 收货信息：`ContactDTO.name`、`ContactDTO.phone`、`ContactDTO.address`
- 物流单号：`LogisticsFeignClient#getLatestLogistics(orderId, "DELIVERY")`

## 耦合与复杂度审查

### 使用 `ProductDTO.tags` 填充 `productType`

可接受，但不要把 `tags` 这个字段名继续传给订单前端。

理由：

- `tags` 是商品服务内部偏搜索/标签语义。
- 订单页面需要的是“商品类型”展示语义。
- DTO 字段命名为 `productType` 能隔离前端与商品服务字段设计。
- 后续商品服务新增 `category` 或 `type` 时，只需调整订单服务映射。

### 不做订单快照表

本次不引入快照表。

理由：

- 当前需求主要是展示优化，不是交易审计或历史价格强一致。
- 订单表已有 `totalPrice`，价格展示不会因商品价格变化而变。
- 商品名、图片、店铺名、logo 目前可接受读取实时信息。
- 快照表会增加下单链路复杂度、迁移成本和一致性处理成本。

### 服务耦合

订单服务会依赖商品、店铺、联系人、物流服务，这是现有架构下合理的查询聚合耦合。

控制方式：

- 只在订单服务内聚合，不让前端跨多个服务拼接。
- 对外 DTO 使用订单展示语义字段。
- Feign 获取失败时，列表接口应尽量降级返回订单基础字段，缺失的展示字段置空；详情接口可同样降级，但保留订单核心信息。

## 错误处理

1. 订单不存在或无权限：保持现有异常行为。
2. 商品信息获取失败：列表返回该订单但商品展示字段为空；详情保留订单字段并将商品字段置空。
3. 店铺信息获取失败：店铺展示字段为空。
4. 联系人信息获取失败：收货信息字段为空。
5. 物流信息不存在：`trackingNumber` 为空。
6. 发货页只返回 `PAID` 订单；空列表返回 `[]`。

## 测试策略

### 单元测试

覆盖 `OrderConverter`：

- `Order -> UserOrderCardDTO`
- `Order + ProductDTO + ShopInfoDTO -> UserOrderCardDTO`
- `Order + ProductDTO + ContactDTO -> SellerOrderCardDTO`
- `Order + ProductDTO + ContactDTO -> ShipmentOrderCardDTO`
- `Order + ProductDTO + ShopInfoDTO + ContactDTO + Logistics -> OrderDetailDTO`

### 服务测试

覆盖 `OrderServiceImpl`：

- 用户订单列表返回完整展示字段。
- 商家订单列表返回完整展示字段且不包含 `userId`。
- 发货列表只返回 `PAID` 订单。
- 商品/店铺/联系人/物流 Feign 异常时降级返回。
- 用户和商家详情返回扩展字段。

### 控制器测试

覆盖：

- `GET /api/user/order/list`
- `GET /api/user/order/{orderId}`
- `GET /api/seller/order/shop/{shopId}/list`
- `GET /api/seller/order/shop/{shopId}/{orderId}`
- `GET /api/seller/order/shop/{shopId}/shipment-list`

## 迁移步骤

1. 新增 `UserOrderCardDTO`、`SellerOrderCardDTO`、`ShipmentOrderCardDTO`。
2. 扩展 `OrderDetailDTO`。
3. 扩展 `OrderConverter`，支持新 DTO 映射。
4. 在 `OrderService` 中替换列表方法返回类型，并新增发货列表方法。
5. 在 `OrderServiceImpl` 中实现订单展示聚合。
6. 调整 `OrderUserController` 和 `OrderSellerController` 返回类型。
7. 更新测试。
8. 前端订单列表、商家订单管理、商家发货页按新 DTO 渲染。

## 开放问题

无。当前确认：商家发货页只展示 `PAID` 待发货订单。