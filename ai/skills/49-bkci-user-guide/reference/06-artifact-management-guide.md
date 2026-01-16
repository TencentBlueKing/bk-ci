# 制品管理与发布指南

## 概述

蓝盾制品库提供了完整的制品生命周期管理能力，支持多种制品类型的存储、版本管理、权限控制和自动清理。本章将详细介绍如何使用制品库进行制品管理和发布。

## 制品库类型

### 1. 流水线仓库
- **用途**: 存储流水线构建产生的制品
- **路径格式**: `/pipelineId/buildId/filename`
- **保留策略**: 默认保留60天
- **特点**: 与构建记录关联，支持构建历史追溯

### 2. 自定义仓库 (Generic)
- **用途**: 存储用户自定义的二进制文件
- **路径格式**: 用户自定义路径结构
- **保留策略**: 永久存储（可配置清理策略）
- **特点**: 灵活的目录结构，支持手动上传

### 3. Docker仓库
- **用途**: 存储Docker镜像
- **支持**: 标准Docker Registry API
- **版本管理**: 支持Tag和Digest管理

### 4. Helm仓库
- **用途**: 存储Helm Chart包
- **版本管理**: 支持语义化版本管理
- **依赖管理**: 支持Chart依赖解析

## 制品上传

### 1. 流水线中上传制品

#### 归档构件插件
```yaml
- name: 归档构件
  uses: archive@latest
  with:
    # 归档路径，支持通配符
    archivePath: |
      dist/**
      *.jar
      reports/**
    # 自定义元数据（可选）
    metadata:
      version: ${{ env.BUILD_VERSION }}
      environment: production
      buildType: release
```

#### 上传到自定义仓库
```yaml
- name: 上传到自定义仓库
  uses: upload-generic@latest
  with:
    # 本地文件路径
    localPath: dist/app.jar
    # 自定义仓库路径
    remotePath: /releases/v1.0.0/app.jar
    # 是否覆盖已存在文件
    overwrite: false
```

### 2. 手动上传制品

#### Web界面上传
1. 访问制品库页面
2. 选择目标仓库（自定义仓库）
3. 点击"上传文件"按钮
4. 选择文件并设置存储路径
5. 可选：添加元数据标签

#### 命令行上传
```bash
# 使用curl上传
curl -u username:password \
  -X PUT \
  "https://bkrepo.woa.com/generic/projectId/custom/path/to/file.jar" \
  --data-binary @local-file.jar

# 使用wget上传
wget --user=username --password=password \
  --method=PUT \
  --body-file=local-file.jar \
  "https://bkrepo.woa.com/generic/projectId/custom/path/to/file.jar"
```

### 3. 流水线变量上传
在流水线编辑页面设置文件类型变量：
1. 变量类型选择"文件"
2. 设置"自定义仓库路径"
3. 上传文件时会自动存储到指定路径

## 制品下载

### 1. 流水线中下载制品

#### 拉取构件插件
```yaml
- name: 拉取构件
  uses: download-artifact@latest
  with:
    # 构建号（可选，默认当前构建）
    buildId: ${{ env.TARGET_BUILD_ID }}
    # 下载路径模式
    downloadPath: "**/*.jar"
    # 本地存储路径
    localPath: ./artifacts/
```

#### 从自定义仓库下载
```yaml
- name: 从自定义仓库下载
  uses: download-generic@latest
  with:
    # 远程文件路径
    remotePath: /releases/v1.0.0/app.jar
    # 本地存储路径
    localPath: ./app.jar
    # 下载过滤条件（可选）
    filters:
      # 文件名模糊匹配
      filename: "*.jar"
      # 元数据过滤
      metadata:
        environment: production
      # 创建时间过滤
      createTimeAfter: "2024-01-01"
```

### 2. 命令行下载

#### 基本下载
```bash
# 使用wget下载
wget --user=username --password=password \
  "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"

# 使用curl下载
curl -u username:password \
  -o local-file.jar \
  "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"
```

#### 合研制品下载
```bash
# 需要添加特殊请求头
curl -u username:password \
  -H "X-BKREPO-ACCESS-FROM: api" \
  -L \
  "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"
```

### 3. 制品库客户端下载

#### 安装客户端
- [Windows (x64)](https://bkrepo.woa.com/generic/bkci-desktop/public/bkdrive/BKDrive-x64.exe)
- [MacOS (x64)](https://bkrepo.woa.com/generic/bkci-desktop/public/bkdrive/BKDrive-x64.pkg)
- [MacOS (arm64)](https://bkrepo.woa.com/generic/bkci-desktop/public/bkdrive/BKDrive-arm64.pkg)

#### 使用方法
1. 设置下载路径
2. 手动下载：在仓库中右键选择文件下载
3. 链接下载：复制浏览器中的下载链接，在客户端中粘贴下载

## 版本管理

### 1. 流水线版本管理

#### 版本生成规则
- **正式版本**: 变更提交到默认分支时生成
- **分支版本**: 变更首次提交到非默认分支时生成
- **版本回滚**: 使用历史版本生成新版本

#### 版本标识
```yaml
# 在流水线中设置版本标识
variables:
  BUILD_VERSION: "v1.0.${BUILD_NUMBER}"
  RELEASE_TAG: "${BRANCH_NAME}-${BUILD_VERSION}"
```

### 2. 语义化版本管理

#### SemVer规范
遵循 `major.minor.patch` 格式：
- **major**: 不兼容的API修改
- **minor**: 向后兼容的功能性新增
- **patch**: 向后兼容的问题修正

#### 自动版本生成
```yaml
- name: 生成版本号
  script: |
    # 基于Git标签生成版本
    if git describe --tags --exact-match HEAD 2>/dev/null; then
      VERSION=$(git describe --tags --exact-match HEAD)
    else
      VERSION="v0.0.0-$(git rev-parse --short HEAD)"
    fi
    echo "BUILD_VERSION=$VERSION" >> $GITHUB_ENV
```

### 3. 制品元数据管理

#### 添加元数据
```yaml
- name: 归档构件
  uses: archive@latest
  with:
    archivePath: dist/**
    metadata:
      version: ${{ env.BUILD_VERSION }}
      commit: ${{ env.GIT_COMMIT_ID }}
      branch: ${{ env.GIT_BRANCH }}
      buildTime: ${{ env.BUILD_TIME }}
      environment: production
```

#### 查询和过滤
```yaml
- name: 下载指定版本制品
  uses: download-generic@latest
  with:
    remotePath: "/releases/**"
    filters:
      metadata:
        version: "v1.0.0"
        environment: production
```

## 权限管理

### 1. 仓库权限设置

#### 权限级别
- **读取权限**: 可以下载制品
- **写入权限**: 可以上传制品
- **删除权限**: 可以删除制品
- **管理权限**: 可以修改仓库设置

#### 权限配置
1. 进入制品库管理页面
2. 选择目标仓库
3. 点击"权限设置"
4. 添加用户或用户组
5. 分配相应权限级别

### 2. 下载限制配置

#### 启用下载限制
1. 打开仓库设置页面
2. 开启"Web端下载限制"或"移动端下载限制"
3. 配置下载规则：
   - 文件名规则（支持通配符）
   - 元数据规则（key:value格式）

#### 下载规则示例
```
# 文件名规则
*.jar,*.war

# 元数据规则
environment:production
version:v1.*
```

### 3. 访问凭证管理

#### 生成访问Token
1. 进入制品库个人设置
2. 点击"生成Token"
3. 设置Token有效期
4. 复制生成的Token用于API访问

#### 使用Token访问
```bash
# 使用Token下载
curl -H "Authorization: Bearer YOUR_TOKEN" \
  "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"
```

## 清理策略

### 1. 自动清理配置

#### 支持的仓库类型
- Generic类型仓库
- Docker类型仓库  
- Helm类型仓库

#### 清理策略类型

##### 保留天数
```yaml
# 配置示例
cleanupPolicy:
  type: retentionDays
  retentionDays: 30  # 保留30天内的制品
```

##### 保留周期
```yaml
# 配置示例
cleanupPolicy:
  type: retentionDate
  retentionDate: "2024-01-01T00:00:00Z"  # 保留指定日期后的制品
```

##### 保留数目
```yaml
# 配置示例
cleanupPolicy:
  type: retentionCount
  retentionCount: 10  # 每个package保留最新10个版本
```

### 2. 清理目标配置

#### Generic仓库目录清理
```yaml
# 只清理指定目录
cleanupTargets:
  - "/test1"
  - "/test2"
  - "/releases/old"
```

#### Docker/Helm包清理
```yaml
# 只清理指定package
cleanupTargets:
  - "nginx"
  - "redis"
  - "mysql"
```

### 3. 默认清理策略

| 制品类型 | 保留策略 | 说明 |
|---------|---------|------|
| 流水线仓库构件 | 60天 | 包含二进制文件和报告 |
| 自定义仓库 | 永久 | 用户可配置清理策略 |
| 构建记录 | 10000条 | 用户可手动保留特定构建 |
| 流水线日志 | 60天 | 构建执行日志 |

## 最佳实践

### 1. 制品命名规范

#### 版本化命名
```bash
# 应用制品
app-${VERSION}.jar
app-${VERSION}-${BUILD_NUMBER}.jar

# 配置文件
config-${ENVIRONMENT}-${VERSION}.yml

# 文档制品
docs-${VERSION}.zip
api-docs-${VERSION}.html
```

#### 目录结构规范
```
/releases/
  ├── v1.0.0/
  │   ├── app.jar
  │   ├── config.yml
  │   └── docs.zip
  ├── v1.0.1/
  └── v1.1.0/

/snapshots/
  ├── develop/
  └── feature/
```

### 2. 元数据标准化

#### 必要元数据
```yaml
metadata:
  version: "v1.0.0"           # 版本号
  commit: "abc123def"         # Git提交ID
  branch: "main"              # 分支名
  buildTime: "2024-01-15T10:30:00Z"  # 构建时间
  environment: "production"    # 环境标识
  buildNumber: "123"          # 构建编号
```

#### 可选元数据
```yaml
metadata:
  # 质量信息
  testCoverage: "85%"
  codeQuality: "A"
  securityScan: "passed"
  
  # 依赖信息
  javaVersion: "11"
  springBootVersion: "2.7.0"
  
  # 部署信息
  deployable: "true"
  releaseNotes: "Bug fixes and performance improvements"
```

### 3. 制品生命周期管理

#### 开发阶段
```yaml
# 快照版本，频繁更新
- name: 上传快照版本
  uses: upload-generic@latest
  with:
    localPath: target/app-SNAPSHOT.jar
    remotePath: /snapshots/${BRANCH_NAME}/app-${BUILD_NUMBER}.jar
    overwrite: true
```

#### 测试阶段
```yaml
# 候选版本，稳定性测试
- name: 上传候选版本
  uses: upload-generic@latest
  with:
    localPath: target/app.jar
    remotePath: /candidates/v${VERSION}-rc${RC_NUMBER}/app.jar
    metadata:
      stage: "testing"
      testStatus: "pending"
```

#### 生产阶段
```yaml
# 正式版本，生产发布
- name: 上传正式版本
  uses: upload-generic@latest
  with:
    localPath: target/app.jar
    remotePath: /releases/v${VERSION}/app.jar
    metadata:
      stage: "production"
      releaseDate: ${{ env.RELEASE_DATE }}
```

### 4. 安全考虑

#### 敏感信息处理
- 不要在制品路径中包含敏感信息
- 使用元数据存储非敏感的描述信息
- 定期轮换访问Token
- 设置适当的权限级别

#### 制品完整性
```yaml
# 生成制品校验和
- name: 生成校验和
  script: |
    sha256sum target/app.jar > target/app.jar.sha256
    
- name: 上传制品和校验和
  uses: upload-generic@latest
  with:
    localPath: |
      target/app.jar
      target/app.jar.sha256
    remotePath: /releases/v${VERSION}/
```

## 故障排查

### 1. 上传问题

#### 文件大小限制
- 默认单文件限制：5MB
- 字体文件限制：10MB
- 解决方案：使用SPM平台处理大文件

#### 文件类型限制
- 检查文件扩展名是否在允许列表中
- 常见允许类型：js, css, html, jar, zip, png, jpg等
- 黑名单文件：node_modules, .git等

#### 权限问题
```bash
# 检查用户权限
curl -u username:password \
  "https://bkrepo.woa.com/api/user/info"

# 检查仓库权限
curl -u username:password \
  "https://bkrepo.woa.com/api/repository/projectId/repoName/permission"
```

### 2. 下载问题

#### 网络连接
```bash
# 测试连接性
ping dl.bkrepo.woa.com
curl -I "https://dl.bkrepo.woa.com"
```

#### 认证问题
```bash
# 验证凭证
curl -u username:password \
  "https://dl.bkrepo.woa.com/api/user/info"
```

#### 文件不存在
```bash
# 检查文件是否存在
curl -u username:password \
  -I "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"
```

### 3. 性能优化

#### 并发下载
```bash
# 使用多线程下载工具
aria2c -x 4 -s 4 \
  --header="Authorization: Basic $(echo -n username:password | base64)" \
  "https://dl.bkrepo.woa.com/generic/projectId/repoName/path/to/file.jar"
```

#### 缓存策略
- 利用HTTP缓存头
- 使用CDN加速（适用于公开制品）
- 本地缓存常用制品

## 总结

制品管理是CI/CD流程中的重要环节，合理的制品管理策略能够：

1. **提高构建效率**: 通过制品复用减少重复构建
2. **保证发布质量**: 通过版本管理确保发布的一致性
3. **降低存储成本**: 通过清理策略控制存储空间
4. **增强安全性**: 通过权限控制保护敏感制品

建议在项目初期就制定完整的制品管理规范，包括命名规则、版本策略、权限设置和清理策略，并在团队中严格执行。