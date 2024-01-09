run:
	wrangler dev

build:
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/libs/prelude.clj > _build_/prelude.js && \
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/main.clj > _build_/main.js

test:
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/libs/prelude.clj > _build_/prelude.js && \
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/main.clj > _build_/main.js && \
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../test/test.clj > _build_/test.js && \
	clear && \
	node --env-file=.dev.vars _build_/test.js

db:
	wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=test_db.sql > _build_/db_result.json

migrate:
	wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=schema.sql

hook:
	@NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source .dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&secret_token=$$TG_SECRET_TOKEN&url=$$NGROK_URL"

.PHONY: run build test db migrate hook
