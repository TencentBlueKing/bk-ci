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
BK_CODECC_SRC_DIR=${BK_CODECC_SRC_DIR:-$BK_PKG_SRC_PATH/codecc}
cd "$CTRL_DIR"
source ./functions

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
./bin/add_or_update_appcode.sh "$BK_CODECC_APP_CODE" "$BK_CODECC_APP_TOKEN" "蓝盾" "mysql-paas"  # 注册app。第4个参数即是login-path。

echo "导入 IAMv3 权限模板."
./bin/bkiam_migrate.sh -t "$BK_IAM_PRIVATE_URL" -a "$BK_CODECC_APP_CODE" -s "$BK_CODECC_APP_TOKEN" "$BK_CODECC_SRC_DIR"/support-files/bkiam/*.json

echo "MongoDB"
echo "create mongodb db:"
BK_CODECC_MONGODB_HOST=${BK_CODECC_MONGODB_ADDR%:*}
BK_CODECC_MONGODB_PORT=${BK_CODECC_MONGODB_ADDR#*:}
for db in db_task db_defect db_schedule db_op db_quartz; do
  # DELETE DB! use with caution.
  # mongo --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} -u ${BK_MONGODB_ADMIN_USER} -p ${BK_MONGODB_ADMIN_PASSWORD} --authenticationDatabase admin "$db" <<< "db.dropDatabase()"
  # 尝试创建用户并强制修改密码.
  pcmd -H $BK_MONGODB_IP0 "${CTRL_DIR}/bin/add_mongodb_user.sh -d '${db}' -i mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@\$LAN_IP:27017/admin -u '$BK_CODECC_MONGODB_USER' -p '$BK_CODECC_MONGODB_PASSWORD'; mongo mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@\$LAN_IP:27017/admin <<EOF
use $db;
db.changeUserPassword('$BK_CODECC_MONGODB_USER', '$BK_CODECC_MONGODB_PASSWORD');
EOF"
done
echo "import mongodb json:"
patt_mongo_json_filename="^[0-9]{4,4}_codecc_(db_[a-z0-9]+)_(t_[a-z0-9_]+)_mongo.json$"
for mongo_json in "$BK_CODECC_SRC_DIR"/support-files/nosql/*.json; do
  read mongo_json_db mongo_json_coll < <(
    sed -nr "s/$patt_mongo_json_filename/\1 \2/p" <<< "${mongo_json##*/}"
  ) || true
  # 提前根据文件名创建collections.
  if [ -n "${mongo_json_db:-}" ] && [ -n "${mongo_json_coll:-}" ]; then
    echo "import data to $mongo_json_db.$mongo_json_coll from file: $mongo_json."
    mongoimport --host ${BK_CODECC_MONGODB_HOST} --port ${BK_CODECC_MONGODB_PORT} \
      -u ${BK_CODECC_MONGODB_USER} -p ${BK_CODECC_MONGODB_PASSWORD} \
      -d "$mongo_json_db" -c "$mongo_json_coll" --mode=upsert --file="$mongo_json"
  else
    echo "ignore illegal filename: $mongo_json. it should match regex: $patt_mongo_json_filename."
  fi
done

echo "RabbitMQ"
install_mq_plugin=
rabbitmq_plugin_dir="/usr/lib/rabbitmq/plugins/"
rabbitmq_plugin_name="rabbitmq_delayed_message_exchange"
rabbitmq_plugin_src="$BK_PKG_SRC_PATH/rabbitmq_delayed_message_exchange-3.8.0.ez"
rabbitmq_plugin_url="https://github.com/rabbitmq/rabbitmq-delayed-message-exchange/releases/download/v3.8.0/rabbitmq_delayed_message_exchange-3.8.0.ez"
# 如果使用了蓝鲸默认的rabbitmq, 则自动刷新权限.
if [ "${BK_CODECC_RABBITMQ_ADDR%:*}" = "$BK_RABBITMQ_IP" ]; then
  echo "复用蓝鲸RabbitMQ, 自动创建vhost及账户."
  install_mq_plugin=1
  pcmd -H "$BK_RABBITMQ_IP" "$CTRL_DIR/bin/add_rabbitmq_user.sh -u \"$BK_CODECC_RABBITMQ_USER\" -p \"$BK_CODECC_RABBITMQ_PASSWORD\" -h \"$BK_CODECC_RABBITMQ_VHOST\"; rabbitmqctl change_password \"$BK_CODECC_RABBITMQ_USER\" \"$BK_CODECC_RABBITMQ_PASSWORD\""
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

echo "注册到 CI 顶部导航栏"
"$BK_CODECC_SRC_DIR/scripts/deploy-codecc/bk-codecc-reg-ci-nav.sh"

