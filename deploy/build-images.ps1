# deploy/build-images.ps1
# 构建所有 Docker 镜像
# 用法: .\build-images.ps1

$JAR_DIR = "..\jar"
$SERVICES = @(
    @{name="gateway-service"; port="8088"},
    @{name="auth-service"; port="8086"},
    @{name="product-service"; port="8081"},
    @{name="order-service"; port="8082"},
    @{name="contact-service"; port="8083"},
    @{name="logistics-service"; port="8084"},
    @{name="chat-service"; port="8085"},
    @{name="shop-service"; port="8087"}
)

# Sentinel 使用独立 Dockerfile，单独构建
$SENTINEL_JAR = "sentinel-dashboard-1.8.10.jar"

# 复制 jar 到构建上下文根目录
foreach ($svc in $SERVICES) {
    Write-Host "Copying $($svc.name).jar..."
    Copy-Item "$JAR_DIR\$($svc.name)-0.0.1-SNAPSHOT.jar" "$($svc.name).jar" -Force
}

# 复制 Sentinel jar
Write-Host "Copying sentinel-dashboard.jar..."
Copy-Item "$JAR_DIR\$SENTINEL_JAR" "sentinel-dashboard.jar" -Force

# 构建 nacos-init 镜像
Write-Host "Building nacos-init image..."
docker build -f dockerfiles/Dockerfile.init -t ai-nacos-init .

# 构建各微服务镜像
foreach ($svc in $SERVICES) {
    Write-Host "Building $($svc.name) image..."
    docker build -f dockerfiles/Dockerfile.java `
        --build-arg SERVICE=$($svc.name) `
        -t ai-$($svc.name) .
}

# 构建 Sentinel 镜像
Write-Host "Building sentinel image..."
docker build -f dockerfiles/Dockerfile.sentinel -t ai-sentinel .

# 清理临时 jar
foreach ($svc in $SERVICES) {
    Remove-Item "$($svc.name).jar" -Force
}
Remove-Item "sentinel-dashboard.jar" -Force

Write-Host "All images built successfully."
