# 订单退货服务边界重构设计

## 问题
`ReturnRequestService` 直接操作 `OrderMapper` 更新订单状态，违反了「每个 Service 只操作自己的 Mapper」原则。订单状态变更应当统一走 `OrderService`。

## 依赖关系（目标）
OrderService → (仅读) OrderMapper
ReturnRequestService → (仅读) ReturnRequestMapper, OrderService

**禁止：** ReturnRequestService → OrderMapper

## 接口变更

### OrderService
| 方法 | 变化 |
|------|------|
| requestReturn(userId, orderId) | **删除** — 已无调用方，controller 直接调 ReturnRequestService |
| approveReturn(shopId, orderId) | **删除** — 被 /return-requests/{orderId}/review 替代 |
| agreeReturnRequest(shopId, orderId) | **新增** — CAS `SHIPPED/DELIVERED → RETURN_PENDING` |
| submitReturnLogisticsStatus(userId, orderId) | **新增** — CAS `RETURN_PENDING → RETURNING` |

### ReturnRequestService
保留现有接口不变，内部将 OrderMapper 操作替换为 OrderService 调用。

## 边界
- `ReturnRequestServiceImpl.createReturnRequest`：通过 `OrderService.getOrderDetailByUser` 读订单
- `ReturnRequestServiceImpl.reviewReturnRequest`（同意）：调 `OrderService.agreeReturnRequest` 完成订单状态变更
- `ReturnRequestServiceImpl.submitReturnLogistics`：通过 `OrderService.getOrderDetailByUser` 读校验，调 `OrderService.submitReturnLogisticsStatus` 完成 CAS

## 验证
- 编译通过
- 全部 220+ 测试通过
- 依赖方向为单向：ReturnRequestService → OrderService
