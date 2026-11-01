import { spawnSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { appendFileSync, readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

export const root = fileURLToPath(new URL('../', import.meta.url));
export function compose(args, options = {}) {
  const project = process.env.PROPRACTIX_E2E_PROJECT;
  if (!/^propractix-e8-\d+$/.test(project ?? '')) throw new Error('Use npm run client:e2e to create an isolated stack');
  const result = spawnSync('docker', ['compose', '-p', project, '-f', 'infra/e2e/compose.yaml', ...args], {
    cwd: root, encoding: 'utf8', timeout: 180_000, ...options,
  });
  if (result.error) throw result.error;
  if (result.status !== 0 && !options.allowFailure) throw new Error(`E8 Docker command failed: ${result.stderr}`);
  return result;
}

// Only the disposable, unexposed E8 database is reachable through this helper.
export function sql(statement) {
  return compose(['exec', '-T', 'postgres', 'psql', '-X', '-A', '-t', '-v', 'ON_ERROR_STOP=1',
    '-U', 'propractix_e8', '-d', 'propractix_e8'], { input: statement }).stdout.trim();
}

export function publish(version, { invalid = false } = {}) {
  const name = `academic-institutions-e8-synthetic-v${version}`;
  const manifest = JSON.parse(readFileSync(path.join(root, `docs/catalog/fixtures/${name}.json`), 'utf8'));
  const actual = createHash('sha256').update(readFileSync(path.join(root, `docs/catalog/fixtures/${manifest.artifact}`))).digest('hex');
  if (actual !== manifest.artifactHash) throw new Error('Synthetic fixture hash mismatch');
  const prefix = '--propractix.academic-institution.catalog-import.';
  const result = compose(['run', '--rm', '--no-deps', '-T', '--entrypoint', 'java', 'server',
    '-Dloader.main=com.propractix.academicinstitution.configuration.CatalogImportPublicationApplication',
    '-cp', '/application/application.jar', 'org.springframework.boot.loader.launch.PropertiesLauncher',
    `${prefix}enabled=true`, `${prefix}artifact-path=/fixtures/${manifest.artifact}`,
    `${prefix}schema-version=${manifest.schemaVersion}`, `${prefix}source-url=${manifest.sourceUrl}`,
    `${prefix}retrieved-at=${manifest.retrievedAt}`, `${prefix}source-attribution=${manifest.sourceAttribution}`,
    `${prefix}artifact-hash=${invalid ? '0'.repeat(64) : manifest.artifactHash}`], { allowFailure: invalid });
  appendFileSync(path.join(root, '.local/e2e/publications.log'), result.stdout + result.stderr);
  if (invalid ? result.status !== 4 : result.status !== 0) throw new Error('Unexpected administrative publication exit code');
  return result;
}

export function metric(name) {
  return compose(['exec', '-T', 'server', 'java', '--add-modules', 'jdk.attach', '/e2e/ReadMetric.java', name]).stdout.trim();
}
