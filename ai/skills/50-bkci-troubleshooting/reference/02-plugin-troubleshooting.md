# 插件问题排查详细指南

## 📋 概述

插件是 BK-CI 流水线的核心执行单元，本文档提供插件相关问题的详细排查方法和解决方案，涵盖官方插件、第三方插件、自定义插件等各种场景。

## 🔍 插件问题分类体系

### 1. 执行失败类问题

#### 1.1 配置参数错误

**问题特征**:
- 插件启动时参数验证失败
- 必填参数缺失或类型错误
- 参数值不符合预期格式

**常见错误示例**:
```bash
# 参数类型错误
Error: Parameter 'timeout' expects number, got string "300s"

# 必填参数缺失
Error: Required parameter 'repository' is missing

# 参数格式错误
Error: Parameter 'branch' format invalid: expected branch name, got "feature/user-story"

# 参数值超出范围
Error: Parameter 'parallelism' value 100 exceeds maximum limit 50
```

**排查步骤**:

**Step 1: 检查插件文档**
```bash
# 查看插件详细信息
1. 进入研发商店 -> 找到对应插件
2. 查看插件详情页面
3. 阅读参数说明和示例
4. 确认必填参数和可选参数
5. 查看参数类型和格式要求
```

**Step 2: 验证参数配置**
```yaml
# 错误配置示例
- name: "Git拉取"
  uses: "checkout@4"
  with:
    repository: ""                    # 空值错误
    branch: null                      # 类型错误
    timeout: "300"                    # 类型错误，应为数字
    enableSubmodule: "true"           # 类型错误，应为布尔值
    
# 正确配置示例
- name: "Git拉取"
  uses: "checkout@4"
  with:
    repository: "https://github.com/user/repo.git"  # 字符串
    branch: "master"                                 # 字符串
    timeout: 300                                     # 数字
    enableSubmodule: true                            # 布尔值
    submodulePath: "."                              # 可选参数
    enableGitLfs: false                             # 布尔值
```

**Step 3: 参数类型对照表**

| 参数类型 | 正确格式 | 错误格式 | 说明 |
|----------|----------|----------|------|
| `string` | `"hello"` | `hello` (无引号) | 字符串必须用引号 |
| `number` | `300` | `"300"` | 数字不能用引号 |
| `boolean` | `true` | `"true"` | 布尔值不能用引号 |
| `array` | `["a", "b"]` | `"a,b"` | 数组格式 |
| `object` | `{key: value}` | `"key=value"` | 对象格式 |

#### 1.2 环境依赖问题

**问题特征**:
- 插件执行时找不到依赖工具
- 工具版本不兼容
- 环境变量配置错误

**常见依赖问题**:
```bash
# 工具未安装
Error: Command 'mvn' not found in PATH
Error: 'docker' command not available
Error: Python module 'requests' not found

# 版本不兼容
Error: Java version 1.7 not supported, requires 1.8+
Error: Node.js version 12.x required, found 10.x
Error: Maven version 3.6+ required, found 3.3.9

# 环境变量缺失
Error: JAVA_HOME environment variable not set
Error: ANDROID_HOME not configured
Error: GOPATH environment variable required
```

**排查和解决方案**:

**Step 1: 环境检查脚本**
```bash
# 创建环境检查步骤
- name: "环境检查"
  run: |
    echo "=== 系统信息 ==="
    uname -a
    cat /etc/os-release
    
    echo "=== 已安装工具 ==="
    which git && git --version || echo "Git not found"
    which java && java -version || echo "Java not found"
    which mvn && mvn -version || echo "Maven not found"
    which node && node --version || echo "Node.js not found"
    which npm && npm --version || echo "NPM not found"
    which docker && docker --version || echo "Docker not found"
    which python && python --version || echo "Python not found"
    which pip && pip --version || echo "Pip not found"
    
    echo "=== 环境变量 ==="
    echo "JAVA_HOME: ${JAVA_HOME:-未设置}"
    echo "MAVEN_HOME: ${MAVEN_HOME:-未设置}"
    echo "NODE_HOME: ${NODE_HOME:-未设置}"
    echo "PYTHON_HOME: ${PYTHON_HOME:-未设置}"
    echo "PATH: $PATH"
    
    echo "=== 磁盘空间 ==="
    df -h
    
    echo "=== 内存信息 ==="
    free -h
```

**Step 2: 依赖安装和配置**
```yaml
# Java 环境配置
- name: "Java环境准备"
  run: |
    # 安装 OpenJDK 8
    sudo apt-get update
    sudo apt-get install -y openjdk-8-jdk
    
    # 设置环境变量
    export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
    echo "JAVA_HOME=$JAVA_HOME" >> $GITHUB_ENV
    echo "$JAVA_HOME/bin" >> $GITHUB_PATH
    
    # 验证安装
    java -version
    javac -version

# Node.js 环境配置
- name: "Node.js环境准备"
  run: |
    # 使用 nvm 安装指定版本
    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
    source ~/.bashrc
    nvm install 16
    nvm use 16
    
    # 验证安装
    node --version
    npm --version

# Docker 环境配置
- name: "Docker环境准备"
  run: |
    # 启动 Docker 服务
    sudo systemctl start docker
    sudo systemctl enable docker
    
    # 添加用户到 docker 组
    sudo usermod -aG docker $USER
    
    # 验证 Docker
    docker --version
    docker info
```

**Step 3: 构建机环境标准化**
```dockerfile
# 创建标准化构建环境镜像
FROM ubuntu:20.04

# 安装基础工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 安装 Java
RUN apt-get update && apt-get install -y openjdk-8-jdk
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# 安装 Maven
RUN wget https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz \
    && tar -xzf apache-maven-3.8.6-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.8.6 /opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=$PATH:$MAVEN_HOME/bin

# 安装 Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash - \
    && apt-get install -y nodejs

# 安装 Docker
RUN curl -fsSL https://get.docker.com -o get-docker.sh \
    && sh get-docker.sh

# 设置工作目录
WORKDIR /workspace

# 验证安装
RUN java -version && \
    mvn -version && \
    node --version && \
    npm --version && \
    docker --version
```

#### 1.3 权限问题

**问题特征**:
- 文件或目录访问被拒绝
- 网络连接权限不足
- API 调用认证失败

**常见权限错误**:
```bash
# 文件系统权限
Permission denied: cannot create directory '/opt/app'
Permission denied: cannot write to file '/etc/hosts'
Permission denied: cannot execute '/usr/local/bin/script.sh'

# 网络访问权限
Connection refused: unable to connect to api.github.com:443
SSL certificate verification failed
Proxy authentication required

# API 认证失败
HTTP 401: Unauthorized access to repository
HTTP 403: Forbidden - insufficient permissions
Token expired or invalid
```

**解决方案**:

**Step 1: 文件权限处理**
```bash
# 检查和修复文件权限
- name: "权限检查和修复"
  run: |
    echo "=== 当前用户信息 ==="
    whoami
    id
    groups
    
    echo "=== 工作目录权限 ==="
    ls -la ${{ ci.workspace }}
    
    echo "=== 修复权限 ==="
    # 确保工作目录可写
    sudo chown -R $(whoami):$(whoami) ${{ ci.workspace }}
    chmod -R 755 ${{ ci.workspace }}
    
    # 确保脚本可执行
    find ${{ ci.workspace }} -name "*.sh" -exec chmod +x {} \;
    
    echo "=== 验证权限 ==="
    ls -la ${{ ci.workspace }}
```

**Step 2: 网络权限配置**
```yaml
# 代理配置
- name: "网络配置"
  run: |
    # 配置 HTTP 代理
    export http_proxy=http://proxy.company.com:8080
    export https_proxy=http://proxy.company.com:8080
    export no_proxy=localhost,127.0.0.1,.company.com
    
    # 配置 Git 代理
    git config --global http.proxy http://proxy.company.com:8080
    git config --global https.proxy http://proxy.company.com:8080
    
    # 配置 NPM 代理
    npm config set proxy http://proxy.company.com:8080
    npm config set https-proxy http://proxy.company.com:8080
    
    # 配置 Maven 代理
    mkdir -p ~/.m2
    cat > ~/.m2/settings.xml << EOF
    <settings>
      <proxies>
        <proxy>
          <id>company-proxy</id>
          <active>true</active>
          <protocol>http</protocol>
          <host>proxy.company.com</host>
          <port>8080</port>
        </proxy>
      </proxies>
    </settings>
    EOF
```

**Step 3: API 认证配置**
```yaml
# 代码库访问配置
- name: "Git认证配置"
  uses: "checkout@4"
  with:
    repository: "https://github.com/user/private-repo.git"
    token: "${{ settings.GITHUB_TOKEN }}"        # GitHub Token
    # 或使用 SSH Key
    sshKey: "${{ settings.SSH_PRIVATE_KEY }}"
    
# Docker 镜像仓库认证
- name: "Docker登录"
  run: |
    echo "${{ settings.DOCKER_PASSWORD }}" | \
    docker login registry.company.com \
      --username "${{ settings.DOCKER_USERNAME }}" \
      --password-stdin

# API 调用认证
- name: "API调用"
  run: |
    # 使用 Bearer Token
    curl -H "Authorization: Bearer ${{ settings.API_TOKEN }}" \
         https://api.company.com/v1/data
         
    # 使用基础认证
    curl -u "${{ settings.API_USERNAME }}:${{ settings.API_PASSWORD }}" \
         https://api.company.com/v1/data
```

#### 1.4 超时问题

**问题特征**:
- 插件执行时间超过设定限制
- 网络操作响应缓慢
- 大文件传输超时

**超时类型分析**:
```bash
# 插件执行超时
Error: Plugin execution timeout after 1800 seconds
Error: Task killed due to timeout (3600s)

# 网络请求超时
Error: Connection timeout: Read timed out after 30 seconds
Error: Download timeout: Failed to download after 300 seconds

# 构建过程超时
Error: Compilation timeout: Process killed after 2 hours
Error: Test execution timeout: Tests running for more than 45 minutes
```

**解决策略**:

**Step 1: 调整超时设置**
```yaml
# 插件级别超时设置
- name: "长时间任务"
  uses: "maven@1.*"
  timeout: 7200  # 2小时超时
  with:
    goals: "clean package"
    
# Job 级别超时设置
jobs:
  - name: "集成测试"
    timeout: 3600  # 1小时超时
    steps:
      - uses: "integration-test@1.*"
      
# Stage 级别超时设置
stages:
  - name: "完整构建"
    timeout: 10800  # 3小时超时
    jobs:
      - name: "构建任务"
```

**Step 2: 网络超时优化**
```yaml
# Git 操作超时配置
- name: "Git拉取"
  uses: "checkout@4"
  with:
    timeout: 600          # 10分钟超时
    retryCount: 3         # 重试3次
    retryInterval: 30     # 重试间隔30秒
    
# HTTP 请求超时配置
- name: "API调用"
  run: |
    # 设置 curl 超时
    curl --connect-timeout 30 \
         --max-time 300 \
         --retry 3 \
         --retry-delay 10 \
         https://api.example.com/data
         
# Maven 下载超时配置
- name: "Maven构建"
  uses: "maven@1.*"
  with:
    mavenOpts: "-Dmaven.wagon.http.connectionTimeout=60000 -Dmaven.wagon.http.readTimeout=300000"
```

**Step 3: 性能优化**
```yaml
# 并行处理减少总时间
- name: "并行测试"
  uses: "maven@1.*"
  with:
    goals: "test"
    parallelThreads: 4    # 4个线程并行
    
# 增量处理
- name: "增量构建"
  uses: "maven@1.*"
  with:
    goals: "compile"
    incrementalBuild: true
    
# 缓存机制
- name: "Maven构建"
  uses: "maven@1.*"
  with:
    enableCache: true
    cacheKey: "maven-${{ hashFiles('pom.xml') }}"
```

### 2. 输出异常类问题

#### 2.1 输出变量错误

**问题特征**:
- 插件输出变量值为空
- 变量类型不符合预期
- 跨步骤变量传递失败

**常见输出问题**:
```bash
# 输出变量为空
Warning: Output variable 'artifact_path' is empty
Error: Required output 'build_number' not found

# 变量类型错误
Error: Expected number for 'exit_code', got string "success"
Error: Expected array for 'test_results', got string

# 变量作用域问题
Error: Cannot access output 'compile_result' from different job
Error: Output variable 'version' not available in next stage
```

**排查和解决**:

**Step 1: 输出变量定义**
```yaml
# 正确的输出变量定义
- name: "编译构建"
  uses: "maven@1.*"
  outputs:
    - name: "artifact_path"
      value: "target/app-${{ variables.VERSION }}.jar"
    - name: "build_status"
      value: "success"
    - name: "test_count"
      value: 150
    - name: "coverage_rate"
      value: 85.5
      
# 动态输出变量
- name: "动态输出"
  run: |
    # 计算并输出变量
    BUILD_TIME=$(date +%s)
    COMMIT_HASH=$(git rev-parse --short HEAD)
    
    # 设置输出变量
    echo "BUILD_TIME=$BUILD_TIME" >> $BK_CI_BUILD_OUTPUT
    echo "COMMIT_HASH=$COMMIT_HASH" >> $BK_CI_BUILD_OUTPUT
```

**Step 2: 变量引用和传递**
```yaml
# 同一 Job 内引用
jobs:
  - name: "build-and-test"
    steps:
      - name: "编译"
        id: "compile"
        uses: "maven@1.*"
        outputs:
          - name: "jar_path"
            value: "target/app.jar"
            
      - name: "测试"
        run: |
          # 引用同一 Job 内的输出
          echo "测试文件: ${{ steps.compile.outputs.jar_path }}"
          java -jar ${{ steps.compile.outputs.jar_path }} --test

# 跨 Job 引用
jobs:
  - name: "build"
    steps:
      - name: "maven-build"
        uses: "maven@1.*"
        outputs:
          - name: "artifact"
            value: "target/app.jar"
            
  - name: "deploy"
    needs: ["build"]
    steps:
      - name: "部署"
        run: |
          # 引用其他 Job 的输出
          echo "部署文件: ${{ jobs.build.steps.maven-build.outputs.artifact }}"
```

**Step 3: 输出变量调试**
```yaml
# 输出变量调试
- name: "变量调试"
  run: |
    echo "=== 所有输出变量 ==="
    cat $BK_CI_BUILD_OUTPUT || echo "输出文件不存在"
    
    echo "=== 环境变量 ==="
    printenv | grep BK_CI | sort
    
    echo "=== 步骤输出验证 ==="
    echo "编译结果: ${{ steps.compile.outputs.result }}"
    echo "制品路径: ${{ steps.compile.outputs.artifact_path }}"
```

#### 2.2 制品上传失败

**问题特征**:
- 制品文件不存在或路径错误
- 制品库连接失败
- 文件大小超过限制

**常见制品问题**:
```bash
# 文件路径问题
Error: Artifact file 'target/app.jar' not found
Error: Path 'dist/' is a directory, expected file
Error: Wildcard pattern 'build/*.zip' matched no files

# 制品库连接问题
Error: Failed to connect to artifact repository
Error: Authentication failed for artifact upload
Error: Insufficient storage space in repository

# 文件大小限制
Error: File size 2.5GB exceeds maximum limit 2GB
Error: Total artifact size 5GB exceeds project quota 3GB
```

**解决方案**:

**Step 1: 制品路径验证**
```yaml
# 制品生成和验证
- name: "构建制品"
  uses: "maven@1.*"
  with:
    goals: "clean package"
    
- name: "制品验证"
  run: |
    echo "=== 构建目录内容 ==="
    find target -type f -name "*.jar" -ls
    
    echo "=== 制品文件信息 ==="
    if [ -f "target/app.jar" ]; then
      ls -lh target/app.jar
      file target/app.jar
      echo "制品文件存在且有效"
    else
      echo "错误: 制品文件不存在"
      exit 1
    fi
    
- name: "上传制品"
  uses: "upload-artifact@1.*"
  with:
    artifactPath: "target/app.jar"
    artifactName: "app-${{ variables.VERSION }}.jar"
```

**Step 2: 制品库配置**
```yaml
# 制品库认证配置
- name: "制品库上传"
  uses: "upload-artifact@1.*"
  with:
    artifactPath: "target/*.jar"
    repository: "maven-releases"
    credentials: "${{ settings.ARTIFACT_CREDENTIALS }}"
    
# 自定义制品库配置
- name: "自定义制品上传"
  run: |
    # 配置制品库认证
    echo "machine nexus.company.com" > ~/.netrc
    echo "login ${{ settings.NEXUS_USERNAME }}" >> ~/.netrc
    echo "password ${{ settings.NEXUS_PASSWORD }}" >> ~/.netrc
    chmod 600 ~/.netrc
    
    # 上传制品
    mvn deploy -DskipTests \
      -DaltDeploymentRepository=nexus::default::http://nexus.company.com/repository/maven-releases/
```

**Step 3: 大文件处理**
```yaml
# 大文件分片上传
- name: "大文件处理"
  run: |
    ARTIFACT_FILE="target/large-app.jar"
    
    # 检查文件大小
    FILE_SIZE=$(stat -c%s "$ARTIFACT_FILE")
    MAX_SIZE=$((2 * 1024 * 1024 * 1024))  # 2GB
    
    if [ $FILE_SIZE -gt $MAX_SIZE ]; then
      echo "文件过大，进行压缩处理"
      
      # 压缩文件
      gzip -9 "$ARTIFACT_FILE"
      ARTIFACT_FILE="${ARTIFACT_FILE}.gz"
      
      # 重新检查大小
      NEW_SIZE=$(stat -c%s "$ARTIFACT_FILE")
      echo "压缩后大小: $(($NEW_SIZE / 1024 / 1024))MB"
    fi
    
    # 上传处理后的文件
    echo "PROCESSED_ARTIFACT=$ARTIFACT_FILE" >> $BK_CI_BUILD_OUTPUT

- name: "上传处理后的制品"
  uses: "upload-artifact@1.*"
  with:
    artifactPath: "${{ steps.process.outputs.PROCESSED_ARTIFACT }}"
```

#### 2.3 报告生成失败

**问题特征**:
- 测试报告格式错误
- 报告模板缺失
- 数据解析失败

**报告问题分析**:
```bash
# 报告格式问题
Error: Invalid XML format in test report
Error: JSON parse error in coverage report
Error: Missing required fields in report data

# 模板问题
Error: Report template 'junit.xsl' not found
Error: Template rendering failed: undefined variable 'testResults'

# 数据问题
Error: No test results found to generate report
Error: Coverage data incomplete or corrupted
```

**解决方案**:

**Step 1: 报告格式标准化**
```xml
<!-- JUnit 测试报告标准格式 -->
<?xml version="1.0" encoding="UTF-8"?>
<testsuite name="com.example.TestSuite" 
           tests="10" 
           failures="1" 
           errors="0" 
           time="15.5">
  <testcase classname="com.example.TestClass" 
            name="testMethod1" 
            time="1.2">
  </testcase>
  <testcase classname="com.example.TestClass" 
            name="testMethod2" 
            time="0.8">
    <failure message="AssertionError" type="junit.framework.AssertionFailedError">
      Expected: true but was: false
    </failure>
  </testcase>
</testsuite>
```

```json
// 覆盖率报告标准格式
{
  "coverage": {
    "lines": {
      "total": 1000,
      "covered": 850,
      "percentage": 85.0
    },
    "branches": {
      "total": 200,
      "covered": 170,
      "percentage": 85.0
    },
    "functions": {
      "total": 100,
      "covered": 95,
      "percentage": 95.0
    }
  },
  "files": [
    {
      "path": "src/main/java/App.java",
      "lines": {"total": 50, "covered": 45},
      "branches": {"total": 10, "covered": 9}
    }
  ]
}
```

**Step 2: 报告生成配置**
```yaml
# Maven 测试报告配置
- name: "Maven测试"
  uses: "maven@1.*"
  with:
    goals: "test"
    reportPath: "target/surefire-reports/"
    reportFormat: "junit"
    
# 自定义报告生成
- name: "生成测试报告"
  run: |
    # 确保报告目录存在
    mkdir -p reports/junit
    mkdir -p reports/coverage
    
    # 运行测试并生成报告
    mvn test \
      -Dmaven.test.failure.ignore=true \
      -Djacoco.destFile=reports/coverage/jacoco.exec
      
    # 生成覆盖率报告
    mvn jacoco:report \
      -Djacoco.dataFile=reports/coverage/jacoco.exec \
      -Djacoco.outputDirectory=reports/coverage
      
    # 验证报告文件
    ls -la reports/junit/
    ls -la reports/coverage/
    
    # 检查报告格式
    xmllint --noout reports/junit/TEST-*.xml || echo "XML格式验证失败"
```

**Step 3: 报告数据处理**
```python
# 报告数据处理脚本
#!/usr/bin/env python3
import json
import xml.etree.ElementTree as ET
from pathlib import Path

def process_junit_reports(report_dir):
    """处理 JUnit 测试报告"""
    reports = []
    
    for xml_file in Path(report_dir).glob("TEST-*.xml"):
        try:
            tree = ET.parse(xml_file)
            root = tree.getroot()
            
            report = {
                "name": root.get("name"),
                "tests": int(root.get("tests", 0)),
                "failures": int(root.get("failures", 0)),
                "errors": int(root.get("errors", 0)),
                "time": float(root.get("time", 0))
            }
            
            reports.append(report)
            
        except Exception as e:
            print(f"处理报告文件 {xml_file} 失败: {e}")
            
    return reports

def generate_summary_report(reports):
    """生成汇总报告"""
    total_tests = sum(r["tests"] for r in reports)
    total_failures = sum(r["failures"] for r in reports)
    total_errors = sum(r["errors"] for r in reports)
    total_time = sum(r["time"] for r in reports)
    
    summary = {
        "total_tests": total_tests,
        "total_failures": total_failures,
        "total_errors": total_errors,
        "success_rate": (total_tests - total_failures - total_errors) / total_tests * 100 if total_tests > 0 else 0,
        "total_time": total_time,
        "reports": reports
    }
    
    return summary

if __name__ == "__main__":
    reports = process_junit_reports("target/surefire-reports")
    summary = generate_summary_report(reports)
    
    # 输出汇总报告
    with open("test-summary.json", "w") as f:
        json.dump(summary, f, indent=2)
        
    print(f"测试汇总: {summary['total_tests']} 个测试, 成功率 {summary['success_rate']:.1f}%")
```

### 3. 版本兼容类问题

#### 3.1 插件版本管理

**版本选择策略**:
```yaml
# 1. 固定版本 (推荐生产环境)
- uses: "maven@1.5.2"          # 精确版本
- uses: "checkout@4.1.0"       # 精确版本

# 2. 主版本固定 (推荐开发环境)
- uses: "maven@1.*"            # 1.x 最新版本
- uses: "checkout@4.*"         # 4.x 最新版本

# 3. 最新版本 (谨慎使用)
- uses: "maven@latest"         # 最新版本，可能不稳定
```

**版本升级流程**:
```yaml
# 分阶段版本升级
stages:
  - name: "版本测试"
    jobs:
      - name: "新版本验证"
        steps:
          # 测试新版本
          - name: "测试新版本插件"
            uses: "maven@2.0.0"
            with:
              goals: "clean compile"
              testMode: true
              
          # 对比测试结果
          - name: "结果对比"
            run: |
              echo "新版本测试完成，对比结果..."
              
  - name: "灰度升级"
    condition: "${{ stages.version-test.result == 'success' }}"
    jobs:
      - name: "部分项目升级"
        steps:
          - uses: "maven@2.0.0"  # 在部分项目中使用新版本
          
  - name: "全面升级"
    condition: "${{ stages.gray-upgrade.result == 'success' }}"
    jobs:
      - name: "全量升级"
        steps:
          - uses: "maven@2.0.0"  # 全面使用新版本
```

#### 3.2 平台兼容性

**平台版本检查**:
```yaml
# 检查平台版本兼容性
- name: "平台兼容性检查"
  run: |
    echo "=== BK-CI 平台信息 ==="
    echo "平台版本: ${{ ci.version }}"
    echo "API版本: ${{ ci.api_version }}"
    
    # 检查插件兼容性
    PLUGIN_MIN_VERSION="1.4.0"
    CURRENT_VERSION="${{ ci.version }}"
    
    if [ "$(printf '%s\n' "$PLUGIN_MIN_VERSION" "$CURRENT_VERSION" | sort -V | head -n1)" != "$PLUGIN_MIN_VERSION" ]; then
      echo "错误: 当前平台版本 $CURRENT_VERSION 低于插件要求的最低版本 $PLUGIN_MIN_VERSION"
      exit 1
    fi
    
    echo "平台版本兼容性检查通过"
```

**API 兼容性处理**:
```yaml
# API 版本适配
- name: "API兼容性处理"
  run: |
    API_VERSION="${{ ci.api_version }}"
    
    case "$API_VERSION" in
      "v3")
        echo "使用 API v3"
        curl -H "API-Version: v3" "${{ ci.api_url }}/builds"
        ;;
      "v4")
        echo "使用 API v4"
        curl -H "API-Version: v4" "${{ ci.api_url }}/v4/builds"
        ;;
      *)
        echo "不支持的 API 版本: $API_VERSION"
        exit 1
        ;;
    esac
```

## 🔧 插件开发问题

### 1. 自定义插件开发

#### 1.1 插件结构问题

**标准插件目录结构**:
```
my-plugin/
├── task.json              # 插件配置文件
├── src/                   # 源代码目录
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/MyPlugin.java
│   │   └── resources/
│   └── test/
├── pom.xml               # Maven 配置 (Java插件)
├── package.json          # NPM 配置 (Node.js插件)
├── requirements.txt      # Python 依赖 (Python插件)
├── README.md            # 插件说明文档
└── CHANGELOG.md         # 版本更新日志
```

**task.json 配置规范**:
```json
{
  "atomCode": "myPlugin",
  "execution": {
    "packagePath": "my-plugin-1.0.jar",
    "language": "java",
    "minimumVersion": "1.8",
    "demands": [],
    "target": "my-plugin-1.0.jar"
  },
  "input": {
    "inputParam": {
      "label": "输入参数",
      "type": "vuex-input",
      "required": true,
      "default": "",
      "desc": "参数描述",
      "groupName": "基础配置"
    },
    "optionalParam": {
      "label": "可选参数",
      "type": "selector",
      "required": false,
      "default": "option1",
      "options": [
        {"id": "option1", "name": "选项1"},
        {"id": "option2", "name": "选项2"}
      ],
      "desc": "可选参数说明"
    }
  },
  "output": {
    "outputParam": {
      "description": "输出参数",
      "type": "string"
    }
  }
}
```

#### 1.2 插件执行问题

**Java 插件示例**:
```java
package com.example;

import java.util.Map;

public class MyPlugin {
    public static void main(String[] args) {
        try {
            // 读取输入参数
            Map<String, String> params = readInputParams();
            String inputParam = params.get("inputParam");
            
            // 验证参数
            if (inputParam == null || inputParam.isEmpty()) {
                System.out.println("##[error] 必填参数 inputParam 不能为空");
                System.exit(2);  // ErrorType: 2 (用户配置问题)
            }
            
            // 执行插件逻辑
            System.out.println("##[info] 开始执行插件逻辑");
            String result = executeLogic(inputParam);
            
            // 输出结果
            System.out.println("##[info] 插件执行完成");
            writeOutput("outputParam", result);
            
            System.exit(0);  // 成功退出
            
        } catch (Exception e) {
            System.out.println("##[error] 插件执行失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);  // ErrorType: 1 (插件问题)
        }
    }
    
    private static Map<String, String> readInputParams() {
        // 从环境变量或文件读取参数
        // 实现参数读取逻辑
        return null;
    }
    
    private static String executeLogic(String input) {
        // 实现具体的插件逻辑
        return "processed: " + input;
    }
    
    private static void writeOutput(String key, String value) {
        // 输出结果到指定文件或环境变量
        System.out.println("##[output] " + key + "=" + value);
    }
}
```

**Python 插件示例**:
```python
#!/usr/bin/env python3
import os
import sys
import json
import traceback

def read_input_params():
    """读取输入参数"""
    params = {}
    
    # 从环境变量读取参数
    for key, value in os.environ.items():
        if key.startswith('INPUT_'):
            param_name = key[6:].lower()  # 移除 INPUT_ 前缀
            params[param_name] = value
            
    return params

def write_output(key, value):
    """输出结果"""
    print(f"##[output] {key}={value}")
    
    # 也可以写入到输出文件
    output_file = os.environ.get('BK_CI_BUILD_OUTPUT')
    if output_file:
        with open(output_file, 'a') as f:
            f.write(f"{key}={value}\n")

def execute_logic(input_param):
    """执行插件逻辑"""
    # 实现具体的插件逻辑
    print(f"##[info] 处理输入参数: {input_param}")
    
    # 模拟处理过程
    result = f"processed: {input_param}"
    
    return result

def main():
    try:
        # 读取输入参数
        params = read_input_params()
        input_param = params.get('inputparam')
        
        # 验证参数
        if not input_param:
            print("##[error] 必填参数 inputParam 不能为空")
            sys.exit(2)  # ErrorType: 2 (用户配置问题)
        
        # 执行插件逻辑
        print("##[info] 开始执行插件逻辑")
        result = execute_logic(input_param)
        
        # 输出结果
        write_output("outputParam", result)
        print("##[info] 插件执行完成")
        
        sys.exit(0)  # 成功退出
        
    except Exception as e:
        print(f"##[error] 插件执行失败: {str(e)}")
        traceback.print_exc()
        sys.exit(1)  # ErrorType: 1 (插件问题)

if __name__ == "__main__":
    main()
```

#### 1.3 插件调试

**本地调试环境**:
```bash
# 设置调试环境变量
export BK_CI_BUILD_ID="b-12345"
export BK_CI_PROJECT_ID="demo"
export BK_CI_PIPELINE_ID="p-67890"
export INPUT_INPUTPARAM="test-value"
export BK_CI_BUILD_OUTPUT="/tmp/build_output"

# 创建输出文件
touch $BK_CI_BUILD_OUTPUT

# 执行插件
java -jar my-plugin-1.0.jar

# 或执行 Python 插件
python3 my_plugin.py

# 查看输出结果
cat $BK_CI_BUILD_OUTPUT
```

**插件测试脚本**:
```bash
#!/bin/bash
# plugin-test.sh

set -e

echo "=== 插件测试开始 ==="

# 测试用例1: 正常参数
echo "测试用例1: 正常参数"
export INPUT_INPUTPARAM="normal-value"
java -jar my-plugin-1.0.jar
echo "测试用例1: 通过"

# 测试用例2: 空参数
echo "测试用例2: 空参数"
export INPUT_INPUTPARAM=""
if java -jar my-plugin-1.0.jar; then
    echo "测试用例2: 失败 - 应该返回错误"
    exit 1
else
    echo "测试用例2: 通过 - 正确返回错误"
fi

# 测试用例3: 特殊字符
echo "测试用例3: 特殊字符"
export INPUT_INPUTPARAM="test with spaces & symbols"
java -jar my-plugin-1.0.jar
echo "测试用例3: 通过"

echo "=== 插件测试完成 ==="
```

### 2. 插件性能优化

#### 2.1 执行效率优化

**资源使用优化**:
```java
// 优化内存使用
public class OptimizedPlugin {
    private static final int BUFFER_SIZE = 8192;
    
    public void processLargeFile(String filePath) {
        // 使用缓冲流减少 I/O 操作
        try (BufferedReader reader = new BufferedReader(
                new FileReader(filePath), BUFFER_SIZE)) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件处理失败", e);
        }
    }
    
    // 使用对象池减少 GC 压力
    private final ObjectPool<StringBuilder> stringBuilderPool = 
        new GenericObjectPool<>(new StringBuilderFactory());
    
    public String processText(String input) {
        StringBuilder sb = null;
        try {
            sb = stringBuilderPool.borrowObject();
            sb.setLength(0);  // 重置
            
            // 处理文本
            sb.append("processed: ").append(input);
            
            return sb.toString();
        } finally {
            if (sb != null) {
                stringBuilderPool.returnObject(sb);
            }
        }
    }
}
```

**并发处理优化**:
```java
// 并发处理大量数据
public class ConcurrentPlugin {
    private final ExecutorService executor = 
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    public void processBatch(List<String> items) {
        List<Future<String>> futures = new ArrayList<>();
        
        // 提交并发任务
        for (String item : items) {
            futures.add(executor.submit(() -> processItem(item)));
        }
        
        // 收集结果
        List<String> results = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                System.out.println("##[warning] 处理项目失败: " + e.getMessage());
            }
        }
        
        // 输出结果
        writeOutput("processedCount", String.valueOf(results.size()));
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

#### 2.2 网络请求优化

**连接池和重试机制**:
```java
// HTTP 客户端优化
public class OptimizedHttpClient {
    private final CloseableHttpClient httpClient;
    
    public OptimizedHttpClient() {
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        
        // 配置重试策略
        HttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(3, true);
        
        // 配置超时
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(30000)
            .setSocketTimeout(60000)
            .setConnectionRequestTimeout(10000)
            .build();
        
        this.httpClient = HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setRetryHandler(retryHandler)
            .setDefaultRequestConfig(requestConfig)
            .build();
    }
    
    public String get(String url) throws IOException {
        HttpGet request = new HttpGet(url);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                return EntityUtils.toString(response.getEntity());
            } else {
                throw new IOException("HTTP请求失败: " + response.getStatusLine());
            }
        }
    }
    
    public void close() throws IOException {
        httpClient.close();
    }
}
```

## 📊 插件监控和诊断

### 1. 插件执行监控

**性能指标收集**:
```java
// 插件性能监控
public class PluginMetrics {
    private final long startTime;
    private long memoryUsed;
    
    public PluginMetrics() {
        this.startTime = System.currentTimeMillis();
        this.memoryUsed = getUsedMemory();
    }
    
    public void recordMetrics() {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        long finalMemory = getUsedMemory();
        long memoryDelta = finalMemory - memoryUsed;
        
        // 输出性能指标
        System.out.println("##[metric] execution_time=" + duration + "ms");
        System.out.println("##[metric] memory_used=" + (memoryDelta / 1024 / 1024) + "MB");
        System.out.println("##[metric] peak_memory=" + (finalMemory / 1024 / 1024) + "MB");
        
        // 输出到监控系统
        sendToMonitoring("plugin.execution.time", duration);
        sendToMonitoring("plugin.memory.used", memoryDelta);
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private void sendToMonitoring(String metric, long value) {
        // 发送到监控系统 (如 Prometheus)
        // 实现具体的监控集成逻辑
    }
}
```

### 2. 错误诊断

**详细错误信息**:
```java
// 错误诊断和上报
public class PluginDiagnostics {
    
    public static void handleError(Exception e, String context) {
        // 收集诊断信息
        Map<String, Object> diagnostics = new HashMap<>();
        diagnostics.put("error_type", e.getClass().getSimpleName());
        diagnostics.put("error_message", e.getMessage());
        diagnostics.put("context", context);
        diagnostics.put("timestamp", System.currentTimeMillis());
        diagnostics.put("java_version", System.getProperty("java.version"));
        diagnostics.put("os_name", System.getProperty("os.name"));
        diagnostics.put("available_memory", Runtime.getRuntime().freeMemory());
        
        // 输出诊断信息
        System.out.println("##[error] 插件执行失败");
        System.out.println("##[diagnostic] " + toJson(diagnostics));
        
        // 输出堆栈跟踪
        System.out.println("##[stacktrace] " + getStackTrace(e));
        
        // 根据错误类型设置退出码
        int exitCode = determineExitCode(e);
        System.exit(exitCode);
    }
    
    private static int determineExitCode(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return 2;  // 用户配置问题
        } else if (e instanceof IOException) {
            return 3;  // 环境依赖问题
        } else {
            return 1;  // 插件问题
        }
    }
    
    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
    
    private static String toJson(Map<String, Object> data) {
        // 简单的 JSON 序列化
        // 实际项目中建议使用 Jackson 或 Gson
        return data.toString();
    }
}
```

## 📞 插件问题支持

### 1. 问题分类和联系

**官方插件支持**:
- **识别标识**: 插件作者显示为 "DevOps平台组"
- **支持渠道**: BK-CI 官方技术群、工单系统
- **响应时间**: 工作日 4 小时内响应
- **支持范围**: 功能问题、Bug 修复、使用指导

**第三方插件支持**:
- **识别方式**: 查看插件详情页的作者信息
- **联系方式**: 插件作者提供的联系方式
- **支持方式**: GitHub Issues、邮件、社区论坛
- **响应时间**: 依赖插件作者的支持政策

### 2. 问题上报模板

```markdown
## 插件问题报告

### 插件信息
- **插件名称**: Git拉取
- **插件版本**: v1.2.3
- **插件作者**: DevOps平台组
- **插件ID**: checkout@4

### 环境信息
- **BK-CI版本**: v1.5.0
- **构建机类型**: Docker
- **操作系统**: Ubuntu 20.04
- **Java版本**: OpenJDK 1.8.0_292

### 问题描述
简要描述插件执行过程中遇到的问题

### 插件配置
```yaml
- name: "Git拉取"
  uses: "checkout@4"
  with:
    repository: "https://github.com/user/repo.git"
    branch: "master"
    timeout: 300
```

### 错误信息
```
粘贴完整的插件执行日志和错误信息
```

### 复现步骤
1. 配置插件参数
2. 执行流水线
3. 观察错误现象

### 期望结果
描述插件应该正常执行的预期行为

### 实际结果
描述实际发生的异常行为

### 影响评估
- 影响范围：XX个项目
- 业务影响：描述对业务的具体影响
- 紧急程度：高/中/低
```

---

## 📚 相关文档

- [流水线问题排查指南](./01-pipeline-troubleshooting.md)
- [环境问题排查指南](./05-environment-troubleshooting.md)
- [插件开发指南](../../47-pipeline-plugin-development/)
- [BK-CI 用户使用指南](../../49-bkci-user-guide/)

---

*最后更新时间：2025-01-09*
*文档版本：v2.0*