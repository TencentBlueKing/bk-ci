CREATE DATABASE IF NOT EXISTS `devops_remotedev` DEFAULT CHARACTER SET utf8mb4;

-- ----------------------------
-- Table structure for T_WORKSPACE
-- 工作空间相关属性
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE` (
                                             `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '工作空间ID',
                                             `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
                                             `NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
                                             `TEMPLATE_ID` int(11) NOT NULL DEFAULT 16 COMMENT '模板ID',
                                             `URL` varchar(1024) NOT NULL DEFAULT '' COMMENT '工蜂项目URL',
                                             `BRANCH` varchar(1024) NOT NULL DEFAULT '' COMMENT '工蜂项目分支',
                                             `YAML` longtext NOT NULL COMMENT '配置yaml内容',
                                             `YAML_PATH` varchar(1024) NOT NULL DEFAULT ''  COMMENT '配置yaml路径',
                                             `DOCKERFILE` longtext NOT NULL COMMENT '依赖镜像的DockerFile内容',
                                             `IMAGE_PATH` varchar(256) NOT NULL DEFAULT '' COMMENT '镜像地址',
                                             `WORK_PATH` varchar(256) NOT NULL DEFAULT '' COMMENT '工作区路径',
                                             `HOST_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '工作空间对应的IP',
                                             `CPU` int(11) NOT NULL DEFAULT 16 COMMENT 'CPU',
                                             `MEMORY` int(11) NOT NULL DEFAULT 32768 COMMENT '内存',
                                             `USAGE_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '已使用时间,单位:s（容器结束时更新）',
                                             `SLEEPING_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '已休眠时间,单位:s（容器启动时更新）',
                                             `DISK` int(11) NOT NULL DEFAULT 100 COMMENT '磁盘',
                                             `CREATOR` varchar(1024) NOT NULL DEFAULT '' COMMENT '创建人',
                                             `CREATOR_BG_NAME` varchar(128) NOT NULL DEFAULT ''  COMMENT '预留字段，CI开启人所在事业群，用作度量统计',
                                             `CREATOR_DEPT_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '预留字段，CI开启人所在部门，用作度量统计',
                                             `CREATOR_CENTER_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '预留字段，CI开启人所在中心，用作度量统计',
                                             `STATUS` int(11) NOT NULL DEFAULT 0 COMMENT '工作空间状态,0-PREPARING,1-RUNNING,2-STOPPED,3-SLEEP,4-DELETED,5-EXCEPTION',
                                             `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                             `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                             `LAST_STATUS_UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '状态最近修改时间',
                                             PRIMARY KEY (`ID`),
                                             UNIQUE INDEX `NAME`(`NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T__WORKSPACE_TEMPLATE
-- 工作空间模板信息表
-- ----------------------------

CREATE TABLE `T_WORKSPACE_TEMPLATE`  (
                                         `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                         `NAME` varchar(64) NOT NULL COMMENT '模板名称',
                                         `IMAGE` varchar(256) NOT NULL DEFAULT ''  COMMENT '模板镜像',
                                         `SOURCE` varchar(64) NOT NULL DEFAULT ''  COMMENT '模板来源',
                                         `LOGO` varchar(256) NOT NULL DEFAULT ''  COMMENT '模板LOGO',
                                         `DESCRIPTION` varchar(256) NOT NULL DEFAULT '' COMMENT '模板描述',
                                         `CREATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '最初创建人',
                                         `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
                                         PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工作空间模板信息表';


-- ----------------------------
-- Table structure for T_WORKSPACE_OP_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_OP_HIS` (
                                                    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
                                                    `WORKSPACE_ID` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作空间ID',
                                                    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
                                                    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
                                                    `ACTION` int(10) NOT NULL DEFAULT 0 COMMENT '操作行为: 0-CREATE, 1-START, 2-SLEEP, 3-DELETE, 4-SHARE',
                                                    `ACTION_MSG` varchar(256) NOT NULL DEFAULT '' COMMENT '操作行为描述',
                                                    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                    PRIMARY KEY (`ID`) USING BTREE,
                                                    KEY `uni_1` (`WORKSPACE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间操作记录表';

-- ----------------------------
-- Table structure for T_WORKSPACE_HISTORY
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_HISTORY` (
                                                     `ID` bigint(20) NOT NULL AUTO_INCREMENT,
                                                     `WORKSPACE_ID` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作空间ID',
                                                     `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
                                                     `STARTER` varchar(64) NOT NULL DEFAULT '' COMMENT '启动人',
                                                     `STOPPER` varchar(64) NOT NULL DEFAULT '' COMMENT '停止人',
                                                     `START_TIME` timestamp NULL DEFAULT NULL COMMENT '开始时间',
                                                     `END_TIME` timestamp NULL DEFAULT NULL COMMENT '结束时间',
                                                     `LAST_SLEEP_TIME_COST` int(10) NOT NULL DEFAULT 0 COMMENT '休眠耗时，单位秒（上次结束到这次启动的时间间隔）',
                                                     `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                                     `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                     PRIMARY KEY (`ID`) USING BTREE,
                                                     KEY `uni_1` (`WORKSPACE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间运行记录表';

-- ----------------------------
-- Table structure for T_REMOTE_DEV_SETTINGS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTE_DEV_SETTINGS` (
                                                       `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户',
                                                       `DEFAULT_SHELL` varchar(10) NOT NULL DEFAULT '' COMMENT '默认shell: zsh | bash',
                                                       `BASIC_SETTING` mediumtext NOT NULL COMMENT '客户端使用，后台只管存的信息',
                                                       `GIT_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接git',
                                                       `TAPD_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接TAPD',
                                                       `GITHUB_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接github',
                                                       `ENVS_FOR_VARIABLE` mediumtext NOT NULL COMMENT '远程开发环境变量配置',
                                                       `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                                       `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                       PRIMARY KEY (`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户远程开发配置表';

-- ----------------------------
-- Table structure for T_WORKSPACE_SHARED 工作空间共享记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_SHARED` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `WORKSPACE_ID` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作空间ID',
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `SHARED_USER` varchar(64) NOT NULL DEFAULT '' COMMENT '被共享的用户',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `uni_1` (`WORKSPACE_ID`),
    KEY `uni_2` (`SHARED_USER`),
    KEY `uni_3` (`OPERATOR`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间共享记录表';

-- ----------------------------
-- Table structure for T_REMOTE_DEV_FILE 云开发文件存储
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTE_DEV_FILE` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PATH` varchar(1024) NOT NULL DEFAULT '' COMMENT '文件路径',
    `CONTENT` blob NOT NULL COMMENT '压缩后文件内容',
    `MD5` varchar(32) NOT NULL DEFAULT '' COMMENT 'md5校验',
    `USER` varchar(64) NOT NULL DEFAULT '' COMMENT '用户',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `idx_user` (`USER`),
    KEY `idx_user_md5` (`USER`, `md5`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '云开发文件存储';

-- ----------------------------
-- Table structure for T_SSH_PUBLIC_KEYS 用户SSH公钥存储
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SSH_PUBLIC_KEYS` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `USER` varchar(64) NOT NULL DEFAULT '' COMMENT '用户',
    `PUBLIC_KEY` varchar(1024) NOT NULL DEFAULT '' COMMENT 'Base64加密公钥',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `idx_user` (`USER`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户SSH公钥存储';
