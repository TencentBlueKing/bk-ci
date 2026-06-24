---
name: artifactory-module-architecture
description: 处理 BK-CI 制品上传下载、制品元数据、BkRepo 或磁盘后端存储、文件任务和清理链路时使用。当用户提到构建产物、制品归档、下载令牌、报告文件、BkRepo 集成或制品清理时优先使用。
---

# Artifactory 模块架构

## 适用场景

- 上传、下载、归档构建产物
- 管理文件元数据、文件任务和下载令牌
- 切换或排查 BkRepo / 磁盘存储后端
- 处理报告文件、自定义目录和流水线产物路径
- 排查构建完成后的制品后处理

## 不适用场景

- 只是商店组件发布问题，不涉及制品存储
- 只是普通文件路径拼接，不涉及制品语义和存储后端
- 只是流水线执行问题，不涉及产物归档或下载

## 快速指导

1. 先判断问题属于哪条主线：
   - 存储模型与路径：看 `reference/1-artifact-storage.md`
   - BkRepo、任务与扩展：看 `reference/2-bkrepo-task-extension.md`
2. `Artifactory` 负责“文件和制品如何被存储与访问”，不是执行引擎本身。
3. 如果问题表现为上传成功但文件找不到，先区分是：
   - 元数据没落库
   - 后端没存成功
   - 路径规则错了
   - 下载令牌或权限错了
4. 存储后端切换或双实现问题，优先确认实际走的是 BkRepo 还是本地磁盘实现。

## 高信号规则

- `PROJECT_CODE` / `PROJECT_ID` 通常仍按项目英文标识理解
- 文件元数据、文件任务和真实后端存储不是同一层
- BkRepo 与 Disk 是两套后端实现，排查时要先确认命中的实现类
- 构建完成后的异步处理也会影响制品最终可见性

## 关键陷阱

- 只看文件存储后端，不看元数据和任务状态
- 路径规则变了却没同步影响下载、报告或自定义目录逻辑
- 只验证上传接口成功，不验证后续查询、下载和清理链路
- 把商店归档和普通流水线制品归档混成同一问题

## 延伸阅读

- 存储基础：`reference/1-artifact-storage.md`
- BkRepo 与任务：`reference/2-bkrepo-task-extension.md`
- 涉及项目语义时：再看 `project-module-architecture`
- 涉及商店归档时：再看 `store-module-architecture`
