FROM node:20-alpine
WORKDIR /app
RUN corepack enable
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY apps/api/package.json apps/api/package.json
COPY packages/core/package.json packages/core/package.json
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build
WORKDIR /app/apps/api
EXPOSE 4000
CMD ["node", "dist/index.js"]