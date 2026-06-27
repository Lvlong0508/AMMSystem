#!/bin/sh
set -e

NACOS_HOST="${NACOS_SERVER_ADDR:-nacos:8848}"
NACOS_USER="${NACOS_USERNAME:-nacos}"
NACOS_PASS="${NACOS_PASSWORD:-nacos}"

echo "Waiting for Nacos API..."
until curl -s "http://${NACOS_HOST}/nacos/v1/console/health/readiness" > /dev/null 2>&1; do
  sleep 2
done
echo "Nacos is ready."

echo "Logging in to Nacos..."
TOKEN=$(curl -s -X POST "http://${NACOS_HOST}/nacos/v1/auth/login" \
  -d "username=${NACOS_USER}&password=${NACOS_PASS}" | jq -r '.accessToken')
echo "Token acquired."

publish_config() {
  local data_id=$1
  local content=$2
  curl -s -X POST "http://${NACOS_HOST}/nacos/v1/cs/configs" \
    --data-urlencode "accessToken=${TOKEN}" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=yaml" \
    --data-urlencode "content=${content}"
  echo "  -> Published: ${data_id}"
}

echo "Publishing app.* configs..."

publish_config "product-service.yml" "app:
  image:
    base-url: http://product-service:8081
    storage-path: /app/static/image/goods/main
    resource-location: file:/app/static/image/
    max-file-size: 5MB
  scheduler:
    reservation:
      enabled: true
      fixed-rate: 120000
      config-path: /app/config/application.yml"

publish_config "shop-service.yml" "app:
  image:
    base-url: http://shop-service:8087
    storage-path: /app/static/image/shop/logo
    resource-location: file:/app/static/image/"

echo "Nacos init completed."
