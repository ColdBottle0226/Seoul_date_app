// src/image/image.service.ts
import { Injectable, Logger } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import * as Minio from 'minio';
import * as sharp from 'sharp';

interface ResizeTarget {
  suffix: string;
  width: number;
}

const RESIZE_TARGETS: ResizeTarget[] = [
  { suffix: '', width: 800 },       // 원본 크기
  { suffix: '_m', width: 400 },     // 미디움
  { suffix: '_s', width: 200 },     // 썸네일
];

@Injectable()
export class ImageService {
  private readonly logger = new Logger(ImageService.name);
  private readonly minioClient: Minio.Client;

  constructor(private readonly config: ConfigService) {
    this.minioClient = new Minio.Client({
      endPoint: this.config.get<string>('MINIO_ENDPOINT', 'minio'),
      port: this.config.get<number>('MINIO_PORT', 9000),
      useSSL: this.config.get<string>('MINIO_USE_SSL', 'false') === 'true',
      accessKey: this.config.get<string>('MINIO_ACCESS_KEY', ''),
      secretKey: this.config.get<string>('MINIO_SECRET_KEY', ''),
    });
  }

  /**
   * Presigned PUT URL 발급 (클라이언트 직접 업로드용)
   */
  async generatePresignedUrl(bucket: string, objectKey: string, _contentType: string): Promise<string> {
    // MinIO presigned URL (10분 유효)
    const url = await this.minioClient.presignedPutObject(bucket, objectKey, 600);
    this.logger.debug(`Presigned URL 발급: bucket=${bucket}, key=${objectKey}`);
    return url;
  }

  /**
   * 이미지 리사이즈 + WebP 변환 후 MinIO 업로드
   * Kafka place.image.upload 이벤트 수신 시 호출
   */
  async processAndUploadImage(
    sourceBucket: string,
    sourceKey: string,
    targetBucket: string,
    baseKey: string,
  ): Promise<string[]> {
    const uploadedKeys: string[] = [];

    // 원본 파일 다운로드
    const stream = await this.minioClient.getObject(sourceBucket, sourceKey);
    const chunks: Buffer[] = [];
    for await (const chunk of stream) {
      chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
    }
    const originalBuffer = Buffer.concat(chunks);

    // 각 크기별 WebP 변환 후 업로드
    for (const target of RESIZE_TARGETS) {
      const webpBuffer = await sharp(originalBuffer)
        .resize(target.width, null, { withoutEnlargement: true })
        .webp({ quality: 85 })
        .toBuffer();

      // 확장자를 .webp로 변경
      const baseWithoutExt = baseKey.replace(/\.[^/.]+$/, '');
      const objectKey = `${baseWithoutExt}${target.suffix}.webp`;

      await this.minioClient.putObject(targetBucket, objectKey, webpBuffer, webpBuffer.length, {
        'Content-Type': 'image/webp',
      });

      uploadedKeys.push(objectKey);
      this.logger.debug(`이미지 업로드 완료: ${objectKey} (${target.width}px)`);
    }

    return uploadedKeys;
  }
}
