BINDIR := $(CURDIR)/bin
CMDDIR := $(CURDIR)/cmd
BuildTime := $(shell date '+%Y-%m-%d.%H:%M:%S%Z')
GitCommit := $(shell git rev-parse HEAD)
AGENT_VERSION=v1.12.0-beta.36
BUILD_FLAGS := -ldflags="-w -s -X github.com/TencentBlueKing/bk-ci/agent/src/pkg/config.BuildTime=$(BuildTime) \
-X github.com/TencentBlueKing/bk-ci/agent/src/pkg/config.GitCommit=$(GitCommit) -X github.com/TencentBlueKing/bk-ci/agent/src/pkg/config.AgentVersion=$(AGENT_VERSION)"

format: 
	find ./ -name "*.go" | xargs gofmt -w

test: test-unit

.PHONY: test-unit
test-unit:
	@echo
	@echo "==> Running unit tests <=="
	GO111MODULE=on go test -tags=${BUILD_OUT_TAG} -run . ./...

# 相对于代码生成器的位置
I18N_DIR := "../../../../../support-files/i18n/agent-slim/"

i18nfilegen: 
	I18N_DIR=${I18N_DIR} go generate -x pkg/i18n/i18n.go

all: linux

linux: build_linux build_linux_arm64 

build_linux: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/agentslim_linux $(CMDDIR)/slim.go
	ls -la $(BINDIR)
build_linux_arm64: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/agentslim_linux_arm64 $(CMDDIR)/slim.go
	ls -la $(BINDIR)

clean:
	mkdir -p $(BINDIR)
	rm -f $(BINDIR)/*
