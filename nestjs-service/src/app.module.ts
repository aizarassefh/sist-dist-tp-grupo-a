import { Module } from '@nestjs/common';
import { RecomendacionesController } from './controllers/recomendaciones.controller';
import { UsuariosController } from './controllers/usuarios.controller';
import { RecomendacionesService } from './services/recomendaciones.service';
import { UsuariosService } from './services/usuarios.service';

@Module({
  imports: [],
  controllers: [
    RecomendacionesController,
    UsuariosController,
  ],
  providers: [
    RecomendacionesService,
    UsuariosService,
  ],
})
export class AppModule {}
