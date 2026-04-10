// src/place/dto/place-search.dto.ts
import { ApiPropertyOptional } from '@nestjs/swagger';
import { IsEnum, IsNumber, IsOptional, IsString, Max, Min } from 'class-validator';
import { Type } from 'class-transformer';

export enum PlaceCategory {
  RESTAURANT = 'restaurant',
  CAFE = 'cafe',
  CULTURE = 'culture',
  PARK = 'park',
  SHOPPING = 'shopping',
  ATTRACTION = 'attraction',
}

export class PlaceSearchDto {
  @ApiPropertyOptional({ description: '검색 키워드' })
  @IsOptional()
  @IsString()
  keyword?: string;

  @ApiPropertyOptional({ enum: PlaceCategory, description: '카테고리 필터' })
  @IsOptional()
  @IsEnum(PlaceCategory)
  category?: PlaceCategory;

  @ApiPropertyOptional({ description: '위도' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  lat?: number;

  @ApiPropertyOptional({ description: '경도' })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  lng?: number;

  @ApiPropertyOptional({ description: '검색 반경 (미터)', default: 2000 })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(100)
  @Max(20000)
  radius?: number = 2000;

  @ApiPropertyOptional({ description: '페이지 번호', default: 1 })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(1)
  page?: number = 1;

  @ApiPropertyOptional({ description: '페이지 크기', default: 20 })
  @IsOptional()
  @Type(() => Number)
  @IsNumber()
  @Min(1)
  @Max(100)
  size?: number = 20;
}
