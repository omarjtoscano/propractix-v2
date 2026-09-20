FROM node:24-alpine@sha256:50c8e8ca1d27439048670df5883f32d57cf81cff6233222c893fd0d9884cbd81 AS build
WORKDIR /workspace
RUN npm install --global npm@11.9.0
COPY package.json package-lock.json .npmrc ./
COPY client/package.json client/package.json
RUN npm ci
COPY client client
RUN npm run build --workspace=client

FROM caddy:2.10.2-alpine@sha256:4c6e91c6ed0e2fa03efd5b44747b625fec79bc9cd06ac5235a779726618e530d
COPY client/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /workspace/client/dist /srv
EXPOSE 80 443 8080
