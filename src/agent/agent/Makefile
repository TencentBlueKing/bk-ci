# =============================================================================
# Makefile — 兼容层，将 make 命令转发到 Taskfile.yml
#
# 所有构建逻辑已迁移至 Taskfile.yml（跨平台），此 Makefile 仅作为薄代理
# 保持流水线中 make xxx 命令平稳过渡。
#
# 如果环境中没有 task 命令，会自动通过 go install 安装。
#
# 用法不变:
#   make build_linux
#   make build_linux GO=go1.19 AGENT_VERSION=v1.0.0
#   make all
# =============================================================================

# ── 变量透传 ────────────────────────────────────────────────────────────────
GO              ?= go
AGENT_VERSION   ?= v0.0.0-beta.0
BUILD_OUT_TAG   ?= out

# ── 定位 task 二进制（绝对路径） ────────────────────────────────────────────
# 优先用 PATH 中的 task，找不到则用 GOPATH/bin/task（go install 的安装位置）
GOPATH_BIN := $(shell $(GO) env GOPATH 2>/dev/null)/bin
TASK       := $(or $(shell command -v task 2>/dev/null),$(GOPATH_BIN)/task)

# 公共参数：将 make 变量导出为 task 变量
TASK_VARS := GO=$(GO) AGENT_VERSION=$(AGENT_VERSION) BUILD_OUT_TAG=$(BUILD_OUT_TAG)

# ── _ensure_task: 检测 task，不存在则 go install ──────────────────────────
.PHONY: _ensure_task
_ensure_task:
	@if [ ! -x "$(TASK)" ]; then \
		echo "==> task not found, installing via: $(GO) install github.com/go-task/task/v3/cmd/task@latest"; \
		GOFLAGS= $(GO) install github.com/go-task/task/v3/cmd/task@latest; \
		echo "==> task installed to $(TASK)"; \
	fi

# ── 格式化 / 测试 / i18n ───────────────────────────────────────────────────
.PHONY: format test test-unit i18nfilegen clean

format: _ensure_task
	$(TASK_VARS) $(TASK) fmt

test: test-unit

test-unit: _ensure_task
	$(TASK_VARS) $(TASK) test

i18nfilegen: _ensure_task
	$(TASK_VARS) $(TASK) i18n

clean: _ensure_task
	$(TASK_VARS) $(TASK) clean

# ── 全平台 ──────────────────────────────────────────────────────────────────
.PHONY: all linux windows macos macos_no_cgo

all: _ensure_task
	$(TASK_VARS) $(TASK) build:all

linux: build_linux build_linux_arm64 build_linux_mips64 build_linux_loong64

windows: build_windows build_windows_386

macos: build_macos build_macos_arm64

macos_no_cgo: build_macos_no_cgo build_macos_arm64_no_cgo

# ── Linux ───────────────────────────────────────────────────────────────────
.PHONY: build_linux build_linux_arm64 build_linux_mips64 build_linux_loong64

build_linux: _ensure_task
	$(TASK_VARS) $(TASK) build:linux

build_linux_arm64: _ensure_task
	$(TASK_VARS) $(TASK) build:linux:arm64

build_linux_mips64: _ensure_task
	$(TASK_VARS) $(TASK) build:linux:mips64

build_linux_loong64: _ensure_task
	$(TASK_VARS) $(TASK) build:linux:loong64

# ── Windows ─────────────────────────────────────────────────────────────────
.PHONY: build_windows build_windows_386

build_windows: _ensure_task
	$(TASK_VARS) $(TASK) build:windows

build_windows_386: _ensure_task
	$(TASK_VARS) $(TASK) build:windows:386

# ── macOS (CGO) ─────────────────────────────────────────────────────────────
.PHONY: build_macos build_macos_arm64

build_macos: _ensure_task
	$(TASK_VARS) $(TASK) build:macos:cgo

build_macos_arm64: _ensure_task
	$(TASK_VARS) $(TASK) build:macos:arm64:cgo

# ── macOS (no CGO) ──────────────────────────────────────────────────────────
.PHONY: build_macos_no_cgo build_macos_arm64_no_cgo

build_macos_no_cgo: _ensure_task
	$(TASK_VARS) $(TASK) build:macos

build_macos_arm64_no_cgo: _ensure_task
	$(TASK_VARS) $(TASK) build:macos:arm64
