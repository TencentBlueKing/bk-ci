# 权限问题排查详细指南

## 📋 概述

权限管理是 BK-CI 平台安全的核心，本文档提供权限相关问题的详细排查方法和解决方案，涵盖用户权限、项目权限、资源访问权限等各种场景。

## 🔍 权限问题分类

### 1. 用户权限问题

#### 1.1 登录认证失败

**问题现象**:
- 无法登录 BK-CI 平台
- 登录后提示权限不足
- SSO 单点登录失败

**排查步骤**:

**Step 1: 检查用户账号状态**
```bash
# 用户账号基本信息检查
1. 确认用户名拼写正确
2. 检查账号是否被禁用
3. 验证密码是否正确
4. 确认账号是否过期

# 在用户管理中查看
- 进入用户管理 -> 用户列表
- 搜索对应用户
- 查看用户状态和权限
```

**Step 2: SSO 认证问题**
```bash
# SSO 配置检查
1. 检查 SSO 服务状态
   curl -I https://sso.company.com/health
   
2. 验证 SSO 配置
   - 检查回调地址配置
   - 确认应用 ID 和密钥
   - 验证用户属性映射

3. 查看 SSO 认证日志
   - 平台认证日志
   - SSO 服务端日志
   - 用户浏览器网络请求
```

**Step 3: 权限同步问题**
```bash
# 权限同步检查
1. 检查用户组同步状态
2. 验证权限继承关系
3. 确认权限更新时间
4. 手动触发权限同步

# 权限同步脚本示例
#!/bin/bash
# 用户权限同步脚本
curl -X POST \
  -H "Authorization: Bearer $API_TOKEN" \
  -H "Content-Type: application/json" \
  https://api.devops.com/v1/auth/sync/user/$USER_ID
```

#### 1.2 用户组权限配置

**用户组管理**:
```yaml
# 用户组权限配置示例
userGroups:
  - name: "开发组"
    description: "开发人员用户组"
    permissions:
      - "pipeline.view"
      - "pipeline.execute"
      - "repository.read"
    members:
      - "developer1@company.com"
      - "developer2@company.com"
      
  - name: "运维组"
    description: "运维人员用户组"
    permissions:
      - "pipeline.view"
      - "pipeline.execute"
      - "pipeline.manage"
      - "environment.manage"
    members:
      - "ops1@company.com"
      - "ops2@company.com"
```

### 2. 项目权限问题

#### 2.1 项目访问权限

**问题现象**:
- 无法查看项目列表
- 项目详情页面访问被拒绝
- 项目操作按钮不可用

**权限检查流程**:
```bash
# 项目权限检查清单
□ 用户是否为项目成员
□ 用户组是否有项目权限
□ 项目是否设置为私有
□ 权限是否正确继承
□ 权限配置是否生效
```

**项目成员管理**:
```bash
# 查看项目成员
1. 进入项目设置 -> 成员管理
2. 查看当前用户权限
3. 检查用户组权限继承
4. 验证权限有效期

# 添加项目成员
1. 项目设置 -> 成员管理 -> 添加成员
2. 选择用户或用户组
3. 分配相应权限角色
4. 设置权限有效期（可选）
```

#### 2.2 项目角色权限

**标准项目角色**:
```yaml
projectRoles:
  - role: "项目管理员"
    permissions:
      - "project.manage"
      - "pipeline.manage"
      - "repository.manage"
      - "environment.manage"
      - "member.manage"
    description: "项目全部权限"
    
  - role: "开发人员"
    permissions:
      - "project.view"
      - "pipeline.view"
      - "pipeline.execute"
      - "repository.read"
      - "repository.write"
    description: "开发相关权限"
    
  - role: "测试人员"
    permissions:
      - "project.view"
      - "pipeline.view"
      - "pipeline.execute"
      - "repository.read"
    description: "测试相关权限"
    
  - role: "访客"
    permissions:
      - "project.view"
      - "pipeline.view"
    description: "只读权限"
```

### 3. 流水线权限问题

#### 3.1 流水线操作权限

**权限检查**:
```bash
# 流水线权限验证
1. 查看流水线权限设置
   - 进入流水线 -> 设置 -> 权限管理
   - 查看执行权限配置
   - 确认用户权限分配

2. 检查用户组权限
   - 验证用户所属组
   - 确认组权限配置
   - 检查权限继承关系

3. 测试权限有效性
   - 尝试执行流水线
   - 查看权限错误信息
   - 确认权限生效时间
```

**流水线权限配置**:
```yaml
# 流水线权限配置示例
pipelinePermissions:
  - pipeline: "backend-ci"
    permissions:
      execute:
        users: ["dev1@company.com", "dev2@company.com"]
        groups: ["开发组", "测试组"]
      manage:
        users: ["lead@company.com"]
        groups: ["项目管理员"]
      view:
        users: ["*"]  # 所有项目成员
        
  - pipeline: "production-deploy"
    permissions:
      execute:
        users: ["ops1@company.com"]
        groups: ["运维组"]
        approvers: ["manager@company.com"]  # 需要审批
      manage:
        users: ["ops-lead@company.com"]
      view:
        groups: ["开发组", "运维组"]
```

#### 3.2 流水线组权限

**流水线组管理**:
```bash
# 流水线组权限配置
1. 创建流水线组
   - 流水线管理 -> 流水线组 -> 新建
   - 设置组名称和描述
   - 添加流水线到组

2. 配置组权限
   - 设置组级别权限
   - 配置权限继承规则
   - 分配用户和用户组

3. 权限继承验证
   - 检查子流水线权限
   - 验证权限覆盖规则
   - 测试权限有效性
```

### 4. 代码库权限问题

#### 4.1 代码库访问权限

**问题现象**:
- Git 克隆失败，提示认证错误
- 代码库列表为空或不完整
- Push 代码时权限被拒绝

**排查步骤**:

**Step 1: 检查代码库配置**
```bash
# 代码库基本信息检查
1. 进入代码库管理 -> 代码库列表
2. 查看代码库授权状态
3. 检查代码库 URL 正确性
4. 验证分支权限配置

# 代码库授权检查
- OAuth 授权状态
- SSH Key 配置
- Personal Access Token
- 用户名密码认证
```

**Step 2: 凭证管理**
```bash
# SSH Key 配置
1. 生成 SSH Key
   ssh-keygen -t rsa -b 4096 -C "user@company.com"
   
2. 添加公钥到代码库
   - GitHub: Settings -> SSH and GPG keys
   - GitLab: User Settings -> SSH Keys
   - 内网 Git: 用户设置 -> SSH 公钥

3. 测试 SSH 连接
   ssh -T git@github.com
   ssh -T git@gitlab.company.com

# Personal Access Token
1. 生成 Token
   - GitHub: Settings -> Developer settings -> Personal access tokens
   - GitLab: User Settings -> Access Tokens
   
2. 配置 Token 权限
   - repo (完整仓库权限)
   - read:user (读取用户信息)
   - write:repo_hook (写入仓库钩子)

3. 在 BK-CI 中配置凭证
   - 凭证管理 -> 新增凭证
   - 选择 Token 类型
   - 输入 Token 值
```

**Step 3: 网络和代理配置**
```bash
# Git 网络配置
# 配置 Git 代理
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy http://proxy.company.com:8080

# 配置 Git 超时
git config --global http.lowSpeedLimit 1000
git config --global http.lowSpeedTime 300

# 测试代码库连接
git ls-remote https://github.com/user/repo.git
git ls-remote git@github.com:user/repo.git
```

#### 4.2 分支权限管理

**分支保护规则**:
```yaml
# 分支保护配置示例
branchProtection:
  - branch: "master"
    protection:
      requirePullRequest: true
      requireStatusChecks: true
      requireUpToDate: true
      dismissStaleReviews: true
      requiredReviewers: 2
      restrictPushes: true
      allowedUsers: ["admin@company.com"]
      allowedTeams: ["maintainers"]
      
  - branch: "develop"
    protection:
      requirePullRequest: true
      requireStatusChecks: false
      requiredReviewers: 1
      restrictPushes: false
      
  - branch: "feature/*"
    protection:
      requirePullRequest: false
      requireStatusChecks: false
      restrictPushes: false
```

### 5. 制品库权限问题

#### 5.1 制品上传下载权限

**问题现象**:
- 制品上传失败，提示权限不足
- 无法下载制品文件
- 制品库连接认证失败

**权限配置**:
```yaml
# 制品库权限配置
artifactoryPermissions:
  - repository: "maven-releases"
    permissions:
      read: ["developers", "testers"]
      write: ["developers"]
      admin: ["ops-team"]
    authentication:
      type: "token"
      credentials: "${{ settings.ARTIFACTORY_TOKEN }}"
      
  - repository: "docker-images"
    permissions:
      read: ["*"]  # 所有用户可读
      write: ["docker-publishers"]
      admin: ["docker-admins"]
    authentication:
      type: "username-password"
      username: "${{ settings.DOCKER_USERNAME }}"
      password: "${{ settings.DOCKER_PASSWORD }}"
```

**制品库认证配置**:
```bash
# Maven 制品库认证
# settings.xml 配置
<servers>
  <server>
    <id>nexus-releases</id>
    <username>${env.NEXUS_USERNAME}</username>
    <password>${env.NEXUS_PASSWORD}</password>
  </server>
</servers>

# Docker 制品库认证
docker login registry.company.com \
  --username $DOCKER_USERNAME \
  --password $DOCKER_PASSWORD

# NPM 制品库认证
npm config set registry https://npm.company.com
npm config set //npm.company.com/:_authToken $NPM_TOKEN
```

### 6. IAM 集成问题

#### 6.1 IAM 权限同步

**IAM 配置检查**:
```bash
# IAM 集成状态检查
1. 检查 IAM 服务连接
   curl -I https://iam.company.com/health
   
2. 验证 IAM 配置
   - 应用 ID 和密钥
   - 权限模型配置
   - 资源类型定义
   - 操作权限映射

3. 权限同步状态
   - 查看同步日志
   - 检查同步频率
   - 验证增量同步
   - 手动触发全量同步
```

**权限模型配置**:
```json
{
  "system": "bk-ci",
  "resources": [
    {
      "type": "project",
      "name": "项目",
      "actions": [
        {"id": "view", "name": "查看项目"},
        {"id": "manage", "name": "管理项目"}
      ]
    },
    {
      "type": "pipeline",
      "name": "流水线",
      "actions": [
        {"id": "view", "name": "查看流水线"},
        {"id": "execute", "name": "执行流水线"},
        {"id": "manage", "name": "管理流水线"}
      ]
    }
  ],
  "policies": [
    {
      "subject": "user:developer",
      "resource": "project:demo",
      "actions": ["view"],
      "effect": "allow"
    }
  ]
}
```

#### 6.2 权限申请流程

**权限申请配置**:
```yaml
# 权限申请流程配置
permissionRequest:
  - resource: "project"
    approvers:
      - type: "role"
        value: "project-admin"
      - type: "user"
        value: "manager@company.com"
    autoApprove: false
    expiration: "30d"
    
  - resource: "pipeline"
    approvers:
      - type: "role"
        value: "pipeline-owner"
    autoApprove: true
    conditions:
      - "same_department"
    expiration: "7d"
```

## 🔧 权限问题解决方案

### 1. 常见权限错误处理

**错误码对照表**:
```bash
# 权限相关错误码
2105001: 用户未登录
2105002: 用户权限不足
2105003: 资源不存在或无权限访问
2105004: 操作权限被拒绝
2105005: 权限已过期
2105006: 权限申请待审批
2105007: IAM 权限同步失败
```

**权限问题快速修复**:
```bash
# 权限问题快速诊断脚本
#!/bin/bash

USER_ID="$1"
RESOURCE_TYPE="$2"
RESOURCE_ID="$3"
ACTION="$4"

echo "=== 权限诊断开始 ==="
echo "用户: $USER_ID"
echo "资源类型: $RESOURCE_TYPE"
echo "资源ID: $RESOURCE_ID"
echo "操作: $ACTION"

# 检查用户基本信息
echo "1. 检查用户基本信息"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/users/$USER_ID" | jq .

# 检查用户权限
echo "2. 检查用户权限"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/permissions?user=$USER_ID&resource=$RESOURCE_TYPE:$RESOURCE_ID&action=$ACTION" | jq .

# 检查权限继承
echo "3. 检查权限继承"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/inheritance?user=$USER_ID&resource=$RESOURCE_TYPE:$RESOURCE_ID" | jq .

echo "=== 权限诊断完成 ==="
```

### 2. 权限配置最佳实践

**权限设计原则**:
```yaml
# 权限设计最佳实践
principles:
  - name: "最小权限原则"
    description: "用户只获得完成工作所需的最小权限"
    implementation:
      - 按角色分配权限
      - 定期审查权限
      - 及时回收不需要的权限
      
  - name: "职责分离原则"
    description: "关键操作需要多人协作完成"
    implementation:
      - 开发和部署权限分离
      - 审批和执行权限分离
      - 监控和操作权限分离
      
  - name: "权限继承原则"
    description: "通过组织结构和角色继承权限"
    implementation:
      - 用户组权限继承
      - 项目权限继承
      - 资源权限继承
```

**权限配置模板**:
```yaml
# 标准权限配置模板
permissionTemplate:
  project:
    roles:
      - name: "项目经理"
        permissions: ["project.*", "pipeline.*", "member.manage"]
      - name: "技术负责人"
        permissions: ["project.view", "pipeline.*", "repository.*"]
      - name: "开发工程师"
        permissions: ["project.view", "pipeline.execute", "repository.read"]
      - name: "测试工程师"
        permissions: ["project.view", "pipeline.execute"]
        
  pipeline:
    categories:
      - name: "开发流水线"
        permissions:
          execute: ["developers"]
          manage: ["tech-leads"]
      - name: "生产流水线"
        permissions:
          execute: ["ops-team"]
          manage: ["ops-leads"]
          approve: ["managers"]
```

### 3. 权限审计和监控

**权限审计**:
```bash
# 权限审计脚本
#!/bin/bash

echo "=== 权限审计报告 ==="

# 统计用户权限分布
echo "1. 用户权限分布"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/audit/users" | \
  jq -r '.[] | "\(.username): \(.permissions | length) 个权限"'

# 检查高权限用户
echo "2. 高权限用户"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/audit/high-privilege" | \
  jq -r '.[] | "\(.username): \(.role)"'

# 检查长期未使用的权限
echo "3. 长期未使用的权限"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/audit/unused?days=30" | \
  jq -r '.[] | "\(.username): \(.permission) (最后使用: \(.lastUsed))"'

# 检查权限变更记录
echo "4. 近期权限变更"
curl -s -H "Authorization: Bearer $API_TOKEN" \
  "https://api.devops.com/v1/auth/audit/changes?days=7" | \
  jq -r '.[] | "\(.timestamp): \(.username) \(.action) \(.permission)"'
```

**权限监控告警**:
```yaml
# 权限监控配置
monitoring:
  alerts:
    - name: "高权限操作告警"
      condition: "action in ['project.delete', 'pipeline.delete']"
      notification:
        - type: "email"
          recipients: ["security@company.com"]
        - type: "webhook"
          url: "https://hooks.slack.com/security-alerts"
          
    - name: "异常权限申请告警"
      condition: "permission_count > 10 in single_request"
      notification:
        - type: "email"
          recipients: ["admin@company.com"]
          
    - name: "权限同步失败告警"
      condition: "iam_sync_failed"
      notification:
        - type: "sms"
          recipients: ["13800138000"]
```

## 📞 权限问题支持

### 1. 问题上报流程

**权限问题分类**:
- **紧急问题**: 生产环境权限故障，影响业务正常运行
- **重要问题**: 权限配置错误，影响团队工作效率
- **一般问题**: 权限使用疑问，需要指导和帮助

**联系方式**:
- **紧急问题**: 安全团队热线 (24小时)
- **重要问题**: 平台支持群 (工作时间 4 小时响应)
- **一般问题**: 工单系统 (工作日 1 天响应)

### 2. 权限问题报告模板

```markdown
## 权限问题报告

### 基本信息
- **用户ID**: user@company.com
- **项目ID**: demo-project
- **问题发生时间**: 2025-01-09 14:30:00
- **问题类型**: 访问被拒绝

### 问题描述
详细描述遇到的权限问题

### 操作步骤
1. 尝试访问的资源
2. 执行的操作
3. 收到的错误信息

### 错误信息
```
HTTP 403: Forbidden
用户无权限执行此操作
```

### 期望权限
描述用户应该具有的权限

### 业务影响
- 影响用户数: XX人
- 影响项目: XX个
- 业务影响程度: 高/中/低

### 紧急程度
- [ ] 紧急 (生产环境权限故障)
- [ ] 重要 (影响团队工作)
- [ ] 一般 (使用疑问)
```

---

## 📚 相关文档

- [流水线问题排查指南](./01-pipeline-troubleshooting.md)
- [插件问题排查指南](./02-plugin-troubleshooting.md)
- [环境问题排查指南](./05-environment-troubleshooting.md)
- [BK-CI 用户使用指南](../../49-bkci-user-guide/)

---

*最后更新时间：2025-01-09*
*文档版本：v2.0*