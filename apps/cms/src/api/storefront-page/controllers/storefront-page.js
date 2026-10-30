'use strict';

const { createCoreController } = require('@strapi/strapi').factories;

const UID = 'api::storefront-page.storefront-page';

module.exports = createCoreController(UID, ({ strapi }) => ({
  async enabled(ctx) {
    const pages = await strapi.documents(UID).findMany({
      status: 'published',
      filters: { enabled: true },
      fields: ['slug', 'title'],
      sort: ['slug:asc'],
    });
    ctx.body = pages.map(({ slug, title }) => ({ slug, title }));
  },

  async bySlug(ctx) {
    const [page] = await strapi.documents(UID).findMany({
      status: 'published',
      filters: { slug: ctx.params.slug, enabled: true },
      fields: ['slug', 'title', 'enabled', 'blocks'],
      limit: 1,
    });
    if (!page) return ctx.notFound('This storefront page is not available.');
    ctx.body = {
      slug: page.slug,
      title: page.title,
      enabled: page.enabled,
      blocks: Array.isArray(page.blocks) ? page.blocks : [],
    };
  },
}));
