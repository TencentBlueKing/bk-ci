#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a
BK_PKG_SRC_PATH=${BK_PKG_SRC_PATH:-/data/src}
BK_CI_SRC_DIR=${BK_CI_SRC_DIR:-$BK_PKG_SRC_PATH/ci}
cd "$CTRL_DIR"

pcmd (){
  local PCMD_TIMEOUT=${PCMD_TIMEOUT:-1200}
  timeout "$PCMD_TIMEOUT" "$CTRL_DIR/pcmd.sh" "$@" || {
    local ret=$?
    [ $ret -ne 124 ] || echo "pcmd 执行超时(PCMD_TIMEOUT=${PCMD_TIMEOUT})"
    echo "$BASH_SOURCE:$BASH_LINENO 调用pcmd时返回 $ret，中控机调试命令如下:"
    printf " %q" "$CTRL_DIR/pcmd.sh" "$@"
    printf "\n"
    return $ret
  }
}

echo "注册 蓝鲸 ESB"
./bin/add_or_update_appcode.sh "$BK_CI_APP_CODE" "$BK_CI_APP_TOKEN" "蓝盾" "mysql-paas"  # 注册app。第4个参数即是login-path。

echo "导入 IAMv3 权限模板."
./bin/bkiam_migrate.sh -t "$BK_IAM_PRIVATE_URL" -a "$BK_CI_APP_CODE" -s "$BK_CI_APP_TOKEN" "$BK_CI_SRC_DIR"/support-files/bkiam/*.json

echo "配置数据库访问权限."
# 中控机配置mysql login-path
./bin/setup_mysql_loginpath.sh -n mysql-ci -h "${BK_CI_MYSQL_ADDR%:*}" -u "$BK_CI_MYSQL_USER" -p "$BK_CI_MYSQL_PASSWORD"
sleep 1
# 如果使用了蓝鲸默认的数据库, 则自动刷新权限.
if [ "${BK_CI_MYSQL_ADDR%:*}" = "$BK_MYSQL_IP" ]; then
  echo "复用蓝鲸MySQL, 自动创建账户并授权给中控机和$BK_CI_IP_COMMA."
  # 提前渲染变量, 避免mysql服务端没有ci的变量.
  pcmd -H "$BK_MYSQL_IP" "$CTRL_DIR/bin/grant_mysql_priv.sh -n default-root -u \"$BK_CI_MYSQL_USER\" -p \"$BK_CI_MYSQL_PASSWORD\" -H \"$(< "$CTRL_DIR/.controller_ip")\""
  pcmd -H "$BK_MYSQL_IP" "$CTRL_DIR/bin/grant_mysql_priv.sh -n default-root -u \"$BK_CI_MYSQL_USER\" -p \"$BK_CI_MYSQL_PASSWORD\" -H \"$BK_CI_IP_COMMA\""
else
  echo "你使用了自定义MySQL. 请自行保障CI全部节点能访问数据库."
fi

echo "导入 SQL 文件"
./bin/sql_migrate.sh -n mysql-ci "$BK_CI_SRC_DIR"/support-files/sql/*.sql
for sub_dir in "$BK_CI_SRC_DIR"/support-files/sql/*
do
   if [[ -d $sub_dir ]]; then
     echo "import sub_dir $sub_dir/*.sql"
     ./bin/sql_migrate.sh -n mysql-ci "$sub_dir/*.sql"
   fi
done

echo "RabbitMQ"
install_mq_plugin=
rabbitmq_plugin_dir="/usr/lib/rabbitmq/plugins/"
rabbitmq_plugin_name="rabbitmq_delayed_message_exchange"
rabbitmq_plugin_src="$BK_PKG_SRC_PATH/rabbitmq_delayed_message_exchange-3.8.0.ez"
rabbitmq_plugin_url="https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.8.0/rabbitmq_delayed_message_exchange-3.8.0.ez"
# 如果使用了蓝鲸默认的rabbitmq, 则自动刷新权限.
if [ "${BK_CI_RABBITMQ_ADDR%:*}" = "$BK_RABBITMQ_IP" ]; then
  echo "复用蓝鲸RabbitMQ, 自动创建vhost及账户."
  install_mq_plugin=1
  pcmd -H "$BK_RABBITMQ_IP" "$CTRL_DIR/bin/add_rabbitmq_user.sh -u \"$BK_CI_RABBITMQ_USER\" -p \"$BK_CI_RABBITMQ_PASSWORD\" -h \"$BK_CI_RABBITMQ_VHOST\"; rabbitmqctl change_password \"$BK_CI_RABBITMQ_USER\" \"$BK_CI_RABBITMQ_PASSWORD\""
else
  echo "你使用了自定义RabbitMQ, 请自行创建vhost, 用户. 并安装 $rabbitmq_plugin_name 插件."
fi
# 处理rabbitmq插件.
down_mq_plugin (){
  echo "参考下载地址: $rabbitmq_plugin_url"
  if ! [ -f "$rabbitmq_plugin_src" ]; then
    echo curl -vkfLo "$rabbitmq_plugin_src" "$rabbitmq_plugin_url"
    curl -vkfLo "$rabbitmq_plugin_src" "$rabbitmq_plugin_url" || return $?
  fi
  return 0
}
enable_mq_plugin (){
  echo " 服务端启用 $rabbitmq_plugin_name 插件."
  chmod 644 "$rabbitmq_plugin_src"
  ./sync.sh rabbitmq "$rabbitmq_plugin_src" "$BK_PKG_SRC_PATH/" || return 11
  pcmd -m rabbitmq "mkdir -p \"$rabbitmq_plugin_dir\" && cp -a \"$rabbitmq_plugin_src\" \"$rabbitmq_plugin_dir\" && ls -la \"$rabbitmq_plugin_dir\"" || return 12
  pcmd -m rabbitmq "rabbitmq-plugins enable $rabbitmq_plugin_name" || return 13
}
check_mq_plugin (){
  pcmd -m rabbitmq "rabbitmq-plugins list | grep $rabbitmq_plugin_name | grep -F 'E*'" &>/dev/null
}
if [ -n "$install_mq_plugin" ]; then
  echo " 检查所有服务端的 $rabbitmq_plugin_name 插件是否启用"
  if check_mq_plugin; then
    echo "RabbitMQ插件 $rabbitmq_plugin_name 已经启用."
  else
    echo "RabbitMQ插件 $rabbitmq_plugin_name 未启用. 尝试自动下载并安装."
    down_mq_plugin
    enable_mq_plugin
    if check_mq_plugin; then
      echo "RabbitMQ插件 $rabbitmq_plugin_name 已经自动启用."
    else
      echo "RabbitMQ插件 $rabbitmq_plugin_name 未能自动启用, 请手动处理."
      exit 23
    fi
  fi
fi

# 不影响部署结果, 如果报错可以忽略:
echo "注册 PaaS 桌面图标"
"$BK_CI_SRC_DIR/scripts/bk-ci-reg-paas-app.sh" || {
  echo "未能注册PaaS桌面图标. 您可以忽略此步骤."
  exit 1
}

