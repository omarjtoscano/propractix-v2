import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { join, relative } from 'node:path';
import { spawnSync } from 'node:child_process';

const root = fileURLToPath(new URL('../..', import.meta.url));
const generated = join(root, 'client/src/shared/api/generated');
async function snapshot(directory = generated) {
  const result = {};
  for (const entry of (await readdir(directory, { withFileTypes: true })).sort((a, b) => a.name.localeCompare(b.name))) {
    const path = join(directory, entry.name);
    if (entry.isDirectory()) Object.assign(result, await snapshot(path));
    else result[relative(generated, path)] = await readFile(path, 'utf8');
  }
  return result;
}
const before = await snapshot();
for (let run = 1; run <= 2; run++) {
  const result = spawnSync('npm', ['run', 'client:generate'], { cwd: root, stdio: 'inherit' });
  assert.equal(result.status, 0, 'client_generation_failed');
  assert.deepEqual(await snapshot(), before, 'generated_client_is_out_of_date_or_not_reproducible');
}
console.log(`Generated client matches OpenAPI; two repeat generations match all ${Object.keys(before).length} files.`);
