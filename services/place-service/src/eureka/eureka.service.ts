// src/eureka/eureka.service.ts
import { Injectable, Logger, OnModuleDestroy, OnModuleInit } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Eureka } from 'eureka-js-client';
import * as os from 'os';

@Injectable()
export class EurekaService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(EurekaService.name);
  private client: Eureka;

  constructor(private readonly config: ConfigService) {}

  onModuleInit() {
    const hostname = this.config.get<string>('HOSTNAME') || os.hostname();
    const port = this.config.get<number>('PORT') || 8082;
    const eurekaHost = this.config.get<string>('EUREKA_HOST') || 'eureka-server';
    const eurekaPort = this.config.get<number>('EUREKA_PORT') || 8761;

    this.client = new Eureka({
      instance: {
        app: 'place-service',
        hostName: hostname,
        ipAddr: this.config.get<string>('POD_IP') || hostname,
        port: { $: port, '@enabled': true },
        vipAddress: 'place-service',
        statusPageUrl: `http://${hostname}:${port}/api`,
        healthCheckUrl: `http://${hostname}:${port}/health`,
        dataCenterInfo: {
          '@class': 'com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo',
          name: 'MyOwn',
        },
      },
      eureka: {
        host: eurekaHost,
        port: eurekaPort,
        servicePath: '/eureka/apps/',
        maxRetries: 10,
        requestRetryDelay: 2000,
      },
    });

    this.client.start((error: Error) => {
      if (error) {
        this.logger.error(`Eureka 등록 실패: ${error.message}`);
      } else {
        this.logger.log(`Eureka 등록 완료: place-service → http://${eurekaHost}:${eurekaPort}`);
      }
    });
  }

  onModuleDestroy() {
    if (this.client) {
      this.client.stop();
      this.logger.log('Eureka 등록 해제 완료');
    }
  }
}
