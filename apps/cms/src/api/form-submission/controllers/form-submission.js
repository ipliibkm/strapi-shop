'use strict';

const { createCoreController } = require('@strapi/strapi').factories;

const UID = 'api::form-submission.form-submission';

module.exports = createCoreController(UID, ({ strapi }) => ({
  async byForm(ctx) {
    const formUid = ctx.params.documentId;
    const result = await strapi.documents(UID).findMany({
      filters: { form: { documentId: { $eq: formUid } }, submittedAt: { $notNull: true } },
      sort: ['submittedAt:desc'],
      populate: { form: false, payload: 'shallow' },
    });
    ctx.body = result;
  },
}));
