WRANGLER_DIR := .github
BIN_DIR := .github/bin
SRC_DIRS := vendor/effects src test

.PHONY: test
test: build
	@ printf '{"type":"module","devDependencies":{"wrangler":"^4.0.0"}}' > $(BIN_DIR)/package.json && \
		cd $(BIN_DIR) && yarn
	@ cd $(BIN_DIR) && node --test test/main_test.js
	@ cd $(BIN_DIR) && node --test test/other_samples_test.js
	@ cd $(BIN_DIR) && node --test test/handler/join_node_test.js

.PHONY: update-golden
update-golden: build
	@ printf '{"type":"module","devDependencies":{"wrangler":"^4.0.0"}}' > $(BIN_DIR)/package.json && \
		cd $(BIN_DIR) && yarn
	@ cd $(BIN_DIR) && node test/main_test.js update
	@ cd $(BIN_DIR) && node test/other_samples_test.js update

.PHONY: build
build:
	@ mkdir -p $(BIN_DIR)
	@ ly2k compile -target eval -src build.clj > $(BIN_DIR)/Makefile
	@ $(MAKE) -f $(BIN_DIR)/Makefile

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

.PHONY: hook-prod
hook-prod:
	@ curl --fail --show-error --request POST "https://api.telegram.org/bot$(TOKEN)/setWebhook" \
		--form "max_connections=1" \
		--form "drop_pending_updates=true" \
		--form "secret_token=$(SECRET)" \
		--form "url=https://declarativebanbot.y2kdev.workers.dev/telegram-bot"
