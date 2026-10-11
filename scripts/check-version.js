#!/usr/bin/env node
// Assert that every hard-coded copy of the cartoj version matches VERSION.
// VERSION is the single source of truth; build.clj and the codox alias read
// it directly, while JSON/markdown/edn files must stay in sync by hand.
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const version = fs.readFileSync(path.join(root, 'VERSION'), 'utf8').trim();

let failed = false;

function ok(msg) {
  console.log('✓ ' + msg);
}

function fail(msg) {
  console.error('✗ ' + msg);
  failed = true;
}

function read(file) {
  return fs.readFileSync(path.join(root, file), 'utf8');
}

function readJson(file) {
  return JSON.parse(read(file));
}

function assertContains(file, needle) {
  if (read(file).includes(needle)) {
    ok(`${file} references ${version}`);
  } else {
    fail(`${file} does not contain "${needle}"`);
  }
}

function assertJsonVersion(file) {
  const actual = readJson(file).version;
  if (actual === version) {
    ok(`${file} version is ${version}`);
  } else {
    fail(`${file} version is ${actual}, expected ${version}`);
  }
}

assertJsonVersion('package.json');
assertJsonVersion('starter/package.json');

assertContains('starter/deps.edn', `io.github.perrygeo/cartoj {:mvn/version "${version}"}`);
assertContains('starter/README.md', `io.github.perrygeo/cartoj ${version}`);
assertContains('doc/01-getting-started.md', `io.github.perrygeo/cartoj {:mvn/version "${version}"}`);
assertContains('doc/01-getting-started.md', `"version": "${version}"`);
assertContains('README.md', `io.github.perrygeo/cartoj {:mvn/version "${version}"}`);

if (failed) {
  console.error('\nVersion drift detected. Bump VERSION and update the files above, or fix the out-of-date references.');
  process.exit(1);
}

console.log(`\nAll version references match VERSION (${version}).`);
