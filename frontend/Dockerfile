# Etape 1 : build de l'appli Vue (Vite)
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# Etape 2 : nginx sert le build et les videos (dossier a monter en volume)
FROM nginx:stable-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
# Point de montage du dossier des videos, ex :
#   docker run -v /var/lib/motion/jours:/usr/share/nginx/videos:ro ...
RUN mkdir -p /usr/share/nginx/videos
EXPOSE 80
