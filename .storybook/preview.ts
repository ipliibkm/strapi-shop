import type { Preview } from '@storybook/angular';

// Global component styling is loaded via the builder's `styles` option (see angular.json),
// so it does not need to be imported here.
const preview: Preview = {
  parameters: {
    controls: {
      matchers: {
        color: /(background|color)$/i,
        date: /Date$/i,
      },
    },
    layout: 'centered',
  },
};

export default preview;
