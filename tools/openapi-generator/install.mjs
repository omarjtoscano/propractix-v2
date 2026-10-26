import assert from 'node:assert/strict';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// Reuse the exact npm CLI that runs the root installation, including in CI/Docker.
assert.ok(process.env.npm_execpath, 'run_installation_through_npm');
const result = spawnSync(process.execPath, [process.env.npm_execpath, 'ci', '--prefix',
  fileURLToPath(new URL('.', import.meta.url)), '--ignore-scripts'], { stdio: 'inherit' });
if (result.error) throw result.error;
process.exit(result.status ?? 1);
