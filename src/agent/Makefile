BINDIR := $(CURDIR)/bin
CMDDIR := $(CURDIR)/src/cmd

all: build_linux build_linux_arm64 build_linux_mips64 build_macos build_macos_arm64 build_windows

linux: build_linux build_linux_arm64 build_linux_mips64

windows: build_windows

build_linux:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o $(BINDIR)/devopsDaemon_linux $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o $(BINDIR)/devopsAgent_linux $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o $(BINDIR)/upgrader_linux $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build -o $(BINDIR)/installer_linux $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_linux_arm64:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o $(BINDIR)/devopsDaemon_linux_arm64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o $(BINDIR)/devopsAgent_linux_arm64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o $(BINDIR)/upgrader_linux_arm64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=arm64 go build -o $(BINDIR)/installer_linux_arm64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_linux_mips64:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build -o $(BINDIR)/devopsDaemon_linux_mips64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build -o $(BINDIR)/devopsAgent_linux_mips64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build -o $(BINDIR)/upgrader_linux_mips64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=linux GOARCH=mips64 go build -o $(BINDIR)/installer_linux_mips64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

# Telegraf 的 cpu和diskio 插件采集使用的 shirou 包需要开启cgo才可以在darwin情况下采集成功
build_macos:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -o $(BINDIR)/devopsDaemon_macos $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -o $(BINDIR)/devopsAgent_macos $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -o $(BINDIR)/upgrader_macos $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=amd64 go build -o $(BINDIR)/installer_macos $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)
build_macos_arm64:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -o $(BINDIR)/devopsDaemon_macos_arm64 $(CMDDIR)/daemon/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -o $(BINDIR)/devopsAgent_macos_arm64 $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -o $(BINDIR)/upgrader_macos_arm64 $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=1 GOOS=darwin GOARCH=arm64 go build -o $(BINDIR)/installer_macos_arm64 $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

build_windows:
	mkdir -p $(BINDIR)
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build -o $(BINDIR)/devopsDaemon.exe $(CMDDIR)/daemon/main_win.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build -o $(BINDIR)/devopsAgent.exe $(CMDDIR)/agent/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build -o $(BINDIR)/upgrader.exe $(CMDDIR)/upgrader/main.go
	GO111MODULE=on CGO_ENABLED=0 GOOS=windows GOARCH=386 go build -o $(BINDIR)/installer.exe $(CMDDIR)/installer/main.go
	ls -la $(BINDIR)

clean:
	mkdir -p $(BINDIR)
	rm -f $(BINDIR)/*
