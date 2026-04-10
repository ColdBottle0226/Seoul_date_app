// src/main.ts
// NOTE: OpenTelemetry 트레이싱은 tracing.ts가 --require로 먼저 로드됨
import { NestFactory } from '@nestjs/core';
import { MicroserviceOptions, Transport } from '@nestjs/microservices';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const logger = new Logger('Bootstrap');
  const port = process.env.PORT || 8082;

  // HTTP 서버 (REST API)
  const app = await NestFactory.create(AppModule, {
    logger: ['log', 'error', 'warn', 'debug'],
  });

  // Global Validation Pipe
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      forbidNonWhitelisted: false,
    }),
  );

  // Swagger (개발/스테이징 환경)
  if (process.env.NODE_ENV !== 'production') {
    const config = new DocumentBuilder()
      .setTitle('Place Service API')
      .setDescription('Seoul Date App — Place Service (NestJS 10)')
      .setVersion('1.0')
      .addBearerAuth()
      .build();
    const document = SwaggerModule.createDocument(app, config);
    SwaggerModule.setup('api/docs', app, document);
  }

  await app.listen(port);
  logger.log(`Place Service 시작: http://localhost:${port}`);

  // Kafka Microservice (Consumer) 연결
  const kafkaApp = await NestFactory.createMicroservice<MicroserviceOptions>(AppModule, {
    transport: Transport.KAFKA,
    options: {
      client: {
        clientId: process.env.KAFKA_CLIENT_ID || 'place-service',
        brokers: (process.env.KAFKA_BROKERS || 'kafka:9092').split(','),
      },
      consumer: {
        groupId: process.env.KAFKA_GROUP_ID || 'place-service-group',
      },
    },
  });

  await kafkaApp.listen();
  logger.log('Kafka Microservice 연결 완료');
}

bootstrap().catch((err) => {
  console.error('place-service 기동 실패:', err);
  process.exit(1);
});
