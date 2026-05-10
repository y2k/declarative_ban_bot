WRANGLER_DIR := .github
OUT_DIR := .github/bin
SRC_DIRS := vendor/effects src test

.PHONY: test
test: build
	@ printf '{"type":"module","devDependencies":{"wrangler":"^4.0.0"}}' > $(OUT_DIR)/package.json && \
		cd $(OUT_DIR) && yarn
	@ cd ${OUT_DIR} && node --test --test-concurrency=1 "test/**/*_test.js"

.PHONY: build
build:
	@ mkdir -p $(OUT_DIR)
	@ ly2k compile -target eval -src build.clj > $(OUT_DIR)/Makefile
	@ $(MAKE) -f $(OUT_DIR)/Makefile

.PHONY: clean
clean:
	@ rm -rf $(OUT_DIR)/src
	@ rm -rf $(OUT_DIR)/test

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
