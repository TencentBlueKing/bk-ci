# BK-CI 质量红线问题排查指南

## 概述

质量红线是 BK-CI 中用于控制代码质量和发布质量的重要机制。本文档提供质量红线相关问题的详细排查方法和解决方案。

## 质量红线架构概述

### 核心组件
- **Quality Gateway**: 质量红线网关服务
- **Rule Engine**: 规则引擎
- **Metrics Collector**: 指标收集器
- **Interceptor**: 拦截器组件

### 工作流程
1. 流水线执行到质量红线节点
2. 收集各类质量指标数据
3. 根据配置的规则进行评估
4. 决定是否允许继续执行

## 常见问题分类

### 1. 质量红线配置问题

#### 1.1 规则配置错误

**症状表现：**
- 质量红线规则不生效
- 规则判断结果异常
- 阈值设置不合理

**排查步骤：**

1. **检查规则配置**
   ```bash
   # 查看质量红线规则配置
   curl -s http://quality-gateway:21925/ms/quality/api/build/service/rule/list/{projectId}
   ```

2. **验证规则语法**
   ```json
   {
     "ruleId": "COVERAGE_RULE",
     "ruleName": "代码覆盖率检查",
     "ruleType": "COVERAGE",
     "threshold": {
       "operator": "GTE",
       "value": 80.0
     },
     "enabled": true,
     "gateKeeper": "BEFORE_MERGE"
   }
   ```

3. **检查规则关联**
   ```bash
   # 查看流水线关联的质量红线
   curl -s http://quality-gateway:21925/ms/quality/api/build/service/pipeline/{pipelineId}/rules
   ```

**解决方案：**
- 修正规则配置语法
- 调整阈值设置
- 重新关联流水线规则
- 验证规则逻辑正确性

#### 1.2 指标收集配置问题

**症状表现：**
- 指标数据收集失败
- 数据源配置错误
- 指标计算异常

**排查步骤：**

1. **检查数据源配置**
   ```yaml
   # 查看指标收集配置
   apiVersion: v1
   kind: ConfigMap
   metadata:
     name: quality-metrics-config
   data:
     metrics.yml: |
       coverage:
         source: "jacoco"
         path: "target/site/jacoco/jacoco.xml"
       security:
         source: "sonarqube"
         endpoint: "http://sonar:9000"
   ```

2. **验证数据源连通性**
   ```bash
   # 测试 SonarQube 连接
   curl -s http://sonar:9000/api/system/status
   
   # 检查覆盖率报告文件
   ls -la target/site/jacoco/jacoco.xml
   ```

3. **查看指标收集日志**
   ```bash
   # 查看质量红线服务日志
   kubectl logs -f deployment/quality-gateway -n bk-ci | grep "metrics"
   ```

**解决方案：**
- 修正数据源配置
- 确保数据源服务可用
- 检查文件路径正确性
- 重新配置指标收集器

### 2. 质量红线执行问题

#### 2.1 红线检查超时

**症状表现：**
- 质量红线检查长时间无响应
- 流水线在红线节点卡住
- 超时错误信息

**排查步骤：**

1. **检查服务状态**
   ```bash
   # 查看质量红线服务状态
   kubectl get pods -l app=quality-gateway -n bk-ci
   
   # 检查服务资源使用
   kubectl top pods -l app=quality-gateway -n bk-ci
   ```

2. **查看执行日志**
   ```bash
   # 查看质量红线执行日志
   kubectl logs -f deployment/quality-gateway -n bk-ci --tail=100
   ```

3. **检查数据库连接**
   ```bash
   # 测试数据库连接
   mysql -h mysql-host -u quality -p -e "SELECT 1"
   ```

4. **分析执行时间**
   ```bash
   # 查看各步骤执行时间
   grep "step.*duration" /data/bkce/logs/quality/app.log
   ```

**解决方案：**
- 增加超时时间配置
- 优化数据查询性能
- 增加服务资源配置
- 重启质量红线服务

#### 2.2 红线拦截异常

**症状表现：**
- 应该通过的流水线被拦截
- 应该拦截的流水线通过了
- 拦截逻辑判断错误

**排查步骤：**

1. **查看拦截详情**
   ```bash
   # 查看具体拦截信息
   curl -s http://quality-gateway:21925/ms/quality/api/build/service/intercept/{buildId}/detail
   ```

2. **检查指标数据**
   ```bash
   # 查看收集到的指标数据
   curl -s http://quality-gateway:21925/ms/quality/api/build/service/metrics/{buildId}
   ```

3. **验证规则计算**
   ```bash
   # 手动验证规则计算逻辑
   echo "覆盖率: 75%, 阈值: >=80%, 结果: $([ 75 -ge 80 ] && echo 'PASS' || echo 'FAIL')"
   ```

4. **查看规则执行日志**
   ```bash
   # 查看规则引擎日志
   grep "rule.*execute" /data/bkce/logs/quality/app.log
   ```

**解决方案：**
- 检查规则配置正确性
- 验证指标数据准确性
- 修正规则计算逻辑
- 重新执行质量检查

### 3. 指标数据问题

#### 3.1 代码覆盖率数据异常

**症状表现：**
- 覆盖率数据为 0 或异常值
- 覆盖率报告解析失败
- 覆盖率数据不更新

**排查步骤：**

1. **检查覆盖率报告**
   ```bash
   # 查看 JaCoCo 报告
   cat target/site/jacoco/jacoco.xml | head -20
   
   # 检查报告文件大小
   ls -lh target/site/jacoco/jacoco.xml
   ```

2. **验证测试执行**
   ```bash
   # 检查测试是否执行
   grep "Tests run:" target/surefire-reports/*.txt
   
   # 查看测试覆盖情况
   find . -name "*.exec" -o -name "*.ec"
   ```

3. **检查解析逻辑**
   ```bash
   # 查看覆盖率解析日志
   grep "coverage.*parse" /data/bkce/logs/quality/app.log
   ```

**解决方案：**
- 确保测试正确执行
- 检查覆盖率工具配置
- 修正报告文件路径
- 更新解析逻辑

#### 3.2 安全扫描数据问题

**症状表现：**
- 安全漏洞数据获取失败
- SonarQube 集成异常
- 安全等级评估错误

**排查步骤：**

1. **检查 SonarQube 集成**
   ```bash
   # 测试 SonarQube API
   curl -u admin:admin http://sonar:9000/api/projects/search
   
   # 查看项目分析状态
   curl -u admin:admin http://sonar:9000/api/ce/activity?component={projectKey}
   ```

2. **验证认证配置**
   ```bash
   # 检查 SonarQube 认证
   curl -u {token}: http://sonar:9000/api/authentication/validate
   ```

3. **查看安全扫描日志**
   ```bash
   # 查看安全扫描执行日志
   grep "security.*scan" /data/bkce/logs/quality/app.log
   ```

**解决方案：**
- 修正 SonarQube 连接配置
- 更新认证凭据
- 重新执行安全扫描
- 检查项目配置

#### 3.3 代码规范检查数据问题

**症状表现：**
- 代码规范检查结果异常
- 规范违规数量不准确
- 检查工具集成失败

**排查步骤：**

1. **检查代码规范工具**
   ```bash
   # 查看 Checkstyle 报告
   cat target/checkstyle-result.xml
   
   # 检查 PMD 报告
   cat target/pmd.xml
   ```

2. **验证工具配置**
   ```bash
   # 检查 Checkstyle 配置
   cat checkstyle.xml | head -10
   
   # 验证 PMD 规则集
   cat pmd-ruleset.xml | head -10
   ```

3. **查看解析日志**
   ```bash
   # 查看代码规范解析日志
   grep "checkstyle\|pmd" /data/bkce/logs/quality/app.log
   ```

**解决方案：**
- 更新工具配置文件
- 修正报告解析逻辑
- 重新执行代码检查
- 调整规则集配置

### 4. 性能和稳定性问题

#### 4.1 质量红线服务性能问题

**症状表现：**
- 质量检查响应缓慢
- 服务 CPU/内存使用率高
- 并发处理能力不足

**排查步骤：**

1. **监控资源使用**
   ```bash
   # 查看服务资源使用
   kubectl top pods -l app=quality-gateway -n bk-ci
   
   # 查看 JVM 内存使用
   jstat -gc {pid} 1s 5
   ```

2. **分析性能瓶颈**
   ```bash
   # 查看慢查询日志
   grep "slow.*query" /data/bkce/logs/quality/app.log
   
   # 分析数据库性能
   mysql -e "SHOW PROCESSLIST" | grep quality
   ```

3. **检查并发配置**
   ```yaml
   # 查看线程池配置
   quality:
     executor:
       corePoolSize: 10
       maxPoolSize: 50
       queueCapacity: 200
   ```

**解决方案：**
- 增加服务实例数量
- 优化数据库查询
- 调整线程池配置
- 启用缓存机制

#### 4.2 数据库性能问题

**症状表现：**
- 数据库查询缓慢
- 连接池耗尽
- 锁等待超时

**排查步骤：**

1. **检查数据库性能**
   ```sql
   -- 查看慢查询
   SELECT * FROM mysql.slow_log WHERE start_time > NOW() - INTERVAL 1 HOUR;
   
   -- 查看锁等待
   SELECT * FROM information_schema.innodb_locks;
   
   -- 查看连接数
   SHOW STATUS LIKE 'Threads_connected';
   ```

2. **分析索引使用**
   ```sql
   -- 查看表索引
   SHOW INDEX FROM t_quality_rule;
   
   -- 分析查询执行计划
   EXPLAIN SELECT * FROM t_quality_rule WHERE project_id = 'demo';
   ```

3. **检查连接池配置**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20
         minimum-idle: 5
         connection-timeout: 30000
   ```

**解决方案：**
- 优化 SQL 查询语句
- 添加必要的数据库索引
- 调整连接池配置
- 分库分表处理

## 配置优化指南

### 1. 质量红线规则优化

```json
{
  "rules": [
    {
      "ruleId": "COVERAGE_RULE",
      "ruleName": "代码覆盖率检查",
      "ruleType": "COVERAGE",
      "threshold": {
        "operator": "GTE",
        "value": 80.0
      },
      "enabled": true,
      "gateKeeper": "BEFORE_MERGE",
      "weight": 0.4
    },
    {
      "ruleId": "SECURITY_RULE", 
      "ruleName": "安全漏洞检查",
      "ruleType": "SECURITY",
      "threshold": {
        "operator": "LTE",
        "value": 0
      },
      "enabled": true,
      "gateKeeper": "BEFORE_DEPLOY",
      "weight": 0.6
    }
  ],
  "strategy": "WEIGHTED_AVERAGE",
  "passThreshold": 75.0
}
```

### 2. 性能优化配置

```yaml
# quality-gateway 服务配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: quality-gateway-config
data:
  application.yml: |
    server:
      tomcat:
        max-threads: 200
        accept-count: 100
    
    spring:
      datasource:
        hikari:
          maximum-pool-size: 20
          minimum-idle: 5
          connection-timeout: 30000
          idle-timeout: 600000
      
      redis:
        jedis:
          pool:
            max-active: 20
            max-idle: 10
    
    quality:
      cache:
        enabled: true
        ttl: 300
      executor:
        corePoolSize: 10
        maxPoolSize: 50
        queueCapacity: 200
      timeout:
        rule-execution: 300
        metrics-collection: 180
```

### 3. 监控配置

```yaml
# Prometheus 监控配置
apiVersion: v1
kind: ConfigMap
metadata:
  name: quality-monitoring-config
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
    
    scrape_configs:
    - job_name: 'quality-gateway'
      static_configs:
      - targets: ['quality-gateway:21925']
      metrics_path: '/actuator/prometheus'
      scrape_interval: 30s
```

## 故障恢复流程

### 1. 服务重启恢复

```bash
# 重启质量红线服务
kubectl rollout restart deployment/quality-gateway -n bk-ci

# 检查服务状态
kubectl get pods -l app=quality-gateway -n bk-ci

# 验证服务可用性
curl -s http://quality-gateway:21925/actuator/health
```

### 2. 数据恢复

```bash
# 恢复质量红线配置数据
mysql -h mysql-host -u root -p quality < /backup/quality_rules.sql

# 重建索引
mysql -h mysql-host -u root -p -e "
USE quality;
ALTER TABLE t_quality_rule ADD INDEX idx_project_id (project_id);
ALTER TABLE t_quality_metrics ADD INDEX idx_build_id (build_id);
"
```

### 3. 缓存重建

```bash
# 清理 Redis 缓存
redis-cli -h redis-host FLUSHDB

# 重新加载规则缓存
curl -X POST http://quality-gateway:21925/ms/quality/api/build/service/cache/reload
```

## 监控和告警

### 关键指标监控

```bash
# 质量红线通过率
curl -s http://quality-gateway:21925/ms/quality/api/build/service/metrics/pass/rate

# 平均检查时间
curl -s http://quality-gateway:21925/ms/quality/api/build/service/metrics/check/duration

# 服务可用性
curl -s http://quality-gateway:21925/actuator/health
```

### 告警规则

```yaml
# Prometheus 告警规则
groups:
- name: quality.rules
  rules:
  - alert: QualityGatewayDown
    expr: up{job="quality-gateway"} == 0
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "质量红线服务不可用"
      
  - alert: QualityCheckTimeout
    expr: quality_check_duration_seconds > 300
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "质量检查超时"
      
  - alert: QualityPassRateLow
    expr: quality_pass_rate < 0.8
    for: 10m
    labels:
      severity: warning
    annotations:
      summary: "质量红线通过率过低"
```

## 最佳实践

### 1. 规则配置最佳实践

- **渐进式质量提升**: 逐步提高质量阈值
- **差异化规则**: 不同项目类型使用不同规则
- **合理权重**: 根据项目特点设置指标权重
- **及时调整**: 定期评估和调整规则配置

### 2. 性能优化最佳实践

- **缓存策略**: 合理使用缓存减少重复计算
- **异步处理**: 使用异步方式处理耗时操作
- **批量操作**: 批量收集和处理指标数据
- **资源隔离**: 不同优先级任务使用不同资源池

### 3. 运维管理最佳实践

- **定期备份**: 定期备份质量规则和历史数据
- **监控告警**: 建立完善的监控告警体系
- **容量规划**: 根据使用情况进行容量规划
- **文档维护**: 维护详细的配置和操作文档

通过以上详细的排查指南和最佳实践，可以有效解决 BK-CI 质量红线相关的各种问题，确保代码质量管控的稳定运行。