# Installation & Deployment

## 1. Deployment Directory Guide

The deployment directory conforms to BlueKing Operation Specifications. Here is an example with /data/bkee as root directory. Users can change the directory name by themselves, like /a/b. Since the directory has multiple lower directories, they need to go through them carefully. The details are as follows.

```
|- /data/bkee  # Root directory of BlueKing
  |- ci      # Directory of ci deployment programs
  |- etc       # Root directory of BlueKing configuration files
    |- ci    # Directory of ci configuration files
```

The following directories are explained in detail.

### 1.1 Directory of ci deployment

```
|- /data/bkee/ci       # Root directory of the program
  |- agent-package       # Location of the agent installation package
  |- frontend            # Directory of frontend static resources
  |- gateway             # Gateway configuration files and Lua scripts
  |- project             # There is a total of 10 microservice directories and they will not be listed here one by one.
    |- project.sh        # Startup script of the Project microservice
    |- boot-project.jar  # SpringBoot.jar of the Project microservice
```

### 1.2 Directory of ci configuration files

```
|- /data/bkee/etc   # Root directory of BlueKing configuration files
  |- ci                 # Directory of ci configuration files
    |- common.yml   # Common configurations of all microservices
    |- application-project.yml  # Configurations of the Project microservice. There are 10 microservice configuration files. If new microservices are added, the corresponding configuration files will be placed here.
```

## 2. Basic Environment Deployment

### 2.1 System Requirements

- CentOS 7.x
- JDK 1.8
- Gradle 6.7
- Redis 2.8.17
- MySQL 5.7
- ES 7.4
- RabbitMQ 3.7.15 [RabbitMQ Installation Document](./linux/rabbitmq.en.md)
- Consul 1.0+ [Consul Installation](./linux/consul.en.md)

### 2.2 Database Initialization

Run files in the support-files/sql directory sequentially by file number.

## 3. Program Deployment

### 3.1 Gateway Deployment

OpenResty is used as the gateway server. The deployment consists of two parts, OpenResty installation as well as deployment of Lua and Nginx configurations of gateway.

- [bk-ci Gateway Deployment](./linux/gateway.en.md)

### 3.2 Frontend Compilation & Deployment

- [Frontend Deployment](./linux/frontend.en.md)

Configuration file templates created from frontend builds require variable substitution.

```bash
  ./render_tpl -m ci /data/bkee/ci/frontend/pipeline/frontend#pipeline#index.html
  ./render_tpl -m ci /data/bkee/ci/frontend/console/frontend#console#index.html
```

### 3.3 Backend Microservice Deployment

Compilation and deployment of backend microservices and agent.jar

- [Backend Service Compilation & Deployment](./linux/backend.en.md)

### 3.4 Agent Compilation & Deployment

- [Agent Compilation & Installation Package Deployment](./linux/agent.en.md)

### 3.5 Initialization of configuration files in the support-files/template directory

Variables in configuration files that are declared with "__" (two underscores) require a placeholder substitution and the substituted ones have been extracted to the scripts/bkenv.properties file.

- The corresponding parameters in scripts/bkenv.properties need to be modified. Parameters relating to BlueKing or unused parameters can remain as default. Save and exit after modification.

  - Set INSTALL_PATH to the root directory of installation. Its default value is /data/bkee.
  - It is not recommended to modify the MODULE parameter. Its default value is ci.

- Running the scripts/render_tpl script will automatically substitute all the variables in all the files in the support-files/templates directory and move these files to the normal installation path.

```bash
cd /data/bkee/ci/scripts
chmod +x render_tpl
./render_tpl -m ci ../support-files/templates/*
```
