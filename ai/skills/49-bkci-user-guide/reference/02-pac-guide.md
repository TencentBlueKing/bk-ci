# Pipeline as Code (PAC) 完整使用指南

## 概述

Pipeline as Code (PAC) 是蓝盾提供的代码化流水线功能，允许用户通过 YAML 文件定义和管理流水线。PAC 将流水线配置与代码一起进行版本控制，实现基础设施即代码的最佳实践。

## 核心优势

### 1. 版本控制
- **配置即代码**: 流水线配置与业务代码一起管理
- **变更追踪**: 通过 Git 历史追踪流水线配置变更
- **分支隔离**: 不同分支可以有不同的流水线配置
- **回滚能力**: 可以快速回滚到历史版本的配置

### 2. 协作效率
- **代码审查**: 流水线变更通过 MR/PR 进行审查
- **团队协作**: 多人可以同时编辑不同的流水线配置
- **知识共享**: 流水线配置成为团队知识资产

### 3. 灵活性
- **条件执行**: 支持复杂的条件判断和分支逻辑
- **模板复用**: 支持流水线模板和片段复用
- **动态配置**: 支持基于变量的动态配置

## PAC 快速上手

### 1. 创建 PAC 流水线

#### 方法一：从 UI 转换
1. 在蓝盾中创建并编排好流水线
2. 进入流水线编辑页面，点击"PAC 模式"
3. 选择代码库和文件路径
4. 系统自动生成 YAML 文件并提交到代码库

#### 方法二：直接编写 YAML
1. 在代码库根目录创建 `.ci` 目录
2. 在 `.ci` 目录下创建 YAML 文件（如 `pipeline.yml`）
3. 编写流水线配置
4. 提交代码触发流水线

### 2. YAML 文件结构

```yaml
# 基础结构示例
version: v3.0                    # YAML 版本

name: "我的流水线"                # 流水线名称

on:                              # 触发器配置
  push:
    branches: ["master"]
  mr:
    target-branches: ["master"]

variables:                       # 变量定义
  APP_NAME: "my-app"
  DEPLOY_ENV: "dev"

stages:                          # 阶段定义
  - name: "构建阶段"
    jobs:
      build:
        name: "构建任务"
        runs-on: linux
        steps:
          - name: "检出代码"
            uses: checkout@v1
          - name: "构建应用"
            run: |
              echo "构建 ${{ variables.APP_NAME }}"

notices:                         # 通知配置
  - type: wechat-message
```

## YAML 语法详解

### 1. 顶级关键字

#### version
指定 YAML 语法版本，当前推荐使用 `v3.0`：

```yaml
version: v3.0
```

#### name
流水线名称，支持中文：

```yaml
name: "前端项目构建流水线"
```

#### label
流水线标签，用于分类管理：

```yaml
label: 
  - "frontend"
  - "production"
```

#### concurrency
并发控制配置：

```yaml
concurrency:
  group: ${{ ci.branch }}           # 并发组名
  cancel-in-progress: true          # 新任务进入时取消正在运行的任务
  queue-length: 5                   # 队列长度
  queue-timeout-minutes: 30         # 队列超时时间
```

#### custom-build-num
自定义构建号格式：

```yaml
custom-build-num: ${{ DATE:"yyyyMMdd" }}.${{ BUILD_NO_OF_DAY }}
```

### 2. 触发器配置 (on)

#### push 触发器
监听代码推送事件：

```yaml
on:
  push:
    branches:                     # 监听的分支
      - master
      - develop
      - release/**              # 支持通配符
    branches-ignore:              # 排除的分支
      - feature/temp-*
    paths:                        # 监听的路径
      - src/**
      - package.json
    paths-ignore:                 # 排除的路径
      - docs/**
      - "*.md"
    users:                        # 指定用户
      - zhangsan
      - lisi
    users-ignore:                 # 排除用户
      - bot-user
    action:                       # 动作类型
      - push-file                 # 文件变更
      - new-branch               # 新建分支
```

#### tag 触发器
监听标签推送事件：

```yaml
on:
  tag:
    tags:                         # 监听的标签
      - v*.*.*
      - release-*
    tags-ignore:                  # 排除的标签
      - beta-*
    from-branches:                # 来源分支
      - master
      - release/**
    users:                        # 指定用户
      - release-manager
```

#### mr 触发器
监听合并请求事件：

```yaml
on:
  mr:
    target-branches:              # 目标分支
      - master
      - develop
    source-branches-ignore:       # 排除源分支
      - temp-*
    paths:                        # 监听路径
      - src/**
    action:                       # MR 动作
      - open                      # 创建 MR
      - reopen                    # 重新打开
      - push-update              # 源分支更新
      - merge                     # 合并完成
    report-commit-check: true     # 上报检查结果
    block-mr: true               # 失败时阻止合并
```

#### schedules 触发器
定时任务触发：

```yaml
on:
  schedules:
    cron: "0 2 * * *"            # 每天凌晨2点
    branches: ["master"]          # 执行分支
    always: false                 # 仅代码变更时执行
```

#### 复合触发器
支持多种触发器组合：

```yaml
on:
  push:
    branches: ["master"]
  mr:
    target-branches: ["master"]
  schedules:
    cron: "0 2 * * 1"            # 每周一凌晨2点
    branches: ["master"]
```

### 3. 变量系统 (variables)

#### 基础变量定义

```yaml
variables:
  # 简单变量
  APP_NAME: "my-application"
  
  # 完整变量配置
  DEPLOY_ENV:
    value: "dev"                  # 默认值
    readonly: false               # 运行时是否只读
    allow-modify-at-startup: true # 是否为启动参数
    
  # 常量定义
  API_VERSION:
    value: "v1.0"
    const: true                   # 标记为常量
```

#### 启动参数配置

```yaml
variables:
  # 文本输入框
  app_version:
    value: "1.0.0"
    props:
      type: vuex-input
      label: "应用版本"
      required: true
      description: "请输入应用版本号"
      
  # 多行文本框
  release_notes:
    value: ""
    props:
      type: vuex-textarea
      label: "发布说明"
      
  # 下拉选择框
  deploy_env:
    value: "dev"
    props:
      type: selector
      label: "部署环境"
      options:
        - id: "dev"
          label: "开发环境"
        - id: "test"
          label: "测试环境"
        - id: "prod"
          label: "生产环境"
          
  # 布尔选择
  enable_test:
    value: true
    props:
      type: boolean
      label: "启用测试"
      
  # 复选框
  test_types:
    value: "unit,integration"
    props:
      type: checkbox
      label: "测试类型"
      options:
        - id: "unit"
          label: "单元测试"
        - id: "integration"
          label: "集成测试"
        - id: "e2e"
          label: "端到端测试"
```

#### 动态选项配置

```yaml
variables:
  pipeline_id:
    value: ""
    props:
      type: selector
      label: "选择流水线"
      payload:
        type: remote
        url: "https://devops.woa.com/api/projects/demo/pipelines"
        dataPath: "data.records"
        paramId: "pipelineId"
        paramName: "pipelineName"
```

#### 代码库相关变量

```yaml
variables:
  # Git 分支选择
  git_branch:
    value: "master"
    props:
      type: git-ref
      label: "Git 分支"
      repo-id: "abc123"
      
  # 代码库选择
  repository:
    value: "group/repo-name"
    props:
      type: code-lib
      label: "代码库"
      scm-type: git
      
  # 代码库和分支组合
  repo_branch:
    value:
      repo-name: "group/repo"
      branch: "master"
    props:
      type: repo-ref
      label: "代码库分支"
```

### 4. 流水线编排 (stages)

#### Stage 配置

```yaml
stages:
  - name: "构建阶段"
    enable: true                  # 是否启用
    fast-kill: false             # 快速终止
    if: ${{ variables.ENABLE_BUILD == 'true' }}  # 执行条件
    jobs:
      # Job 配置...
```

#### Job 配置

```yaml
jobs:
  build:
    name: "构建任务"
    enable: true                  # 是否启用
    runs-on: linux              # 运行环境
    if: ${{ ci.branch == 'master' }}  # 执行条件
    timeout-minutes: 60          # 超时时间
    depends-on: ["setup"]        # 依赖的 Job
    strategy:                    # 矩阵策略
      matrix:
        node-version: [14, 16, 18]
        os: [ubuntu, windows]
    steps:
      # 步骤配置...
```

#### 构建环境配置

```yaml
jobs:
  build:
    runs-on:
      # 公共构建机
      pool-name: "linux"
      
      # 或指定镜像
      image: "node:16-alpine"
      
      # 或私有构建机
      agent-selector:
        - "env=prod"
        - "type=docker"
        
      # 环境复用配置
      reuse-strategy: "REUSE_JOB_ID"
      max-reuse-count: 5
```

#### 步骤配置

```yaml
steps:
  # 使用插件
  - name: "检出代码"
    uses: checkout@v1
    with:
      repository: ${{ variables.repo_url }}
      branch: ${{ ci.branch }}
      
  # 执行脚本
  - name: "构建应用"
    run: |
      npm install
      npm run build
    shell: bash
    
  # 条件执行
  - name: "部署到生产"
    uses: deploy@v1
    if: ${{ ci.branch == 'master' }}
    with:
      environment: "production"
      
  # 错误处理
  - name: "清理环境"
    run: docker system prune -f
    if: always()                 # 总是执行
    
  # 超时控制
  - name: "长时间任务"
    run: ./long-running-task.sh
    timeout-minutes: 30
```

### 5. 条件执行控制

#### 内置条件函数

```yaml
steps:
  - name: "成功时执行"
    run: echo "前面步骤成功"
    if: success()
    
  - name: "失败时执行"
    run: echo "前面步骤失败"
    if: failure()
    
  - name: "总是执行"
    run: echo "无论如何都执行"
    if: always()
    
  - name: "取消时不执行"
    run: echo "未被取消时执行"
    if: "!cancelled()"
```

#### 变量条件判断

```yaml
steps:
  - name: "生产环境部署"
    run: ./deploy-prod.sh
    if: ${{ variables.DEPLOY_ENV == 'prod' }}
    
  - name: "多条件判断"
    run: ./complex-task.sh
    if: ${{ variables.ENABLE_DEPLOY == 'true' && ci.branch == 'master' }}
    
  - name: "包含判断"
    run: ./feature-task.sh
    if: ${{ contains(ci.branch, 'feature/') }}
```

#### 复杂表达式

```yaml
steps:
  - name: "复杂条件"
    run: ./conditional-task.sh
    if: |
      ${{
        (variables.DEPLOY_ENV == 'prod' && ci.branch == 'master') ||
        (variables.DEPLOY_ENV == 'test' && startsWith(ci.branch, 'release/'))
      }}
```

### 6. 并发控制策略

#### 矩阵策略

```yaml
jobs:
  test:
    strategy:
      matrix:
        node-version: [14, 16, 18]
        os: [ubuntu-latest, windows-latest]
        include:
          - node-version: 20
            os: ubuntu-latest
        exclude:
          - node-version: 14
            os: windows-latest
      max-parallel: 3            # 最大并发数
      fail-fast: false          # 不快速失败
    runs-on: ${{ matrix.os }}
    steps:
      - uses: setup-node@v1
        with:
          node-version: ${{ matrix.node-version }}
```

#### Job 依赖关系

```yaml
jobs:
  setup:
    name: "环境准备"
    steps:
      - run: echo "准备环境"
      
  build:
    name: "构建"
    depends-on: ["setup"]
    steps:
      - run: echo "开始构建"
      
  test:
    name: "测试"
    depends-on: ["build"]
    steps:
      - run: echo "开始测试"
      
  deploy:
    name: "部署"
    depends-on: ["test"]
    if: ${{ ci.branch == 'master' }}
    steps:
      - run: echo "开始部署"
```

### 7. 通知配置 (notices)

#### 企业微信通知

```yaml
notices:
  - type: wechat-message
    when: always                 # 通知时机：always/success/failure
    title: "构建通知"
    content: |
      流水线：${{ ci.pipeline_name }}
      分支：${{ ci.branch }}
      状态：${{ ci.build_status }}
      构建号：#${{ ci.build_num }}
    receivers:
      - "zhangsan"
      - "lisi"
```

#### 邮件通知

```yaml
notices:
  - type: email
    when: failure
    title: "构建失败通知"
    content: |
      项目：${{ ci.project_name }}
      流水线：${{ ci.pipeline_name }}
      失败原因：请查看构建日志
    receivers:
      - "dev-team@company.com"
```

#### 条件通知

```yaml
notices:
  - type: wechat-message
    when: always
    if: ${{ ci.branch == 'master' }}
    title: "主分支构建通知"
    content: "主分支构建完成"
```

## 流水线模板

### 1. 模板定义

```yaml
# templates/base-template.yml
version: v3.0

name: "基础模板"

parameters:
  - name: app_name
    type: string
    required: true
    default: "my-app"
  - name: node_version
    type: string
    default: "16"

stages:
  - name: "构建"
    jobs:
      build:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-node@v1
            with:
              node-version: ${{ parameters.node_version }}
          - run: |
              npm install
              npm run build
```

### 2. 模板使用

```yaml
# .ci/pipeline.yml
version: v3.0

extends:
  template: templates/base-template.yml
  parameters:
    app_name: "frontend-app"
    node_version: "18"

# 可以覆盖或扩展模板配置
variables:
  CUSTOM_VAR: "value"

stages:
  - name: "部署"
    jobs:
      deploy:
        runs-on: linux
        steps:
          - run: echo "部署应用"
```

### 3. 模板片段复用

```yaml
# 定义可复用的步骤片段
.setup_node: &setup_node
  - uses: setup-node@v1
    with:
      node-version: "16"
  - run: npm ci

jobs:
  test:
    steps:
      - uses: checkout@v1
      - <<: *setup_node          # 引用片段
      - run: npm test
      
  build:
    steps:
      - uses: checkout@v1
      - <<: *setup_node          # 复用相同片段
      - run: npm run build
```

## 实用场景案例

### 1. 前端项目 CI/CD

```yaml
version: v3.0

name: "前端项目流水线"

on:
  push:
    branches: ["master", "develop"]
  mr:
    target-branches: ["master"]

variables:
  NODE_VERSION: "16"
  DEPLOY_ENV:
    value: "dev"
    props:
      type: selector
      options:
        - id: "dev"
          label: "开发环境"
        - id: "prod"
          label: "生产环境"

stages:
  - name: "代码检查"
    jobs:
      lint:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-node@v1
            with:
              node-version: ${{ variables.NODE_VERSION }}
          - run: |
              npm ci
              npm run lint
              npm run type-check

  - name: "测试"
    jobs:
      unit-test:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-node@v1
            with:
              node-version: ${{ variables.NODE_VERSION }}
          - run: |
              npm ci
              npm run test:unit
          - uses: upload-test-report@v1
            with:
              path: "coverage/"

  - name: "构建"
    jobs:
      build:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-node@v1
            with:
              node-version: ${{ variables.NODE_VERSION }}
          - run: |
              npm ci
              npm run build
          - uses: upload-artifact@v1
            with:
              name: "dist"
              path: "dist/"

  - name: "部署"
    if: ${{ ci.branch == 'master' }}
    jobs:
      deploy:
        runs-on: linux
        steps:
          - uses: download-artifact@v1
            with:
              name: "dist"
          - uses: deploy-to-server@v1
            with:
              environment: ${{ variables.DEPLOY_ENV }}
              path: "dist/"

notices:
  - type: wechat-message
    when: always
    title: "前端构建通知"
    content: |
      项目：${{ ci.project_name }}
      分支：${{ ci.branch }}
      状态：${{ ci.build_status }}
```

### 2. 后端微服务 CI/CD

```yaml
version: v3.0

name: "后端微服务流水线"

on:
  push:
    branches: ["master", "develop"]
    paths: ["src/**", "pom.xml"]
  mr:
    target-branches: ["master"]

variables:
  JAVA_VERSION: "11"
  SERVICE_NAME: "user-service"
  IMAGE_TAG: "${{ ci.branch }}-${{ ci.build_num }}"

stages:
  - name: "代码质量"
    jobs:
      code-check:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-java@v1
            with:
              java-version: ${{ variables.JAVA_VERSION }}
          - run: |
              mvn clean compile
              mvn checkstyle:check
              mvn spotbugs:check
          - uses: codecc@v1
            with:
              language: "java"

  - name: "测试"
    jobs:
      unit-test:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-java@v1
            with:
              java-version: ${{ variables.JAVA_VERSION }}
          - run: |
              mvn clean test
              mvn jacoco:report
          - uses: upload-test-report@v1
            with:
              path: "target/site/jacoco/"

  - name: "构建镜像"
    jobs:
      build-image:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: setup-java@v1
            with:
              java-version: ${{ variables.JAVA_VERSION }}
          - run: |
              mvn clean package -DskipTests
          - uses: docker-build@v1
            with:
              dockerfile: "Dockerfile"
              image-name: "${{ variables.SERVICE_NAME }}"
              image-tag: ${{ variables.IMAGE_TAG }}
          - uses: docker-push@v1
            with:
              image: "${{ variables.SERVICE_NAME }}:${{ variables.IMAGE_TAG }}"

  - name: "部署"
    if: ${{ ci.branch == 'master' }}
    jobs:
      deploy-k8s:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - uses: kubectl-deploy@v1
            with:
              namespace: "production"
              image: "${{ variables.SERVICE_NAME }}:${{ variables.IMAGE_TAG }}"
              config-file: "k8s/deployment.yaml"
```

### 3. 多环境部署流水线

```yaml
version: v3.0

name: "多环境部署流水线"

on:
  push:
    branches: ["develop", "release/*", "master"]

variables:
  APP_NAME: "my-app"

stages:
  - name: "构建"
    jobs:
      build:
        runs-on: linux
        steps:
          - uses: checkout@v1
          - run: |
              docker build -t ${{ variables.APP_NAME }}:${{ ci.build_num }} .
          - uses: docker-push@v1

  - name: "开发环境部署"
    if: ${{ ci.branch == 'develop' }}
    jobs:
      deploy-dev:
        runs-on: linux
        steps:
          - uses: deploy@v1
            with:
              environment: "development"
              image: "${{ variables.APP_NAME }}:${{ ci.build_num }}"

  - name: "测试环境部署"
    if: ${{ startsWith(ci.branch, 'release/') }}
    jobs:
      deploy-test:
        runs-on: linux
        steps:
          - uses: deploy@v1
            with:
              environment: "testing"
              image: "${{ variables.APP_NAME }}:${{ ci.build_num }}"

  - name: "生产环境部署"
    if: ${{ ci.branch == 'master' }}
    jobs:
      deploy-prod:
        runs-on: linux
        steps:
          - name: "人工审核"
            uses: manual-approval@v1
            with:
              approvers: ["admin", "ops-team"]
              timeout-minutes: 60
          - uses: deploy@v1
            with:
              environment: "production"
              image: "${{ variables.APP_NAME }}:${{ ci.build_num }}"
```

## 表达式和上下文

### 1. 内置上下文

#### ci 上下文
```yaml
steps:
  - run: |
      echo "项目ID: ${{ ci.project_id }}"
      echo "流水线ID: ${{ ci.pipeline_id }}"
      echo "构建号: ${{ ci.build_num }}"
      echo "分支: ${{ ci.branch }}"
      echo "提交ID: ${{ ci.commit_id }}"
      echo "提交者: ${{ ci.actor }}"
      echo "事件类型: ${{ ci.event }}"
```

#### variables 上下文
```yaml
variables:
  APP_NAME: "my-app"
  VERSION: "1.0.0"

steps:
  - run: |
      echo "应用名: ${{ variables.APP_NAME }}"
      echo "版本: ${{ variables.VERSION }}"
```

#### steps 上下文
```yaml
steps:
  - id: build
    run: |
      echo "build_result=success" >> $GITHUB_OUTPUT
      echo "artifact_path=/tmp/app.jar" >> $GITHUB_OUTPUT
      
  - run: |
      echo "构建结果: ${{ steps.build.outputs.build_result }}"
      echo "制品路径: ${{ steps.build.outputs.artifact_path }}"
```

### 2. 表达式函数

#### 字符串函数
```yaml
steps:
  - run: echo "分支包含feature: ${{ contains(ci.branch, 'feature') }}"
  - run: echo "以release开头: ${{ startsWith(ci.branch, 'release/') }}"
  - run: echo "以.js结尾: ${{ endsWith(ci.changed_files, '.js') }}"
  - run: echo "转大写: ${{ upper(variables.APP_NAME) }}"
  - run: echo "转小写: ${{ lower(variables.APP_NAME) }}"
```

#### 逻辑函数
```yaml
steps:
  - run: echo "生产环境且主分支"
    if: ${{ variables.ENV == 'prod' && ci.branch == 'master' }}
    
  - run: echo "开发或测试环境"
    if: ${{ variables.ENV == 'dev' || variables.ENV == 'test' }}
    
  - run: echo "非生产环境"
    if: ${{ variables.ENV != 'prod' }}
```

#### 类型转换
```yaml
variables:
  ENABLE_DEPLOY: "true"
  RETRY_COUNT: "3"

steps:
  - run: echo "部署启用"
    if: ${{ toBool(variables.ENABLE_DEPLOY) }}
    
  - run: echo "重试次数: ${{ toNumber(variables.RETRY_COUNT) }}"
```

## 最佳实践

### 1. 文件组织

```
.ci/
├── pipeline.yml              # 主流水线
├── pr-check.yml             # PR 检查流水线
├── release.yml              # 发布流水线
├── templates/               # 模板目录
│   ├── base.yml
│   └── deploy.yml
└── scripts/                 # 脚本目录
    ├── build.sh
    └── deploy.sh
```

### 2. 变量管理

```yaml
# 使用有意义的变量名
variables:
  # ❌ 不好的命名
  VAR1: "value"
  X: "test"
  
  # ✅ 好的命名
  APP_NAME: "user-service"
  DEPLOY_ENVIRONMENT: "production"
  DOCKER_REGISTRY: "registry.company.com"
  
  # 使用分组管理
  BUILD_NODE_VERSION: "16"
  BUILD_JAVA_VERSION: "11"
  
  DEPLOY_NAMESPACE: "default"
  DEPLOY_REPLICAS: "3"
```

### 3. 条件执行优化

```yaml
# ❌ 复杂的内联条件
steps:
  - run: ./deploy.sh
    if: ${{ variables.DEPLOY_ENV == 'prod' && ci.branch == 'master' && variables.ENABLE_DEPLOY == 'true' }}

# ✅ 使用变量简化条件
variables:
  SHOULD_DEPLOY: ${{ variables.DEPLOY_ENV == 'prod' && ci.branch == 'master' && variables.ENABLE_DEPLOY == 'true' }}

steps:
  - run: ./deploy.sh
    if: ${{ variables.SHOULD_DEPLOY }}
```

### 4. 错误处理

```yaml
jobs:
  build:
    steps:
      - name: "构建应用"
        run: |
          set -e  # 遇到错误立即退出
          npm install
          npm run build
        timeout-minutes: 10
        
      - name: "清理临时文件"
        run: rm -rf /tmp/build-*
        if: always()  # 无论成功失败都执行清理
        
      - name: "上传失败日志"
        uses: upload-artifact@v1
        if: failure()  # 仅在失败时上传日志
        with:
          name: "build-logs"
          path: "logs/"
```

### 5. 性能优化

```yaml
jobs:
  test:
    strategy:
      matrix:
        test-group: [unit, integration, e2e]
      max-parallel: 3  # 控制并发数避免资源竞争
      
    steps:
      - uses: checkout@v1
        with:
          fetch-depth: 1  # 浅克隆提高速度
          
      - uses: cache@v1  # 使用缓存
        with:
          path: ~/.npm
          key: npm-${{ hashFiles('package-lock.json') }}
          
      - run: npm ci  # 使用 ci 而不是 install
      - run: npm run test:${{ matrix.test-group }}
```

### 6. 安全最佳实践

```yaml
variables:
  # ❌ 不要在 YAML 中硬编码敏感信息
  DATABASE_PASSWORD: "password123"
  
  # ✅ 使用凭据管理
  DATABASE_URL:
    value: ""
    props:
      type: credential
      credential-type: "password"

steps:
  # ❌ 不要在日志中暴露敏感信息
  - run: echo "Password is ${{ variables.DATABASE_PASSWORD }}"
  
  # ✅ 使用安全的方式处理敏感信息
  - run: |
      # 设置敏感变量不在日志中显示
      echo "::add-mask::${{ variables.DATABASE_PASSWORD }}"
      ./deploy.sh
```

## 常见问题

### Q1: YAML 语法错误如何排查？

**排查步骤**：
1. 使用 YAML 验证工具检查语法
2. 注意缩进必须使用空格，不能使用 Tab
3. 检查引号配对和特殊字符转义
4. 查看流水线执行日志中的详细错误信息

### Q2: 变量引用不生效怎么办？

**检查要点**：
1. 变量名拼写是否正确
2. 变量作用域是否匹配使用位置
3. 表达式语法是否正确 `${{ variables.VAR_NAME }}`
4. 是否存在变量覆盖问题

### Q3: 条件执行不按预期工作？

**调试方法**：
1. 在步骤中输出条件变量的值
2. 检查条件表达式的逻辑
3. 注意字符串比较的大小写敏感性
4. 使用 `toBool()` 函数处理字符串布尔值

### Q4: 如何调试复杂的流水线？

**调试技巧**：
1. 使用 `echo` 输出关键变量值
2. 分阶段注释部分配置进行测试
3. 使用草稿模式进行调试
4. 查看详细的执行日志

## 相关资源

- [流水线基础指南](01-pipeline-guide.md)
- [蓝盾新手入门](../51-bkci-getting-started)
- [流水线插件开发](../47-pipeline-plugin-development)
- [蓝盾问题排查](../50-bkci-troubleshooting)

---

*本文档基于蓝盾最新版本编写，如有疑问请联系蓝盾技术支持团队。*