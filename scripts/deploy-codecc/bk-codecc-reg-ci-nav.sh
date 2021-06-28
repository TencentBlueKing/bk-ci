#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
  local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
  echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

mysql --login-path=mysql-ci <<EOF
update devops_ci_project.T_SERVICE set status='ok', deleted=b'0' where english_name='CodeCC';
EOF
