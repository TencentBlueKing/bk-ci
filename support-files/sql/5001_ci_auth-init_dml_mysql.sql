use devops_ci_auth;
set names utf8mb4;

-- 蓝盾资源初始化

REPLACE INTO `T_AUTH_RESOURCE` (`RESOURCETYPE`, `NAME`, `ENGLISHNAME`, `DESC`, `ENGLISHDESC`, `PARENT`, `SYSTEM`, `CREATOR`, `CREATETIME`, `UPDATER`, `UPDATETIME`, `DELETE` ) VALUES ('bba91f6256244970b7655b997b7f01c2','2121001','21','权限系统： 用户组已存在','','auth: group already exist');
