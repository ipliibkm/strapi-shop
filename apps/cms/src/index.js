const fs = require('node:fs');
module.exports = {
 async bootstrap({ strapi }) {
  const path = process.env.BUSINESS_SETUP_FILE || '/opt/app/config/business.json';
  let source;
  try {
    if (!fs.statSync(path).isFile()) return;
    source = JSON.parse(fs.readFileSync(path,'utf8'));
  } catch (error) {
    strapi.log.warn(`Business setup file is not usable: ${error.message}`);
    return;
  }
  const docs = strapi.documents('api::business-profile.business-profile');
  const existing = await docs.findFirst();
  if (existing) return; // Existing business data is never overwritten by restart.
  await docs.create({data:{
    name:source.name,slug:source.slug,contactEmail:source.contactEmail,
    timezone:source.timezone,localeCode:source.locale,currency:source.currency,
    address:source.address || null,phone:source.phone || null
  }});
  strapi.log.info('Initial business profile seeded once');
 }
};
