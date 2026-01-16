# 代码库集成与管理指南

## 目录

1. [代码库集成概述](#代码库集成概述)
2. [代码库类型与支持](#代码库类型与支持)
3. [代码库关联配置](#代码库关联配置)
4. [凭证管理](#凭证管理)
5. [代码检出配置](#代码检出配置)
6. [事件触发器](#事件触发器)
7. [分支策略管理](#分支策略管理)
8. [代码质量集成](#代码质量集成)
9. [最佳实践](#最佳实践)
10. [故障排查](#故障排查)

---

## 代码库集成概述

### 什么是代码库集成

代码库集成是蓝盾 CI/CD 流程的起点，通过与各种代码管理平台的集成，实现代码的自动拉取、事件监听和触发构建等功能。

### 集成价值

```mermaid
graph LR
    A[代码提交] --> B[自动触发]
    B --> C[代码检出]
    C --> D[构建流水线]
    D --> E[质量检查]
    E --> F[部署发布]
    
    G[事件监听] --> B
    H[分支保护] --> C
    I[权限控制] --> C
```

**核心优势**：
- 🔄 **自动化触发**：代码变更自动启动流水线
- 🔒 **安全访问**：统一的凭证管理和权限控制
- 📊 **质量保障**：集成代码质量检查工具
- 🌿 **分支管理**：支持多分支开发策略

---

## 代码库类型与支持

### 支持的代码库平台

| 平台类型 | 域名 | 支持功能 | 认证方式 |
|----------|------|----------|----------|
| **内网工蜂** | git.woa.com | 全功能支持 | OAuth/SSH/用户名密码 |
| **外网工蜂** | git.tencent.com<br/>git.code.tencent.com | 基础功能 | 用户名密码+Token |
| **GitHub** | github.com | 基础功能 | Personal Access Token |
| **GitLab** | 自建 GitLab | 基础功能 | Personal Access Token |
| **SVN** | svn.woa.com | 基础功能 | 用户名密码 |
| **Perforce** | 自定义域名 | 基础功能 | 用户名密码 |

### 功能支持对比

| 功能 | 内网工蜂 | 外网工蜂 | GitHub | GitLab | SVN |
|------|----------|----------|--------|--------|-----|
| **代码检出** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **Push 触发** | ✅ | ✅ | ✅ | ✅ | ✅ |
| **MR/PR 触发** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **Tag 触发** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **CR 触发** | ✅ | ❌ | ❌ | ❌ | ❌ |
| **Issue 触发** | ✅ | ✅ | ✅ | ✅ | ❌ |
| **PAC 模式** | ✅ | ✅ | ✅ | ✅ | ❌ |

---

## 代码库关联配置

### 内网工蜂关联

#### 1. OAuth 授权（推荐）

**配置步骤**：
```
蓝盾控制台 → 服务 → 代码库 → 关联代码库 → 选择 OAuth
```

**特点**：
- ✅ 配置简单，一键授权
- ✅ 自动刷新 Token
- ✅ 权限继承用户权限

**搜索规则**：
```bash
# 代码库地址：https://git.woa.com/group/project.git
# 搜索关键词：group/project
```

#### 2. SSH 密钥授权

**生成 SSH 密钥**：
```bash
# 生成 SSH 密钥对
ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# 查看公钥
cat ~/.ssh/id_rsa.pub
```

**配置步骤**：
1. **添加公钥到工蜂**
   ```
   工蜂个人设置 → SSH Keys → 添加公钥
   ```

2. **创建凭证**
   ```
   蓝盾凭证管理 → 新增凭证 → SSH私钥+私有Token
   ```

3. **关联代码库**
   ```
   代码库管理 → 关联代码库 → 选择 SSH → 选择凭证
   ```

#### 3. 用户名密码授权

**适用场景**：
- 临时访问
- 特殊权限需求

**配置示例**：
```json
{
  "authType": "USERNAME_PASSWORD",
  "username": "your_username",
  "password": "your_password"
}
```

### 外网工蜂关联

#### Personal Access Token 配置

**获取 Token**：
```
工蜂设置 → Account → Personal Access Token → 生成新 Token
```

**权限范围**：
- `api`：访问 API
- `read_repository`：读取代码库
- `write_repository`：写入代码库（如需推送）

**凭证配置**：
```json
{
  "credentialType": "USERNAME_PASSWORD_PRIVATE_TOKEN",
  "username": "your_username", 
  "password": "your_password",
  "privateToken": "your_personal_access_token"
}
```

### GitHub 集成

#### GitHub App 方式（推荐）

**安装 GitHub App**：
1. 访问 GitHub App 安装页面
2. 选择要集成的仓库
3. 授权必要权限

**支持功能**：
- 代码检出
- Webhook 事件
- 状态回写

#### Personal Access Token 方式

**Token 权限**：
```
repo - 完整的仓库访问权限
admin:repo_hook - 管理仓库 Webhook
```

**配置示例**：
```yaml
# 在流水线中使用
checkout: https://github.com/owner/repo.git
with:
  authType: PERSONAL_ACCESS_TOKEN
  personalAccessToken: ${{ settings.github_token }}
```

### GitLab 集成

#### Project Access Token

**创建 Token**：
```
GitLab 项目 → Settings → Access Tokens → 创建项目访问令牌
```

**权限配置**：
- `read_repository`：读取代码
- `write_repository`：推送代码
- `api`：API 访问

**使用示例**：
```yaml
checkout: https://gitlab.example.com/group/project.git
with:
  authType: PERSONAL_ACCESS_TOKEN
  personalAccessToken: ${{ settings.gitlab_token }}
```

---

## 凭证管理

### 凭证类型详解

#### 1. 用户名+密码

**适用场景**：
- 基础 HTTP 认证
- 简单的代码库访问

**配置示例**：
```json
{
  "credentialType": "USERNAME_PASSWORD",
  "username": "your_username",
  "password": "your_password"
}
```

#### 2. 用户名密码+私有Token

**适用场景**：
- 工蜂代码库访问
- 需要 API 权限的场景

**配置示例**：
```json
{
  "credentialType": "USERNAME_PASSWORD_PRIVATE_TOKEN", 
  "username": "your_username",
  "password": "your_password",
  "privateToken": "glpat-xxxxxxxxxxxxxxxxxxxx"
}
```

#### 3. SSH 私钥

**适用场景**：
- SSH 协议访问
- 高安全性要求

**配置示例**：
```json
{
  "credentialType": "SSH_PRIVATEKEY",
  "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----"
}
```

#### 4. SSH 私钥+私有Token

**适用场景**：
- SSH 访问 + API 操作
- 工蜂完整功能支持

**配置示例**：
```json
{
  "credentialType": "SSH_PRIVATEKEY_PRIVATE_TOKEN",
  "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----",
  "privateToken": "your_private_token"
}
```

### 凭证安全管理

#### 凭证轮换策略

```bash
#!/bin/bash
# 凭证轮换脚本示例

# 1. 生成新的访问令牌
NEW_TOKEN=$(curl -X POST "https://api.example.com/tokens" \
  -H "Authorization: Bearer $OLD_TOKEN" \
  -d '{"name": "ci-token-'$(date +%Y%m%d)'"}' | jq -r '.token')

# 2. 更新蓝盾凭证
curl -X PUT "https://devnet.devops.woa.com/ms/ticket/api/user/credentials/$CREDENTIAL_ID" \
  -H "X-DEVOPS-PROJECT-ID: $PROJECT_ID" \
  -H "Content-Type: application/json" \
  -d "{\"v1\": \"$NEW_TOKEN\"}"

# 3. 验证新凭证
if curl -f -H "Authorization: Bearer $NEW_TOKEN" "https://api.example.com/user"; then
  echo "✅ 凭证轮换成功"
  # 4. 撤销旧令牌
  curl -X DELETE "https://api.example.com/tokens/$OLD_TOKEN_ID"
else
  echo "❌ 凭证轮换失败"
  exit 1
fi
```

#### 权限最小化原则

```yaml
# 不同场景的权限配置
scenarios:
  # 只读访问
  readonly:
    permissions:
      - read_repository
      
  # 基础 CI/CD
  basic_ci:
    permissions:
      - read_repository
      - write_repository
      - read_api
      
  # 完整 DevOps
  full_devops:
    permissions:
      - read_repository
      - write_repository
      - api
      - admin:repo_hook
```

---

## 代码检出配置

### Checkout 插件详解

#### 基础配置

```yaml
# 检出当前仓库代码
steps:
  - checkout: self
    name: "检出代码"
    with:
      # 拉取策略
      strategy: REVERT_UPDATE
      # 拉取深度
      fetchDepth: 1
      # 启用 Git LFS
      enableGitLfs: true
```

#### 高级配置选项

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `pullType` | String | 自动 | BRANCH/TAG/COMMIT_ID |
| `refName` | String | 自动 | 分支名/标签名/提交ID |
| `localPath` | String | 根目录 | 本地保存路径 |
| `strategy` | String | REVERT_UPDATE | 拉取策略 |
| `fetchDepth` | Integer | 1 | Git 拉取深度 |
| `enableSubmodule` | Boolean | true | 启用子模块 |
| `enableGitLfs` | Boolean | true | 启用 Git LFS |

#### 拉取策略对比

| 策略 | 说明 | 优点 | 缺点 | 适用场景 |
|------|------|------|------|----------|
| **REVERT_UPDATE** | 增量拉取，先重置再拉取 | 速度快，冲突少 | - | 推荐使用 |
| **FRESH_CHECKOUT** | 全量拉取，清空后重新克隆 | 最干净 | 速度慢 | 大型项目，缓存问题 |
| **INCREMENT_UPDATE** | 纯增量拉取 | 最快 | 可能有冲突 | 稳定环境 |

### 多代码库检出

#### 检出多个仓库

```yaml
steps:
  # 检出主仓库
  - checkout: self
    name: "检出主仓库"
    
  # 检出依赖仓库
  - checkout: https://git.woa.com/group/dependency.git
    name: "检出依赖仓库"
    with:
      localPath: "deps/"
      refName: "v1.0.0"
      pullType: "TAG"
      
  # 检出配置仓库
  - checkout: https://git.woa.com/group/config.git
    name: "检出配置仓库"
    with:
      localPath: "config/"
      authType: TICKET
      ticketId: "config_repo_credential"
```

#### 子模块处理

```yaml
# 启用子模块
checkout: self
with:
  enableSubmodule: true
  enableSubmoduleRecursive: true
  enableSubmoduleRemote: false
  submodulePath: "third-party/"
```

### 大仓库优化

#### 部分克隆

```yaml
# 只克隆指定路径
checkout: self
with:
  includePath: "src/,docs/"
  excludePath: "tests/,examples/"
  fetchDepth: 1
```

#### 归档缓存

```yaml
# 使用归档缓存加速大仓库拉取
checkout: self
with:
  # 归档缓存路径（需要先创建缓存）
  archivePath: "checkout"
  strategy: REVERT_UPDATE
```

**创建归档缓存**：
```bash
# 使用"拉取代码定时缓存"模板
# 定时任务会创建代码归档，加速后续拉取
```

---

## 事件触发器

### Git 事件触发

#### Push 事件

```yaml
# 监听 Push 事件
on:
  push:
    branches:
      - "master"
      - "develop"
      - "feature/*"
    branches-ignore:
      - "temp/*"
    paths:
      - "src/**"
      - "docs/**"
    paths-ignore:
      - "*.md"
      - "tests/**"
```

**路径过滤规则**：
```bash
# 前缀匹配，不支持正则
src/main/java/     # 匹配 src/main/java/ 下所有文件
src/main/java/*.java  # 不支持，应该用 src/main/java/

# 多路径用逗号分隔
src/,docs/,config/
```

**分支过滤规则**：
```bash
# 支持 Ant Path 规则
*                  # 匹配任何不包含 / 的分支名
**                 # 匹配任何分支名
feature/*          # 匹配 feature/xxx
release/v?.?.?     # 匹配 release/v1.0.0 等
```

#### Merge Request 事件

```yaml
on:
  mr:
    # 源分支过滤
    source-branches:
      - "feature/*"
      - "hotfix/*"
    source-branches-ignore:
      - "feature/temp*"
      
    # 目标分支过滤  
    target-branches:
      - "master"
      - "develop"
      
    # 路径过滤
    paths:
      - "src/**"
    paths-ignore:
      - "docs/**"
      
    # 人员过滤
    users:
      - "developer1"
      - "developer2"
    users-ignore:
      - "bot-user"
      
    # 是否阻塞 MR
    block-mr: true
```

#### Tag 事件

```yaml
on:
  tag:
    tags:
      - "v*"
      - "release-*"
    tags-ignore:
      - "*-beta"
      - "*-alpha"
      
    # 来源分支（仅工蜂创建的 Tag 有效）
    from-branches:
      - "master"
      - "release/*"
```

#### Code Review 事件

```yaml
on:
  review:
    states:
      - "approved"        # 已批准
      - "approving"       # 审批中
      - "change_denied"   # 拒绝变更
      - "change_required" # 需要修改
```

### 外网工蜂事件

```yaml
# 外网工蜂配置
on:
  repo-name: "group/project"
  type: tgit  # 外网工蜂固定为 tgit
  push:
    branches: ["master"]
  mr:
    source-branches: ["feature/*"]
    target-branches: ["master"]
```

### GitHub 事件

```yaml
# GitHub 事件配置
on:
  repo-name: "owner/repository"
  type: github
  push:
    branches: ["main"]
  pull_request:
    branches: ["main"]
```

### 事件变量

#### 内置变量

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `ci.event` | 触发事件类型 | `push`, `merge_request` |
| `ci.branch` | 分支名 | `feature/new-feature` |
| `ci.commit_id` | 提交ID | `abc123def456` |
| `ci.commit_message` | 提交信息 | `feat: add new feature` |
| `ci.actor` | 触发用户 | `developer` |

#### Push 事件变量

```yaml
# Push 事件可用变量
env:
  BRANCH_NAME: ${{ ci.branch }}
  COMMIT_ID: ${{ ci.commit_id }}
  COMMIT_MESSAGE: ${{ ci.commit_message }}
  PUSHER: ${{ ci.actor }}
  REPO_URL: ${{ ci.repo_url }}
```

#### MR 事件变量

```yaml
# MR 事件可用变量
env:
  SOURCE_BRANCH: ${{ ci.mr_source_branch }}
  TARGET_BRANCH: ${{ ci.mr_target_branch }}
  MR_ID: ${{ ci.mr_id }}
  MR_TITLE: ${{ ci.mr_title }}
  MR_AUTHOR: ${{ ci.mr_author }}
```

---

## 分支策略管理

### Git Flow 策略

#### 分支模型

```mermaid
graph LR
    A[master] --> B[develop]
    B --> C[feature/xxx]
    B --> D[release/x.x.x]
    A --> E[hotfix/xxx]
    
    C --> B
    D --> A
    D --> B
    E --> A
    E --> B
```

#### 流水线配置

```yaml
# 主分支：生产部署
on:
  push:
    branches: ["master"]
jobs:
  deploy-prod:
    runs-on: docker
    steps:
      - checkout: self
      - name: "部署到生产环境"
        run: ./deploy.sh prod

---
# 开发分支：测试环境
on:
  push:
    branches: ["develop"]
jobs:
  deploy-test:
    runs-on: docker
    steps:
      - checkout: self
      - name: "部署到测试环境"
        run: ./deploy.sh test

---
# 功能分支：代码检查
on:
  push:
    branches: ["feature/*"]
jobs:
  code-check:
    runs-on: docker
    steps:
      - checkout: self
      - name: "代码质量检查"
        run: ./check.sh
```

### GitHub Flow 策略

#### 简化分支模型

```mermaid
graph LR
    A[main] --> B[feature/xxx]
    B --> C[Pull Request]
    C --> A
```

#### 流水线配置

```yaml
# 主分支：自动部署
on:
  push:
    branches: ["main"]
jobs:
  ci-cd:
    runs-on: docker
    steps:
      - checkout: self
      - name: "测试"
        run: npm test
      - name: "构建"
        run: npm run build
      - name: "部署"
        run: ./deploy.sh

---
# PR：质量检查
on:
  pull_request:
    branches: ["main"]
jobs:
  quality-check:
    runs-on: docker
    steps:
      - checkout: self
      - name: "代码检查"
        run: npm run lint
      - name: "单元测试"
        run: npm test
      - name: "安全扫描"
        run: npm audit
```

### 分支保护策略

#### Pre-Merge 检查

```yaml
# MR 触发时自动合并检查
on:
  mr:
    target-branches: ["master"]
    
jobs:
  pre-merge-check:
    runs-on: docker
    steps:
      - checkout: self
        with:
          # 启用虚拟合并分支
          enableVirtualMergeBranch: true
          
      - name: "合并冲突检查"
        run: |
          if git merge-tree $(git merge-base HEAD origin/master) HEAD origin/master | grep -q "<<<<<<< "; then
            echo "❌ 存在合并冲突"
            exit 1
          fi
          
      - name: "运行测试"
        run: npm test
```

#### 质量门禁

```yaml
# 质量检查门禁
jobs:
  quality-gate:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "代码覆盖率检查"
        run: |
          coverage=$(npm run test:coverage | grep "Lines" | awk '{print $4}' | sed 's/%//')
          if [ "$coverage" -lt 80 ]; then
            echo "❌ 代码覆盖率不足: ${coverage}%"
            exit 1
          fi
          
      - name: "代码质量检查"
        run: |
          npm run lint
          if [ $? -ne 0 ]; then
            echo "❌ 代码质量检查失败"
            exit 1
          fi
```

---

## 代码质量集成

### SonarQube 集成

#### 配置 SonarQube

```yaml
jobs:
  sonar-analysis:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "SonarQube 代码分析"
        run: |
          sonar-scanner \
            -Dsonar.projectKey=${{ ci.project_name }} \
            -Dsonar.sources=src \
            -Dsonar.host.url=${{ settings.sonar_url }} \
            -Dsonar.login=${{ settings.sonar_token }}
            
      - name: "质量门禁检查"
        run: |
          # 等待分析完成
          sleep 30
          
          # 检查质量门禁状态
          status=$(curl -u "${{ settings.sonar_token }}:" \
            "${{ settings.sonar_url }}/api/qualitygates/project_status?projectKey=${{ ci.project_name }}" \
            | jq -r '.projectStatus.status')
            
          if [ "$status" != "OK" ]; then
            echo "❌ SonarQube 质量门禁未通过: $status"
            exit 1
          fi
```

### ESLint 集成

```yaml
jobs:
  eslint-check:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "安装依赖"
        run: npm install
        
      - name: "ESLint 检查"
        run: |
          # 运行 ESLint 并生成报告
          npx eslint src/ --format json --output-file eslint-report.json || true
          
          # 解析报告
          errors=$(jq '[.[] | .errorCount] | add' eslint-report.json)
          warnings=$(jq '[.[] | .warningCount] | add' eslint-report.json)
          
          echo "ESLint 检查结果:"
          echo "- 错误: $errors"
          echo "- 警告: $warnings"
          
          # 设置质量门禁
          if [ "$errors" -gt 0 ]; then
            echo "❌ 存在 ESLint 错误，构建失败"
            exit 1
          fi
          
          if [ "$warnings" -gt 10 ]; then
            echo "⚠️ ESLint 警告过多: $warnings"
            exit 1
          fi
```

### 安全扫描集成

#### 依赖安全扫描

```yaml
jobs:
  security-scan:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "依赖安全扫描"
        run: |
          # NPM 安全审计
          npm audit --audit-level moderate
          
          # 生成安全报告
          npm audit --json > security-report.json
          
          # 检查高危漏洞
          high_vulns=$(jq '.metadata.vulnerabilities.high' security-report.json)
          critical_vulns=$(jq '.metadata.vulnerabilities.critical' security-report.json)
          
          if [ "$critical_vulns" -gt 0 ] || [ "$high_vulns" -gt 5 ]; then
            echo "❌ 发现严重安全漏洞"
            echo "- 严重: $critical_vulns"
            echo "- 高危: $high_vulns"
            exit 1
          fi
```

#### 代码安全扫描

```yaml
jobs:
  code-security:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "代码安全扫描"
        run: |
          # 使用 Semgrep 进行安全扫描
          docker run --rm -v "$PWD:/src" \
            returntocorp/semgrep:latest \
            --config=auto \
            --json \
            --output=/src/semgrep-report.json \
            /src
            
          # 检查安全问题
          security_issues=$(jq '.results | length' semgrep-report.json)
          
          if [ "$security_issues" -gt 0 ]; then
            echo "❌ 发现 $security_issues 个安全问题"
            jq -r '.results[] | "- \(.check_id): \(.message)"' semgrep-report.json
            exit 1
          fi
```

---

## 最佳实践

### 代码库组织

#### 单体仓库 vs 多仓库

**单体仓库（Monorepo）**：
```yaml
# 单体仓库结构
project/
├── services/
│   ├── api/
│   ├── web/
│   └── worker/
├── packages/
│   ├── common/
│   └── utils/
└── .ci/
    └── pipeline.yml

# 路径触发策略
on:
  push:
    paths:
      - "services/api/**"
jobs:
  api-build:
    # 只有 API 服务变更时才构建
```

**多仓库（Polyrepo）**：
```yaml
# 每个服务独立仓库
api-service/
├── src/
├── tests/
└── .ci/
    └── pipeline.yml

web-service/
├── src/
├── tests/  
└── .ci/
    └── pipeline.yml
```

#### 分支命名规范

```bash
# 功能分支
feature/JIRA-123-add-user-login
feature/user-authentication

# 修复分支
hotfix/JIRA-456-fix-memory-leak
bugfix/fix-login-error

# 发布分支
release/v1.2.0
release/2024.01

# 实验分支
experiment/new-architecture
poc/microservices
```

### 提交规范

#### Conventional Commits

```bash
# 提交格式
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]

# 示例
feat(auth): add OAuth2 login support

Add support for OAuth2 authentication with Google and GitHub providers.
This enables users to login using their existing accounts.

Closes #123
```

**提交类型**：
- `feat`: 新功能
- `fix`: 修复bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建工具、辅助工具变动

#### 提交检查

```yaml
jobs:
  commit-check:
    runs-on: docker
    steps:
      - checkout: self
      
      - name: "检查提交信息格式"
        run: |
          # 获取提交信息
          commit_msg=$(git log -1 --pretty=format:"%s")
          
          # 检查格式
          if ! echo "$commit_msg" | grep -qE "^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .+"; then
            echo "❌ 提交信息格式不符合规范"
            echo "格式: <type>[scope]: <description>"
            echo "实际: $commit_msg"
            exit 1
          fi
```

### 权限管理

#### 最小权限原则

```yaml
# 不同环境使用不同凭证
environments:
  development:
    credentials:
      - repo_read_only
      - dev_deploy
      
  staging:
    credentials:
      - repo_read_only
      - staging_deploy
      
  production:
    credentials:
      - repo_read_only
      - prod_deploy_limited
```

#### 凭证轮换

```bash
#!/bin/bash
# 定期轮换凭证脚本

CREDENTIALS=(
  "github_token"
  "gitlab_token"
  "docker_registry"
)

for cred in "${CREDENTIALS[@]}"; do
  echo "轮换凭证: $cred"
  
  # 生成新凭证
  new_token=$(generate_new_token "$cred")
  
  # 更新蓝盾凭证
  update_credential "$cred" "$new_token"
  
  # 验证新凭证
  if validate_credential "$cred"; then
    echo "✅ $cred 轮换成功"
  else
    echo "❌ $cred 轮换失败"
    rollback_credential "$cred"
  fi
done
```

---

## 故障排查

### 常见问题

#### 1. 凭证相关问题

**问题**：`代码库关联的凭证[xxx]不存在`

**排查步骤**：
```bash
# 1. 检查凭证是否存在
curl -H "X-DEVOPS-PROJECT-ID: $PROJECT_ID" \
     "https://devnet.devops.woa.com/ms/ticket/api/user/credentials"

# 2. 检查凭证权限
curl -H "X-DEVOPS-PROJECT-ID: $PROJECT_ID" \
     "https://devnet.devops.woa.com/ms/ticket/api/user/credentials/$CREDENTIAL_ID"

# 3. 测试凭证有效性
git ls-remote https://username:token@git.woa.com/group/project.git
```

**解决方案**：
1. 重新创建凭证
2. 更新代码库关联的凭证
3. 检查凭证权限范围

#### 2. 网络连接问题

**问题**：`Failed to connect to git.woa.com`

**排查步骤**：
```bash
# 1. 检查网络连通性
telnet git.woa.com 443
ping git.woa.com

# 2. 检查代理设置
echo $http_proxy
echo $https_proxy

# 3. 检查DNS解析
nslookup git.woa.com
```

**解决方案**：
```bash
# 配置代理
export https_proxy=http://proxy.company.com:8080
export no_proxy=localhost,127.0.0.1,.company.com

# 或在流水线中配置
env:
  https_proxy: "http://proxy.company.com:8080"
  no_proxy: "localhost,127.0.0.1,.company.com"
```

#### 3. 权限问题

**问题**：`Permission denied (publickey)`

**排查步骤**：
```bash
# 1. 检查SSH密钥
ssh -T git@git.woa.com

# 2. 检查密钥格式
ssh-keygen -l -f ~/.ssh/id_rsa

# 3. 测试密钥权限
ssh -vT git@git.woa.com
```

**解决方案**：
1. 重新生成SSH密钥
2. 确保公钥已添加到代码库
3. 检查私钥格式和权限

#### 4. 分支不存在

**问题**：`Remote branch not found`

**排查步骤**：
```bash
# 1. 列出远程分支
git ls-remote --heads origin

# 2. 检查分支名拼写
git branch -r | grep feature

# 3. 检查分支权限
curl -H "Authorization: token $TOKEN" \
     "https://api.github.com/repos/owner/repo/branches"
```

### 调试技巧

#### 启用详细日志

```yaml
# 在 checkout 中启用调试
checkout: self
with:
  # 启用Git详细输出
  enableGitTrace: true
  # 显示更多调试信息
  verboseMode: true
```

#### 网络诊断

```bash
# 网络连接测试脚本
#!/bin/bash

HOSTS=(
  "git.woa.com:443"
  "github.com:443"
  "gitlab.com:443"
)

for host in "${HOSTS[@]}"; do
  echo "Testing connection to $host..."
  
  if timeout 5 bash -c "</dev/tcp/${host/:/ }"; then
    echo "✅ $host - Connected"
  else
    echo "❌ $host - Failed"
  fi
done

# DNS解析测试
echo "DNS Resolution Test:"
nslookup git.woa.com
nslookup github.com
```

#### 凭证验证

```bash
# 凭证验证脚本
#!/bin/bash

validate_git_credential() {
  local repo_url=$1
  local username=$2
  local token=$3
  
  echo "验证凭证: $repo_url"
  
  # 测试仓库访问
  if git ls-remote "https://${username}:${token}@${repo_url#https://}" >/dev/null 2>&1; then
    echo "✅ 凭证有效"
    return 0
  else
    echo "❌ 凭证无效"
    return 1
  fi
}

# 使用示例
validate_git_credential "https://git.woa.com/group/project.git" "username" "token"
```

---

## 总结

本章节全面介绍了蓝盾的代码库集成与管理，涵盖了从基础配置到高级应用的所有内容。

### 关键要点

1. **多平台支持**：支持工蜂、GitHub、GitLab等主流代码管理平台
2. **安全认证**：提供多种认证方式，确保代码访问安全
3. **智能触发**：基于代码事件的自动化触发机制
4. **质量保障**：集成代码质量检查和安全扫描工具
5. **最佳实践**：遵循行业标准的分支管理和提交规范

### 最佳实践总结

- ✅ **统一认证**：使用统一的凭证管理系统
- ✅ **权限最小化**：按需分配最小必要权限
- ✅ **自动化触发**：基于分支和路径的智能触发
- ✅ **质量门禁**：集成代码质量和安全检查
- ✅ **规范管理**：建立清晰的分支和提交规范

通过遵循本指南的建议和最佳实践，您可以构建一个安全、高效的代码管理体系，为团队的协作开发提供坚实的基础。