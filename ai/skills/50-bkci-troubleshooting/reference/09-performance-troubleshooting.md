# BK-CI 性能问题排查指南

## 概述

性能问题是 BK-CI 系统运行中的常见挑战，涉及流水线执行效率、系统响应速度、资源利用率等多个方面。本文档提供全面的性能问题排查方法和优化策略。

## 性能监控体系

### 关键性能指标 (KPI)

#### 1. 流水线性能指标
- **执行时间**: 流水线总执行时间
- **排队时间**: 任务在队列中的等待时间
- **吞吐量**: 单位时间内完成的构建数量
- **成功率**: 构建成功的百分比

#### 2. 系统性能指标
- **响应时间**: API 接口响应时间
- **并发能力**: 系统支持的最大并发数
- **资源利用率**: CPU、内存、磁盘、网络使用率
- **服务可用性**: 服务正常运行时间百分比

#### 3. 基础设施指标
- **数据库性能**: 查询响应时间、连接数、锁等待
- **消息队列**: 消息堆积、处理速度
- **存储性能**: I/O 吞吐量、延迟
- **网络性能**: 带宽使用、延迟、丢包率

## 常见性能问题分类

### 1. 流水线执行性能问题

#### 1.1 流水线执行缓慢

**症状表现：**
- 流水线执行时间异常长
- 某些步骤耗时过多
- 整体吞吐量下降

**排查步骤：**

1. **分析流水线执行时间分布**
   ```bash
   # 查询流水线执行统计
   curl -s http://process-gateway:21901/ms/process/api/build/service/builds/{projectId}/statistics
   
   # 分析各阶段耗时
   curl -s http://process-gateway:21901/ms/process/api/build/service/builds/{buildId}/stages/duration
   ```

2. **检查构建机资源使用**
   ```bash
   # 查看构建机 CPU 使用率
   kubectl top nodes | grep agent
   
   # 查看构建机内存使用
   kubectl top pods -l app=agent -n bk-ci
   
   # 检查磁盘 I/O
   iostat -x 1 5
   ```

3. **分析插件执行性能**
   ```bash
   # 查看插件执行日志
   kubectl logs -f pod/worker-{pod-id} -n bk-ci | grep "plugin.*duration"
   
   # 统计插件执行时间
   grep "plugin execute" /data/bkce/logs/worker/app.log | awk '{print $NF}' | sort -n
   ```

4. **检查网络延迟**
   ```bash
   # 测试构建机到各服务的网络延迟
   ping -c 5 process-gateway
   ping -c 5 artifactory-gateway
   ping -c 5 repository-gateway
   ```

**解决方案：**
- 优化插件执行逻辑
- 增加构建机资源配置
- 使用并行执行策略
- 优化网络连接

#### 1.2 构建排队时间长

**症状表现：**
- 构建任务长时间处于排队状态
- 构建机资源不足
- 任务分发不均匀

**排查步骤：**

1. **检查构建机状态**
   ```bash
   # 查看可用构建机数量
   curl -s http://environment-gateway:21924/ms/environment/api/build/service/nodes/available
   
   # 查看构建机负载分布
   curl -s http://dispatch-gateway:21938/ms/dispatch/api/build/service/agents/load
   ```

2. **分析任务队列情况**
   ```bash
   # 查看任务队列长度
   curl -s http://dispatch-gateway:21938/ms/dispatch/api/build/service/queue/status
   
   # 查看排队任务详情
   curl -s http://dispatch-gateway:21938/ms/dispatch/api/build/service/queue/pending
   ```

3. **检查调度策略**
   ```bash
   # 查看调度配置
   kubectl get configmap dispatch-config -o yaml | grep -A 10 schedule
   ```

**解决方案：**
- 增加构建机数量
- 优化任务调度算法
- 实施负载均衡策略
- 配置任务优先级

#### 1.3 插件执行性能问题

**症状表现：**
- 特定插件执行缓慢
- 插件资源消耗过高
- 插件超时失败

**排查步骤：**

1. **分析插件性能数据**
   ```bash
   # 查看插件执行统计
   curl -s http://store-gateway:21929/ms/store/api/build/service/plugin/{atomCode}/statistics
   
   # 分析插件资源使用
   kubectl top pods -l plugin={atomCode} -n bk-ci
   ```

2. **检查插件配置**
   ```bash
   # 查看插件配置参数
   curl -s http://store-gateway:21929/ms/store/api/build/service/plugin/{atomCode}/config
   ```

3. **分析插件执行日志**
   ```bash
   # 查看插件详细执行日志
   kubectl logs -f pod/worker-{pod-id} -c plugin-{atomCode} -n bk-ci
   ```

**解决方案：**
- 优化插件算法逻辑
- 调整插件资源限制
- 使用插件缓存机制
- 升级插件版本

### 2. 系统服务性能问题

#### 2.1 API 响应缓慢

**症状表现：**
- 接口响应时间过长
- 用户操作卡顿
- 超时错误频发

**排查步骤：**

1. **监控 API 响应时间**
   ```bash
   # 查看各服务 API 响应时间
   curl -s http://process-gateway:21901/actuator/metrics/http.server.requests
   curl -s http://artifactory-gateway:21918/actuator/metrics/http.server.requests
   ```

2. **分析慢查询**
   ```sql
   -- 查看数据库慢查询
   SELECT * FROM mysql.slow_log WHERE start_time > NOW() - INTERVAL 1 HOUR;
   
   -- 分析查询执行计划
   EXPLAIN SELECT * FROM t_pipeline_build_history WHERE project_id = 'demo' ORDER BY start_time DESC LIMIT 20;
   ```

3. **检查服务资源使用**
   ```bash
   # 查看各微服务资源使用
   kubectl top pods -n bk-ci | grep -E "(process|artifactory|repository)"
   
   # 查看 JVM 性能指标
   curl -s http://process-gateway:21901/actuator/metrics/jvm.memory.used
   ```

4. **分析线程池状态**
   ```bash
   # 查看线程池使用情况
   curl -s http://process-gateway:21901/actuator/metrics/executor.active
   curl -s http://process-gateway:21901/actuator/metrics/executor.queue.remaining
   ```

**解决方案：**
- 优化数据库查询
- 增加服务实例数量
- 调整线程池配置
- 启用接口缓存

#### 2.2 数据库性能瓶颈

**症状表现：**
- 数据库查询缓慢
- 连接池耗尽
- 锁等待超时

**排查步骤：**

1. **监控数据库性能指标**
   ```sql
   -- 查看数据库状态
   SHOW STATUS LIKE 'Threads_connected';
   SHOW STATUS LIKE 'Queries';
   SHOW STATUS LIKE 'Slow_queries';
   
   -- 查看锁等待情况
   SELECT * FROM information_schema.innodb_locks;
   SELECT * FROM information_schema.innodb_lock_waits;
   ```

2. **分析表结构和索引**
   ```sql
   -- 查看表大小
   SELECT table_name, 
          ROUND(((data_length + index_length) / 1024 / 1024), 2) AS 'Size(MB)'
   FROM information_schema.tables 
   WHERE table_schema = 'devops_ci_process';
   
   -- 分析索引使用情况
   SELECT * FROM sys.schema_unused_indexes WHERE object_schema = 'devops_ci_process';
   ```

3. **检查连接池配置**
   ```yaml
   # 查看数据库连接池配置
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
         idle-timeout: 600000
   ```

**解决方案：**
- 添加必要的数据库索引
- 优化 SQL 查询语句
- 调整连接池参数
- 实施读写分离

#### 2.3 消息队列性能问题

**症状表现：**
- 消息堆积严重
- 消息处理延迟
- 消费者处理缓慢

**排查步骤：**

1. **检查消息队列状态**
   ```bash
   # 查看 RabbitMQ 队列状态
   rabbitmqctl list_queues name messages consumers
   
   # 查看消息堆积情况
   curl -u admin:admin http://rabbitmq:15672/api/queues | jq '.[] | {name, messages}'
   ```

2. **监控消费者性能**
   ```bash
   # 查看消费者处理速度
   kubectl logs -f deployment/process-gateway -n bk-ci | grep "message.*consumed"
   ```

3. **分析消息处理逻辑**
   ```bash
   # 查看消息处理时间
   grep "message process duration" /data/bkce/logs/process/app.log
   ```

**解决方案：**
- 增加消费者实例数量
- 优化消息处理逻辑
- 调整队列配置参数
- 实施消息分片策略

### 3. 资源利用率问题

#### 3.1 CPU 使用率过高

**症状表现：**
- 系统响应缓慢
- CPU 使用率持续高位
- 服务频繁超时

**排查步骤：**

1. **识别 CPU 热点**
   ```bash
   # 查看系统 CPU 使用情况
   top -p $(pgrep -f "java.*bk-ci")
   
   # 分析 Java 进程 CPU 使用
   jstack {pid} > thread_dump.txt
   
   # 查看 Kubernetes 资源使用
   kubectl top pods -n bk-ci --sort-by=cpu
   ```

2. **分析 CPU 使用模式**
   ```bash
   # 监控 CPU 使用趋势
   sar -u 1 60
   
   # 查看进程 CPU 使用历史
   pidstat -p {pid} 1 10
   ```

3. **检查 JVM 性能**
   ```bash
   # 查看 GC 情况
   jstat -gc {pid} 1s 10
   
   # 分析 JVM 参数
   jinfo -flags {pid}
   ```

**解决方案：**
- 优化算法和代码逻辑
- 调整 JVM 参数
- 增加 CPU 资源配置
- 实施水平扩展

#### 3.2 内存使用问题

**症状表现：**
- 内存使用率过高
- 频繁 GC 或 OOM
- 系统交换分区使用

**排查步骤：**

1. **分析内存使用情况**
   ```bash
   # 查看系统内存使用
   free -h
   
   # 查看进程内存使用
   ps aux --sort=-%mem | head -20
   
   # 查看 Kubernetes Pod 内存使用
   kubectl top pods -n bk-ci --sort-by=memory
   ```

2. **分析 JVM 堆内存**
   ```bash
   # 查看堆内存使用
   jstat -gccapacity {pid}
   
   # 生成堆转储文件
   jmap -dump:format=b,file=heap.hprof {pid}
   
   # 分析堆内存分布
   jmap -histo {pid} | head -20
   ```

3. **检查内存泄漏**
   ```bash
   # 监控内存使用趋势
   sar -r 1 60
   
   # 查看内存映射
   pmap -x {pid}
   ```

**解决方案：**
- 调整 JVM 堆内存大小
- 修复内存泄漏问题
- 优化对象生命周期
- 增加物理内存

#### 3.3 磁盘 I/O 性能问题

**症状表现：**
- 磁盘 I/O 等待时间长
- 系统响应缓慢
- 存储操作超时

**排查步骤：**

1. **监控磁盘 I/O 性能**
   ```bash
   # 查看磁盘 I/O 统计
   iostat -x 1 5
   
   # 查看磁盘使用情况
   df -h
   
   # 分析 I/O 等待
   sar -d 1 10
   ```

2. **识别 I/O 热点**
   ```bash
   # 查看进程 I/O 使用
   iotop -o
   
   # 分析文件访问模式
   lsof +D /data/bkce/
   ```

3. **检查存储配置**
   ```bash
   # 查看存储类型和配置
   kubectl get pv -o wide
   
   # 检查存储性能
   dd if=/dev/zero of=/data/test bs=1M count=1000 oflag=direct
   ```

**解决方案：**
- 使用 SSD 存储提升性能
- 优化文件访问模式
- 实施存储分层策略
- 调整文件系统参数

## 性能优化策略

### 1. 应用层优化

#### 1.1 代码层面优化

```java
// 数据库查询优化
@Query("SELECT b FROM BuildHistory b WHERE b.projectId = :projectId AND b.startTime >= :startTime ORDER BY b.startTime DESC")
Page<BuildHistory> findRecentBuilds(@Param("projectId") String projectId, 
                                   @Param("startTime") LocalDateTime startTime, 
                                   Pageable pageable);

// 缓存使用
@Cacheable(value = "pipeline", key = "#projectId + '_' + #pipelineId")
public Pipeline getPipeline(String projectId, String pipelineId) {
    return pipelineRepository.findByProjectIdAndPipelineId(projectId, pipelineId);
}

// 异步处理
@Async("taskExecutor")
public CompletableFuture<Void> processNotification(NotificationEvent event) {
    // 异步处理通知逻辑
    return CompletableFuture.completedFuture(null);
}
```

#### 1.2 配置优化

```yaml
# 线程池配置优化
spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50
        queue-capacity: 200
        keep-alive: 60s

# 数据库连接池优化
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

# JVM 参数优化
JAVA_OPTS: >
  -Xms2g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+PrintGCDetails
  -XX:+PrintGCTimeStamps
```

### 2. 数据库优化

#### 2.1 索引优化

```sql
-- 添加复合索引
ALTER TABLE t_pipeline_build_history 
ADD INDEX idx_project_pipeline_start (project_id, pipeline_id, start_time);

-- 添加覆盖索引
ALTER TABLE t_pipeline_build_summary 
ADD INDEX idx_project_status_time (project_id, status, start_time) 
INCLUDE (build_id, pipeline_id, build_num);

-- 分区表优化
ALTER TABLE t_pipeline_build_history 
PARTITION BY RANGE (YEAR(start_time)) (
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026)
);
```

#### 2.2 查询优化

```sql
-- 避免全表扫描
SELECT * FROM t_pipeline_build_history 
WHERE project_id = 'demo' 
  AND start_time >= DATE_SUB(NOW(), INTERVAL 7 DAY)
ORDER BY start_time DESC 
LIMIT 100;

-- 使用批量操作
INSERT INTO t_pipeline_build_detail (build_id, stage_id, status, start_time)
VALUES 
  ('build1', 'stage1', 'RUNNING', NOW()),
  ('build1', 'stage2', 'QUEUE', NOW()),
  ('build1', 'stage3', 'QUEUE', NOW());
```

### 3. 缓存策略优化

#### 3.1 多级缓存

```yaml
# Redis 缓存配置
spring:
  redis:
    host: redis-cluster
    port: 6379
    database: 0
    jedis:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5

# 本地缓存配置
caffeine:
  cache:
    pipeline:
      maximum-size: 1000
      expire-after-write: 300s
    project:
      maximum-size: 500
      expire-after-write: 600s
```

#### 3.2 缓存策略

```java
// 缓存穿透保护
@Cacheable(value = "pipeline", key = "#projectId + '_' + #pipelineId", 
           unless = "#result == null")
public Pipeline getPipeline(String projectId, String pipelineId) {
    Pipeline pipeline = pipelineRepository.findByProjectIdAndPipelineId(projectId, pipelineId);
    return pipeline != null ? pipeline : Pipeline.EMPTY;
}

// 缓存更新策略
@CacheEvict(value = "pipeline", key = "#pipeline.projectId + '_' + #pipeline.pipelineId")
public void updatePipeline(Pipeline pipeline) {
    pipelineRepository.save(pipeline);
}
```

### 4. 基础设施优化

#### 4.1 Kubernetes 资源配置

```yaml
# 服务资源配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: process-gateway
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: process-gateway
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
        env:
        - name: JAVA_OPTS
          value: "-Xms2g -Xmx3g -XX:+UseG1GC"
```

#### 4.2 网络优化

```yaml
# 服务网格配置
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: process-gateway
spec:
  host: process-gateway
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 10
    loadBalancer:
      simple: LEAST_CONN
```

## 性能监控和告警

### 1. 监控指标配置

```yaml
# Prometheus 监控配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    
    scrape_configs:
    - job_name: 'bk-ci-services'
      kubernetes_sd_configs:
      - role: pod
        namespaces:
          names: ['bk-ci']
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
```

### 2. 告警规则

```yaml
# 性能告警规则
groups:
- name: performance.rules
  rules:
  - alert: HighCPUUsage
    expr: rate(container_cpu_usage_seconds_total[5m]) > 0.8
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "容器 CPU 使用率过高"
      
  - alert: HighMemoryUsage
    expr: container_memory_usage_bytes / container_spec_memory_limit_bytes > 0.9
    for: 5m
    labels:
      severity: critical
    annotations:
      summary: "容器内存使用率过高"
      
  - alert: SlowAPIResponse
    expr: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m])) > 2
    for: 3m
    labels:
      severity: warning
    annotations:
      summary: "API 响应时间过长"
```

### 3. 性能分析工具

```bash
# 使用 JProfiler 进行性能分析
java -agentpath:/opt/jprofiler/bin/linux-x64/libjprofilerti.so=port=8849 \
     -jar bk-ci-process.jar

# 使用 Arthas 进行在线诊断
java -jar arthas-boot.jar --target-ip 0.0.0.0

# 火焰图生成
java -jar async-profiler.jar -e cpu -d 60 -f profile.html {pid}
```

## 容量规划

### 1. 容量评估模型

```python
# 容量规划计算
def calculate_capacity(concurrent_builds, avg_build_time, peak_factor=1.5):
    """
    计算所需的构建机容量
    """
    base_capacity = concurrent_builds * avg_build_time / 3600  # 小时
    peak_capacity = base_capacity * peak_factor
    return {
        'base_agents': math.ceil(base_capacity),
        'peak_agents': math.ceil(peak_capacity),
        'recommended_agents': math.ceil(peak_capacity * 1.2)  # 20% 缓冲
    }

# 存储容量规划
def calculate_storage(daily_artifacts, retention_days, growth_rate=0.1):
    """
    计算存储容量需求
    """
    base_storage = daily_artifacts * retention_days
    growth_storage = base_storage * (1 + growth_rate) ** 2  # 2年增长
    return {
        'base_storage_gb': base_storage,
        'growth_storage_gb': growth_storage,
        'recommended_storage_gb': growth_storage * 1.3  # 30% 缓冲
    }
```

### 2. 扩容策略

```yaml
# HPA 自动扩容配置
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: process-gateway-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: process-gateway
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

## 最佳实践总结

### 1. 性能优化原则

- **测量先行**: 先测量再优化，避免过早优化
- **瓶颈识别**: 找到真正的性能瓶颈点
- **渐进优化**: 逐步优化，验证效果
- **全链路考虑**: 从用户请求到数据存储的全链路优化

### 2. 监控体系建设

- **多维度监控**: 应用、系统、业务指标全覆盖
- **实时告警**: 及时发现和响应性能问题
- **趋势分析**: 基于历史数据进行容量规划
- **自动化运维**: 自动扩容、自愈能力

### 3. 持续优化

- **定期评估**: 定期进行性能评估和优化
- **技术升级**: 跟进新技术，持续改进架构
- **经验总结**: 建立性能优化知识库
- **团队培训**: 提升团队性能优化能力

通过系统性的性能问题排查和优化，可以显著提升 BK-CI 系统的整体性能和用户体验。