![LOGO](docs/resource/img/bkci_cn.png)
---
[![license](https://img.shields.io/badge/license-mit-brightgreen.svg?style=flat)](https://github.com/Tencent/bk-ci/blob/master/LICENSE.txt) [![Release Version](https://img.shields.io/github/v/release/Tencent/bk-ci?include_prereleases)](https://github.com/Tencent/bk-ci/releases) [![Backend CI](https://github.com/Tencent/bk-ci/workflows/Backend%20CI/badge.svg?branch=master)](https://img.shields.io/github/workflow/status/Tencent/bk-ci/Tag%20Realse)

English | [简体中文](README.md)

> **Note**: The `master` branch may be in an unstable or even broken state during development. Please use [releases](https://github.com/tencent/bk-ci/releases) instead of  `master` branch in order to get stable binaries.

a free & open source CI server, bk-ci(BlueKing Continuous Integration) helps you automate your build-test-release workflows, continuous delivery of your product faster, easier, with fewer bugs.

bk-ci removes all the tedious tasks in R&D process and enables you to focus on coding. It is often used for:

- Project compilation
- Static code analysis
- Running test cases to find bugs in time
- Deployment and release

bk-ci provides seven core services, namely Process, CodeCheck, Repository, Ticket, Environment, Store and Turbo. They can be combined in multiple ways to meet business needs in different scenarios.

- **Process**: It visualizes current R&D process of the team. One pipeline can handle compilation, testing and deployment.
- **CodeCheck**：Provide professional code-check solutions to check multiple dimensions of code issues such as defects, security vulnerabilities, code styles, etc., to promote product quality.
- **Repository**: It links current source code hosting service of the enterprise to bk-ci.
- **Ticket**: It provides the management of various kinds of tickets and certificates for services like Repository and Process.
- **Environment**: It can host the internal agents of the enterprise on bk-ci.
- **Store**: It consists of pipeline plugins and pipeline templates. Plugins are used to integrate with various third-party services of the enterprise and templates help to standardize its internal R&D process.
- **Turbo**: Based on self-developed acceleration engine, it supports acceleration in multiple scenarios such as C/C++ compilation, UE4 code compilation, and UE4 Shader compilation, making construction tasks faster


## Overview

- [Architecture](docs/overview/architecture.en.md)
- [Code Directory](docs/overview/code_framework.en.md)
- [Design](docs/overview/design.en.md)

## Features

- Continuous integration and continuous delivery: Due to the scalability of the framework, bk-ci can not only be used in simple CI scenarios, but also as the continuous delivery center of all the projects of the enterprise.
- What you see is what you get: bk-ci provides flexible and visualized pipelines for you to orchestrate. Move your fingers and you can describe the R&D process here.
- Parallel and scalable framework: The flexible framework can be scaled horizontally at will to meet the enterprise’s need for large-scale use.
- Distributed system: bk-ci can manage multiple agents easily to help you perform cross-platform build, testing and deployment in a faster manner.
- Pipeline plugins: bk-ci has a complete plugin development system with properties like low barrier to entry and scalability.
- Pipeline templates: It promotes the standardization of R&D within the enterprise.
- CodeCheck rule set：accumulate the team's code requirements, and can be shared and upgraded across projects or teams.

## Experience

- [bk-ci in docker](https://hub.docker.com/r/blueking/bk-ci)

## Getting started

- [Download and Compile](docs/overview/source_compile.en.md)
- [Install and Deploy Within One Minute](docs/overview/installation.en.md)

## Support
1. [GitHub Discussions](https://github.com/Tencent/bk-ci/discussions)
2. QQ Group: 495299374

## BlueKing Community

- [BK-BCS](https://github.com/Tencent/bk-bcs): BlueKing Container Service is an orchestration platform for microservices based on container technology.
- [BK-CMDB](https://github.com/Tencent/bk-cmdb): BlueKing Configuration Management DataBase (BlueKing CMDB) is an enterprise level configuration management platform for assets and applications.
- [BK-JOB](https://github.com/Tencent/bk-job): BlueKing JOB is a set of operation and maintenance script management platform with the ability to handle a large number of tasks concurrently.
- [BK-PaaS](https://github.com/Tencent/bk-PaaS): BlueKing PaaS is an open development platform that allows developers to create, develop, deploy and manage SaaS applications quickly and easily.
- [BK-SOPS](https://github.com/Tencent/bk-sops): BlueKing Standard OPS (SOPS) is a light-weighted SaaS product in the Tencent BlueKing product system designed for the orchestration and execution of tasks through a graphical interface.
## Contributing

- Please read [Contributing](CONTRIBUTING.en.md) for the branch management, issue and pr specifications of bk-ci.
- [Tencent Open Source Incentive Program](https://opensource.tencent.com/contribution) encourages the participation and contribution of developers. We look forward to having you join it.

## License
BK-CI is based on the MIT license. Please refer to [LICENCE](LICENSE.txt) for details.

We undertake not to change the open source license (MIT license) applicable to the current version of the project delivered to anyone in the future.
