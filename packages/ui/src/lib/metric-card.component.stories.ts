import type { Meta, StoryObj } from '@storybook/angular';
import { OpenShopfrontMetricCardComponent } from './metric-card.component';

const meta: Meta<OpenShopfrontMetricCardComponent> = {
  title: 'Components/MetricCard',
  component: OpenShopfrontMetricCardComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontMetricCardComponent>;

const cardTemplate = `<openshopfront-metric-card [icon]="icon" [tone]="tone" [label]="label" [value]="value" [hint]="hint"></openshopfront-metric-card>`;

export const Default: Story = {
  render: (args) => ({ props: args, template: cardTemplate }),
  args: {
    icon: 'event_available',
    tone: 'blue',
    label: 'Bookings',
    value: '128',
    hint: 'All confirmed bookings',
  },
};

export const Grid: Story = {
  render: () => ({
    props: {},
    template: `
      <div class="story-metric-row">
        <openshopfront-metric-card icon="event_available" tone="blue" label="Bookings" value="128" hint="All confirmed bookings"></openshopfront-metric-card>
        <openshopfront-metric-card icon="dynamic_form" tone="amber" label="Form submissions" value="342" hint="All storefront forms"></openshopfront-metric-card>
        <openshopfront-metric-card icon="shopping_bag" tone="green" label="Paid orders" value="96" hint="1,204 units sold"></openshopfront-metric-card>
        <openshopfront-metric-card icon="payments" tone="violet" label="Revenue" value="€48,210" hint="€1,320 refunded"></openshopfront-metric-card>
      </div>
    `,
  }),
};
