// src/place/place.service.ts
import { Injectable, Logger, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { PlaceSearchDto } from './dto/place-search.dto';
import { SearchService } from '../search/elasticsearch.service';
import { ImageService } from '../image/image.service';
import { PlaceKafkaProducer } from '../kafka/place-kafka.producer';
import { CongestionMessage, PlaceUpdatedMessage, EventUpdatedMessage } from '../kafka/place-kafka.consumer';

// TypeORM Entity (별도 파일로 분리 권장)
// import { PlaceEntity } from './entities/place.entity';
// import { PlaceDetailDocument } from './schemas/place-detail.schema';

@Injectable()
export class PlaceService {
  private readonly logger = new Logger(PlaceService.name);

  constructor(
    // @InjectRepository(PlaceEntity) private readonly placeRepo: Repository<PlaceEntity>,
    // @InjectModel('PlaceDetail') private readonly placeDetailModel: Model<PlaceDetailDocument>,
    private readonly searchService: SearchService,
    private readonly imageService: ImageService,
    private readonly kafkaProducer: PlaceKafkaProducer,
  ) {}

  /**
   * Elasticsearch 기반 장소 검색
   */
  async search(dto: PlaceSearchDto) {
    return this.searchService.searchPlaces(dto);
  }

  /**
   * 장소 상세 조회 (MySQL + MongoDB 조인)
   */
  async findOneWithDetail(id: number) {
    // TODO: TypeORM + Mongoose 조합 조회
    this.logger.debug(`장소 상세 조회: id=${id}`);
    return { id, message: 'TODO: implement' };
  }

  /**
   * 장소 메타 조회 (recommendation-service Feign Client 호출용)
   */
  async findMeta(id: number) {
    this.logger.debug(`장소 메타 조회: id=${id}`);
    return { placeId: id, placeName: 'TODO', thumbnailImageKey: null };
  }

  /**
   * MinIO Presigned URL 발급
   */
  async generatePresignedUrl(placeId: number, fileName: string, contentType: string) {
    const objectKey = `places/${placeId}/${Date.now()}_${fileName}`;
    const url = await this.imageService.generatePresignedUrl(
      process.env.MINIO_BUCKET_PLACES || 'places',
      objectKey,
      contentType,
    );
    return { presignedUrl: url, objectKey };
  }

  /**
   * 실시간 혼잡도 MongoDB 갱신
   */
  async updateRealtimeData(message: CongestionMessage): Promise<void> {
    this.logger.debug(`혼잡도 갱신: areaCode=${message.areaCode}`);
    // TODO: Mongoose placeDetailModel.updateMany 구현
  }

  /**
   * 서울 공공 장소 데이터 동기화 (MySQL + ES + 임베딩 요청)
   */
  async syncPlaceFromSeoul(message: PlaceUpdatedMessage): Promise<void> {
    this.logger.debug(`장소 동기화: source=${message.source}, action=${message.action}`);
    // TODO: TypeORM upsert + ES index + Kafka 임베딩 요청
    if (message.action === 'upsert' && message.placeId) {
      await this.kafkaProducer.requestVectorEmbed({
        placeId: message.placeId,
        placeName: String(message.payload['name'] || ''),
        subCategory: String(message.payload['subCategory'] || ''),
        atmosphereTags: Array.isArray(message.payload['atmosphereTags'])
          ? (message.payload['atmosphereTags'] as string[])
          : [],
        shortDescription: String(message.payload['shortDescription'] || ''),
        action: 'upsert',
      });
    }
  }

  /**
   * 문화 행사 데이터 동기화
   */
  async syncEventFromSeoul(message: EventUpdatedMessage): Promise<void> {
    this.logger.debug(`이벤트 동기화: eventId=${message.eventId}`);
    // TODO: MySQL cultural_events upsert + MongoDB 갱신
  }
}
