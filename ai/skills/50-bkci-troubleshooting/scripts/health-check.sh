#!/bin/bash

# BK-CI 系统健康检查脚本
# 用于快速检查 BK-CI 各组件的运行状态

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置参数
NAMESPACE=${NAMESPACE:-"bk-ci"}
TIMEOUT=${TIMEOUT:-30}

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

# 检查 kubectl 命令
check_kubectl() {
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 命令未找到，请先安装 kubectl"
        exit 1
    fi
    log_success "kubectl 命令可用"
}

# 检查 Kubernetes 连接
check_k8s_connection() {
    log_info "检查 Kubernetes 集群连接..."
    if kubectl cluster-info &> /dev/null; then
        log_success "Kubernetes 集群连接正常"
    else
        log_error "无法连接到 Kubernetes 集群"
        exit 1
    fi
}

# 检查命名空间
check_namespace() {
    log_info "检查命名空间 ${NAMESPACE}..."
    if kubectl get namespace ${NAMESPACE} &> /dev/null; then
        log_success "命名空间 ${NAMESPACE} 存在"
    else
        log_error "命名空间 ${NAMESPACE} 不存在"
        exit 1
    fi
}

# 检查 Pod 状态
check_pods() {
    log_info "检查 Pod 状态..."
    
    # 获取所有 Pod 状态
    pods=$(kubectl get pods -n ${NAMESPACE} --no-headers)
    
    if [ -z "$pods" ]; then
        log_warning "命名空间 ${NAMESPACE} 中没有找到 Pod"
        return
    fi
    
    echo "$pods" | while read line; do
        name=$(echo $line | awk '{print $1}')
        ready=$(echo $line | awk '{print $2}')
        status=$(echo $line | awk '{print $3}')
        restarts=$(echo $line | awk '{print $4}')
        age=$(echo $line | awk '{print $5}')
        
        if [[ "$status" == "Running" && "$ready" =~ ^[0-9]+/[0-9]+$ ]]; then
            ready_count=$(echo $ready | cut -d'/' -f1)
            total_count=$(echo $ready | cut -d'/' -f2)
            if [ "$ready_count" -eq "$total_count" ]; then
                log_success "Pod $name 运行正常 ($ready)"
            else
                log_warning "Pod $name 部分容器未就绪 ($ready)"
            fi
        else
            log_error "Pod $name 状态异常: $status ($ready)"
        fi
        
        # 检查重启次数
        if [ "$restarts" -gt 5 ]; then
            log_warning "Pod $name 重启次数过多: $restarts"
        fi
    done
}

# 检查服务状态
check_services() {
    log_info "检查服务状态..."
    
    services=$(kubectl get services -n ${NAMESPACE} --no-headers)
    
    if [ -z "$services" ]; then
        log_warning "命名空间 ${NAMESPACE} 中没有找到服务"
        return
    fi
    
    echo "$services" | while read line; do
        name=$(echo $line | awk '{print $1}')
        type=$(echo $line | awk '{print $2}')
        cluster_ip=$(echo $line | awk '{print $3}')
        external_ip=$(echo $line | awk '{print $4}')
        ports=$(echo $line | awk '{print $5}')
        
        log_success "服务 $name 可用 (类型: $type, 端口: $ports)"
    done
}

# 检查 Ingress 状态
check_ingress() {
    log_info "检查 Ingress 状态..."
    
    ingresses=$(kubectl get ingress -n ${NAMESPACE} --no-headers 2>/dev/null || true)
    
    if [ -z "$ingresses" ]; then
        log_info "没有找到 Ingress 资源"
        return
    fi
    
    echo "$ingresses" | while read line; do
        name=$(echo $line | awk '{print $1}')
        hosts=$(echo $line | awk '{print $2}')
        address=$(echo $line | awk '{print $3}')
        
        if [ "$address" != "<none>" ] && [ -n "$address" ]; then
            log_success "Ingress $name 可用 (主机: $hosts, 地址: $address)"
        else
            log_warning "Ingress $name 地址未分配 (主机: $hosts)"
        fi
    done
}

# 检查 PVC 状态
check_pvc() {
    log_info "检查 PVC 状态..."
    
    pvcs=$(kubectl get pvc -n ${NAMESPACE} --no-headers 2>/dev/null || true)
    
    if [ -z "$pvcs" ]; then
        log_info "没有找到 PVC 资源"
        return
    fi
    
    echo "$pvcs" | while read line; do
        name=$(echo $line | awk '{print $1}')
        status=$(echo $line | awk '{print $2}')
        volume=$(echo $line | awk '{print $3}')
        capacity=$(echo $line | awk '{print $4}')
        
        if [ "$status" == "Bound" ]; then
            log_success "PVC $name 已绑定 (卷: $volume, 容量: $capacity)"
        else
            log_error "PVC $name 状态异常: $status"
        fi
    done
}

# 检查资源使用情况
check_resource_usage() {
    log_info "检查资源使用情况..."
    
    # 检查节点资源
    log_info "节点资源使用情况:"
    kubectl top nodes 2>/dev/null || log_warning "无法获取节点资源使用情况 (需要 metrics-server)"
    
    # 检查 Pod 资源
    log_info "Pod 资源使用情况:"
    kubectl top pods -n ${NAMESPACE} 2>/dev/null || log_warning "无法获取 Pod 资源使用情况 (需要 metrics-server)"
}

# 检查网络连通性
check_network_connectivity() {
    log_info "检查网络连通性..."
    
    # 获取一个运行中的 Pod 进行网络测试
    pod=$(kubectl get pods -n ${NAMESPACE} --field-selector=status.phase=Running -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
    
    if [ -z "$pod" ]; then
        log_warning "没有找到运行中的 Pod，跳过网络连通性检查"
        return
    fi
    
    log_info "使用 Pod $pod 进行网络测试..."
    
    # 测试 DNS 解析
    if kubectl exec -n ${NAMESPACE} $pod -- nslookup kubernetes.default.svc.cluster.local &>/dev/null; then
        log_success "DNS 解析正常"
    else
        log_error "DNS 解析失败"
    fi
    
    # 测试集群内服务连通性
    services=$(kubectl get services -n ${NAMESPACE} -o jsonpath='{.items[*].metadata.name}')
    for service in $services; do
        if kubectl exec -n ${NAMESPACE} $pod -- nc -z $service 80 &>/dev/null; then
            log_success "服务 $service 网络连通正常"
        else
            log_warning "服务 $service 网络连通异常或端口不是 80"
        fi
    done
}

# 检查事件
check_events() {
    log_info "检查最近的事件..."
    
    events=$(kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' --no-headers | tail -10)
    
    if [ -z "$events" ]; then
        log_info "没有找到最近的事件"
        return
    fi
    
    echo "$events" | while read line; do
        type=$(echo $line | awk '{print $2}')
        reason=$(echo $line | awk '{print $3}')
        object=$(echo $line | awk '{print $4}')
        message=$(echo $line | cut -d' ' -f6-)
        
        if [ "$type" == "Warning" ] || [ "$type" == "Error" ]; then
            log_warning "事件: $object - $reason: $message"
        else
            log_info "事件: $object - $reason: $message"
        fi
    done
}

# 生成健康报告
generate_health_report() {
    log_info "生成健康检查报告..."
    
    report_file="bk-ci-health-report-$(date +%Y%m%d-%H%M%S).txt"
    
    {
        echo "BK-CI 健康检查报告"
        echo "生成时间: $(date)"
        echo "命名空间: ${NAMESPACE}"
        echo "========================================"
        echo
        
        echo "Pod 状态:"
        kubectl get pods -n ${NAMESPACE}
        echo
        
        echo "服务状态:"
        kubectl get services -n ${NAMESPACE}
        echo
        
        echo "资源使用情况:"
        kubectl top pods -n ${NAMESPACE} 2>/dev/null || echo "无法获取资源使用情况"
        echo
        
        echo "最近事件:"
        kubectl get events -n ${NAMESPACE} --sort-by='.lastTimestamp' | tail -20
        echo
        
    } > $report_file
    
    log_success "健康检查报告已生成: $report_file"
}

# 主函数
main() {
    echo "========================================"
    echo "BK-CI 系统健康检查"
    echo "========================================"
    
    check_kubectl
    check_k8s_connection
    check_namespace
    check_pods
    check_services
    check_ingress
    check_pvc
    check_resource_usage
    check_network_connectivity
    check_events
    generate_health_report
    
    echo "========================================"
    log_success "健康检查完成"
    echo "========================================"
}

# 执行主函数
main "$@"