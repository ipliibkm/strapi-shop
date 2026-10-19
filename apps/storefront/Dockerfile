FROM node:24-bookworm-slim AS build
WORKDIR /workspace
COPY package*.json angular.json tsconfig.json .postcssrc.json ./
COPY apps/storefront ./apps/storefront
COPY packages/ui ./packages/ui
COPY apps/admin/package.json ./apps/admin/package.json
COPY apps/setup/package.json ./apps/setup/package.json
RUN npm install --no-audit --no-fund
RUN npm run build:storefront
FROM nginx:1.27-alpine
COPY apps/storefront/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/storefront/browser/ /usr/share/nginx/html/
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=10 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
