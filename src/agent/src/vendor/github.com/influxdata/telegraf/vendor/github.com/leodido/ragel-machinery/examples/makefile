SHELL := /bin/bash

.PHONY: all
all: newlines.go multiline.go

newlines.go: newlines.go.rl
	ragel -Z -G2 -e -o $@ $<
	@sed -i '/^\/\/line/d' $@
	$(MAKE) file=$@ snake2camel
	@gofmt -w -s $@

multiline.go: multiline.go.rl
	ragel -Z -G2 -e -o $@ $<
	@sed -i '/^\/\/line/d' $@
	$(MAKE) file=$@ snake2camel
	@gofmt -w -s $@

.PHONY: tests
tests: *_test.go
	go test -race -timeout 10s -v ./...

.PHONY: snake2camel
snake2camel:
	@awk -i inplace '{ \
	while ( match($$0, /(.*)([a-z]+[0-9]*)_([a-zA-Z0-9])(.*)/, cap) ) \
	$$0 = cap[1] cap[2] toupper(cap[3]) cap[4]; \
	print \
	}' $(file)