WRANGLER_DIR := .github
BIN_DIR := .github/bin
SRC_DIRS := vendor/effects src test

.PHONY: test
test: build
	@ cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/main_test.js test

.PHONY: update-golden
update-golden: build
	@ cd $(WRANGLER_DIR) && node --env-file=.dev.vars bin/test/main_test.js update

.PHONY: build
build:
	@ mkdir -p $(BIN_DIR)
	@ ly2k compile -target eval -src build.clj > $(BIN_DIR)/Makefile
	@ $(MAKE) -f $(BIN_DIR)/Makefile
	@ ly2k generate -target js > $(BIN_DIR)/src/prelude.js
	@ ly2k generate -target js > $(BIN_DIR)/test/prelude.js

.PHONY: clean
clean:
	@ rm -rf $(BIN_DIR)

# Run

.PHONY: run
run: build hook
	@ cd $(WRANGLER_DIR) && wrangler dev --port 8787

.PHONY: deploy
deploy: test
	@ cd $(WRANGLER_DIR) && wrangler deploy

# Tooling

.PHONY: hook
hook:
	NGROK_API="http://localhost:4040/api/tunnels" ; \
	NGROK_URL=$$(curl -s $$NGROK_API | grep -o '"public_url":"[^"]*' | grep -o 'http[^"]*') ; \
	source $(WRANGLER_DIR)/.dev.vars ; \
	curl "https://api.telegram.org/bot$$TG_TOKEN/setWebhook?max_connections=1&drop_pending_updates=true&secret_token=$$TG_SECRET_TOKEN&url=$$NGROK_URL"
