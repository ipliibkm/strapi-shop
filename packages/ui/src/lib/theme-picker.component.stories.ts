import type { Meta, StoryObj } from '@storybook/angular';
import {
  OpenShopfrontThemePickerComponent,
  PaletteFieldDef,
  ThemePickerLabels,
  ThemePickerOption,
} from './theme-picker.component';

const themes: ThemePickerOption[] = [
  {
    key: 'light',
    label: 'Maison',
    hint: 'Warm cream surfaces with copper accents.',
    swatches: ['#f5f0e8', '#ffffff', '#a65e37', '#211b15'],
  },
  {
    key: 'dark',
    label: 'Noir Atelier',
    hint: 'Deep charcoal surfaces with gold-copper light.',
    swatches: ['#141210', '#1d1a17', '#c98a4e', '#0f0d0b'],
  },

  {
    key: 'ocean',
    label: 'Lagoon',
    hint: 'Cool seafoam surfaces with deep teal accents.',
    swatches: ['#eef6f6', '#ffffff', '#0e7490', '#0b2830'],
  },
  {
    key: 'rosewater',
    label: 'Blush',
    hint: 'Soft pink surfaces with raspberry accents.',
    swatches: ['#fbeef1', '#ffffff', '#c25a7d', '#3a1f2b'],
  },
];

const paletteFields: PaletteFieldDef[] = [
  { key: 'bg', label: 'Background' },
  { key: 'surface', label: 'Surface' },
  { key: 'accent', label: 'Accent' },
  { key: 'fg', label: 'Text' },
  { key: 'sidebar', label: 'Sidebar' },
];

const labels: ThemePickerLabels = {
  groupLabel: 'Color palette',
  customTitle: 'Custom palette',
  customHint: 'Choose each color yourself.',
  saveLabel: 'Save custom palette',
  idleHint: 'Select a color to start editing.',
};

const meta: Meta<OpenShopfrontThemePickerComponent> = {
  title: 'Components/ThemePicker',
  component: OpenShopfrontThemePickerComponent,
};
export default meta;
type Story = StoryObj<OpenShopfrontThemePickerComponent>;

const pickerTemplate = `
  <openshopfront-theme-picker
    [themes]="themes"
    [selectedKey]="selectedKey"
    [usingCustom]="usingCustom"
    [showCustom]="showCustom"
    [customSwatches]="customSwatches"
    [paletteFields]="paletteFields"
    [draftPalette]="draftPalette"
    [busy]="busy"
    [labels]="labels"
  ></openshopfront-theme-picker>
`;

export const Default: Story = {
  render: (args) => ({ props: args, template: pickerTemplate }),
  args: {
    themes,
    selectedKey: 'light',
    usingCustom: false,
    showCustom: true,
    customSwatches: ['#f5f0e8', '#ffffff', '#a65e37', '#211b15'],
    paletteFields,
    draftPalette: null,
    busy: false,
    labels,
  },
};

export const CustomActive: Story = {
  render: (args) => ({ props: args, template: pickerTemplate }),
  args: {
    ...Default.args,
    selectedKey: null,
    usingCustom: true,
    customSwatches: ['#eef6f6', '#ffffff', '#0e7490', '#0b2830'],
  },
};

export const EditorOpen: Story = {
  render: (args) => ({ props: args, template: pickerTemplate }),
  args: {
    ...Default.args,
    selectedKey: null,
    usingCustom: true,
    customSwatches: ['#eef6f6', '#ffffff', '#0e7490', '#0b2830'],
    draftPalette: {
      bg: '#eef6f6',
      surface: '#ffffff',
      accent: '#0e7490',
      fg: '#0c3742',
      sidebar: '#0b2830',
    },
  },
};

export const PresetsOnly: Story = {
  render: (args) => ({ props: args, template: pickerTemplate }),
  args: { ...Default.args, showCustom: false },
};
