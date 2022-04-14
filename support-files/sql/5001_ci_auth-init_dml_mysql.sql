use devops_ci_auth;
set names utf8mb4;

-- 蓝盾资源初始化

-- 蓝盾资源类型
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('project','项目','project','项目资源','project','','ALL','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('pipeline','流水线','pipeline','流水线资源','pipeline','project','ALL','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('repertory','代码库','pipeline','代码库资源','repertory','project','ALL','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('credential','凭据','credential','凭据资源','credential','project','CI','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('cert','证书','cert','证书资源','cert','project','CI','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('environment','环境','environment','环境资源','environment','project','CI','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('env_node','节点','env_node','节点资源','env_node','project','CI','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('rule','质量红线规则','rule','质量红线规则','rule','project','ALL','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);
REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('quality_group','红线通知组','quality_group','红线通知组','quality_group','project','ALL','system','2022-04-01 13:34:57','system','2022-04-01 13:34:57',0);

-- 所有权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('all_action','project','项目所有权限','system','2022-04-01 13:34:57',0,'view');

-- 项目权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_view','project','查看项目','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_create','project','添加项目','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_edit','project','编辑项目','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_delete','project','禁用项目','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_manage','project','项目管理','system','2022-04-01 13:34:57',0,'manage');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('project_views_manager','project','项目视图管理','system','2022-04-01 13:34:57',0,'edit');

-- 流水线权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_view','pipeline','查看流水线','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_edit','pipeline','编辑流水线','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_create','pipeline','添加流水线','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_download','pipeline','下载流水线构件','system','2022-04-01 13:34:57',0,'execute');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_delete','pipeline','删除流水线','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_share','pipeline','分享流水线构件','system','2022-04-01 13:34:57',0,'execute');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('pipeline_execute','pipeline','执行流水线','system','2022-04-01 13:34:57',0,'execute');

-- 代码库权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('repertory_view','repertory','查看代码库','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('repertory_edit','repertory','编辑代码库','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('repertory_create','repertory','添加代码库','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('repertory_delete','repertory','删除代码库','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('repertory_use','repertory','使用代码库','system','2022-04-01 13:34:57',0,'execute');

-- 凭证权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('credential_view','credential','查看凭据','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('credential_edit','credential','编辑凭据','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('credential_create','credential','添加凭据','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('credential_delete','credential','删除凭据','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('credential_use','credential','使用凭据','system','2022-04-01 13:34:57',0,'execute');

-- 证书权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('cert_view','cert','查看证书','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('cert_edit','cert','编辑证书','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('cert_create','cert','添加证书','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('cert_delete','cert','删除证书','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('cert_use','cert','使用证书','system','2022-04-01 13:34:57',0,'execute');

-- 环境管理权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('environment_view','environment','查看环境','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('environment_edit','environment','编辑环境','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('environment_create','environment','添加环境','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('environment_delete','environment','删除环境','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('environment_use','environment','使用环境','system','2022-04-01 13:34:57',0,'execute');

-- 节点权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('env_node_view','env_node','查看节点','system','2022-04-01 13:34:57',0,'view');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('env_node_edit','env_node','编辑节点','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('env_node_create','env_node','添加节点','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('env_node_delete','env_node','删除节点','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('env_node_use','env_node','使用节点','system','2022-04-01 13:34:57',0,'execute');

-- 质量红线用户组权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('quality_group_create','quality_group','创建质量红线用户组','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('quality_group_edit','quality_group','编辑质量红线用户组','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('quality_group_enable','quality_group','使用质量红线用户组','system','2022-04-01 13:34:57',0,'use');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('quality_group_delete','quality_group','删除质量红线用户组','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('quality_group_view','quality_group','查看质量红线用户组','system','2022-04-01 13:34:57',0,'view');

-- 质量红线规则权限
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('rule_create','rule','创建质量红线规则','system','2022-04-01 13:34:57',0,'create');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('rule_edit','rule','编辑质量红线规则','system','2022-04-01 13:34:57',0,'edit');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('rule_enable','rule','使用质量红线规则','system','2022-04-01 13:34:57',0,'use');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('rule_delete','rule','删除质量红线规则','system','2022-04-01 13:34:57',0,'delete');
REPLACE INTO `T_AUTH_ACTION` (`ACTIONID`, `RESOURCEID`, `ACTIONNAME`,`CREATOR`, `CREATETIME`, `DELETE`, `ACTIONTYPE`) VALUES ('rule_view','rule','查看质量红线规则','system','2022-04-01 13:34:57',0,'view');
