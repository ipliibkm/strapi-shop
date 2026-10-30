import type { Meta, StoryObj } from '@storybook/angular';
import {
  CapabilityRow,
  OpenShopfrontCapabilityTableComponent,
  CapabilityTableLabels,
} from './capability-table.component';

const rows: CapabilityRow[] = [
  { name: 'Booking management', live: true, owner: 'Operations' },
  { name: 'Form submissions', live: true, owner: 'Marketing' },
  { name: 'Commerce & inventory', live: true, owner: 'Storefront' },
  { name: 'Automated email campaigns', live: false, owner: 'Planned' },
];

const labels: CapabilityTableLabels = {
  capability: 'Capability',
  status: 'Status',
  owner: 'Owner',
  available: 'AVAILABLE',
  notImplemented: 'NOT IMPLEMENTED',
};

const meta: Meta<OpenShopfrontCapabilityTableComponent> = {
  title: 'Components/CapabilityTable',
  component: OpenShopfrontCapabilityTableComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontCapabilityTableComponent>;

const tableTemplate = `<openshopfront-capability-table [rows]="rows" [labels]="labels"></openshopfront-capability-table>`;

export const Default: Story = {
  render: (args) => ({ props: args, template: tableTemplate }),
  args: { rows, labels },
};
