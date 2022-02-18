#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

source "${CTRL_DIR:-/data/install}/load_env.sh"

mysql --login-path=mysql-ci <<EOF
update devops_ci_project.T_SERVICE set status='ok', deleted=b'0',
 iframe_url='$BK_CODECC_PUBLIC_URL/codecc/'
 where english_name='CodeCC';
EOF

mysql --login-path=mysql-ci <<EOF
select * from devops_ci_project.T_SERVICE where english_name='CodeCC' \G
EOF
