run:
	wrangler dev

build:
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/main.clj > _build_/main.js

hook:
	@NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source .dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&url=$$NGROK_URL"

test:
	clear && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../src/main.clj > _build_/main.js && \
	~/Projects/language/clj2js/_build/default/bin/main.exe ../test/test.clj > _build_/test.js && \
	node --env-file=.dev.vars _build_/test.js

.PHONY: run build hook test
