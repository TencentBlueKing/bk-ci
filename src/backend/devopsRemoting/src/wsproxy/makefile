WORKSPACE_PROXY_LOCAL_REGISTRY=local
WORKSPACE_PROXY_LOCAL_IMAGE=wsproxy:lastet

VERSION=$(shell cd . && git log -1 --pretty=format:"%H")

# 和后台通信的秘钥，每次编译不同
SHA1KEY=2dE7xmMb86w1cxgV0=br

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

.PHONY: build.wsproxy.amd64
build.wsproxy.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -trimpath -ldflags "-X main.Version=$(VERSION)" \
 	-o ./bin/wsproxy-amd64 ./cmd

.PHONY: debug.wsproxy.amd64
debug.wsproxy.amd64: build.wsproxy.amd64
	docker build -f Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="true" \
	-t $(WORKSPACE_PROXY_LOCAL_REGISTRY)/$(WORKSPACE_PROXY_LOCAL_IMAGE)-debug .

.PHONY: release.wsproxy.amd64
release.wsproxy.amd64: build.wsproxy.amd64
	docker build -f Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="false" \
	-t $(WORKSPACE_PROXY_LOCAL_REGISTRY)/$(WORKSPACE_PROXY_LOCAL_IMAGE) .
