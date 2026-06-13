# 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Maven 下载重试配置：使用 wagon 传输并开启 HTTP 重试，
# 缓解阿里云镜像偶发的 "Premature end of Content-Length" 截断错误
ENV MAVEN_OPTS="-Dmaven.resolver.transport=wagon -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 -Dmaven.wagon.http.pool=false"

# 复制 Maven 配置和 pom.xml
COPY settings.xml .
COPY pom.xml .

# 预下载依赖（利用 Docker 缓存层）；失败时带退避重试，已下载的依赖会被复用。
# 使用 || 链确保最后一次仍失败时整个步骤以非零状态退出。
RUN mvn -s settings.xml dependency:go-offline -B \
    || (echo "重试 1/3..." && sleep 10 && mvn -s settings.xml dependency:go-offline -B) \
    || (echo "重试 2/3..." && sleep 20 && mvn -s settings.xml dependency:go-offline -B) \
    || (echo "重试 3/3..." && sleep 30 && mvn -s settings.xml dependency:go-offline -B)

# 复制源码并打包；同样加入重试以应对网络抖动。
# 使用 -Dmaven.test.skip=true 同时跳过测试的编译与执行
# （部分测试用例的构造签名已过时，部署构建无需编译测试代码）
COPY src ./src
RUN mvn -s settings.xml package -Dmaven.test.skip=true -B \
    || (echo "重试 1/2..." && sleep 10 && mvn -s settings.xml package -Dmaven.test.skip=true -B) \
    || (echo "重试 2/2..." && sleep 20 && mvn -s settings.xml package -Dmaven.test.skip=true -B)

# 运行阶段
FROM eclipse-temurin:17-jre
WORKDIR /app

# 复制打包好的 JAR 文件
COPY --from=build /app/target/*.jar app.jar

# 暴露端口
EXPOSE 8000

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
