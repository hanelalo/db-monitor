#!/bin/bash

# 监控配置管理系统演示脚本
# 此脚本演示如何使用监控配置管理系统的API

BASE_URL="http://localhost:8080"
API_BASE="${BASE_URL}/api/monitor-config"

echo "=== 监控配置管理系统演示 ==="
echo "Base URL: $BASE_URL"
echo ""

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务是否启动
check_service() {
    log_info "检查服务状态..."
    if curl -s --connect-timeout 5 "$BASE_URL" > /dev/null; then
        log_info "服务正常运行"
        return 0
    else
        log_error "服务未启动或无法连接到 $BASE_URL"
        return 1
    fi
}

# 1. 自动检测时间字段
detect_time_columns() {
    log_info "1. 自动检测时间字段..."
    
    # 假设有一个user表
    TABLE_NAME="user"
    DATA_SOURCE="primary"
    
    echo "检测表 $TABLE_NAME 的时间字段..."
    
    response=$(curl -s -X GET "${API_BASE}/detect-time-columns?dataSourceName=${DATA_SOURCE}&tableName=${TABLE_NAME}")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "时间字段检测成功"
        echo "$response" | jq '.'
    else
        log_warn "时间字段检测失败或表不存在"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 2. 创建监控配置
create_config() {
    log_info "2. 创建监控配置..."
    
    # 创建用户表监控配置
    CONFIG_DATA='{
        "configName": "demo_user_table_monitor",
        "dataSourceName": "primary",
        "tableName": "user",
        "timeColumnName": "created_time",
        "timeColumnType": "DATETIME",
        "enabled": true,
        "intervalType": "MINUTES",
        "intervalValue": 10,
        "description": "演示用户表监控配置",
        "createdBy": "demo"
    }'
    
    echo "创建监控配置: demo_user_table_monitor"
    
    response=$(curl -s -X POST "${API_BASE}" \
        -H "Content-Type: application/json" \
        -d "$CONFIG_DATA")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "监控配置创建成功"
        echo "$response" | jq '.'
        # 提取配置ID
        CONFIG_ID=$(echo "$response" | jq -r '.data.id')
        echo "配置ID: $CONFIG_ID"
    else
        log_error "监控配置创建失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 3. 获取所有监控配置
get_all_configs() {
    log_info "3. 获取所有监控配置..."
    
    response=$(curl -s -X GET "${API_BASE}")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "获取配置列表成功"
        echo "$response" | jq '.'
    else
        log_error "获取配置列表失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 4. 测试监控配置
test_config() {
    log_info "4. 测试监控配置..."
    
    TEST_DATA='{
        "dataSourceName": "primary",
        "tableName": "user",
        "timeColumnName": "created_time",
        "timeColumnType": "DATETIME"
    }'
    
    echo "测试配置有效性..."
    
    response=$(curl -s -X POST "${API_BASE}/test" \
        -H "Content-Type: application/json" \
        -d "$TEST_DATA")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置测试成功"
        echo "$response" | jq '.'
    else
        log_warn "配置测试失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 5. 更新监控配置
update_config() {
    log_info "5. 更新监控配置..."
    
    if [ -z "$CONFIG_ID" ]; then
        log_warn "未找到配置ID，跳过更新操作"
        return
    fi
    
    UPDATE_DATA='{
        "configName": "demo_user_table_monitor_updated",
        "dataSourceName": "primary",
        "tableName": "user",
        "timeColumnName": "updated_time",
        "timeColumnType": "DATETIME",
        "enabled": true,
        "intervalType": "HOURS",
        "intervalValue": 1,
        "description": "更新的演示用户表监控配置",
        "updatedBy": "demo"
    }'
    
    echo "更新配置ID: $CONFIG_ID"
    
    response=$(curl -s -X PUT "${API_BASE}/${CONFIG_ID}" \
        -H "Content-Type: application/json" \
        -d "$UPDATE_DATA")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置更新成功"
        echo "$response" | jq '.'
    else
        log_error "配置更新失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 6. 禁用监控配置
disable_config() {
    log_info "6. 禁用监控配置..."
    
    if [ -z "$CONFIG_ID" ]; then
        log_warn "未找到配置ID，跳过禁用操作"
        return
    fi
    
    echo "禁用配置ID: $CONFIG_ID"
    
    response=$(curl -s -X PUT "${API_BASE}/${CONFIG_ID}/enabled?enabled=false&updatedBy=demo")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置禁用成功"
        echo "$response" | jq '.'
    else
        log_error "配置禁用失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 7. 启用监控配置
enable_config() {
    log_info "7. 启用监控配置..."
    
    if [ -z "$CONFIG_ID" ]; then
        log_warn "未找到配置ID，跳过启用操作"
        return
    fi
    
    echo "启用配置ID: $CONFIG_ID"
    
    response=$(curl -s -X PUT "${API_BASE}/${CONFIG_ID}/enabled?enabled=true&updatedBy=demo")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置启用成功"
        echo "$response" | jq '.'
    else
        log_error "配置启用失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 8. 获取启用的监控配置
get_enabled_configs() {
    log_info "8. 获取启用的监控配置..."
    
    response=$(curl -s -X GET "${API_BASE}?enabled=true")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "获取启用配置成功"
        echo "$response" | jq '.'
    else
        log_error "获取启用配置失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 9. 根据配置名称查找
find_config_by_name() {
    log_info "9. 根据配置名称查找..."
    
    CONFIG_NAME="demo_user_table_monitor_updated"
    
    echo "查找配置: $CONFIG_NAME"
    
    response=$(curl -s -X GET "${API_BASE}/by-name/${CONFIG_NAME}")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置查找成功"
        echo "$response" | jq '.'
    else
        log_warn "配置查找失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 10. 删除监控配置
delete_config() {
    log_info "10. 删除监控配置..."
    
    if [ -z "$CONFIG_ID" ]; then
        log_warn "未找到配置ID，跳过删除操作"
        return
    fi
    
    echo "删除配置ID: $CONFIG_ID"
    
    response=$(curl -s -X DELETE "${API_BASE}/${CONFIG_ID}")
    
    if echo "$response" | grep -q '"success":true'; then
        log_info "配置删除成功"
        echo "$response" | jq '.'
    else
        log_error "配置删除失败"
        echo "$response" | jq '.'
    fi
    
    echo ""
}

# 批量操作演示
batch_operations() {
    log_info "11. 批量操作演示..."
    
    # 创建多个配置
    log_info "创建多个测试配置..."
    
    configs=("order" "product" "payment")
    config_ids=()
    
    for table in "${configs[@]}"; do
        echo "创建 ${table} 表监控配置..."
        
        batch_config='{
            "configName": "demo_'${table}'_table_monitor",
            "dataSourceName": "primary",
            "tableName": "'${table}'",
            "timeColumnName": "created_time",
            "timeColumnType": "DATETIME",
            "enabled": true,
            "intervalType": "MINUTES",
            "intervalValue": 15,
            "description": "演示'${table}'表监控配置",
            "createdBy": "demo"
        }'
        
        response=$(curl -s -X POST "${API_BASE}" \
            -H "Content-Type: application/json" \
            -d "$batch_config")
        
        if echo "$response" | grep -q '"success":true'; then
            id=$(echo "$response" | jq -r '.data.id')
            config_ids+=("$id")
            log_info "${table} 表配置创建成功，ID: $id"
        else
            log_warn "${table} 表配置创建失败"
        fi
    done
    
    # 批量禁用
    if [ ${#config_ids[@]} -gt 0 ]; then
        log_info "批量禁用配置..."
        
        ids_json=$(printf '%s\n' "${config_ids[@]}" | jq -R . | jq -s .)
        
        batch_data='{
            "ids": '${ids_json}',
            "enabled": false,
            "updatedBy": "demo"
        }'
        
        response=$(curl -s -X PUT "${API_BASE}/batch/enabled" \
            -H "Content-Type: application/json" \
            -d "$batch_data")
        
        if echo "$response" | grep -q '"success":true'; then
            log_info "批量禁用成功"
            echo "$response" | jq '.'
        else
            log_error "批量禁用失败"
            echo "$response" | jq '.'
        fi
        
        # 批量删除
        log_info "批量删除测试配置..."
        for id in "${config_ids[@]}"; do
            curl -s -X DELETE "${API_BASE}/${id}" > /dev/null
            log_info "删除配置ID: $id"
        done
    fi
    
    echo ""
}

# 主函数
main() {
    echo "开始监控配置管理系统演示..."
    echo ""
    
    # 检查依赖
    if ! command -v curl &> /dev/null; then
        log_error "curl 命令未找到，请安装 curl"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        log_error "jq 命令未找到，请安装 jq"
        exit 1
    fi
    
    # 检查服务状态
    if ! check_service; then
        log_error "请确保服务正在运行"
        exit 1
    fi
    
    echo ""
    
    # 执行演示步骤
    detect_time_columns
    create_config
    get_all_configs
    test_config
    update_config
    disable_config
    enable_config
    get_enabled_configs
    find_config_by_name
    batch_operations
    delete_config
    
    log_info "演示完成!"
    echo ""
    echo "=== 演示总结 ==="
    echo "✅ 自动检测时间字段"
    echo "✅ 创建监控配置"
    echo "✅ 获取配置列表"
    echo "✅ 测试配置有效性"
    echo "✅ 更新配置"
    echo "✅ 启用/禁用配置"
    echo "✅ 查找配置"
    echo "✅ 批量操作"
    echo "✅ 删除配置"
    echo ""
    echo "监控配置管理系统所有功能演示完成！"
    echo "请查看API文档了解更多详细信息：MONITOR_CONFIG_GUIDE.md"
}

# 脚本入口
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi