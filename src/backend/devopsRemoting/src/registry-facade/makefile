LOCAL_REGISTRY=local
LOCAL_IMAGE=registry-facade:lastet

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
build.registry.amd64: test clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
 	go build -trimpath -ldflags "-X registry-facade/cmd.Version=$(VERSION)" \
 	-o ./bin/registry-amd64 main.go

.PHONY: debug.registry.amd64
debug.registry.amd64: build.registry.amd64
	docker build -f Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="true" \
	-t $(LOCAL_REGISTRY)/$(LOCAL_IMAGE)-debug .

.PHONY: release.registry.amd64
release.registry.amd64: build.registry.amd64
	docker build -f Dockerfile --build-arg ARCH="amd64" --build-arg DEBUG="false" \
	-t $(LOCAL_REGISTRY)/$(LOCAL_IMAGE) .
