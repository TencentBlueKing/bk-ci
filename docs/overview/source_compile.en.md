# bk-ci Compilation Guide

## Frontend Code Compilation

For BlueKing ci frontend (in the frontend directory), common-lib and svg-sprites contain static resources of project dependencies. The rest of directories contain SPA projects built with Vue, among which devop-nav is the main entry point. Other sub-services are integrated using the iframe element or the UMD pattern.

## System Requirement

Node.js 8.0.0+

## Installation Guide

- 1、Package and deploy the corresponding Vue project, and then navigate to the src/frontend directory.

```
# First, install yarn globally
npm install -g yarn
# Then run install
yarn install
# Next, install the dependencies of each subroutine  
yarn start
# Finally, run the packaging command
yarn public
```

## Gateway Code

The gateway is built with Lua scripts and Nginx configurations, so no compilation is needed.

## Agent Compilation (Go)

### System Requirement

- Golang 1.12
- Agent is written in Golang. Currently it supports Linux, Windows and macOS three operating systems. No compilation verification has been performed on other operating systems. Go compiler is used for compilation on these three operating systems.

### Compilation

- Do not use cross compilation to create agent executables.
- After installing Golang, add bk-ci/src/agent to the GOPATH environment variable.
- Compile on Linux: build_linux.sh
- Compile on Windows: build_linux.sh
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

## Backend Microservice Compilation (Kotlin)

### System Requirements

- MySQL 5.7
- JDK 1.8
- Gradle 6.7

#### Database Initialization

jOOQ is used to map database tables to POs for compilation, which depends on database tables. Therefore, the database needs to be initialized first. The initialization scripts are in the bk-ci/support-files/sql directory. Login to the database and run these scripts sequentially.

#### Gradle Configurations Before Compilation

Modify the following parameters in gradle.properties.

```
MAVEN_REPO_URL=Set it to your private Maven repository if you have one. Otherwise, you can use a public one.
MAVEN_REPO_SNAPSHOT_URL=Set it to your private Maven snapshot repository if you have one. Otherwise, you can use a public one.
MAVEN_REPO_DEPLOY_URL=If you need to deploy JAR packages to your private Maven repository, set it to your repository address.
MAVEN_REPO_USERNAME=Only fill in when deployment is needed.
MAVEN_REPO_PASSWORD=Only fill in when deployment is needed.
DB_HOST=Your database. jOOQ needs to connect to your database and access database tables to create POs for compilation.  
DB_USERNAME=Database username
DB_PASSWORD=Database password
```

#### Compilation

```shell
cd bk-ci/src/backend/ci & gradle clean build
```

All the artifacts are stored in the backend/release directory, which mainly include the following ones.
Package Name | Description
:--- | :---
worker-agent.jar | This process runs tasks in the agent. As it is written in Kotlin, it is compiled with the backend. Eventually it will be merged with GoAgent installation package for deployment.
boot-assembly.jar | This is a monolithic Spring Boot package which assembles 10 microservices. If you need to deploy using the monolithic package, set the service_name parameter in init.lua of gateway to “bk-ci”. By default, this parameter is set to empty and each microservice is deployed separately.
boot-artifactory.jar  | Artifactory microservice Springboot.jar
boot-dispatch.jar     | Dispatch microservice Springboot.jar
boot-environment.jar  | Environment microservice Springboot.jar
boot-log.jar          | Log microservice Springboot.jar
boot-plugin.jar       | Plugin microservice Springboot.jar
boot-process.jar      | Process microservice Springboot.jar
boot-project.jar      | Project microservice Springboot.jar
boot-repository.jar   | Repository microservice Springboot.jar
boot-store.jar        | Store microservice Springboot.jar
boot-ticket.jar       | Ticket microservice Springboot.jar
