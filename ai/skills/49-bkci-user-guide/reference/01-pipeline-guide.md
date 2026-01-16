# 流水线完整使用指南

## 概述

流水线是蓝盾的核心功能，用于实现持续集成和持续部署(CI/CD)。本指南涵盖流水线的所有功能，从基础查看到高级管理。

## 1. 流水线查看与基础操作

### 1.1 流水线详情页面

在流水线列表界面，点击流水线名称进入详情页面。页面结构如下：

#### 页面顶部信息栏

**1. PAC 标识**
- 如果流水线开启了 Pipeline as Code 模式，会显示 PAC 标识
- 点击标识可以查看流水线 YAML 文件路径以及存储的代码库
- 未开启 PAC 的流水线无此标识

**2. 版本信息**
- 默认选中最新正式版本
- 可以点击下拉切换到存量版本查看
- 支持版本对比功能

**3. 流水线徽章**
- 展示最新一次执行的结果
- 点击可以复制链接，添加到项目的 README.md 中
- 方便访问者快速了解项目的构建状态、测试结果或部署状态等信息

**4. 操作按钮**
可以进行的操作：
- 编辑流水线
- 执行流水线  
- 更多：如收藏、导出、禁用、删除等

当切换到非最新版本时，可以进行的操作会有变化。

**5. 菜单栏**
可以切换到如下功能区：
- 查看执行历史
- 查看触发事件，需要时可以重放事件触发流水线
- 查看流水线编排/触发器/通知设置/基础设置
- 进行流水线权限管理
- 查看操作日志

### 1.2 流水线徽章功能

流水线徽章用于在项目 README 中展示构建状态：

```markdown
# 基础徽章
![Build Status](https://devops.woa.com/ms/process/api/external/pipelines/projects/{projectId}/pipelines/{pipelineId}/badge)

# 带链接的徽章
[![Build Status](https://devops.woa.com/ms/process/api/external/pipelines/projects/{projectId}/pipelines/{pipelineId}/badge)](https://devops.woa.com/console/pipeline/{projectId}/{pipelineId}/history)
```

**徽章状态类型**：
- 🟢 SUCCESS - 构建成功
- 🔴 FAILED - 构建失败  
- 🟡 RUNNING - 正在构建
- ⚪ CANCELED - 构建取消
- ⚫ UNKNOWN - 未知状态

## 2. 流水线编排与管理

### 2.1 流水线草稿和调试

#### 草稿模式的优势
- **安全编辑**: 在草稿状态下修改不会影响正式版本
- **增量保存**: 支持保存未完成的配置
- **协作编辑**: 多人可以同时编辑不同的草稿

#### 调试功能详解
```yaml
# 调试模式配置示例
debug:
  enabled: true
  breakpoints:
    - step: "build"
      condition: "variables.debug_mode == 'true'"
  log_level: "DEBUG"
  variable_watch:
    - "build_number"
    - "commit_id"
```

**调试功能特性**:
- **单步执行**: 逐个插件执行，便于定位问题
- **断点调试**: 在指定位置暂停，检查变量状态
- **变量监控**: 实时查看变量值的变化
- **日志增强**: 输出更详细的调试信息

### 2.2 流水线版本管理

#### 版本控制机制
- **自动版本号**: 每次保存自动递增
- **版本对比**: 对比不同版本的差异
- **版本回滚**: 快速回滚到历史版本
- **版本标签**: 为重要版本添加标签

#### 版本命名规范
```yaml
# 推荐的版本命名规范
版本格式: v{major}.{minor}.{patch}[-{tag}]

示例:
v1.0.0        # 初始正式版本
v1.1.0        # 新增功能
v1.1.1        # 问题修复
v2.0.0        # 重大变更
v2.0.0-beta   # 测试版本
v2.0.0-rc1    # 候选版本
```

#### 版本管理策略
- **主版本号**: 不兼容的重大变更
- **次版本号**: 向后兼容的功能新增
- **修订版本号**: 向后兼容的问题修复
- **预发布标识**: alpha、beta、rc 等

### 2.3 流水线变量管理

#### 变量类型与作用域

```yaml
# 全局变量定义
variables:
  global:
    APP_NAME: "my-application"
    VERSION: "1.0.0"
    
# Job 级变量
jobs:
  build:
    variables:
      BUILD_TYPE: "release"
      
# Step 级变量  
steps:
  - name: "compile"
    env:
      COMPILE_FLAGS: "-O2"
```

#### 变量引用语法详解

```bash
# 基础变量引用
echo "${{ variables.APP_NAME }}"

# 带默认值的引用
echo "${{ variables.CUSTOM_VAR || 'default_value' }}"

# 条件表达式
BUILD_ENV="${{ variables.BRANCH == 'master' && 'prod' || 'dev' }}"

# 字符串操作
UPPER_NAME="${{ upper(variables.APP_NAME) }}"
SUBSTRING="${{ substring(variables.VERSION, 0, 3) }}"

# 数组操作
FIRST_ITEM="${{ variables.ARRAY[0] }}"
ARRAY_LENGTH="${{ length(variables.ARRAY) }}"
```

#### 变量最佳实践

**命名规范**:
```yaml
# 推荐的变量命名
APP_NAME          # 应用名称 - 大写下划线
build_number      # 构建号 - 小写下划线  
deployEnv         # 部署环境 - 驼峰命名
DOCKER_REGISTRY   # 常量 - 全大写
```

**安全考虑**:
- 敏感信息使用凭据管理，不要直接写在变量中
- 生产环境变量设置为只读
- 定期审查变量使用情况

### 2.4 构建环境配置

#### 公共构建机配置

```yaml
# 公共构建机配置示例
jobs:
  build:
    runs-on:
      type: "public"
      image: "bkci/ci:latest"  # 标准镜像
      resources:
        cpu: "2"
        memory: "4Gi"
        disk: "20Gi"
```

**镜像选择指南**:
- `bkci/ci:latest` - 通用构建镜像，包含常用工具
- `bkci/node:16` - Node.js 专用镜像
- `bkci/java:11` - Java 开发镜像
- `bkci/python:3.9` - Python 开发镜像

#### 私有构建机配置

```yaml
# 私有构建机配置
jobs:
  build:
    runs-on:
      type: "private"
      pool: "my-build-pool"     # 构建机池
      labels:                   # 标签选择
        - "gpu-enabled"
        - "high-memory"
      env:                      # 环境变量
        CUSTOM_PATH: "/opt/tools"
```

**私有构建机优势**:
- 更好的性能和资源控制
- 可以安装自定义软件和工具
- 支持 GPU 等特殊硬件需求
- 更好的网络访问控制

#### 构建环境复用

```yaml
# 环境复用配置示例
jobs:
  build:
    runs-on: 
      pool-name: "my-build-pool"
      reuse-strategy: "REUSE_JOB_ID"  # 复用策略
      max-reuse-count: 5              # 最大复用次数
```

### 2.5 并发控制策略

#### 流水线级并发控制

```yaml
# 流水线并发配置
concurrency:
  # 最大并发执行数
  max-concurrent: 3
  
  # 排队策略
  queue-strategy: "FIFO"  # FIFO, LIFO, PRIORITY
  
  # 超时设置
  queue-timeout: "30m"
  
  # 取消策略
  cancel-in-progress: false
```

#### Job 级并发控制

```yaml
# Job 并发配置
jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu, windows, macos]
        node: [12, 14, 16]
      max-parallel: 6        # 最大并发数
      fail-fast: false       # 是否快速失败
```

#### 互斥组配置

```yaml
# 互斥组配置 - 多个流水线共享资源
concurrency:
  group: "deploy-production"
  cancel-in-progress: true
```

**互斥组使用场景**:
- 生产环境部署 - 确保同时只有一个部署
- 数据库迁移 - 避免并发修改数据库结构
- 资源清理 - 防止清理操作冲突

## 3. 流水线模板系统

### 3.1 模板概念与优势

当项目多个业务、多个服务的构建流程是标准化的、仅参数不一致时，可以通过模版来管理构建流程，规范业务构建流程、同时降低维护流水线成本。

通过模版实例化出不同业务/服务的约束模式流水线进行使用。当需要修改构建流程时，仅修改模版即可。变更可一键同步至所有流水线实例。

模版可以发布到商店，安装到不同的蓝盾项目下使用。

#### 模板的价值
- 标准化构建流程
- 降低维护成本
- 快速复制最佳实践
- 统一团队规范

#### 模板类型
- **约束模板**: 限制用户只能修改参数
- **非约束模板**: 允许用户自由修改
- **公共模板**: 发布到研发商店供所有项目使用
- **私有模板**: 仅在当前项目内使用

### 3.2 模板创建流程

#### 步骤1: 基础流水线准备
```yaml
# 创建标准的基础流水线
name: "标准构建模板"
description: "适用于 Java Spring Boot 项目的标准构建流程"

parameters:
  - name: "app_name"
    type: "string"
    required: true
    description: "应用名称"
    
  - name: "java_version"
    type: "enum"
    options: ["8", "11", "17"]
    default: "11"
    description: "Java 版本"
```

#### 步骤2: 参数化配置
```yaml
# 参数化示例
steps:
  - name: "代码检出"
    uses: "checkout@v1"
    with:
      repository: "${{ parameters.repository_url }}"
      
  - name: "Java 构建"
    uses: "maven@v1"
    with:
      java-version: "${{ parameters.java_version }}"
      goals: "clean package"
      
  - name: "Docker 构建"
    uses: "docker@v1"
    with:
      image-name: "${{ parameters.app_name }}"
      tag: "${{ variables.BUILD_NUMBER }}"
```

### 3.3 约束模板 vs 非约束模板

#### 约束模板特点
- 用户只能修改预定义的参数
- 确保流程标准化和一致性
- 适合企业级标准化场景

```yaml
# 约束模板配置
template:
  type: "constrained"
  allowed-modifications:
    - "parameters"
    - "variables"
  restricted-sections:
    - "jobs.build.steps"
    - "triggers"
```

#### 非约束模板特点
- 用户可以自由修改所有配置
- 提供基础框架，允许灵活定制
- 适合快速原型和个性化需求

### 3.4 模板发布与管理

#### 发布到研发商店
```yaml
# 模板发布配置
publish:
  store: "public"
  category: "build-templates"
  tags: ["java", "spring-boot", "maven"]
  version: "1.0.0"
  changelog: |
    - 初始版本发布
    - 支持 Java 8/11/17
    - 集成 SonarQube 代码检查
```

#### 版本管理
- **语义化版本**: 遵循 SemVer 规范
- **变更日志**: 详细记录每个版本的变更
- **兼容性**: 保持向后兼容性

## 4. 流水线插件系统

### 4.1 插件分类详解

蓝盾插件系统包含四大类插件，涵盖 CI/CD 全流程：

#### SCM (Source Code Management) 插件
```yaml
# Git 插件配置
- name: "代码检出"
  uses: "git@v2"
  with:
    repository: "https://git.woa.com/group/project.git"
    branch: "${{ variables.TARGET_BRANCH }}"
    depth: 1                    # 浅克隆
    submodules: true           # 包含子模块
    lfs: true                  # 支持 Git LFS
```

**主要 SCM 插件**:
- Git 拉取代码
- SVN 拉取代码
- 代码库触发器
- 合并请求检查

#### Build (构建类) 插件
```yaml
# Maven 构建插件
- name: "Maven 构建"
  uses: "maven@v3"
  with:
    goals: "clean compile test package"
    profiles: "production"
    options: |
      -DskipTests=false
      -Dmaven.test.failure.ignore=false
    settings: "${{ secrets.MAVEN_SETTINGS }}"
```

**主要构建插件**:
- Maven 构建
- Gradle 构建
- NPM 构建
- Docker 构建
- 编译加速 (TBS)

#### Deploy (部署类) 插件
```yaml
# Kubernetes 部署插件
- name: "K8s 部署"
  uses: "kubernetes@v1"
  with:
    cluster: "${{ variables.K8S_CLUSTER }}"
    namespace: "${{ variables.NAMESPACE }}"
    manifests: |
      deployment.yaml
      service.yaml
    strategy: "rolling-update"
```

**主要部署插件**:
- Kubernetes 部署
- 服务器部署
- 容器部署
- 蓝绿部署

#### Others (其他类) 插件
**主要其他插件**:
- 通知插件
- 质量检查
- 安全扫描
- 制品归档

### 4.2 插件配置最佳实践

#### 错误处理
```yaml
# 插件错误处理配置
- name: "构建应用"
  uses: "maven@v3"
  continue-on-error: false     # 失败时是否继续
  timeout: "30m"               # 超时设置
  retry:
    attempts: 3                # 重试次数
    delay: "10s"              # 重试间隔
```

#### 条件执行
```yaml
# 条件执行配置
- name: "部署到生产"
  uses: "deploy@v1"
  if: "${{ variables.BRANCH == 'master' && variables.DEPLOY_PROD == 'true' }}"
  with:
    environment: "production"
```

## 5. 流水线组与批量管理

### 5.1 流水线组功能

#### 为什么要流水线组？

当项目在不断的发展中，流水线数量日益增多，为了在繁多的流水线中让自己或项目成员便捷的定位所需求的，我们增加了流水线组功能，方便您对流水线进行分类管理。

#### 创建方式
在流水线首页，左侧默认会分为"我的流水线组"和"项目流水线组"：

**分组类型**：
- **"我的流水线组"**: 任意项目均可创建，创建后仅自己可见
- **"项目流水线组"**: 只有管理员和CI管理员可创建，创建后所有项目成员可见

#### 分组策略
流水线组分为"静态分组"和"动态分组"两种分组管理策略：

- **静态分组**: 精确手动指定在组内的流水线
- **动态分组**: 通过设置流水线名称、创建人、流水线标签等条件，动态将流水线划归进组管理

### 5.2 动态分组规则配置

```yaml
# 动态分组配置示例
group:
  name: "前端项目组"
  type: "dynamic"
  rules:
    - field: "pipeline_name"
      operator: "regex"
      value: "^frontend-.*"
    - field: "tags"
      operator: "contains"
      value: "react"
    - field: "creator"
      operator: "in"
      value: ["zhangsan", "lisi", "wangwu"]
```

**支持的操作符**:
- `equals` - 精确匹配
- `contains` - 包含字符串
- `regex` - 正则表达式匹配
- `in` - 在列表中
- `starts_with` - 以...开头
- `ends_with` - 以...结尾

### 5.3 批量管理功能

#### 批量操作类型
点击批量管理，进入"批量"操作页面，支持：
- 批量添加流水线至组
- 批量删除流水线
- 批量修改权限
- 批量执行流水线

#### 批量操作安全机制

```yaml
# 批量操作配置
batch_operation:
  type: "delete"
  targets: ["pipeline-1", "pipeline-2", "pipeline-3"]
  confirmation:
    required: true
    message: "确认删除 3 个流水线？此操作不可恢复。"
    code: "DELETE-CONFIRM"
```

**安全机制**:
- 操作确认 - 二次确认防误操作
- 权限检查 - 验证每个流水线的操作权限
- 操作日志 - 记录所有批量操作

## 6. 制品质量展示

### 6.1 质量元数据定义

流水线里，对制品进行了一系列的质量检测（比如自动化测试、安全扫描、代码质量检测、冒烟测试、签名等）之后，可以将质量检测结果标记到制品上，方便协同人员快速了解到制品质量信息，减少来回沟通成本。

#### 定义质量元数据

这一步的主要作用是确定哪些元数据，在构建详情页的小结、以及构建历史列表展示。因为制品元数据标签会有很多，全部展示太骚扰，也没有重点。

首先，需要在项目设置中，定义用于标识质量的制品元数据。

注意，元数据分组，需选择「质量」。

每一个枚举值，可以定义对应的颜色，常用的几个色值如下：

**常用质量指标**：
- 🟢 深绿 #2DCB56 - 测试通过
- 🔴 深红 #EA3636 - 测试失败
- 🟡 橙红 #FF9C01 - 部分通过
- 🔵 蓝色 #3A84FF - 待测试

### 6.2 制品质量标记

#### 在流水线中给制品打标记

**可以在归档的同时给制品打上标记**：

```yaml
# 在归档插件中标记
- name: "归档制品"
  uses: "artifactory@v1"
  with:
    path: "target/*.jar"
    metadata:
      test_result: "${{ variables.test_status }}"
      security_scan: "passed"
```

元数据的属性值，可以引用流水线变量，如 `${{ variables.autotest_result }}`。

**通过单独的「已归档制品打标签」插件进行标记**：

也可以在对制品多次加工后，通过单独的「已归档制品打标签」插件进行标记。

### 6.3 Web 端查看制品质量

#### 构建详情
在构建详情页面，构建概览部分可以看到制品质量汇总展示，如下图，代表共有 1 个制品进行了企业签名、2 个制品通过了自动化测试。

点击构建概览中的元数据标签，将自动切换到「构建制品」，并筛选了打了对应标签的制品。

#### 构建历史
构建历史界面，可以查看制品质量。

### 6.4 移动端查看制品质量

#### 执行历史
移动端蓝盾 App 也支持查看制品质量信息。

#### 执行详情
在执行详情页面也可以查看制品质量标记。

## 7. 流水线监控与告警

### 7.1 监控指标

需开启监控的项目，请先联系 O2000 助手按项目开启数据上报。默认不开启过程数据上报服务。

#### 核心监控指标

**1. pipeline_status_info**
- **描述**: 监控流水线的状态
- **标签**:
  - `pipelineId`: 流水线ID
  - `buildId`: 构建ID
  - `status`: 流水线状态（`SUCCEED`、`FAILED`、`CANCELED`等）
  - `projectId`: 项目ID
  - `triggerUser`: 触发人
  - `pipelineName`: 流水线名称
  - `trigger`: 触发类型（`TIME_TRIGGER` 定时触发、`MANUAL` 手动触发、`WEB_HOOK` 代码库触发、`REMOTE` 远程触发等）
  - `eventType`: BUILD_END

**2. pipeline_running_time_seconds**
- **描述**: 监控整个流水线的运行时间（秒）
- **标签**:
  - `pipeline_id`: 流水线ID
  - `build_id`: 构建ID
  - `projectId`: 项目ID

**3. pipeline_queue_time_seconds**
- **描述**: 监控流水线排队的运行时间（秒）
- **标签**:
  - `pipeline_id`: 流水线ID
  - `build_id`: 构建ID
  - `projectId`: 项目ID

**4. pipeline_job_running_time_seconds**
- **描述**: 监控流水线作业的运行时间（秒）
- **标签**:
  - `pipeline_id`: 流水线ID
  - `build_id`: 构建ID
  - `job_id`: Job ID
  - `projectId`: 项目ID

**5. pipeline_step_running_time_seconds**
- **描述**: 监控流水线插件的运行时间（秒）。需要流水线编排对应插件填写了STEP ID才会上报
- **标签**:
  - `pipeline_id`: 流水线ID
  - `build_id`: 构建ID
  - `step_id`: Step ID
  - `job_id`: Job ID
  - `projectId`: 项目ID

**6. pipeline_agent_running_time_seconds**
- **描述**: 监控流水线第三方构建机的运行时间（秒）
- **标签**:
  - `pipeline_id`: 流水线ID
  - `build_id`: 构建ID
  - `projectId`: 项目ID
  - `agentIp`: 构建机IP
  - `agentId`: 构建机ID
  - `nodeHashId`: 节点ID
  - `envHashId`: 构建类型为环境时，值为环境ID

### 7.2 监控配置示例

#### 监控流水线运行时间
```yaml
# 监控查询配置
query: |
  max by (pipeline_id, projectId) (
    max_over_time(
      custom:devops_build_metrics:pipeline_running_time_seconds{
        pipeline_id="$pipeline_id",
        projectId="$project_id"
      }[1m]
    )
  )
```

#### 监控流水线成功率
数据源选择事件数据：
- 数据A：`dimensions.pipelineId:"p-xxx" AND dimensions.status:"SUCCEED" AND dimensions.eventType:"BUILD_END"`
- 数据B：`dimensions.pipelineId:"p-xxx" AND dimensions.eventType:"BUILD_END"`

### 7.3 告警配置

#### 核心步骤超时告警

监控具体步骤时，对应的步骤需设置步骤 ID。

例如：
- 指标：pipeline_step_running_time_seconds  
- 流水线ID：p-aaea7859233148b9b7b26f3920a35617  
- 步骤ID：my_step
- 告警策略：my_step 执行超过 20s 告警

#### 告警通知模板

在告警信息中增加蓝盾流水线链接：

```yaml
# 告警通知配置
notification:
  template: |
    流水线执行异常告警
    项目: {{alarm.dimensions['projectId'].display_value}}
    [蓝盾流水线](https://devops.woa.com/console/pipeline/{{alarm.dimensions['projectId'].display_value}}/{{alarm.dimensions['pipeline_id'].display_value}}/detail/{{alarm.dimensions['build_id'].display_value}}/executeDetail)
    状态: {{alarm.dimensions['status'].display_value}}
```

**注意**：
- 变量引用格式为：`{{alarm.dimensions['xxx'].display_value}}`，其中 xxx 为变量名称
- 变量列表参考使用到的指标的标签字段
- 通知模版中引用到的变量，需在「数据查询」的「维度」字段范围内

## 8. 流水线跨项目迁移

### 8.1 迁移准备工作

当项目变更或组织架构调整时，可能会涉及到流水线跨项目迁移。但流水线不是一个单独的资源文件，会涉及很多以项目为边界管理的周边服务（如代码库、构建机、凭据、制品库），迁移起来比较复杂，需人工介入准备好关联资源或者通过 OPENAPI 结合项目实际的冲突解决策略等实现自定义迁移工具。

#### 资源依赖检查清单

**1. 确认目标项目构建资源满足需求**

**公共构建机**：
- 检查是否使用了自定义镜像
- 如果是选择了研发商店的镜像，检查目标项目是否安装了此镜像
- 如果是手动输入的镜像，检查目标项目下是否已经关联了对应的凭据

**私有构建机**：
- 如果使用私有构建机集群，确认目标项目是否已有对应的集群
- 如果使用私有构建机节点，确认目标项目是否已有对应的节点
- 流水线中保存的是当前项目下的节点的后台唯一ID，即使目标项目下导入了节点，流水线迁移到目标项目后，仍需重新选择节点

**2. 确认目标项目是否已关联需要的代码库资源**

流水线中，会使用 checkout 等插件拉取代码，这些插件会使用蓝盾代码库服务下关联的代码库资源。

流水线里保存的是蓝盾该项目下的代码库ID。目标项目下即使也关联了此代码库，流水线迁移到目标项目后也需要重新选择目标项目下的代码库。

**3. 确认归档制品库时，自定义仓库的路径是否存在、是否有冲突**

合理规划制品库路径，避免互相覆盖或者冲突。

**4. 确认流水线使用到的凭据资源，在目标项目是否存在**

流水线中使用的是凭据的名称，如果名称存在，即可。但需确认是不是同一个东西（命名可能相同但类型、密钥不一定相同）。

**5. 确认流水线通知是否设置了发送给用户组，目标项目下的用户组是否满足预期**

**6. 确认是否使用用子流水线调用，子流水线是否需要一起迁移**

**7. 确认流水线的权限，迁移后是否影响原来的用户使用**

### 8.2 迁移执行步骤

#### 导出流水线
在查看流水线页面，导出 Pipeline Json。

可以根据「准备工作」下梳理出来的变更点，直接修改流水线 Json 文件，或者在后边导入界面修改。

#### 在目标项目下，导入流水线
将上一步导出的 Json 文件，导入。

在导入界面，根据「准备工作」梳理出来的变更点，修改流水线。

#### 调试流水线，确认功能是否正常
保存草稿，调试。

注意：调试和实际执行是一样的，如果是一些敏感步骤，比如发布，请不要随便运行。

### 8.3 通过 OPENAPI 进行迁移

如果根据之前的「准备工作」，分析出来可以自动化修改，可以通过 OPENAPI 获取流水线，自动修改后导入目标项目。

```bash
# 使用 OpenAPI 进行批量迁移
curl -X GET "https://bkapigw.woa.com/api/devops/v4/projects/{projectId}/pipelines/{pipelineId}" \
  -H "X-Bkapi-Authorization: {\"access_token\": \"xxx\"}"
```

## 9. MCP 接入指南

### 9.1 MCP 协议支持

蓝盾支持 MCP (Model Context Protocol) 协议，通过蓝鲸API网关-MCP市场提供服务。

#### 支持接口
- `v4_user_build_status` - 查看构建状态信息
- `v4_user_build_start` - 启动构建
- `v4_user_build_list` - 获取流水线构建历史
- `v4_user_build_startInfo` - 获取流水线手动启动参数

### 9.2 MCP 客户端配置

#### 支持 MCP 协议的客户端配置

```json
{
  "mcpServers": {
    "devops-prod-pipeline-streamable": {
      "type": "streamableHttp",
      "url": "https://bk-apigateway.apigw.o.woa.com/prod/api/v2/mcp-servers/devops-prod-pipeline-streamable/mcp/",
      "description": "蓝盾流水线构建相关MCP工具",
      "headers": {
        "X-Bkapi-Authorization": "{\"access_token\": \"xxx\"}"
      }
    }
  }
}
```

#### 鉴权方式
目前 MCP proxy 接入了蓝鲸 API 网关，目前需要进行 `用户认证` 和 `应用认证` 双重认证。

推荐使用 `access_token`，有效期较长：

```bash
X-Bkapi-Authorization: {"access_token": "xxx"}
```

### 9.3 第一次使用指南

#### 1. 创建外链应用
访问蓝鲸开发者中心创建外链应用。

#### 2. 获取 access_token
点击【云API权限】 -> 【创建新令牌】获取 access_token。

#### 3. 申请 mcp server
申请 mcp server 权限。

#### 4. 联系审批
联系 o2000 助手进行审批。

### 9.4 进阶使用

已提供基础 Skills 模板，可参照 Readme 文件配置。

**解答**：
- 问：是以谁的身份请求的接口？
- 答：access_token 对应用户的身份

## 最佳实践

### 1. 流水线设计原则

#### 单一职责原则
- 每个流水线专注一个主要功能
- 避免在一个流水线中混合多种环境的部署

#### 可重用性原则
- 使用模板标准化常用流程
- 通过变量参数化不同环境的配置

#### 可观测性原则
- 添加充分的日志输出
- 配置关键步骤的监控告警
- 使用制品质量标记跟踪质量状态

### 2. 性能优化建议

#### 并发优化
```yaml
# 合理设置并发数
jobs:
  test:
    strategy:
      matrix:
        node: [12, 14, 16]
      max-parallel: 3  # 避免资源竞争
```

#### 缓存策略
```yaml
# 使用构建缓存
- name: "缓存依赖"
  uses: "cache@v1"
  with:
    path: "~/.m2/repository"
    key: "maven-${{ hashFiles('pom.xml') }}"
```

#### 资源复用
```yaml
# Agent 复用配置
jobs:
  build:
    runs-on:
      reuse-strategy: "REUSE_JOB_ID"
      max-reuse-count: 5
```

### 3. 安全最佳实践

#### 凭据管理
- 使用蓝盾凭据管理，避免硬编码密钥
- 定期轮换敏感凭据
- 最小权限原则分配凭据访问权限

#### 权限控制
- 生产环境流水线限制执行权限
- 使用项目流水线组进行批量权限管理
- 定期审计流水线权限配置

## 常见问题

### Q1: 流水线执行失败如何排查？

**排查步骤**：
1. 查看构建日志定位具体失败步骤
2. 检查变量配置是否正确
3. 验证构建环境和依赖是否满足要求
4. 查看监控指标确认资源使用情况

### Q2: 如何提高流水线执行效率？

**优化方案**：
1. 使用并发执行减少总体时间
2. 配置构建缓存避免重复下载
3. 选择合适的构建机规格
4. 优化插件配置减少不必要的操作

### Q3: 流水线变量不生效怎么办？

**检查要点**：
1. 变量名拼写是否正确
2. 变量作用域是否匹配使用位置
3. 变量引用语法是否正确
4. 是否存在变量覆盖问题

### Q4: 如何修改流水线名称？

可以在流水线设置页面修改流水线名称。

### Q5: 如何删除流水线？

在流水线操作菜单中选择删除，需要确认操作。

---

*本文档基于蓝盾最新版本编写，如有疑问请联系蓝盾技术支持团队。*