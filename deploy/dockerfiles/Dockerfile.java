FROM eclipse-temurin:17-jre-alpine AS builder
WORKDIR /app
ARG SERVICE
COPY ${SERVICE}.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN mkdir -p /app/static/image/goods/main /app/static/image/shop/logo \
    && chown -R 1000:1000 /app/static/image
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
