WRANGLER_DIR := .github
BIN_DIR := .github/bin
SRC_DIRS := vendor/effects src test

.PHONY: test
test: build
	@ clear && OCAMLRUNPARAM=b clj2js js test/test_spam.clj > $(BIN_DIR)/test/test_spam.js
	@ clear && OCAMLRUNPARAM=b clj2js js test/test.clj > $(BIN_DIR)/test/test.js
	@ clear && cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test_spam.js
	@ clear && cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/test.js

.PHONY: run
run: hook
	@ cd $(WRANGLER_DIR) && wrangler dev --port 8787

.PHONY: deploy
deploy: test
	@ cd $(WRANGLER_DIR) && wrangler deploy

.PHONY: build
build:
	@ set -e; find $(SRC_DIRS) -name '*.clj' | while read clj_file; do \
		out_file=$(BIN_DIR)/$$(echo $$clj_file | sed 's|\.clj$$|.js|'); \
		mkdir -p $$(dirname $$out_file); \
		clj2js js $$clj_file > $$out_file; \
	  done

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
