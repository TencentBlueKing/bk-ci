![LOGO](docs/resource/img/bkci_cn.png)
---
[![license](https://img.shields.io/badge/license-mit-brightgreen.svg?style=flat)](https://github.com/Tencent/bk-ci/blob/master/LICENSE) [![Release Version](https://img.shields.io/badge/release-0.0.1-brightgreen.svg)](https://github.com/Tencent/bk-ci/releases) [![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/bk-ci/pulls) ![Backend CI](https://github.com/Tencent/bk-ci/workflows/Backend%20CI/badge.svg?branch=master)

> **Note**: The `master` branch may be in an unstable or even broken state during development. Please use [releases](https://github.com/tencent/bk-ci/releases) instead of  `master` branch in order to get stable binaries.

a free & open source CI server, bk-ci(BlueKing Continuous Integration) helps you automate your build-test-release workflows, continuous delivery of your product faster, easier, with fewer bugs.

TODO
使用bkci屏蔽掉所有研发流程中的繁琐环节，让你聚焦于编码。bkci通常被用于：
- 工程编译
- 静态代码检查
- 运行测试用例，及时发现BUG
- 部署与发布

TODO
bkci提供了流水线、代码库、凭证管理、环境管理、研发商店5大核心服务，多重组合，满足企业不同场景的需求：
- **Pipeline**：将团队现有的研发流程以可视化方式呈现出来，编译、测试、部署，一条流水线搞定
- **Code**：将企业内已有的代码托管服务关联至bkci
- **凭证管理**：为代码库、流水线等服务提供不同类型的凭据、证书管理功能
- **环境管理**：可以将企业内部的开发编译机托管至bkci
- **研发商店**：由流水线插件和流水线模板组成，插件用于对接企业内部的各种第三方服务，模板助力企业内部的研发流程规范化

## Overview
- [Architecture Design](docs/overview/architecture.md)
- [Code Directory](docs/overview/code_framework.md)
- [Design Philosophy](docs/overview/design.md)

## Features
- CI/CD: 由于框架的可扩展性，bkci既可以用作简单的CI场景，也可以成为企业内所有项目的持续交付中心
- Visualize:  bkci提供了灵活的可视化编排流水线，动动指尖，将研发流程描述与此
- Scalable: 灵活的架构设计可以随意横向扩容，满足企业大规模使用
- Distributed: bkci可以便捷的管控多台构建机，助你更快的跨多平台构建、测试和部署
- Pipeline Plugins: bkci拥有完善的插件开发体系，其具备了低门槛、灵活可扩展等特性
- Pipeline Templated: 流水线模板将是企业内部推行研发规范的一大助力

## Experience
- [bk-ci in docker](https://hub.docker.com/r/blueking/bk-ci)

## Getting started
- [Download and Compile](docs/overview/source_compile.md)
- [Installation and Deployment](docs/overview/installation.md)

## Support
1. [wiki](https://github.com/Tencent/bk-ci/wiki)
2. [BK forum](https://bk.tencent.com/s-mart/community)
3. QQ Group: 744672165

## BlueKing Community
- [BK-BCS](https://github.com/Tencent/bk-bcs)：a basic container service platform which provides orchestration and management for micro-service business.
- [BK-BCS-SaaS](https://github.com/Tencent/bk-bcs-saas)：a SaaS provides users with highly scalable , flexible and easy-to-use container products and services.
- [BK-CMDB](https://github.com/Tencent/bk-cmdb)：an enterprise level configuration management serivce database.
- [BK-PaaS](https://github.com/Tencent/bk-PaaS)：an development platform that allows developers to create, develop, deploy and manage SaaS applications easily and quickly.
- [BK-SOPS](https://github.com/Tencent/bk-sops)：an lightweight scheduling SaaS for task flow scheduling and execution through a visual graphical interface.

## Contributing
- If you have good ideas or suggestions, please let us know by Issues or Pull Requests and contribute to the Blue Whale Open Source Community. For bk-ci branch management, issues, and pr specifications, read the [Contributing Guide](CONTRIBUTING.md)。
- If you are interested in contributing, check out the [CONTRIBUTING.md](https://opensource.tencent.com/contribution), also join our Tencent OpenSource Plan.

## License
BK-CI is based on the MIT protocol. Please refer to [LICENSE](LICENSE.txt) for details.
