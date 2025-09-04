.PHONY: clean
clean:
	rm -rf ./bin

.PHONY: build.vscode.desktop.status
build.vscode.desktop.status: clean
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 \
	go build \
	-o ./bin/vscode-desktop-status main.go	
