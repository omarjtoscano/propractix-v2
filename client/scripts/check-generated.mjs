import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join, relative } from 'node:path';
import { spawnSync } from 'node:child_process';

const root = fileURLToPath(new URL('../..', import.meta.url));
const generated = join(root, 'client/src/shared/api/generated');
async function snapshot(directory = generated) {
  const entries = (await readdir(directory, { withFileTypes: true })).sort((a, b) => a.name.localeCompare(b.name));
  const parts = await Promise.all(entries.map(async (entry) => {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) return snapshot(path);
    return { [relative(generated, path)]: await readFile(path, 'utf8') };
  }));
  return Object.assign({}, ...parts);
}
const before = await snapshot();
assert.ok(process.env.npm_execpath, 'run_generation_check_through_npm');
async function verifyGeneration() {
  const result = spawnSync(process.execPath, [process.env.npm_execpath, 'run', 'client:generate'], { cwd: root, stdio: 'inherit' });
  assert.equal(result.status, 0, 'client_generation_failed');
  assert.deepEqual(await snapshot(), before, 'generated_client_is_out_of_date_or_not_reproducible');
}
// These mutations share an output directory and must finish one after the other.
await verifyGeneration();
await verifyGeneration();
console.log(`Generated client matches OpenAPI; two repeat generations match all ${Object.keys(before).length} files.`);
