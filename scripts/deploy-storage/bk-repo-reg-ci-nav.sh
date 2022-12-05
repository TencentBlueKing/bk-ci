#!/bin/bash
set -eu
trap "on_ERR;" ERR
on_ERR (){
    local fn=$0 ret=$? lineno=${BASH_LINENO:-$LINENO}
    echo >&2 "ERROR $fn exit with $ret at line $lineno: $(sed -n ${lineno}p $0)."
}

source "${CTRL_DIR:-/data/install}/load_env.sh"

#mysql --login-path=mysql-ci <<EOF
#update devops_ci_project.T_SERVICE set status='ok', deleted=b'0',
#iframe_url='$BK_REPO_PUBLIC_URL/ui/'
#where english_name='Repo';
#EOF
#
#mysql --login-path=mysql-ci <<EOF
#select * from devops_ci_project.T_SERVICE where english_name='Repo' \G
#EOF

IFS_BACKUP=$IFS ; IFS=$(echo -en "\n\b") ;
current_time=$(date +'%Y-%m-%d %H:%M:%S')

if mysql --login-path=mysql-ci -e 'select * from devops_ci_project.T_SERVICE where english_name="Repo" limit 1'|grep -wF Repo ; then
    echo "Repo alreay exist in devops_ci_project , update it now"
    mysql --login-path=mysql-ci <<EOF
update devops_ci_project.T_SERVICE set status='ok', deleted=b'0',
iframe_url='$BK_REPO_PUBLIC_URL/ui/'
where english_name='Repo';
EOF
else
    echo "Repo not exist in devops_ci_project , processing now"
    #echo "mysql --login-path=mysql-ci -e \"INSERT IGNORE INTO \`devops_ci_project.T_SERVICE\` (\`name\`, \`english_name\`, \`service_type_id\`, \`link\`, \`link_new\`, \`inject_type\`, \`iframe_url\`, \`css_url\`, \`js_url\`, \`show_project_list\`, \`show_nav\`, \`project_id_type\`, \`status\`, \`created_user\`, \`created_time\`, \`updated_user\`, \`updated_time\`, \`deleted\`, \`gray_css_url\`, \`gray_js_url\`, \`weight\`, \`logo_url\`, \`web_socket\`) VALUES ('蓝鲸制品库(Repo)', 'Repo', 2, '/repo/', '/repo/', 'iframe', '$BK_REPO_PUBLIC_URL/ui/', '', '', b'1', b'1', 'path', 'planning', 'system', '\"$current_time\"', 'system', '\"$current_time\"', b'1', '', '', 96, 'artifactory', '');\""
    mysql --login-path=mysql-ci << EOF
USE devops_ci_project;
INSERT IGNORE INTO \`T_SERVICE\` (\`name\`, \`english_name\`, \`service_type_id\`, \`link\`, \`link_new\`, \`inject_type\`, \`iframe_url\`, \`css_url\`, \`js_url\`, \`show_project_list\`, \`show_nav\`, \`project_id_type\`, \`status\`, \`created_user\`, \`created_time\`, \`updated_user\`, \`updated_time\`, \`deleted\`, \`gray_css_url\`, \`gray_js_url\`, \`weight\`, \`logo_url\`, \`web_socket\`)
 VALUES ('蓝鲸制品库(Repo)', 'Repo', 2, '/repo/', '/repo/', 'iframe', 'https://$BK_REPO_FQDN/ui/', '', '', b'1', b'1', 'path', 'planning', 'system', '"$current_time"', 'system', '"$current_time"', b'1', '', '', 96, 'artifactory', '');\""
EOF

    mysql --login-path=mysql-ci <<EOF
update devops_ci_project.T_SERVICE set status='ok', deleted=b'0',
iframe_url='$BK_REPO_PUBLIC_URL/ui/'
where english_name='Repo';
EOF
fi
IFS=$IFS_BACKUP

mysql --login-path=mysql-ci <<EOF
select * from devops_ci_project.T_SERVICE where english_name='Repo' \G
EOF
