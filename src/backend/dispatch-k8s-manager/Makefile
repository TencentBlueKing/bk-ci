LOCAL_REGISTRY=bkci
LOCAL_IMAGE=kubernetes-manager:0.0.31

CONFIG_DIR=/data/workspace/kubernetes-manager/config
OUT_DIR=/data/workspace/kubernetes-manager/out

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

.PHONY: build.amd64
build.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -ldflags "-X main.configDir=$(CONFIG_DIR) -X main.outDir=$(OUT_DIR) -X main.debug=true" \
 	-o bin/kubernetes-manager-amd64 ./cmd/apiserver/apiserver.go

.PHONY: build.arm64v8
build.arm64v8: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 \
	go build -ldflags "-X main.configDir=$(CONFIG_DIR) -X main.outDir=$(OUT_DIR) -X main.debug=true" \
	-o bin/kubernetes-manager-arm64v8 ./cmd/apiserver/apiserver.go

.PHONY: release.amd64
release.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -ldflags "-X main.configDir=$(CONFIG_DIR) -X main.outDir=$(OUT_DIR) -X main.debug=false" \
 	-o bin/kubernetes-manager-amd64 ./cmd/apiserver/apiserver.go

.PHONY: release.arm64v8
release.arm64v8: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 \
	go build -ldflags "-X main.configDir=$(CONFIG_DIR) -X main.outDir=$(OUT_DIR) -X main.debug=false" \
	-o bin/kubernetes-manager-arm64v8 ./cmd/apiserver/apiserver.go

.PHONY: image.amd64
image.amd64: release.amd64
	docker build -f ./docker/Dockerfile --build-arg ARCH="amd64" -t $(LOCAL_REGISTRY)/$(LOCAL_IMAGE) .

.PHONY: image.arm64v8
image.arm64v8: release.arm64v8
	docker build -f ./docker/Dockerfile --build-arg ARCH="arm64v8" -t $(LOCAL_REGISTRY)/$(LOCAL_IMAGE) .

.PHONY: clean
clean:
	rm -rf ./bin
	rm -rf ./bin
