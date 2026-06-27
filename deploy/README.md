# AI-Shopping Docker 部署

## 前置条件

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 4.0+
- 至少 8GB 可用内存

## 快速启动

```bash
# 1. 进入部署目录
cd deploy

# 2. 构建并启动所有服务
docker compose up

# 3. 等待所有服务就绪（首次约 3-5 分钟）
#    看到 "ai-nginx" 日志输出启动信息即可
```

## 访问地址

| 地址 | 说明 |
|------|------|
| `http://localhost/` | 用户端首页（自动跳转 /user/） |
| `http://localhost/user/` | 用户端（商品浏览、下单） |
| `http://localhost/seller/` | 商家端（商品管理、订单管理） |
| `http://localhost:8848/nacos` | Nacos 控制台（账号: nacos / nacos） |

## 组件端口映射

| 组件 | 容器内端口 | 宿主机端口 | 说明 |
|------|-----------|-----------|------|
| nginx | 80 | 80 | 前端入口 |
| mysql | 3306 | 3306 | 数据库 |
| redis | 6379 | 6379 | 缓存 |
| mongodb | 27017 | 27017 | chat 记忆 |
| nacos | 8848 | 8848 | 服务发现 |
| gateway | 8088 | 8088 | API 网关 |
| auth | 8086 | 8086 | 认证 |
| product | 8081 | 8081 | 商品 |
| order | 8082 | 8082 | 订单 |
| contact | 8083 | 8083 | 联系人 |
| logistics | 8084 | 8084 | 物流 |
| chat | 8085 | 8085 | 聊天 |
| shop | 8087 | 8087 | 店铺 |

## 管理命令

```bash
# 后台启动
docker compose up -d

# 查看日志
docker compose logs -f <service-name>

# 重启某个服务
docker compose restart <service-name>

# 停止所有服务
docker compose down

# 停止并删除数据卷（⚠️ 会丢失数据库数据）
docker compose down -v
```

## Nacos 配置修改

系统启动后，可在 Nacos 控制台修改 `app.*` 配置项：

1. 访问 `http://localhost:8848/nacos`，登录（nacos/nacos）
2. 进入「配置管理」→「配置列表」
3. 选择 `DEFAULT_GROUP`，找到对应服务的 Data ID
4. 修改后点击「发布」

### 配置项清单

| Key | 所在 Data ID | 默认值 | 说明 |
|-----|-------------|--------|------|
| `app.image.base-url` | product-service.yml | `http://product-service:8081` | 商品图片访问地址 |
| `app.image.storage-path` | product-service.yml | `/app/static/image/goods/main` | 商品图片存储路径 |
| `app.image.resource-location` | product-service.yml | `file:/app/static/image/` | 静态资源映射 |
| `app.image.max-file-size` | product-service.yml | `5MB` | 上传大小限制 |
| `app.scheduler.reservation.*` | product-service.yml | `true` / `120000` | 库存清理任务 |
| `app.image.base-url` | shop-service.yml | `http://shop-service:8087` | 店铺图片访问地址 |
| `app.image.storage-path` | shop-service.yml | `/app/static/image/shop/logo` | 店铺图片存储路径 |
| `app.image.resource-location` | shop-service.yml | `file:/app/static/image/` | 静态资源映射 |

## 常见问题

**Q: 启动时端口被占用？**
修改 `.env` 中的端口映射，或停止占用端口的进程。

**Q: 图片上传失败？**
检查 `image-data` volume 是否正确挂载，容器内路径是否为 `/app/static/image/`。

**Q: 服务启动缓慢？**
首次启动需要拉取镜像和初始化数据库，之后重启会快很多。
