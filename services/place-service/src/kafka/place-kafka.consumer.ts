// src/kafka/place-kafka.consumer.ts
import { Controller, Logger } from '@nestjs/common';
import { MessagePattern, Payload } from '@nestjs/microservices';
import { PlaceService } from '../place/place.service';

export interface CongestionMessage {
  areaCode: string;
  areaName: string;
  congestionLevel: string;
  congestionMsg: string;
  forecastList: unknown[];
  collectedAt: string;
}

export interface PlaceUpdatedMessage {
  source: string;          // 'license' | 'tourism' | 'excellence'
  action: 'upsert' | 'delete';
  placeId?: string;
  payload: Record<string, unknown>;
}

export interface EventUpdatedMessage {
  eventId: string;
  action: 'upsert' | 'delete';
  payload: Record<string, unknown>;
}

@Controller()
export class PlaceKafkaConsumer {
  private readonly logger = new Logger(PlaceKafkaConsumer.name);

  constructor(private readonly placeService: PlaceService) {}

  /**
   * 서울 실시간 혼잡도 수신 → MongoDB realtime 필드 갱신
   */
  @MessagePattern('seoul.realtime.congestion')
  async handleCongestion(@Payload() message: CongestionMessage) {
    this.logger.debug(`혼잡도 수신: ${message.areaCode} — ${message.congestionLevel}`);
    await this.placeService.updateRealtimeData(message);
  }

  /**
   * 서울 공공 장소 데이터 갱신 수신 → MySQL + MongoDB + ES 동기화
   */
  @MessagePattern('seoul.place.updated')
  async handlePlaceUpdated(@Payload() message: PlaceUpdatedMessage) {
    this.logger.debug(`장소 업데이트 수신: ${message.source} — ${message.action}`);
    await this.placeService.syncPlaceFromSeoul(message);
  }

  /**
   * 문화행사 갱신 수신
   */
  @MessagePattern('seoul.event.updated')
  async handleEventUpdated(@Payload() message: EventUpdatedMessage) {
    this.logger.debug(`이벤트 업데이트 수신: ${message.eventId}`);
    await this.placeService.syncEventFromSeoul(message);
  }
}
