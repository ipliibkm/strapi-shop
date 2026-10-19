module.exports = ({ env }) => ({
 connection: {
  client: 'postgres',
  connection: {
   host: env('DATABASE_HOST', 'localhost'), port: env.int('DATABASE_PORT', 5432),
   database: env('DATABASE_NAME', 'cms'), user: env('DATABASE_USERNAME', 'cms'),
   password: env('DATABASE_PASSWORD'), ssl: false
  }, pool: { min: 0, max: 10 }
 }
});
