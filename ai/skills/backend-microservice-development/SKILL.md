---
name: backend-microservice-development
description: 编写 BK-CI 后端微服务代码时使用，例如新增 Resource、组织 API/Service/DAO 分层、依赖注入、服务归属判断和 Spring Boot 开发约定。当用户要做 Kotlin/Java 后端开发时优先使用。
---

# 后端微服务开发

## 适用场景

- 新增或修改 Kotlin / Java 后端功能
- 新增 Resource、Service、DAO 或微服务模块
- 判断功能应该落到哪个服务
- 按项目分层约定组织代码

## 不适用场景

- 前端 Vue 页面开发
- Go Agent 开发
- 单纯设计 API 契约或数据库模型

## 快速指导

1. 这个 skill 关注的是“BK-CI 后端功能如何组织和落位”，不是单纯 Spring Boot 入门。
2. 开发前先做服务归属判断，再决定接口、服务和数据层放哪。
3. 核心分层仍然是 API、业务实现、启动模块和数据模型，不要跨层乱放逻辑。
4. 服务间交互走 API 契约，不直接碰别的服务数据库。
5. 如果问题是接口设计细节，联动看 `api-interface-design`；如果是测试，联动看 `unit-testing`。

## 高信号规则

- 功能落位比代码实现本身更容易决定后续维护成本
- Resource、Service、DAO 的边界清楚，后续扩展才稳定
- 微服务之间只共享契约，不共享存储实现

## 关键陷阱

- 为了快，直接把逻辑塞进 Resource
- 服务归属判断错误，导致后续跨服务耦合
- 因为调用方便而直接访问其他服务数据

## 延伸阅读

- 如果你在设计接口：再看 `api-interface-design`
- 如果你在写测试：再看 `unit-testing`
- 如果你在看基础设施：再看 `microservice-infrastructure`
- 如果你在看 Spring Boot 框架实践：再看 `springboot-backend-conventions`
