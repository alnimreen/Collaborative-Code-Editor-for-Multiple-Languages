FROM node:18-alpine as build

WORKDIR /app

COPY package*.json ./


COPY . /app
RUN npm install \
    && npm run build \
    && npm install -g serve
FROM nginx:alpine
COPY --from=build /app/build /usr/share/nginx/html

COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 3000

CMD ["serve", "-s", "build"]
