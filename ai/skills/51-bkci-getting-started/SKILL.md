---
name: 51-bkci-getting-started
description: >
  蓝盾 (BK-CI) 新手入门指南和最佳实践，涵盖快速接入、项目配置、流水线设计、
  性能优化、安全规范等内容。当新用户初次使用蓝盾或需要了解最佳实践时使用。
---

# 蓝盾新手入门与最佳实践

## 概述

本指南面向蓝盾新用户，提供从零开始的完整入门路径，以及经验丰富的用户总结的最佳实践。

### 学习路径

| 阶段 | 内容 | 预计时间 | 参考文档 |
|------|------|----------|----------|
| **入门阶段** | 基础概念、快速接入 | 1-2天 | [快速入门](./reference/quick-start.md) |
| **进阶阶段** | 流水线设计、插件使用 | 3-5天 | [进阶指南](./reference/advanced-guide.md) |
| **优化阶段** | 性能优化、最佳实践 | 1-2周 | [最佳实践](./reference/best-practices.md) |
| **专家阶段** | 插件开发、架构设计 | 持续学习 | [专家指南](./reference/expert-guide.md) |

---

## 🚀 快速开始

### 第一步：了解基础概念

**核心概念**:
- **项目**: 代码和流水线的组织单位
- **流水线**: CI/CD 流程的定义和执行
- **插件**: 可复用的功能组件
- **构建机**: 执行任务的计算资源
- **制品库**: 构建产物的存储仓库

### 第二步：创建第一个项目

1. **登录蓝盾平台**
2. **创建新项目**
3. **关联代码库**
4. **配置基础权限**

### 第三步：创建第一条流水线

```yaml
# 最简单的流水线示例
version: v3.0
name: "我的第一条流水线"

on:
  push:
    branches: [main]

steps:
  - name: "检出代码"
    uses: checkout@latest
    
  - name: "构建项目"
    run: |
      echo "开始构建..."
      # 添加你的构建命令
      
  - name: "运行测试"
    run: |
      echo "运行测试..."
      # 添加你的测试命令
```

---

## 📚 学习指南

### 🎯 新手必读

#### 1. 基础操作
- [项目创建和配置](./reference/project-setup.md)
- [代码库关联](./reference/repository-setup.md)
- [第一条流水线](./reference/first-pipeline.md)
- [权限管理基础](./reference/permission-basics.md)

#### 2. 核心功能
- [流水线编排](./reference/pipeline-design.md)
- [插件使用指南](./reference/plugin-usage.md)
- [变量和参数](./reference/variables-guide.md)
- [触发器配置](./reference/trigger-setup.md)

#### 3. 常用场景
- [Java 项目 CI/CD](./reference/java-cicd.md)
- [前端项目构建](./reference/frontend-build.md)
- [Docker 镜像构建](./reference/docker-build.md)
- [多环境部署](./reference/multi-env-deploy.md)

### 🏆 最佳实践

#### 流水线设计原则

**✅ 推荐做法**:
- 善用系统内置变量
- 将审批功能挪到 Stage 准入上
- 编译脚本里能用相对路径，就不要用绝对路径
- 能用平台提供的 CI 模板，就尽量不要自己折腾
- 及时给好用的插件点赞，鼓励开发者
- 多个相似流水线要使用模板，便于后期维护
- 要把 Stage 标签用起来，便于分析执行耗时
- CI 流程中引用 token，尽量用公共账号
- 及时关停非必要的流水线，降低构建机成本

**❌ 不建议做法**:
- **不要**在编译过程里引入外网依赖，能用软件源代理的最好
- **不要**把密码凭据写到流水线里，用凭据管理功能
- **不要**一出问题就找技术支持，先查看日志错误提示
- **不要**尝试在一个 step 里搞定所有事情，steps 拆得细便于定位问题
- **不要**在流水线变量里定义操作系统级变量
- **不要**认为本地 OK，CI 构建就一定行
- **不要**在流水线插件运行时加入运行时间较长的步骤

#### 性能优化建议

**构建性能**:
- 使用合适的构建机规格
- 启用构建缓存
- 并行执行独立任务
- 优化依赖下载

**资源使用**:
- 合理设置超时时间
- 及时清理临时文件
- 使用资源锁避免冲突
- 监控资源使用情况

---

## 🛠️ 实践案例

### 案例1：Java Spring Boot 项目

```yaml
version: v3.0
name: "Java Spring Boot CI/CD"

on:
  push:
    branches: [main, develop]
  mr:
    target-branches: [main]

variables:
  - name: "JAVA_VERSION"
    value: "11"
  - name: "MAVEN_OPTS"
    value: "-Xmx1024m"

stages:
  - name: "构建测试"
    jobs:
      - name: "编译测试"
        runs-on: linux
        steps:
          - uses: checkout@latest
          
          - name: "设置 Java 环境"
            uses: setup-java@2.*
            with:
              java-version: ${{ variables.JAVA_VERSION }}
              
          - name: "Maven 构建"
            run: |
              mvn clean compile test package
              
          - name: "上传测试报告"
            uses: upload-artifact@1.*
            with:
              name: "test-reports"
              path: "target/surefire-reports/"

  - name: "部署"
    if: ${{ ci.branch == 'main' }}
    jobs:
      - name: "部署到测试环境"
        runs-on: linux
        steps:
          - name: "部署应用"
            run: |
              echo "部署到测试环境..."
              # 部署脚本
```

### 案例2：前端 Vue 项目

```yaml
version: v3.0
name: "Vue 前端项目 CI/CD"

on:
  push:
    branches: [main, develop]

variables:
  - name: "NODE_VERSION"
    value: "16"

stages:
  - name: "构建"
    jobs:
      - name: "前端构建"
        runs-on: linux
        steps:
          - uses: checkout@latest
          
          - name: "设置 Node.js"
            uses: setup-node@3.*
            with:
              node-version: ${{ variables.NODE_VERSION }}
              
          - name: "安装依赖"
            run: npm ci
            
          - name: "代码检查"
            run: npm run lint
            
          - name: "运行测试"
            run: npm run test:unit
            
          - name: "构建项目"
            run: npm run build
            
          - name: "上传构建产物"
            uses: upload-artifact@1.*
            with:
              name: "dist"
              path: "dist/"
```

---

## 🎓 进阶学习

### 高级功能

#### 1. Pipeline as Code (PAC)
- YAML 语法掌握
- 版本控制集成
- 分支策略设计
- 模板复用

#### 2. 插件开发
- 插件架构理解
- 多语言开发支持
- 插件发布流程
- 社区贡献

#### 3. 企业级应用
- 多项目管理
- 权限体系设计
- 安全合规要求
- 成本优化

### 学习资源

**官方文档**:
- [使用指南](../49-bkci-user-guide/) - 完整功能说明
- [插件开发](../47-pipeline-plugin-development/) - 插件开发指南
- [问题排查](../50-bkci-troubleshooting/) - 故障排查手册

**社区资源**:
- 蓝盾技术交流群
- 插件开发者社区
- 最佳实践分享
- 定期技术沙龙

---

## 🏅 认证路径

### 初级认证
- **掌握基础概念**
- **能创建简单流水线**
- **了解常用插件**
- **会基础问题排查**

### 中级认证
- **熟练使用 PAC 模式**
- **能设计复杂流水线**
- **掌握性能优化**
- **了解安全最佳实践**

### 高级认证
- **能开发自定义插件**
- **掌握企业级部署**
- **能进行架构设计**
- **具备培训指导能力**

---

## 📋 检查清单

### 新项目接入检查

- [ ] 项目创建完成
- [ ] 代码库正确关联
- [ ] 权限配置合理
- [ ] 第一条流水线运行成功
- [ ] 团队成员已添加
- [ ] 基础监控已配置

### 流水线质量检查

- [ ] 触发器配置正确
- [ ] 变量命名规范
- [ ] 步骤拆分合理
- [ ] 错误处理完善
- [ ] 通知配置到位
- [ ] 性能表现良好

### 安全合规检查

- [ ] 敏感信息使用凭据管理
- [ ] 权限设置最小化原则
- [ ] 代码扫描已启用
- [ ] 制品安全检查
- [ ] 审计日志完整
- [ ] 合规要求满足

---

## 🤝 获得帮助

### 自助资源
1. **查看文档**: 优先查阅官方文档
2. **搜索问题**: 在常见问题中搜索
3. **查看日志**: 分析错误日志信息
4. **社区讨论**: 参与社区交流

### 技术支持
- **平台问题**: O2000 技术支持
- **插件问题**: 联系插件作者
- **紧急问题**: 蓝盾技术支持群
- **培训需求**: 申请专项培训

### 反馈渠道
- **功能建议**: 产品反馈渠道
- **Bug 报告**: 问题反馈系统
- **文档改进**: 文档协作平台
- **最佳实践**: 社区分享平台

---

## 📖 详细指南

各阶段的详细学习内容请参考 `reference/` 目录下的对应文档：

- 📖 [快速入门指南](./reference/quick-start.md)
- 📖 [进阶使用指南](./reference/advanced-guide.md)
- 📖 [最佳实践汇总](./reference/best-practices.md)
- 📖 [专家级指南](./reference/expert-guide.md)
- 📖 [实践案例集](./reference/use-cases.md)

---

## 相关 Skills

- [49-bkci-user-guide](../49-bkci-user-guide/) - 使用指南
- [50-bkci-troubleshooting](../50-bkci-troubleshooting/) - 问题排查
- [47-pipeline-plugin-development](../47-pipeline-plugin-development/) - 插件开发

---

## 更新日志

- 2025-01-09: 创建新手入门和最佳实践 Skill，整合学习路径和实践经验