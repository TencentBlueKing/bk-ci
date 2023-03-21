VERSION=$(shell cd . && git log -1 --pretty=format:"%H")

.PHONY: build.vscodehelper
build.vscodehelper: clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build -trimpath -ldflags "-X main.Version=$(VERSION)" \
	-o ./bin/vscodehelper main.go

.PHONY: clean
clean:
	rm -rf ./bin
