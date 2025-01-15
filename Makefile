WRANGLER_DIR := .github
BIN_DIR := .github/bin
SRC_DIRS := vendor/effects src test

.PHONY: test
test: build
	@ cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test_spam.js
	@ cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test.js

.PHONY: build
build:
	@ export OCAMLRUNPARAM=b && clj2js compile -target repl -src build.clj > .github/Makefile
	@ $(MAKE) -f .github/Makefile

.PHONY: run
run: hook
	@ cd $(WRANGLER_DIR) && wrangler dev --port 8787

.PHONY: deploy
deploy: test
	@ cd $(WRANGLER_DIR) && wrangler deploy

.PHONY: db
db:
	@ cd $(WRANGLER_DIR) && mkdir -p bin && wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB -y --command="SELECT content FROM log ORDER BY id DESC LIMIT 10" --json > bin/db_result.json
	@ test/tools/get_last_messages.clj > $(BIN_DIR)/db_result.pretty.json
	@ rm -f $(BIN_DIR)/db_result.json

.PHONY: prod_db
prod_db:
	@ cd $(WRANGLER_DIR) && mkdir -p bin && wrangler d1 execute ANROID_DECLARATIVE_BAN_BOT_DB -y --command="SELECT content FROM log ORDER BY id DESC LIMIT 10" --remote --json > bin/db_result.json
	@ test/tools/get_last_messages.clj > $(BIN_DIR)/db_result.pretty.json
	@ rm -f $(BIN_DIR)/db_result.json

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
