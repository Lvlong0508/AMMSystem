# deploy/build-images.ps1
# 构建所有 Docker 镜像
# 用法: .\build-images.ps1

$JAR_DIR = "..\微服务jar包"
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

# 复制 jar 到构建上下文根目录
foreach ($svc in $SERVICES) {
    Write-Host "Copying $($svc.name).jar..."
    Copy-Item "$JAR_DIR\$($svc.name)-0.0.1-SNAPSHOT.jar" "$($svc.name).jar" -Force
}

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

# 清理临时 jar
foreach ($svc in $SERVICES) {
    Remove-Item "$($svc.name).jar" -Force
}

Write-Host "All images built successfully."
