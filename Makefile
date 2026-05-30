_default:
	@echo "make test|compile|dev|build|install|clean"

.PHONY: _default test compile dev build install clean

SHADOW := npx shadow-cljs

compile:
	$(SHADOW) compile test

test: compile
	node target/test/test.js

dev:
	$(SHADOW) watch dev

build:
	$(SHADOW) release lib

install:
	npm install

clean:
	rm -rf target dist public/js .shadow-cljs

doc:
	clj -X:codox && open target/doc/index.html

