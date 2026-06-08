# 商家端注册流程重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 将商家注册拆分为两步（账号注册 → 店铺注册），每个步骤发独立请求；登录后检查店铺状态，无店铺则弹窗引导注册。

**Architecture:** 
- 后端：ShopMerchantController 新增 POST /api/seller/shop/register，复用 ShopServiceImpl.createShop()
- 前端：Register 页使用 el-steps 分两步表单；Login 页登录后调用 getMyShop 决定跳转或弹窗

**Tech Stack:** Spring Boot + Vue 3 + Element Plus + Pinia

---

### Task 1: 后端 - ShopMerchantController 新增 register 接口

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\controller\ShopMerchantController.java`

在 ShopMerchantController 中新增 POST /api/seller/shop/register 端点，复用 ShopServiceImpl.createShop()，接收 X-User-Id 从 Header 获取商家 ID，接收 CreateShopRequest 请求体（name 必填、description 可选）

### Task 2: 后端 - ShopServiceImpl 增加防重复创建校验

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-backend_Eureka\shop-service\src\main\java\com\gzasc\aishopping\shop\service\impl\ShopServiceImpl.java`

在 createShop() 方法开头调用 selectShopByMerchantId(userId) 检查是否已有店铺，有则抛出 ShopException("您已拥有店铺")

### Task 3: 前端 - shop.js 新增 createShop 方法

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\api\shop.js`

新增 createShop(data) 方法，调用 POST /api/seller/shop/register

### Task 4: 前端 - Register 页重写为两步表单

**Files:**
- Create/Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Register\Register.vue`
- Create/Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Register\Register.js`
- Create/Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Register\Register.css`
- Create/Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Register\Text.js`

使用 el-steps 分两步：Step1 账号信息 → 调用 merchantRegister → 自动登录 → Step2 店铺信息 → 调用 createShop → 跳转管理页

### Task 5: 前端 - Login 页增加店铺检查弹窗

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Login\Login.js`
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\Login\Text.js`

登录成功后调用 getMyShop()，有店铺则跳转管理页，无店铺则弹出 ElMessageBox.confirm 引导前往注册

### Task 6: 前端 - 路由调整 + 删除 ShopRegister

**Files:**
- Modify: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\router\index.js`
- Delete: `F:\IdeaProjects\AI-Shopping\AI-Shopping-frontier\frontier-seller\src\views\ShopRegister\` 整个文件夹

删除 /shop/register 路由配置及其导入；删除 ShopRegister 文件夹所有文件
