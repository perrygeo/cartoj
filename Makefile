_default:
	@echo "make test|dev|deps|clean|docs|compile|jar|install-local|release"

.PHONY: _default test dev build install clean docs view-docs install-jar release compile publish

SHADOW := npx shadow-cljs

test:
	npm run test

test-coverage:
	npm run test:coverage

test-starter:
	cd starter && $(SHADOW) release app && cd public && python -m http.server 8006

dev:
	$(SHADOW) watch dev

release-test:
	$(SHADOW) release test

deps:
	npm install

clean:
	rm -rf target dist public/js .shadow-cljs

compile:
	$(SHADOW) release dev

docs:
	clj -X:codox
	cp -r public/js/main.js docs/js/main.js
	cp -r public/css/* docs/css/
	cp -r public/data docs/data
	cp -r public/index.html docs/03-examples.html

view-docs: docs
	cd docs && python -m http.server 8005

# Install the JAR into ~/.m2 so another local project can consume it via :mvn/version.
install-jar:
	clj -T:build install

release: clean compile docs
	clj -T:build jar

# Push the JAR to Clojars. Requires CLOJARS_USERNAME and CLOJARS_PASSWORD
# (use a deploy token, not your account password).
publish:
	clj -T:build deploy
