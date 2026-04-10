// src/place/place.controller.ts
import {
  Controller,
  Get,
  Post,
  Param,
  Body,
  Query,
  UseGuards,
  ParseIntPipe,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiBearerAuth,
  ApiParam,
  ApiResponse,
} from '@nestjs/swagger';
import { PlaceService } from './place.service';
import { PlaceSearchDto } from './dto/place-search.dto';

// Gateway가 X-User-Id 헤더를 주입하므로, Guard는 헤더 존재 여부만 검증
// @UseGuards(JwtHeaderGuard)

@ApiTags('places')
@Controller('api/places')
export class PlaceController {
  constructor(private readonly placeService: PlaceService) {}

  /**
   * 장소 검색 (Elasticsearch 기반)
   * GET /api/places/search?keyword=홍대&category=cafe&lat=37.55&lng=126.92&radius=2000
   */
  @Get('search')
  @ApiOperation({ summary: '장소 검색 (ES 키워드·카테고리·거리 필터)' })
  @ApiResponse({ status: 200, description: '검색 결과 목록' })
  async search(@Query() dto: PlaceSearchDto) {
    return this.placeService.search(dto);
  }

  /**
   * 장소 단건 조회
   * GET /api/places/:id
   */
  @Get(':id')
  @ApiOperation({ summary: '장소 상세 조회' })
  @ApiParam({ name: 'id', type: Number })
  async findOne(@Param('id', ParseIntPipe) id: number) {
    return this.placeService.findOneWithDetail(id);
  }

  /**
   * 장소 메타 조회 (recommendation-service Feign Client용 내부 엔드포인트)
   * GET /api/places/:id/meta
   */
  @Get(':id/meta')
  @ApiOperation({ summary: '장소 메타 조회 (서비스 간 내부 호출용)' })
  async getMeta(@Param('id', ParseIntPipe) id: number) {
    return this.placeService.findMeta(id);
  }

  /**
   * MinIO Presigned URL 발급
   * POST /api/places/:id/images/presigned-url
   */
  @Post(':id/images/presigned-url')
  @ApiBearerAuth()
  @ApiOperation({ summary: '이미지 업로드용 Presigned URL 발급' })
  async getPresignedUrl(
    @Param('id', ParseIntPipe) placeId: number,
    @Body() body: { fileName: string; contentType: string },
  ) {
    return this.placeService.generatePresignedUrl(placeId, body.fileName, body.contentType);
  }
}
