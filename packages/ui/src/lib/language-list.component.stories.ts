import type { Meta, StoryObj } from '@storybook/angular';
import { OpenShopfrontLanguageListComponent, LanguageRow } from './language-list.component';

const languages: LanguageRow[] = [
  { code: 'de', name: 'Deutsch', enabled: false },
  { code: 'en', name: 'English', enabled: true },
  { code: 'ru', name: 'Русский', enabled: true },
  { code: 'tr', name: 'Türkçe', enabled: true },
];

const meta: Meta<OpenShopfrontLanguageListComponent> = {
  title: 'Components/LanguageList',
  component: OpenShopfrontLanguageListComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontLanguageListComponent>;

const listTemplate = `<openshopfront-language-list [languages]="languages" [label]="label"></openshopfront-language-list>`;

export const Default: Story = {
  render: (args) => ({ props: args, template: listTemplate }),
  args: { languages, label: 'Website languages' },
};

export const AllEnabled: Story = {
  render: (args) => ({ props: args, template: listTemplate }),
  args: {
    languages: languages.map((item) => ({ ...item, enabled: true })),
    label: 'Admin interface languages',
  },
};
