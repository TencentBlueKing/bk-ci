#!/bin/bash
# shellcheck disable=SC2128

set -eu
trap "on_ERR;" ERR
on_ERR (){
    local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
    echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

set -a
CTRL_DIR="${CTRL_DIR}"
source ${CTRL_DIR:-/data/install}/load_env.sh
set +a

BK_PKG_SRC_PATH=${BK_REPO_SRC_DIR:-/data/src}
BK_REPO_HOME="${BK_REPO_HOME:-/data/bkce/repo}"

echo "Put kv value to Consul"
if [[ -f $BK_HOME/etc/repo/application.yaml ]] ; then
    consul kv put bkrepo-config/application/data @$BK_HOME/etc/repo/application.yaml || return $?
else
    echo "$BK_HOME/etc/repo/application.yaml not exist"
fi

for x in auth generic repository ; do
    if [[ -f $BK_HOME/etc/repo/${x}.yaml ]] ; then
        consul kv put bkrepo-config/repo-${x}/data @$BK_HOME/etc/repo/${x}.yaml || return $?
    else
        echo "$BK_HOME/etc/repo/${x}.yaml not exist"
    fi
done

echo "get application data"
consul kv get bkrepo-config/application/data

echo "get repo-auth data"
consul kv get bkrepo-config/repo-auth/data

echo "get repo-generic data"
consul kv get bkrepo-config/repo-generic/data

echo "get repo-repository data"
consul kv get bkrepo-config/repo-repository/data
