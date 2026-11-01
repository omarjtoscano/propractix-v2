import { spawnSync } from 'node:child_process';
import { randomBytes } from 'node:crypto';
import { mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { compose, publish, root } from './e2e-environment.mjs';

function run(command, args) {
  const result = spawnSync(command, args, { cwd: root, stdio: 'inherit' });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`E8 prerequisite failed: ${command}`);
}

process.chdir(root);
process.env.PROPRACTIX_E2E_PROJECT = `propractix-e8-${process.pid}`;
mkdirSync('.local/e2e', { recursive: true });
try { mkdirSync('.local/e2e/run.lock'); }
catch { throw new Error('Another E8 run owns .local/e2e/run.lock; see the local E2E runbook'); }
mkdirSync('.local/e2e/secrets', { recursive: true, mode: 0o700 });
for (const name of ['database.password', 'rate-limit-hmac-rl-ip-v1']) {
  writeFileSync(`.local/e2e/secrets/${name}`, randomBytes(32).toString('hex'), { mode: 0o600 });
}
writeFileSync('.local/e2e/publications.log', '');
let exitCode = 1;
let cleaned = false;
function cleanup() {
  if (cleaned) return;
  cleaned = true;
  try { writeFileSync('.local/e2e/stack.log', compose(['logs', '--no-color']).stdout); }
  catch (error) { console.error(error); exitCode = 1; }
  try { compose(['down', '--volumes', '--remove-orphans'], { stdio: 'inherit' }); }
  catch (error) { console.error(error); exitCode = 1; }
  rmSync('.local/e2e/secrets', { recursive: true, force: true });
  rmSync('.local/e2e/run.lock', { recursive: true, force: true });
}
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => { cleanup(); process.exit(130); });
}
try {
  run('./server/mvnw', ['-B', '-f', 'server/pom.xml', '-DskipTests', 'package']);
  run(process.execPath, [process.env.npm_execpath, 'run', 'client:build']);
  compose(['up', '-d', '--wait', 'postgres', 'caddy'], { stdio: 'inherit' });
  const caddyId = compose(['ps', '-q', 'caddy']).stdout.trim();
  const inspected = spawnSync('docker', ['inspect', caddyId], { encoding: 'utf8' });
  if (inspected.status !== 0) throw new Error('Cannot inspect isolated Caddy peer');
  const networks = JSON.parse(inspected.stdout)[0].NetworkSettings.Networks;
  const peer = Object.values(networks)[0].IPAddress;
  process.env.PROPRACTIX_E2E_CLIENT_GATEWAY = Object.values(networks)[0].Gateway;
  process.env.PROPRACTIX_E2E_PROXY_PEER = peer;
  process.env.PROPRACTIX_E2E_TRUSTED_PROXY_CIDR = `${peer}/32`;
  compose(['up', '-d', 'server'], { stdio: 'inherit' });
  const address = compose(['port', 'caddy', '8080']).stdout.trim();
  process.env.PROPRACTIX_E2E_BASE_URL = `http://${address}`;
  const deadline = Date.now() + 90_000;
  let ready = false;
  while (Date.now() < deadline) {
    ready = compose(['exec', '-T', 'server', 'wget', '-qO-',
      'http://127.0.0.1:8080/actuator/health/readiness'], { allowFailure: true }).status === 0;
    if (ready) break;
    await new Promise((resolve) => setTimeout(resolve, 500));
  }
  if (!ready) throw new Error('E8 backend did not become ready');
  publish(1);
  const tests = spawnSync(process.execPath, ['node_modules/playwright/cli.js', 'test', '-c', 'client/playwright.config.ts', ...process.argv.slice(2)],
    { cwd: root, stdio: 'inherit' });
  exitCode = tests.status ?? 1;
} catch (error) {
  console.error(error);
} finally {
  cleanup();
}
process.exitCode = exitCode;
