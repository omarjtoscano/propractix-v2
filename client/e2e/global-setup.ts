export default function globalSetup() {
  if (!process.env.PROPRACTIX_E2E_BASE_URL
      || !/^propractix-e8-\d+$/.test(process.env.PROPRACTIX_E2E_PROJECT ?? '')) {
    throw new Error('Run npm run client:e2e to execute tests with the isolated orchestrator environment');
  }
}
