# BlueKing Continuous Integration (bk-ci) Architecture

BlueKing Continuous Integration (**bk-ci**) is built with multiple programming languages, including Kotlin, Java, JS, Go, Lua and Shell. It adopts a high-availability and scalable service framework based on complete separation of frontend and backend, as well as plugin development.

- **WebAPI Gateway & FrontEnd:**
  - **WebAPI Gateway:** Powered with OpenResty. It contains user login handlers and identity authentication, as well as Lua scripts and Nginx configurations forwarded by **Consul** service discovery of backend APIs.
  - **FrondEnd:** Pure frontend design based on Vue. It contains static resources like js, img and html files.
- **MicroService BackEnd:** Based on Kotlin and Java. It uses the SpringCloud microservice architecture. Each microservice module is introduced below in startup order.
  - **Project:** Project management, It manages pipeline projects. Multiple modules depend on it.
  - **Log:** Build logging service. It stores build logs and returns query outputs.
  - **Ticket:** Ticket management service. It stores user ticket information, such as the username and password of the repository, SSL, Token, etc.
  - **Repository:** Repository management service. It stores user repositories and depends on the interaction with Ticket.
  - **Artifactory:** Artifact storage service. This service only performs the simplified version of artifact access, and it can be extended to integrate into your own storage system.
  - **Environment:** Agent service. It imports agents and uses environment management to manage agent clusters for the scheduling of parallel builds.
  - **Store:** Development store service. It facilitates the management of pipeline plugins and pipeline template, including their updates, releases or removals. It interacts with Process and Artifactory.
  - **Process:** Pipeline management. It manages pipelines and core services of the pipeline scheduling function.
  - **Dispatch:** Agent scheduling. It receives agent startup events of pipelines and distributes them to the corresponding agent to handle.
  - **Plugin:** Plugin extension service. It is empty for now and is mainly used to extend some backend services interacting with frontend pages, such as integration with all sorts of CD platforms, testing platforms, quality assurance platforms, etc. These services can be configured with frontend pages and leave room for imagination.
- **Resource:** It provides storage service, fundamental middleware, etc.
  - **Storage:** Basic development environment of dependencies like storage service, middleware, etc.
    - **MySQL/MariaDB:** Master database of bk-ci. MySQL 5.7.2 or MariaDB 10.x can be used to store relational data from all the microservices above.
    - **Redis:** Core service caching. Its version is 3.x. It caches agent and build information, provides distributed lock management, etc.
    - **ElasticSearch:** Log storage. Log module is integrated with ES to access build logs.
    - **RabbitMQ:** Core message queue service. The pipeline event mechanism of bk-ci is based on RabbitMQ to deliver event messages.
    - **FileSystem:** It mainly provides services for Artifactory and is used to store binary files like plugins and artifacts. It can be integrated with files or cloud storage to extend the services of Artifactory module.
    - **Consul:** As the service discovery server of microservices, Consul server should be set up. Consul should also be installed and run in Agent mode on machines deployed with bk-ci microservices. A cluster can be built via starting Consul Server in Agent mode directly on two machines deployed with bk-ci microservices in order to reduce the need for machines.
  - **Storage:** Agent is a server or a PC that compiles and builds code in CI. It is a two-part process which combines compilation dependencies of Go, GCC, Java, Python, Node.js, etc. with functions provided by bk-ci.
    - **Agent:** Written in Golang. It is divided into DevopsDaemon and DevopsAgent two processes.
      - **DevopsDaemon:** It runs in the background as daemon and starts DevopsAgent.
      - **DevopsAgent:** It communicates with the **Dispatch** and **Environment** module and is responsible for upgrading the whole **Agent** as well as starting and destroying the **Worker** process.
    - **Worker:** Written in Kotlin. It is a file named agent.jar and is the actual task performer. It is started by **DevopsAgent** via JRE. It then communicates with the **Process** module, receives and implements plugin tasks, as well as reports results  (**Log & Process**).
