import type { Meta, StoryObj } from '@storybook/angular';
import {
  OpenShopfrontProductListComponent,
  OpenShopfrontProductListItem,
} from './product-list.component';

const meta: Meta<OpenShopfrontProductListComponent> = {
  title: 'Components/ProductList',
  component: OpenShopfrontProductListComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontProductListComponent>;

const products: OpenShopfrontProductListItem[] = [
  {
    id: '1',
    name: 'Signature lash set',
    description: 'Full set with retention care included.',
    priceLabel: '€189',
    badge: 'New',
  },
  {
    id: '2',
    name: 'Brow lamination',
    description: 'Shaping and tinting for a defined look.',
    priceLabel: '€79',
  },
  {
    id: '3',
    name: 'Care serum duo',
    description: 'Nightly growth support, two-month supply.',
    priceLabel: '€45',
    badge: '-20%',
  },
  {
    id: '4',
    name: 'Studio gift card',
    description: 'Valid for all services and retail items.',
    priceLabel: 'From €25',
  },
];

export const Default: Story = {
  render: (args) => ({
    props: args,
    template: `<openshopfront-product-list [products]="products"></openshopfront-product-list>`,
  }),
  args: { products },
};

export const WithAddToCart: Story = {
  render: () => ({
    props: {},
    template: `
      <openshopfront-product-list
        [products]="[
          { id: '1', name: 'Signature lash set', description: 'Full set with retention care included.', priceLabel: '€189' },
          { id: '2', name: 'Brow lamination', description: 'Shaping and tinting for a defined look.', priceLabel: '€79' }
        ]"
        [labels]="{ addToCart: 'Add to cart' }"
      ></openshopfront-product-list>
    `,
  }),
};

export const Empty: Story = {
  render: () => ({
    props: {},
    template: `<openshopfront-product-list [products]="[]" [labels]="{ empty: 'No products yet.' }"></openshopfront-product-list>`,
  }),
};
