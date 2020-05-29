BINDIR := $(CURDIR)/bin
CMDDIR := $(CURDIR)/src/cmd

all: build
build:
	mkdir -p $(BINDIR)
	GO111MODULE=on go build -o $(BINDIR)/devopsDaemon $(CMDDIR)/daemon
	GO111MODULE=on go build -o $(BINDIR)/devopsAgent $(CMDDIR)/agent
	GO111MODULE=on go build -o $(BINDIR)/upgrader $(CMDDIR)/upgrader
clean:
	mkdir -p $(BINDIR)
	rm -f $(BINDIR)/devopsDaemon
	rm -f $(BINDIR)/devopsAgent
	rm -f $(BINDIR)/upgrader