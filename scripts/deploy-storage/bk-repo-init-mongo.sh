#!/bin/bash

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

MIGRATE_DIR=$HOME/.migrate

tag_file () {
    local dir=$1
    local file=$2
    local filename=${file##*/}
    [[ -d "$dir" ]] || mkdir -p $dir
    md5sum "$file" | cut -d' ' -f1 > "$dir/$filename" && \
    chattr +i "$dir/$filename"
    return $?
}

[[ -d $MIGRATE_DIR ]] || mkdir -p "$MIGRATE_DIR"

echo "MongoDB"
echo "create mongodb db:"
BK_REPO_MONGODB_HOST=${BK_REPO_MONGODB_ADDR%:*}
BK_REPO_MONGODB_PORT=${BK_REPO_MONGODB_ADDR#*:}

if [[ ! -f $MIGRATE_DIR/0001_repo_db_bkrepo_t_init_mongo.json ]] ; then
    for db in bkrepo ; do
        # DELETE DB! use with caution.
        # mongo --host ${BK_REPO_MONGODB_HOST} --port ${BK_REPO_MONGODB_PORT} -u ${BK_MONGODB_ADMIN_USER} -p ${BK_MONGODB_ADMIN_PASSWORD} --authenticationDatabase admin "$db" <<< "db.dropDatabase()"
        # 尝试创建用户并强制修改密码.
        ${CTRL_DIR}/bin/add_mongodb_user.sh -d ${db} -i "mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@$LAN_IP:27017/admin" -u $BK_REPO_MONGODB_USER -p $BK_REPO_MONGODB_PASSWORD
        mongo mongodb://$BK_MONGODB_ADMIN_USER:$(urlencode $BK_MONGODB_ADMIN_PASSWORD)@$LAN_IP:27017/admin <<EOF
use $db;
db;
db.changeUserPassword('$BK_REPO_MONGODB_USER', '$BK_REPO_MONGODB_PASSWORD');
EOF
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
    
        if [[ $($BK_REPO_SRC_DIR/scripts/bk-repo-check-mongo.sh|grep -wE "account|project|repository|user"|wc -l) == 4 ]] ; then
            echo "Create repo user and initdata repo mongodb successed"
            tag_file "$MIGRATE_DIR" $mongo_json
        else
            echo "Create repo user and initdata repo mongodb failed" && exit 3
        fi
    done
else
    echo "Repo mongo user and initdata already done"
fi
