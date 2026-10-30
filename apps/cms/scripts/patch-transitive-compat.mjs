import { readFileSync, writeFileSync } from 'node:fs';

const zodFiles = [
  'node_modules/koa-session/dist/commonjs/index.js',
  'node_modules/koa-session/dist/commonjs/index.d.ts',
  'node_modules/koa-session/dist/esm/index.js',
  'node_modules/koa-session/dist/esm/index.d.ts',
  'node_modules/koa-body/lib/types.js',
  'node_modules/koa-body/lib/types.d.ts',
];

for (const file of zodFiles) {
  patch(file, (source) =>
    source
      .replaceAll('require("zod")', 'require("zod/v3")')
      .replaceAll("from 'zod'", "from 'zod/v3'")
      .replaceAll('from "zod"', 'from "zod/v3"'),
  );
}

for (const file of [
  'node_modules/markdown-it-emoji/index.js',
  'node_modules/markdown-it-emoji/light.js',
  'node_modules/markdown-it-emoji/bare.js',
]) {
  patch(file, (source) => source.replaceAll('md.utils.assign', 'Object.assign'));
}

function patch(file, transform) {
  const source = readFileSync(file, 'utf8');
  const patched = transform(source);
  if (source === patched) {
    throw new Error(`The compatibility patch did not match ${file}.`);
  }
  writeFileSync(file, patched);
}
