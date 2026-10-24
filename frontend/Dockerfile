# ============================================================================
# Dockerfile para Frontend React + Vite
# ============================================================================

FROM node:20-alpine

WORKDIR /app

# Copiar archivos de configuración
COPY package*.json ./

# Instalar dependencias
RUN npm ci

# Copiar código fuente
COPY . .

# Exponer puerto de desarrollo de Vite
EXPOSE 5173

# Configuración para permitir acceso externo
ENV VITE_API_URL=http://localhost:8080

# Comando de inicio en modo desarrollo
CMD ["npm", "run", "dev", "--", "--host", "0.0.0.0"]
