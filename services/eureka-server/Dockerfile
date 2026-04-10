FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 결과물 복사 (Gradle bootJar)
COPY build/libs/*.jar app.jar

# 타임존 설정
ENV TZ=Asia/Seoul

# JVM 보안 난수 설정 (기동 시간 단축)
# file:/dev/./urandom: /dev/random 블로킹 방지
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Djava.net.preferIPv4Stack=true"

EXPOSE 8761

# Graceful shutdown을 위해 exec 형식 ENTRYPOINT 사용
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
