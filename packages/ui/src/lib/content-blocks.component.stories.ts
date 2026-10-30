import type { Meta, StoryObj } from '@storybook/angular';
import {
  ContentBlock,
  ContentBlocksLabels,
  ContentProduct,
  OpenShopfrontContentBlocksComponent,
} from './content-blocks.component';

const blocks: ContentBlock[] = [
  { type: 'heading', eyebrow: 'OUR WORK', title: 'A studio built around you' },
  {
    type: 'text',
    html: '<p>Every project starts with a conversation. We shape the details so the result feels unmistakably yours.</p>',
  },
  {
    type: 'image',
    url: 'https://images.unsplash.com/photo-1493663284031-b7e3aefcae8e?w=1200&q=80',
    caption: 'The atelier, morning light',
  },
  { type: 'cta', label: 'Book a visit', target: 'booking' },
  { type: 'products', limit: 6 },
];

const products: ContentProduct[] = [
  { id: 'p1', name: 'Signature Candle', priceLabel: 'From €24' },
  { id: 'p2', name: 'Atelier Tote', priceLabel: 'From €58' },
  { id: 'p3', name: 'Studio Print', priceLabel: 'From €36' },
];

const labels: ContentBlocksLabels = {
  continue: 'Continue',
  productsNone: 'No products are available yet.',
};

const meta: Meta<OpenShopfrontContentBlocksComponent> = {
  title: 'Components/ContentBlocks',
  component: OpenShopfrontContentBlocksComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontContentBlocksComponent>;

const blocksTemplate = `
  <openshopfront-content-blocks [blocks]="blocks" [products]="products" [labels]="labels"></openshopfront-content-blocks>
`;

export const Default: Story = {
  render: (args) => ({ props: args, template: blocksTemplate }),
  args: { blocks, products, labels },
};

const formBlocks: ContentBlock[] = [
  { type: 'heading', eyebrow: 'GET IN TOUCH', title: 'Send us a note' },
  { type: 'form', formSlug: 'contact' },
];

export const WithFormBlock: Story = {
  render: (args) => ({
    props: args,
    template: `
      <openshopfront-content-blocks [blocks]="blocks" [products]="[]" [labels]="labels">
        <ng-template #tpl openshopfront-form-block let-slug>
          <div class="story-form-slot">Form for slug: {{ slug }}</div>
        </ng-template>
      </openshopfront-content-blocks>
    `,
  }),
  args: { blocks: formBlocks, products: [], labels },
};
