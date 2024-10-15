PRELUDE_PATH := $(shell realpath vendor/prelude/js/src/prelude.clj)
WRANGLER_DIR := .github
BIN_DIR := .github/bin

.PHONY: run
run: hook
	@ cd $(WRANGLER_DIR) && wrangler dev --port 8787

.PHONY: deploy
deploy: test
	@ cd $(WRANGLER_DIR) && wrangler deploy

.PHONY: test
test: build
	@ clear && OCAMLRUNPARAM=b clj2js js test/test_spam.clj $(PRELUDE_PATH) > $(BIN_DIR)/test/test_spam.js
	@ clear && OCAMLRUNPARAM=b clj2js js test/test.clj $(PRELUDE_PATH) > $(BIN_DIR)/test/test.js
	@ clear && cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test_spam.js
	@ clear && cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test.js

.PHONY: build
build:
	@ mkdir -p $(BIN_DIR)/src && mkdir -p $(BIN_DIR)/test && mkdir -p $(BIN_DIR)/vendor/packages/effects && mkdir -p $(BIN_DIR)/vendor/prelude/js/src
	@ echo '{"type": "module"}' > $(BIN_DIR)/package.json
	@ clear && echo "effects.clj"   && OCAMLRUNPARAM=b clj2js js vendor/packages/effects/effects.clj $(PRELUDE_PATH) > $(BIN_DIR)/vendor/packages/effects/effects.js
	@ clear && echo "moderator.clj" && OCAMLRUNPARAM=b clj2js js src/moderator.clj $(PRELUDE_PATH) > $(BIN_DIR)/src/moderator.js
	@ clear && echo "main.clj"      && OCAMLRUNPARAM=b clj2js js src/main.clj $(PRELUDE_PATH) > $(BIN_DIR)/src/main.js

.PHONY: db
db:
	@ cd $(WRANGLER_DIR) && mkdir -p && wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=test_db.sql > bin/db_result.json

.PHONY: prod_db
prod_db:
	@ cd $(WRANGLER_DIR) && mkdir -p bin && wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --file=test_db.sql > bin/db_result.json

.PHONY: migrate
migrate:
	@ cd $(WRANGLER_DIR) && wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB --local --file=schema.sql

.PHONY: clean
clean:
	@ rm -rf $(BIN_DIR)

.PHONY: hook
hook:
	@NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source $(WRANGLER_DIR)/.dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&secret_token=$$TG_SECRET_TOKEN&url=$$NGROK_URL"
