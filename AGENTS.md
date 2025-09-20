# Repository Guidelines

## Project Structure & Module Organization
- `src/` holds the Cloudflare Worker logic written in ly2k-flavored ClojureScript (`main.clj` entrypoint, `effects.clj` wrappers, `moderator.clj` heuristics).
- `test/` stores promise-driven regression tests plus fixtures in `samples/` and helper scripts in `tools/`.
- `.github/` keeps deployment assets: generated JS in `bin/`, Wrangler config, and the D1 schema. Treat `bin/` as build artifacts—regenerate instead of editing by hand.

## Build, Test, and Development Commands
- `make build` transpiles `src/*.clj` and `test/*.clj` to `.github/bin/**` via `ly2k`.
- `make test` runs the Node-based suites (`bin/test/test_spam.js`, `bin/test/test.js`). Provide `TG_TOKEN` and `TG_SECRET_TOKEN` in `.github/.dev.vars` before running.
- `make run` launches `wrangler dev` on port 8787; `make hook` refreshes the Telegram webhook after ngrok exposes a tunnel.
- `make deploy` publishes to Cloudflare once tests pass. Use it only after review.
- `make clean` removes generated binaries; re-run `make build` afterward.

## Coding Style & Naming Conventions
- Prefer two-space indentation and align threaded forms (`->`, `->>`) for readability.
- Keep functions and locals in `snake_case`; reserve `SCREAMING_SNAKE_CASE` for constants, mirroring current modules.
- Place pure helpers in `src/moderator.clj`; route all side-effects through `effects.clj` shims so they stay mockable in tests.
- Rely on the build step to vend dependencies under `src/vendor`; do not commit local symlinks or generated JS.

## Testing Guidelines
- Tests compare serialized interaction logs under `test/samples/output/`; hash-based filenames must remain stable. Update expectations by rerunning tests and reviewing the diff.
- Name new suites `test_*.clj` and require the worker via relative paths as done in `test/test_spam.clj`.
- Run `make test` before every push; CI assumes a clean git tree and no outstanding `FIXME` failures.

## Commit & Pull Request Guidelines
- Commit messages stay short and imperative (`update rules`, `add new rule`) and focus on a single concern.
- PRs should capture behavior changes, list manual verification steps (e.g., `make test`, Wrangler smoke run), and link related issues or chats.
- Include screenshots or log snippets when touching moderation flows or D1 queries, and double-check that secrets remain confined to `.dev.vars`.
