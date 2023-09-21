# Backend Microservice Deployment

There are 10 microservices (artifactory, dispatch, environment, log, plugin, process, project, repository, store, ticket) and one Agent (worker) under BlueKing ci backend (under backend/ directory).

## 1. System Requirements

- JDK 1.8
- Gradle 6.7
- Redis 2.8.17
- MySQL 5.7
- ES 7.4
- Consul 1.0 [Consul Installation](consul.en.md)
- RabbitMQ 3.7.15 [RabbitMQ Deployment](rabbitmq.en.md)

## 2. Installation Guide

### 2.1 Compilation

#### 2.1.1 Database Initialization

- jOOQ is used to map database tables to POs for compilation, which depends on database tables. Therefore, the database needs to be initialized first. The initialization scripts are in the bk-ci/support-files/sql directory. Login to the database and run these scripts sequentially.

#### 2.1.2 Gradle Configurations Before Compilation

- Modify the following parameters in gradle.properties.
  ```
  MAVEN_REPO_URL=Set it to your private Maven repository if you have one. Otherwise, you can use a public one.
  MAVEN_REPO_DEPLOY_URL=If you need to deploy JAR packages to your private Maven repository, set it to your repository address.
  MAVEN_REPO_USERNAME=Only fill in when deployment is needed.
  MAVEN_REPO_PASSWORD=Only fill in when deployment is needed.
  DB_HOST=Your database. jOOQ needs to connect to your database and access database tables to create POs for compilation.  
  DB_USERNAME=Database username
  DB_PASSWORD=Database password
  ```

#### 2.1.3 Start Gradle Compilation

- cd bk-ci/src/backend & gradle clean build
- All the artifacts are placed in the backend/release directory, which mainly include the following ones.
  - worker-agent.jar  This process runs tasks in the agent. As it is written in Kotlin, it is compiled with the backend. Eventually it will be merged with GoAgent installation package for deployment.
  - boot-assembly.jar
    - This is a monolithic Springboot package which integrates 10 microservices. If you need to deploy using the monolithic package, set the service_name parameter in init.lua of gateway to “bk-ci”. By default, this parameter is set to empty and each microservice is deployed separately.
  - boot-artifactory.jar       Artifact archiving microservice Springboot.jar
  - boot-dispatch.jar          Build scheduling microservice Springboot.jar
  - boot-environment.jar  Environment management microservice Springboot.jar
  - boot-log.jar                   Log microservice Springboot.jar
  - boot-plugin.jar              Extension microservice Springboot.jar
  - boot-process.jar           Pipeline microservice Springboot.jar
  - boot-project.jar            Project management microservice Springboot.jar
  - boot-repository.jar      Repository microservice Springboot.jar
  - boot-store.jar               Store microservice Springboot.jar
  - boot-ticket.jar              Ticket microservice Springboot.jar

### 2.2 Microservice Deployment

Create a ci/ directory in the /data/bkee root directory on the deployment server.

- Create directories in ci/ according to the microservice name and put the above boot-xxx.jar files in them.
- Modify the name of each microservice script in the /bk-ci/support-files/templates/ directory. For example, #project#project.sh, which corresponds to the project management service, should be changed to project.sh and put in the corresponding directory.
- Variables in configuration files that are declared with two underscores need to be substituted. See [support-files placeholder declaration](../../support-files/README.MD) to substitute the corresponding placeholders.

```
|- /data/bkee
  |- etc
    |- ci
      |- common.yml                # Common configuration file
      |- application-project.yml   # Microservice configuration file
  |- ci                  # Root directory of the program
    |- project             # Microservice directory. There is a total of 10 microservice directories and they will not be listed here one by one.
      |- project.sh        # Startup script of the Project microservice
      |- boot-project.jar  # SpringBoot.jar of the Project microservice
```

- Start microservice: Take the project management service as an example. `/data/bkee/ci/project/project.sh start`

### 2.3 Special Deployment Process of the Artifactory service

- If the microservice is deployed on multiple nodes, the value of archiveLocalBasePath in the application-artifactory.yml configuration file should be distributed and highly available. For example, it can mount NFS or CephFS.
- For the initialization of the icon files of default plugins, all the directories under bk-ci/support-files/file should be put into the path specified by the archiveLocalBasePath parameter in the application-artifactory.xml file. Otherwise, the icons will not be displayed properly.
