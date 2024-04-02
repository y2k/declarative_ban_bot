deploy:
	@ wrangler deploy

run: hook
	@ wrangler dev

test: build
	@ clear && clj2js js ../vendor/packages/effects/effects.clj ../vendor/prelude/js/src/prelude.clj > _build_/effects.js
	@ clear && clj2js js ../test/test_spam.clj ../vendor/prelude/js/src/prelude.clj > _build_/test_spam.js
	@ clear && node --env-file=.dev.vars _build_/test_spam.js
	@ clear && clj2js js ../test/test.clj > _build_/test.js
	@ clear && node --env-file=.dev.vars _build_/test.js

build:
	@ mkdir -p _build_
	@ echo '{"type": "module"}' > _build_/package.json
	@ clear && clj2js js prelude > _build_/prelude.js
	@ clear && clj2js js ../vendor/packages/effects/effects.clj > _build_/effects.js
	@ clear && clj2js js ../src/main.clj > _build_/main.js

db:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=test_db.sql > _build_/db_result.json

prod_db:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --file=test_db.sql > _build_/db_result.json

migrate:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=schema.sql

clean:
	@ rm -rf _build_

hook:
	@NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source .dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&secret_token=$$TG_SECRET_TOKEN&url=$$NGROK_URL"

.PHONY: run build test db migrate clean hook prod_db
