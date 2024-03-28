# Framework of BlueKing Continuous Integration (BK-CI)

```
|- bk-ci
  |- docs  
  |- scripts
  |- src
    |- agent
    |- backend
    |- frontend
    |- gateway
  |- support-files
```

## Source Code (src) of the Project

The project is built with multiple languages, including Vue, Lua, Kotlin, Java, Go and Shell. It is a multi-tier application with gateway, frontend, backend, agent, pipeline, etc.

### Gateway Code (gateway)

```
|- bk-ci/src
  |- gateway
    |- html     # Stores HTML standard templates corresponding to various HTTP status codes. These templates can be replaced if necessary.
    |- lua      # Stores Lua scripts.
      |- *.lua  # Contains some Lua scripts. Pay special attention to init.lua, which contains some important configurations.
      |- resty  # Contains Resty code for public use, including open source implementation of MD5, UUID, cookie, etc.
    |- *.conf   # Contains various conf configurations. Pay special attention to server. server.devops.conf and auth.conf configuration files.
```

The gateway is built with OpenResty. Its high-performance web server based on Nginx and Lua is extended by Lua script to integrate with Consul microservice discovery and identity authentication functions.

### Frontend Code (frontend)

```
|- bk-ci/src
  |- frontend
    |- devops-atomstore   # Development store (Store)
    |- devops-codelib     # Repository management (Code)
    |- devops-environment # Environment management (Env)
    |- devops-pipeline    # Pipeline (Pipeline)
    |- devops-ticket      # Ticket management (Ticket)
    |- devops-nav         # Top navigation bar (Nav)
    |- svg-sprites        # Vector graphics
```
The frontend development is based on Vue and the directory is divided according to the service module.

### Backend Microservice (Kotlin/Gradel) & Agent (Go) Code

```
|- bk-ci/src
  |- agent         # Agent is written in Go and is used to run DevopsDaemon DevopsAgent on agents.
  |- backend
    |- project                  # Root directory of microservices of the project
      |- api-project            # API abstraction layer
      |- api-project-sample     # Default API abstraction layer that integrates with unique parts of different platforms
      |- api-project-blueking   # API abstraction layer that integrates with parts unique to BlueKing
      |- api-project-op         # API abstraction layer of backend operations
      |- biz-project            # Implementation of APIs and business services. Some abstractions that need to be extended will be implemented in the sample directory.
      |- biz-project-blueking   # Implementation of integration with BlueKing business services
      |- biz-project-sample     # Sample implementation of business service extensions. It mainly shows how to implement extensions.
      |- biz-project-op         # Implementation of backend operation APIs
      |- boot-process           # Builds Spring Boot microservice packages, configures build dependencies and stores outputs in the release directory.
      |- model-process          # Uses jOOQ to create POs from database tables rebuild.
    |- boot-assembly            # Used to build monolithic microservices. It assembles the monolithic jar packages of all microservices.
    |- common                   # Common module
      |- common-auth            # Authorization module
        |- common-auth-api      # API of the authorization module
        |- common-auth-provider # authorization module provider
    |- dispatch    # Root directory of the Dispatch microservice
    |- environment # Root directory of the Environment microservice
    |- log         # Root directory of the Log microservice
    |- artifactory # Root directory of the Artifactory microservice
    |- process     # Root directory of the Process microservice
    |- release     # Directory created by local packaging to store jar outputs
    |- repository  # Root directory of the Repository microservice
    |- store       # Root directory of the Store microservice
    |- ticket      # Root directory of the Ticket microservice
    |- worker      # Agent worker submodule
      |- worker-agent   # agent.jar in Agent is used to receive and send build tasks. New features can be added via Gradle dependencies.
      |- worker-api-sdk # Implementation and abstraction of various APIs that communicate with backend microservices
      |- worker-common  # Common implementation of agent.jar dependencies and API abstraction
      |- worker-plugin-archive # Implementation of built-in plugins for archiving artifacts. It is integrated into Agent.
      |- worker-plugin-scm     # Implementation of built-in git plugins for pulling code. It is integrated into Agent.
```

### Pipeline Plugin SDK & Demo (Java/Maven)

This is a Java SDK for pipeline plugin development. Completed plugins will be released in Store for users to install and use in their pipelines. See readme in the directory for details.

```
|- bk-ci/src
  |- pipeline-plugin
    |- bksdk    # Plugin SDK
    |- demo     # A starter demo of a pipeline plugin. Manage code by yourself for subsequent development and do not put your code here.
```

### Configuration File Templates (support-files)

```
|- bk-ci/support-files
  |- agent-package  # Distributed to the root directory of the project during deployment initialization. It is used to store Agent installation package for download.
  |- file           # Distributed to the directory specified in application-artifactory.yml during deployment initialization
  |- sql            # SQL initialization scripts. Initialize them first during compilation. Otherwise, jOOQ cannot create POs properly.
  |- template       # All the deployment configuration/script files that require substitution
```

- template Directory Description
  - The # in the file name represents the delimiting character of the relative path of the current file. Take #etc#bkci#common.yml as an example. Assume the root directory of BlueKing is /data/bkee/, then the final path of the file after the name change is /data/bkee/etc/ci/common.yml.
  - For placeholders, see support-files/README.md.

## Installation Scripts (scripts)

It stores some automated installation scripts for substitution. More content will be added.

```
|- bk-ci
  |- scripts
    |- bkenv.properties   # Declaration of some configuration parameters that require manual modification
    |- render_tpl         # Shell script to help users substitute all the parameters quickly and put them in the specified directories.
```
