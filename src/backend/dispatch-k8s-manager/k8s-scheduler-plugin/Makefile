ARCHS = amd64 arm64
COMMONENVVAR=GOOS=$(shell uname -s | tr A-Z a-z)
BUILDENVVAR=CGO_ENABLED=0

LOCAL_REGISTRY=localhost:5000/scheduler-plugins
LOCAL_IMAGE=kube-scheduler:latest
LOCAL_CONTROLLER_IMAGE=controller:latest

# RELEASE_REGISTRY is the container registry to push
# into. The default is to push to the staging
# registry, not production(k8s.gcr.io).
RELEASE_REGISTRY?=1016394505/bkdevops-scheduler-plugin
RELEASE_VERSION?=v$(shell date +%Y%m%d)-$(shell git describe --tags --match "v*")
RELEASE_IMAGE:=kube-scheduler:$(RELEASE_VERSION)
RELEASE_CONTROLLER_IMAGE:=controller:$(RELEASE_VERSION)

# VERSION is the scheduler's version
#
# The RELEASE_VERSION variable can have one of two formats:
# v20201009-v0.18.800-46-g939c1c0 - automated build for a commit(not a tag) and also a local build
# v20200521-v0.18.800             - automated build for a tag
VERSION=v0.20.11

NAME=bkdevops-scheduler-plugin

IMAGE_TAG=$(shell date +%s)

# go option
PKG        := ./...
TESTS      := .
TESTFLAGS  :=
GOFLAGS    :=

# gin
export GIN_MODE=release

test: test-unit

.PHONY: test-unit
test-unit:
	@echo
	@echo "==> Running unit tests <=="
	GO111MODULE=on go test $(GOFLAGS) -run $(TESTS) $(PKG) $(TESTFLAGS)

.PHONY: build
build.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build \
 	-o bin/$(NAME)-amd64 ./cmd/scheduler/main.go

.PHONY: build.arm64v8
build.arm64v8: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 \
	go build \
	-o bin/$(NAME)-arm64v8 ./cmd/scheduler/main.go

.PHONY: build-scheduler
build-scheduler:
	$(COMMONENVVAR) $(BUILDENVVAR) go build -ldflags '-X k8s.io/component-base/version.gitVersion=$(VERSION) -w' -o bin/kube-scheduler cmd/scheduler/main.go

.PHONY: build-scheduler.amd64
build-scheduler.amd64:
	$(COMMONENVVAR) $(BUILDENVVAR) GOARCH=amd64 go build -ldflags '-X k8s.io/component-base/version.gitVersion=$(VERSION) -w' -o bin/kube-scheduler cmd/scheduler/main.go

.PHONY: build-scheduler.arm64v8
build-scheduler.arm64v8:
	GOOS=linux $(BUILDENVVAR) GOARCH=arm64 go build -ldflags '-X k8s.io/component-base/version.gitVersion=$(VERSION) -w' -o bin/kube-scheduler cmd/scheduler/main.go


.PHONY: local-image
local-image: clean
	docker build -f ./build/scheduler/Dockerfile --build-arg ARCH="amd64" --build-arg RELEASE_VERSION="$(RELEASE_VERSION)" -t $(LOCAL_REGISTRY)/$(LOCAL_IMAGE) .

.PHONY: release-image.amd64
release-image.amd64: clean
	docker build -f ./build/scheduler/Dockerfile --build-arg ARCH="amd64" --build-arg RELEASE_VERSION="$(RELEASE_VERSION)" -t $(RELEASE_REGISTRY):$(IMAGE_TAG) .

.PHONY: release-image.arm64v8
release-image.arm64v8: clean
	docker build -f ./build/scheduler/Dockerfile --build-arg ARCH="arm64v8" --build-arg RELEASE_VERSION="$(RELEASE_VERSION)" -t $(RELEASE_REGISTRY)/$(RELEASE_IMAGE)-arm64 .

.PHONY: clean
clean:
	rm -rf ./bin
