# 构建环境与执行

## 概述

构建环境是流水线执行的基础设施，提供代码编译、测试、打包等操作所需的计算资源和运行环境。本章将详细介绍构建环境的配置、管理和优化方法。

## 构建环境类型

### 环境分类

#### 1. 无编译环境
**特点**: 轻量级，无需构建机资源
```yaml
适用场景:
  - 通知发送
  - 人工审批
  - API 调用
  - 简单脚本执行
  
资源消耗:
  - CPU: 极低
  - 内存: 极低
  - 存储: 无需持久化
  
执行限制:
  - 无文件系统访问
  - 无网络文件传输
  - 执行时间限制: 10 分钟
```

#### 2. Linux 构建机
**特点**: 通用性强，支持大部分开发语言
```yaml
适用场景:
  - Java/Maven/Gradle 构建
  - Node.js/NPM 构建
  - Python 构建
  - Go 构建
  - Docker 镜像构建
  
预装环境:
  - 操作系统: Ubuntu 18.04/20.04
  - 开发工具: Git, Docker, curl, wget
  - 运行时: Java 8/11, Node.js 14/16, Python 3.x
  - 构建工具: Maven, Gradle, NPM, Yarn
```

#### 3. Windows 构建机
**特点**: 支持 Windows 生态开发
```yaml
适用场景:
  - .NET Framework/.NET Core 构建
  - MSBuild 项目
  - PowerShell 脚本
  - Windows 应用打包
  
预装环境:
  - 操作系统: Windows Server 2019
  - 开发工具: Git, Visual Studio Build Tools
  - 运行时: .NET Framework 4.x, .NET Core 3.x/5.x
  - 构建工具: MSBuild, NuGet
```

#### 4. macOS 构建机
**特点**: 支持 iOS/macOS 应用开发
```yaml
适用场景:
  - iOS 应用构建
  - macOS 应用构建
  - Xcode 项目编译
  - 应用签名和分发
  
预装环境:
  - 操作系统: macOS Big Sur/Monterey
  - 开发工具: Xcode, Git, Homebrew
  - 运行时: Swift, Objective-C
  - 构建工具: xcodebuild, fastlane
```

### 构建机管理

#### 1. 公共构建机
**特点**: 平台提供的共享资源
```yaml
优势:
  - 免维护: 平台统一管理
  - 成本低: 按使用量计费
  - 环境标准: 预配置开发环境
  - 弹性扩容: 自动资源调度
  
限制:
  - 资源共享: 可能需要排队
  - 环境固定: 无法自定义深度配置
  - 网络限制: 可能无法访问内网资源
  
适用场景:
  - 标准化构建流程
  - 开源项目构建
  - 轻量级任务
```

#### 2. 私有构建机
**特点**: 用户自建的专用资源
```yaml
优势:
  - 专用资源: 无需与他人共享
  - 自定义环境: 完全控制构建环境
  - 内网访问: 可访问企业内网资源
  - 性能稳定: 资源独占，性能可控
  
责任:
  - 环境维护: 需要自行维护系统
  - 成本管理: 需要承担硬件成本
  - 安全管理: 需要确保环境安全
  
适用场景:
  - 企业内部项目
  - 特殊环境需求
  - 高性能要求
  - 安全敏感项目
```

#### 3. 第三方构建机
**特点**: 对接外部构建资源
```yaml
支持平台:
  - Jenkins: 对接 Jenkins 构建节点
  - GitHub Actions: 使用 GitHub 托管运行器
  - GitLab CI: 使用 GitLab Runner
  - 云服务商: AWS CodeBuild, 阿里云构建
  
集成方式:
  - API 对接: 通过 API 调用外部服务
  - Agent 代理: 部署代理程序转发任务
  - Webhook 回调: 通过 Webhook 接收结果
```

## 构建机配置

### 环境标签

#### 1. 标签系统
**作用**: 用于匹配合适的构建机
```yaml
系统标签:
  - linux: Linux 操作系统
  - windows: Windows 操作系统
  - macos: macOS 操作系统
  - docker: 支持 Docker
  
语言标签:
  - java: Java 开发环境
  - nodejs: Node.js 环境
  - python: Python 环境
  - dotnet: .NET 环境
  
工具标签:
  - maven: Maven 构建工具
  - gradle: Gradle 构建工具
  - npm: NPM 包管理器
  - xcode: Xcode 开发工具
```

#### 2. 标签匹配策略
```yaml
精确匹配:
  任务标签: [linux, java, maven]
  构建机标签: [linux, java, maven, docker]
  匹配结果: ✅ 匹配成功
  
部分匹配:
  任务标签: [linux, java, gradle]
  构建机标签: [linux, java, maven]
  匹配结果: ❌ 匹配失败 (缺少 gradle)
  
优先级匹配:
  多个构建机匹配时，按以下优先级选择:
  1. 空闲时间最长
  2. 标签匹配度最高
  3. 性能配置最优
```

### 环境变量配置

#### 1. 系统环境变量
```yaml
Linux 环境变量:
  PATH: /usr/local/bin:/usr/bin:/bin
  JAVA_HOME: /usr/lib/jvm/java-11-openjdk
  MAVEN_HOME: /opt/maven
  NODE_HOME: /usr/local/node
  
Windows 环境变量:
  PATH: C:\Windows\System32;C:\Program Files\Git\bin
  JAVA_HOME: C:\Program Files\Java\jdk-11
  DOTNET_ROOT: C:\Program Files\dotnet
  
macOS 环境变量:
  PATH: /usr/local/bin:/usr/bin:/bin
  JAVA_HOME: /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home
  XCODE_PATH: /Applications/Xcode.app/Contents/Developer
```

#### 2. 自定义环境变量
```yaml
流水线级别:
  APP_NAME: mobile-app
  APP_VERSION: 1.0.${BUILD_NUMBER}
  DEPLOY_ENV: ${DEPLOY_ENVIRONMENT}
  
任务级别:
  MAVEN_OPTS: -Xmx2g -XX:MaxPermSize=512m
  NODE_OPTIONS: --max-old-space-size=4096
  GRADLE_OPTS: -Xmx2g -Dfile.encoding=UTF-8
  
敏感变量:
  DB_PASSWORD: [加密存储]
  API_KEY: [凭证管理]
  SSH_PRIVATE_KEY: [文件凭证]
```

### 构建机性能配置

#### 1. 资源规格
```yaml
轻量级配置:
  CPU: 2 核
  内存: 4GB
  存储: 50GB SSD
  适用: 简单构建、测试任务
  
标准配置:
  CPU: 4 核
  内存: 8GB
  存储: 100GB SSD
  适用: 一般 Java/Node.js 项目
  
高性能配置:
  CPU: 8 核
  内存: 16GB
  存储: 200GB SSD
  适用: 大型项目、并行构建
  
专业配置:
  CPU: 16 核
  内存: 32GB
  存储: 500GB SSD
  适用: 企业级项目、复杂构建
```

#### 2. 性能优化
```yaml
CPU 优化:
  - 启用并行编译: -j$(nproc)
  - 使用多线程构建: -T 4C
  - 优化编译器选项: -O2
  
内存优化:
  - 调整 JVM 堆大小: -Xmx4g
  - 配置 Node.js 内存: --max-old-space-size=4096
  - 使用内存文件系统: tmpfs
  
存储优化:
  - 使用 SSD 存储
  - 启用构建缓存
  - 定期清理临时文件
```

## 构建执行管理

### 任务调度

#### 1. 调度策略
```yaml
先进先出 (FIFO):
  规则: 按任务提交时间排队
  适用: 公平调度，简单场景
  
优先级调度:
  规则: 按任务优先级排队
  配置:
    - 生产发布: 高优先级
    - 开发构建: 中优先级
    - 定时任务: 低优先级
    
负载均衡:
  规则: 分散任务到不同构建机
  策略:
    - 轮询分配
    - 最少任务优先
    - 资源使用率优先
```

#### 2. 并发控制
```yaml
流水线并发:
  同一流水线: 允许/禁止并发执行
  不同流水线: 独立并发控制
  
构建机并发:
  单机并发数: 根据资源配置调整
  任务隔离: 使用容器或虚拟机隔离
  
资源限制:
  CPU 限制: 限制任务 CPU 使用率
  内存限制: 限制任务内存使用量
  网络限制: 限制网络带宽使用
```

### 工作空间管理

#### 1. 工作空间结构
```yaml
标准工作空间:
  /data/landun/workspace/
  ├── {pipeline_id}/           # 流水线目录
  │   ├── {build_id}/         # 构建目录
  │   │   ├── src/            # 源码目录
  │   │   ├── target/         # 构建输出
  │   │   └── logs/           # 构建日志
  │   └── cache/              # 缓存目录
  └── shared/                 # 共享目录
      ├── tools/              # 构建工具
      └── cache/              # 全局缓存
```

#### 2. 工作空间清理
```yaml
清理策略:
  构建前清理: 清理上次构建残留
  构建后清理: 清理当前构建临时文件
  定期清理: 清理过期构建目录
  
清理配置:
  保留天数: 7 天
  保留构建数: 10 个
  清理规则: 
    - 保留最新构建
    - 保留成功构建
    - 清理失败构建
```

### 构建缓存

#### 1. 缓存类型
```yaml
依赖缓存:
  Maven: ~/.m2/repository
  NPM: ~/.npm
  Gradle: ~/.gradle/caches
  Pip: ~/.cache/pip
  
构建缓存:
  编译输出: target/, build/
  测试结果: test-results/
  静态分析: sonar-cache/
  
Docker 缓存:
  镜像层缓存: Docker layer cache
  构建缓存: Docker build cache
```

#### 2. 缓存策略
```yaml
缓存键策略:
  依赖文件: pom.xml, package.json 的 hash
  源码变更: Git commit hash
  环境配置: 构建环境标识
  
缓存生命周期:
  TTL: 7 天自动过期
  LRU: 最近最少使用淘汰
  手动清理: 支持手动清理缓存
  
缓存共享:
  项目级: 同项目流水线共享
  全局级: 所有项目共享
  私有级: 仅当前流水线使用
```

## 容器化构建

### Docker 构建环境

#### 1. 基础镜像选择
```yaml
官方基础镜像:
  - openjdk:11-jdk: Java 开发环境
  - node:16-alpine: Node.js 轻量环境
  - python:3.9-slim: Python 精简环境
  - golang:1.17-alpine: Go 开发环境
  
自定义镜像:
  - 基于官方镜像扩展
  - 预装项目依赖
  - 配置企业环境
  - 优化镜像大小
```

#### 2. Dockerfile 最佳实践
```dockerfile
# 多阶段构建示例
FROM node:16-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:16-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/node_modules ./node_modules
COPY . .
EXPOSE 3000
CMD ["npm", "start"]
```

#### 3. 容器配置
```yaml
资源限制:
  CPU: 2 核
  内存: 4GB
  存储: 20GB
  
网络配置:
  网络模式: bridge
  端口映射: 8080:8080
  DNS: 8.8.8.8
  
卷挂载:
  工作目录: /data/workspace
  缓存目录: /data/cache
  日志目录: /data/logs
```

### Kubernetes 构建

#### 1. Pod 配置
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: build-pod
spec:
  containers:
  - name: builder
    image: openjdk:11-jdk
    resources:
      requests:
        cpu: 1000m
        memory: 2Gi
      limits:
        cpu: 2000m
        memory: 4Gi
    volumeMounts:
    - name: workspace
      mountPath: /workspace
  volumes:
  - name: workspace
    emptyDir: {}
```

#### 2. 构建策略
```yaml
Pod 生命周期:
  创建: 任务开始时创建 Pod
  执行: 在 Pod 中执行构建任务
  清理: 任务完成后删除 Pod
  
资源调度:
  节点选择: 基于标签选择器
  资源预留: 预留 CPU 和内存
  亲和性: 配置 Pod 亲和性规则
```

## 构建监控

### 性能监控

#### 1. 资源使用监控
```yaml
CPU 监控:
  指标: CPU 使用率、负载均衡
  阈值: CPU > 80% 告警
  
内存监控:
  指标: 内存使用率、内存泄漏
  阈值: 内存 > 90% 告警
  
存储监控:
  指标: 磁盘使用率、IO 性能
  阈值: 磁盘 > 85% 告警
  
网络监控:
  指标: 网络带宽、连接数
  阈值: 带宽 > 80% 告警
```

#### 2. 构建性能分析
```yaml
构建时间分析:
  - 代码检出时间
  - 依赖下载时间
  - 编译构建时间
  - 测试执行时间
  - 制品上传时间
  
性能瓶颈识别:
  - 慢步骤识别
  - 资源瓶颈分析
  - 网络延迟分析
  - 缓存命中率分析
```

### 构建机健康检查

#### 1. 健康检查项
```yaml
系统检查:
  - 操作系统状态
  - 系统资源可用性
  - 网络连通性
  - 存储空间
  
服务检查:
  - Agent 服务状态
  - Docker 服务状态
  - 构建工具可用性
  - 环境变量配置
  
安全检查:
  - 系统补丁状态
  - 安全配置检查
  - 访问权限验证
  - 日志审计
```

#### 2. 自动恢复机制
```yaml
故障检测:
  - 心跳检测: 定期发送心跳
  - 健康检查: 定期执行健康检查
  - 异常监控: 监控异常日志
  
自动恢复:
  - 服务重启: 自动重启异常服务
  - 环境重置: 重置构建环境
  - 节点替换: 替换故障节点
  
故障通知:
  - 实时告警: 立即通知运维人员
  - 状态报告: 定期发送状态报告
  - 恢复确认: 确认恢复成功
```

## 环境安全

### 访问控制

#### 1. 网络安全
```yaml
网络隔离:
  - VPC 隔离: 使用虚拟私有云
  - 子网分割: 按环境分割子网
  - 安全组: 配置访问规则
  
访问控制:
  - 白名单: 限制访问来源
  - VPN 接入: 通过 VPN 访问
  - 堡垒机: 统一访问入口
```

#### 2. 权限管理
```yaml
最小权限原则:
  - 构建用户权限最小化
  - 临时权限使用
  - 权限定期审查
  
权限隔离:
  - 项目间权限隔离
  - 环境间权限隔离
  - 用户权限隔离
```

### 数据安全

#### 1. 敏感数据保护
```yaml
凭证管理:
  - 加密存储: 敏感信息加密存储
  - 访问控制: 限制凭证访问权限
  - 定期轮换: 定期更新凭证
  
日志脱敏:
  - 敏感信息过滤
  - 日志访问控制
  - 日志保留策略
```

#### 2. 数据传输安全
```yaml
传输加密:
  - HTTPS: 使用 HTTPS 传输
  - SSH: 使用 SSH 协议
  - TLS: 使用 TLS 加密
  
数据完整性:
  - 校验和: 文件完整性校验
  - 数字签名: 制品签名验证
  - 版本控制: 代码版本追踪
```

## 常见问题

### 环境问题

**Q: 构建机资源不足**
```yaml
解决方案:
  1. 增加构建机数量
  2. 升级构建机配置
  3. 优化构建脚本
  4. 使用构建缓存
  5. 调整并发设置
```

**Q: 环境配置不一致**
```yaml
解决方案:
  1. 使用容器化构建
  2. 标准化环境镜像
  3. 自动化环境配置
  4. 环境配置版本控制
  5. 定期环境检查
```

### 性能问题

**Q: 构建速度慢**
```yaml
优化策略:
  1. 启用并行构建
  2. 使用增量构建
  3. 优化依赖管理
  4. 使用构建缓存
  5. 选择高性能构建机
```

**Q: 网络访问慢**
```yaml
解决方案:
  1. 使用国内镜像源
  2. 配置代理服务器
  3. 启用本地缓存
  4. 优化网络配置
  5. 使用 CDN 加速
```

## 下一步

完成构建环境配置后，建议继续学习：

1. [制品管理与发布](./06-artifact-management-guide.md) - 管理构建制品
2. [质量红线与代码检查](./07-quality-gate-guide.md) - 配置质量门禁
3. [监控与日志管理](./09-monitoring-logging-guide.md) - 监控构建过程

---

*本章介绍了构建环境的配置和管理，为高效稳定的 CI/CD 提供基础保障。*