# 网络问题排查详细指南

## 📋 概述

网络连接是 BK-CI 平台正常运行的基础，本文档提供网络相关问题的详细排查方法和解决方案，涵盖连接超时、代理配置、防火墙限制等各种场景。

## 🔍 网络问题分类

### 1. 连接超时问题

#### 1.1 平台 API 连接超时

**问题现象**:
- 构建机无法连接到 BK-CI 平台
- API 请求超时
- 心跳检测失败

**网络诊断脚本**:
```bash
#!/bin/bash
# BK-CI 网络连通性诊断脚本

echo "=== BK-CI 网络诊断开始 ==="

# 基础网络测试
echo "1. 基础网络连通性测试"
ping -c 3 8.8.8.8 && echo "✓ 外网连通正常" || echo "✗ 外网连通失败"
ping -c 3 114.114.114.114 && echo "✓ 国内 DNS 正常" || echo "✗ 国内 DNS 失败"

# DNS 解析测试
echo "2. DNS 解析测试"
nslookup gateway.devops.com && echo "✓ 平台域名解析正常" || echo "✗ 平台域名解析失败"
dig +short gateway.devops.com

# 平台服务连接测试
echo "3. 平台服务连接测试"
GATEWAY_URL="https://gateway.devops.com"
API_URL="https://api.devops.com"

curl -I --connect-timeout 10 --max-time 30 $GATEWAY_URL && echo "✓ Gateway 连接正常" || echo "✗ Gateway 连接失败"
curl -I --connect-timeout 10 --max-time 30 $API_URL && echo "✓ API 连接正常" || echo "✗ API 连接失败"

# 端口连通性测试
echo "4. 端口连通性测试"
nc -zv gateway.devops.com 80 2>&1 | grep -q "succeeded" && echo "✓ HTTP 端口 (80) 正常" || echo "✗ HTTP 端口 (80) 失败"
nc -zv gateway.devops.com 443 2>&1 | grep -q "succeeded" && echo "✓ HTTPS 端口 (443) 正常" || echo "✗ HTTPS 端口 (443) 失败"

# 网络延迟测试
echo "5. 网络延迟测试"
PING_RESULT=$(ping -c 5 gateway.devops.com | tail -1 | awk '{print $4}' | cut -d '/' -f 2)
echo "平均延迟: ${PING_RESULT}ms"

if (( $(echo "$PING_RESULT > 100" | bc -l) )); then
    echo "⚠ 网络延迟较高，可能影响性能"
else
    echo "✓ 网络延迟正常"
fi

echo "=== BK-CI 网络诊断完成 ==="
```

#### 1.2 代码库连接超时

**Git 操作超时**:
```bash
# Git 连接测试脚本
#!/bin/bash

echo "=== Git 连接测试 ==="

# GitHub 连接测试
echo "1. GitHub 连接测试"
curl -I --connect-timeout 10 https://github.com && echo "✓ GitHub HTTPS 正常" || echo "✗ GitHub HTTPS 失败"
ssh -T -o ConnectTimeout=10 git@github.com 2>&1 | grep -q "successfully authenticated" && echo "✓ GitHub SSH 正常" || echo "✗ GitHub SSH 失败"

# GitLab 连接测试
echo "2. GitLab 连接测试"
curl -I --connect-timeout 10 https://gitlab.com && echo "✓ GitLab HTTPS 正常" || echo "✗ GitLab HTTPS 失败"
ssh -T -o ConnectTimeout=10 git@gitlab.com 2>&1 | grep -q "Welcome to GitLab" && echo "✓ GitLab SSH 正常" || echo "✗ GitLab SSH 失败"

# 内网 Git 服务器测试
INTERNAL_GIT="git.company.com"
echo "3. 内网 Git 服务器测试"
curl -I --connect-timeout 10 https://$INTERNAL_GIT && echo "✓ 内网 Git HTTPS 正常" || echo "✗ 内网 Git HTTPS 失败"
ssh -T -o ConnectTimeout=10 git@$INTERNAL_GIT 2>&1 && echo "✓ 内网 Git SSH 正常" || echo "✗ 内网 Git SSH 失败"

# Git 配置检查
echo "4. Git 配置检查"
git config --global --list | grep -E "(http|https|proxy)"
```

**Git 超时配置优化**:
```bash
# Git 全局超时配置
git config --global http.lowSpeedLimit 1000
git config --global http.lowSpeedTime 300
git config --global http.postBuffer 524288000

# 针对特定域名的配置
git config --global http.https://github.com.proxy http://proxy.company.com:8080
git config --global http.https://github.com.sslVerify false

# SSH 连接配置优化
cat >> ~/.ssh/config << EOF
Host github.com
    Hostname github.com
    Port 22
    User git
    ServerAliveInterval 60
    ServerAliveCountMax 3
    ConnectTimeout 10

Host *.company.com
    User git
    ServerAliveInterval 60
    ServerAliveCountMax 3
    ConnectTimeout 10
    ProxyCommand nc -X connect -x proxy.company.com:1080 %h %p
EOF
```

### 2. 代理配置问题

#### 2.1 HTTP/HTTPS 代理

**代理配置检查**:
```bash
# 系统代理环境变量检查
echo "=== 代理配置检查 ==="

echo "当前代理配置:"
echo "http_proxy: ${http_proxy:-未设置}"
echo "https_proxy: ${https_proxy:-未设置}"
echo "no_proxy: ${no_proxy:-未设置}"
echo "HTTP_PROXY: ${HTTP_PROXY:-未设置}"
echo "HTTPS_PROXY: ${HTTPS_PROXY:-未设置}"
echo "NO_PROXY: ${NO_PROXY:-未设置}"

# 测试代理连通性
if [ -n "$http_proxy" ]; then
    echo "测试 HTTP 代理连通性:"
    curl -I --proxy $http_proxy --connect-timeout 10 http://www.baidu.com
fi

if [ -n "$https_proxy" ]; then
    echo "测试 HTTPS 代理连通性:"
    curl -I --proxy $https_proxy --connect-timeout 10 https://www.baidu.com
fi
```

**代理配置脚本**:
```bash
#!/bin/bash
# 代理配置脚本

PROXY_HOST="proxy.company.com"
PROXY_PORT="8080"
NO_PROXY_LIST="localhost,127.0.0.1,.company.com,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16"

echo "=== 配置系统代理 ==="

# 设置环境变量
export http_proxy=http://$PROXY_HOST:$PROXY_PORT
export https_proxy=http://$PROXY_HOST:$PROXY_PORT
export no_proxy=$NO_PROXY_LIST
export HTTP_PROXY=$http_proxy
export HTTPS_PROXY=$https_proxy
export NO_PROXY=$no_proxy

# 持久化到 shell 配置文件
cat >> ~/.bashrc << EOF
# 代理配置
export http_proxy=http://$PROXY_HOST:$PROXY_PORT
export https_proxy=http://$PROXY_HOST:$PROXY_PORT
export no_proxy=$NO_PROXY_LIST
export HTTP_PROXY=\$http_proxy
export HTTPS_PROXY=\$https_proxy
export NO_PROXY=\$no_proxy
EOF

echo "✓ 系统代理配置完成"

# 配置 Git 代理
echo "=== 配置 Git 代理 ==="
git config --global http.proxy http://$PROXY_HOST:$PROXY_PORT
git config --global https.proxy http://$PROXY_HOST:$PROXY_PORT
echo "✓ Git 代理配置完成"

# 配置 NPM 代理
echo "=== 配置 NPM 代理 ==="
npm config set proxy http://$PROXY_HOST:$PROXY_PORT
npm config set https-proxy http://$PROXY_HOST:$PROXY_PORT
npm config set registry https://registry.npm.taobao.org
echo "✓ NPM 代理配置完成"

# 配置 Maven 代理
echo "=== 配置 Maven 代理 ==="
mkdir -p ~/.m2
cat > ~/.m2/settings.xml << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings>
  <proxies>
    <proxy>
      <id>company-proxy</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>$PROXY_HOST</host>
      <port>$PROXY_PORT</port>
      <nonProxyHosts>$NO_PROXY_LIST</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
EOF
echo "✓ Maven 代理配置完成"

# 配置 Docker 代理
echo "=== 配置 Docker 代理 ==="
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf << EOF
[Service]
Environment="HTTP_PROXY=http://$PROXY_HOST:$PROXY_PORT"
Environment="HTTPS_PROXY=http://$PROXY_HOST:$PROXY_PORT"
Environment="NO_PROXY=$NO_PROXY_LIST"
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
echo "✓ Docker 代理配置完成"

echo "=== 代理配置全部完成 ==="
```

#### 2.2 SOCKS 代理

**SOCKS 代理配置**:
```bash
# SOCKS 代理配置
SOCKS_PROXY="socks5://proxy.company.com:1080"

# 配置 curl 使用 SOCKS 代理
curl --socks5-hostname proxy.company.com:1080 https://www.google.com

# 配置 SSH 使用 SOCKS 代理
cat >> ~/.ssh/config << EOF
Host github.com
    ProxyCommand nc -X 5 -x proxy.company.com:1080 %h %p

Host *.external.com
    ProxyCommand nc -X 5 -x proxy.company.com:1080 %h %p
EOF

# 配置 Git 使用 SOCKS 代理
git config --global http.proxy $SOCKS_PROXY
git config --global https.proxy $SOCKS_PROXY
```

### 3. 防火墙和安全组问题

#### 3.1 防火墙规则检查

**Linux 防火墙检查**:
```bash
#!/bin/bash
# 防火墙规则检查脚本

echo "=== 防火墙状态检查 ==="

# 检查 ufw 状态 (Ubuntu/Debian)
if command -v ufw &> /dev/null; then
    echo "UFW 防火墙状态:"
    sudo ufw status verbose
    
    echo "UFW 规则列表:"
    sudo ufw --dry-run status numbered
fi

# 检查 firewalld 状态 (CentOS/RHEL)
if command -v firewall-cmd &> /dev/null; then
    echo "Firewalld 防火墙状态:"
    sudo firewall-cmd --state
    
    echo "活动区域:"
    sudo firewall-cmd --get-active-zones
    
    echo "默认区域规则:"
    sudo firewall-cmd --list-all
    
    echo "所有区域规则:"
    sudo firewall-cmd --list-all-zones
fi

# 检查 iptables 规则
echo "iptables 规则:"
sudo iptables -L -n --line-numbers

echo "iptables NAT 规则:"
sudo iptables -t nat -L -n --line-numbers

# 检查监听端口
echo "监听端口:"
netstat -tuln | grep LISTEN
```

**防火墙规则配置**:
```bash
#!/bin/bash
# BK-CI 防火墙规则配置

echo "=== 配置 BK-CI 防火墙规则 ==="

# UFW 配置 (Ubuntu/Debian)
if command -v ufw &> /dev/null; then
    echo "配置 UFW 规则..."
    
    # 允许 SSH
    sudo ufw allow 22/tcp
    
    # 允许 HTTP/HTTPS
    sudo ufw allow 80/tcp
    sudo ufw allow 443/tcp
    
    # 允许 BK-CI 相关端口
    sudo ufw allow 8080/tcp  # Agent 通信端口
    sudo ufw allow 8081/tcp  # 内部服务端口
    
    # 启用防火墙
    sudo ufw --force enable
    
    echo "✓ UFW 规则配置完成"
fi

# Firewalld 配置 (CentOS/RHEL)
if command -v firewall-cmd &> /dev/null; then
    echo "配置 Firewalld 规则..."
    
    # 允许 HTTP/HTTPS 服务
    sudo firewall-cmd --permanent --add-service=http
    sudo firewall-cmd --permanent --add-service=https
    
    # 允许 SSH 服务
    sudo firewall-cmd --permanent --add-service=ssh
    
    # 允许 BK-CI 相关端口
    sudo firewall-cmd --permanent --add-port=8080/tcp
    sudo firewall-cmd --permanent --add-port=8081/tcp
    
    # 重新加载规则
    sudo firewall-cmd --reload
    
    echo "✓ Firewalld 规则配置完成"
fi

# iptables 配置
echo "配置 iptables 规则..."

# 允许已建立的连接
sudo iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT

# 允许本地回环
sudo iptables -A INPUT -i lo -j ACCEPT

# 允许 SSH
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT

# 允许 HTTP/HTTPS
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 443 -j ACCEPT

# 允许 BK-CI 端口
sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8081 -j ACCEPT

# 保存 iptables 规则
if command -v iptables-save &> /dev/null; then
    sudo iptables-save > /etc/iptables/rules.v4
fi

echo "✓ iptables 规则配置完成"
```

#### 3.2 云服务商安全组

**AWS 安全组配置**:
```bash
# AWS 安全组规则配置
aws ec2 authorize-security-group-ingress \
    --group-id sg-12345678 \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-id sg-12345678 \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0

aws ec2 authorize-security-group-ingress \
    --group-id sg-12345678 \
    --protocol tcp \
    --port 8080 \
    --source-group sg-87654321  # 仅允许特定安全组访问
```

**腾讯云安全组配置**:
```bash
# 腾讯云安全组规则配置
tccli cvm CreateSecurityGroupPolicy \
    --GroupId sg-12345678 \
    --SecurityGroupPolicySet '{
        "Ingress": [
            {
                "Protocol": "TCP",
                "Port": "80",
                "CidrBlock": "0.0.0.0/0",
                "Action": "ACCEPT"
            },
            {
                "Protocol": "TCP", 
                "Port": "443",
                "CidrBlock": "0.0.0.0/0",
                "Action": "ACCEPT"
            }
        ]
    }'
```

### 4. DNS 解析问题

#### 4.1 DNS 配置检查

**DNS 诊断脚本**:
```bash
#!/bin/bash
# DNS 解析诊断脚本

echo "=== DNS 解析诊断 ==="

# 检查 DNS 配置
echo "1. DNS 配置检查"
cat /etc/resolv.conf

# 检查 hosts 文件
echo "2. Hosts 文件检查"
grep -E "(devops|bkci)" /etc/hosts

# 测试 DNS 解析
echo "3. DNS 解析测试"
DOMAINS=(
    "gateway.devops.com"
    "api.devops.com"
    "github.com"
    "gitlab.com"
    "registry-1.docker.io"
)

for domain in "${DOMAINS[@]}"; do
    echo "测试域名: $domain"
    
    # nslookup 测试
    nslookup $domain
    
    # dig 测试
    dig +short $domain
    
    # 测试解析时间
    time nslookup $domain > /dev/null 2>&1
    
    echo "---"
done

# 测试不同 DNS 服务器
echo "4. 不同 DNS 服务器测试"
DNS_SERVERS=(
    "8.8.8.8"
    "114.114.114.114"
    "223.5.5.5"
    "1.1.1.1"
)

for dns in "${DNS_SERVERS[@]}"; do
    echo "测试 DNS 服务器: $dns"
    dig @$dns gateway.devops.com +short
done
```

**DNS 配置优化**:
```bash
#!/bin/bash
# DNS 配置优化脚本

echo "=== DNS 配置优化 ==="

# 备份原配置
sudo cp /etc/resolv.conf /etc/resolv.conf.backup

# 配置多个 DNS 服务器
sudo tee /etc/resolv.conf << EOF
# 主 DNS 服务器
nameserver 114.114.114.114
nameserver 8.8.8.8

# 备用 DNS 服务器
nameserver 223.5.5.5
nameserver 1.1.1.1

# DNS 选项
options timeout:2
options attempts:3
options rotate
options single-request-reopen
EOF

# 配置 hosts 文件加速解析
sudo tee -a /etc/hosts << EOF

# BK-CI 相关域名
192.168.1.100 gateway.devops.com
192.168.1.101 api.devops.com

# 常用开发域名
140.82.112.3 github.com
172.65.251.78 gitlab.com
EOF

# 刷新 DNS 缓存
if command -v systemd-resolve &> /dev/null; then
    sudo systemd-resolve --flush-caches
elif command -v dscacheutil &> /dev/null; then
    sudo dscacheutil -flushcache
fi

echo "✓ DNS 配置优化完成"
```

### 5. SSL/TLS 证书问题

#### 5.1 证书验证问题

**SSL 证书检查**:
```bash
#!/bin/bash
# SSL 证书检查脚本

echo "=== SSL 证书检查 ==="

DOMAINS=(
    "gateway.devops.com"
    "api.devops.com"
    "github.com"
)

for domain in "${DOMAINS[@]}"; do
    echo "检查域名: $domain"
    
    # 获取证书信息
    echo | openssl s_client -servername $domain -connect $domain:443 2>/dev/null | \
    openssl x509 -noout -dates -subject -issuer
    
    # 检查证书有效期
    expiry_date=$(echo | openssl s_client -servername $domain -connect $domain:443 2>/dev/null | \
    openssl x509 -noout -enddate | cut -d= -f2)
    
    expiry_timestamp=$(date -d "$expiry_date" +%s)
    current_timestamp=$(date +%s)
    days_until_expiry=$(( (expiry_timestamp - current_timestamp) / 86400 ))
    
    echo "证书到期时间: $expiry_date"
    echo "距离到期: $days_until_expiry 天"
    
    if [ $days_until_expiry -lt 30 ]; then
        echo "⚠ 证书即将到期，请及时更新"
    else
        echo "✓ 证书有效期正常"
    fi
    
    echo "---"
done
```

**证书问题解决**:
```bash
# 跳过 SSL 验证 (仅用于测试)
export GIT_SSL_NO_VERIFY=true
git config --global http.sslVerify false

# 添加自签名证书到信任列表
sudo cp custom-ca.crt /usr/local/share/ca-certificates/
sudo update-ca-certificates

# 为特定域名配置证书
git config --global http.https://internal.company.com.sslCAInfo /path/to/ca-cert.pem
```

### 6. 网络性能优化

#### 6.1 带宽和延迟优化

**网络性能测试**:
```bash
#!/bin/bash
# 网络性能测试脚本

echo "=== 网络性能测试 ==="

# 带宽测试
echo "1. 带宽测试"
if command -v speedtest-cli &> /dev/null; then
    speedtest-cli --simple
else
    echo "请安装 speedtest-cli: pip install speedtest-cli"
fi

# 延迟测试
echo "2. 延迟测试"
HOSTS=(
    "gateway.devops.com"
    "github.com"
    "8.8.8.8"
)

for host in "${HOSTS[@]}"; do
    echo "测试主机: $host"
    ping -c 10 $host | tail -1
done

# TCP 连接测试
echo "3. TCP 连接测试"
time curl -I --connect-timeout 10 https://gateway.devops.com

# 下载速度测试
echo "4. 下载速度测试"
curl -o /dev/null -s -w "下载速度: %{speed_download} bytes/sec\n连接时间: %{time_connect}s\n总时间: %{time_total}s\n" \
    https://github.com/git/git/archive/master.zip
```

**网络参数调优**:
```bash
#!/bin/bash
# 网络参数调优脚本

echo "=== 网络参数调优 ==="

# TCP 参数优化
sudo tee -a /etc/sysctl.conf << EOF

# TCP 优化参数
net.core.rmem_default = 262144
net.core.rmem_max = 16777216
net.core.wmem_default = 262144
net.core.wmem_max = 16777216
net.ipv4.tcp_rmem = 4096 65536 16777216
net.ipv4.tcp_wmem = 4096 65536 16777216
net.ipv4.tcp_congestion_control = bbr
net.core.netdev_max_backlog = 5000
net.ipv4.tcp_window_scaling = 1
EOF

# 应用配置
sudo sysctl -p

echo "✓ 网络参数调优完成"

# Git 性能优化
git config --global core.preloadindex true
git config --global core.fscache true
git config --global gc.auto 256
git config --global http.postBuffer 524288000

echo "✓ Git 性能优化完成"
```

### 7. 网络监控和告警

#### 7.1 网络监控脚本

**持续网络监控**:
```bash
#!/bin/bash
# 网络监控脚本

MONITOR_HOSTS=(
    "gateway.devops.com"
    "api.devops.com"
    "github.com"
)

LOG_FILE="/var/log/network-monitor.log"
ALERT_THRESHOLD=1000  # 延迟阈值 (ms)

while true; do
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    for host in "${MONITOR_HOSTS[@]}"; do
        # 测试连通性和延迟
        ping_result=$(ping -c 1 -W 5 $host 2>/dev/null)
        
        if [ $? -eq 0 ]; then
            latency=$(echo "$ping_result" | grep 'time=' | sed 's/.*time=\([0-9.]*\).*/\1/')
            status="OK"
            
            # 检查延迟是否超过阈值
            if (( $(echo "$latency > $ALERT_THRESHOLD" | bc -l) )); then
                status="HIGH_LATENCY"
                echo "[$timestamp] ALERT: $host 延迟过高: ${latency}ms" | tee -a $LOG_FILE
            fi
        else
            status="FAILED"
            latency="N/A"
            echo "[$timestamp] ALERT: $host 连接失败" | tee -a $LOG_FILE
        fi
        
        echo "[$timestamp] $host: $status (${latency}ms)" >> $LOG_FILE
    done
    
    sleep 60  # 每分钟检查一次
done
```

#### 7.2 网络告警配置

**告警通知脚本**:
```bash
#!/bin/bash
# 网络告警通知脚本

send_alert() {
    local message="$1"
    local severity="$2"
    
    # 邮件告警
    echo "$message" | mail -s "BK-CI 网络告警 [$severity]" admin@company.com
    
    # 企业微信告警
    curl -X POST \
        -H "Content-Type: application/json" \
        -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"$message\"}}" \
        "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=YOUR_WEBHOOK_KEY"
    
    # Slack 告警
    curl -X POST \
        -H "Content-Type: application/json" \
        -d "{\"text\":\"$message\"}" \
        "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK"
}

# 网络故障告警
if ! ping -c 3 gateway.devops.com > /dev/null 2>&1; then
    send_alert "BK-CI Gateway 连接失败，请立即检查网络状态" "CRITICAL"
fi

# 延迟告警
latency=$(ping -c 5 gateway.devops.com | tail -1 | awk '{print $4}' | cut -d '/' -f 2)
if (( $(echo "$latency > 500" | bc -l) )); then
    send_alert "BK-CI Gateway 延迟过高: ${latency}ms" "WARNING"
fi
```

## 📞 网络问题支持

### 1. 问题上报流程

**网络问题分类**:
- **紧急问题**: 网络完全中断，影响所有用户
- **重要问题**: 网络不稳定，影响部分功能
- **一般问题**: 网络配置疑问，需要指导

**联系方式**:
- **紧急问题**: 网络运维热线 (24小时)
- **重要问题**: 平台支持群 (工作时间 2 小时响应)
- **一般问题**: 工单系统 (工作日 4 小时响应)

### 2. 网络问题报告模板

```markdown
## 网络问题报告

### 基本信息
- **报告时间**: 2025-01-09 14:30:00
- **影响范围**: 构建机/用户端/服务端
- **问题类型**: 连接超时/DNS解析失败/SSL证书错误

### 网络环境
- **网络类型**: 内网/外网/混合
- **代理配置**: 有/无
- **防火墙**: 有/无
- **操作系统**: Ubuntu 20.04

### 问题描述
详细描述网络问题现象

### 错误信息
```
curl: (7) Failed to connect to gateway.devops.com port 443: Connection timed out
```

### 网络诊断结果
```bash
# 粘贴网络诊断脚本的输出结果
```

### 已尝试的解决方案
1. 重启网络服务
2. 清空 DNS 缓存
3. 检查防火墙规则

### 业务影响
- 影响构建机数量: XX台
- 影响用户数: XX人
- 业务影响程度: 高/中/低
```

---

## 📚 相关文档

- [环境问题排查指南](./05-environment-troubleshooting.md)
- [权限问题排查指南](./07-permission-troubleshooting.md)
- [BK-CI 用户使用指南](../../49-bkci-user-guide/)

---

*最后更新时间：2025-01-09*
*文档版本：v2.0*