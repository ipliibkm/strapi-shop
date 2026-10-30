import type { Meta, StoryObj } from '@storybook/angular';
import { OpenShopfrontImageUploadComponent } from './image-upload.component';

const meta: Meta<OpenShopfrontImageUploadComponent> = {
  title: 'Components/Image Upload',
  component: OpenShopfrontImageUploadComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontImageUploadComponent>;

export const Default: Story = {
  args: {
    images: ['https://picsum.photos/id/1025/400/400'],
    maxImages: 8,
  },
};
