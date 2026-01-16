# 故障排查与问题解决指南

## 概述

本章提供了蓝盾平台常见问题的系统性排查方法和解决方案。通过分类整理各种故障场景，帮助用户快速定位问题根因并找到相应的解决方案。

## 插件执行故障排查

### 1. 插件错误码分类

#### 标准错误码体系
蓝盾插件遵循统一的错误码规范，通过错误码可以快速判断问题类型：

| 错误码 | 分类 | 描述 | 责任方 |
|--------|------|------|--------|
| 2199001 | 插件默认异常 | 插件未按规范返回错误码 | 插件开发者 |
| 2199002 | 用户配置有误 | 插件配置参数错误或不准确 | 用户 |
| 2199003 | 插件依赖异常 | 插件执行环境或依赖工具异常 | 插件开发者/环境 |
| 2199004 | 用户任务执行失败 | 业务逻辑执行失败 | 用户/业务逻辑 |
| 2199005 | 用户任务执行超时 | 用户设置的超时时间导致失败 | 用户 |
| 2199006 | 插件执行超时 | 插件自身执行超时 | 插件开发者 |
| 2199007 | 触碰质量红线 | 质量检查未通过 | 用户/代码质量 |
| 2199008 | 质量红线审核驳回 | 人工审核拒绝 | 审核人员 |
| 2199009 | 脚本命令执行异常 | 脚本无法正常执行 | 用户/脚本 |
| 2199010 | Stage FastKill | 其他插件失败导致提前终止 | 流水线配置 |

### 2. 插件故障排查流程

#### 第三方插件排查步骤
```bash
#!/bin/bash

# 插件故障排查脚本
troubleshoot_plugin_failure() {
    local build_log=$1
    local plugin_name=$2
    local error_code=$3
    
    echo "=== 插件故障排查 ==="
    echo "插件名称: $plugin_name"
    echo "错误码: $error_code"
    echo ""
    
    # Step 1: 分析错误码
    case $error_code in
        "2199002")
            echo "❌ 用户配置问题"
            echo "建议:"
            echo "1. 检查插件配置参数是否正确"
            echo "2. 查看插件文档确认必填参数"
            echo "3. 验证参数格式和取值范围"
            ;;
        "2199003")
            echo "❌ 插件依赖问题"
            echo "建议:"
            echo "1. 检查构建环境是否满足插件要求"
            echo "2. 确认依赖工具是否正确安装"
            echo "3. 验证网络连接和权限设置"
            ;;
        "2199004")
            echo "❌ 业务执行失败"
            echo "建议:"
            echo "1. 检查业务逻辑和数据"
            echo "2. 查看详细错误信息"
            echo "3. 在本地环境复现问题"
            ;;
        "2199005"|"2199006")
            echo "⏰ 执行超时"
            echo "建议:"
            echo "1. 增加超时时间设置"
            echo "2. 优化执行逻辑提高效率"
            echo "3. 检查是否有死循环或阻塞"
            ;;
        *)
            echo "❓ 其他错误"
            echo "建议联系插件作者或技术支持"
            ;;
    esac
    
    # Step 2: 提取关键错误信息
    echo ""
    echo "=== 关键错误信息 ==="
    grep -A 5 -B 5 "ERROR\|FAILED\|Exception" "$build_log" | head -20
    
    # Step 3: 检查插件配置
    echo ""
    echo "=== 配置检查建议 ==="
    echo "1. 在流水线编辑页面查看插件配置"
    echo "2. 点击插件的'了解更多'查看文档"
    echo "3. 对比成功案例的配置差异"
    
    # Step 4: 联系方式
    echo ""
    echo "=== 获取帮助 ==="
    echo "1. 查看插件详情页的作者信息"
    echo "2. 在插件下方评论区反馈问题"
    echo "3. 联系流水线负责人协助"
}

# 使用示例
troubleshoot_plugin_failure "build.log" "CodeccCheckAtom" "2199002"
```

#### 官方插件排查步骤
```yaml
# 官方插件故障排查清单
official_plugin_troubleshooting:
  identification:
    # 识别官方插件
    - 插件标识为"DevOps平台组"
    - 插件来源为蓝盾官方
  
  common_issues:
    bash_plugin:
      - 脚本语法错误
      - 权限不足
      - 环境变量缺失
      - 依赖工具未安装
    
    git_plugin:
      - 代码库权限问题
      - 网络连接异常
      - 凭证配置错误
      - 分支或标签不存在
    
    docker_plugin:
      - Docker服务未启动
      - 镜像拉取失败
      - 构建上下文问题
      - 资源不足
  
  escalation:
    - 联系O2000技术支持
    - 提供详细错误日志
    - 描述复现步骤
```

### 3. Bash插件特殊排查

#### 脚本执行失败诊断
```bash
#!/bin/bash

# Bash插件故障诊断
diagnose_bash_failure() {
    local script_path=$1
    local exit_code=$2
    
    echo "=== Bash插件故障诊断 ==="
    echo "脚本路径: $script_path"
    echo "退出码: $exit_code"
    echo ""
    
    # 检查脚本语法
    echo "=== 语法检查 ==="
    if bash -n "$script_path" 2>/dev/null; then
        echo "✅ 脚本语法正确"
    else
        echo "❌ 脚本语法错误:"
        bash -n "$script_path"
        return 1
    fi
    
    # 检查权限
    echo ""
    echo "=== 权限检查 ==="
    if [[ -r "$script_path" ]]; then
        echo "✅ 脚本可读"
    else
        echo "❌ 脚本不可读"
    fi
    
    if [[ -x "$script_path" ]]; then
        echo "✅ 脚本可执行"
    else
        echo "❌ 脚本不可执行"
        echo "建议: chmod +x $script_path"
    fi
    
    # 检查依赖命令
    echo ""
    echo "=== 依赖检查 ==="
    local commands=$(grep -o '\b[a-zA-Z_][a-zA-Z0-9_]*\b' "$script_path" | sort -u)
    for cmd in $commands; do
        if command -v "$cmd" >/dev/null 2>&1; then
            echo "✅ $cmd: 可用"
        else
            echo "❌ $cmd: 不可用"
        fi
    done
    
    # 分析退出码
    echo ""
    echo "=== 退出码分析 ==="
    case $exit_code in
        0)
            echo "✅ 正常退出"
            ;;
        1)
            echo "❌ 一般错误"
            ;;
        2)
            echo "❌ 误用shell命令"
            ;;
        126)
            echo "❌ 命令不可执行"
            ;;
        127)
            echo "❌ 命令未找到"
            ;;
        128)
            echo "❌ 无效的退出参数"
            ;;
        130)
            echo "❌ 脚本被Ctrl+C终止"
            ;;
        *)
            echo "❓ 其他错误码: $exit_code"
            ;;
    esac
}

# 调试模式脚本生成
generate_debug_script() {
    local original_script=$1
    local debug_script="${original_script}.debug"
    
    cat > "$debug_script" << 'EOF'
#!/bin/bash

# 启用调试模式
set -x  # 显示执行的命令
set -e  # 遇到错误立即退出
set -u  # 使用未定义变量时报错
set -o pipefail  # 管道中任何命令失败都会导致整个管道失败

# 输出环境信息
echo "=== 环境信息 ==="
echo "当前用户: $(whoami)"
echo "当前目录: $(pwd)"
echo "PATH: $PATH"
echo "环境变量:"
env | sort
echo ""

# 输出系统信息
echo "=== 系统信息 ==="
uname -a
df -h
free -h
echo ""

EOF
    
    # 添加原始脚本内容（跳过shebang）
    tail -n +2 "$original_script" >> "$debug_script"
    
    chmod +x "$debug_script"
    echo "调试脚本已生成: $debug_script"
}

# 使用示例
diagnose_bash_failure "build.sh" 1
generate_debug_script "build.sh"
```

## 构建环境故障排查

### 1. Agent连接问题

#### Agent状态诊断
```bash
#!/bin/bash

# Agent状态诊断脚本
diagnose_agent_status() {
    local agent_dir=$1
    
    echo "=== Agent状态诊断 ==="
    echo "Agent目录: $agent_dir"
    echo ""
    
    # 检查进程状态
    echo "=== 进程检查 ==="
    if pgrep -f "devopsDaemon" >/dev/null; then
        echo "✅ devopsDaemon进程运行中"
        echo "PID: $(pgrep -f devopsDaemon)"
    else
        echo "❌ devopsDaemon进程未运行"
    fi
    
    if pgrep -f "devopsAgent" >/dev/null; then
        echo "✅ devopsAgent进程运行中"
        echo "PID: $(pgrep -f devopsAgent)"
    else
        echo "❌ devopsAgent进程未运行"
    fi
    
    # 检查日志
    echo ""
    echo "=== 日志检查 ==="
    local log_file="$agent_dir/logs/devopsAgent.log"
    if [[ -f "$log_file" ]]; then
        echo "最近的日志条目:"
        tail -10 "$log_file"
        
        # 检查常见错误
        echo ""
        echo "=== 错误检查 ==="
        if grep -q "network\|timeout\|connection" "$log_file"; then
            echo "❌ 发现网络相关错误"
            grep -n "network\|timeout\|connection" "$log_file" | tail -5
        fi
        
        if grep -q "killed\|terminated" "$log_file"; then
            echo "❌ 发现进程被终止"
            grep -n "killed\|terminated" "$log_file" | tail -5
        fi
    else
        echo "❌ 日志文件不存在: $log_file"
    fi
    
    # 检查网络连接
    echo ""
    echo "=== 网络检查 ==="
    local server_url="https://devops.woa.com"
    if curl -s --connect-timeout 5 "$server_url" >/dev/null; then
        echo "✅ 网络连接正常"
    else
        echo "❌ 网络连接异常"
        echo "建议检查代理设置和防火墙"
    fi
    
    # 检查磁盘空间
    echo ""
    echo "=== 磁盘空间检查 ==="
    local disk_usage=$(df "$agent_dir" | awk 'NR==2 {print $5}' | sed 's/%//')
    if [[ $disk_usage -lt 90 ]]; then
        echo "✅ 磁盘空间充足 ($disk_usage%)"
    else
        echo "❌ 磁盘空间不足 ($disk_usage%)"
    fi
    
    # 生成修复建议
    echo ""
    echo "=== 修复建议 ==="
    if ! pgrep -f "devops" >/dev/null; then
        echo "1. 启动Agent服务:"
        echo "   cd $agent_dir && ./start.sh"
    fi
    
    echo "2. 检查网络和代理配置"
    echo "3. 确认IOA登录状态"
    echo "4. 检查防火墙和安全软件"
    echo "5. 如问题持续，联系O2000技术支持"
}

# Agent重启脚本
restart_agent() {
    local agent_dir=$1
    
    echo "=== 重启Agent ==="
    
    # 停止服务
    echo "停止Agent服务..."
    cd "$agent_dir"
    if [[ -f "stop.sh" ]]; then
        ./stop.sh
    else
        pkill -f "devops"
    fi
    
    # 等待进程完全停止
    sleep 5
    
    # 启动服务
    echo "启动Agent服务..."
    ./start.sh
    
    # 验证启动状态
    sleep 10
    if pgrep -f "devops" >/dev/null; then
        echo "✅ Agent启动成功"
    else
        echo "❌ Agent启动失败"
        echo "请检查日志: $agent_dir/logs/devopsAgent.log"
    fi
}

# 使用示例
diagnose_agent_status "/path/to/agent"
```

### 2. 构建机资源问题

#### 资源监控脚本
```python
#!/usr/bin/env python3
import psutil
import shutil
import time
import json
from datetime import datetime

class ResourceMonitor:
    def __init__(self):
        self.thresholds = {
            'cpu_percent': 80,
            'memory_percent': 85,
            'disk_percent': 90,
            'load_average': psutil.cpu_count() * 0.8
        }
    
    def check_system_resources(self):
        """检查系统资源使用情况"""
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'cpu': self._check_cpu(),
            'memory': self._check_memory(),
            'disk': self._check_disk(),
            'network': self._check_network(),
            'processes': self._check_processes(),
            'recommendations': []
        }
        
        # 生成建议
        report['recommendations'] = self._generate_recommendations(report)
        
        return report
    
    def _check_cpu(self):
        """检查CPU使用情况"""
        cpu_percent = psutil.cpu_percent(interval=1)
        cpu_count = psutil.cpu_count()
        load_avg = psutil.getloadavg() if hasattr(psutil, 'getloadavg') else [0, 0, 0]
        
        return {
            'usage_percent': cpu_percent,
            'cpu_count': cpu_count,
            'load_average': load_avg,
            'status': 'critical' if cpu_percent > self.thresholds['cpu_percent'] else 'normal'
        }
    
    def _check_memory(self):
        """检查内存使用情况"""
        memory = psutil.virtual_memory()
        swap = psutil.swap_memory()
        
        return {
            'total_gb': round(memory.total / (1024**3), 2),
            'used_gb': round(memory.used / (1024**3), 2),
            'available_gb': round(memory.available / (1024**3), 2),
            'usage_percent': memory.percent,
            'swap_usage_percent': swap.percent,
            'status': 'critical' if memory.percent > self.thresholds['memory_percent'] else 'normal'
        }
    
    def _check_disk(self):
        """检查磁盘使用情况"""
        disk_info = []
        
        for partition in psutil.disk_partitions():
            try:
                usage = psutil.disk_usage(partition.mountpoint)
                disk_info.append({
                    'device': partition.device,
                    'mountpoint': partition.mountpoint,
                    'fstype': partition.fstype,
                    'total_gb': round(usage.total / (1024**3), 2),
                    'used_gb': round(usage.used / (1024**3), 2),
                    'free_gb': round(usage.free / (1024**3), 2),
                    'usage_percent': round((usage.used / usage.total) * 100, 2),
                    'status': 'critical' if (usage.used / usage.total) * 100 > self.thresholds['disk_percent'] else 'normal'
                })
            except PermissionError:
                continue
        
        return disk_info
    
    def _check_network(self):
        """检查网络使用情况"""
        net_io = psutil.net_io_counters()
        
        return {
            'bytes_sent': net_io.bytes_sent,
            'bytes_recv': net_io.bytes_recv,
            'packets_sent': net_io.packets_sent,
            'packets_recv': net_io.packets_recv,
            'errors_in': net_io.errin,
            'errors_out': net_io.errout,
            'drops_in': net_io.dropin,
            'drops_out': net_io.dropout
        }
    
    def _check_processes(self):
        """检查进程情况"""
        processes = []
        
        for proc in psutil.process_iter(['pid', 'name', 'cpu_percent', 'memory_percent']):
            try:
                if proc.info['cpu_percent'] > 10 or proc.info['memory_percent'] > 5:
                    processes.append(proc.info)
            except (psutil.NoSuchProcess, psutil.AccessDenied):
                continue
        
        # 按CPU使用率排序
        processes.sort(key=lambda x: x['cpu_percent'], reverse=True)
        
        return processes[:10]  # 返回前10个高资源使用进程
    
    def _generate_recommendations(self, report):
        """生成优化建议"""
        recommendations = []
        
        # CPU建议
        if report['cpu']['status'] == 'critical':
            recommendations.append({
                'type': 'cpu',
                'message': f"CPU使用率过高 ({report['cpu']['usage_percent']:.1f}%)",
                'suggestions': [
                    "检查是否有CPU密集型进程",
                    "考虑增加CPU核心数",
                    "优化构建脚本减少CPU使用"
                ]
            })
        
        # 内存建议
        if report['memory']['status'] == 'critical':
            recommendations.append({
                'type': 'memory',
                'message': f"内存使用率过高 ({report['memory']['usage_percent']:.1f}%)",
                'suggestions': [
                    "检查内存泄漏的进程",
                    "增加物理内存",
                    "优化构建过程减少内存使用",
                    "清理不必要的缓存文件"
                ]
            })
        
        # 磁盘建议
        for disk in report['disk']:
            if disk['status'] == 'critical':
                recommendations.append({
                    'type': 'disk',
                    'message': f"磁盘空间不足 {disk['mountpoint']} ({disk['usage_percent']:.1f}%)",
                    'suggestions': [
                        "清理临时文件和日志",
                        "删除不必要的构建缓存",
                        "扩展磁盘容量",
                        "设置自动清理策略"
                    ]
                })
        
        return recommendations
    
    def generate_report(self):
        """生成资源监控报告"""
        report = self.check_system_resources()
        
        print("=== 构建机资源监控报告 ===")
        print(f"检查时间: {report['timestamp']}")
        print()
        
        # CPU信息
        cpu = report['cpu']
        print(f"CPU使用率: {cpu['usage_percent']:.1f}% ({cpu['status']})")
        print(f"CPU核心数: {cpu['cpu_count']}")
        print(f"负载平均: {cpu['load_average']}")
        print()
        
        # 内存信息
        memory = report['memory']
        print(f"内存使用: {memory['used_gb']:.1f}GB / {memory['total_gb']:.1f}GB ({memory['usage_percent']:.1f}%)")
        print(f"可用内存: {memory['available_gb']:.1f}GB")
        print(f"Swap使用: {memory['swap_usage_percent']:.1f}%")
        print()
        
        # 磁盘信息
        print("磁盘使用:")
        for disk in report['disk']:
            print(f"  {disk['mountpoint']}: {disk['used_gb']:.1f}GB / {disk['total_gb']:.1f}GB ({disk['usage_percent']:.1f}%)")
        print()
        
        # 高资源使用进程
        if report['processes']:
            print("高资源使用进程:")
            for proc in report['processes'][:5]:
                print(f"  PID {proc['pid']}: {proc['name']} (CPU: {proc['cpu_percent']:.1f}%, 内存: {proc['memory_percent']:.1f}%)")
        print()
        
        # 建议
        if report['recommendations']:
            print("=== 优化建议 ===")
            for rec in report['recommendations']:
                print(f"❌ {rec['message']}")
                for suggestion in rec['suggestions']:
                    print(f"   - {suggestion}")
                print()
        else:
            print("✅ 系统资源使用正常")
        
        return report

# 使用示例
if __name__ == "__main__":
    monitor = ResourceMonitor()
    report = monitor.generate_report()
    
    # 保存报告到文件
    with open('resource_report.json', 'w') as f:
        json.dump(report, f, indent=2)
```

## 流水线触发问题

### 1. 代码事件触发排查

#### Webhook配置检查
```bash
#!/bin/bash

# Webhook触发问题排查
troubleshoot_webhook() {
    local project_id=$1
    local pipeline_id=$2
    local repo_url=$3
    
    echo "=== Webhook触发问题排查 ==="
    echo "项目ID: $project_id"
    echo "流水线ID: $pipeline_id"
    echo "代码库: $repo_url"
    echo ""
    
    # 检查Webhook配置
    echo "=== Webhook配置检查 ==="
    echo "1. 检查代码库Webhook设置:"
    echo "   - URL: https://devops.woa.com/ms/process/api/external/scm/$project_id/webhook/commit"
    echo "   - 事件: Push events, Merge request events"
    echo "   - SSL验证: 启用"
    echo ""
    
    # 检查触发器配置
    echo "=== 触发器配置检查 ==="
    echo "2. 检查流水线触发器设置:"
    echo "   - 代码库已关联"
    echo "   - 分支规则正确"
    echo "   - 事件类型匹配"
    echo ""
    
    # 网络连通性测试
    echo "=== 网络连通性测试 ==="
    local webhook_url="https://devops.woa.com/ms/process/api/external/scm/$project_id/webhook/commit"
    if curl -s --connect-timeout 10 "$webhook_url" >/dev/null; then
        echo "✅ Webhook URL可访问"
    else
        echo "❌ Webhook URL不可访问"
        echo "建议检查网络和防火墙设置"
    fi
    echo ""
    
    # 检查最近的Webhook日志
    echo "=== Webhook日志检查 ==="
    echo "3. 在代码库设置中查看Webhook日志:"
    echo "   - 检查最近的推送记录"
    echo "   - 确认HTTP状态码"
    echo "   - 查看错误信息"
    echo ""
    
    # 常见问题和解决方案
    echo "=== 常见问题和解决方案 ==="
    cat << 'EOF'
问题1: Webhook未触发
解决: 
- 检查代码库Webhook配置是否正确
- 确认推送的分支是否匹配触发规则
- 验证网络连接和SSL证书

问题2: 触发了但流水线未执行
解决:
- 检查流水线触发器配置
- 确认分支过滤规则
- 查看流水线执行历史

问题3: 部分事件未触发
解决:
- 检查事件类型配置
- 确认Merge Request设置
- 验证Tag推送配置
EOF
}

# Git事件触发测试
test_git_trigger() {
    local repo_path=$1
    local branch=$2
    
    echo "=== Git事件触发测试 ==="
    
    cd "$repo_path" || exit 1
    
    # 创建测试提交
    echo "创建测试提交..."
    echo "Test commit $(date)" > test_trigger.txt
    git add test_trigger.txt
    git commit -m "test: trigger pipeline $(date)"
    
    # 推送到远程
    echo "推送到远程分支: $branch"
    git push origin "$branch"
    
    echo "✅ 测试提交已推送"
    echo "请检查流水线是否被触发"
    
    # 清理测试文件
    git rm test_trigger.txt
    git commit -m "cleanup: remove test file"
    git push origin "$branch"
}

# 使用示例
troubleshoot_webhook "project-123" "pipeline-456" "https://git.code.oa.com/group/repo.git"
```

### 2. 定时触发问题

#### Cron表达式验证
```python
#!/usr/bin/env python3
import re
from datetime import datetime, timedelta
from croniter import croniter

class CronValidator:
    def __init__(self):
        self.cron_fields = [
            {'name': 'minute', 'range': (0, 59)},
            {'name': 'hour', 'range': (0, 23)},
            {'name': 'day', 'range': (1, 31)},
            {'name': 'month', 'range': (1, 12)},
            {'name': 'weekday', 'range': (0, 6)}
        ]
    
    def validate_cron_expression(self, cron_expr):
        """验证Cron表达式"""
        
        print(f"=== Cron表达式验证 ===")
        print(f"表达式: {cron_expr}")
        print()
        
        # 基本格式检查
        fields = cron_expr.split()
        if len(fields) != 5:
            print("❌ 错误: Cron表达式必须包含5个字段")
            print("格式: 分钟 小时 日 月 星期")
            return False
        
        # 字段验证
        for i, field in enumerate(fields):
            field_info = self.cron_fields[i]
            if not self._validate_field(field, field_info):
                print(f"❌ 错误: {field_info['name']}字段无效: {field}")
                return False
        
        # 使用croniter验证
        try:
            cron = croniter(cron_expr, datetime.now())
            print("✅ Cron表达式语法正确")
            
            # 显示下次执行时间
            next_runs = []
            for _ in range(5):
                next_runs.append(cron.get_next(datetime))
            
            print("\n下次执行时间:")
            for i, next_run in enumerate(next_runs, 1):
                print(f"  {i}. {next_run.strftime('%Y-%m-%d %H:%M:%S')}")
            
            return True
            
        except Exception as e:
            print(f"❌ 错误: {str(e)}")
            return False
    
    def _validate_field(self, field, field_info):
        """验证单个字段"""
        
        # 通配符
        if field == '*':
            return True
        
        # 步长值
        if '/' in field:
            parts = field.split('/')
            if len(parts) != 2:
                return False
            base, step = parts
            if base != '*' and not self._validate_range(base, field_info['range']):
                return False
            try:
                step_val = int(step)
                return step_val > 0
            except ValueError:
                return False
        
        # 范围值
        if '-' in field:
            parts = field.split('-')
            if len(parts) != 2:
                return False
            try:
                start, end = int(parts[0]), int(parts[1])
                return (field_info['range'][0] <= start <= field_info['range'][1] and
                        field_info['range'][0] <= end <= field_info['range'][1] and
                        start <= end)
            except ValueError:
                return False
        
        # 列表值
        if ',' in field:
            values = field.split(',')
            for value in values:
                if not self._validate_single_value(value, field_info['range']):
                    return False
            return True
        
        # 单个值
        return self._validate_single_value(field, field_info['range'])
    
    def _validate_single_value(self, value, value_range):
        """验证单个数值"""
        try:
            val = int(value)
            return value_range[0] <= val <= value_range[1]
        except ValueError:
            return False
    
    def _validate_range(self, range_expr, value_range):
        """验证范围表达式"""
        if range_expr == '*':
            return True
        return self._validate_single_value(range_expr, value_range)
    
    def suggest_common_patterns(self):
        """提供常用Cron模式建议"""
        
        patterns = {
            "每分钟": "* * * * *",
            "每小时": "0 * * * *",
            "每天凌晨2点": "0 2 * * *",
            "每周一上午9点": "0 9 * * 1",
            "每月1号凌晨3点": "0 3 1 * *",
            "工作日上午9点": "0 9 * * 1-5",
            "每15分钟": "*/15 * * * *",
            "每2小时": "0 */2 * * *",
            "每天上午9点和下午6点": "0 9,18 * * *"
        }
        
        print("\n=== 常用Cron模式 ===")
        for desc, pattern in patterns.items():
            print(f"{desc}: {pattern}")

# 定时触发问题排查
def troubleshoot_schedule_trigger(cron_expr, timezone="Asia/Shanghai"):
    """排查定时触发问题"""
    
    print("=== 定时触发问题排查 ===")
    print(f"Cron表达式: {cron_expr}")
    print(f"时区: {timezone}")
    print()
    
    # 验证Cron表达式
    validator = CronValidator()
    if not validator.validate_cron_expression(cron_expr):
        print("\n建议:")
        validator.suggest_common_patterns()
        return
    
    # 检查时区设置
    print(f"\n=== 时区检查 ===")
    print(f"当前系统时间: {datetime.now()}")
    print(f"配置时区: {timezone}")
    print("注意: 确保流水线时区设置与预期一致")
    
    # 检查执行历史
    print(f"\n=== 排查建议 ===")
    print("1. 检查流水线执行历史，确认是否有定时触发记录")
    print("2. 验证Cron表达式在预期时间是否正确")
    print("3. 检查流水线是否被禁用或暂停")
    print("4. 确认项目和流水线权限设置")
    print("5. 查看系统通知，是否有相关错误信息")

# 使用示例
if __name__ == "__main__":
    # 验证Cron表达式
    validator = CronValidator()
    
    test_expressions = [
        "0 9 * * 1-5",  # 工作日上午9点
        "*/15 * * * *",  # 每15分钟
        "0 2 1 * *",     # 每月1号凌晨2点
        "0 9,18 * * *",  # 每天上午9点和下午6点
        "invalid cron"   # 无效表达式
    ]
    
    for expr in test_expressions:
        validator.validate_cron_expression(expr)
        print("-" * 50)
```

## 网络和权限问题

### 1. 网络连接诊断

#### 网络连通性测试
```bash
#!/bin/bash

# 网络连接诊断脚本
diagnose_network_connectivity() {
    echo "=== 蓝盾网络连接诊断 ==="
    echo "诊断时间: $(date)"
    echo ""
    
    # 定义测试目标
    local endpoints=(
        "devops.woa.com:443:蓝盾主服务"
        "git.code.oa.com:443:工蜂代码库"
        "bkrepo.woa.com:443:制品库"
        "bkm.woa.com:443:监控平台"
    )
    
    # DNS解析测试
    echo "=== DNS解析测试 ==="
    for endpoint in "${endpoints[@]}"; do
        IFS=':' read -r host port desc <<< "$endpoint"
        
        echo -n "测试 $desc ($host): "
        if nslookup "$host" >/dev/null 2>&1; then
            echo "✅ DNS解析成功"
        else
            echo "❌ DNS解析失败"
        fi
    done
    echo ""
    
    # 端口连通性测试
    echo "=== 端口连通性测试 ==="
    for endpoint in "${endpoints[@]}"; do
        IFS=':' read -r host port desc <<< "$endpoint"
        
        echo -n "测试 $desc ($host:$port): "
        if timeout 10 bash -c "cat < /dev/null > /dev/tcp/$host/$port" 2>/dev/null; then
            echo "✅ 连接成功"
        else
            echo "❌ 连接失败"
        fi
    done
    echo ""
    
    # HTTP/HTTPS测试
    echo "=== HTTP/HTTPS测试 ==="
    local urls=(
        "https://devops.woa.com/console/"
        "https://git.code.oa.com/"
        "https://bkrepo.woa.com/"
    )
    
    for url in "${urls[@]}"; do
        echo -n "测试 $url: "
        local status_code=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout 10 "$url")
        if [[ $status_code -eq 200 || $status_code -eq 302 ]]; then
            echo "✅ HTTP响应正常 ($status_code)"
        else
            echo "❌ HTTP响应异常 ($status_code)"
        fi
    done
    echo ""
    
    # 代理设置检查
    echo "=== 代理设置检查 ==="
    if [[ -n "$http_proxy" ]]; then
        echo "HTTP代理: $http_proxy"
    else
        echo "HTTP代理: 未设置"
    fi
    
    if [[ -n "$https_proxy" ]]; then
        echo "HTTPS代理: $https_proxy"
    else
        echo "HTTPS代理: 未设置"
    fi
    
    if [[ -n "$no_proxy" ]]; then
        echo "代理排除: $no_proxy"
    else
        echo "代理排除: 未设置"
    fi
    echo ""
    
    # 防火墙检查
    echo "=== 防火墙检查 ==="
    if command -v iptables >/dev/null 2>&1; then
        local iptables_rules=$(iptables -L | wc -l)
        echo "iptables规则数: $iptables_rules"
        
        # 检查是否有阻止规则
        if iptables -L | grep -q "DROP\|REJECT"; then
            echo "⚠️  发现DROP/REJECT规则，可能影响网络连接"
        fi
    else
        echo "iptables: 未安装或无权限"
    fi
    
    # 系统防火墙状态
    if command -v ufw >/dev/null 2>&1; then
        local ufw_status=$(ufw status | head -1)
        echo "UFW状态: $ufw_status"
    fi
    
    if command -v firewall-cmd >/dev/null 2>&1; then
        local firewalld_status=$(firewall-cmd --state 2>/dev/null || echo "inactive")
        echo "Firewalld状态: $firewalld_status"
    fi
    echo ""
    
    # 网络接口信息
    echo "=== 网络接口信息 ==="
    ip addr show | grep -E "^[0-9]+:|inet " | head -10
    echo ""
    
    # 路由信息
    echo "=== 路由信息 ==="
    ip route | head -5
    echo ""
    
    # 生成修复建议
    echo "=== 修复建议 ==="
    echo "1. 如果DNS解析失败:"
    echo "   - 检查/etc/resolv.conf配置"
    echo "   - 尝试使用公共DNS: 8.8.8.8, 114.114.114.114"
    echo ""
    echo "2. 如果端口连接失败:"
    echo "   - 检查防火墙设置"
    echo "   - 确认代理配置"
    echo "   - 联系网络管理员"
    echo ""
    echo "3. 如果HTTP响应异常:"
    echo "   - 检查SSL证书"
    echo "   - 验证代理设置"
    echo "   - 确认服务状态"
}

# 代理配置检查
check_proxy_configuration() {
    echo "=== 代理配置检查 ==="
    
    # 环境变量代理
    echo "环境变量代理设置:"
    env | grep -i proxy | sort
    echo ""
    
    # Git代理设置
    echo "Git代理设置:"
    git config --global --get http.proxy 2>/dev/null || echo "未设置"
    git config --global --get https.proxy 2>/dev/null || echo "未设置"
    echo ""
    
    # Docker代理设置
    if command -v docker >/dev/null 2>&1; then
        echo "Docker代理设置:"
        if [[ -f ~/.docker/config.json ]]; then
            cat ~/.docker/config.json | grep -A 5 -B 5 proxy || echo "未设置"
        else
            echo "Docker配置文件不存在"
        fi
    fi
    echo ""
    
    # 系统代理设置建议
    echo "=== 代理设置建议 ==="
    cat << 'EOF'
如需设置代理，请添加以下环境变量:

export http_proxy=http://proxy.company.com:8080
export https_proxy=http://proxy.company.com:8080
export no_proxy=localhost,127.0.0.1,.company.com

Git代理设置:
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy http://proxy.company.com:8080

取消代理设置:
unset http_proxy https_proxy no_proxy
git config --global --unset http.proxy
git config --global --unset https.proxy
EOF
}

# 使用示例
diagnose_network_connectivity
check_proxy_configuration
```

### 2. 权限问题排查

#### 权限诊断脚本
```bash
#!/bin/bash

# 权限问题诊断脚本
diagnose_permission_issues() {
    local user_id=$1
    local project_id=$2
    
    echo "=== 权限问题诊断 ==="
    echo "用户ID: $user_id"
    echo "项目ID: $project_id"
    echo ""
    
    # 检查用户基本信息
    echo "=== 用户信息检查 ==="
    echo "当前用户: $(whoami)"
    echo "用户组: $(groups)"
    echo "用户ID: $(id -u)"
    echo "组ID: $(id -g)"
    echo ""
    
    # 检查文件权限
    echo "=== 文件权限检查 ==="
    local important_paths=(
        "/tmp"
        "/var/log"
        "$HOME"
        "$(pwd)"
    )
    
    for path in "${important_paths[@]}"; do
        if [[ -e "$path" ]]; then
            echo "$path: $(ls -ld "$path")"
        else
            echo "$path: 不存在"
        fi
    done
    echo ""
    
    # 检查sudo权限
    echo "=== Sudo权限检查 ==="
    if sudo -n true 2>/dev/null; then
        echo "✅ 具有sudo权限"
    else
        echo "❌ 无sudo权限或需要密码"
    fi
    echo ""
    
    # 检查Docker权限
    if command -v docker >/dev/null 2>&1; then
        echo "=== Docker权限检查 ==="
        if docker ps >/dev/null 2>&1; then
            echo "✅ Docker权限正常"
        else
            echo "❌ Docker权限不足"
            echo "建议: 将用户添加到docker组"
            echo "sudo usermod -aG docker $(whoami)"
        fi
        echo ""
    fi
    
    # 检查网络权限
    echo "=== 网络权限检查 ==="
    if curl -s --connect-timeout 5 https://www.baidu.com >/dev/null; then
        echo "✅ 外网访问正常"
    else
        echo "❌ 外网访问受限"
    fi
    
    if curl -s --connect-timeout 5 https://devops.woa.com >/dev/null; then
        echo "✅ 蓝盾服务访问正常"
    else
        echo "❌ 蓝盾服务访问受限"
    fi
    echo ""
    
    # 生成权限修复建议
    echo "=== 权限修复建议 ==="
    cat << 'EOF'
常见权限问题及解决方案:

1. 文件权限不足:
   chmod +x script.sh
   chmod 755 directory/
   
2. 目录权限问题:
   sudo chown -R $(whoami):$(whoami) /path/to/directory
   
3. Docker权限问题:
   sudo usermod -aG docker $(whoami)
   # 需要重新登录生效
   
4. 临时文件权限:
   sudo chmod 1777 /tmp
   
5. 日志文件权限:
   sudo chmod 755 /var/log
   
6. SSH密钥权限:
   chmod 600 ~/.ssh/id_rsa
   chmod 644 ~/.ssh/id_rsa.pub
EOF
}

# 蓝盾项目权限检查
check_bkci_project_permissions() {
    local project_id=$1
    local token=$2
    
    echo "=== 蓝盾项目权限检查 ==="
    echo "项目ID: $project_id"
    echo ""
    
    if [[ -z "$token" ]]; then
        echo "❌ 未提供访问Token"
        echo "请在蓝盾个人设置中生成访问Token"
        return 1
    fi
    
    # 检查项目访问权限
    echo "检查项目访问权限..."
    local response=$(curl -s -H "Authorization: Bearer $token" \
        "https://devops.woa.com/ms/project/api/user/projects/$project_id")
    
    if echo "$response" | grep -q "\"code\":0"; then
        echo "✅ 项目访问权限正常"
    else
        echo "❌ 项目访问权限不足"
        echo "响应: $response"
    fi
    
    # 检查流水线权限
    echo "检查流水线权限..."
    local pipelines=$(curl -s -H "Authorization: Bearer $token" \
        "https://devops.woa.com/ms/process/api/user/projects/$project_id/pipelines")
    
    if echo "$pipelines" | grep -q "\"code\":0"; then
        local count=$(echo "$pipelines" | jq '.data.records | length' 2>/dev/null || echo "0")
        echo "✅ 流水线权限正常，可访问 $count 条流水线"
    else
        echo "❌ 流水线权限不足"
    fi
    
    echo ""
    echo "权限问题解决建议:"
    echo "1. 确认是否为项目成员"
    echo "2. 检查用户组权限设置"
    echo "3. 联系项目管理员添加权限"
    echo "4. 验证Token是否有效"
}

# 使用示例
diagnose_permission_issues "user123" "project456"
check_bkci_project_permissions "project456" "your-access-token"
```

## 性能问题排查

### 1. 构建性能分析

#### 性能瓶颈诊断
```python
#!/usr/bin/env python3
import re
import json
import statistics
from datetime import datetime, timedelta
from collections import defaultdict

class BuildPerformanceAnalyzer:
    def __init__(self, log_file):
        self.log_file = log_file
        self.performance_data = {
            'plugins': defaultdict(list),
            'stages': defaultdict(list),
            'total_time': 0,
            'bottlenecks': [],
            'trends': []
        }
    
    def analyze_build_performance(self):
        """分析构建性能"""
        
        print("=== 构建性能分析 ===")
        print(f"日志文件: {self.log_file}")
        print()
        
        # 解析日志文件
        self._parse_log_file()
        
        # 分析性能数据
        self._analyze_performance_data()
        
        # 生成报告
        self._generate_performance_report()
        
        return self.performance_data
    
    def _parse_log_file(self):
        """解析日志文件"""
        
        with open(self.log_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        current_plugin = None
        plugin_start_time = None
        build_start_time = None
        
        for line in lines:
            timestamp = self._extract_timestamp(line)
            
            # 构建开始时间
            if 'Build started' in line and build_start_time is None:
                build_start_time = timestamp
            
            # 插件开始
            plugin_start_match = re.search(r'Starting plugin: (\w+)', line)
            if plugin_start_match:
                current_plugin = plugin_start_match.group(1)
                plugin_start_time = timestamp
                continue
            
            # 插件结束
            plugin_end_match = re.search(r'Plugin (\w+) completed in (\d+)ms', line)
            if plugin_end_match:
                plugin_name = plugin_end_match.group(1)
                duration = int(plugin_end_match.group(2))
                
                self.performance_data['plugins'][plugin_name].append({
                    'duration': duration,
                    'timestamp': timestamp
                })
                continue
            
            # Stage信息
            stage_match = re.search(r'Stage (\w+) completed in (\d+)ms', line)
            if stage_match:
                stage_name = stage_match.group(1)
                duration = int(stage_match.group(2))
                
                self.performance_data['stages'][stage_name].append({
                    'duration': duration,
                    'timestamp': timestamp
                })
                continue
            
            # 构建结束
            if 'Build completed' in line and build_start_time:
                if timestamp:
                    self.performance_data['total_time'] = (timestamp - build_start_time).total_seconds() * 1000
    
    def _extract_timestamp(self, line):
        """提取时间戳"""
        match = re.search(r'\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}', line)
        if match:
            try:
                return datetime.strptime(match.group(0), '%Y-%m-%d %H:%M:%S')
            except:
                pass
        return None
    
    def _analyze_performance_data(self):
        """分析性能数据"""
        
        # 分析插件性能
        for plugin_name, executions in self.performance_data['plugins'].items():
            durations = [exec['duration'] for exec in executions]
            
            avg_duration = statistics.mean(durations)
            max_duration = max(durations)
            min_duration = min(durations)
            
            # 识别性能瓶颈
            if avg_duration > 60000:  # 超过1分钟
                self.performance_data['bottlenecks'].append({
                    'type': 'plugin',
                    'name': plugin_name,
                    'avg_duration': avg_duration,
                    'max_duration': max_duration,
                    'severity': 'high' if avg_duration > 300000 else 'medium'
                })
        
        # 分析Stage性能
        for stage_name, executions in self.performance_data['stages'].items():
            durations = [exec['duration'] for exec in executions]
            
            avg_duration = statistics.mean(durations)
            max_duration = max(durations)
            
            if avg_duration > 120000:  # 超过2分钟
                self.performance_data['bottlenecks'].append({
                    'type': 'stage',
                    'name': stage_name,
                    'avg_duration': avg_duration,
                    'max_duration': max_duration,
                    'severity': 'high' if avg_duration > 600000 else 'medium'
                })
    
    def _generate_performance_report(self):
        """生成性能报告"""
        
        print("=== 性能分析结果 ===")
        
        # 总体性能
        if self.performance_data['total_time'] > 0:
            print(f"总构建时间: {self.performance_data['total_time']/1000:.1f}秒")
        
        # 插件性能排行
        plugin_avg_times = {}
        for plugin_name, executions in self.performance_data['plugins'].items():
            durations = [exec['duration'] for exec in executions]
            plugin_avg_times[plugin_name] = statistics.mean(durations)
        
        if plugin_avg_times:
            print("\n插件平均执行时间排行:")
            sorted_plugins = sorted(plugin_avg_times.items(), key=lambda x: x[1], reverse=True)
            for i, (plugin, avg_time) in enumerate(sorted_plugins[:10], 1):
                print(f"  {i}. {plugin}: {avg_time/1000:.1f}秒")
        
        # 性能瓶颈
        if self.performance_data['bottlenecks']:
            print(f"\n发现 {len(self.performance_data['bottlenecks'])} 个性能瓶颈:")
            for bottleneck in self.performance_data['bottlenecks']:
                severity_icon = "🔴" if bottleneck['severity'] == 'high' else "🟡"
                print(f"  {severity_icon} {bottleneck['type']}: {bottleneck['name']} "
                      f"(平均: {bottleneck['avg_duration']/1000:.1f}秒)")
        
        # 优化建议
        print("\n=== 优化建议 ===")
        self._generate_optimization_suggestions()
    
    def _generate_optimization_suggestions(self):
        """生成优化建议"""
        
        suggestions = []
        
        # 基于瓶颈生成建议
        for bottleneck in self.performance_data['bottlenecks']:
            if bottleneck['type'] == 'plugin':
                if 'test' in bottleneck['name'].lower():
                    suggestions.append(f"优化测试插件 {bottleneck['name']}：考虑并行执行或减少测试用例")
                elif 'build' in bottleneck['name'].lower():
                    suggestions.append(f"优化构建插件 {bottleneck['name']}：检查编译配置和依赖管理")
                elif 'deploy' in bottleneck['name'].lower():
                    suggestions.append(f"优化部署插件 {bottleneck['name']}：考虑增量部署或优化网络连接")
                else:
                    suggestions.append(f"优化插件 {bottleneck['name']}：检查插件配置和执行逻辑")
        
        # 通用优化建议
        if self.performance_data['total_time'] > 1800000:  # 超过30分钟
            suggestions.extend([
                "构建时间过长，考虑以下优化：",
                "- 启用构建缓存",
                "- 并行执行独立任务",
                "- 优化依赖下载",
                "- 使用更高性能的构建机"
            ])
        
        if not suggestions:
            suggestions.append("构建性能良好，无需特殊优化")
        
        for suggestion in suggestions:
            print(f"• {suggestion}")

# 构建缓存分析
class BuildCacheAnalyzer:
    def __init__(self):
        self.cache_stats = {
            'hits': 0,
            'misses': 0,
            'total_size': 0,
            'cache_types': defaultdict(int)
        }
    
    def analyze_cache_usage(self, log_file):
        """分析构建缓存使用情况"""
        
        print("=== 构建缓存分析 ===")
        
        with open(log_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检测缓存命中
        cache_hit_patterns = [
            r'cache hit',
            r'using cached',
            r'restored from cache',
            r'cache restored'
        ]
        
        for pattern in cache_hit_patterns:
            hits = len(re.findall(pattern, content, re.IGNORECASE))
            self.cache_stats['hits'] += hits
        
        # 检测缓存未命中
        cache_miss_patterns = [
            r'cache miss',
            r'cache not found',
            r'building from scratch',
            r'no cache available'
        ]
        
        for pattern in cache_miss_patterns:
            misses = len(re.findall(pattern, content, re.IGNORECASE))
            self.cache_stats['misses'] += misses
        
        # 分析缓存类型
        cache_type_patterns = {
            'docker': r'docker.*cache',
            'npm': r'npm.*cache',
            'maven': r'maven.*cache',
            'gradle': r'gradle.*cache',
            'pip': r'pip.*cache'
        }
        
        for cache_type, pattern in cache_type_patterns.items():
            count = len(re.findall(pattern, content, re.IGNORECASE))
            if count > 0:
                self.cache_stats['cache_types'][cache_type] = count
        
        # 生成缓存报告
        self._generate_cache_report()
    
    def _generate_cache_report(self):
        """生成缓存报告"""
        
        total_cache_ops = self.cache_stats['hits'] + self.cache_stats['misses']
        
        if total_cache_ops > 0:
            hit_rate = (self.cache_stats['hits'] / total_cache_ops) * 100
            print(f"缓存命中率: {hit_rate:.1f}% ({self.cache_stats['hits']}/{total_cache_ops})")
            
            if hit_rate < 50:
                print("⚠️  缓存命中率较低，建议优化缓存策略")
            elif hit_rate > 80:
                print("✅ 缓存命中率良好")
        else:
            print("未检测到缓存使用情况")
        
        if self.cache_stats['cache_types']:
            print("\n缓存类型使用情况:")
            for cache_type, count in self.cache_stats['cache_types'].items():
                print(f"  {cache_type}: {count}次")
        
        print("\n缓存优化建议:")
        print("• 启用构建缓存以提高构建速度")
        print("• 合理设置缓存键值，避免缓存失效")
        print("• 定期清理过期缓存释放存储空间")
        print("• 使用分层缓存策略优化缓存效果")

# 使用示例
if __name__ == "__main__":
    # 性能分析
    analyzer = BuildPerformanceAnalyzer('build.log')
    performance_data = analyzer.analyze_build_performance()
    
    print("\n" + "="*50 + "\n")
    
    # 缓存分析
    cache_analyzer = BuildCacheAnalyzer()
    cache_analyzer.analyze_cache_usage('build.log')
```

## 故障预防和最佳实践

### 1. 预防性检查清单

#### 流水线健康检查
```yaml
# 流水线健康检查清单
pipeline_health_checklist:
  # 配置检查
  configuration:
    - name: "触发器配置"
      checks:
        - "代码库连接正常"
        - "分支规则正确"
        - "Webhook配置有效"
        - "定时触发表达式正确"
    
    - name: "插件配置"
      checks:
        - "必填参数已设置"
        - "凭证配置正确"
        - "超时时间合理"
        - "依赖关系明确"
    
    - name: "环境配置"
      checks:
        - "构建机可用"
        - "环境变量正确"
        - "资源配额充足"
        - "网络连接正常"
  
  # 性能检查
  performance:
    - name: "执行时间"
      thresholds:
        - "总执行时间 < 30分钟"
        - "单个插件 < 10分钟"
        - "排队时间 < 5分钟"
    
    - name: "资源使用"
      thresholds:
        - "CPU使用率 < 80%"
        - "内存使用率 < 85%"
        - "磁盘使用率 < 90%"
  
  # 质量检查
  quality:
    - name: "成功率"
      thresholds:
        - "构建成功率 > 90%"
        - "部署成功率 > 95%"
        - "测试通过率 > 95%"
    
    - name: "稳定性"
      checks:
        - "无间歇性失败"
        - "错误信息明确"
        - "日志输出完整"
```

### 2. 监控告警配置

#### 主动监控策略
```yaml
# 主动监控告警配置
monitoring_alerts:
  # 构建失败告警
  build_failure:
    condition: "构建失败次数 > 3 (1小时内)"
    severity: "warning"
    actions:
      - "通知开发团队"
      - "自动重试一次"
      - "记录失败原因"
  
  # 性能异常告警
  performance_degradation:
    condition: "构建时间 > 平均时间的150%"
    severity: "warning"
    actions:
      - "通知运维团队"
      - "分析性能瓶颈"
      - "检查资源使用"
  
  # 资源不足告警
  resource_shortage:
    conditions:
      - "构建机CPU > 90% (5分钟)"
      - "构建机内存 > 95%"
      - "磁盘空间 < 10%"
    severity: "critical"
    actions:
      - "立即通知运维"
      - "暂停新构建"
      - "扩容资源"
  
  # 依赖服务异常
  dependency_failure:
    conditions:
      - "代码库连接失败"
      - "制品库不可用"
      - "网络连接异常"
    severity: "critical"
    actions:
      - "通知相关团队"
      - "切换备用服务"
      - "记录故障时间"
```

### 3. 故障恢复流程

#### 自动恢复机制
```bash
#!/bin/bash

# 故障自动恢复脚本
auto_recovery_system() {
    local failure_type=$1
    local context=$2
    
    echo "=== 故障自动恢复系统 ==="
    echo "故障类型: $failure_type"
    echo "上下文: $context"
    echo ""
    
    case $failure_type in
        "build_failure")
            handle_build_failure "$context"
            ;;
        "network_issue")
            handle_network_issue "$context"
            ;;
        "resource_shortage")
            handle_resource_shortage "$context"
            ;;
        "plugin_error")
            handle_plugin_error "$context"
            ;;
        *)
            echo "未知故障类型，执行通用恢复流程"
            generic_recovery "$context"
            ;;
    esac
}

# 构建失败恢复
handle_build_failure() {
    local build_id=$1
    
    echo "处理构建失败: $build_id"
    
    # 1. 分析失败原因
    echo "1. 分析失败原因..."
    local failure_reason=$(analyze_build_failure "$build_id")
    echo "失败原因: $failure_reason"
    
    # 2. 自动重试
    if [[ "$failure_reason" == "network" || "$failure_reason" == "timeout" ]]; then
        echo "2. 执行自动重试..."
        retry_build "$build_id"
    fi
    
    # 3. 通知相关人员
    echo "3. 发送通知..."
    notify_team "构建失败" "$build_id" "$failure_reason"
    
    # 4. 记录故障信息
    log_incident "build_failure" "$build_id" "$failure_reason"
}

# 网络问题恢复
handle_network_issue() {
    local service=$1
    
    echo "处理网络问题: $service"
    
    # 1. 检查网络连接
    echo "1. 检查网络连接..."
    if ping -c 3 "$service" >/dev/null 2>&1; then
        echo "网络连接已恢复"
        return 0
    fi
    
    # 2. 重启网络服务
    echo "2. 尝试重启网络服务..."
    sudo systemctl restart networking
    sleep 10
    
    # 3. 验证恢复状态
    if ping -c 3 "$service" >/dev/null 2>&1; then
        echo "网络问题已解决"
        notify_team "网络恢复" "$service" "自动重启网络服务"
    else
        echo "网络问题未解决，需要人工介入"
        escalate_incident "network_issue" "$service"
    fi
}

# 资源不足恢复
handle_resource_shortage() {
    local resource_type=$1
    
    echo "处理资源不足: $resource_type"
    
    case $resource_type in
        "disk")
            # 清理临时文件
            echo "清理磁盘空间..."
            cleanup_disk_space
            ;;
        "memory")
            # 重启服务释放内存
            echo "释放内存资源..."
            restart_memory_intensive_services
            ;;
        "cpu")
            # 降低并发度
            echo "降低CPU负载..."
            reduce_concurrent_builds
            ;;
    esac
    
    # 验证资源状态
    if check_resource_status "$resource_type"; then
        echo "资源问题已解决"
        notify_team "资源恢复" "$resource_type" "自动清理"
    else
        echo "资源问题未解决，需要扩容"
        request_resource_scaling "$resource_type"
    fi
}

# 插件错误恢复
handle_plugin_error() {
    local plugin_name=$1
    
    echo "处理插件错误: $plugin_name"
    
    # 1. 检查插件状态
    if check_plugin_health "$plugin_name"; then
        echo "插件状态正常，可能是临时问题"
        return 0
    fi
    
    # 2. 重启插件服务
    echo "重启插件服务..."
    restart_plugin_service "$plugin_name"
    
    # 3. 验证插件功能
    if test_plugin_functionality "$plugin_name"; then
        echo "插件恢复正常"
        notify_team "插件恢复" "$plugin_name" "自动重启"
    else
        echo "插件问题未解决"
        escalate_to_plugin_owner "$plugin_name"
    fi
}

# 通用恢复流程
generic_recovery() {
    local context=$1
    
    echo "执行通用恢复流程: $context"
    
    # 1. 收集诊断信息
    collect_diagnostic_info "$context"
    
    # 2. 尝试服务重启
    restart_related_services "$context"
    
    # 3. 验证系统状态
    if verify_system_health; then
        echo "系统恢复正常"
    else
        echo "需要人工介入"
        create_incident_ticket "$context"
    fi
}

# 辅助函数
analyze_build_failure() {
    local build_id=$1
    # 分析构建日志，返回失败原因
    echo "network"  # 示例返回值
}

retry_build() {
    local build_id=$1
    echo "重试构建: $build_id"
}

notify_team() {
    local event=$1
    local target=$2
    local reason=$3
    echo "通知团队: $event - $target ($reason)"
}

log_incident() {
    local type=$1
    local target=$2
    local reason=$3
    echo "$(date): $type - $target - $reason" >> /var/log/incidents.log
}

escalate_incident() {
    local type=$1
    local target=$2
    echo "升级故障: $type - $target"
}

# 使用示例
auto_recovery_system "build_failure" "build-123"
```

## 总结

故障排查是保障CI/CD系统稳定运行的关键能力：

1. **系统化排查**: 建立标准化的故障排查流程和方法
2. **预防为主**: 通过监控和检查提前发现潜在问题
3. **快速恢复**: 建立自动化的故障恢复机制
4. **持续改进**: 基于故障分析不断优化系统和流程

建议团队建立完善的故障管理体系，包括故障分类、排查手册、恢复流程和预防措施，确保能够快速响应和解决各类问题。