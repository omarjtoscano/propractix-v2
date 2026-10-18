import { readFile, readdir } from 'node:fs/promises';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { flattenCatalog, readCatalogs } from './i18n-catalogs.mjs';
import { visibleTextViolations } from './visible-text.mjs';

async function sourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? sourceFiles(path) : [path];
  }));
  return nested.flat().filter((path) => ['.ts', '.tsx', '.js', '.jsx'].includes(extname(path))
    && !/\.(?:test|spec)\.[jt]sx?$/u.test(path) && !path.endsWith('.d.ts'));
}

const { catalogs, fallbackLocale } = await readCatalogs();
const knownKeys = new Set(flattenCatalog(catalogs.get(fallbackLocale)).map(([key]) => key));
for (const file of await sourceFiles(fileURLToPath(new URL('../src', import.meta.url)))) {
  const content = await readFile(file, 'utf8');
  const issues = visibleTextViolations(content, file, knownKeys);
  if (issues.length > 0) throw new Error(issues.join('\n'));
}
