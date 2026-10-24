# ============================================================================
# Dockerfile para Microservicio NestJS con gRPC
# ============================================================================

# Etapa 1: Construcción
FROM node:20-alpine AS builder

WORKDIR /app

# Copiar archivos de configuración
COPY package*.json ./
COPY tsconfig.json ./

# Instalar dependencias
RUN npm ci

# Copiar código fuente
COPY src/ src/
COPY proto/ proto/

# Compilar TypeScript
RUN npm run build

# ===========================================================================
# Etapa 2: Imagen de producción
FROM node:20-alpine

WORKDIR /app

# Instalar curl para healthcheck
RUN apk add --no-cache curl

# Copiar dependencias y código compilado
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/package*.json ./
COPY proto/ proto/

# Usuario no-root
RUN addgroup -g 1001 -S nodejs && \
    adduser -S nestjs -u 1001

RUN chown -R nestjs:nodejs /app
USER nestjs

# Puerto gRPC
EXPOSE 9091

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:9091 || exit 0

# Usar npm start en lugar de node directo (leerá desde dist/main.js)
CMD ["node", "dist/main.js"]
