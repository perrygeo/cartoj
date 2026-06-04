#!/usr/bin/env node
// Rewrites source-map `sources` entries from the shadow-cljs output
// to absolute paths so c8/istanbul's HTML reporter can read the
// original .cljs files from disk.

const fs = require('fs');
const path = require('path');

const SRC_ROOTS = ['src', 'test', 'dev'].map(p => path.resolve(__dirname, '..', p));
const MAP_DIR = path.resolve(
  __dirname,
  '..',
  '.shadow-cljs/builds/test/dev/out/cljs-runtime'
);

function resolveSource(rel) {
  for (const root of SRC_ROOTS) {
    const candidate = path.join(root, rel);
    if (fs.existsSync(candidate)) return candidate;
  }
  return null;
}

if (!fs.existsSync(MAP_DIR)) {
  console.error(`map dir not found: ${MAP_DIR}`);
  process.exit(1);
}

let rewritten = 0;
for (const file of fs.readdirSync(MAP_DIR)) {
  if (!file.endsWith('.js.map')) continue;
  const full = path.join(MAP_DIR, file);
  const map = JSON.parse(fs.readFileSync(full, 'utf8'));
  if (!Array.isArray(map.sources)) continue;
  let changed = false;
  map.sources = map.sources.map(src => {
    if (path.isAbsolute(src)) return src;
    const abs = resolveSource(src);
    if (abs) {
      changed = true;
      return abs;
    }
    return src;
  });
  if (changed) {
    fs.writeFileSync(full, JSON.stringify(map));
    rewritten++;
  }
}
console.log(`rewrote ${rewritten} source map(s)`);
