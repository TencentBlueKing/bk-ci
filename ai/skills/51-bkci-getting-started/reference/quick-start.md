# 蓝盾快速入门指南

## 概述

本指南将帮助你在 30 分钟内完成蓝盾的基础配置，创建第一条流水线并成功执行。

## 前置准备

### 账号和权限
- 腾讯内网账号
- 项目访问权限
- 代码库访问权限

### 开发环境
- 代码库（工蜂/GitHub/GitLab）
- 本地开发环境
- 基础的 Git 操作知识

## 第一步：创建项目

### 1.1 登录蓝盾平台

访问蓝盾平台：`https://devops.oa.com`

### 1.2 创建新项目

1. 点击"创建项目"
2. 填写项目信息：
   - **项目名称**: 使用有意义的名称
   - **项目描述**: 简要说明项目用途
   - **项目类型**: 选择合适的项目类型
3. 点击"确定"完成创建

### 1.3 项目配置

**基础设置**:
```yaml
项目名称: my-first-project
项目描述: 我的第一个蓝盾项目
项目类型: 通用项目
可见范围: 项目成员
```

## 第二步：关联代码库

### 2.1 添加代码库

1. 进入项目 -> 代码库管理
2. 点击"关联代码库"
3. 选择代码库类型（工蜂/GitHub/GitLab）
4. 填写代码库信息

### 2.2 配置代码库

**工蜂代码库示例**:
```yaml
代码库类型: 工蜂
代码库地址: https://git.woa.com/group/project.git
代码库别名: my-repo
认证方式: SSH Key / HTTPS
```

### 2.3 验证连接

- 点击"测试连接"
- 确认连接成功
- 保存配置

## 第三步：创建第一条流水线

### 3.1 选择创建方式

**方式一：使用模板创建**
1. 进入项目 -> 流水线
2. 点击"新建" -> "从模板创建"
3. 选择合适的模板
4. 填写基本信息

**方式二：空白流水线**
1. 点击"新建" -> "空白流水线"
2. 填写流水线名称
3. 选择触发方式

### 3.2 基础流水线配置

```yaml
# 最简单的流水线示例
version: v3.0
name: "Hello World 流水线"

# 触发器：代码推送时触发
on:
  push:
    branches: [main, master]

# 执行步骤
steps:
  # 步骤1：检出代码
  - name: "检出代码"
    uses: checkout@latest
    
  # 步骤2：打印信息
  - name: "Hello World"
    run: |
      echo "Hello, 蓝盾!"
      echo "当前分支: ${{ ci.branch }}"
      echo "提交ID: ${{ ci.commit_id }}"
      
  # 步骤3：列出文件
  - name: "查看文件"
    run: |
      echo "项目文件列表:"
      ls -la
```

### 3.3 保存并发布

1. 点击"保存"
2. 填写版本说明
3. 点击"发布"

## 第四步：执行流水线

### 4.1 手动触发

1. 进入流水线详情页
2. 点击"执行"按钮
3. 确认执行参数
4. 点击"确定"开始执行

### 4.2 查看执行结果

**执行状态**:
- 🟡 执行中
- 🟢 执行成功
- 🔴 执行失败
- ⚪ 已取消

**查看日志**:
1. 点击执行记录
2. 查看各步骤执行状态
3. 点击步骤查看详细日志

### 4.3 代码触发测试

1. 修改代码并推送到配置的分支
2. 观察流水线是否自动触发
3. 确认触发器配置正确

## 第五步：添加实际构建步骤

### 5.1 Java 项目示例

```yaml
version: v3.0
name: "Java 项目构建"

on:
  push:
    branches: [main]

steps:
  - name: "检出代码"
    uses: checkout@latest
    
  - name: "设置 Java 环境"
    uses: setup-java@2.*
    with:
      java-version: "11"
      
  - name: "Maven 构建"
    run: |
      mvn clean compile test package
      
  - name: "上传构建产物"
    uses: upload-artifact@1.*
    with:
      name: "jar-files"
      path: "target/*.jar"
```

### 5.2 Node.js 项目示例

```yaml
version: v3.0
name: "Node.js 项目构建"

on:
  push:
    branches: [main]

steps:
  - name: "检出代码"
    uses: checkout@latest
    
  - name: "设置 Node.js 环境"
    uses: setup-node@3.*
    with:
      node-version: "16"
      
  - name: "安装依赖"
    run: npm ci
    
  - name: "运行测试"
    run: npm test
    
  - name: "构建项目"
    run: npm run build
    
  - name: "上传构建产物"
    uses: upload-artifact@1.*
    with:
      name: "dist"
      path: "dist/"
```

## 第六步：配置通知

### 6.1 添加通知配置

```yaml
# 在流水线末尾添加通知配置
notices:
  - type: email
    receivers: ["your-email@tencent.com"]
    title: "构建通知"
    content: |
      流水线: ${{ ci.pipeline_name }}
      状态: ${{ ci.build_status }}
      分支: ${{ ci.branch }}
      
  - type: wework-message
    receivers: ["@all"]
    title: "构建完成"
```

### 6.2 测试通知

1. 执行流水线
2. 检查邮件和企业微信通知
3. 确认通知内容正确

## 常见问题

### Q1: 流水线不触发怎么办？

**检查项**:
- 触发器分支配置是否正确
- 代码库 Webhook 是否配置
- 推送的分支是否匹配触发条件

**解决方法**:
```yaml
# 确认触发器配置
on:
  push:
    branches: ["main", "master", "develop"]  # 添加所有需要的分支
```

### Q2: 构建失败如何排查？

**排查步骤**:
1. 查看失败步骤的详细日志
2. 检查错误信息和错误码
3. 确认构建环境和依赖
4. 验证脚本在本地是否正常

### Q3: 如何查看构建产物？

**查看方法**:
1. 进入流水线执行详情
2. 点击"构建产物"标签
3. 下载或查看上传的文件

### Q4: 权限不足怎么处理？

**解决方法**:
1. 确认用户在项目成员中
2. 检查流水线权限设置
3. 联系项目管理员添加权限

## 下一步学习

### 进阶功能
- [变量和参数使用](./variables-guide.md)
- [插件市场探索](./plugin-usage.md)
- [多环境部署](./multi-env-deploy.md)
- [性能优化技巧](./performance-optimization.md)

### 最佳实践
- [流水线设计原则](./best-practices.md)
- [安全配置指南](./security-guide.md)
- [团队协作模式](./team-collaboration.md)
- [问题排查技巧](../50-bkci-troubleshooting/)

## 总结

恭喜！你已经完成了蓝盾的快速入门：

✅ **已完成**:
- 创建了第一个项目
- 关联了代码库
- 创建并执行了第一条流水线
- 配置了基础通知

🎯 **下一步**:
- 根据项目需求优化流水线
- 探索更多插件和功能
- 学习高级配置和最佳实践
- 参与社区交流和分享

**记住**: 蓝盾是一个强大的平台，慢慢探索，逐步掌握。遇到问题时，优先查看日志和文档，大部分问题都能快速解决！