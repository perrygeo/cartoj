_default:
	@echo "Available targets:"
	@echo "  dev            Start shadow-cljs dev watch and REPL"
	@echo "  clean          Remove build artifacts"
	@echo "  compile        Release build of the CLJS :dev target"
	@echo "  deps           Install npm dependencies"
	@echo "  docs           Generate codox docs and copy assets"
	@echo "  install-jar    Install the JAR into ~/.m2"
	@echo "  publish        Deploy the JAR to Clojars"
	@echo "  release        Clean, compile, docs, and build the JAR"
	@echo "  starter        Build and serve the starter app, manual test"
	@echo "  test           Run the unit test suite"
	@echo "  test-coverage  Run tests with coverage"
	@echo "  test-e2e       Run end-to-end tests"
	@echo "  test-advanced  Smoke test, does it build with :advanced compilation"
	@echo "  view-docs      Build docs and serve them locally"

.PHONY: _default test test-e2e dev build install clean docs view-docs install-jar release compile publish check-version

SHADOW := npx shadow-cljs

dev:
	$(SHADOW) watch dev

test:
	npm run test

check-version:
	node scripts/check-version.js

test-lint:
	clj-kondo --repro --cache false --lint src test dev
	cljfmt check src test dev

test-coverage:
	npm run test:coverage

test-e2e:
	npm run test:e2e

test-advanced:
	$(SHADOW) release test

test-all: check-version test-lint test-advanced test-coverage test-e2e

starter:
	cd starter && $(SHADOW) release app && cd public && python -m http.server 8006

deps:
	npm install

clean:
	rm -rf target dist public/js .shadow-cljs

compile:
	$(SHADOW) release dev

docs:
	clojure -X:codox
	cp -r public/js/main.js docs/js/main.js
	cp -r public/css/* docs/css/
	cp -r public/data docs/data
	cp -r public/index.html docs/03-examples.html

view-docs: docs
	cd docs && python -m http.server 8005

# Install the JAR into ~/.m2 so another local project can consume it via :mvn/version.
install-jar:
	clojure -T:build install

release: clean compile docs
	clojure -T:build jar

# Push the JAR to Clojars. Requires CLOJARS_USERNAME and CLOJARS_PASSWORD
# (use a deploy token, not your account password).
publish:
	clojure -T:build deploy
