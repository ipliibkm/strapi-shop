'use strict';

const { createCoreController } = require('@strapi/strapi').factories;

const UID = 'api::business-profile.business-profile';

module.exports = createCoreController(UID, ({ strapi }) => ({
  async public(ctx) {
    const [profile] = await strapi.documents(UID).findMany({ limit: 1 });
    if (!profile) return ctx.notFound('The business profile is not available.');
    ctx.body = {
      contactEmail: typeof profile.contactEmail === 'string' ? profile.contactEmail : '',
    };
  },
}));
