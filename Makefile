_default:
	@echo "make test|dev|install|clean|doc"
	@echo "Clojars: make jar|install-local|deploy"

.PHONY: _default test compile-test dev build install clean doc jar install-jar deploy

SHADOW := npx shadow-cljs

compile-test:
	$(SHADOW) compile test

test: compile-test
	node target/test/test.js

dev:
	$(SHADOW) watch dev

# release build of the shadow-cljs :test target
release-test:
	$(SHADOW) release test

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
install-jar:
	clj -T:build install

# Push the JAR to Clojars. Requires CLOJARS_USERNAME and CLOJARS_PASSWORD
# (use a deploy token, not your account password).
deploy:
	clj -T:build deploy
