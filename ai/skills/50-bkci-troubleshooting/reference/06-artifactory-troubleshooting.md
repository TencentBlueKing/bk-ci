# BK-CI 制品库问题排查指南

## 概述

制品库（Artifactory）是 BK-CI 中用于存储和管理构建制品的核心组件。本文档提供制品库相关问题的详细排查方法和解决方案。

## 制品库架构概述

### 核心组件
- **Artifactory Gateway**: 制品库网关服务
- **Storage Backend**: 存储后端（本地存储/对象存储）
- **Metadata Service**: 元数据管理服务
- **Archive Service**: 归档服务

### 存储类型
- **本地存储**: 基于文件系统的存储
- **对象存储**: 支持 S3、COS、OSS 等
- **混合存储**: 本地+对象存储的混合模式

## 常见问题分类

### 1. 制品上传问题

#### 1.1 上传失败

**症状表现：**
- 制品上传过程中断
- 上传超时错误
- 权限拒绝错误

**排查步骤：**

1. **检查网络连接**
   ```bash
   # 测试制品库服务连通性
   curl -I http://artifactory-gateway:21918/
   
   # 检查网络延迟
   ping artifactory-gateway
   ```

2. **验证存储空间**
   ```bash
   # 检查磁盘空间
   df -h /data/bkce/artifactory/
   
   # 查看存储配置
   kubectl get pv | grep artifactory
   ```

3. **检查文件大小限制**
   ```bash
   # 查看上传文件大小
   ls -lh /tmp/upload/artifact.jar
   
   # 检查服务配置的大小限制
   curl -s http://artifactory-gateway:21918/api/system/config | grep maxFileSize
   ```

4. **查看上传日志**
   ```bash
   # 查看制品库服务日志
   kubectl logs -f deployment/artifactory-gateway -n bk-ci | grep upload
   ```

**解决方案：**
- 检查网络连接稳定性
- 清理磁盘空间或扩容
- 调整文件大小限制配置
- 重试上传操作

#### 1.2 上传权限问题

**症状表现：**
- 403 Forbidden 错误
- 认证失败
- 项目权限不足

**排查步骤：**

1. **检查用户权限**
   ```bash
   # 查看用户项目权限
   curl -H "Authorization: Bearer {token}" \
        http://artifactory-gateway:21918/api/user/projects
   ```

2. **验证仓库权限**
   ```bash
   # 查看仓库权限配置
   curl -H "Authorization: Bearer {token}" \
        http://artifactory-gateway:21918/api/repository/{repoName}/permission
   ```

3. **检查认证配置**
   ```bash
   # 验证 Token 有效性
   curl -H "Authorization: Bearer {token}" \
        http://artifactory-gateway:21918/api/user/info
   ```

**解决方案：**
- 为用户分配正确的项目权限
- 配置仓库访问权限
- 更新或重新生成访问 Token
- 检查 IAM 权限配置

#### 1.3 大文件上传问题

**症状表现：**
- 大文件上传超时
- 分片上传失败
- 内存溢出错误

**排查步骤：**

1. **检查分片上传配置**
   ```yaml
   # 查看分片上传配置
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: artifactory-config
   data:
     application.yml: |
       artifactory:
         upload:
           chunkSize: 10MB
           maxChunks: 1000
           timeout: 3600
   ```

2. **监控内存使用**
   ```bash
   # 查看服务内存使用
   kubectl top pods -l app=artifactory-gateway -n bk-ci
   
   # 查看 JVM 堆内存
   jstat -gc {pid} 1s 5
   ```

3. **检查临时存储**
   ```bash
   # 查看临时目录空间
   df -h /tmp/artifactory/
   
   # 清理临时文件
   find /tmp/artifactory/ -type f -mtime +1 -delete
   ```

**解决方案：**
- 启用分片上传功能
- 增加服务内存配置
- 清理临时存储空间
- 优化上传超时配置

### 2. 制品下载问题

#### 2.1 下载失败

**症状表现：**
- 下载中断或超时
- 文件损坏或不完整
- 404 文件不存在错误

**排查步骤：**

1. **验证文件存在性**
   ```bash
   # 查询制品元数据
   curl -H "Authorization: Bearer {token}" \
        http://artifactory-gateway:21918/api/repository/{repoName}/search?name={fileName}
   ```

2. **检查文件完整性**
   ```bash
   # 验证文件 MD5
   curl -H "Authorization: Bearer {token}" \
        http://artifactory-gateway:21918/api/repository/{repoName}/file/{path}/info
   
   # 本地文件 MD5 校验
   md5sum /path/to/downloaded/file
   ```

3. **查看下载日志**
   ```bash
   # 查看下载访问日志
   kubectl logs -f deployment/artifactory-gateway -n bk-ci | grep download
   ```

4. **检查存储后端**
   ```bash
   # 检查本地存储文件
   ls -la /data/bkce/artifactory/storage/{repoName}/{path}
   
   # 检查对象存储连接
   aws s3 ls s3://artifactory-bucket/{path} --endpoint-url={endpoint}
   ```

**解决方案：**
- 重新上传损坏的文件
- 修复存储后端连接
- 重建文件索引
- 检查网络连接稳定性

#### 2.2 下载性能问题

**症状表现：**
- 下载速度缓慢
- 并发下载限制
- 带宽使用不充分

**排查步骤：**

1. **检查带宽使用**
   ```bash
   # 监控网络带宽
   iftop -i eth0
   
   # 查看网络连接数
   netstat -an | grep :21918 | wc -l
   ```

2. **分析下载瓶颈**
   ```bash
   # 查看磁盘 I/O
   iostat -x 1 5
   
   # 监控 CPU 使用
   top -p $(pgrep -f artifactory)
   ```

3. **检查并发配置**
   ```yaml
   # 查看并发下载配置
   artifactory:
     download:
       maxConcurrent: 50
       bandwidthLimit: 100MB
       cacheEnabled: true
   ```

**解决方案：**
- 增加并发下载数量
- 启用下载缓存
- 优化存储 I/O 性能
- 使用 CDN 加速下载

### 3. 存储后端问题

#### 3.1 本地存储问题

**症状表现：**
- 磁盘空间不足
- 文件系统错误
- 权限问题

**排查步骤：**

1. **检查磁盘使用情况**
   ```bash
   # 查看磁盘空间
   df -h /data/bkce/artifactory/
   
   # 查看 inode 使用情况
   df -i /data/bkce/artifactory/
   
   # 分析目录大小
   du -sh /data/bkce/artifactory/storage/*
   ```

2. **检查文件系统健康**
   ```bash
   # 检查文件系统错误
   dmesg | grep -i "error\|corrupt"
   
   # 运行文件系统检查
   fsck -n /dev/sdb1
   ```

3. **验证文件权限**
   ```bash
   # 检查目录权限
   ls -ld /data/bkce/artifactory/storage/
   
   # 检查文件所有者
   ls -la /data/bkce/artifactory/storage/
   ```

**解决方案：**
- 清理过期文件释放空间
- 扩容存储容量
- 修复文件系统错误
- 调整文件权限

#### 3.2 对象存储问题

**症状表现：**
- 对象存储连接失败
- 认证错误
- 上传/下载超时

**排查步骤：**

1. **测试对象存储连接**
   ```bash
   # 测试 S3 连接
   aws s3 ls --endpoint-url={endpoint}
   
   # 测试腾讯云 COS
   coscli ls cos://bucket-name/
   ```

2. **验证认证配置**
   ```bash
   # 检查访问密钥配置
   kubectl get secret artifactory-storage-secret -o yaml
   ```

3. **查看存储服务日志**
   ```bash
   # 查看对象存储相关日志
   kubectl logs -f deployment/artifactory-gateway -n bk-ci | grep "storage\|s3\|cos"
   ```

4. **测试网络连通性**
   ```bash
   # 测试对象存储端点连通性
   curl -I https://cos.ap-beijing.myqcloud.com
   ```

**解决方案：**
- 更新对象存储认证信息
- 检查网络连接和防火墙
- 调整超时配置
- 验证存储桶权限

### 4. 元数据管理问题

#### 4.1 元数据不一致

**症状表现：**
- 文件存在但查询不到
- 元数据与实际文件不匹配
- 索引损坏

**排查步骤：**

1. **检查数据库连接**
   ```bash
   # 测试数据库连接
   mysql -h mysql-host -u artifactory -p -e "SELECT 1"
   ```

2. **查询元数据记录**
   ```sql
   -- 查询文件元数据
   SELECT * FROM t_repository_file WHERE project_id = 'demo' AND repo_name = 'generic';
   
   -- 检查索引状态
   SHOW INDEX FROM t_repository_file;
   ```

3. **验证文件一致性**
   ```bash
   # 比较数据库记录与实际文件
   mysql -e "SELECT path, size, md5 FROM t_repository_file WHERE repo_name='generic'" > db_files.txt
   find /data/bkce/artifactory/storage/generic/ -type f -exec ls -l {} \; > fs_files.txt
   ```

**解决方案：**
- 重建文件索引
- 同步元数据与实际文件
- 修复数据库索引
- 执行数据一致性检查

#### 4.2 元数据查询性能问题

**症状表现：**
- 文件搜索缓慢
- 数据库查询超时
- 列表操作响应慢

**排查步骤：**

1. **分析慢查询**
   ```sql
   -- 查看慢查询日志
   SELECT * FROM mysql.slow_log WHERE start_time > NOW() - INTERVAL 1 HOUR;
   
   -- 分析查询执行计划
   EXPLAIN SELECT * FROM t_repository_file WHERE project_id = 'demo' AND path LIKE '/path/%';
   ```

2. **检查索引使用**
   ```sql
   -- 查看索引统计信息
   SELECT * FROM information_schema.statistics WHERE table_name = 't_repository_file';
   
   -- 分析索引使用情况
   SHOW STATUS LIKE 'Handler_read%';
   ```

3. **监控数据库性能**
   ```bash
   # 查看数据库连接数
   mysql -e "SHOW STATUS LIKE 'Threads_connected'"
   
   # 监控数据库负载
   mysqladmin -i 1 -c 5 status
   ```

**解决方案：**
- 添加必要的数据库索引
- 优化查询语句
- 增加数据库连接池
- 启用查询缓存

### 5. 清理和归档问题

#### 5.1 自动清理不工作

**症状表现：**
- 过期文件未被清理
- 清理任务执行失败
- 存储空间持续增长

**排查步骤：**

1. **检查清理配置**
   ```yaml
   # 查看清理策略配置
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: artifactory-cleanup-config
   data:
     cleanup.yml: |
       policies:
         - name: "generic-cleanup"
           repository: "generic"
           retentionDays: 30
           sizeLimit: "10GB"
           enabled: true
   ```

2. **查看清理任务状态**
   ```bash
   # 查看定时任务状态
   kubectl get cronjobs -n bk-ci | grep cleanup
   
   # 查看最近的清理任务执行
   kubectl get jobs -n bk-ci | grep cleanup
   ```

3. **检查清理日志**
   ```bash
   # 查看清理任务日志
   kubectl logs -f job/artifactory-cleanup-{timestamp} -n bk-ci
   ```

**解决方案：**
- 修正清理策略配置
- 重启清理定时任务
- 手动执行清理操作
- 检查清理权限配置

#### 5.2 归档功能问题

**症状表现：**
- 文件归档失败
- 归档存储不可用
- 归档文件无法访问

**排查步骤：**

1. **检查归档配置**
   ```yaml
   # 查看归档存储配置
   artifactory:
     archive:
       enabled: true
       storage:
         type: "s3"
         endpoint: "https://s3.amazonaws.com"
         bucket: "artifactory-archive"
       policy:
         archiveAfterDays: 90
         deleteAfterDays: 365
   ```

2. **测试归档存储**
   ```bash
   # 测试归档存储连接
   aws s3 ls s3://artifactory-archive/ --endpoint-url={endpoint}
   ```

3. **查看归档任务日志**
   ```bash
   # 查看归档服务日志
   kubectl logs -f deployment/artifactory-archive -n bk-ci
   ```

**解决方案：**
- 修正归档存储配置
- 验证归档存储权限
- 重新执行归档任务
- 恢复归档文件访问

## 性能优化指南

### 1. 存储性能优化

```yaml
# 存储性能优化配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: artifactory-storage-config
data:
  storage.yml: |
    storage:
      cache:
        enabled: true
        size: "2GB"
        ttl: 3600
      compression:
        enabled: true
        level: 6
      chunking:
        enabled: true
        size: "10MB"
      concurrent:
        upload: 10
        download: 20
```

### 2. 数据库优化

```sql
-- 添加必要索引
ALTER TABLE t_repository_file ADD INDEX idx_project_repo (project_id, repo_name);
ALTER TABLE t_repository_file ADD INDEX idx_path (path(255));
ALTER TABLE t_repository_file ADD INDEX idx_created_date (created_date);

-- 优化表结构
ALTER TABLE t_repository_file ENGINE=InnoDB;
ALTER TABLE t_repository_file ROW_FORMAT=COMPRESSED;
```

### 3. 缓存配置

```yaml
# Redis 缓存配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: artifactory-cache-config
data:
  cache.yml: |
    spring:
      redis:
        host: redis-host
        port: 6379
        database: 1
        jedis:
          pool:
            max-active: 20
            max-idle: 10
    
    artifactory:
      cache:
        metadata:
          enabled: true
          ttl: 1800
        file:
          enabled: true
          ttl: 3600
        search:
          enabled: true
          ttl: 600
```

## 监控和告警

### 关键指标监控

```bash
# 存储使用情况
curl -s http://artifactory-gateway:21918/api/system/storage/usage

# 上传下载统计
curl -s http://artifactory-gateway:21918/api/system/statistics

# 服务健康状态
curl -s http://artifactory-gateway:21918/actuator/health
```

### 告警规则配置

```yaml
# Prometheus 告警规则
groups:
- name: artifactory.rules
  rules:
  - alert: ArtifactoryStorageFull
    expr: artifactory_storage_usage_percent > 90
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "制品库存储空间不足"
      
  - alert: ArtifactoryUploadFailed
    expr: increase(artifactory_upload_failed_total[5m]) > 10
    for: 2m
    labels:
      severity: warning
    annotations:
      summary: "制品上传失败次数过多"
      
  - alert: ArtifactoryServiceDown
    expr: up{job="artifactory-gateway"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "制品库服务不可用"
```

## 故障恢复流程

### 1. 服务恢复

```bash
# 重启制品库服务
kubectl rollout restart deployment/artifactory-gateway -n bk-ci
kubectl rollout restart deployment/artifactory-archive -n bk-ci

# 检查服务状态
kubectl get pods -l app=artifactory -n bk-ci

# 验证服务可用性
curl -s http://artifactory-gateway:21918/actuator/health
```

### 2. 数据恢复

```bash
# 恢复数据库
mysql -h mysql-host -u root -p artifactory < /backup/artifactory.sql

# 恢复文件存储
rsync -av /backup/artifactory/storage/ /data/bkce/artifactory/storage/

# 重建索引
curl -X POST http://artifactory-gateway:21918/api/system/reindex
```

### 3. 一致性检查

```bash
# 执行数据一致性检查
curl -X POST http://artifactory-gateway:21918/api/system/consistency/check

# 修复不一致数据
curl -X POST http://artifactory-gateway:21918/api/system/consistency/repair
```

## 最佳实践

### 1. 存储管理最佳实践

- **分层存储**: 热数据本地存储，冷数据对象存储
- **定期清理**: 配置合理的清理策略
- **容量规划**: 监控存储使用趋势，提前扩容
- **备份策略**: 定期备份重要制品和元数据

### 2. 性能优化最佳实践

- **缓存策略**: 合理配置多级缓存
- **并发控制**: 根据资源情况调整并发数
- **网络优化**: 使用 CDN 加速制品分发
- **索引优化**: 定期分析和优化数据库索引

### 3. 安全管理最佳实践

- **权限控制**: 实施细粒度的访问权限控制
- **审计日志**: 记录所有制品操作日志
- **病毒扫描**: 对上传制品进行安全扫描
- **加密传输**: 使用 HTTPS 保护数据传输

通过以上详细的排查指南和最佳实践，可以有效解决 BK-CI 制品库相关的各种问题，确保制品存储和管理的稳定运行。