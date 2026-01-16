# BK-CI 故障快速排查检查清单

## 使用说明

本检查清单用于快速定位常见问题，按照清单顺序逐项检查，可快速排除大部分故障。

---

## 一、服务不可用检查清单

### 1.1 网关层检查
- [ ] Nginx/OpenResty 进程是否运行
  ```bash
  ps aux | grep nginx
  systemctl status openresty
  ```
- [ ] 网关配置是否正确
  ```bash
  nginx -t
  ```
- [ ] 端口是否监听
  ```bash
  netstat -tlnp | grep 80
  ```
- [ ] 访问日志是否有错误
  ```bash
  tail -f /data/bkci/logs/gateway/access.log
  tail -f /data/bkci/logs/gateway/error.log
  ```

### 1.2 后端服务检查
- [ ] 服务进程是否运行
  ```bash
  ps aux | grep java | grep bkci
  ```
- [ ] 服务健康检查
  ```bash
  curl http://localhost:21912/api/health  # project
  curl http://localhost:21921/api/health  # process
  ```
- [ ] 服务日志是否有错误
  ```bash
  tail -f /data/bkci/logs/ci/*/error.log
  ```

### 1.3 依赖组件检查
- [ ] MySQL 是否可连接
  ```bash
  mysql -h localhost -u root -p -e "SELECT 1"
  ```
- [ ] Redis 是否可连接
  ```bash
  redis-cli ping
  ```
- [ ] RabbitMQ 是否正常
  ```bash
  rabbitmqctl status
  ```
- [ ] Consul 是否正常
  ```bash
  consul members
  ```

---

## 二、构建失败检查清单

### 2.1 基础检查
- [ ] 流水线配置是否正确
- [ ] 启动参数是否完整
- [ ] 触发条件是否满足

### 2.2 构建机检查
- [ ] 是否有可用构建机
  ```bash
  # 查看 Agent 状态
  curl http://localhost:21923/api/dispatch/agents
  ```
- [ ] 构建机资源是否充足
  ```bash
  # 在构建机上执行
  df -h
  free -m
  top -bn1 | head -20
  ```
- [ ] Agent 进程是否正常
  ```bash
  ps aux | grep devopsAgent
  ```

### 2.3 插件检查
- [ ] 插件是否存在
- [ ] 插件版本是否正确
- [ ] 插件是否有执行权限
- [ ] 插件依赖是否满足

### 2.4 日志检查
- [ ] 查看构建日志
  ```bash
  # 通过 API 获取
  curl "http://localhost:21941/api/log?buildId=xxx"
  ```
- [ ] 查看 Worker 日志
  ```bash
  tail -f /data/bkci/logs/worker/*.log
  ```

---

## 三、Agent 问题检查清单

### 3.1 进程检查
- [ ] Agent 进程是否存在
  ```bash
  ps aux | grep devopsAgent
  pgrep -f devopsAgent
  ```
- [ ] 进程资源占用
  ```bash
  top -p $(pgrep -f devopsAgent)
  ```

### 3.2 网络检查
- [ ] 能否访问 Gateway
  ```bash
  curl -v http://{gateway_host}/ms/environment
  ```
- [ ] DNS 解析是否正常
  ```bash
  nslookup {gateway_host}
  ```
- [ ] 防火墙是否放行
  ```bash
  telnet {gateway_host} 80
  ```

### 3.3 配置检查
- [ ] 配置文件是否正确
  ```bash
  cat /data/bkci/agent/.agent.properties
  ```
- [ ] Agent ID 是否有效
- [ ] 密钥是否正确

### 3.4 日志检查
- [ ] Agent 日志
  ```bash
  tail -f /data/bkci/logs/agent/agent.log
  ```
- [ ] 系统日志
  ```bash
  journalctl -u devops-agent -f
  ```

---

## 四、性能问题检查清单

### 4.1 系统资源
- [ ] CPU 使用率
  ```bash
  top -bn1 | head -5
  mpstat 1 5
  ```
- [ ] 内存使用
  ```bash
  free -m
  vmstat 1 5
  ```
- [ ] 磁盘 I/O
  ```bash
  iostat -x 1 5
  df -h
  ```
- [ ] 网络流量
  ```bash
  iftop
  nethogs
  ```

### 4.2 JVM 检查
- [ ] 堆内存使用
  ```bash
  jstat -gc $(pgrep -f process) 1000 5
  ```
- [ ] GC 情况
  ```bash
  jstat -gcutil $(pgrep -f process) 1000 5
  ```
- [ ] 线程状态
  ```bash
  jstack $(pgrep -f process) | grep -c "java.lang.Thread.State"
  ```

### 4.3 数据库检查
- [ ] 慢查询
  ```sql
  SHOW PROCESSLIST;
  SHOW STATUS LIKE 'Slow_queries';
  ```
- [ ] 连接数
  ```sql
  SHOW STATUS LIKE 'Threads_connected';
  ```
- [ ] 锁等待
  ```sql
  SELECT * FROM information_schema.innodb_lock_waits;
  ```

### 4.4 缓存检查
- [ ] Redis 内存
  ```bash
  redis-cli info memory
  ```
- [ ] Redis 连接数
  ```bash
  redis-cli info clients
  ```
- [ ] 命中率
  ```bash
  redis-cli info stats | grep keyspace
  ```

---

## 五、权限问题检查清单

### 5.1 用户权限
- [ ] 用户是否存在
- [ ] 用户是否为项目成员
- [ ] 用户角色是否正确

### 5.2 资源权限
- [ ] 是否有流水线查看/编辑权限
- [ ] 是否有代码库访问权限
- [ ] 是否有凭证使用权限

### 5.3 IAM 检查
- [ ] IAM 服务是否正常
- [ ] 权限策略是否正确
- [ ] 权限是否已同步

---

## 六、日志收集检查清单

### 需要收集的信息
- [ ] 错误码
- [ ] 请求 ID (X-DEVOPS-RID)
- [ ] 发生时间
- [ ] 用户 ID
- [ ] 项目 ID
- [ ] 流水线 ID（如适用）
- [ ] 构建 ID（如适用）

### 需要收集的日志
- [ ] Gateway 访问日志
- [ ] Gateway 错误日志
- [ ] 对应服务日志
- [ ] 数据库慢查询日志（如适用）

### 日志收集命令
```bash
# 一键收集脚本
/data/bkci/scripts/log-collector.sh -t "2024-01-01 10:00:00" -d 10
```

---

## 七、紧急恢复检查清单

### 7.1 服务重启
```bash
# 重启单个服务
systemctl restart bkci-process

# 重启所有服务
systemctl restart bkci-*
```

### 7.2 清理缓存
```bash
# 清理 Redis 缓存（谨慎操作）
redis-cli FLUSHDB

# 清理本地缓存
rm -rf /data/bkci/cache/*
```

### 7.3 数据库恢复
```bash
# 终止长事务
mysql -e "KILL <process_id>"

# 释放锁
mysql -e "UNLOCK TABLES"
```

### 7.4 磁盘清理
```bash
# 清理日志
find /data/bkci/logs -name "*.log" -mtime +7 -delete

# 清理临时文件
rm -rf /data/bkci/tmp/*
```

---

## 常用命令速查

| 场景 | 命令 |
|------|------|
| 查看服务状态 | `systemctl status bkci-*` |
| 查看服务日志 | `tail -f /data/bkci/logs/ci/*/error.log` |
| 查看 JVM 状态 | `jstat -gc $(pgrep -f process) 1000` |
| 查看数据库连接 | `mysql -e "SHOW PROCESSLIST"` |
| 查看 Redis 状态 | `redis-cli info` |
| 查看 Agent 状态 | `ps aux \| grep devopsAgent` |
| 网络连通测试 | `curl -v http://gateway/api/health` |
| 磁盘使用情况 | `df -h` |
| 内存使用情况 | `free -m` |
| CPU 使用情况 | `top -bn1 \| head -20` |
