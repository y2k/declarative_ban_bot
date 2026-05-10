# AGENTS.md - Declarative Ban Bot

Guidance for AI coding agents working on this Telegram spam moderation bot.

## Project Overview

Clojure (Lisp dialect) compiled to JavaScript via `ly2k` transpiler, running on Cloudflare Workers.

| File | Purpose |
|------|---------|
| `src/main.clj` | Entry point, request routing, effect handlers |
| `src/telegram.clj` | Telegram API wrapper |
| `src/moderator.clj` | Spam detection logic |
| `src/handler/join.clj` | Join request/captcha handler |
| `src/handler/report.clj` | Spam report handler |

Effect system is provided by external dependency `effects-promise` (v0.3.0).

## Build & Test Commands

```bash
make build      # Compiles Clojure to JavaScript
make test       # Build and run all tests
make clean      # Removes build artifacts (.github/bin/)
make db         # Query local D1 database
make migrate    # Run database migrations locally
make hook       # Set up ngrok webhook for dev
```

**Single test**: Not supported. Uses golden file comparison (`test/resources/sample/`).

**Update golden files**: In `test/main_test.clj`, uncomment `((update-golden) {})` and
comment out `((run-all-tests) {})`, then `make test`.

**No linter configured** - follow style guidelines below.

## Code Style

- Prefer a small number of clear functions over many tiny helpers.
- Keep comments rare: add them only when the code is not self-explanatory.
- Be concise in code, commit messages, and user-facing explanations.

### Clojure (ly2k dialect)

Supports: `defn`, `let`, `if`, `cond`, `->`, `->>`, JS interop via `.method`, optional
chaining `?.`, keywords `:keyword`, atoms with `atom`/`swap!`/`deref`.

### Namespace

```clojure
(ns handler.report
  (:require [effects-promise :as e]
            [moderator :as m]
            [telegram :as tg]))
```

One namespace per file. Path matches namespace: `handler.report` -> `src/handler/report.clj`.
Use short aliases: `fx`, `e`, `m`, `tg`.

### Naming

| Type | Convention | Example |
|------|------------|---------|
| Functions | `snake_case` | `send_message`, `check_is_spam` |
| Private functions | `defn-` kebab-case | `defn- report-command?` |
| Constants | `UPPER_SNAKE_CASE` | `LIMIT_SPAM_OLD_SEC` |
| Private constants | `def-` | `def- admin-chat-id` |
| Predicates | End with `?` | `report-command?` |
| Local bindings | `kebab-case` | `reply-text`, `chat-id` |

### Patterns

Public functions at file end; private helpers first:

```clojure
(defn- build-url [chat-name message-id]
  (str "https://t.me/" chat-name "/" message-id))

(defn handle [cofx update]  ;; Public API
  (if-let [text update?.message?.text]
    (e/batch [(effect1) (effect2)])
    (e/pure nil)))
```

### Effect System

```clojure
(defn fetch [url decoder props]
  (fn [w] ((:fetch w) {:url url :decoder decoder :props props})))

(e/batch [(effect1) (effect2)])  ; Batch effects
(e/pure value)                   ; Wrap pure values
```

### Error Handling & Control Flow

```clojure
(FIXME "Error message")                    ; Throw error
(.catch promise-chain console.error)       ; Promise errors
update?.message?.chat?.id                  ; Optional chaining

(if-let [text message?.text]               ; Safe binding
  (process text)
  (e/pure nil))

(cond
  condition1 result1
  condition2 result2
  :else default)
```

### JS Interop

```clojure
(.toLowerCase text)                        ; Method call
(.includes message "keyword")
request.headers                            ; Property access

(-> text (.toLowerCase) (.replaceAll "a" "b"))  ; Threading
(-> (fetch url) (.then handler) (.catch console.error))
```

## Project Structure

```
src/                    # Clojure source
  handler/              # Request handlers
test/                   # Tests
  resources/sample/     # Golden test data (in/out JSON)
.github/                # Cloudflare Workers
  bin/                  # Build artifacts
  wrangler.toml         # Config
  schema.sql            # DB schema
build.clj               # ly2k build config
```

## Testing

Golden file comparison:
1. Input: `test/resources/sample/in/*.json` or `*.txt`
2. Expected: `test/resources/sample/out/*.json`
3. Tests mock fetch handler and record API calls

## Environment Variables

Required in `.github/.dev.vars`:
- `TG_TOKEN` - Telegram Bot API token
- `TG_SECRET_TOKEN` - Webhook secret

## Common Tasks

**Add spam keyword** in `src/moderator.clj`:
```clojure
(.includes message "newkeyword")
```

**Add Telegram API call**:
```clojure
(tg/send_message :methodName {:chat_id id :text "msg"})
```

**Add new handler**: Create `src/handler/new.clj` with `handle` fn, add to `e/batch` in `main.clj`.
