_default:
	@echo "make test|compile|dev|build|install|clean|doc"
	@echo "Clojars: make jar|install-local|deploy"

.PHONY: _default test compile dev build install clean doc jar install-local deploy

SHADOW := npx shadow-cljs

compile:
	$(SHADOW) compile test

test: compile
	node target/test/test.js

dev:
	$(SHADOW) watch dev

# Shadow-cljs :npm-module bundle (optional, for JS/TS consumers; separate from Clojars).
build:
	$(SHADOW) release lib

install:
	npm install

clean:
	rm -rf target dist public/js .shadow-cljs

doc:
	clj -X:codox && open target/doc/index.html

# --- Clojars distribution ---------------------------------------------------

# Build the source-only JAR (target/cartoj-<version>.jar) via tools.build.
jar:
	clj -T:build jar

# Install the JAR into ~/.m2 so another local project can consume it via :mvn/version.
install-local:
	clj -T:build install

# Push the JAR to Clojars. Requires CLOJARS_USERNAME and CLOJARS_PASSWORD
# (use a deploy token, not your account password).
deploy:
	clj -T:build deploy
