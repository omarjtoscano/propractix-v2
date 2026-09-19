import { readFile, readdir } from 'node:fs/promises';
import { extname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

async function sourceFiles(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map((entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? sourceFiles(path) : [path];
  }));
  return nested.flat().filter((path) => ['.tsx', '.jsx'].includes(extname(path)) && !path.endsWith('.test.tsx'));
}

const visibleLiteral = />\s*[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][^<{]*</u;
for (const file of await sourceFiles(fileURLToPath(new URL('../src', import.meta.url)))) {
  const content = await readFile(file, 'utf8');
  if (visibleLiteral.test(content)) {
    throw new Error(`visible_text_must_use_i18n:${file}`);
  }
}
