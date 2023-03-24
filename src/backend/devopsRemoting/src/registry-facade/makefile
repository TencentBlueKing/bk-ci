LOCAL_REGISTRY=mirrors.tencent.com
LOCAL_IMAGE=ruotiantang/wsproxy-demo:0.0.31

VERSION=$(shell cd . && git log -1 --pretty=format:"%H")

format: 
	find ./ -name "*.go" | xargs gofmt -w

test: test-unit

.PHONY: test-unit
test-unit:
	@echo
	@echo "==> Running unit tests <=="
	GO111MODULE=on go test -run . ./... 

.PHONY: clean
clean:
	rm -rf ./bin

.PHONY: build
build: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -trimpath -ldflags "-X cmd.Version=$(VERSION)" \
 	-o ./bin/registry-amd64 main.go