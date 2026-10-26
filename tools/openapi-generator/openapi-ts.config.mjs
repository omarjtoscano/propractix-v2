import { fileURLToPath } from 'node:url';
import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: fileURLToPath(new URL('../../docs/api/openapi.yaml', import.meta.url)),
  output: {
    path: fileURLToPath(new URL('../../client/src/shared/api/generated', import.meta.url)),
    clean: true,
  },
  plugins: ['@hey-api/typescript', '@hey-api/client-fetch', '@hey-api/sdk'],
});
