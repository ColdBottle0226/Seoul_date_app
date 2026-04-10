// src/app.module.ts
import { Module } from '@nestjs/common';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { TypeOrmModule } from '@nestjs/typeorm';
import { MongooseModule } from '@nestjs/mongoose';
import { TerminusModule } from '@nestjs/terminus';
import { HttpModule } from '@nestjs/axios';

import { EurekaModule } from './eureka/eureka.module';
import { KafkaModule } from './kafka/kafka.module';
import { HealthController } from './health/health.controller';
import { PlaceController } from './place/place.controller';
import { PlaceService } from './place/place.service';
import { SearchService } from './search/elasticsearch.service';
import { ImageService } from './image/image.service';

@Module({
  imports: [
    // 환경변수 전역 설정
    ConfigModule.forRoot({ isGlobal: true }),

    // MySQL (TypeORM)
    TypeOrmModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        type: 'mysql',
        host: config.get<string>('DB_HOST', 'mysql-place'),
        port: config.get<number>('DB_PORT', 3306),
        database: config.get<string>('DB_DATABASE', 'place_db'),
        username: config.get<string>('DB_USERNAME'),
        password: config.get<string>('DB_PASSWORD'),
        entities: [__dirname + '/**/*.entity{.ts,.js}'],
        synchronize: config.get<string>('DB_SYNCHRONIZE', 'false') === 'true',
        logging: config.get<string>('DB_LOGGING', 'false') === 'true',
        timezone: '+09:00',
        charset: 'utf8mb4',
      }),
      inject: [ConfigService],
    }),

    // MongoDB (Mongoose)
    MongooseModule.forRootAsync({
      imports: [ConfigModule],
      useFactory: (config: ConfigService) => ({
        uri: config.get<string>('MONGODB_URI'),
      }),
      inject: [ConfigService],
    }),

    // Health Check (Eureka 헬스체크)
    TerminusModule,

    // HTTP Client
    HttpModule,

    // Eureka 등록
    EurekaModule,

    // Kafka Consumer / Producer
    KafkaModule,
  ],
  controllers: [HealthController, PlaceController],
  providers: [PlaceService, SearchService, ImageService],
})
export class AppModule {}
