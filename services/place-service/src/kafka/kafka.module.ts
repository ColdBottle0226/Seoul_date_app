// src/kafka/kafka.module.ts
import { Module } from '@nestjs/common';
import { ClientsModule, Transport } from '@nestjs/microservices';
import { ConfigModule, ConfigService } from '@nestjs/config';
import { PlaceKafkaProducer } from './place-kafka.producer';
import { PlaceKafkaConsumer } from './place-kafka.consumer';

@Module({
  imports: [
    ClientsModule.registerAsync([
      {
        name: 'KAFKA_SERVICE',
        imports: [ConfigModule],
        useFactory: (config: ConfigService) => ({
          transport: Transport.KAFKA,
          options: {
            client: {
              clientId: config.get<string>('KAFKA_CLIENT_ID', 'place-service'),
              brokers: config.get<string>('KAFKA_BROKERS', 'kafka:9092').split(','),
            },
            consumer: {
              groupId: config.get<string>('KAFKA_GROUP_ID', 'place-service-group'),
            },
          },
        }),
        inject: [ConfigService],
      },
    ]),
  ],
  providers: [PlaceKafkaProducer, PlaceKafkaConsumer],
  exports: [PlaceKafkaProducer],
})
export class KafkaModule {}
