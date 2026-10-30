const fs = require('node:fs');

const LEGACY_CONTACT_BLOCKS = [
  { type: 'heading', eyebrow: 'CONTACT', title: 'Say hello.' },
  { type: 'text', html: '<p>Add your contact details and opening hours here.</p>' },
];

function hasLegacyContactBlocks(blocks) {
  return (
    Array.isArray(blocks) &&
    blocks.length === 2 &&
    blocks[0]?.type === 'heading' &&
    blocks[0]?.eyebrow === 'CONTACT' &&
    blocks[0]?.title === 'Say hello.' &&
    blocks[1]?.type === 'text' &&
    blocks[1]?.html === '<p>Add your contact details and opening hours here.</p>'
  );
}

const DEFAULT_STOREFRONT_PAGES = [
  {
    slug: 'home',
    title: 'Home',
    blocks: [
      { type: 'heading', eyebrow: 'YOUR BUSINESS. YOUR STORY.', title: 'Good things start here.' },
      {
        type: 'text',
        html: '<p>Welcome to a space built for your customers. Edit this page in the administration panel.</p>',
      },
      { type: 'cta', label: 'Book a visit', target: 'booking' },
    ],
  },
  {
    slug: 'services',
    title: 'Services',
    blocks: [
      { type: 'heading', eyebrow: 'OUR SERVICES', title: 'What we offer.' },
      { type: 'text', html: '<p>Explore our services and available times.</p>' },
      { type: 'cta', label: 'Book a visit', target: 'booking' },
    ],
  },
  {
    slug: 'booking',
    title: 'Book',
    blocks: [{ type: 'heading', eyebrow: 'BOOK ONLINE', title: 'Find time for your visit.' }],
  },
  {
    slug: 'shop',
    title: 'Shop',
    blocks: [{ type: 'heading', eyebrow: 'SHOP', title: 'Our collection.' }],
  },
  {
    slug: 'about',
    title: 'About',
    blocks: [
      { type: 'heading', eyebrow: 'OUR STORY', title: 'People come first.' },
      { type: 'text', html: '<p>Tell customers about your business and your team.</p>' },
    ],
  },
  {
    slug: 'contact',
    title: 'Contact',
    blocks: [...LEGACY_CONTACT_BLOCKS, { type: 'form', formSlug: 'contact' }],
  },
];

const DEFAULT_STOREFRONT_FORMS = [
  {
    slug: 'contact',
    name: 'Contact form',
    enabled: true,
    sendMailEnabled: true,
    submitLabel: 'Send message',
    successMessage: 'Thank you. Your message was submitted.',
    fields: [
      {
        key: 'name',
        label: 'Name',
        type: 'text',
        required: true,
        placeholder: 'Your name',
        minLength: 2,
        maxLength: 120,
      },
      {
        key: 'email',
        label: 'Email address',
        type: 'email',
        required: true,
        placeholder: 'you@example.com',
        maxLength: 254,
      },
      {
        key: 'subject',
        label: 'Subject',
        type: 'select',
        required: true,
        options: ['General question', 'Booking question', 'Product question'],
      },
      {
        key: 'message',
        label: 'Message',
        type: 'textarea',
        required: true,
        placeholder: 'How can we help?',
        minLength: 10,
        maxLength: 5000,
      },
    ],
  },
];

module.exports = {
  async bootstrap({ strapi }) {
    const path = process.env.BUSINESS_SETUP_FILE || '/opt/app/config/business.json';
    let source;
    try {
      if (!fs.statSync(path).isFile()) throw new Error('The path is not a file.');
      source = JSON.parse(fs.readFileSync(path, 'utf8'));
    } catch (error) {
      strapi.log.warn(`Business setup file is not usable: ${error.message}`);
    }
    if (source) {
      const docs = strapi.documents('api::business-profile.business-profile');
      const existing = await docs.findFirst();
      if (!existing) {
        await docs.create({
          data: {
            name: source.name,
            slug: source.slug,
            contactEmail: source.contactEmail,
            timezone: source.timezone,
            localeCode: source.locale,
            currency: source.currency,
            address: source.address || null,
            phone: source.phone || null,
          },
        });
        strapi.log.info('Initial business profile seeded once');
      }
    }

    const pages = strapi.documents('api::storefront-page.storefront-page');
    for (const page of DEFAULT_STOREFRONT_PAGES) {
      const existing = await pages.findFirst({ filters: { slug: page.slug } });
      if (!existing) {
        await pages.create({ data: { ...page, enabled: true }, status: 'published' });
      } else if (page.slug === 'contact' && hasLegacyContactBlocks(existing.blocks)) {
        await pages.update({
          documentId: existing.documentId,
          data: { blocks: page.blocks },
          status: 'published',
        });
      }
    }

    const forms = strapi.documents('api::storefront-form.storefront-form');
    for (const form of DEFAULT_STOREFRONT_FORMS) {
      const existing = await forms.findFirst({ filters: { slug: form.slug } });
      if (!existing) await forms.create({ data: form, status: 'published' });
    }
  },
};
