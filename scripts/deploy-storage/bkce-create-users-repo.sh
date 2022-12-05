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
BK_REPO_SRC_DIR=${BK_REPO_SRC_DIR:-$BK_PKG_SRC_PATH/repo}
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
./bin/add_or_update_appcode.sh "$BK_REPO_APP_CODE" "$BK_REPO_APP_TOKEN" "蓝盾" "mysql-paas"  # 注册app。第4个参数即是login-path。

#echo "导入 IAMv3 权限模板."
#./bin/bkiam_migrate.sh -t "$BK_IAM_PRIVATE_URL" -a "$BK_REPO_APP_CODE" -s "$BK_REPO_APP_TOKEN" "$BK_REPO_SRC_DIR"/support-files/bkiam/*.json

echo "MongoDB"
echo "create mongodb db:"
BK_REPO_MONGODB_HOST=${BK_REPO_MONGODB_ADDR%:*}
BK_REPO_MONGODB_PORT=${BK_REPO_MONGODB_ADDR#*:}
for db in bkrepo ; do
    # DELETE DB! use with caution.
    # mongo --host ${BK_REPO_MONGODB_HOST} --port ${BK_REPO_MONGODB_PORT} -u ${BK_MONGODB_ADMIN_USER} -p ${BK_MONGODB_ADMIN_PASSWORD} --authenticationDatabase admin "$db" <<< "db.dropDatabase()"
    # 尝试创建用户并强制修改密码.
    pcmd -H $BK_MONGODB_IP0 "${CTRL_DIR}/bin/add_mongodb_user.sh -d '${db}' -i mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@\$LAN_IP:27017/admin -u '$BK_REPO_MONGODB_USER' -p '$BK_REPO_MONGODB_PASSWORD'; mongo mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@\$LAN_IP:27017/admin <<EOF
use $db;
db;
db.changeUserPassword('$BK_REPO_MONGODB_USER', '$BK_REPO_MONGODB_PASSWORD');
EOF"
done

echo "import mongodb json:"
patt_mongo_json_filename="^[0-9]{4,4}_repo_(db_[a-z0-9]+)_(t_[a-z0-9_]+)_mongo.json$"
for mongo_json in "$BK_REPO_SRC_DIR"/support-files/nosql/*.json ; do
    read mongo_json_db mongo_json_coll < <(
        sed -nr "s/$patt_mongo_json_filename/\1 \2/p" <<< "${mongo_json##*/}"
    ) || true
    # 提前根据文件名创建collections.
    if [ -n "${mongo_json_db:-}" ] && [ -n "${mongo_json_coll:-}" ]; then
        echo "import data to $mongo_json_db.$mongo_json_coll from file: $mongo_json."
        echo "mongoimport --host ${BK_REPO_MONGODB_HOST} --port ${BK_REPO_MONGODB_PORT} \
        -u ${BK_REPO_MONGODB_USER} -p ${BK_REPO_MONGODB_PASSWORD} \
        -d $mongo_json_db -c $mongo_json_coll --mode=upsert --file=$mongo_json"
        #mongoimport --host ${BK_REPO_MONGODB_HOST} --port ${BK_REPO_MONGODB_PORT} \
        #-u ${BK_REPO_MONGODB_USER} -p ${BK_REPO_MONGODB_PASSWORD} \
        #-d "$mongo_json_db" -c "$mongo_json_coll" --mode=upsert --file="$mongo_json"
        mongo mongodb://$BK_REPO_MONGODB_USER:$(urlencode $BK_REPO_MONGODB_PASSWORD)@$LAN_IP:27017/bkrepo $mongo_json
    else
        echo "ignore illegal filename: $mongo_json. it should match regex: $patt_mongo_json_filename."
    fi
done

#echo "注册到 CI 顶部导航栏"
"$BK_REPO_SRC_DIR/scripts/bk-repo-reg-ci-nav.sh"

