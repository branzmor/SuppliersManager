import js from '@eslint/js';
import globals from 'globals';
import tseslint from 'typescript-eslint';
import playwright from 'eslint-plugin-playwright';

export default tseslint.config(
  { ignores: ['node_modules', '.features-gen', 'playwright-report', 'test-results'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.ts'],
    ...playwright.configs['flat/recommended'],
    languageOptions: { globals: globals.node },
    rules: {
      ...playwright.configs['flat/recommended'].rules,
      // Step definitions assert through the page object / helpers, not inline `expect`s.
      'playwright/no-standalone-expect': 'off',
      'playwright/expect-expect': 'off',
      '@typescript-eslint/no-unused-vars': ['warn', { argsIgnorePattern: '^_' }],
    },
  },
);
