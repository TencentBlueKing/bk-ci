REMOTING_DEBUG_IMAGE=devops/remoting:debug

VERSION=$(shell cd . && git log -1 --pretty=format:"%H")

# 和后台通信的秘钥，每次编译不同
SHA1KEY=2dE7xmMb86w1cxgV0=br

test: test-unit

.PHONY: test-unit
test-unit:
	@echo
	@echo "==> Running unit tests <=="
	GO111MODULE=on go test -run . ./... 

.PHONY: build.devopsRemoting
build.devopsRemoting: clean test
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build -trimpath -ldflags "-X main.Version=$(VERSION) -X devopsRemoting/src/pkg/remoting/thirdpartapi.Sha1key=$(SHA1KEY)" \
	-o bin/devopsRemoting ./src/cmd/devopsRemoting

.PHONY: build.vscodehelper
build.vscodehelper: clean test
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build -trimpath -ldflags "-X main.Version=$(VERSION)" \
	-o bin/vscodehelper ./ide/vscode/helper/main.go

.PHONY: build.vscode.desktop.status
build.vscode.desktop.status: clean test
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build \
	-o bin/vscode-desktop-status ./ide/vscode-desktop/status/main.go	

.PHONY: debug.ide.vscode.image
debug.ide.vscode.image: build.vscodehelper build.vscode.desktop.status build.devopsRemoting
	docker build -f docker/remoting/Dockerfile.debug -t $(REMOTING_DEBUG_IMAGE) .

.PHONY: clean
clean:
	rm -rf ./bin
	rm -rf ./bin

.PHONY: build.openssh
build.openssh: 
	./openssh/disassemble.sh

WORKSPACE_PROXY_LOCAL_REGISTRY=mirrors.tencent.com
WORKSPACE_PROXY_LOCAL_IMAGE=ruotiantang/wsproxy-demo:0.0.31

.PHONY: build.wsproxy.amd64
build.wsproxy.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -trimpath -ldflags "-X main.Version=$(VERSION)" \
 	-o bin/wsproxy-amd64 ./src/cmd/wsproxy

.PHONY: debug.wsproxy.amd64
debug.wsproxy.amd64: build.wsproxy.amd64
	docker build -f ./docker/wsproxy/Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="true" \
	-t $(WORKSPACE_PROXY_LOCAL_REGISTRY)/$(WORKSPACE_PROXY_LOCAL_IMAGE)-debug .

.PHONY: release.wsproxy.amd64
release.wsproxy.amd64: build.wsproxy.amd64
	docker build -f ./docker/wsproxy/Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="false" \
	-t $(WORKSPACE_PROXY_LOCAL_REGISTRY)/$(WORKSPACE_PROXY_LOCAL_IMAGE) .
