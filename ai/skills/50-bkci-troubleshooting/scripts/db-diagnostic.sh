#!/bin/bash
# BK-CI 数据库诊断脚本
# 用于检查数据库连接、性能和常见问题

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 配置
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_section() { echo -e "\n${BLUE}=== $1 ===${NC}"; }

# MySQL 命令封装
mysql_cmd() {
    mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "$1" 2>/dev/null
}

# 检查连接
check_connection() {
    log_section "数据库连接检查"
    
    if mysql_cmd "SELECT 1" > /dev/null 2>&1; then
        log_info "数据库连接正常"
        
        # 获取版本信息
        VERSION=$(mysql_cmd "SELECT VERSION()" | tail -1)
        log_info "MySQL 版本: $VERSION"
        
        # 获取运行时间
        UPTIME=$(mysql_cmd "SHOW STATUS LIKE 'Uptime'" | awk '{print $2}' | tail -1)
        log_info "运行时间: $((UPTIME/86400)) 天 $((UPTIME%86400/3600)) 小时"
    else
        log_error "无法连接到数据库"
        exit 1
    fi
}

# 检查数据库状态
check_databases() {
    log_section "BK-CI 数据库状态"
    
    DATABASES=("devops_ci_project" "devops_ci_process" "devops_ci_repository" 
               "devops_ci_dispatch" "devops_ci_store" "devops_ci_artifactory"
               "devops_ci_environment" "devops_ci_ticket" "devops_ci_quality"
               "devops_ci_notify" "devops_ci_log" "devops_ci_auth")
    
    for db in "${DATABASES[@]}"; do
        if mysql_cmd "USE $db" > /dev/null 2>&1; then
            TABLE_COUNT=$(mysql_cmd "SELECT COUNT(*) FROM information_schema.tables \
                WHERE table_schema='$db'" | tail -1)
            SIZE=$(mysql_cmd "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) \
                FROM information_schema.tables WHERE table_schema='$db'" | tail -1)
            log_info "$db: $TABLE_COUNT 表, ${SIZE:-0} MB"
        else
            log_warn "$db: 不存在"
        fi
    done
}

# 检查连接池状态
check_connections() {
    log_section "连接状态"
    
    # 当前连接数
    CURRENT=$(mysql_cmd "SHOW STATUS LIKE 'Threads_connected'" | awk '{print $2}' | tail -1)
    MAX=$(mysql_cmd "SHOW VARIABLES LIKE 'max_connections'" | awk '{print $2}' | tail -1)
    
    log_info "当前连接: $CURRENT / $MAX"
    
    USAGE=$((CURRENT * 100 / MAX))
    if [ "$USAGE" -gt 80 ]; then
        log_warn "连接使用率过高: ${USAGE}%"
    fi
    
    # 按用户统计连接
    log_info "按用户统计连接:"
    mysql_cmd "SELECT user, COUNT(*) as count FROM information_schema.processlist \
        GROUP BY user ORDER BY count DESC LIMIT 10"
}

# 检查慢查询
check_slow_queries() {
    log_section "慢查询分析"
    
    SLOW_ENABLED=$(mysql_cmd "SHOW VARIABLES LIKE 'slow_query_log'" | awk '{print $2}' | tail -1)
    SLOW_TIME=$(mysql_cmd "SHOW VARIABLES LIKE 'long_query_time'" | awk '{print $2}' | tail -1)
    
    log_info "慢查询日志: $SLOW_ENABLED"
    log_info "慢查询阈值: ${SLOW_TIME}s"
    
    # 慢查询统计
    SLOW_COUNT=$(mysql_cmd "SHOW STATUS LIKE 'Slow_queries'" | awk '{print $2}' | tail -1)
    log_info "慢查询总数: $SLOW_COUNT"
}

# 检查锁状态
check_locks() {
    log_section "锁状态检查"
    
    # InnoDB 锁等待
    LOCK_WAITS=$(mysql_cmd "SELECT COUNT(*) FROM information_schema.innodb_lock_waits" \
        2>/dev/null | tail -1)
    
    if [ "${LOCK_WAITS:-0}" -gt 0 ]; then
        log_warn "存在 $LOCK_WAITS 个锁等待"
        mysql_cmd "SELECT * FROM information_schema.innodb_lock_waits LIMIT 5" 2>/dev/null
    else
        log_info "无锁等待"
    fi
    
    # 长事务检查
    LONG_TRX=$(mysql_cmd "SELECT COUNT(*) FROM information_schema.innodb_trx \
        WHERE TIME_TO_SEC(TIMEDIFF(NOW(), trx_started)) > 60" 2>/dev/null | tail -1)
    
    if [ "${LONG_TRX:-0}" -gt 0 ]; then
        log_warn "存在 $LONG_TRX 个长事务(>60s)"
    fi
}

# 检查表状态
check_tables() {
    log_section "关键表状态"
    
    # 检查 process 模块核心表
    TABLES=(
        "devops_ci_process.T_PIPELINE_INFO"
        "devops_ci_process.T_PIPELINE_BUILD_HISTORY"
        "devops_ci_process.T_PIPELINE_BUILD_TASK"
        "devops_ci_project.T_PROJECT"
    )
    
    for table in "${TABLES[@]}"; do
        DB=$(echo "$table" | cut -d'.' -f1)
        TBL=$(echo "$table" | cut -d'.' -f2)
        
        COUNT=$(mysql_cmd "SELECT COUNT(*) FROM $table" 2>/dev/null | tail -1)
        if [ -n "$COUNT" ]; then
            log_info "$table: $COUNT 行"
        fi
    done
}

# 检查复制状态
check_replication() {
    log_section "复制状态"
    
    SLAVE_STATUS=$(mysql_cmd "SHOW SLAVE STATUS\G" 2>/dev/null)
    
    if [ -n "$SLAVE_STATUS" ]; then
        IO_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_IO_Running" | awk '{print $2}')
        SQL_RUNNING=$(echo "$SLAVE_STATUS" | grep "Slave_SQL_Running" | awk '{print $2}')
        SECONDS_BEHIND=$(echo "$SLAVE_STATUS" | grep "Seconds_Behind_Master" | awk '{print $2}')
        
        log_info "IO 线程: $IO_RUNNING"
        log_info "SQL 线程: $SQL_RUNNING"
        log_info "复制延迟: ${SECONDS_BEHIND}s"
        
        if [ "$IO_RUNNING" != "Yes" ] || [ "$SQL_RUNNING" != "Yes" ]; then
            log_error "复制异常!"
        fi
    else
        log_info "非从库或未配置复制"
    fi
}

# 性能指标
check_performance() {
    log_section "性能指标"
    
    # Buffer Pool 命中率
    READS=$(mysql_cmd "SHOW STATUS LIKE 'Innodb_buffer_pool_reads'" | awk '{print $2}' | tail -1)
    READ_REQUESTS=$(mysql_cmd "SHOW STATUS LIKE 'Innodb_buffer_pool_read_requests'" \
        | awk '{print $2}' | tail -1)
    
    if [ "$READ_REQUESTS" -gt 0 ]; then
        HIT_RATE=$(echo "scale=2; (1 - $READS / $READ_REQUESTS) * 100" | bc)
        log_info "Buffer Pool 命中率: ${HIT_RATE}%"
        
        if [ "$(echo "$HIT_RATE < 95" | bc)" -eq 1 ]; then
            log_warn "Buffer Pool 命中率偏低，建议增加 innodb_buffer_pool_size"
        fi
    fi
    
    # QPS
    QUESTIONS=$(mysql_cmd "SHOW STATUS LIKE 'Questions'" | awk '{print $2}' | tail -1)
    UPTIME=$(mysql_cmd "SHOW STATUS LIKE 'Uptime'" | awk '{print $2}' | tail -1)
    QPS=$(echo "scale=2; $QUESTIONS / $UPTIME" | bc)
    log_info "平均 QPS: $QPS"
}

# 主函数
main() {
    echo "========================================"
    echo "  BK-CI 数据库诊断工具"
    echo "  时间: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================"
    
    check_connection
    check_databases
    check_connections
    check_slow_queries
    check_locks
    check_tables
    check_replication
    check_performance
    
    log_section "诊断完成"
}

main "$@"
