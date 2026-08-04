# Log 服务 Prometheus 监控指标配置说明

本文档对应 `biz-log` 模块 `LogMetrics`（Micrometer）埋点，用于判断 **Kafka 投递/消费** 与 **ES 读写** 是否异常。

指标出口（无需额外配置）：

```bash
curl -s "http://localhost/management/prometheus" | grep -E '^log_'
```

> 部署后请先用上面命令确认实际指标名。Micrometer Timer 在 Prometheus 中会带 `_seconds_*` 后缀；Counter 名若已以 `_total` 结尾，一般不会再叠一层。

---

## 0. 埋点完备性核对（相对 #13327 压测需求）

| 能力 | 指标 | 打点位置 | 状态 |
|---|---|---|---|
| ES bulk 耗时/成败（含 cluster） | `log_es_bulk*` | `LogStorageBean.bulkRequest` ← `LogBulkAggregator` / `LogServiceESImpl` | ✅ |
| ES 写批耗时/成败 | `log_es_batch_write*` | `LogStorageBean.batchWrite` | ✅ |
| origin 直写成败 | `log_es_direct_write_total` | `LogStorageBean.directWrite` | ✅ |
| 降级到 storage | `log_es_degrade_to_storage_total` | `LogStorageBean.degradeToStorage` | ✅ |
| 直写熔断状态 | `log_es_circuit_open` | `LogMetrics` Gauge ← `LogStorageDegradeSwitcher` | ✅ |
| ES 查询/下载 | `log_es_query*` / `log_es_download*` | `LogStorageBean.query` / `download` | ✅ |
| Kafka 投递（按 destination） | `log_kafka_produce*` | `BuildLogPrintService.sendWithMetrics` | ✅ |
| Kafka 消费（按 destination + retried） | `log_kafka_consume*` | `BuildLogListenerService` 四个 `handleEvent` | ✅ |
| heavy 分流可观测 | `destination=origin_heavy` | produce/consume 自动带标签 | ✅ |
| 打印线程池背压 | `log_print_*` / `log_print_rejected_total` | Gauge + 拒绝 Counter | ✅ |
| 热点 build 规模 | `log_traffic_heavy_size` | `LogTrafficStatsService.heavySize()` | ✅ |

**当前刻意未做（非判断 Kafka/ES 性能的必要项）：**

- Timer 百分位直方图（P99）——默认只有 `_sum/_count/_max`，需要时可再开 `publishPercentileHistogram`
- 腾讯 `MultiESLogClient` inactive 集群 Gauge——可用 `log_es_bulk*{cluster=}` 失败率对比替代
- 按 `buildId` 打点——基数过高，禁止

---

## 1. 指标一览

### 1.1 ES 相关

| 代码指标名 | Prometheus 常见形态 | 类型 | Labels | 含义 |
|---|---|---|---|---|
| `log_es_bulk` | `log_es_bulk_seconds_{count,sum,max}` | Timer | `success`, `cluster` | 真正打到 ES 的 bulk 请求耗时（聚合 flush / 兼容路径） |
| `log_es_bulk_total` | `log_es_bulk_total` | Counter | `success`, `cluster` | bulk 请求次数 |
| `log_es_batch_write` | `log_es_batch_write_seconds_*` | Timer | `success` | origin/storage 路径一次写批处理耗时 |
| `log_es_batch_write_total` | `log_es_batch_write_total` | Counter | `success` | 写批次数 |
| `log_es_direct_write_total` | `log_es_direct_write_total` | Counter | `success` | origin 直写 ES 尝试次数（成功/失败） |
| `log_es_degrade_to_storage_total` | `log_es_degrade_to_storage_total` | Counter | 无 | 直写失败/熔断后降级到 storage 队列次数 |
| `log_es_query` | `log_es_query_seconds_*` | Timer | `success` | 日志查询耗时 |
| `log_es_query_total` | `log_es_query_total` | Counter | `success` | 查询次数 |
| `log_es_download` | `log_es_download_seconds_*` | Timer | `success` | 日志下载耗时 |
| `log_es_download_total` | `log_es_download_total` | Counter | `success` | 下载次数 |
| `log_es_circuit_open` | `log_es_circuit_open` | Gauge | 无 | 直写熔断是否打开：`1`=打开，`0`=关闭 |

`cluster`：ES 集群名（腾讯多集群为真实 `clusterName`；拿不到时为 `unknown`）。

### 1.2 Kafka 相关

| 代码指标名 | Prometheus 常见形态 | 类型 | Labels | 含义 |
|---|---|---|---|---|
| `log_kafka_produce` | `log_kafka_produce_seconds_*` | Timer | `destination`, `success` | 投递到 Kafka（`sendTo`）耗时 |
| `log_kafka_produce_total` | `log_kafka_produce_total` | Counter | `destination`, `success` | 投递次数 |
| `log_kafka_consume` | `log_kafka_consume_seconds_*` | Timer | `destination`, `success`, `retried` | 消费处理（`handleEvent`）耗时 |
| `log_kafka_consume_total` | `log_kafka_consume_total` | Counter | `destination`, `success`, `retried` | 消费次数 |

`destination` 取值：

| 值 | 对应队列 / 事件 |
|---|---|
| `origin` | `build.log.origin.event` / `LogOriginEvent` |
| `origin_heavy` | `build.log.origin.heavy.event` / `LogOriginHeavyEvent` |
| `storage` | `build.log.storage.event` / `LogStorageEvent` |
| `status` | 日志状态事件 / `LogStatusEvent` |
| `unknown` | 未识别类型 |

`retried=true`：本次消费失败且仍会重新投递重试。

> **验证 heavy 分流**：开启 `log.traffic.routeHeavyEnabled=true` 并跑压测后，应出现  
> `log_kafka_produce_total{destination="origin_heavy"}` 与对应 consume；仅看应用业务日志无法证明分流（成功路径默认不打 INFO）。

### 1.3 打印线程池 / 流量

| 代码指标名 | 类型 | 含义 |
|---|---|---|
| `log_print_task_count` | Gauge | 异步打印线程池已完成任务数 |
| `log_print_active_count` | Gauge | 异步打印活跃线程数 |
| `log_print_queue_size` | Gauge | 异步打印队列堆积长度 |
| `log_print_rejected_total` | Counter | 异步打印队列满被拒绝次数（HTTP 509） |
| `log_traffic_heavy_size` | Gauge | 当前仍处于热点粘性窗口的 build 数量 |

---

## 2. 推荐 Grafana 面板（PromQL）

以下表达式按「单实例」写法；多副本请加 `sum by (pod)` / `avg by (pod)`，并带上你们的 `job`/`namespace` 标签。

### 2.1 ES 写入健康

**bulk QPS（按成败）**

```promql
sum(rate(log_es_bulk_seconds_count[1m])) by (success, cluster)
```

**bulk 平均耗时（秒）**

```promql
sum(rate(log_es_bulk_seconds_sum[1m])) by (cluster)
/
sum(rate(log_es_bulk_seconds_count[1m])) by (cluster)
```

**bulk 失败率**

```promql
sum(rate(log_es_bulk_total{success="false"}[1m])) by (cluster)
/
sum(rate(log_es_bulk_total[1m])) by (cluster)
```

**直写成功率**

```promql
sum(rate(log_es_direct_write_total{success="true"}[1m]))
/
sum(rate(log_es_direct_write_total[1m]))
```

**降级到 storage 速率**

```promql
sum(rate(log_es_degrade_to_storage_total[1m]))
```

**熔断状态**

```promql
max(log_es_circuit_open)
```

### 2.2 ES 读（查询/下载）

**查询 QPS / 平均耗时**

```promql
sum(rate(log_es_query_seconds_count[1m])) by (success)

sum(rate(log_es_query_seconds_sum[1m]))
/
sum(rate(log_es_query_seconds_count[1m]))
```

**下载失败率**

```promql
sum(rate(log_es_download_total{success="false"}[1m]))
/
sum(rate(log_es_download_total[1m]))
```

### 2.3 Kafka 投递 / 消费

**各队列投递 QPS**

```promql
sum(rate(log_kafka_produce_seconds_count[1m])) by (destination, success)
```

**各队列消费平均耗时**

```promql
sum(rate(log_kafka_consume_seconds_sum[1m])) by (destination)
/
sum(rate(log_kafka_consume_seconds_count[1m])) by (destination)
```

**消费失败率（按队列）**

```promql
sum(rate(log_kafka_consume_total{success="false"}[1m])) by (destination)
/
sum(rate(log_kafka_consume_total[1m])) by (destination)
```

**重试速率（积压/写失败信号）**

```promql
sum(rate(log_kafka_consume_total{retried="true"}[1m])) by (destination)
```

**origin vs heavy 分流对比（需开启 routeHeavy）**

```promql
sum(rate(log_kafka_produce_total{destination="origin"}[1m]))
sum(rate(log_kafka_produce_total{destination="origin_heavy"}[1m]))
```

### 2.4 入口线程池 / 热点

```promql
# 打印队列堆积
max(log_print_queue_size)

# 热点 build 数
max(log_traffic_heavy_size)

# 队列满拒绝
sum(rate(log_print_rejected_total[1m]))
```

---

## 3. 推荐告警规则（可按环境调阈值）

### P1：ES 写入严重异常

```yaml
# bulk 失败率持续过高
- alert: LogEsBulkHighFailureRate
  expr: |
    (
      sum(rate(log_es_bulk_total{success="false"}[2m]))
      /
      clamp_min(sum(rate(log_es_bulk_total[2m])), 0.001)
    ) > 0.2
  for: 2m
  labels:
    severity: critical
  annotations:
    summary: "log ES bulk 失败率 > 20%"
    description: "可能触发直写超时、熔断降级或 ES 集群不可用"

# 熔断打开
- alert: LogEsCircuitOpen
  expr: max(log_es_circuit_open) == 1
  for: 1m
  labels:
    severity: warning
  annotations:
    summary: "log ES 直写熔断已打开"
    description: "origin 将降级走 storage 队列，请关注 ES 与 storage 积压"
```

### P1：Kafka 消费异常

```yaml
- alert: LogKafkaConsumeHighFailureRate
  expr: |
    (
      sum(rate(log_kafka_consume_total{success="false"}[2m])) by (destination)
      /
      clamp_min(sum(rate(log_kafka_consume_total[2m])) by (destination), 0.001)
    ) > 0.1
  for: 3m
  labels:
    severity: critical
  annotations:
    summary: "log Kafka 消费失败率过高 ({{ $labels.destination }})"

- alert: LogKafkaStorageRetrySpike
  expr: sum(rate(log_kafka_consume_total{destination="storage",retried="true"}[2m])) > 5
  for: 2m
  labels:
    severity: warning
  annotations:
    summary: "storage 队列重试激增"
    description: "通常伴随 ES bulk 超时或熔断后的积压消化"
```

### P2：入口背压 / 读变慢

```yaml
- alert: LogPrintQueueBacklog
  expr: max(log_print_queue_size) > 500
  for: 3m
  labels:
    severity: warning
  annotations:
    summary: "log 异步打印队列堆积"

- alert: LogPrintRejected
  expr: sum(rate(log_print_rejected_total[1m])) > 0
  for: 1m
  labels:
    severity: warning
  annotations:
    summary: "log 异步打印队列满，出现拒绝"

- alert: LogEsQuerySlow
  expr: |
    (
      sum(rate(log_es_query_seconds_sum[5m]))
      /
      clamp_min(sum(rate(log_es_query_seconds_count[5m])), 0.001)
    ) > 1
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "log ES 查询平均耗时 > 1s"
```

---

## 4. 和 #13327 场景的对应关系

| 现象 | 优先看的指标 |
|---|---|
| 突发流量压垮 ES | `log_es_bulk_*` 耗时↑、`success=false`↑；随后 `log_es_degrade_to_storage_total`↑、`log_es_circuit_open=1` |
| 热点 build 是否识别 | `log_traffic_heavy_size` > 0；开启 `routeHeavy` 后看 `destination=origin_heavy` 的 produce/consume |
| 降级后是否在消化 | `destination=storage` 的 consume QPS↑；`retried=true` 短时升高后应回落 |
| heavy 隔离是否生效 | `origin_heavy` 有量，且普通 `origin`/`storage` 耗时不被拖死 |
| 多集群单点劣化（腾讯） | `log_es_bulk_*{cluster="xxx"}` 按集群对比失败率/耗时 |
| 上报入口顶满 | `log_print_queue_size`↑、`log_print_rejected_total`↑ |
| 页面查日志变慢 | `log_es_query_seconds_*`、`log_es_download_seconds_*` |

压测流水线：`bin/log-test-pipeline.yml`（该目录被 gitignore，仅本地/运维使用）。

---

## 5. 采集与权限注意

1. **Scrape**：对 log Pod/Service 抓取 `/management/prometheus`（与现有 actuator 一致）。
2. **多副本**：告警尽量 `sum`/`max by (pod)`，避免被平均值抹平。
3. **标签基数**：`cluster`、`destination` 基数很小，可放心用；不要自行加 `buildId` 等高基数标签。
4. **Histogram**：当前 Timer 默认提供 `_sum/_count/_max`。若要 P99，需在应用侧为 Timer 打开 `publishPercentileHistogram`（后续可再增强）。
5. **无流量时**：部分 Counter/Timer 在首次打点前不出现；Gauge（如 `log_es_circuit_open`）启动即注册。

---

## 6. 快速自检清单

部署含 `LogMetrics` 的版本后：

```bash
# 1) 指标是否出现
curl -s localhost/management/prometheus | grep -E 'log_es_|log_kafka_|log_print_|log_traffic_'

# 2) 跑一轮压测流水线后，确认有成功/失败样本
curl -s localhost/management/prometheus | grep 'log_es_bulk'
curl -s localhost/management/prometheus | grep 'log_kafka_consume'
curl -s localhost/management/prometheus | grep 'log_es_circuit_open\|log_es_degrade'

# 3) 开启 routeHeavy 后确认分流
curl -s localhost/management/prometheus | grep 'destination="origin_heavy"'
```

正常有流量时，至少应能看到：

- `log_kafka_produce_*` / `log_kafka_consume_*`（各 destination）
- `log_es_bulk_*` 或 `log_es_batch_write_*`
- `log_print_queue_size`、`log_es_circuit_open`、`log_traffic_heavy_size`

---

## 7. 代码位置

| 组件 | 路径 |
|---|---|
| 指标封装 | `src/backend/ci/core/log/biz-log/.../metrics/LogMetrics.kt` |
| ES 打点桥接 | `.../jmx/LogStorageBean.kt` |
| Kafka 投递 | `.../service/BuildLogPrintService.kt` |
| Kafka 消费 | `.../service/BuildLogListenerService.kt` |
| Bean 注入 | `.../configuration/LogMQConfiguration.kt` |
| bulk 带 cluster | `.../service/LogBulkAggregator.kt`、`LogServiceESImpl.kt` |
| 热点 size | `.../service/LogTrafficStatsService.heavySize()` |
