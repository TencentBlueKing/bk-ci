---
name: springboot-backend-conventions
description: 编写 BK-CI Spring Boot 后端代码时使用，例如配置管理、Bean 装配、事务与数据访问、异常处理、监控和框架层最佳实践。当用户要处理 Spring Boot 框架用法而不是服务归属或语言风格时优先使用。
---

# Spring Boot 后端规范

## 适用场景

- 处理 Spring Boot 配置与多环境管理
- 设计 Bean 装配、条件配置和依赖注入方式
- 调整事务、数据访问、异常处理和监控能力
- 判断某段后端实现是否符合框架层最佳实践

## 不适用场景

- 只是判断功能应该落在哪个微服务
- 只是 Kotlin 语言风格和代码格式问题
- 只是 API 契约设计，不涉及 Spring Boot 框架用法

## 快速指导

1. 这个 skill 关注的是 BK-CI 后端里的 Spring Boot 框架实践，不替代微服务分层、API 设计或 Kotlin 语言规范。
2. Spring Boot 相关问题建议分三块看：
   - 配置管理与 Bean 装配：`reference/1-config-bean.md`
   - 数据访问、事务与异常处理：`reference/2-data-transaction-exception.md`
   - 监控、日志、测试与质量：`reference/3-observability-testing-quality.md`
3. 优先使用构造器注入、类型安全配置和明确的生命周期边界。
4. 能由框架和基础设施托底的约束，不要在业务代码里重复手工实现。
5. 如果问题已经转向服务归属，联动看 `backend-microservice-development`；如果转向微服务底座能力，联动看 `microservice-infrastructure`。

## 高信号规则

- Spring Boot 规范的重点是稳定装配、清晰边界和可运维性
- 配置、事务、异常、监控这几类框架能力往往会一起决定系统稳定性
- 框架手册不适合常驻在 rule 中，更适合作为按需加载的 skill

## 关键陷阱

- 把 Spring Boot 框架实践和业务分层规范混成一个文档
- 只会用注解堆功能，不考虑生命周期、事务边界和运行时影响
- 把长篇框架说明放进 always rule，导致常驻噪声过高

## 延伸阅读

- 配置管理与 Bean 装配：`reference/1-config-bean.md`
- 数据访问、事务与异常处理：`reference/2-data-transaction-exception.md`
- 监控、日志、测试与质量：`reference/3-observability-testing-quality.md`
- 如果你在看微服务分层：再看 `backend-microservice-development`
- 如果你在看微服务底座：再看 `microservice-infrastructure`
