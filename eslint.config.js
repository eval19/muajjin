import js from '@eslint/js';
import globals from 'globals';
import reactHooks from 'eslint-plugin-react-hooks';
import reactRefresh from 'eslint-plugin-react-refresh';
import tseslint from 'typescript-eslint';

// These modules intentionally co-locate a component with its public hook/style
// factory. Vite may invalidate their importers instead of retaining local state;
// the exception names do not exempt any other exports or component modules.
const colocatedExports = [
  ['src/components/ui/badge.tsx', ['badgeVariants']],
  ['src/components/ui/button.tsx', ['buttonVariants']],
  ['src/components/ui/form.tsx', ['useFormField']],
  ['src/components/ui/navigation-menu.tsx', ['navigationMenuTriggerStyle']],
  ['src/components/ui/sidebar.tsx', ['useSidebar']],
  ['src/components/ui/toggle.tsx', ['toggleVariants']],
  ['src/contexts/app-context.tsx', ['useApp']],
  ['src/contexts/translation-context.tsx', ['useTranslation']],
];

export default [
  {
    ignores: [
      'dist/**', 'android/**', 'node_modules/**', 'build/**', '*.lock',
      '.tools/**', 'reproducible-output/**',
    ],
  },
  {
    files: ['**/*.{js,mjs,cjs}'],
    languageOptions: { globals: globals.node },
    rules: js.configs.recommended.rules,
  },
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: globals.browser,
      parser: tseslint.parser,
      parserOptions: { ecmaFeatures: { jsx: true } },
    },
    plugins: {
      '@typescript-eslint': tseslint.plugin,
      'react-hooks': reactHooks,
      'react-refresh': reactRefresh,
    },
    rules: {
      ...js.configs.recommended.rules,
      ...reactHooks.configs.recommended.rules,
      'react-refresh/only-export-components': ['warn', { allowConstantExport: true }],
      // TypeScript resolves declarations and overloads. The TS-aware unused
      // rule is enabled below for tooling/tests without a whole-app migration.
      'no-undef': 'off',
      'no-unused-vars': 'off',
      'no-redeclare': 'off',
    },
  },
  {
    files: ['*.config.ts', 'tests/**/*.ts', 'scripts/**/*.ts'],
    languageOptions: { globals: { ...globals.node, Bun: 'readonly' } },
    rules: {
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' }],
    },
  },
  ...colocatedExports.map(([file, names]) => ({
    files: [file],
    rules: {
      'react-refresh/only-export-components': ['warn', {
        allowConstantExport: true,
        allowExportNames: names,
      }],
    },
  })),
];
