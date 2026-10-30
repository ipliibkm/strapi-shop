FROM node:22-bookworm-slim AS build
WORKDIR /opt/app
COPY package.json package-lock.json ./
COPY scripts ./scripts
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build
ENV NODE_ENV=production
EXPOSE 1337
CMD ["npm","run","start"]
