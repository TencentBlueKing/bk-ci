---
name: kotlin-backend-conventions
description: 编写 BK-CI Kotlin 后端代码时使用，例如 Kotlin 文件结构、格式、命名、函数设计、空安全和 BK-CI 项目内的调用约定。当用户要修改 `.kt` 或 `.kts` 后端代码并需要项目级 Kotlin 规范时优先使用。
---

# Kotlin 后端规范

## 适用场景

- 编写或重构 BK-CI Kotlin 后端代码
- 处理 Kotlin 文件结构、格式和命名规范
- 调整函数设计、对象创建、空安全和集合使用方式
- 判断某段 Kotlin 写法是否符合项目约定

## 不适用场景

- 只是一般后端分层或服务归属判断
- 只是 Spring Boot 配置和框架用法
- 只是单元测试写法，不涉及 Kotlin 语言约定

## 快速指导

1. 这个 skill 关注的是 BK-CI Kotlin 代码约定，不替代后端分层、API 设计或 Spring Boot 架构 skill。
2. Kotlin 规范建议分三块看：
   - 文件结构、格式与导入：`reference/1-file-format-import.md`
   - 命名、函数设计与对象调用：`reference/2-naming-function-design.md`
   - 空安全、集合与 BK-CI 项目约定：`reference/3-null-safety-project-practice.md`
3. 能交给格式化工具和静态检查的规则，不要在主 skill 里重复展开。
4. 项目特有的高价值约定要优先关注，例如行宽、命名参数、禁止 wildcard import、避免完整类路径直接调用。
5. 如果问题已经转向后端服务分层，联动看 `backend-microservice-development`；如果问题转向 Spring Boot 框架用法，后续再看对应框架规范。

## 高信号规则

- Kotlin 规范的重点是可读性、一致性和低歧义，而不是把语言特性全部用满
- 项目特有约定应优先于泛化教程式知识
- 语言规范适合按需加载为 skill，不适合以长篇 rule 常驻在每次会话中

## 关键陷阱

- 把通用 Kotlin 教程内容原样塞进规则层
- 只记格式，不看 BK-CI 项目特有约束
- 语言规范、后端架构规范、Spring Boot 用法混成一个大文档

## 延伸阅读

- 文件结构、格式与导入：`reference/1-file-format-import.md`
- 命名、函数设计与对象调用：`reference/2-naming-function-design.md`
- 空安全、集合与 BK-CI 项目约定：`reference/3-null-safety-project-practice.md`
- 如果你在改后端分层：再看 `backend-microservice-development`
- 如果你在写测试：再看 `unit-testing`
