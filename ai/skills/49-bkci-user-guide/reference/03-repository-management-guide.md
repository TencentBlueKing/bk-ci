# 代码库接入与管理

## 概述

代码库是 BK-CI 流水线的数据源，支持 Git、SVN 等多种代码管理系统。本章将详细介绍如何接入和管理代码库，配置触发器，以及处理多代码库协作场景。

## 代码库类型

### 支持的代码库类型

| 类型 | 说明 | 支持功能 |
|------|------|----------|
| **Git** | 分布式版本控制系统 | 分支管理、标签、Webhook、SSH/HTTP |
| **GitHub** | GitHub 托管服务 | OAuth 认证、PR 触发、状态回调 |
| **GitLab** | GitLab 托管服务 | OAuth 认证、MR 触发、状态回调 |
| **SVN** | 集中式版本控制系统 | 目录管理、版本号、轮询触发 |
| **工蜂** | 腾讯内部 Git 服务 | 企业认证、内网访问、权限集成 |

### 代码库特性对比

```yaml
功能特性对比:
  触发方式:
    Git: Webhook + 轮询
    GitHub: Webhook + OAuth
    GitLab: Webhook + OAuth  
    SVN: 轮询
    工蜂: Webhook + 企业认证
    
  认证方式:
    Git: SSH Key + HTTP 认证
    GitHub: OAuth + Personal Token
    GitLab: OAuth + Personal Token
    SVN: 用户名密码
    工蜂: 企业账号 + SSH Key
    
  高级功能:
    Git: ✅ 分支策略、标签管理
    GitHub: ✅ PR 集成、状态检查
    GitLab: ✅ MR 集成、CI 变量
    SVN: ❌ 功能相对简单
    工蜂: ✅ 企业级功能
```

## 代码库接入

### Git 代码库接入

#### 1. 添加 Git 代码库

**路径**: `项目 → 代码库 → 关联代码库`

**配置步骤**:
1. 选择代码库类型：Git
2. 填写代码库信息
3. 配置认证方式
4. 测试连接
5. 保存配置

**配置示例**:
```yaml
代码库信息:
  代码库名称: mobile-app-backend
  代码库 URL: https://git.example.com/mobile/backend.git
  代码库别名: backend
  
认证配置:
  认证方式: SSH Key
  SSH 私钥: [上传私钥文件]
  
或者:
  认证方式: HTTP
  用户名: developer
  密码/Token: [凭证管理]
```

#### 2. SSH Key 配置

**生成 SSH Key**:
```bash
# 生成 SSH 密钥对
ssh-keygen -t rsa -b 4096 -C "bkci@example.com"

# 查看公钥内容
cat ~/.ssh/id_rsa.pub
```

**配置流程**:
```
1. 生成 SSH 密钥对
    ↓
2. 将公钥添加到 Git 服务器
    ↓
3. 将私钥上传到 BK-CI
    ↓
4. 测试连接
```

#### 3. HTTP 认证配置

**Personal Access Token**:
```yaml
GitHub Token 配置:
  1. GitHub Settings → Developer settings
  2. Personal access tokens → Generate new token
  3. 选择权限范围: repo, admin:repo_hook
  4. 复制 Token 到 BK-CI
  
GitLab Token 配置:
  1. GitLab User Settings → Access Tokens
  2. 创建 Token，选择 api, read_repository 权限
  3. 复制 Token 到 BK-CI
```

### GitHub/GitLab 接入

#### 1. OAuth 认证接入

**GitHub OAuth 配置**:
```yaml
配置步骤:
  1. 在 GitHub 创建 OAuth App
  2. 配置回调 URL: https://bkci.example.com/oauth/github
  3. 获取 Client ID 和 Client Secret
  4. 在 BK-CI 中配置 OAuth 信息
  5. 用户授权访问
```

**GitLab OAuth 配置**:
```yaml
配置步骤:
  1. 在 GitLab 创建 Application
  2. 配置 Redirect URI
  3. 选择权限范围: api, read_repository
  4. 在 BK-CI 中配置应用信息
  5. 用户授权访问
```

#### 2. Webhook 自动配置

**自动配置流程**:
```
OAuth 认证成功
    ↓
选择要接入的仓库
    ↓
BK-CI 自动创建 Webhook
    ↓
配置触发事件
    ↓
测试 Webhook 连通性
```

**Webhook 事件配置**:
```yaml
支持的事件类型:
  - push: 代码推送
  - pull_request: PR 创建/更新
  - merge_request: MR 创建/更新
  - tag_push: 标签推送
  - release: 发布创建
```

### SVN 代码库接入

#### 1. SVN 配置

**基本配置**:
```yaml
代码库信息:
  代码库 URL: https://svn.example.com/project/trunk
  代码库名称: project-svn
  
认证信息:
  用户名: developer
  密码: [使用凭证管理]
  
高级配置:
  检出目录: trunk
  包含路径: src/,docs/
  排除路径: .svn/,*.tmp
```

#### 2. SVN 目录结构

**标准 SVN 结构**:
```
project/
├── trunk/          # 主开发分支
├── branches/       # 功能分支
│   ├── feature-1/
│   └── feature-2/
└── tags/          # 版本标签
    ├── v1.0.0/
    └── v1.1.0/
```

**BK-CI 配置建议**:
```yaml
主流水线配置:
  代码库 URL: https://svn.example.com/project/trunk
  触发方式: 轮询检查
  轮询间隔: 5 分钟
  
分支流水线配置:
  代码库 URL: https://svn.example.com/project/branches/feature-1
  触发方式: 手动触发
```

## 触发器配置

### Webhook 触发器

#### 1. 基础 Webhook 配置

**配置路径**: `流水线编辑 → 触发器 → 代码库事件触发`

**基本配置**:
```yaml
触发器配置:
  代码库: 选择已关联的代码库
  事件类型: Push Hook
  分支规则: master,develop,release/*
  路径过滤: 
    包含: src/,docs/
    排除: README.md,*.log
```

#### 2. 高级触发规则

**分支过滤规则**:
```yaml
分支匹配规则:
  精确匹配: master
  前缀匹配: feature/*
  后缀匹配: */dev
  正则表达式: ^(master|develop)$
  
示例配置:
  生产发布: master
  测试构建: develop,test/*
  功能开发: feature/*,hotfix/*
```

**路径过滤规则**:
```yaml
路径过滤示例:
  前端项目:
    包含: frontend/src/,frontend/public/
    排除: frontend/node_modules/
    
  后端项目:
    包含: backend/src/,backend/pom.xml
    排除: backend/target/,backend/*.log
    
  文档项目:
    包含: docs/,README.md
    排除: docs/temp/
```

#### 3. 事件类型配置

**Push 事件**:
```yaml
Push Hook 配置:
  触发条件: 代码推送到指定分支
  适用场景: 持续集成构建
  配置示例:
    分支: master,develop
    动作: 自动触发构建
```

**Pull Request 事件**:
```yaml
PR Hook 配置:
  触发条件: PR 创建、更新、合并
  适用场景: 代码审查、预合并测试
  配置示例:
    目标分支: master,develop
    动作: opened,synchronize
    自动评论: 构建结果反馈
```

**Tag 事件**:
```yaml
Tag Hook 配置:
  触发条件: 标签推送
  适用场景: 版本发布构建
  配置示例:
    标签规则: v*,release-*
    动作: 自动发布流水线
```

### 定时触发器

#### 1. Cron 表达式

**基本语法**:
```
秒 分 时 日 月 周 年
*  *  *  *  *  *  *
```

**常用示例**:
```yaml
每日构建:
  表达式: "0 0 2 * * ?" 
  说明: 每天凌晨 2 点执行
  
工作日构建:
  表达式: "0 0 9 * * MON-FRI"
  说明: 工作日上午 9 点执行
  
每周发布:
  表达式: "0 0 20 * * FRI"
  说明: 每周五晚上 8 点执行
  
每月报告:
  表达式: "0 0 10 1 * ?"
  说明: 每月 1 号上午 10 点执行
```

#### 2. 定时触发场景

**夜间构建**:
```yaml
使用场景: 全量测试、性能测试
配置示例:
  时间: 每天凌晨 2:00
  流水线: 完整测试流水线
  通知: 构建结果邮件通知
```

**定期清理**:
```yaml
使用场景: 清理临时文件、旧版本制品
配置示例:
  时间: 每周日凌晨 1:00
  流水线: 清理维护流水线
  动作: 删除 30 天前的构建制品
```

### 手动触发器

#### 1. 参数化触发

**参数类型**:
```yaml
字符串参数:
  参数名: BRANCH_NAME
  默认值: master
  描述: 构建分支名称
  
选择参数:
  参数名: DEPLOY_ENV
  选项: dev,test,prod
  默认值: test
  描述: 部署环境
  
布尔参数:
  参数名: SKIP_TESTS
  默认值: false
  描述: 是否跳过测试
```

#### 2. 触发权限控制

**权限配置**:
```yaml
触发权限设置:
  所有项目成员: 可触发开发构建
  测试人员: 可触发测试部署
  运维人员: 可触发生产发布
  项目管理员: 可触发所有流水线
```

## 多代码库管理

### 多仓库协作模式

#### 1. 微服务架构

**仓库结构**:
```
项目组织结构:
├── frontend-web/          # 前端 Web 应用
├── frontend-mobile/       # 移动端应用
├── backend-user/          # 用户服务
├── backend-order/         # 订单服务
├── backend-payment/       # 支付服务
├── infrastructure/        # 基础设施代码
└── docs/                 # 文档仓库
```

**流水线设计**:
```yaml
单服务流水线:
  - 每个服务独立的 CI/CD 流水线
  - 服务间依赖通过制品库传递
  - 独立的测试和部署流程
  
集成流水线:
  - 跨服务集成测试流水线
  - 整体部署编排流水线
  - 端到端测试流水线
```

#### 2. 单体应用多模块

**仓库结构**:
```
monorepo 结构:
project/
├── frontend/             # 前端模块
├── backend/              # 后端模块
├── shared/               # 共享代码
├── docs/                 # 文档
└── scripts/              # 构建脚本
```

**流水线策略**:
```yaml
路径触发策略:
  前端流水线:
    触发路径: frontend/,shared/
    构建内容: 前端应用
    
  后端流水线:
    触发路径: backend/,shared/
    构建内容: 后端服务
    
  文档流水线:
    触发路径: docs/
    构建内容: 文档站点
```

### 代码库依赖管理

#### 1. 子模块 (Git Submodule)

**配置子模块**:
```bash
# 添加子模块
git submodule add https://git.example.com/shared/common.git shared/common

# 更新子模块
git submodule update --init --recursive
```

**BK-CI 配置**:
```yaml
代码检出配置:
  主仓库: https://git.example.com/main/project.git
  子模块: 启用递归检出
  子模块认证: 使用相同凭证
  
流水线变量:
  SUBMODULE_UPDATE: true
  SUBMODULE_RECURSIVE: true
```

#### 2. 多仓库检出

**多仓库检出插件配置**:
```yaml
仓库 1:
  代码库: frontend-repo
  检出路径: ./frontend
  分支: ${FRONTEND_BRANCH}
  
仓库 2:
  代码库: backend-repo  
  检出路径: ./backend
  分支: ${BACKEND_BRANCH}
  
仓库 3:
  代码库: shared-repo
  检出路径: ./shared
  分支: master
```

### 分支策略管理

#### 1. Git Flow 模式

**分支结构**:
```
Git Flow 分支模型:
├── master          # 生产分支
├── develop         # 开发分支
├── feature/*       # 功能分支
├── release/*       # 发布分支
└── hotfix/*        # 热修复分支
```

**流水线映射**:
```yaml
分支流水线策略:
  master 分支:
    - 生产发布流水线
    - 完整测试套件
    - 自动部署到生产环境
    
  develop 分支:
    - 集成测试流水线
    - 自动部署到测试环境
    - 代码质量检查
    
  feature/* 分支:
    - 功能测试流水线
    - 单元测试和代码检查
    - 部署到开发环境
    
  release/* 分支:
    - 发布准备流水线
    - 完整回归测试
    - 部署到预发布环境
```

#### 2. GitHub Flow 模式

**分支结构**:
```
GitHub Flow 分支模型:
├── main            # 主分支
└── feature/*       # 功能分支
```

**PR 流水线策略**:
```yaml
PR 触发流水线:
  触发条件: PR 创建/更新
  执行内容:
    - 代码检查和测试
    - 构建验证
    - 部署到预览环境
    
  合并后流水线:
  触发条件: PR 合并到 main
  执行内容:
    - 生产构建
    - 自动部署
    - 通知相关人员
```

## 代码库安全

### 访问控制

#### 1. 凭证管理

**凭证类型**:
```yaml
SSH 私钥:
  用途: Git SSH 认证
  安全性: 高
  管理: 定期轮换
  
用户名密码:
  用途: HTTP 认证
  安全性: 中
  管理: 强密码策略
  
Personal Token:
  用途: API 访问
  安全性: 高
  管理: 权限最小化
```

#### 2. 权限最小化

**权限配置原则**:
```yaml
只读权限:
  适用: 构建流水线代码检出
  风险: 低
  
读写权限:
  适用: 自动提交、标签创建
  风险: 中
  限制: 仅特定分支
  
管理权限:
  适用: Webhook 管理
  风险: 高
  限制: 仅管理员账号
```

### 代码安全扫描

#### 1. 静态代码扫描

**扫描工具集成**:
```yaml
SonarQube 集成:
  扫描内容: 代码质量、安全漏洞
  触发时机: 每次代码提交
  阈值设置: 质量门禁
  
Checkmarx 集成:
  扫描内容: 安全漏洞
  触发时机: 发布前扫描
  处理方式: 阻断发布
```

#### 2. 依赖安全扫描

**依赖扫描配置**:
```yaml
npm audit:
  适用: Node.js 项目
  扫描文件: package.json, package-lock.json
  
OWASP Dependency Check:
  适用: Java 项目
  扫描文件: pom.xml, build.gradle
  
Snyk 扫描:
  适用: 多语言项目
  扫描内容: 依赖漏洞、许可证
```

## 常见问题

### 连接问题

**Q: Git 代码库连接失败**
```yaml
排查步骤:
  1. 检查网络连通性
  2. 验证 URL 格式正确性
  3. 确认认证信息有效性
  4. 检查防火墙设置
  5. 查看详细错误日志
```

**Q: Webhook 触发失败**
```yaml
排查步骤:
  1. 检查 Webhook URL 可访问性
  2. 验证 Webhook 配置正确性
  3. 查看 Git 服务器 Webhook 日志
  4. 检查 BK-CI 接收日志
  5. 验证触发条件匹配
```

### 权限问题

**Q: 代码检出权限不足**
```yaml
解决方案:
  1. 确认凭证配置正确
  2. 检查代码库访问权限
  3. 验证 SSH Key 或 Token 有效性
  4. 确认网络访问策略
```

**Q: 无法创建 Webhook**
```yaml
解决方案:
  1. 确认账号有仓库管理权限
  2. 检查 OAuth 授权范围
  3. 验证 API Token 权限
  4. 联系仓库管理员授权
```

## 下一步

完成代码库接入后，建议继续学习：

1. [流水线创建与配置](./04-pipeline-creation-guide.md) - 创建第一条流水线
2. [构建环境与执行](./05-build-environment-guide.md) - 配置构建环境
3. [制品管理与发布](./06-artifact-management-guide.md) - 管理构建制品

---

*本章介绍了代码库接入和管理的完整流程，为流水线构建提供数据源基础。*