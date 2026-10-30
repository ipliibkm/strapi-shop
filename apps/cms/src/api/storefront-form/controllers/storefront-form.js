'use strict';

const { createCoreController } = require('@strapi/strapi').factories;

const UID = 'api::storefront-form.storefront-form';

module.exports = createCoreController(UID, ({ strapi }) => ({
  async bySlug(ctx) {
    const [form] = await strapi.documents(UID).findMany({
      status: 'published',
      filters: { slug: ctx.params.slug, enabled: true },
      fields: [
        'id',
        'documentId',
        'name',
        'slug',
        'enabled',
        'submitLabel',
        'successMessage',
        'sendMailEnabled',
        'fields',
      ],
      limit: 1,
    });
    if (!form) return ctx.notFound('This form is not available.');
    ctx.body = {
      id: Number(form.id),
      documentId: form.documentId,
      name: form.name,
      slug: form.slug,
      enabled: form.enabled,
      submitLabel: form.submitLabel,
      successMessage: form.successMessage,
      sendMailEnabled: form.sendMailEnabled !== false,
      fields: Array.isArray(form.fields) ? form.fields : [],
    };
  },
}));
