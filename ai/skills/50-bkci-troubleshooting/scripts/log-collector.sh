#!/bin/bash

# BK-CI 日志收集脚本
# 用于收集 BK-CI 各组件的日志信息，便于问题排查

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置参数
NAMESPACE=${NAMESPACE:-"bk-ci"}
OUTPUT_DIR=${OUTPUT_DIR:-"./bk-ci-logs-$(date +%Y%m%d-%H%M%S)"}
LOG_LINES=${LOG_LINES:-1000}
TIME_RANGE=${TIME_RANGE:-"1h"}

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 创建输出目录
create_output_dir() {
    log_info "创建输出目录: $OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR"
    mkdir -p "$OUTPUT_DIR/pods"
    mkdir -p "$OUTPUT_DIR/services"
    mkdir -p "$OUTPUT_DIR/events"
    mkdir -p "$OUTPUT_DIR/configs"
    mkdir -p "$OUTPUT_DIR/describe"
}

# 收集 Pod 日志
collect_pod_logs() {
    log_info "收集 Pod 日志..."
    
    pods=$(kubectl get pods -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
    
    for pod in $pods; do
        log_info "收集 Pod $pod 的日志..."
        
        # 获取当前日志
        kubectl logs -n ${NAMESPACE} $pod --tail=${LOG_LINES} > "$OUTPUT_DIR/pods/${pod}.log" 2>/dev/null || {
            log_warning "无法获取 Pod $pod 的日志"
            echo "无法获取日志" > "$OUTPUT_DIR/pods/${pod}.log"
        }
        
        # 获取前一个容器的日志（如果存在）
        kubectl logs -n ${NAMESPACE} $pod --previous --tail=${LOG_LINES} > "$OUTPUT_DIR/pods/${pod}-previous.log" 2>/dev/null || {
            echo "没有前一个容器的日志" > "$OUTPUT_DIR/pods/${pod}-previous.log"
        }
        
        # 如果 Pod 有多个容器，分别收集
        containers=$(kubectl get pod -n ${NAMESPACE} $pod -o jsonpath='{.spec.containers[*].name}')
        container_count=$(echo $containers | wc -w)
        
        if [ $container_count -gt 1 ]; then
            for container in $containers; do
                log_info "收集 Pod $pod 容器 $container 的日志..."
                kubectl logs -n ${NAMESPACE} $pod -c $container --tail=${LOG_LINES} > "$OUTPUT_DIR/pods/${pod}-${container}.log" 2>/dev/null || {
                    log_warning "无法获取 Pod $pod 容器 $container 的日志"
                    echo "无法获取日志" > "$OUTPUT_DIR/pods/${pod}-${container}.log"
                }
            done
        fi
    done
}

# 收集 Pod 描述信息
collect_pod_describe() {
    log_info "收集 Pod 描述信息..."
    
    pods=$(kubectl get pods -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
    
    for pod in $pods; do
        kubectl describe pod -n ${NAMESPACE} $pod > "$OUTPUT_DIR/describe/${pod}-describe.txt" 2>/dev/null || {
            log_warning "无法获取 Pod $pod 的描述信息"
        }
    done
}

# 收集服务信息
collect_service_info() {
    log_info "收集服务信息..."
    
    # 收集服务列表
    kubectl get services -n ${NAMESPACE} -o wide > "$OUTPUT_DIR/services/services-list.txt" 2>/dev/null
    
    # 收集端点信息
    kubectl get endpoints -n ${NAMESPACE} -o wide > "$OUTPUT_DIR/services/endpoints-list.txt" 2>/dev/null
    
    # 收集 Ingress 信息
    kubectl get ingress -n ${NAMESPACE} -o wide > "$OUTPUT_DIR/services/ingress-list.txt" 2>/dev/null
    
    # 详细描述每个服务
    services=$(kubectl get services -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
    for service in $services; do
        kubectl describe service -n ${NAMESPACE} $service > "$OUTPUT_DIR/describe/${service}-service-describe.txt" 2>/dev/null
    done
}

# 收集事件信息
collect_events() {
    log_info "收集事件信息..."
    
    # 收集所有事件
    kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events/all-events.txt" 2>/dev/null
    
    # 收集警告和错误事件
    kubectl get events -n ${NAMESPACE} --field-selector type=Warning --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events/warning-events.txt" 2>/dev/null
    kubectl get events -n ${NAMESPACE} --field-selector type=Error --sort-by='.lastTimestamp' > "$OUTPUT_DIR/events/error-events.txt" 2>/dev/null
    
    # 收集最近的事件（详细格式）
    kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' -o yaml > "$OUTPUT_DIR/events/events-detail.yaml" 2>/dev/null
}

# 收集配置信息
collect_configs() {
    log_info "收集配置信息..."
    
    # 收集 ConfigMap
    kubectl get configmaps -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/configs/configmaps.yaml" 2>/dev/null
    
    # 收集 Secret（不包含敏感数据）
    kubectl get secrets -n ${NAMESPACE} > "$OUTPUT_DIR/configs/secrets-list.txt" 2>/dev/null
    
    # 收集 PVC
    kubectl get pvc -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/configs/pvc.yaml" 2>/dev/null
    
    # 收集 Deployment
    kubectl get deployments -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/configs/deployments.yaml" 2>/dev/null
    
    # 收集 StatefulSet
    kubectl get statefulsets -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/configs/statefulsets.yaml" 2>/dev/null
    
    # 收集 DaemonSet
    kubectl get daemonsets -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/configs/daemonsets.yaml" 2>/dev/null
}

# 收集资源使用情况
collect_resource_usage() {
    log_info "收集资源使用情况..."
    
    # 节点资源使用
    kubectl top nodes > "$OUTPUT_DIR/resource-usage-nodes.txt" 2>/dev/null || {
        echo "无法获取节点资源使用情况 (需要 metrics-server)" > "$OUTPUT_DIR/resource-usage-nodes.txt"
    }
    
    # Pod 资源使用
    kubectl top pods -n ${NAMESPACE} > "$OUTPUT_DIR/resource-usage-pods.txt" 2>/dev/null || {
        echo "无法获取 Pod 资源使用情况 (需要 metrics-server)" > "$OUTPUT_DIR/resource-usage-pods.txt"
    }
    
    # 集群信息
    kubectl cluster-info > "$OUTPUT_DIR/cluster-info.txt" 2>/dev/null
    
    # 节点信息
    kubectl get nodes -o wide > "$OUTPUT_DIR/nodes-info.txt" 2>/dev/null
}

# 收集网络信息
collect_network_info() {
    log_info "收集网络信息..."
    
    # 收集网络策略
    kubectl get networkpolicies -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/network-policies.yaml" 2>/dev/null
    
    # 收集服务网格信息（如果使用 Istio）
    kubectl get virtualservices -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/virtualservices.yaml" 2>/dev/null || true
    kubectl get destinationrules -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/destinationrules.yaml" 2>/dev/null || true
    kubectl get gateways -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/gateways.yaml" 2>/dev/null || true
}

# 收集应用特定信息
collect_app_specific_info() {
    log_info "收集 BK-CI 应用特定信息..."
    
    # 创建应用特定目录
    mkdir -p "$OUTPUT_DIR/app-specific"
    
    # 收集 HPA 信息
    kubectl get hpa -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/app-specific/hpa.yaml" 2>/dev/null
    
    # 收集 CronJob 信息
    kubectl get cronjobs -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/app-specific/cronjobs.yaml" 2>/dev/null
    
    # 收集 Job 信息
    kubectl get jobs -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/app-specific/jobs.yaml" 2>/dev/null
    
    # 收集自定义资源（如果有）
    kubectl get crd -o name | while read crd; do
        resource_type=$(echo $crd | cut -d'/' -f2)
        kubectl get $resource_type -n ${NAMESPACE} -o yaml > "$OUTPUT_DIR/app-specific/${resource_type}.yaml" 2>/dev/null || true
    done
}

# 执行诊断命令
run_diagnostics() {
    log_info "执行诊断命令..."
    
    mkdir -p "$OUTPUT_DIR/diagnostics"
    
    # 检查 DNS 解析
    {
        echo "=== DNS 诊断 ==="
        kubectl run dns-test --image=busybox --rm -it --restart=Never -n ${NAMESPACE} -- nslookup kubernetes.default.svc.cluster.local
    } > "$OUTPUT_DIR/diagnostics/dns-test.txt" 2>&1 || true
    
    # 检查存储
    {
        echo "=== 存储诊断 ==="
        kubectl get pv
        kubectl get storageclass
    } > "$OUTPUT_DIR/diagnostics/storage-test.txt" 2>&1
    
    # 检查 RBAC
    {
        echo "=== RBAC 诊断 ==="
        kubectl get roles -n ${NAMESPACE}
        kubectl get rolebindings -n ${NAMESPACE}
        kubectl get clusterroles | grep bk-ci || true
        kubectl get clusterrolebindings | grep bk-ci || true
    } > "$OUTPUT_DIR/diagnostics/rbac-test.txt" 2>&1
}

# 生成摘要报告
generate_summary() {
    log_info "生成摘要报告..."
    
    {
        echo "BK-CI 日志收集摘要报告"
        echo "收集时间: $(date)"
        echo "命名空间: ${NAMESPACE}"
        echo "输出目录: ${OUTPUT_DIR}"
        echo "========================================"
        echo
        
        echo "Pod 状态摘要:"
        kubectl get pods -n ${NAMESPACE} --no-headers | awk '{
            total++; 
            if($3=="Running" && $2 ~ /^[0-9]+\/[0-9]+$/) {
                split($2, ready, "/");
                if(ready[1] == ready[2]) running++;
                else partial++;
            } else {
                failed++;
            }
        } END {
            print "总计: " total;
            print "运行正常: " (running ? running : 0);
            print "部分就绪: " (partial ? partial : 0);
            print "异常状态: " (failed ? failed : 0);
        }'
        echo
        
        echo "最近的警告事件:"
        kubectl get events -n ${NAMESPACE} --field-selector type=Warning --sort-by='.lastTimestamp' | tail -5
        echo
        
        echo "收集的文件列表:"
        find "$OUTPUT_DIR" -type f -name "*.txt" -o -name "*.log" -o -name "*.yaml" | sort
        echo
        
        echo "收集完成时间: $(date)"
        
    } > "$OUTPUT_DIR/SUMMARY.txt"
}

# 压缩日志文件
compress_logs() {
    log_info "压缩日志文件..."
    
    archive_name="bk-ci-logs-$(date +%Y%m%d-%H%M%S).tar.gz"
    tar -czf "$archive_name" -C "$(dirname "$OUTPUT_DIR")" "$(basename "$OUTPUT_DIR")"
    
    log_success "日志已压缩为: $archive_name"
    log_info "可以使用以下命令解压: tar -xzf $archive_name"
}

# 清理临时文件
cleanup() {
    if [ "$KEEP_TEMP" != "true" ]; then
        log_info "清理临时文件..."
        rm -rf "$OUTPUT_DIR"
    else
        log_info "保留临时目录: $OUTPUT_DIR"
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
BK-CI 日志收集脚本

用法: $0 [选项]

选项:
  -n, --namespace NAMESPACE    指定 Kubernetes 命名空间 (默认: bk-ci)
  -o, --output OUTPUT_DIR      指定输出目录 (默认: ./bk-ci-logs-TIMESTAMP)
  -l, --lines LOG_LINES        指定收集的日志行数 (默认: 1000)
  -t, --time TIME_RANGE        指定时间范围 (默认: 1h)
  -k, --keep-temp              保留临时目录，不压缩
  -h, --help                   显示此帮助信息

示例:
  $0                           # 使用默认参数收集日志
  $0 -n bk-ci-prod -l 5000     # 收集生产环境 5000 行日志
  $0 -o /tmp/logs -k           # 输出到指定目录并保留临时文件

EOF
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            -o|--output)
                OUTPUT_DIR="$2"
                shift 2
                ;;
            -l|--lines)
                LOG_LINES="$2"
                shift 2
                ;;
            -t|--time)
                TIME_RANGE="$2"
                shift 2
                ;;
            -k|--keep-temp)
                KEEP_TEMP="true"
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 主函数
main() {
    parse_args "$@"
    
    echo "========================================"
    echo "BK-CI 日志收集工具"
    echo "========================================"
    echo "命名空间: $NAMESPACE"
    echo "输出目录: $OUTPUT_DIR"
    echo "日志行数: $LOG_LINES"
    echo "========================================"
    
    # 检查 kubectl 命令
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 命令未找到，请先安装 kubectl"
        exit 1
    fi
    
    # 检查 Kubernetes 连接
    if ! kubectl cluster-info &> /dev/null; then
        log_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
    
    # 检查命名空间
    if ! kubectl get namespace ${NAMESPACE} &> /dev/null; then
        log_error "命名空间 ${NAMESPACE} 不存在"
        exit 1
    fi
    
    create_output_dir
    collect_pod_logs
    collect_pod_describe
    collect_service_info
    collect_events
    collect_configs
    collect_resource_usage
    collect_network_info
    collect_app_specific_info
    run_diagnostics
    generate_summary
    
    if [ "$KEEP_TEMP" != "true" ]; then
        compress_logs
        cleanup
    else
        log_success "日志收集完成，输出目录: $OUTPUT_DIR"
    fi
    
    echo "========================================"
    log_success "日志收集完成"
    echo "========================================"
}

# 执行主函数
main "$@"