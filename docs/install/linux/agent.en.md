# Agent Deployment

Agent consists of backend worker-agent.jar and agent (Go) two parts.

## Agent (Go) Code Description

Agent is written in Golang. Currently it supports Linux, Windows and macOS three operating systems. No compilation verification has been performed on other operating systems.

For the convenience of compilation, all the compilation dependencies have been put in the vendor folder of the source code.

## Compile Agent to Create Installation Packages

- It is recommended to use Golang 1.12 for compilation.
- It is not recommended to use cross compilation to create agent executables.
- Compile on Linux: build_linux.sh
- Compile on Windows: build_windows.bat
- Compile on macOS: build_macos.sh

- The outputs are stored in the following files (installation packages) in the agent/bin directory:

```
    |- devopsAgent.exe
    |- devopsAgent_linux
    |- devopsAgent_macos
    |- devopsDaemon.exe
    |- devopsDaemon_linux
    |- devopsDaemon_macos
    |- upgrader.exe
    |- upgrader_linux
    |- upgrader_linux
```

## Deploy Agent Installation Package

In the /data/bkee/bkci root directory:

- Copy the support-files/agent-package directory in the source code package to /data/bkee/ci/agent-package.
- This path is specified in /data/bkee/etc/ci/application-environment.yml. Please check if the paths are the same.
- Please comply with the following requirements to store the corresponding installation packages.
  - Agent Installation Package Directory Guide (agent-package)

```
|- agent-package # Location of Agent installation package 
  |- config   
    |- .agent.properties   # Agent configuration file
  |- jar
    |- worker-agent.jar  #  release/worker-agent.jar compiled from the backend/worker submodule
  |- jre  
    |- linux
      |- jre.zip   # JRE 1.8 for Linux. bcprov-jdk16-1.46.jar should be added to lib/ext.
    |- windows
      |- jre.zip   # JRE 1.8 for Windows. bcprov-jdk16-1.46.jar should be added to lib/ext.
    |- macos
      |- jre.zip  # JRE 1.8 for macOS. bcprov-jdk16-1.46.jar should be added to lib/ext.
  |- upgrade      # Stores goAgent upgrade packages. Packages created from agent builds are stored here. 
    |- devopsAgent.exe
    |- devopsAgent_linux
    |- devopsAgent_macos
    |- devopsDaemon.exe
    |- devopsDaemon_linux
    |- devopsDaemon_macos
    |- upgrader.exe
    |- upgrader_linux
    |- upgrader_linux
  |- script    # Stores scripts that control the start and stop of Agent installation
    |- linux
    |- windows
    |- macos
```

### Deploy Agent Installation Package

Agent consists of devopsDaemon and devopsAgent compiled by Go and backend/release/worker-agent.jar.

- Copy backend/release/worker-agent.jar complied by backend to the jar/ directory.
- Move devopsDaemon, devopsAgent and upgrader compiled by Go in the agent/bin directory to the upgrade/ directory.

### JRE Directory Description

This directory is used to store JRE for Linux/Windows/macOS and serves as the Java execution environment for worker-agent.jar.

#### Prepare to create jre.zip

- Please download JRE 1.8 for Linux/Windows/macOS (Note that a fee may be charged) and unzip it to the current directory. Do not put it in the jre_xxxx directory created.
- Download the cryptography toolkit bcprov-jdk16-1.46.jar and move it into the lib/ext directory created from the previous step. Please download it from qualified Maven repositories. The following link is for reference only: [Download bcprov-jdk16-1.64.jar](http://central.maven.org/maven2/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar)

#### Rezip JRE Package

- Compress files in jre/'s root directory: `zip -r jre.zip *`. In other words, the jre/ directory no longer exists in the zip file.
- Move jre.zip to the directory corresponding to your operating system, namely Linux/macOS/Windows.

#### .agent.properties Description

Only one Agent can be installed on one machine and this Agent can belong to only one project simultaneously.

- Content of the Agent configuration file config/.agent.properties:

```
devops.project.id=##projectId##
devops.agent.id=##agentId##
devops.agent.secret.key=##agentSecretKey##
landun.gateway=##gateWay##
devops.parallel.task.count=4
landun.env=##landun.env##
devops.master.restart.hour=0
```

- devops.project.id is the English name of the project that Agent binds to. It will be automatically replaced when the user downloads and installs it.
- devops.agent.id is Agent ID. It will be automatically replaced when the user downloads and installs it.
- devops.agent.secret.key is Agent key. It will be automatically replaced when the user downloads and installs it.
- devops.parallel.task.count is the number of concurrent builds. By default, 4 builds are can be run concurrently.
- landun.gateway is the bkci gateway. It will be automatically replaced when the user downloads and installs it.
- landun.env is the environment type. It will be automatically replaced when the user downloads and installs it.
