import type { StorybookConfig } from '@storybook/angular';

const config: StorybookConfig = {
  stories: ['../packages/ui/src/**/*.stories.ts'],
  framework: {
    name: '@storybook/angular',
    options: {},
  },
  webpackFinal: async (webpackConfig) => ({
    ...webpackConfig,
    // Storybook includes the Angular compiler in its preview bundle by design.
    performance: { hints: false },
  }),
};

export default config;
