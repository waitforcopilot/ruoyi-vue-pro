# HRM frontend source

The Vue3 application is imported from https://github.com/yudaocode/yudao-ui-admin-vue3 at commit `0af03a93b6b6300f878e28add69b1c7a9f09ec34`. Upstream MIT LICENSE is preserved. Existing MES source files in this repository were retained. Registry URLs in the upstream lockfile are normalized to registry.npmjs.org without changing integrity checks.

Local development uses the standard Vite `development` mode with `.env.development`. Payroll audit timestamps are formatted for display instead of rendering epoch values. Runtime validation is documented in `hrm-runtime-verification.md`.
