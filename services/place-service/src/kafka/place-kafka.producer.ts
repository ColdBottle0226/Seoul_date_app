// src/kafka/place-kafka.producer.ts
import { Injectable, Logger } from '@nestjs/common';
import { ClientKafka } from '@nestjs/microservices';
import { Inject } from '@nestjs/common';

export interface VectorEmbedRequest {
  placeId: string;
  placeName: string;
  subCategory: string;
  atmosphereTags: string[];
  shortDescription: string;
  action: 'upsert' | 'delete';
}

@Injectable()
export class PlaceKafkaProducer {
  private readonly logger = new Logger(PlaceKafkaProducer.name);

  constructor(
    @Inject('KAFKA_SERVICE') private readonly kafkaClient: ClientKafka,
  ) {}

  /**
   * ai-service에 벡터 임베딩 요청 발행
   */
  async requestVectorEmbed(payload: VectorEmbedRequest): Promise<void> {
    this.logger.debug(`벡터 임베딩 요청 발행: placeId=${payload.placeId}`);
    this.kafkaClient.emit('vector.embed.requested', {
      key: payload.placeId,
      value: payload,
    });
  }

  /**
   * 이미지 업로드 완료 이벤트 발행 → 이미지 리사이즈 처리 트리거
   */
  async publishImageUploadEvent(placeId: string, objectKey: string): Promise<void> {
    this.logger.debug(`이미지 업로드 이벤트 발행: placeId=${placeId}, key=${objectKey}`);
    this.kafkaClient.emit('place.image.upload', {
      key: placeId,
      value: { placeId, objectKey, uploadedAt: new Date().toISOString() },
    });
  }
}
