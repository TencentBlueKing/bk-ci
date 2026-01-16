# BK-CI 代码检查问题排查指南

## 概述

代码检查（CodeCC）是 BK-CI 中用于代码质量检测的重要组件。本文档提供代码检查相关问题的详细排查方法。

## 常见问题分类

### 1. 代码检查任务执行问题

#### 1.1 代码检查任务启动失败

**症状表现：**
- 流水线中代码检查插件执行失败
- 日志显示 "CodeCC task start failed"
- 任务状态一直处于等待状态

**排查步骤：**

1. **检查插件配置**
   ```bash
   # 检查代码检查插件配置
   cat pipeline.yml | grep -A 20 "codecc"
   ```

2. **验证代码库配置**
   - 确认代码库 URL 正确
   - 检查代码库访问权限
   - 验证分支名称是否存在

3. **检查 CodeCC 服务状态**
   ```bash
   # 检查 CodeCC 服务健康状态
   curl -s http://codecc-gateway:21936/ms/schedule/api/build/service/schedule/task/status
   ```

4. **查看详细日志**
   ```bash
   # 查看 CodeCC 调度服务日志
   kubectl logs -f deployment/codecc-schedule -n bk-ci
   
   # 查看 CodeCC 任务服务日志
   kubectl logs -f deployment/codecc-task -n bk-ci
   ```

**解决方案：**
- 修正插件配置参数
- 确保代码库权限正确
- 重启 CodeCC 相关服务
- 检查网络连通性

#### 1.2 代码检查工具配置错误

**症状表现：**
- 特定检查工具执行失败
- 工具版本不兼容
- 规则集配置错误

**排查步骤：**

1. **检查工具配置**
   ```json
   {
     "toolName": "COVERITY",
     "toolVersion": "2019.03",
     "checkerSet": "standard",
     "paramJson": {
       "buildCommand": "make",
       "skipPath": "third_party/"
     }
   }
   ```

2. **验证工具版本兼容性**
   ```bash
   # 查看支持的工具版本
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/checker/tools
   ```

3. **检查规则集配置**
   ```bash
   # 查看可用规则集
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/checker/sets/{toolName}
   ```

**解决方案：**
- 更新工具版本到兼容版本
- 修正规则集配置
- 调整工具参数设置

### 2. 代码扫描执行问题

#### 2.1 扫描超时问题

**症状表现：**
- 代码扫描任务执行时间过长
- 任务超时被强制终止
- 大型项目扫描失败

**排查步骤：**

1. **检查项目规模**
   ```bash
   # 统计代码行数
   find . -name "*.java" -o -name "*.cpp" -o -name "*.c" | xargs wc -l
   
   # 检查文件数量
   find . -type f | wc -l
   ```

2. **查看扫描进度**
   ```bash
   # 查看扫描任务状态
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/task/{taskId}/status
   ```

3. **检查资源使用情况**
   ```bash
   # 查看 Worker 资源使用
   kubectl top pods -l app=codecc-worker -n bk-ci
   ```

**解决方案：**
- 增加扫描超时时间配置
- 优化扫描范围，排除不必要的目录
- 增加 Worker 资源配置
- 使用增量扫描模式

#### 2.2 扫描结果异常

**症状表现：**
- 扫描完成但无结果数据
- 缺陷数据不准确
- 历史数据丢失

**排查步骤：**

1. **检查扫描日志**
   ```bash
   # 查看扫描执行日志
   kubectl logs -f pod/codecc-worker-{pod-id} -n bk-ci
   ```

2. **验证数据存储**
   ```bash
   # 检查 MongoDB 连接
   mongo --host mongodb-host:27017 --eval "db.adminCommand('ismaster')"
   
   # 查询缺陷数据
   mongo codecc --eval "db.defect_entity.find({taskId: '{taskId}'}).count()"
   ```

3. **检查数据同步状态**
   ```bash
   # 查看数据同步服务日志
   kubectl logs -f deployment/codecc-defect -n bk-ci
   ```

**解决方案：**
- 重新执行扫描任务
- 检查数据库连接和权限
- 修复数据同步服务
- 恢复历史数据备份

### 3. 代码检查规则问题

#### 3.1 规则配置不生效

**症状表现：**
- 自定义规则未生效
- 规则严重级别设置无效
- 忽略路径配置失效

**排查步骤：**

1. **检查规则配置格式**
   ```json
   {
     "checkerProps": [
       {
         "checkerKey": "CERT.FLP30-C",
         "checkerSeverity": "SERIOUS",
         "props": {
           "RULE_DESC": "自定义规则描述"
         }
       }
     ],
     "skipPaths": [
       "*/test/*",
       "*/third_party/*"
     ]
   }
   ```

2. **验证规则有效性**
   ```bash
   # 查看规则详情
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/checker/detail/{checkerKey}
   ```

3. **检查路径匹配**
   ```bash
   # 测试路径匹配规则
   echo "src/main/java/Test.java" | grep -E "*/test/*"
   ```

**解决方案：**
- 修正规则配置格式
- 验证规则键值正确性
- 调整路径匹配模式
- 重新加载规则配置

#### 3.2 误报和漏报问题

**症状表现：**
- 大量误报影响开发效率
- 真实问题未被检出
- 规则覆盖不全面

**排查步骤：**

1. **分析误报原因**
   ```bash
   # 查看具体缺陷详情
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/defect/{defectId}
   ```

2. **检查代码上下文**
   ```bash
   # 查看缺陷代码片段
   git show HEAD:{filePath} | sed -n '{startLine},{endLine}p'
   ```

3. **验证规则适用性**
   ```bash
   # 查看规则描述和示例
   curl -s http://codecc-gateway:21936/ms/defect/api/build/service/checker/example/{checkerKey}
   ```

**解决方案：**
- 调整规则严重级别
- 添加特定忽略注释
- 优化规则参数配置
- 提交规则改进建议

### 4. 性能和资源问题

#### 4.1 扫描性能慢

**症状表现：**
- 扫描时间过长
- 系统响应缓慢
- 资源使用率高

**排查步骤：**

1. **监控资源使用**
   ```bash
   # 查看 CPU 和内存使用
   kubectl top pods -n bk-ci | grep codecc
   
   # 查看磁盘 I/O
   iostat -x 1 5
   ```

2. **分析扫描瓶颈**
   ```bash
   # 查看扫描各阶段耗时
   grep "stage.*cost" /var/log/codecc/scan.log
   ```

3. **检查并发配置**
   ```yaml
   # 查看 Worker 并发配置
   kubectl get configmap codecc-worker-config -o yaml
   ```

**解决方案：**
- 增加 Worker 实例数量
- 优化扫描并发度
- 使用 SSD 存储提升 I/O
- 启用增量扫描

#### 4.2 内存溢出问题

**症状表现：**
- Worker 进程异常退出
- OOM (Out of Memory) 错误
- 大文件扫描失败

**排查步骤：**

1. **查看内存使用情况**
   ```bash
   # 查看 Pod 内存限制和使用
   kubectl describe pod codecc-worker-{pod-id} -n bk-ci
   
   # 查看系统内存
   free -h
   ```

2. **分析内存泄漏**
   ```bash
   # 查看 Java 堆内存使用
   jstat -gc {pid} 1s 10
   
   # 生成堆转储文件
   jmap -dump:format=b,file=heap.hprof {pid}
   ```

3. **检查大文件处理**
   ```bash
   # 查找大文件
   find . -type f -size +100M
   ```

**解决方案：**
- 增加 Worker 内存限制
- 优化大文件处理逻辑
- 调整 JVM 堆内存参数
- 分批处理大型项目

## 日志分析指南

### 关键日志位置

```bash
# CodeCC 调度服务日志
/data/bkce/logs/codecc/schedule/

# CodeCC 任务服务日志
/data/bkce/logs/codecc/task/

# CodeCC 缺陷服务日志
/data/bkce/logs/codecc/defect/

# Worker 扫描日志
/data/bkce/logs/codecc/worker/
```

### 重要日志关键词

```bash
# 搜索错误信息
grep -i "error\|exception\|failed" /data/bkce/logs/codecc/*/app.log

# 搜索超时信息
grep -i "timeout\|time out" /data/bkce/logs/codecc/*/app.log

# 搜索内存问题
grep -i "outofmemory\|oom\|memory" /data/bkce/logs/codecc/*/app.log
```

## 配置优化建议

### 扫描性能优化

```yaml
# codecc-worker 配置优化
apiVersion: v1
kind: ConfigMap
metadata:
  name: codecc-worker-config
data:
  application.yml: |
    worker:
      concurrent: 4              # 并发扫描数
      memory: "4Gi"             # 内存限制
      timeout: 7200             # 超时时间(秒)
      incremental: true         # 启用增量扫描
      cache:
        enabled: true           # 启用缓存
        size: "1Gi"            # 缓存大小
```

### 规则集优化

```json
{
  "toolName": "COVERITY",
  "checkerSetType": "CUSTOM",
  "checkerProps": [
    {
      "checkerKey": "DEADCODE",
      "checkerSeverity": "NORMAL",
      "props": {
        "SKIP_FUNCTION_SIZE": "200"
      }
    }
  ],
  "skipPaths": [
    "*/build/*",
    "*/target/*",
    "*/node_modules/*",
    "*/third_party/*",
    "*/test/*",
    "*/.git/*"
  ]
}
```

## 监控和告警

### 关键指标监控

```bash
# 扫描成功率
curl -s http://codecc-gateway:21936/ms/defect/api/build/service/metrics/scan/success/rate

# 平均扫描时间
curl -s http://codecc-gateway:21936/ms/defect/api/build/service/metrics/scan/duration/avg

# 缺陷趋势
curl -s http://codecc-gateway:21936/ms/defect/api/build/service/metrics/defect/trend
```

### 告警规则配置

```yaml
# Prometheus 告警规则
groups:
- name: codecc.rules
  rules:
  - alert: CodeCCScanFailed
    expr: codecc_scan_failed_total > 5
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "CodeCC 扫描失败次数过多"
      
  - alert: CodeCCScanTimeout
    expr: codecc_scan_duration_seconds > 3600
    for: 10m
    labels:
      severity: critical
    annotations:
      summary: "CodeCC 扫描超时"
```

## 故障恢复流程

### 1. 服务重启恢复

```bash
# 重启 CodeCC 相关服务
kubectl rollout restart deployment/codecc-schedule -n bk-ci
kubectl rollout restart deployment/codecc-task -n bk-ci
kubectl rollout restart deployment/codecc-defect -n bk-ci
kubectl rollout restart deployment/codecc-worker -n bk-ci

# 检查服务状态
kubectl get pods -l app=codecc -n bk-ci
```

### 2. 数据恢复

```bash
# 恢复 MongoDB 数据
mongorestore --host mongodb-host:27017 --db codecc /backup/codecc/

# 重建索引
mongo codecc --eval "db.defect_entity.reIndex()"
```

### 3. 任务重新执行

```bash
# 重新触发扫描任务
curl -X POST http://codecc-gateway:21936/ms/schedule/api/build/service/schedule/task/{taskId}/restart
```

## 预防措施

### 1. 定期维护

- 每周清理过期扫描数据
- 每月检查规则集更新
- 定期备份配置和数据

### 2. 容量规划

- 监控存储使用情况
- 预估扫描资源需求
- 制定扩容计划

### 3. 最佳实践

- 使用增量扫描减少资源消耗
- 合理配置忽略路径
- 定期优化规则集配置
- 建立扫描质量基线

通过以上详细的排查指南，可以有效解决 BK-CI 代码检查过程中遇到的各种问题，确保代码质量检测的稳定运行。