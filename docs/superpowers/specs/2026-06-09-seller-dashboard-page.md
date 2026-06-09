# 卖家端概览页设计

## 目标
卖家登录后默认重定向到概览页，展示欢迎语，后续可完善为数据面板。

## 方案
- 新建 `views/Dashboard/`（Dashboard.vue + Dashboard.js + Dashboard.css + Text.js）
- 路由：`/shop/:shopId` 映射到 Dashboard，放在 layout children 第一位作为默认子路由
- 重定向：路由守卫 `home` 和注册完成后都指向 `/shop/{shopId}`（原为 `/shop/{shopId}/products`）
- 边栏：第一个菜单项"概览"，指向 `/shop/{shopId}`

## 页面内容（第一期）
居中卡片：`<h2>欢迎回来，{{ shopName }}</h2>` + 一个简短副标题

## 影响范围
- 新增：`views/Dashboard/` 目录（4 个文件）
- 修改：`router/index.js`（添加路由、改重定向）
- 修改：`layout/AppSidebar.vue`（添加概览链接）
- 修改：`views/Register/Register.js`（改注册后重定向目的地）
