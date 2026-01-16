# 蓝盾最佳实践指南

## 概述

本文档汇总了蓝盾使用过程中的最佳实践，基于大量用户的实际经验总结，帮助你避免常见陷阱，提高开发效率。

## 🏗️ 流水线设计最佳实践

### ✅ 推荐做法

#### 1. 善用系统内置变量

```yaml
# 使用内置变量而不是硬编码
steps:
  - name: "构建信息"
    run: |
      echo "项目: ${{ ci.project_name }}"
      echo "分支: ${{ ci.branch }}"
      echo "构建号: ${{ ci.build_num }}"
      echo "提交ID: ${{ ci.commit_id }}"
      
# 而不是
steps:
  - name: "构建信息"
    run: |
      echo "项目: my-project"  # ❌ 硬编码
      echo "分支: main"        # ❌ 硬编码
```

#### 2. 合理使用 Stage 准入审批

```yaml
# ✅ 推荐：在 Stage 级别设置审批
stages:
  - name: "构建测试"
    jobs: [...]
    
  - name: "生产部署"
    # 在 Stage 准入设置审批，而不是在插件中
    approval:
      required: true
      approvers: ["admin", "ops-team"]
    jobs:
      - name: "部署到生产"
        steps: [...]
```

#### 3. 使用相对路径

```yaml
# ✅ 推荐：使用相对路径
steps:
  - name: "构建"
    run: |
      cd src
      ./build.sh
      cp target/*.jar ../artifacts/

# ❌ 避免：使用绝对路径
steps:
  - name: "构建"
    run: |
      cd /data/workspace/src  # 环境依赖性强
      /usr/local/bin/build.sh
```

#### 4. 优先使用平台模板

```yaml
# ✅ 推荐：使用官方或团队模板
# 在创建流水线时选择合适的模板，而不是从零开始

# 常用模板类型：
# - Java Maven 项目模板
# - Node.js 项目模板  
# - Docker 构建模板
# - 多环境部署模板
```

#### 5. 合理拆分步骤

```yaml
# ✅ 推荐：步骤拆分细致
steps:
  - name: "检出代码"
    uses: checkout@latest
    
  - name: "安装依赖"
    run: npm ci
    
  - name: "代码检查"
    run: npm run lint
    
  - name: "运行测试"
    run: npm test
    
  - name: "构建项目"
    run: npm run build

# ❌ 避免：所有操作放在一个步骤
steps:
  - name: "构建和测试"
    run: |
      npm ci
      npm run lint
      npm test
      npm run build
      # 问题：失败时难以定位具体环节
```

#### 6. 使用 Stage 标签

```yaml
# ✅ 推荐：为 Stage 添加标签
stages:
  - name: "代码检查"
    label: ["CodeCheck"]
    jobs: [...]
    
  - name: "单元测试"
    label: ["Test"]
    jobs: [...]
    
  - name: "构建打包"
    label: ["Build"]
    jobs: [...]
    
  - name: "部署发布"
    label: ["Deploy"]
    jobs: [...]
```

#### 7. 及时关停无用流水线

```yaml
# 定期检查和清理：
# - 已废弃的流水线
# - 测试用的临时流水线
# - 长期未使用的流水线

# 设置流水线状态为"禁用"而不是直接删除
# 便于后续需要时重新启用
```

### ❌ 不建议做法

#### 1. 避免外网依赖

```yaml
# ❌ 避免：直接使用外网依赖
steps:
  - name: "安装依赖"
    run: |
      npm install --registry=https://registry.npmjs.org/
      pip install -i https://pypi.org/simple/

# ✅ 推荐：使用内网镜像
steps:
  - name: "安装依赖"
    run: |
      npm install --registry=https://mirrors.tencent.com/npm/
      pip install -i https://mirrors.tencent.com/pypi/simple/
```

#### 2. 避免硬编码敏感信息

```yaml
# ❌ 避免：硬编码密码和密钥
steps:
  - name: "部署"
    run: |
      scp -i ~/.ssh/id_rsa app.jar user@server:/app/
      mysql -h db.example.com -u root -ppassword123

# ✅ 推荐：使用凭据管理
steps:
  - name: "部署"
    run: |
      scp -i ${{ credentials.ssh_key }} app.jar ${{ credentials.deploy_user }}@${{ variables.server }}:/app/
      mysql -h ${{ variables.db_host }} -u ${{ credentials.db_user }} -p${{ credentials.db_password }}
```

#### 3. 避免长时间运行步骤

```yaml
# ❌ 避免：在流水线中放置长时间任务
steps:
  - name: "人工审批"
    # 长时间等待会占用构建资源
    uses: manual-approval@1.*
    
  - name: "长时间测试"
    run: |
      # 超过2小时的测试任务
      ./long-running-test.sh

# ✅ 推荐：优化或分离长时间任务
steps:
  - name: "触发异步测试"
    run: |
      # 触发异步测试任务
      curl -X POST ${{ variables.test_api }}/trigger
      
  - name: "快速冒烟测试"
    run: |
      # 只运行关键的快速测试
      ./smoke-test.sh
```

## 🔧 性能优化最佳实践

### 构建性能优化

#### 1. 使用构建缓存

```yaml
# Maven 项目缓存
steps:
  - name: "缓存 Maven 依赖"
    uses: cache@2.*
    with:
      path: ~/.m2/repository
      key: maven-${{ hashFiles('**/pom.xml') }}
      
  - name: "Maven 构建"
    run: mvn clean package

# Node.js 项目缓存
steps:
  - name: "缓存 Node 模块"
    uses: cache@2.*
    with:
      path: node_modules
      key: node-${{ hashFiles('package-lock.json') }}
      
  - name: "安装依赖"
    run: npm ci
```

#### 2. 并行执行

```yaml
# ✅ 推荐：并行执行独立任务
stages:
  - name: "并行构建"
    jobs:
      - name: "前端构建"
        steps:
          - uses: checkout@latest
          - run: npm run build
          
      - name: "后端构建"  
        steps:
          - uses: checkout@latest
          - run: mvn package
          
      - name: "文档构建"
        steps:
          - uses: checkout@latest
          - run: mkdocs build
```

#### 3. 选择合适的构建机

```yaml
# 根据任务特点选择构建机规格
jobs:
  - name: "轻量级任务"
    runs-on: linux-small    # 1核2G
    steps: [...]
    
  - name: "编译任务"
    runs-on: linux-large    # 4核8G
    steps: [...]
    
  - name: "大型测试"
    runs-on: linux-xlarge   # 8核16G
    steps: [...]
```

### 资源使用优化

#### 1. 合理设置超时

```yaml
# 为不同类型的任务设置合适的超时时间
jobs:
  - name: "快速检查"
    timeout: 300          # 5分钟
    steps: [...]
    
  - name: "编译构建"
    timeout: 1800         # 30分钟
    steps: [...]
    
  - name: "集成测试"
    timeout: 3600         # 1小时
    steps: [...]
```

#### 2. 及时清理资源

```yaml
# 在流水线结束时清理临时资源
stages:
  - name: "清理"
    if: ALWAYS_UNLESS_CANCELLED
    jobs:
      - name: "资源清理"
        steps:
          - name: "清理临时文件"
            run: |
              rm -rf /tmp/build-*
              docker system prune -f
```

## 🔐 安全最佳实践

### 凭据管理

#### 1. 使用凭据管理功能

```yaml
# ✅ 推荐：使用蓝盾凭据管理
steps:
  - name: "安全部署"
    env:
      SSH_KEY: ${{ credentials.deploy_ssh_key }}
      DB_PASSWORD: ${{ credentials.database_password }}
      API_TOKEN: ${{ credentials.api_token }}
    run: |
      # 使用环境变量中的凭据
      deploy.sh
```

#### 2. 最小权限原则

```yaml
# 为不同环境设置不同的权限级别
variables:
  - name: "DEPLOY_ENV"
    value: "dev"

# 根据环境使用不同的凭据
steps:
  - name: "部署"
    if: ${{ variables.DEPLOY_ENV == 'prod' }}
    env:
      DEPLOY_KEY: ${{ credentials.prod_deploy_key }}
    run: deploy-prod.sh
    
  - name: "部署"
    if: ${{ variables.DEPLOY_ENV == 'dev' }}
    env:
      DEPLOY_KEY: ${{ credentials.dev_deploy_key }}
    run: deploy-dev.sh
```

### 代码安全

#### 1. 启用代码扫描

```yaml
# 集成代码安全扫描
steps:
  - name: "代码安全扫描"
    uses: security-scan@1.*
    with:
      scan-type: "sast"
      
  - name: "依赖漏洞扫描"
    uses: dependency-check@1.*
```

#### 2. 制品安全检查

```yaml
# 对构建产物进行安全检查
steps:
  - name: "镜像安全扫描"
    uses: image-scan@1.*
    with:
      image: ${{ variables.image_name }}:${{ ci.build_num }}
```

## 📊 监控和可观测性

### 构建监控

#### 1. 添加关键指标监控

```yaml
# 在关键步骤添加性能监控
steps:
  - name: "构建开始"
    run: |
      echo "::set-variable name=build_start_time::$(date +%s)"
      
  - name: "执行构建"
    run: |
      # 构建逻辑
      mvn clean package
      
  - name: "构建结束"
    run: |
      build_end_time=$(date +%s)
      build_duration=$((build_end_time - ${{ variables.build_start_time }}))
      echo "构建耗时: ${build_duration}秒"
```

#### 2. 设置告警通知

```yaml
# 配置不同场景的通知
notices:
  # 成功通知（可选）
  - type: wework-message
    condition: SUCCESS
    receivers: ["dev-team"]
    
  # 失败通知（必需）
  - type: email
    condition: FAILURE
    receivers: ["admin@example.com"]
    title: "🚨 构建失败告警"
    
  # 长时间运行告警
  - type: wework-message
    condition: TIMEOUT
    receivers: ["ops-team"]
```

## 🤝 团队协作最佳实践

### 代码管理

#### 1. 分支策略

```yaml
# 针对不同分支设置不同的流水线策略
on:
  # 功能分支：快速验证
  - push:
      branches: ["feature/**"]
      name: "功能分支验证"
  
  # 开发分支：完整测试
  - push:
      branches: ["develop"]
      name: "开发分支测试"
      
  # 主分支：部署发布
  - push:
      branches: ["main", "master"]
      name: "生产发布"
```

#### 2. 代码评审集成

```yaml
# MR 触发代码检查
on:
  mr:
    target-branches: ["main", "develop"]
    report-commit-check: true
    block-mr: true

steps:
  - name: "代码质量检查"
    uses: sonar@1.*
    
  - name: "安全扫描"
    uses: security-scan@1.*
```

### 模板管理

#### 1. 创建团队模板

```yaml
# 为团队创建标准化模板
# 包含：
# - 统一的构建流程
# - 标准的质量检查
# - 一致的部署策略
# - 规范的通知配置
```

#### 2. 模板版本管理

```yaml
# 模板版本控制策略：
# - 主版本：重大变更
# - 次版本：功能增加
# - 修订版本：Bug修复

# 示例：template-java-v2.1.3
```

## 📈 成本优化

### 资源使用优化

#### 1. 合理选择构建机

```yaml
# 根据任务类型选择合适规格
matrix_build:
  strategy:
    matrix:
      task_type: [lint, test, build, deploy]
      
  runs-on: |
    ${{ 
      matrix.task_type == 'lint' && 'linux-small' ||
      matrix.task_type == 'test' && 'linux-medium' ||
      matrix.task_type == 'build' && 'linux-large' ||
      'linux-xlarge'
    }}
```

#### 2. 避免资源浪费

```yaml
# 快速失败策略
jobs:
  - name: "代码检查"
    steps:
      - name: "语法检查"
        run: npm run lint
        # 语法错误时快速失败，避免后续资源浪费
        
  - name: "构建"
    needs: ["代码检查"]  # 依赖前置检查
    steps: [...]
```

## 🎯 质量保证

### 测试策略

#### 1. 分层测试

```yaml
stages:
  - name: "单元测试"
    jobs:
      - name: "快速测试"
        steps:
          - run: npm run test:unit
          
  - name: "集成测试"
    jobs:
      - name: "API测试"
        steps:
          - run: npm run test:integration
          
  - name: "端到端测试"
    jobs:
      - name: "E2E测试"
        steps:
          - run: npm run test:e2e
```

#### 2. 质量门禁

```yaml
# 设置质量红线
steps:
  - name: "质量检查"
    uses: quality-gate@1.*
    with:
      coverage-threshold: 80
      duplication-threshold: 3
      maintainability-rating: A
```

## 📚 学习和改进

### 持续改进

#### 1. 定期回顾

- **每周**: 检查流水线执行情况
- **每月**: 分析性能指标和成本
- **每季度**: 评估和更新最佳实践

#### 2. 知识分享

- 团队内部分享成功案例
- 参与社区最佳实践讨论
- 贡献开源插件和模板

### 学习资源

- 官方文档和更新日志
- 社区最佳实践分享
- 技术会议和培训
- 同行经验交流

## 总结

遵循这些最佳实践可以帮助你：

✅ **提高效率**:
- 减少构建时间
- 降低失败率
- 提升开发体验

✅ **保证质量**:
- 统一团队标准
- 自动化质量检查
- 及时发现问题

✅ **控制成本**:
- 优化资源使用
- 避免不必要的浪费
- 提高投资回报率

✅ **增强安全**:
- 保护敏感信息
- 实施安全扫描
- 遵循合规要求

记住：最佳实践不是一成不变的，要根据团队和项目的实际情况进行调整和优化！