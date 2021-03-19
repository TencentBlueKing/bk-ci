# Design of BK-CI

In terms of technical architecture, bk-ci independently develops a continuous integration framework and a pipeline engine. Its aim is to enhance the security, stability and extensibility of the platform and ensure the high availability of services.

- We extract each function needed in the development life cycle to a microservice and decompose complex modules by decomposing a monolithic application into multiple services, which ensures that each service is relatively simple, clearly structured and easy to test.
- The microservice architecture can integrate with services by other teams of the company as well. Every service can be added to bk-ci to provide service for the whole development life cycle.
- You can scale sub-services horizontally and dynamically and add sub-services flexibly to meet business needs when performance bottlenecks occur.
- The failure of one microservice do not lead to the failure of other microservices. The reliability of microservices can be greatly improved by techniques like isolation and circuit breaker.
- The microservice architecture also ensures that each sub-service can be deployed independently, and canary releases can be used. It speeds up the iteration of backend services, which makes the continuous delivery of the platform itself possible.

