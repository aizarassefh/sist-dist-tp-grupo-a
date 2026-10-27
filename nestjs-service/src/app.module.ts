import { Module } from '@nestjs/common';
import { MotorController } from './motor/motor.controller';

@Module({
  controllers: [MotorController],
})
export class AppModule {}
