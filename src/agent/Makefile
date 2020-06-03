BINDIR := $(CURDIR)/bin
CMDDIR := $(CURDIR)/src/cmd

all: build_linux
build_linux:
	mkdir -p $(BINDIR)
	go env
	GO111MODULE=on go build -o $(BINDIR)/devopsDaemon_linux $(CMDDIR)/daemon
	GO111MODULE=on go build -o $(BINDIR)/devopsAgent_linux $(CMDDIR)/agent
	GO111MODULE=on go build -o $(BINDIR)/upgrader_linux $(CMDDIR)/upgrader
	ls -la $(BINDIR)
build_macos:
	mkdir -p $(BINDIR)
	go env
	GO111MODULE=on go build -o $(BINDIR)/devopsDaemon_macos $(CMDDIR)/daemon
	GO111MODULE=on go build -o $(BINDIR)/devopsAgent_macos $(CMDDIR)/agent
	GO111MODULE=on go build -o $(BINDIR)/upgrader_macos $(CMDDIR)/upgrader
	ls -la $(BINDIR)
clean:
	mkdir -p $(BINDIR)
	rm -f $(BINDIR)/*