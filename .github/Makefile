PRELUDE_PATH := $(shell realpath ../vendor/prelude/js/src/prelude.clj)

run: hook
	@ wrangler dev --port 8787

deploy: test
	@ wrangler deploy

test: build
	@ clear && OCAMLRUNPARAM=b clj2js js ../test/test_spam.clj $(PRELUDE_PATH) > bin/test/test_spam.js
	@ clear && OCAMLRUNPARAM=b clj2js js ../test/test.clj $(PRELUDE_PATH) > bin/test/test.js
	@ clear && node --env-file=.dev.vars bin/test/test_spam.js
	@ clear && node --env-file=.dev.vars bin/test/test.js

build:
	@ mkdir -p bin/src && mkdir -p bin/test && mkdir -p bin/vendor/packages/effects && mkdir -p bin/vendor/prelude/js/src
	@ echo '{"type": "module"}' > bin/package.json
	@ clear && echo "effects.clj"   && OCAMLRUNPARAM=b clj2js js ../vendor/packages/effects/effects.clj $(PRELUDE_PATH) > bin/vendor/packages/effects/effects.js
	@ clear && echo "moderator.clj" && OCAMLRUNPARAM=b clj2js js ../src/moderator.clj $(PRELUDE_PATH) > bin/src/moderator.js
	@ clear && echo "main.clj"      && OCAMLRUNPARAM=b clj2js js ../src/main.clj $(PRELUDE_PATH) > bin/src/main.js

db:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=test_db.sql > bin/db_result.json

prod_db:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --file=test_db.sql > bin/db_result.json

migrate:
	@ wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=schema.sql

clean:
	@ rm -rf bin

hook:
	@NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source .dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&secret_token=$$TG_SECRET_TOKEN&url=$$NGROK_URL"

.PHONY: run build test db migrate clean hook prod_db
