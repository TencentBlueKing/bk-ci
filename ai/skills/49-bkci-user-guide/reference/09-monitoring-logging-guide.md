# 监控与日志管理指南

## 概述

蓝盾提供了全面的监控和日志管理能力，帮助用户实时了解流水线执行状态、性能指标和问题排查。通过合理使用监控和日志功能，可以提高系统可观测性，快速定位和解决问题。

## 构建日志管理

### 1. 日志查看

#### 实时日志查看
```yaml
# 在流水线执行过程中查看实时日志
- name: "查看实时日志"
  run: |
    echo "开始执行构建任务"
    
    # 输出带时间戳的日志
    echo "$(date '+%Y-%m-%d %H:%M:%S') - 开始编译"
    
    # 输出彩色日志（支持ANSI颜色码）
    echo -e "\033[32m成功: 编译完成\033[0m"
    echo -e "\033[31m错误: 发现问题\033[0m"
    echo -e "\033[33m警告: 需要注意\033[0m"
    
    # 输出结构化日志
    echo "##[section]开始测试阶段"
    echo "##[debug]调试信息: 变量值为 $VAR_NAME"
    echo "##[warning]警告: 配置文件未找到"
    echo "##[error]错误: 连接数据库失败"
```

#### 日志级别控制
```yaml
# 设置日志级别
- name: "配置日志级别"
  env:
    LOG_LEVEL: "DEBUG"  # DEBUG, INFO, WARN, ERROR
  run: |
    case $LOG_LEVEL in
      "DEBUG")
        echo "##[debug]调试模式已启用"
        set -x  # 显示执行的命令
        ;;
      "INFO")
        echo "##[info]信息模式"
        ;;
      "WARN")
        echo "##[warning]警告模式"
        ;;
      "ERROR")
        echo "##[error]错误模式"
        ;;
    esac
```

### 2. 日志下载和导出

#### 下载插件执行日志
1. 在插件执行界面右上角点击三点图标
2. 选择"下载日志"按钮
3. 日志将保存为文本文件供后续分析

#### 批量日志导出
```bash
# 使用API批量下载构建日志
#!/bin/bash

PROJECT_ID="your-project-id"
PIPELINE_ID="your-pipeline-id"
BUILD_ID="your-build-id"
TOKEN="your-access-token"

# 获取构建详情
curl -H "Authorization: Bearer $TOKEN" \
  "https://devops.woa.com/ms/process/api/user/projects/$PROJECT_ID/pipelines/$PIPELINE_ID/builds/$BUILD_ID/detail" \
  -o build_detail.json

# 下载完整构建日志
curl -H "Authorization: Bearer $TOKEN" \
  "https://devops.woa.com/ms/log/api/user/projects/$PROJECT_ID/pipelines/$PIPELINE_ID/builds/$BUILD_ID/logs/download" \
  -o "build_${BUILD_ID}_logs.txt"

# 下载特定插件日志
ELEMENT_ID="your-element-id"
curl -H "Authorization: Bearer $TOKEN" \
  "https://devops.woa.com/ms/log/api/user/projects/$PROJECT_ID/pipelines/$PIPELINE_ID/builds/$BUILD_ID/logs/$ELEMENT_ID/download" \
  -o "element_${ELEMENT_ID}_logs.txt"
```

### 3. 日志分析和搜索

#### 日志搜索语法
```bash
# 基本搜索
grep "ERROR" build.log

# 时间范围搜索
grep "2024-01-15 10:" build.log

# 正则表达式搜索
grep -E "ERROR|FATAL|Exception" build.log

# 多文件搜索
grep -r "构建失败" logs/

# 统计错误数量
grep -c "ERROR" build.log

# 显示错误前后上下文
grep -A 5 -B 5 "ERROR" build.log
```

#### 结构化日志分析
```python
#!/usr/bin/env python3
import json
import re
from datetime import datetime

def analyze_build_log(log_file):
    """分析构建日志，提取关键信息"""
    
    stats = {
        'total_lines': 0,
        'errors': [],
        'warnings': [],
        'duration': {},
        'plugins': {}
    }
    
    with open(log_file, 'r', encoding='utf-8') as f:
        for line in f:
            stats['total_lines'] += 1
            
            # 提取错误信息
            if 'ERROR' in line or 'FATAL' in line:
                stats['errors'].append({
                    'line': stats['total_lines'],
                    'message': line.strip(),
                    'timestamp': extract_timestamp(line)
                })
            
            # 提取警告信息
            if 'WARNING' in line or 'WARN' in line:
                stats['warnings'].append({
                    'line': stats['total_lines'],
                    'message': line.strip(),
                    'timestamp': extract_timestamp(line)
                })
            
            # 提取插件执行时间
            plugin_match = re.search(r'Plugin (\w+) completed in (\d+)ms', line)
            if plugin_match:
                plugin_name = plugin_match.group(1)
                duration = int(plugin_match.group(2))
                stats['plugins'][plugin_name] = duration
    
    return stats

def extract_timestamp(line):
    """从日志行中提取时间戳"""
    timestamp_pattern = r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}'
    match = re.search(timestamp_pattern, line)
    return match.group(0) if match else None

# 使用示例
if __name__ == "__main__":
    stats = analyze_build_log('build.log')
    
    print(f"总行数: {stats['total_lines']}")
    print(f"错误数: {len(stats['errors'])}")
    print(f"警告数: {len(stats['warnings'])}")
    
    if stats['plugins']:
        print("\n插件执行时间:")
        for plugin, duration in stats['plugins'].items():
            print(f"  {plugin}: {duration}ms")
```

## 流水线监控

### 1. 监控指标配置

#### 基础监控指标
蓝盾提供以下核心监控指标：

```yaml
# 流水线状态监控
pipeline_status_info:
  labels:
    - pipelineId: "流水线ID"
    - buildId: "构建ID" 
    - status: "SUCCEED|FAILED|CANCELED"
    - projectId: "项目ID"
    - triggerUser: "触发用户"
    - pipelineName: "流水线名称"
    - trigger: "TIME_TRIGGER|MANUAL|WEB_HOOK|REMOTE"
    - eventType: "BUILD_END"

# 流水线运行时间监控
pipeline_running_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - projectId: "项目ID"
  value: "运行时间（秒）"

# 流水线排队时间监控
pipeline_queue_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - projectId: "项目ID"
  value: "排队时间（秒）"
```

#### 插件级别监控
```yaml
# 插件状态监控
pipeline_step_status_info:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - step_id: "步骤ID"
    - status: "SUCCEED|FAILED|RUNNING"
    - projectId: "项目ID"

# 插件运行时间监控
pipeline_step_running_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - step_id: "步骤ID"
    - job_id: "Job ID"
    - projectId: "项目ID"
  value: "运行时间（秒）"
```

#### Job级别监控
```yaml
# Job运行时间监控
pipeline_job_running_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - job_id: "Job ID"
    - projectId: "项目ID"
  value: "运行时间（秒）"

# Job排队时间监控
pipeline_job_queue_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - projectId: "项目ID"
    - mutexGroup: "互斥组名称"
    - agentReuseMutex: "构建机互斥ID"
  value: "排队时间（秒）"
```

#### 构建机监控
```yaml
# 构建机运行时间监控
pipeline_agent_running_time_seconds:
  labels:
    - pipeline_id: "流水线ID"
    - build_id: "构建ID"
    - projectId: "项目ID"
    - agentIp: "构建机IP"
    - agentId: "构建机ID"
    - nodeHashId: "节点ID"
    - envHashId: "环境ID"
  value: "运行时间（秒）"
```

### 2. 监控仪表盘配置

#### 创建监控仪表盘
1. 访问[蓝鲸监控平台](https://bkm.woa.com/)
2. 选择对应的研发项目
3. 创建新的仪表盘
4. 添加可视化图表

#### 流水线成功率监控
```yaml
# 仪表盘配置示例
dashboard_config:
  title: "流水线监控仪表盘"
  panels:
    - title: "流水线成功率"
      type: "stat"
      targets:
        - expr: |
            (
              sum(rate(pipeline_status_info{status="SUCCEED",pipelineId="$pipeline_id"}[5m])) /
              sum(rate(pipeline_status_info{eventType="BUILD_END",pipelineId="$pipeline_id"}[5m]))
            ) * 100
      options:
        unit: "percent"
        min: 0
        max: 100
        thresholds:
          - color: "red"
            value: 80
          - color: "yellow" 
            value: 90
          - color: "green"
            value: 95
```

#### 流水线执行时间趋势
```yaml
- title: "流水线执行时间趋势"
  type: "graph"
  targets:
    - expr: |
        max by (pipeline_id, projectId) (
          max_over_time(
            pipeline_running_time_seconds{
              pipeline_id="$pipeline_id",
              projectId="$project_id"
            }[1m]
          )
        )
  options:
    legend:
      show: true
    tooltip:
      shared: true
    yAxis:
      unit: "seconds"
      label: "执行时间"
```

#### 构建机并发监控
```yaml
- title: "构建机并发情况"
  type: "graph"
  targets:
    - expr: |
        count by (agentIp) (
          pipeline_agent_running_time_seconds{
            projectId="$project_id"
          }
        )
  options:
    legend:
      show: true
      values: true
    yAxis:
      label: "并发数"
      min: 0
```

### 3. 告警配置

#### 流水线失败告警
```yaml
alert_rules:
  - name: "流水线执行失败告警"
    expr: |
      increase(pipeline_status_info{
        status="FAILED",
        pipelineId="$pipeline_id"
      }[5m]) > 0
    for: "0m"
    labels:
      severity: "warning"
      team: "dev-team"
    annotations:
      summary: "流水线执行失败"
      description: |
        流水线 {{ $labels.pipelineName }} 执行失败
        项目: {{ $labels.projectId }}
        构建ID: {{ $labels.buildId }}
        触发用户: {{ $labels.triggerUser }}
        
        [查看详情](https://devops.woa.com/console/pipeline/{{ $labels.projectId }}/{{ $labels.pipelineId }}/detail/{{ $labels.buildId }}/executeDetail)
```

#### 执行时间超时告警
```yaml
- name: "流水线执行超时告警"
  expr: |
    pipeline_running_time_seconds{
      pipeline_id="$pipeline_id"
    } > 1800  # 30分钟
  for: "1m"
  labels:
    severity: "warning"
  annotations:
    summary: "流水线执行时间过长"
    description: |
      流水线执行时间超过30分钟
      当前执行时间: {{ $value }}秒
      
      [查看详情](https://devops.woa.com/console/pipeline/{{ $labels.projectId }}/{{ $labels.pipeline_id }}/detail/{{ $labels.build_id }}/executeDetail)
```

#### 插件执行超时告警
```yaml
- name: "插件执行超时告警"
  expr: |
    pipeline_step_running_time_seconds{
      step_id="$step_id",
      pipeline_id="$pipeline_id"
    } > 600  # 10分钟
  for: "30s"
  labels:
    severity: "critical"
  annotations:
    summary: "插件执行超时"
    description: |
      插件 {{ $labels.step_id }} 执行时间超过10分钟
      当前执行时间: {{ $value }}秒
      流水线: {{ $labels.pipeline_id }}
      构建: {{ $labels.build_id }}
```

#### 构建机资源告警
```yaml
- name: "构建机并发过高告警"
  expr: |
    count by (agentIp) (
      pipeline_agent_running_time_seconds{
        projectId="$project_id"
      }
    ) > 5  # 单个构建机并发超过5个
  for: "2m"
  labels:
    severity: "warning"
  annotations:
    summary: "构建机并发过高"
    description: |
      构建机 {{ $labels.agentIp }} 当前并发数: {{ $value }}
      建议检查构建机负载情况
```

## 性能监控

### 1. 系统性能指标

#### 构建性能分析
```python
#!/usr/bin/env python3
import requests
import json
from datetime import datetime, timedelta

class BuildPerformanceAnalyzer:
    def __init__(self, project_id, token):
        self.project_id = project_id
        self.token = token
        self.base_url = "https://devops.woa.com/ms"
        
    def get_build_metrics(self, pipeline_id, days=7):
        """获取流水线性能指标"""
        end_time = datetime.now()
        start_time = end_time - timedelta(days=days)
        
        # 获取构建历史
        builds = self._get_build_history(pipeline_id, start_time, end_time)
        
        metrics = {
            'total_builds': len(builds),
            'success_rate': 0,
            'avg_duration': 0,
            'max_duration': 0,
            'min_duration': float('inf'),
            'failure_reasons': {},
            'performance_trend': []
        }
        
        successful_builds = 0
        total_duration = 0
        
        for build in builds:
            duration = build.get('totalTime', 0) / 1000  # 转换为秒
            
            if build.get('status') == 'SUCCEED':
                successful_builds += 1
                total_duration += duration
                
                metrics['max_duration'] = max(metrics['max_duration'], duration)
                metrics['min_duration'] = min(metrics['min_duration'], duration)
            else:
                # 统计失败原因
                reason = build.get('errorInfo', {}).get('errorMsg', 'Unknown')
                metrics['failure_reasons'][reason] = metrics['failure_reasons'].get(reason, 0) + 1
            
            # 记录性能趋势
            metrics['performance_trend'].append({
                'build_id': build.get('id'),
                'duration': duration,
                'status': build.get('status'),
                'start_time': build.get('startTime')
            })
        
        if successful_builds > 0:
            metrics['success_rate'] = (successful_builds / len(builds)) * 100
            metrics['avg_duration'] = total_duration / successful_builds
        
        if metrics['min_duration'] == float('inf'):
            metrics['min_duration'] = 0
            
        return metrics
    
    def _get_build_history(self, pipeline_id, start_time, end_time):
        """获取构建历史记录"""
        url = f"{self.base_url}/process/api/user/projects/{self.project_id}/pipelines/{pipeline_id}/builds"
        
        params = {
            'page': 1,
            'pageSize': 100,
            'startTimeStartTime': int(start_time.timestamp() * 1000),
            'startTimeEndTime': int(end_time.timestamp() * 1000)
        }
        
        headers = {
            'Authorization': f'Bearer {self.token}',
            'Content-Type': 'application/json'
        }
        
        response = requests.get(url, params=params, headers=headers)
        if response.status_code == 200:
            data = response.json()
            return data.get('data', {}).get('records', [])
        else:
            return []
    
    def generate_report(self, pipeline_id):
        """生成性能报告"""
        metrics = self.get_build_metrics(pipeline_id)
        
        report = f"""
# 流水线性能报告

## 基础指标
- 总构建次数: {metrics['total_builds']}
- 成功率: {metrics['success_rate']:.2f}%
- 平均执行时间: {metrics['avg_duration']:.2f}秒
- 最长执行时间: {metrics['max_duration']:.2f}秒
- 最短执行时间: {metrics['min_duration']:.2f}秒

## 失败原因分析
"""
        
        for reason, count in metrics['failure_reasons'].items():
            report += f"- {reason}: {count}次\n"
        
        return report

# 使用示例
analyzer = BuildPerformanceAnalyzer("your-project-id", "your-token")
report = analyzer.generate_report("your-pipeline-id")
print(report)
```

### 2. 资源使用监控

#### 构建机资源监控
```yaml
# 构建机CPU使用率监控
- name: "构建机CPU监控"
  expr: |
    avg by (agentIp) (
      rate(node_cpu_seconds_total{
        mode!="idle",
        instance=~"$agent_ip:.*"
      }[5m])
    ) * 100
  alert:
    condition: "> 80"
    duration: "5m"
    message: "构建机CPU使用率过高"

# 构建机内存使用率监控
- name: "构建机内存监控"
  expr: |
    (1 - (
      node_memory_MemAvailable_bytes{instance=~"$agent_ip:.*"} /
      node_memory_MemTotal_bytes{instance=~"$agent_ip:.*"}
    )) * 100
  alert:
    condition: "> 85"
    duration: "3m"
    message: "构建机内存使用率过高"

# 构建机磁盘使用率监控
- name: "构建机磁盘监控"
  expr: |
    (1 - (
      node_filesystem_avail_bytes{
        instance=~"$agent_ip:.*",
        fstype!="tmpfs"
      } /
      node_filesystem_size_bytes{
        instance=~"$agent_ip:.*",
        fstype!="tmpfs"
      }
    )) * 100
  alert:
    condition: "> 90"
    duration: "1m"
    message: "构建机磁盘空间不足"
```

### 3. 网络和依赖监控

#### 外部依赖监控
```yaml
# 代码库连接监控
- name: "代码库连接监控"
  expr: |
    probe_success{
      job="git-repo-probe",
      instance=~"git.code.oa.com.*"
    }
  alert:
    condition: "== 0"
    duration: "1m"
    message: "代码库连接失败"

# 制品库连接监控
- name: "制品库连接监控"
  expr: |
    probe_success{
      job="artifact-repo-probe",
      instance=~"bkrepo.woa.com.*"
    }
  alert:
    condition: "== 0"
    duration: "2m"
    message: "制品库连接失败"

# 外部API响应时间监控
- name: "外部API响应时间"
  expr: |
    probe_duration_seconds{
      job="external-api-probe"
    }
  alert:
    condition: "> 5"
    duration: "2m"
    message: "外部API响应时间过长"
```

## 日志聚合和分析

### 1. 日志收集配置

#### 结构化日志输出
```bash
#!/bin/bash

# 日志函数库
log_info() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $1" | tee -a build.log
}

log_warn() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [WARN] $1" | tee -a build.log
}

log_error() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $1" | tee -a build.log
}

log_debug() {
    if [[ "$LOG_LEVEL" == "DEBUG" ]]; then
        echo "$(date '+%Y-%m-%d %H:%M:%S') [DEBUG] $1" | tee -a build.log
    fi
}

# JSON格式日志
log_json() {
    local level=$1
    local message=$2
    local extra=${3:-"{}"}
    
    jq -n \
        --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)" \
        --arg level "$level" \
        --arg message "$message" \
        --argjson extra "$extra" \
        '{
            timestamp: $timestamp,
            level: $level,
            message: $message,
            pipeline_id: env.PIPELINE_ID,
            build_id: env.BUILD_ID,
            job_id: env.JOB_ID,
            extra: $extra
        }' >> structured.log
}

# 使用示例
log_info "开始构建流程"
log_json "INFO" "开始编译" '{"component": "compiler", "language": "java"}'
```

### 2. 日志分析工具

#### ELK Stack集成
```yaml
# Logstash配置示例
input {
  file {
    path => "/var/log/bkci/builds/*.log"
    start_position => "beginning"
    codec => "json"
  }
}

filter {
  if [level] {
    mutate {
      add_field => { "log_level" => "%{level}" }
    }
  }
  
  if [pipeline_id] {
    mutate {
      add_field => { "pipeline" => "%{pipeline_id}" }
    }
  }
  
  # 解析错误堆栈
  if [level] == "ERROR" and [message] =~ /Exception/ {
    grok {
      match => { 
        "message" => "(?<exception_type>\w+Exception): (?<exception_message>.*)"
      }
    }
  }
  
  # 添加时间戳
  date {
    match => [ "timestamp", "ISO8601" ]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "bkci-logs-%{+YYYY.MM.dd}"
  }
}
```

#### 日志搜索和分析
```bash
# Elasticsearch查询示例

# 查询特定流水线的错误日志
curl -X GET "elasticsearch:9200/bkci-logs-*/_search" -H 'Content-Type: application/json' -d'
{
  "query": {
    "bool": {
      "must": [
        {"term": {"pipeline_id": "your-pipeline-id"}},
        {"term": {"level": "ERROR"}},
        {"range": {"@timestamp": {"gte": "now-1d"}}}
      ]
    }
  },
  "sort": [{"@timestamp": {"order": "desc"}}],
  "size": 100
}'

# 聚合分析错误类型
curl -X GET "elasticsearch:9200/bkci-logs-*/_search" -H 'Content-Type: application/json' -d'
{
  "size": 0,
  "aggs": {
    "error_types": {
      "terms": {
        "field": "exception_type.keyword",
        "size": 10
      }
    },
    "error_timeline": {
      "date_histogram": {
        "field": "@timestamp",
        "interval": "1h"
      },
      "aggs": {
        "error_count": {
          "filter": {"term": {"level": "ERROR"}}
        }
      }
    }
  }
}'
```

### 3. 智能日志分析

#### 异常检测
```python
#!/usr/bin/env python3
import re
import json
from collections import defaultdict, Counter
from datetime import datetime, timedelta

class LogAnalyzer:
    def __init__(self):
        self.error_patterns = [
            r'Exception in thread',
            r'java\.lang\.\w*Exception',
            r'Error: (.+)',
            r'FATAL: (.+)',
            r'Failed to (.+)',
            r'Connection refused',
            r'Timeout',
            r'Out of memory'
        ]
        
        self.warning_patterns = [
            r'Warning: (.+)',
            r'WARN: (.+)',
            r'Deprecated',
            r'Retry attempt'
        ]
    
    def analyze_log_file(self, log_file):
        """分析日志文件，提取异常模式"""
        
        analysis = {
            'summary': {
                'total_lines': 0,
                'error_count': 0,
                'warning_count': 0,
                'unique_errors': set(),
                'error_timeline': defaultdict(int)
            },
            'errors': [],
            'warnings': [],
            'patterns': {
                'frequent_errors': Counter(),
                'error_sequences': [],
                'performance_issues': []
            }
        }
        
        with open(log_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        for i, line in enumerate(lines):
            analysis['summary']['total_lines'] += 1
            timestamp = self._extract_timestamp(line)
            
            # 检测错误
            for pattern in self.error_patterns:
                match = re.search(pattern, line, re.IGNORECASE)
                if match:
                    error_info = {
                        'line_number': i + 1,
                        'timestamp': timestamp,
                        'pattern': pattern,
                        'message': line.strip(),
                        'context': self._get_context(lines, i)
                    }
                    
                    analysis['errors'].append(error_info)
                    analysis['summary']['error_count'] += 1
                    analysis['summary']['unique_errors'].add(match.group(0))
                    analysis['patterns']['frequent_errors'][match.group(0)] += 1
                    
                    if timestamp:
                        hour_key = timestamp.strftime('%Y-%m-%d %H:00')
                        analysis['summary']['error_timeline'][hour_key] += 1
                    break
            
            # 检测警告
            for pattern in self.warning_patterns:
                match = re.search(pattern, line, re.IGNORECASE)
                if match:
                    warning_info = {
                        'line_number': i + 1,
                        'timestamp': timestamp,
                        'message': line.strip()
                    }
                    analysis['warnings'].append(warning_info)
                    analysis['summary']['warning_count'] += 1
                    break
            
            # 检测性能问题
            if 'took' in line.lower() or 'duration' in line.lower():
                duration_match = re.search(r'(\d+(?:\.\d+)?)\s*(ms|seconds?|minutes?)', line, re.IGNORECASE)
                if duration_match:
                    duration = float(duration_match.group(1))
                    unit = duration_match.group(2).lower()
                    
                    # 转换为毫秒
                    if 'second' in unit:
                        duration *= 1000
                    elif 'minute' in unit:
                        duration *= 60000
                    
                    if duration > 5000:  # 超过5秒认为是性能问题
                        analysis['patterns']['performance_issues'].append({
                            'line_number': i + 1,
                            'duration': duration,
                            'message': line.strip()
                        })
        
        # 检测错误序列
        analysis['patterns']['error_sequences'] = self._detect_error_sequences(analysis['errors'])
        
        return analysis
    
    def _extract_timestamp(self, line):
        """从日志行中提取时间戳"""
        patterns = [
            r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}',
            r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}',
            r'\w{3} \d{2} \d{2}:\d{2}:\d{2}'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, line)
            if match:
                try:
                    return datetime.strptime(match.group(0), '%Y-%m-%d %H:%M:%S')
                except:
                    continue
        return None
    
    def _get_context(self, lines, index, context_size=3):
        """获取错误行的上下文"""
        start = max(0, index - context_size)
        end = min(len(lines), index + context_size + 1)
        
        context = []
        for i in range(start, end):
            prefix = ">>> " if i == index else "    "
            context.append(f"{prefix}{i+1}: {lines[i].rstrip()}")
        
        return "\n".join(context)
    
    def _detect_error_sequences(self, errors):
        """检测错误序列模式"""
        sequences = []
        
        if len(errors) < 2:
            return sequences
        
        for i in range(len(errors) - 1):
            current_error = errors[i]
            next_error = errors[i + 1]
            
            if (current_error['timestamp'] and next_error['timestamp'] and
                (next_error['timestamp'] - current_error['timestamp']).seconds < 60):
                
                sequences.append({
                    'start_line': current_error['line_number'],
                    'end_line': next_error['line_number'],
                    'duration': (next_error['timestamp'] - current_error['timestamp']).seconds,
                    'error_count': 2
                })
        
        return sequences
    
    def generate_report(self, analysis):
        """生成分析报告"""
        report = f"""
# 日志分析报告

## 概览
- 总行数: {analysis['summary']['total_lines']:,}
- 错误数: {analysis['summary']['error_count']:,}
- 警告数: {analysis['summary']['warning_count']:,}
- 唯一错误类型: {len(analysis['summary']['unique_errors'])}

## 高频错误
"""
        
        for error, count in analysis['patterns']['frequent_errors'].most_common(10):
            report += f"- {error}: {count}次\n"
        
        if analysis['patterns']['performance_issues']:
            report += f"\n## 性能问题 ({len(analysis['patterns']['performance_issues'])}个)\n"
            for issue in analysis['patterns']['performance_issues'][:5]:
                report += f"- 行{issue['line_number']}: {issue['duration']:.0f}ms\n"
        
        if analysis['patterns']['error_sequences']:
            report += f"\n## 错误序列 ({len(analysis['patterns']['error_sequences'])}个)\n"
            for seq in analysis['patterns']['error_sequences'][:5]:
                report += f"- 行{seq['start_line']}-{seq['end_line']}: {seq['duration']}秒内连续错误\n"
        
        return report

# 使用示例
analyzer = LogAnalyzer()
analysis = analyzer.analyze_log_file('build.log')
report = analyzer.generate_report(analysis)
print(report)
```

## 故障排查

### 1. 常见问题诊断

#### 构建失败诊断
```bash
#!/bin/bash

# 构建失败诊断脚本
diagnose_build_failure() {
    local build_log=$1
    local project_id=$2
    local pipeline_id=$3
    local build_id=$4
    
    echo "=== 构建失败诊断报告 ==="
    echo "项目ID: $project_id"
    echo "流水线ID: $pipeline_id"
    echo "构建ID: $build_id"
    echo "日志文件: $build_log"
    echo ""
    
    # 检查常见错误模式
    echo "=== 错误模式分析 ==="
    
    # 编译错误
    if grep -q "compilation failed\|compile error\|build failed" "$build_log"; then
        echo "❌ 发现编译错误"
        grep -n -A 3 -B 1 "compilation failed\|compile error\|build failed" "$build_log" | head -20
        echo ""
    fi
    
    # 测试失败
    if grep -q "test failed\|tests failed\|assertion failed" "$build_log"; then
        echo "❌ 发现测试失败"
        grep -n -A 3 -B 1 "test failed\|tests failed\|assertion failed" "$build_log" | head -20
        echo ""
    fi
    
    # 网络问题
    if grep -q "connection refused\|timeout\|network error\|dns resolution failed" "$build_log"; then
        echo "🌐 发现网络问题"
        grep -n -A 2 -B 1 "connection refused\|timeout\|network error\|dns resolution failed" "$build_log" | head -10
        echo ""
    fi
    
    # 权限问题
    if grep -q "permission denied\|access denied\|unauthorized" "$build_log"; then
        echo "🔒 发现权限问题"
        grep -n -A 2 -B 1 "permission denied\|access denied\|unauthorized" "$build_log" | head -10
        echo ""
    fi
    
    # 资源不足
    if grep -q "out of memory\|disk space\|no space left" "$build_log"; then
        echo "💾 发现资源不足问题"
        grep -n -A 2 -B 1 "out of memory\|disk space\|no space left" "$build_log" | head -10
        echo ""
    fi
    
    # 依赖问题
    if grep -q "dependency not found\|module not found\|package not found" "$build_log"; then
        echo "📦 发现依赖问题"
        grep -n -A 2 -B 1 "dependency not found\|module not found\|package not found" "$build_log" | head -10
        echo ""
    fi
    
    # 统计信息
    echo "=== 统计信息 ==="
    echo "总行数: $(wc -l < "$build_log")"
    echo "错误行数: $(grep -c -i "error\|failed\|exception" "$build_log")"
    echo "警告行数: $(grep -c -i "warning\|warn" "$build_log")"
    echo ""
    
    # 建议
    echo "=== 排查建议 ==="
    if grep -q "compilation failed" "$build_log"; then
        echo "1. 检查代码语法错误"
        echo "2. 确认依赖版本兼容性"
        echo "3. 检查编译环境配置"
    fi
    
    if grep -q "test failed" "$build_log"; then
        echo "1. 运行本地测试确认问题"
        echo "2. 检查测试数据和环境"
        echo "3. 确认测试用例的正确性"
    fi
    
    if grep -q "timeout\|network error" "$build_log"; then
        echo "1. 检查网络连接"
        echo "2. 确认防火墙设置"
        echo "3. 考虑增加超时时间"
    fi
}

# 使用示例
diagnose_build_failure "build.log" "project-123" "pipeline-456" "build-789"
```

### 2. 性能问题排查

#### 构建性能分析
```python
#!/usr/bin/env python3
import re
import json
from datetime import datetime
import matplotlib.pyplot as plt
import pandas as pd

class BuildPerformanceDiagnostic:
    def __init__(self, log_file):
        self.log_file = log_file
        self.timeline = []
        self.plugins = {}
        self.bottlenecks = []
    
    def analyze_performance(self):
        """分析构建性能"""
        
        with open(self.log_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        current_plugin = None
        plugin_start_time = None
        
        for line in lines:
            timestamp = self._extract_timestamp(line)
            if not timestamp:
                continue
            
            # 检测插件开始
            plugin_start_match = re.search(r'Starting plugin: (\w+)', line)
            if plugin_start_match:
                current_plugin = plugin_start_match.group(1)
                plugin_start_time = timestamp
                continue
            
            # 检测插件结束
            plugin_end_match = re.search(r'Plugin (\w+) completed in (\d+)ms', line)
            if plugin_end_match:
                plugin_name = plugin_end_match.group(1)
                duration = int(plugin_end_match.group(2))
                
                if plugin_name not in self.plugins:
                    self.plugins[plugin_name] = []
                
                self.plugins[plugin_name].append({
                    'duration': duration,
                    'timestamp': timestamp
                })
                
                # 记录到时间线
                self.timeline.append({
                    'timestamp': timestamp,
                    'plugin': plugin_name,
                    'duration': duration,
                    'type': 'plugin_completion'
                })
                
                # 检测性能瓶颈
                if duration > 60000:  # 超过1分钟
                    self.bottlenecks.append({
                        'plugin': plugin_name,
                        'duration': duration,
                        'timestamp': timestamp,
                        'severity': 'high' if duration > 300000 else 'medium'
                    })
        
        return self._generate_performance_report()
    
    def _extract_timestamp(self, line):
        """提取时间戳"""
        match = re.search(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}', line)
        if match:
            try:
                return datetime.strptime(match.group(0), '%Y-%m-%d %H:%M:%S')
            except:
                pass
        return None
    
    def _generate_performance_report(self):
        """生成性能报告"""
        
        report = {
            'summary': {
                'total_plugins': len(self.plugins),
                'total_bottlenecks': len(self.bottlenecks),
                'slowest_plugins': [],
                'performance_score': 0
            },
            'plugin_analysis': {},
            'bottlenecks': self.bottlenecks,
            'recommendations': []
        }
        
        # 分析每个插件
        plugin_stats = []
        for plugin_name, executions in self.plugins.items():
            durations = [exec['duration'] for exec in executions]
            
            stats = {
                'name': plugin_name,
                'executions': len(executions),
                'avg_duration': sum(durations) / len(durations),
                'max_duration': max(durations),
                'min_duration': min(durations),
                'total_time': sum(durations)
            }
            
            plugin_stats.append(stats)
            report['plugin_analysis'][plugin_name] = stats
        
        # 排序找出最慢的插件
        plugin_stats.sort(key=lambda x: x['avg_duration'], reverse=True)
        report['summary']['slowest_plugins'] = plugin_stats[:5]
        
        # 计算性能评分
        total_time = sum(stats['total_time'] for stats in plugin_stats)
        if total_time > 0:
            bottleneck_time = sum(b['duration'] for b in self.bottlenecks)
            report['summary']['performance_score'] = max(0, 100 - (bottleneck_time / total_time * 100))
        
        # 生成建议
        report['recommendations'] = self._generate_recommendations(plugin_stats)
        
        return report
    
    def _generate_recommendations(self, plugin_stats):
        """生成优化建议"""
        recommendations = []
        
        # 检查慢插件
        for stats in plugin_stats[:3]:
            if stats['avg_duration'] > 120000:  # 超过2分钟
                recommendations.append({
                    'type': 'slow_plugin',
                    'plugin': stats['name'],
                    'message': f"插件 {stats['name']} 平均执行时间 {stats['avg_duration']/1000:.1f}秒，建议优化",
                    'suggestions': [
                        "检查插件配置是否合理",
                        "考虑并行执行或缓存优化",
                        "检查网络依赖和资源访问"
                    ]
                })
        
        # 检查重复执行
        for stats in plugin_stats:
            if stats['executions'] > 3:
                recommendations.append({
                    'type': 'repeated_execution',
                    'plugin': stats['name'],
                    'message': f"插件 {stats['name']} 执行了 {stats['executions']} 次，可能存在重复",
                    'suggestions': [
                        "检查流水线配置是否有重复步骤",
                        "考虑合并相似的插件执行",
                        "使用条件执行避免不必要的重复"
                    ]
                })
        
        return recommendations
    
    def visualize_performance(self, output_file='performance_analysis.png'):
        """可视化性能分析结果"""
        if not self.plugins:
            return
        
        # 创建图表
        fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(12, 10))
        
        # 插件执行时间分布
        plugin_names = list(self.plugins.keys())
        avg_durations = []
        
        for plugin in plugin_names:
            durations = [exec['duration'] for exec in self.plugins[plugin]]
            avg_durations.append(sum(durations) / len(durations) / 1000)  # 转换为秒
        
        ax1.bar(plugin_names, avg_durations)
        ax1.set_title('插件平均执行时间')
        ax1.set_ylabel('时间 (秒)')
        ax1.tick_params(axis='x', rotation=45)
        
        # 性能瓶颈时间线
        if self.bottlenecks:
            bottleneck_times = [b['timestamp'] for b in self.bottlenecks]
            bottleneck_durations = [b['duration'] / 1000 for b in self.bottlenecks]
            
            ax2.scatter(bottleneck_times, bottleneck_durations, c='red', alpha=0.7)
            ax2.set_title('性能瓶颈时间线')
            ax2.set_ylabel('执行时间 (秒)')
            ax2.set_xlabel('时间')
        
        plt.tight_layout()
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        plt.close()
        
        return output_file

# 使用示例
diagnostic = BuildPerformanceDiagnostic('build.log')
report = diagnostic.analyze_performance()

print("=== 构建性能分析报告 ===")
print(f"总插件数: {report['summary']['total_plugins']}")
print(f"性能瓶颈数: {report['summary']['total_bottlenecks']}")
print(f"性能评分: {report['summary']['performance_score']:.1f}/100")

print("\n=== 最慢的插件 ===")
for plugin in report['summary']['slowest_plugins']:
    print(f"{plugin['name']}: {plugin['avg_duration']/1000:.1f}秒")

print("\n=== 优化建议 ===")
for rec in report['recommendations']:
    print(f"- {rec['message']}")
```

## 最佳实践

### 1. 监控策略设计

#### 分层监控体系
```yaml
monitoring_strategy:
  # 基础设施层
  infrastructure:
    metrics:
      - cpu_usage
      - memory_usage
      - disk_usage
      - network_io
    alerts:
      - threshold: 80%
        severity: warning
      - threshold: 95%
        severity: critical
  
  # 应用层
  application:
    metrics:
      - pipeline_success_rate
      - build_duration
      - queue_time
      - plugin_performance
    alerts:
      - success_rate < 90%
      - build_duration > 30min
      - queue_time > 10min
  
  # 业务层
  business:
    metrics:
      - deployment_frequency
      - lead_time
      - mttr
      - change_failure_rate
    alerts:
      - deployment_frequency < daily
      - lead_time > 1day
      - change_failure_rate > 15%
```

### 2. 日志管理规范

#### 日志保留策略
```yaml
log_retention_policy:
  # 构建日志
  build_logs:
    retention: "60 days"
    compression: true
    archive_location: "s3://logs-archive/builds/"
  
  # 系统日志
  system_logs:
    retention: "30 days"
    rotation: "daily"
    max_size: "100MB"
  
  # 审计日志
  audit_logs:
    retention: "1 year"
    encryption: true
    immutable: true
  
  # 错误日志
  error_logs:
    retention: "90 days"
    priority: "high"
    alerting: true
```

### 3. 告警管理

#### 告警分级和处理
```yaml
alert_management:
  # 告警级别
  levels:
    critical:
      response_time: "5 minutes"
      escalation: "immediate"
      channels: ["phone", "sms", "email"]
    
    warning:
      response_time: "30 minutes"
      escalation: "1 hour"
      channels: ["email", "wework"]
    
    info:
      response_time: "2 hours"
      escalation: "none"
      channels: ["email"]
  
  # 告警抑制
  suppression:
    - name: "maintenance_window"
      schedule: "0 2 * * 0"  # 每周日凌晨2点
      duration: "4 hours"
    
    - name: "known_issues"
      conditions:
        - alert_name: "disk_space_low"
          duration: "1 hour"
  
  # 告警聚合
  aggregation:
    - name: "build_failures"
      group_by: ["pipeline_id"]
      group_wait: "30s"
      group_interval: "5m"
      repeat_interval: "1h"
```

## 总结

监控和日志管理是保障CI/CD系统稳定运行的重要手段：

1. **全面监控**: 从基础设施到业务指标的多层次监控
2. **智能告警**: 基于阈值和趋势的智能告警机制
3. **高效日志**: 结构化日志和智能分析工具
4. **快速排查**: 完善的故障诊断和性能分析工具

建议根据团队规模和业务需求，建立适合的监控和日志管理体系，并持续优化监控策略和告警规则。