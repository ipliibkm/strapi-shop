FROM node:24-bookworm-slim AS build
WORKDIR /workspace
RUN npm install --global npm@10.9.4
COPY package*.json angular.json tsconfig.json .postcssrc.json ./
COPY apps/admin ./apps/admin
COPY packages/ui ./packages/ui
COPY apps/storefront/package.json ./apps/storefront/package.json
COPY apps/setup/package.json ./apps/setup/package.json
RUN npm ci --no-audit --no-fund
RUN npm run build:admin

FROM nginx:1.27-alpine
COPY apps/admin/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/admin/browser/ /usr/share/nginx/html/
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
