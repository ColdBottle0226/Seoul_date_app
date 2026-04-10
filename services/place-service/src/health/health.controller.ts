// src/health/health.controller.ts
import { Controller, Get } from '@nestjs/common';
import {
  HealthCheck,
  HealthCheckService,
  TypeOrmHealthIndicator,
  MongooseHealthIndicator,
} from '@nestjs/terminus';
import { ApiTags, ApiOperation } from '@nestjs/swagger';

@ApiTags('health')
@Controller('health')
export class HealthController {
  constructor(
    private readonly health: HealthCheckService,
    private readonly db: TypeOrmHealthIndicator,  // TypeORM (MySQL)
    private readonly mongo: MongooseHealthIndicator,
  ) {}

  @Get()
  @HealthCheck()
  @ApiOperation({ summary: '헬스체크 (Eureka 등록용)' })
  check() {
    return this.health.check([
      () => this.db.pingCheck('mysql'),
      () => this.mongo.pingCheck('mongodb'),
    ]);
  }
}
