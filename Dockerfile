# syntax=docker/dockerfile:1
#
# 两阶段构建：构建阶段用 Maven 镜像打好 fat jar，运行阶段只保留 JRE + jar。
# 运行镜像以非 root 用户启动，并挂载 /app/data 作为 H2 兜底存储（用 MySQL 时该目录不被使用）。
#
# 构建：docker build -t cxh-blog:1.0.0 .
# 运行（连外部 MySQL）：见 docker-compose.yml

# ---------------- 构建阶段 ----------------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 先把 pom 单独拷入并预热依赖：源码改动不会让依赖层缓存失效
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# ---------------- 运行阶段 ----------------
FROM eclipse-temurin:17-jre-jammy

LABEL org.opencontainers.image.title="cxh-blog" \
      org.opencontainers.image.description="cxh 个人博客：站点风格与简介可自由编辑"

# curl 仅用于 HEALTHCHECK，同时方便进入容器排查问题
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd --system cxh \
 && useradd --system --gid cxh --home-dir /app --create-home cxh

WORKDIR /app
COPY --from=builder /build/target/cxh-blog.jar /app/app.jar

# H2 兜底库的落盘位置（用 MySQL 时不会被写入），预先建好并交给非 root 用户
RUN mkdir -p /app/data && chown -R cxh:cxh /app

USER cxh
EXPOSE 8080

# 容器内存自适应（MaxRAMPercentage 在 JDK 10+ 生效），时区可被 compose 覆盖
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Shanghai" \
    SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
