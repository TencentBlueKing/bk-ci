BINDIR := $(CURDIR)/bin
CMDDIR := $(CURDIR)/src/cmd
BuildTime := $(shell date '+%Y-%m-%d.%H:%M:%S%Z')
GitCommit := $(shell git rev-parse HEAD)
AGENT_VERSION=v1.9.18
BUILD_FLAGS := -ldflags="-w -s -X github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config.BuildTime=$(BuildTime) \
-X github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config.GitCommit=$(GitCommit) -X github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config.AgentVersion=$(AGENT_VERSION)"

# 编译外部出包版本时需要设置当前变量为out
BUILD_OUT_TAG=inner

format: 
	find ./ -name "*.go" | xargs gofmt -w

test: test-unit

.PHONY: test-unit
test-unit:
	@echo
	@echo "==> Running unit tests <=="
	GO111MODULE=on go test -tags=${BUILD_OUT_TAG} -run . ./...

# 相对于代码生成器的位置
I18N_DIR := "../../../../../support-files/i18n/agent/"

i18nfilegen: 
	I18N_DIR=${I18N_DIR} go generate -x src/pkg/i18n/i18n.go

all: linux windows macos_no_cgo

linux: build_linux build_linux_arm64 build_linux_mips64

windows: build_windows

macos: build_macos build_macos_arm64

macos_no_cgo: build_macos_no_cgo build_macos_arm64_no_cgo

build_linux: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_linux $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_linux $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_linux $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_linux $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_linux_arm64: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_linux_arm64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_linux_arm64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_linux_arm64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_linux_arm64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_linux_mips64: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_linux_mips64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_linux_mips64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_linux_mips64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_linux_mips64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

# Telegraf 的 cpu和diskio 插件采集使用的 shirou 包需要开启cgo才可以在darwin情况下采集成功
build_macos: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_macos $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_macos $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_macos $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_macos $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_macos_arm64: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_macos_arm64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_macos_arm64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_macos_arm64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_macos_arm64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

# 方便统一交叉编译，这里会提供不开启cgo的macos编译，但是其监控的 cpu和diskio 会没有数据
build_macos_no_cgo: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_macos $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_macos $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_macos $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=amd64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_macos $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_macos_arm64_no_cgo: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon_macos_arm64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent_macos_arm64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader_macos_arm64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=darwin GOARCH=arm64 go build ${BUILD_FLAGS} -o $(BINDIR)/installer_macos_arm64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

build_windows: test clean i18nfilegen
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build ${BUILD_FLAGS} -o $(BINDIR)/devopsDaemon.exe $(CMDDIR)/daemon/main_win.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build -tags=${BUILD_OUT_TAG} ${BUILD_FLAGS} -o $(BINDIR)/devopsAgent.exe $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build ${BUILD_FLAGS} -o $(BINDIR)/upgrader.exe $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build ${BUILD_FLAGS} -o $(BINDIR)/installer.exe $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

clean:
	mkdir -p $(BINDIR)
	rm -f $(BINDIR)/*
