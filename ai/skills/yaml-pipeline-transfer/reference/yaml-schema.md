# YAML Schema 完整结构

BK-CI 流水线 YAML 的完整配置字段说明。

---

## 完整 YAML 结构示例

```yaml
version: v2.0                    # 版本号（必填：v2.0 / v3.0）

name: CI Pipeline                # 流水线名称
desc: 持续集成流水线描述           # 描述
label:                           # 标签
  - backend
  - production

# ========== 触发器配置 ==========
on:
  push:                          # 推送触发
    branches:
      - master
      - develop
      - /^feature\/.*/
    paths:                       # 路径过滤
      - src/**
      - build.gradle
    paths-ignore:                # 路径排除
      - docs/**
      - "*.md"

  mr:                            # 合并请求触发
    target-branches:
      - master
    action:
      - open
      - update
      - close
    block-mr: true               # 阻塞 MR
    report-commit-check: true    # 上报提交检查

  tag:                           # Tag 触发
    tags:
      - /^v.*/

  schedules:                     # 定时触发
    - cron: "0 2 * * *"          # Cron 表达式
      always: true               # 总是执行
      branches:
        - master
    - interval:                  # 固定时间触发
        week:
          - Mon
          - Fri
        time-points:
          - "02:00"
          - "14:00"

  manual:                        # 手动触发
    enable: true
    use-latest-parameters: true  # 使用最近一次参数

  remote:                        # 远程触发
    enable: true

# ========== 变量定义 ==========
variables:
  BUILD_TYPE:                    # 简单变量
    value: release
    readonly: false
    allow-modify-at-startup: true
    as-instance-input: true

  DEPLOY_ENV:                    # 枚举变量
    value: prod
    props:
      type: enum
      options:
        - dev
        - test
        - prod
      label: "部署环境"
      description: "选择部署的目标环境"

  API_TOKEN:                     # 密码变量
    value: ""
    props:
      type: password
      label: "API Token"

  VERSION_NUMBER:                # 数字变量
    value: 1
    props:
      type: number
      min: 1
      max: 100

# ========== 并发控制 ==========
concurrency:
  group: ${{ variables.BUILD_TYPE }}  # 并发组
  cancel-in-progress: true             # 取消进行中的构建
  queue-length: 10                     # 队列长度
  queue-timeout-minutes: 30            # 队列超时（分钟）
  max-parallel: 5                      # 最大并发数

# ========== 资源池配置 ==========
resources:
  repositories:                  # 代码库资源
    - repository: my-repo
      type: github
      name: my-org/my-repo
      ref: main
  pools:                         # 构建池
    - pool: my-pool
      container: linux

# ========== 模板引用 ==========
extends:
  template: templates/base.yml   # 模板路径
  parameters:                    # 模板参数
    BUILD_TYPE: ${{ variables.BUILD_TYPE }}

# ========== Stage 定义 ==========
stages:
  - name: Build                  # Stage 名称
    label:                       # Stage 标签
      - compile
    if: ${{ eq(variables.BUILD_TYPE, 'release') }}  # 执行条件
    if-modify:                   # 路径变更条件
      - src/**
    check-in: manual             # 准入审核
    check-out: manual            # 准出审核
    fast-kill: true              # 快速终止
    jobs:                        # Job 列表
      compile:                   # Job ID
        name: 编译构建            # Job 名称
        runs-on: linux           # 运行环境
        if: success()            # 执行条件
        timeout-minutes: 60      # 超时时间
        continue-on-error: false # 失败继续
        strategy:                # 矩阵策略
          matrix:
            os: [linux, windows]
            node: [14, 16, 18]
          fail-fast: true
        env:                     # 环境变量
          NODE_ENV: production
        steps:                   # 步骤列表
          - uses: checkout@v2
            with:
              repository: ${{ resources.repositories.my-repo }}
              ref: ${{ on.push.branch }}
              fetch-depth: 1
              submodules: false
              lfs: false
              enable-git-clean: true

          - name: Build
            run: |
              echo "Building..."
              ./gradlew build
            if: success()
            continue-on-error: false
            timeout-minutes: 30
            retry-times: 3

          - name: Upload Artifact
            uses: upload-artifact@v2
            with:
              name: build-output
              path: build/
              retention-days: 7

          - template: templates/deploy-step.yml
            parameters:
              env: prod

          - name: Manual Review
            uses: manual-review@v1
            with:
              desc: "请审核构建产物"
              reviewers:
                - user1
                - user2
              notify-type: [email, wechat]

  - name: Deploy
    label:
      - deployment
    depends-on:                  # 依赖的 Stage
      - Build
    jobs:
      deploy:
        name: 部署
        runs-on:
          pool: prod-pool        # 指定构建池
        steps:
          - name: Download Artifact
            uses: download-artifact@v2
            with:
              name: build-output
          - name: Deploy
            run: ./deploy.sh

# ========== Finally Stage ==========
finally:                         # 最终执行（无论成功失败）
  cleanup:
    name: 清理
    runs-on: linux
    if: always()                 # 总是执行
    steps:
      - name: Cleanup
        run: rm -rf temp/

# ========== 通知配置 ==========
notices:
  - notify-type: [email, wechat] # 通知类型
    notify-when: [fail]          # 通知时机
    notify-group: [开发组]        # 通知组
    notify-user: [user1]         # 通知用户
    content: "构建失败，请及时处理"
    title: "流水线失败通知"

# ========== 其他配置 ==========
disable-pipeline: false          # 禁用流水线
custom-build-num: ${{ variables.VERSION_NUMBER }}  # 自定义构建号
syntax-dialect: CLASSIC          # 语法方言（CLASSIC/CONSTRAINT）
fail-if-variable-invalid: false  # 变量无效时失败
cancel-policy: SIMPLE            # 取消策略

# ========== 推荐版本 ==========
recommended-version:
  enabled: true
  version: "1.0.0"
  reason: "稳定版本"
```

---

## 字段说明索引

### 顶级字段

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `version` | string | 是 | YAML 版本（`v2.0` / `v3.0`） |
| `name` | string | 推荐 | 流水线名称 |
| `desc` | string | 否 | 流水线描述 |
| `label` | list | 否 | 标签列表 |
| `on` | object | 否 | 触发器配置 |
| `variables` | map | 否 | 变量定义 |
| `concurrency` | object | 否 | 并发控制 |
| `resources` | object | 否 | 资源池配置 |
| `extends` | object | 否 | 模板继承 |
| `stages` | list | 是 | Stage 列表 |
| `finally` | map | 否 | 最终执行的 Job |
| `notices` | list | 否 | 通知配置 |
| `disable-pipeline` | boolean | 否 | 禁用流水线 |
| `custom-build-num` | string | 否 | 自定义构建号 |
| `syntax-dialect` | string | 否 | 语法方言（CLASSIC/CONSTRAINT） |
| `fail-if-variable-invalid` | boolean | 否 | 变量无效时失败 |
| `cancel-policy` | string | 否 | 取消策略（SIMPLE） |
| `recommended-version` | object | 否 | 推荐版本配置 |

### 触发器字段（on）

| 字段 | 说明 |
|------|------|
| `on.push` | 推送触发，支持 `branches`、`paths`、`paths-ignore` |
| `on.mr` | 合并请求触发，支持 `target-branches`、`action`、`block-mr`、`report-commit-check` |
| `on.tag` | Tag 触发，支持 `tags` 正则匹配 |
| `on.schedules` | 定时触发，支持 `cron` 表达式和 `interval` 固定时间 |
| `on.manual` | 手动触发，支持 `use-latest-parameters` |
| `on.remote` | 远程触发 |

### 变量字段（variables）

| 字段 | 说明 |
|------|------|
| `value` | 默认值 |
| `readonly` | 是否只读 |
| `allow-modify-at-startup` | 是否允许启动时修改 |
| `as-instance-input` | 是否作为实例输入 |
| `props.type` | 变量类型（enum / password / number） |
| `props.options` | 枚举选项列表 |
| `props.label` | 显示标签 |
| `props.description` | 变量描述 |
| `props.min` / `props.max` | 数字范围 |

### Job 字段

| 字段 | 说明 |
|------|------|
| `name` | Job 名称 |
| `runs-on` | 运行环境（linux/windows/macos/agent-id/agent-name/pool/self-hosted） |
| `if` | 执行条件表达式 |
| `timeout-minutes` | 超时时间（分钟） |
| `continue-on-error` | 失败是否继续 |
| `strategy.matrix` | 矩阵策略 |
| `strategy.fail-fast` | 快速失败 |
| `env` | 环境变量 |
| `steps` | 步骤列表 |

### Step 字段

| 字段 | 说明 |
|------|------|
| `name` | 步骤名称 |
| `uses` | 插件引用（`checkout@v2`、`atomCode@version`） |
| `run` | 脚本内容 |
| `template` | 步骤模板路径 |
| `with` | 参数传入 |
| `if` | 执行条件 |
| `continue-on-error` | 失败是否继续 |
| `timeout-minutes` | 超时时间 |
| `retry-times` | 重试次数 |
| `parameters` | 模板参数 |
