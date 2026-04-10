// src/search/elasticsearch.service.ts
import { Injectable, Logger } from '@nestjs/common';
import { Client } from '@elastic/elasticsearch';
import { ConfigService } from '@nestjs/config';
import { PlaceSearchDto } from '../place/dto/place-search.dto';

@Injectable()
export class SearchService {
  private readonly logger = new Logger(SearchService.name);
  private readonly client: Client;
  private readonly INDEX = 'places';

  constructor(private readonly config: ConfigService) {
    this.client = new Client({
      node: this.config.get<string>('ELASTICSEARCH_NODE', 'http://elasticsearch:9200'),
      auth: {
        username: this.config.get<string>('ELASTICSEARCH_USERNAME', 'elastic'),
        password: this.config.get<string>('ELASTICSEARCH_PASSWORD', ''),
      },
    });
  }

  /**
   * 장소 검색 (Full-text + 지리 검색)
   */
  async searchPlaces(dto: PlaceSearchDto) {
    const must: unknown[] = [];
    const filter: unknown[] = [];

    // 키워드 검색
    if (dto.keyword) {
      must.push({
        multi_match: {
          query: dto.keyword,
          fields: ['place_name^3', 'sub_category^2', 'atmosphere_tags', 'short_description'],
          type: 'best_fields',
          fuzziness: 'AUTO',
        },
      });
    }

    // 카테고리 필터
    if (dto.category) {
      filter.push({ term: { category: dto.category } });
    }

    // 거리 필터
    if (dto.lat && dto.lng) {
      filter.push({
        geo_distance: {
          distance: `${dto.radius || 2000}m`,
          location: { lat: dto.lat, lon: dto.lng },
        },
      });
    }

    const from = ((dto.page || 1) - 1) * (dto.size || 20);
    const size = dto.size || 20;

    const response = await this.client.search({
      index: this.INDEX,
      body: {
        from,
        size,
        query: {
          bool: {
            must: must.length > 0 ? must : [{ match_all: {} }],
            filter,
          },
        },
        sort: [
          { _score: 'desc' },
          ...(dto.lat && dto.lng
            ? [{ _geo_distance: { location: { lat: dto.lat, lon: dto.lng }, order: 'asc', unit: 'm' } }]
            : []),
        ],
      },
    });

    return {
      total: (response.hits.total as { value: number }).value,
      page: dto.page,
      size: dto.size,
      items: response.hits.hits.map((hit) => ({
        id: hit._id,
        score: hit._score,
        ...(hit._source as object),
      })),
    };
  }

  /**
   * 장소 ES 인덱스 upsert
   */
  async indexPlace(placeId: string, doc: Record<string, unknown>): Promise<void> {
    await this.client.index({
      index: this.INDEX,
      id: placeId,
      document: doc,
    });
    this.logger.debug(`ES 인덱싱 완료: placeId=${placeId}`);
  }

  /**
   * 장소 ES 인덱스 삭제
   */
  async deletePlace(placeId: string): Promise<void> {
    await this.client.delete({ index: this.INDEX, id: placeId });
    this.logger.debug(`ES 삭제 완료: placeId=${placeId}`);
  }
}
