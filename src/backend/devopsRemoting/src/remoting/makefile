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
	rm -rf ./bin

.PHONY: build.devopsRemoting
build.devopsRemoting: clean test
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build -trimpath -ldflags "-X main.Version=$(VERSION) -X remoting/pkg/thirdpartapi.Sha1key=$(SHA1KEY)" \
	-o ./bin/devopsRemoting ./cmd

.PHONY: build.openssh
build.openssh: 
	./openssh/disassemble.sh
