CREATE DATABASE IF NOT EXISTS `devops_remotedev` DEFAULT CHARACTER SET utf8mb4;

-- ----------------------------
-- Table structure for T_WORKSPACE
-- 工作空间相关属性
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE` (
                                             `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '工作空间ID',
                                             `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户',
                                             `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
                                             `NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
                                             `TEMPLATE_ID` int(11) NOT NULL DEFAULT 16 COMMENT '模板ID',
                                             `URL` varchar(1024) NOT NULL DEFAULT '' COMMENT '工蜂项目URL',
                                             `BRANCH` varchar(1024) NOT NULL DEFAULT '' COMMENT '工蜂项目分支',
                                             `YAML` longtext NOT NULL DEFAULT ''  COMMENT '配置yaml内容',
                                             `YAML_PATH` varchar(1024) NOT NULL DEFAULT ''  COMMENT '配置yaml路径',
                                             `DOCKERFILE` longtext NOT NULL DEFAULT ''  COMMENT '依赖镜像的DockerFile内容',
                                             `IMAGE_PATH` varchar(256) NOT NULL DEFAULT '' COMMENT '镜像地址',
                                             `CPU` int(11) NOT NULL DEFAULT 16 COMMENT 'CPU',
                                             `MEMORY` varchar(64) NOT NULL DEFAULT '32768M' COMMENT '内存',
                                             `DISK` varchar(64) NOT NULL DEFAULT '100G' COMMENT '磁盘',
                                             `CREATOR` varchar(1024) NOT NULL DEFAULT '' COMMENT '创建人',
                                             `CREATOR_BG_NAME` varchar(128) NOT NULL DEFAULT ''  COMMENT '预留字段，CI开启人所在事业群，用作度量统计',
                                             `CREATOR_DEPT_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '预留字段，CI开启人所在部门，用作度量统计',
                                             `CREATOR_CENTER_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '预留字段，CI开启人所在中心，用作度量统计',
                                             `STATUS` int(11) NOT NULL DEFAULT 0 COMMENT '工作空间状态',
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
                                                    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
                                                    `ACTION` int(10) NOT NULL DEFAULT 0 COMMENT '操作行为: CREATE, START, STOP, DELETE, SHARE',
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
-- Table structure for T_WORKSPACE_OP_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTE_DEV_SETTINGS` (
                                                       `ID` bigint(20) NOT NULL AUTO_INCREMENT,
                                                       `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户',
                                                       `DEFAULT_SHELL` varchar(10) NOT NULL DEFAULT '' COMMENT '默认shell: zsh | bash',
                                                       `APPEARANCE` varchar(64) NOT NULL DEFAULT '' COMMENT '外观',
                                                       `LANGUAGE` varchar(64) NOT NULL DEFAULT '' COMMENT '语言',
                                                       `VOICE_NOTICE` tinyint NOT NULL DEFAULT 0 COMMENT '是否开启声音提醒',
                                                       `DOCK_NOTICE` tinyint NOT NULL DEFAULT 0 COMMENT '是否开启DOCK显示',
                                                       `GIT_ATTACHED` tinyint NOT NULL DEFAULT 0 COMMENT '是否连接git',
                                                       `TAPD_ATTACHED` tinyint NOT NULL DEFAULT 0 COMMENT '是否连接TAPD',
                                                       `GITHUB_ATTACHED` tinyint NOT NULL DEFAULT 0 COMMENT '是否连接github',
                                                       `ENV` varchar(1024) NOT NULL DEFAULT '' COMMENT '环境变量配置',
                                                       `FILE_CONTENT` longtext NOT NULL DEFAULT '' COMMENT '文件内容',
                                                       `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
                                                       `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                       PRIMARY KEY (`ID`) USING BTREE,
                                                       KEY `uni_1` (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户远程开发配置表';

-- ----------------------------
-- Table structure for T_WORKSPACE_SHARED 工作空间共享记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_SHARED` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `WORKSPACE_ID` bigint(20) NOT NULL DEFAULT 0 COMMENT '工作空间ID',
    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `SHARED_USER` varchar(64) NOT NULL DEFAULT '' COMMENT '被共享的用户',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `uni_1` (`WORKSPACE_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间共享记录表';
