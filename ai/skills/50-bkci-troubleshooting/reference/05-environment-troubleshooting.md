# 环境问题排查详细指南

## 📋 概述

构建环境是流水线执行的基础，本文档提供环境相关问题的详细排查方法和解决方案，涵盖构建机管理、网络配置、依赖安装等各种场景。

## 🔍 环境问题分类

### 1. 构建机问题

#### 1.1 构建机离线

**问题现象**:
- 流水线排队但不执行
- 构建机列表显示离线状态
- 心跳检测失败

**排查步骤**:

**Step 1: 检查构建机状态**
```bash
# 进入环境管理查看构建机状态
1. 环境管理 -> 构建机列表
2. 查看构建机在线状态
3. 检查最后心跳时间
4. 确认构建机资源使用情况

# 构建机状态说明
- 正常: 绿色，可以接受任务
- 异常: 红色，无法接受任务  
- 构建中: 黄色，正在执行任务
- 离线: 灰色，失去连接
```

**Step 2: 网络连通性测试**
```bash
# Docker 构建机网络测试
docker exec bkci-agent ping -c 3 gateway.devops.com
docker exec bkci-agent curl -I https://api.devops.com/health

# 物理机网络测试
ping -c 3 gateway.devops.com
curl -I https://api.devops.com/health
telnet gateway.devops.com 80

# 检查 DNS 解析
nslookup gateway.devops.com
dig gateway.devops.com
```

**Step 3: 构建机服务检查**
```bash
# Docker 构建机
docker ps | grep bkci-agent
docker logs bkci-agent --tail 100

# 物理机构建机
systemctl status bkci-agent
journalctl -u bkci-agent -f --lines 100

# 检查配置文件
cat /data/bkci/agent/.agent.properties
```

**解决方案**:
```bash
# 重启构建机服务
# Docker 方式
docker restart bkci-agent

# 物理机方式
systemctl restart bkci-agent

# 重新安装构建机
1. 下载最新 Agent 安装包
2. 停止现有服务
3. 重新安装并配置
4. 启动服务并验证连接
```

#### 1.2 资源不足

**问题现象**:
- 构建过程中内存溢出
- 磁盘空间不足错误
- CPU 使用率过高导致超时

**资源监控和诊断**:
```bash
# 系统资源检查脚本
#!/bin/bash
echo "=== 系统资源状态 ==="

echo "CPU 使用情况:"
top -bn1 | grep "Cpu(s)" | awk '{print $2 $4}'
lscpu | grep -E "CPU\(s\)|Model name"

echo "内存使用情况:"
free -h
cat /proc/meminfo | grep -E "MemTotal|MemFree|MemAvailable"

echo "磁盘使用情况:"
df -h
du -sh /tmp /var/log /data/bkci

echo "网络连接状态:"
netstat -tuln | grep LISTEN | head -10

echo "进程资源占用 TOP 10:"
ps aux --sort=-%cpu | head -11
ps aux --sort=-%mem | head -11

echo "系统负载:"
uptime
cat /proc/loadavg
```

**资源优化配置**:
```yaml
# 构建机资源配置
agent:
  resources:
    cpu: "4"           # CPU 核数
    memory: "8Gi"      # 内存大小
    disk: "100Gi"      # 磁盘空间
  limits:
    cpu: "6"           # CPU 限制
    memory: "12Gi"     # 内存限制
    
# JVM 参数优化
environment:
  JAVA_OPTS: "-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
  MAVEN_OPTS: "-Xmx2g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
```

**磁盘清理脚本**:
```bash
#!/bin/bash
# 构建机磁盘清理脚本

echo "=== 开始磁盘清理 ==="

# 清理 Docker 资源
echo "清理 Docker 资源..."
docker system prune -f
docker volume prune -f
docker image prune -a -f

# 清理构建缓存
echo "清理构建缓存..."
rm -rf ~/.m2/repository/.cache
rm -rf ~/.gradle/caches/modules-2/files-2.1/*/
rm -rf ~/.npm/_cacache/*
rm -rf /tmp/npm-*

# 清理日志文件
echo "清理日志文件..."
find /var/log -name "*.log" -mtime +7 -delete
find /data/bkci/logs -name "*.log" -mtime +3 -delete

# 清理临时文件
echo "清理临时文件..."
rm -rf /tmp/*
rm -rf /var/tmp/*

echo "=== 磁盘清理完成 ==="
df -h
```

#### 1.3 构建机配置问题

**配置文件检查**:
```bash
# Agent 配置文件位置
/data/bkci/agent/.agent.properties

# 关键配置项检查
cat .agent.properties | grep -E "(devops.gateway|devops.project.id|devops.agent.id)"

# 配置文件示例
devops.gateway=https://gateway.devops.com
devops.project.id=demo
devops.agent.id=agent-12345
devops.agent.secret.key=xxxxx
devops.parallel.task.count=4
```

**网络配置问题**:
```bash
# 代理配置
export http_proxy=http://proxy.company.com:8080
export https_proxy=http://proxy.company.com:8080
export no_proxy=localhost,127.0.0.1,.company.com

# 防火墙配置检查
iptables -L
ufw status
firewall-cmd --list-all

# 端口连通性测试
telnet gateway.devops.com 80
telnet gateway.devops.com 443
nc -zv gateway.devops.com 80
```

### 2. Docker 环境问题

#### 2.1 Docker 服务问题

**Docker 服务状态检查**:
```bash
# 检查 Docker 服务状态
systemctl status docker
docker version
docker info

# 检查 Docker 守护进程日志
journalctl -u docker.service -f

# 检查 Docker 存储驱动
docker info | grep -E "(Storage Driver|Docker Root Dir)"
```

**Docker 服务配置**:
```json
// /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com",
    "https://registry.docker-cn.com"
  ],
  "insecure-registries": [
    "registry.company.com"
  ],
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "100m",
    "max-file": "3"
  },
  "storage-driver": "overlay2",
  "data-root": "/data/docker"
}
```

#### 2.2 镜像问题

**镜像拉取失败**:
```bash
# 问题现象
Error response from daemon: pull access denied for image
Error response from daemon: Get https://registry-1.docker.io/v2/: net/http: TLS handshake timeout

# 解决方案
# 1. 配置镜像加速器
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://mirror.ccs.tencentyun.com"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker

# 2. 使用内网镜像仓库
docker pull registry.company.com/library/ubuntu:20.04
docker tag registry.company.com/library/ubuntu:20.04 ubuntu:20.04

# 3. 手动下载镜像
docker save ubuntu:20.04 | gzip > ubuntu-20.04.tar.gz
# 传输到构建机后加载
docker load < ubuntu-20.04.tar.gz
```

**镜像构建问题**:
```dockerfile
# 优化 Dockerfile
FROM ubuntu:20.04

# 设置非交互模式
ENV DEBIAN_FRONTEND=noninteractive

# 使用国内源
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        wget \
        git \
        build-essential && \
    rm -rf /var/lib/apt/lists/*

# 设置工作目录
WORKDIR /workspace

# 复制文件时使用 .dockerignore
COPY . .

# 多阶段构建减少镜像大小
FROM ubuntu:20.04 AS builder
RUN apt-get update && apt-get install -y build-essential
COPY . /src
RUN cd /src && make build

FROM ubuntu:20.04
COPY --from=builder /src/dist/app /usr/local/bin/
CMD ["/usr/local/bin/app"]
```

#### 2.3 容器运行问题

**容器资源限制**:
```yaml
# 容器资源配置
- name: "Docker构建"
  uses: "docker-build@1.*"
  with:
    dockerfile: "Dockerfile"
    resources:
      requests:
        memory: "2Gi"
        cpu: "1"
      limits:
        memory: "4Gi"
        cpu: "2"
```

**容器网络问题**:
```bash
# 检查容器网络
docker network ls
docker inspect bridge

# 测试容器网络连通性
docker run --rm alpine ping -c 3 8.8.8.8
docker run --rm alpine nslookup google.com

# 自定义网络配置
docker network create --driver bridge custom-network
docker run --network custom-network alpine
```

### 3. 依赖环境问题

#### 3.1 编程语言环境

**Java 环境配置**:
```bash
# Java 环境检查和配置
echo "=== Java 环境检查 ==="

# 检查已安装的 Java 版本
java -version
javac -version
echo "JAVA_HOME: ${JAVA_HOME:-未设置}"

# 安装 OpenJDK 8
sudo apt-get update
sudo apt-get install -y openjdk-8-jdk

# 设置 JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$PATH:$JAVA_HOME/bin' >> ~/.bashrc

# 验证安装
java -version
which java
```

**Node.js 环境配置**:
```bash
# Node.js 环境管理
echo "=== Node.js 环境配置 ==="

# 使用 nvm 管理 Node.js 版本
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash
source ~/.bashrc

# 安装和使用指定版本
nvm install 16.20.0
nvm use 16.20.0
nvm alias default 16.20.0

# 配置 npm 镜像源
npm config set registry https://registry.npm.taobao.org
npm config set disturl https://npm.taobao.org/dist

# 验证安装
node --version
npm --version
```

**Python 环境配置**:
```bash
# Python 环境管理
echo "=== Python 环境配置 ==="

# 安装 Python 和 pip
sudo apt-get install -y python3 python3-pip python3-venv

# 创建虚拟环境
python3 -m venv /opt/python-env
source /opt/python-env/bin/activate

# 配置 pip 镜像源
mkdir -p ~/.pip
cat > ~/.pip/pip.conf << EOF
[global]
index-url = https://pypi.tuna.tsinghua.edu.cn/simple
trusted-host = pypi.tuna.tsinghua.edu.cn
EOF

# 安装常用包
pip install --upgrade pip
pip install requests pytest coverage

# 验证安装
python3 --version
pip --version
```

#### 3.2 构建工具环境

**Maven 环境配置**:
```bash
# Maven 安装和配置
echo "=== Maven 环境配置 ==="

# 下载和安装 Maven
MAVEN_VERSION=3.8.6
wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz
tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt
ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven

# 设置环境变量
export MAVEN_HOME=/opt/maven
export PATH=$PATH:$MAVEN_HOME/bin
echo 'export MAVEN_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$PATH:$MAVEN_HOME/bin' >> ~/.bashrc

# 配置 Maven settings.xml
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <mirrorOf>central</mirrorOf>
      <name>Aliyun Maven Mirror</name>
      <url>https://maven.aliyun.com/repository/central</url>
    </mirror>
  </mirrors>
  
  <profiles>
    <profile>
      <id>jdk-1.8</id>
      <activation>
        <activeByDefault>true</activeByDefault>
        <jdk>1.8</jdk>
      </activation>
      <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.compilerVersion>1.8</maven.compiler.compilerVersion>
      </properties>
    </profile>
  </profiles>
</settings>
EOF

# 验证安装
mvn -version
```

**Gradle 环境配置**:
```bash
# Gradle 安装和配置
echo "=== Gradle 环境配置 ==="

# 下载和安装 Gradle
GRADLE_VERSION=7.6
wget https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip
unzip gradle-${GRADLE_VERSION}-bin.zip -d /opt
ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle

# 设置环境变量
export GRADLE_HOME=/opt/gradle
export PATH=$PATH:$GRADLE_HOME/bin

# 配置 Gradle 镜像源
mkdir -p ~/.gradle
cat > ~/.gradle/init.gradle << EOF
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/central' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
    }
}
EOF

# 验证安装
gradle --version
```

### 4. 网络环境问题

#### 4.1 网络连通性

**网络诊断脚本**:
```bash
#!/bin/bash
# 网络连通性诊断脚本

echo "=== 网络连通性诊断 ==="

# 基础网络测试
echo "1. 基础网络测试"
ping -c 3 8.8.8.8
ping -c 3 114.114.114.114

# DNS 解析测试
echo "2. DNS 解析测试"
nslookup google.com
dig @8.8.8.8 google.com

# 平台连接测试
echo "3. 平台连接测试"
curl -I --connect-timeout 10 https://gateway.devops.com
curl -I --connect-timeout 10 https://api.devops.com

# 代码库连接测试
echo "4. 代码库连接测试"
curl -I --connect-timeout 10 https://github.com
curl -I --connect-timeout 10 https://gitlab.com

# 制品库连接测试
echo "5. 制品库连接测试"
curl -I --connect-timeout 10 https://registry-1.docker.io
curl -I --connect-timeout 10 https://repo1.maven.org

# 端口连通性测试
echo "6. 端口连通性测试"
nc -zv gateway.devops.com 80
nc -zv gateway.devops.com 443
nc -zv github.com 22
```

#### 4.2 代理配置

**HTTP 代理配置**:
```bash
# 系统级代理配置
export http_proxy=http://proxy.company.com:8080
export https_proxy=http://proxy.company.com:8080
export no_proxy=localhost,127.0.0.1,.company.com,10.0.0.0/8

# 持久化代理配置
cat >> ~/.bashrc << EOF
export http_proxy=http://proxy.company.com:8080
export https_proxy=http://proxy.company.com:8080
export no_proxy=localhost,127.0.0.1,.company.com,10.0.0.0/8
EOF

# Git 代理配置
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy http://proxy.company.com:8080

# NPM 代理配置
npm config set proxy http://proxy.company.com:8080
npm config set https-proxy http://proxy.company.com:8080

# Maven 代理配置 (在 settings.xml 中)
<proxies>
  <proxy>
    <id>company-proxy</id>
    <active>true</active>
    <protocol>http</protocol>
    <host>proxy.company.com</host>
    <port>8080</port>
  </proxy>
</proxies>
```

#### 4.3 防火墙配置

**防火墙规则检查**:
```bash
# Ubuntu/Debian 防火墙
ufw status verbose
ufw allow 80/tcp
ufw allow 443/tcp
ufw allow 22/tcp

# CentOS/RHEL 防火墙
firewall-cmd --list-all
firewall-cmd --permanent --add-port=80/tcp
firewall-cmd --permanent --add-port=443/tcp
firewall-cmd --reload

# iptables 规则
iptables -L -n
iptables -A INPUT -p tcp --dport 80 -j ACCEPT
iptables -A INPUT -p tcp --dport 443 -j ACCEPT
```

### 5. 环境标准化

#### 5.1 构建环境镜像

**标准化 Dockerfile**:
```dockerfile
# BK-CI 标准构建环境
FROM ubuntu:20.04

# 设置环境变量
ENV DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Shanghai

# 安装基础工具
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    build-essential \
    ca-certificates \
    gnupg \
    lsb-release \
    && rm -rf /var/lib/apt/lists/*

# 安装 Java 8
RUN apt-get update && apt-get install -y openjdk-8-jdk
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$PATH:$JAVA_HOME/bin

# 安装 Maven
ARG MAVEN_VERSION=3.8.6
RUN wget https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && tar -xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && rm apache-maven-${MAVEN_VERSION}-bin.tar.gz
ENV MAVEN_HOME=/opt/maven
ENV PATH=$PATH:$MAVEN_HOME/bin

# 安装 Node.js
RUN curl -fsSL https://deb.nodesource.com/setup_16.x | bash - \
    && apt-get install -y nodejs

# 安装 Docker
RUN curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null \
    && apt-get update \
    && apt-get install -y docker-ce-cli

# 配置镜像源
COPY maven-settings.xml /root/.m2/settings.xml
COPY npm-config /root/.npmrc

# 设置工作目录
WORKDIR /workspace

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# 验证安装
RUN java -version && \
    mvn -version && \
    node --version && \
    npm --version && \
    docker --version
```

#### 5.2 环境初始化脚本

**环境准备脚本**:
```bash
#!/bin/bash
# 构建环境初始化脚本

set -e

echo "=== BK-CI 构建环境初始化 ==="

# 检查系统信息
echo "系统信息:"
uname -a
cat /etc/os-release

# 更新系统包
echo "更新系统包..."
apt-get update

# 安装基础工具
echo "安装基础工具..."
apt-get install -y \
    curl \
    wget \
    git \
    unzip \
    build-essential \
    ca-certificates

# 配置时区
echo "配置时区..."
ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime
echo "Asia/Shanghai" > /etc/timezone

# 配置 Git
echo "配置 Git..."
git config --global user.name "BK-CI Agent"
git config --global user.email "agent@bkci.tencent.com"
git config --global core.autocrlf false
git config --global core.filemode false

# 创建工作目录
echo "创建工作目录..."
mkdir -p /workspace
mkdir -p /data/cache
mkdir -p /data/logs

# 设置权限
echo "设置权限..."
chmod 755 /workspace
chmod 755 /data/cache
chmod 755 /data/logs

# 清理临时文件
echo "清理临时文件..."
apt-get clean
rm -rf /var/lib/apt/lists/*
rm -rf /tmp/*

echo "=== 环境初始化完成 ==="

# 输出环境信息
echo "=== 环境信息 ==="
echo "Java: $(java -version 2>&1 | head -1)"
echo "Maven: $(mvn -version | head -1)"
echo "Node.js: $(node --version)"
echo "NPM: $(npm --version)"
echo "Git: $(git --version)"
echo "Docker: $(docker --version)"
```

## 📊 环境监控和维护

### 1. 监控指标

**关键监控指标**:
```bash
# 系统资源监控
cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
memory_usage=$(free | grep Mem | awk '{printf "%.2f", $3/$2 * 100.0}')
disk_usage=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')

echo "CPU使用率: ${cpu_usage}%"
echo "内存使用率: ${memory_usage}%"
echo "磁盘使用率: ${disk_usage}%"

# 构建机连接状态
agent_status=$(curl -s http://localhost:8080/api/agent/status | jq -r '.status')
echo "Agent状态: $agent_status"

# 网络延迟监控
ping_latency=$(ping -c 3 gateway.devops.com | tail -1 | awk '{print $4}' | cut -d '/' -f 2)
echo "网络延迟: ${ping_latency}ms"
```

### 2. 自动化维护

**定期维护脚本**:
```bash
#!/bin/bash
# 构建机定期维护脚本

echo "=== 开始定期维护 ==="

# 清理 Docker 资源
echo "清理 Docker 资源..."
docker system prune -f
docker volume prune -f

# 清理构建缓存
echo "清理构建缓存..."
find ~/.m2/repository -name "*.lastUpdated" -delete
find ~/.gradle/caches -name "*.lock" -delete
npm cache clean --force

# 清理日志文件
echo "清理日志文件..."
find /var/log -name "*.log" -mtime +7 -delete
find /data/bkci/logs -name "*.log" -mtime +3 -delete

# 更新系统包
echo "更新系统包..."
apt-get update
apt-get upgrade -y

# 重启服务
echo "重启 Agent 服务..."
systemctl restart bkci-agent

echo "=== 定期维护完成 ==="
```

---

## 📚 相关文档

- [流水线问题排查指南](./01-pipeline-troubleshooting.md)
- [插件问题排查指南](./02-plugin-troubleshooting.md)
- [网络问题排查指南](./08-network-troubleshooting.md)
- [BK-CI 用户使用指南](../../49-bkci-user-guide/)

---

*最后更新时间：2025-01-09*
*文档版本：v2.0*