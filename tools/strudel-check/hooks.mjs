// @kabelsalat/web, which @strudel/core imports, only points "main" at a CommonJS build. Node then
// misses its named exports; the ES module build next to it works.
export async function resolve(specifier, context, next) {
  if (specifier === '@kabelsalat/web') {
    return next(new URL('./node_modules/@kabelsalat/web/dist/index.mjs', import.meta.url).href, context);
  }
  return next(specifier, context);
}
