import type { Meta, StoryObj } from '@storybook/angular';
import { OpenShopfrontGalleryComponent, OpenShopfrontGalleryItem } from './gallery.component';

const meta: Meta<OpenShopfrontGalleryComponent> = {
  title: 'Components/Gallery',
  component: OpenShopfrontGalleryComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontGalleryComponent>;

const photos: OpenShopfrontGalleryItem[] = [
  {
    id: '1',
    url: 'https://picsum.photos/id/1015/800/800',
    title: 'River valley',
    caption: 'Morning light',
  },
  { id: '2', url: 'https://picsum.photos/id/1025/800/800', title: 'Studio companion' },
  { id: '3', url: 'https://picsum.photos/id/1035/800/800', caption: 'Behind the scenes' },
  { id: '4', url: 'https://picsum.photos/id/1043/800/800', title: 'City walk' },
  { id: '5', url: 'https://picsum.photos/id/1050/800/800', caption: 'Product detail' },
  { id: '6', url: 'https://picsum.photos/id/1062/800/800', title: 'Team event' },
];

export const Default: Story = {
  render: (args) => ({
    props: args,
    template: `<openshopfront-gallery [items]="items"></openshopfront-gallery>`,
  }),
  args: { items: photos },
};

export const Placeholders: Story = {
  render: () => ({
    props: {},
    template: `
      <openshopfront-gallery
        [items]="[
          { id: 'a', title: 'Bridal suite' },
          { id: 'b', title: 'Lash studio', caption: 'New room' },
          { id: 'c', title: 'Consultation corner' },
          { id: 'd', title: 'Retail shelf' }
        ]"
      ></openshopfront-gallery>
    `,
  }),
};
