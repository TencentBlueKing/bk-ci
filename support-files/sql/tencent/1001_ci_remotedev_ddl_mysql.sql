USE devops_ci_remotedev;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_CERT
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT '工作空间ID',
    `PROJECT_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '项目ID',
    `NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
    `DISPLAY_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间备注名称',
    `HOST_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '工作空间对应的IP(待废弃)',
    `USAGE_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '已使用时间,单位:s（容器结束时更新）',
    `SLEEPING_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '已休眠时间,单位:s（容器启动时更新）',
    `CREATOR` varchar(128) NOT NULL DEFAULT '' COMMENT '创建人',
    `CREATOR_BG_NAME` varchar(128) NOT NULL DEFAULT ''  COMMENT '所在事业群，用作度量统计',
    `CREATOR_DEPT_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '所在部门，用作度量统计',
    `CREATOR_CENTER_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '所在中心，用作度量统计',
    `CREATOR_GROUP_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '所在组，用作度量统计',
    `STATUS` int(11) NOT NULL DEFAULT 0 COMMENT '预留字工作空间状态,0-PREPARING,1-RUNNING,2-STOPPED,3-SLEEP,4-DELETED,5-EXCEPTION,6-STARTING,7-SLEEPING,8-DELETING,9-DELIVERING,10-DISTRIBUTING段，CI开启人所在小组，用作度量统计',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `LAST_STATUS_UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '状态最近修改时间',
	`WORKSPACE_MOUNT_TYPE` varchar(32) NOT NULL DEFAULT 'DEVCLOUD' COMMENT '挂载平台（DEVCLOUD、BCS、START）',
	`SYSTEM_TYPE` varchar(32) NOT NULL DEFAULT 'LINUX' COMMENT '系统类型（LINUX、WINDOWS-GPU）',
	`OWNER_TYPE` varchar(32) NOT NULL DEFAULT 'PERSONAL' COMMENT '工作空间所属（PERSONAL、PROJECT）',
    `PROJECT_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '项目名称',
    `BUSINESS_LINE_NAME` varchar(255) NOT NULL DEFAULT '' COMMENT '业务线名称',
    `REMARK` varchar(255) NULL DEFAULT '' COMMENT '备注',
    `LABELS` varchar(1024) NULL DEFAULT NULL COMMENT '标签',
	`BAK_NAME` varchar(128) NULL DEFAULT NULL COMMENT '备份的workspace name',
    `IP` varchar(64) NULL COMMENT '工作空间对应的IP',
    PRIMARY KEY (`ID`),
    UNIQUE INDEX `NAME`(`NAME`),
	KEY `T_WORKSPACE_IP_index`(`IP`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
    
-- ----------------------------
-- Table structure for T_WORKSPACE_WINDOWS windows工作空间详情数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_WINDOWS` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称',
	`WIN_CONFIG_ID` int(11) NULL COMMENT 'windows资源配置id',
    `RESOURCE_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '最长32位字符串， 用于后续调度时传给start sdk',
    `HOST_IP` varchar(64) NOT NULL DEFAULT '' COMMENT '云桌面IP',
    `MAC_ADDRESS` varchar(64) NOT NULL DEFAULT '' COMMENT 'mac地址',
    `IMAGE_ID` varchar(256) default '' not null comment '镜像唯一标识',
    `ZONE_ID` varchar(32) default '' null comment '地域id',
    `CUR_LAUNCH_ID` int(11) NULL COMMENT '根据项目区分的计费id',
    `REGION_ID` int(11) NULL COMMENT '云区域ID',
    `ENABLE_RECORD_USER` varchar(1024) NULL COMMENT '开启云桌面录屏的人，有值等同于开启云桌面',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`WORKSPACE_NAME`),
    KEY `ipKey`(`HOST_IP`),
    KEY `imageKey`(`IMAGE_ID`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='windows工作空间详情数据';

-- ----------------------------
-- Table structure for T_WORKSPACE_OP_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_OP_HIS` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `ACTION` int(10) NOT NULL DEFAULT 0 COMMENT '操作行为: 0-CREATE, 1-START, 2-SLEEP, 3-DELETE, 4-SHARE',
    `ACTION_MSG` varchar(256) NOT NULL DEFAULT '' COMMENT '操作行为描述',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `uni_1` (`WORKSPACE_NAME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间操作记录表';

-- ----------------------------
-- Table structure for T_WORKSPACE_HISTORY
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_HISTORY` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
    `STARTER` varchar(64) NOT NULL DEFAULT '' COMMENT '启动人',
    `STOPPER` varchar(64) NOT NULL DEFAULT '' COMMENT '停止人',
    `START_TIME` timestamp NULL DEFAULT NULL COMMENT '开始时间',
    `END_TIME` timestamp NULL DEFAULT NULL COMMENT '结束时间',
    `LAST_SLEEP_TIME_COST` int(10) NOT NULL DEFAULT 0 COMMENT '休眠耗时，单位秒（上次结束到这次启动的时间间隔）',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `uni_1` (`WORKSPACE_NAME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间运行记录表';

-- ----------------------------
-- Table structure for T_REMOTE_DEV_SETTINGS
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTE_DEV_SETTINGS` (
    `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户ID',
    `USER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '用户中文名',
    `COMPANY_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '公司名称',
    `PHONE` varchar(64) NOT NULL DEFAULT '' COMMENT '电话',
    `PHONE_COUNTRY_CODE` varchar(16) NOT NULL DEFAULT '' COMMENT '电话区号',
    `EMAIL` varchar(128) NOT NULL DEFAULT '' COMMENT '邮箱',
    `DEFAULT_SHELL` varchar(10) NOT NULL DEFAULT '' COMMENT '默认shell: zsh | bash',
    `BASIC_SETTING` mediumtext NOT NULL COMMENT '客户端使用，后台只管存的信息',
    `GIT_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接git',
    `TAPD_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接TAPD',
    `GITHUB_ATTACHED` boolean NOT NULL DEFAULT 0 COMMENT '是否连接github',
    `DOTFILE_REPO` varchar(256) NOT NULL DEFAULT '' COMMENT 'dotfiles仓库路径',
    `ENVS_FOR_VARIABLE` mediumtext NOT NULL COMMENT '远程开发环境变量配置',
    `CUMULATIVE_USAGE_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '当月累计使用时间(月初清空)',
    `CUMULATIVE_BILLING_TIME` int(11) NOT NULL DEFAULT 0 COMMENT '用户累计计费时间(当月计费数据暂不统计)',
    `WIN_USAGE_REMAINING_TIME` int(11) NULL COMMENT '云桌面使用剩余时间(分钟)',
    `WORKSPACE_MAX_RUNNING_COUNT` int(11) NULL COMMENT '最大运行数',
    `WORKSPACE_MAX_HAVING_COUNT` int(11) NULL COMMENT '最大创建个数(每人拥有的运行中+已休眠的开发环境)',
    `IN_GRAY` boolean NOT NULL DEFAULT 0 COMMENT '是否灰度',
    `PROJECT_ID` varchar(64) NULL COMMENT '个人对应的项目id',
    `USER_SETTING` mediumtext NOT NULL COMMENT '用户设置，统一维护一个json字符串',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`USER_ID`),
    KEY `uni_1` (`USER_NAME`),
    KEY `uni_2` (`COMPANY_NAME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '用户远程开发配置表';

-- ----------------------------
-- Table structure for T_WORKSPACE_SHARED 工作空间共享记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_SHARED` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称，唯一性',
    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `SHARED_USER` varchar(64) NOT NULL DEFAULT '' COMMENT '被共享的用户',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
	`ASSIGN_TYPE` varchar(32) NOT NULL DEFAULT 'VIEWER' COMMENT '分享人所属类型（OWNER、VIEWER）',
    `RESOURCE_ID` varchar(32) NOT NULL DEFAULT '' COMMENT '最长32位字符串， 用于后续调度时传给start sdk',
    `EXPIRATION` TIMESTAMP NULL COMMENT '过期时间',
    PRIMARY KEY (`ID`),
    KEY `uni_1` (`WORKSPACE_NAME`),
    KEY `uni_2` (`SHARED_USER`),
    KEY `uni_3` (`OPERATOR`),
    KEY `EXPIRATION_IDX` (`EXPIRATION`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '工作空间共享记录表';

-- ----------------------------
-- Table structure for T_WINDOWS_RESOURCE_ZONE 云桌面地域配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WINDOWS_RESOURCE_ZONE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `ZONE` varchar(32) NOT NULL COMMENT '区域，深圳，南京等',
    `SHORT_NAME` varchar(10) NOT NULL DEFAULT '' COMMENT '区域简称，SZ,NJ',
    `AVAILABLED` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可用，默认可见',
    `DESCRIPTION` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
	`TYPE` varchar(32) default 'DEFAULT' not null comment '区分类型，以满足特殊区域',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`ZONE`,`SHORT_NAME`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='云桌面地域配置';

-- ----------------------------
-- Table structure for T_WINDOWS_RESOURCE_TYPE 云桌面资源配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WINDOWS_RESOURCE_TYPE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `SIZE` varchar(10) NOT NULL DEFAULT '' COMMENT '资源类型：M，L，XL，S',
    `TYPE` varchar(32) NOT NULL DEFAULT '3080' COMMENT 'GPU卡类型',
    `GPU` int(11) NOT NULL DEFAULT '16' COMMENT 'vGPU,备注：后续要删掉',
    `VGPU` varchar(32) NOT NULL DEFAULT '1' COMMENT 'vGPU',
    `CPU` int(11) NOT NULL DEFAULT '16' COMMENT 'CPU',
    `VCPU` varchar(128) NOT NULL DEFAULT '' COMMENT '主频跟核数配置',
    `MEMORY` int(11) NOT NULL DEFAULT '32768' COMMENT '内存',
    `VMEMORY` varchar(32) NOT NULL DEFAULT '' COMMENT '内存配置，可独享',
    `SDISK` varchar(32) NOT NULL DEFAULT '200' COMMENT '系统盘，本地SSD盘，单位GB',
    `DISK` varchar(32) NOT NULL DEFAULT '200' COMMENT '本地SSD盘，单位GB',
    `HDISK` varchar(32) NOT NULL DEFAULT '1' COMMENT '云SSD盘，单位TB',
    `AVAILABLED` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否可用，默认可见',
    `WEIGHT` int(11) NOT NULL DEFAULT '0' COMMENT '权重，用于控制台页面展示先后顺序',
    `DESCRIPTION` varchar(256) NOT NULL DEFAULT '' COMMENT '描述',
    `SPEC_MODEL` bit(1) DEFAULT b'0' NOT NULL COMMENT '是否是特殊机型',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `idx_size` (`SIZE`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='云桌面资源配置';
-- ----------------------------
-- Table structure for T_PROJECT_IMAGES 项目下镜像信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_PROJECT_IMAGES` (
    `ID` bigint(11) auto_increment comment 'ID',
    `IMAGE_ID` varchar(32) default '' not null comment '镜像唯一标识',
    `PROJECT_ID` varchar(64) default '' not null comment '蓝盾项目ID',
    `IMAGE_NAME` varchar(128) default '' not null comment '镜像名称',
    `IMAGE_COS_FILE` varchar(256) default '' not null comment '惊喜地址',
    `SIZE` varchar(32) default '' not null comment '镜像大小，单位G',
    `SOURCE_CGS_ID` varchar(32) default '' not null comment '镜像关联的cgsId',
    `SOURCE_CGS_TYPE` varchar(32) default '' not null comment '镜像关联的cgsType',
    `SOURCE_CGS_ZONE` varchar(32) default '' not null comment '区域：深圳等',
    `STATUS` int(11) default 0 not null comment '镜像状态,0-building,1-success,2-failure',
    `CREATOR` varchar(32) default '' not null comment '创建人',
    `CREATE_TIME` timestamp default CURRENT_TIMESTAMP not null comment '创建时间',
    `ERROR_MSG` varchar(1024) NULL COMMENT '制作镜像失败时的失败信息',
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `uni_1` (`PROJECT_ID`,`IMAGE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '控制台项目镜像模板信息';

-- ----------------------------
-- Table structure for T_SYSTEM_SOFTWARES 系统软件信息
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SYSTEM_SOFTWARES` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `NAME` varchar(32) NOT NULL DEFAULT '' COMMENT '应用名称',
    `VERSION` varchar(32) NOT NULL DEFAULT '' COMMENT '版本',
    `SOURCE` varchar(64) NOT NULL DEFAULT '' COMMENT '来源',
    `CREATOR` varchar(32) NOT NULL DEFAULT '' COMMENT '创建人',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`NAME`,`VERSION`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='系统软件信息';

-- ----------------------------
-- Table structure for T_SYSTEM_SOFTWARES_INSTALLED_RECORDS 系统软件安装记录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SYSTEM_INSTALLED_RECORDS` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `TASK_ID` bigint(11) NOT NULL DEFAULT '0' COMMENT '任务ID',
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '云桌面名称',
    `SOFTWARE_NAME` varchar(32) NOT NULL DEFAULT '' COMMENT '应用名称',
    `STATUS` int(11) NOT NULL DEFAULT 0 COMMENT '任务状态,0-RUNNING,1-FINISHED,2-FAILED,3-SUSPENDED,4-REVOKED,5-WAITING',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`TASK_ID`,`SOFTWARE_NAME`,`WORKSPACE_NAME`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='系统软件安装记录';

-- ----------------------------
-- Table structure for T_WORKSPACE_DETAIL 工作空间详情数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_DETAIL` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `WORKSPACE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '工作空间名称',
    `DETAIL` text NOT NULL COMMENT '详情',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`WORKSPACE_NAME`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='工作空间详情数据';

-- ----------------------------
-- Table structure for T_WHITE_LIST 白名单控制
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WHITE_LIST` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '名称',
    `TYPE` varchar(32) NOT NULL COMMENT '白名单类型',
    `WINDOWS_GPU_LIMIT` int(11) NULL COMMENT '云桌面访问限制，type=WINDOWS_GPU 有效',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`NAME`,`TYPE`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='白名单控制';

-- ----------------------------
-- Table structure for T_CLIENT_VERSION 客户端版本控制
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_CLIENT_VERSION` (
    `IP` varchar(32) NOT NULL DEFAULT '' COMMENT '客户端ip地址',
    `USER` varchar(128) NOT NULL DEFAULT '' COMMENT '用户',
    `VERSION` varchar(16) NOT NULL COMMENT '客户端版本',
    `LAST_UPDATE_TIME` timestamp NULL COMMENT '最近版本更新时间',
    `LAST_VERSION` varchar(16) NULL COMMENT '上次的版本',
    `MAC_ADDRESS` varchar(64) NULL COMMENT 'MAC地址',
    `UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NULL COMMENT '最近更新时间',
    UNIQUE `ukey`(`IP`,`USER`),
    KEY `idx_version` (`VERSION`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端版本控制';

CREATE TABLE IF NOT EXISTS `T_CLIENT` (
	`MAC_ADDRESS` varchar(64)  NOT NULL COMMENT 'MAC地址',
	`CURRENT_USER` varchar(128)  NOT NULL COMMENT '当前使用用户',
    `CURRENT_WORKSPACE_NAMES` json NOT NULL COMMENT '当前用户所属的工作空间名称列表',
    `CURRENT_PROJECT_IDS` json NOT NULL COMMENT '当前机器所属的蓝盾项目ID列表',
	`VERSION` varchar(16)  NOT NULL COMMENT '客户端版本',
    `OS` varchar(16)  NOT NULL COMMENT '客户端系统',
	`START_VERSION` varchar(64)  NOT NULL COMMENT 'START 版本',
    `CREATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
	`UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '最近更新时间',
    PRIMARY KEY (`MAC_ADDRESS`),
    KEY `T_CLIENT_VERSION_IDX`  (`VERSION`),
    KEY `T_CLIENT_START_VERSION_IDX` (`START_VERSION`),
    KEY `T_CLIENT_UPDATE_TIME_IDX` (`UPDATE_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端信息';

-- ----------------------------
-- Table structure for T_REMOTEDEV_EXPERT_SUPPORT 专家支持通知表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTEDEV_EXPERT_SUPPORT` (
	`ID` bigint(11) AUTO_INCREMENT NOT NULL,
	`PROJECT_ID` varchar(64)  NOT NULL COMMENT '蓝盾项目ID',
    `HOST_IP` varchar(64) NOT NULL COMMENT '云桌面IP',
    `WORKSPACE_NAME` varchar(128) NOT NULL COMMENT '工作空间名称，唯一性',
	`CREATOR` varchar(32)  NOT NULL COMMENT '创建人',
	`SUPPORTER` varchar(256)  NULL COMMENT '协助人',
	`STATUS` varchar(16) NOT NULL COMMENT '单据状态',
	`CONTENT` varchar(256)  NOT NULL COMMENT '单据内容',
    `CITY` varchar(32) NOT NULL COMMENT '城市',
    `MACHINE_TYPE` varchar(16) NOT NULL COMMENT '机型',
	`CREATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
	`UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `idx_project_id` (`PROJECT_ID`),
    KEY `idx_host_ip` (`HOST_IP`),
    KEY `idx_workspace_name` (`WORKSPACE_NAME`),
    KEY `idx_creator` (`CREATOR`),
    KEY `idx_status` (`STATUS`),
    KEY `idx_content` (`CONTENT`)
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_REMOTEDEV_EXPERT_SUPPORT_CONFIG 专家支持配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REMOTEDEV_EXPERT_SUPPORT_CONFIG` (
	`ID` bigint(11) AUTO_INCREMENT NOT NULL,
	`TYPE` varchar(16)  NOT NULL COMMENT '配置类型',
	`CONTENT` varchar(256)  NOT NULL COMMENT '配置内容',
    PRIMARY KEY (`ID`),
    KEY `idx_type` (`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_DAILY_CGS_DATA 统计每天云桌面的数据快照
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_DAILY_CGS_DATA` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `DATE` varchar(64) NOT NULL DEFAULT '' COMMENT '日期',
    `OWNER_TYPE` varchar(32) NOT NULL DEFAULT 'PERSONAL' COMMENT '工作空间所属（PERSONAL、PROJECT）',
    `NUMBER` int(11) NOT NULL DEFAULT '0' COMMENT '云桌面数',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE `ukey`(`DATE`,`OWNER_TYPE`),
    KEY `idx_date` (`DATE`),
    KEY `idx_type` (`OWNER_TYPE`)
    ) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='统计每天云桌面的数据快照';

-- ----------------------------
-- Table structure for T_WINDOWS_SPEC_RESOURCE windows特殊机型配额表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WINDOWS_SPEC_RESOURCE` (
	`PROJECT_ID` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' NOT NULL COMMENT '项目ID',
	`SIZE` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT '' NOT NULL COMMENT '资源类型',
	`QUOTA` INT NOT NULL COMMENT '配额',
    PRIMARY KEY (`PROJECT_ID`, `SIZE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='windows特殊机型配额表';

-- ----------------------------
-- Table structure for T_PROJECT_TCLOUD_CFS 项目和腾讯云cfs关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_PROJECT_TCLOUD_CFS` (
	PROJECT_ID varchar(64) NOT NULL COMMENT '蓝盾项目ID',
	CFS_ID varchar(64) NOT NULL,
	PG_ID varchar(64) NULL COMMENT '权限组ID',
    REGION varchar(32) NOT NULL COMMENT '区域',
	PRIMARY KEY (`PROJECT_ID`, `CFS_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_WORKSPACE_NOTIFY
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WORKSPACE_NOTIFY` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `OPERATOR` varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `PROJECT_IDS` varchar(1024) NOT NULL DEFAULT '' COMMENT '项目ID列表',
    `IPS` mediumtext NOT NULL COMMENT 'IP列表',
    `TITLE` varchar(256) NOT NULL DEFAULT '' COMMENT '标题',
    `DESC` varchar(1024) NOT NULL DEFAULT '' COMMENT '描述内容',
    `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `uni_1` (`OPERATOR`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '云桌面消息通知记录';

-- ----------------------------
-- Table structure for T_PROJECT_TGIT_ID_LINK 蓝盾项目和工蜂ID关联表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_PROJECT_TGIT_ID_LINK`(
	`PROJECT_ID` varchar(64) NOT NULL COMMENT '蓝盾项目ID',
    `TGIT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
    `STATUS` varchar(32) NOT NULL COMMENT '仓库状态',
    `OAUTH_USER` varchar(32) NOT NULL COMMENT '授予oauth权限的用户',
    `GIT_TYPE` varchar(16) NOT NULL COMMENT 'GIT仓库类型SVN或者GIT',
    `URL` varchar(255) NULL COMMENT '工蜂url地址',
    `CREATE_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `UPDATE_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '修改时间',
	PRIMARY KEY (`PROJECT_ID`, `TGIT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_PROJECT_START_APP_LINK` (
	`APPNAME` varchar(64) NOT NULL COMMENT 'project的english_name',
	`DETAIL` varchar(64) NOT NULL COMMENT 'project的project_name',
	`APPID` bigint(20) NULL COMMENT 'start的appid',
	PRIMARY KEY (`APPNAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_WORKSPACE_LOGIN` (
	`PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
	`WORKSPACE_NAME` varchar(128) NOT NULL COMMENT '工作空间名称，唯一性',
	`HOST_IP` varchar(64) NOT NULL COMMENT '云桌面IP',
	`LAST_LOGIN_USER` varchar(64) NOT NULL,
	`LAST_LOGIN_TIME` datetime NOT NULL,
    PRIMARY KEY (`PROJECT_ID`, `WORKSPACE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REMOTEDEV_JOB_SCHEMA` (
	`JOB_ID` varchar(64) NOT NULL,
	`JOB_NAME` varchar(64) NOT NULL,
	`JOB_SCHEMA` json NOT NULL,
	`JOB_TYPE` varchar(32) NOT NULL COMMENT '任务类型,如一次性或者周期任务',
    `JOB_ACTION_TYPE` varchar(32) NOT NULL COMMENT '任务执行类型,如后台或者流水线',
	`JOB_ACTION_EXTRA_PARAM` json NOT NULL COMMENT '任务的额外参数，随着tion变更不同类型，比如job执行流水线时需要的参数',
	PRIMARY KEY (`JOB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REMOTEDEV_JOB_EXEC_RECORD` (
	`ID` bigint(20) auto_increment NOT NULL,
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
	`NAME` varchar(256) NOT NULL,
	`CREATE_TIME` timestamp NOT NULL COMMENT '创建时间',
	`END_TIME` timestamp NULL COMMENT '结束时间',
	`CREATOR` varchar(32) NOT NULL COMMENT '用户',
	`STATUS` varchar(32) NOT NULL,
    `ERROR_MSG` varchar(256) NULL COMMENT '执行失败时错误日志',
    `JOB_SCHEMA_ID` varchar(64) NOT NULL,
    `JOB_SCHEMA_PARAM` json NOT NULL,
    `RECEIPT_INFO` json NULL COMMENT '调用一些异步任务时的回执信息，用来追踪状态',
    PRIMARY KEY (`ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REMOTEDEV_CRON_JOB` (
	`ID` bigint(20) auto_increment NOT NULL,
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
	`JOB_NAME` varchar(256) NOT NULL,
	`CREATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
	`UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
	`CREATOR` varchar(32) NOT NULL COMMENT '创建人',
	`CRON_EXP` varchar(16) NOT NULL,
	`LAST_RUN_TIME` timestamp NULL COMMENT '上一次运行时间',
	`UPDATER` varchar(32) NOT NULL COMMENT '更新人',
	`RUN_TIMES` bigint(20) NOT NULL DEFAULT 0 COMMENT '运行次数',
    `JOB_SCHEMA_ID` varchar(64) NOT NULL,
    `JOB_SCHEMA_PARAM` json NOT NULL,
    `ENABLE` bit(1) NOT NULL, 
    PRIMARY KEY (`ID`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table IF NOT EXISTS T_WORKSPACE_LABELS
(
    PROJECT_ID     varchar(64)  not null comment '项目ID',
    WORKSPACE_NAME varchar(128) not null,
    LABEL         varchar(128) not null comment '标签',
    constraint ukey
        unique (PROJECT_ID, LABEL, WORKSPACE_NAME),
    KEY `IDX_WORKSPACE_NAME` (`WORKSPACE_NAME`),
    KEY `IDX_PROJECT_ID` (`PROJECT_ID`),
    KEY `IDX_LABEL` (`LABEL`)
)
    comment '工作空间标签表' charset = utf8;
	
CREATE TABLE `T_WORKSPACE_NOTIFY_HISTORY`
(
    ID             bigint auto_increment
        primary key,
    `BIZ_ID`       varchar(64) NOT NULL COMMENT '会话ID，同一批次的通知，BIZ_ID 会一样',
    `OPERATOR`     varchar(64) NOT NULL DEFAULT '' COMMENT '操作人',
    `USER_IDS`        varchar(64) NOT NULL DEFAULT '' COMMENT '接收人',
    `TYPE`         varchar(32) NOT NULL DEFAULT '' COMMENT '通知类型',
    `STATUS`       varchar(32) NOT NULL COMMENT '通知状态',
    `BODY_PARAMS`   text        NOT NULL COMMENT '描述内容',
    `CREATED_TIME` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY `uni_1` (`BIZ_ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='云桌面消息通知历史';

-- ----------------------------
-- Table structure for T_DISPATCH_WORKSPACE_OP_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS T_DISPATCH_WORKSPACE_OP_HIS
(
    ID              bigint auto_increment primary key,
    WORKSPACE_NAME  varchar(128) default ''                not null comment '工作空间名称',
    ENVIRONMENT_UID varchar(128) default ''                not null comment 'DevCloud环境ID',
    OPERATOR        varchar(64)  default ''                not null comment '操作人',
    ACTION          varchar(64)  default ''                not null comment '操作行为: CREATE, START, STOP, DELETE, SHARE',
    ACTION_MSG      varchar(256) default ''                not null comment '操作行为描述',
    CREATED_TIME    timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    UID             varchar(128) default ''                not null comment 'task id',
    STATUS          varchar(32)  default ''                not null comment '操作状态',
    UPDATE_TIME     timestamp    default CURRENT_TIMESTAMP null comment '修改时间',
    KEY `uni_1` (`WORKSPACE_NAME`),
    KEY `uni_2` (`UID`),
    KEY `uni_3` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KUBERNETES构建集群工作空间操作记录表';
create table IF NOT EXISTS T_WORKSPACE_APP_OAUTH2_MATERIALS
(
    APP_ID     varchar(64)  not null comment '云桌面研发商店应用唯一ID',
    WORKSPACE_NAME varchar(128) not null comment '工作空间唯一ID',
    CLIENT_ID               varchar(32)                           not null comment '客户端标识',
    CLIENT_SECRET           varchar(64)                           not null comment '客户端秘钥',
    constraint ukey
        unique (APP_ID, WORKSPACE_NAME)
)
    comment '云研发应用oauth原材料';

CREATE TABLE IF NOT EXISTS `T_WORKSPACE_RECORD_USER_APPROVAL` (
	`WORKSPACE_NAME` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '工作空间名称',
	`USER` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请的用户',
	`PROJECT_ID` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '项目ID',
	`CREATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
	`UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '修改时间', 
    PRIMARY KEY (`WORKSPACE_NAME`,`USER`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户申请查看工作空间录屏记录';


CREATE TABLE  IF NOT EXISTS `T_CLIENT_TIPS` (
	`ID` bigint(11) auto_increment NOT NULL COMMENT '自增ID',
	`TITLE` varchar(1024) NOT NULL COMMENT '标题',
	`CONTENT` text NOT NULL COMMENT '内容',
	`WEIGHT` int NOT NULL COMMENT '权重',
	`EFFECTIVE_USERS` json NULL COMMENT '生效人员',
	`EFFECTIVE_PROJECTS` json NULL COMMENT '生效项目',
    PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端加载时的提示配置表';

CREATE TABLE IF NOT EXISTS `T_USER_AUTH_APPLY` (
    `ID` bigint(11) auto_increment NOT NULL COMMENT 'ID',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
    `USER_ID` varchar(64) NOT NULL COMMENT '用户',
    `STATUS` int(11) NOT NULL COMMENT '单据状态',
    `AUTH_INFO` json NOT NULL COMMENT '保存权限申请信息',
    `CREATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `UPDATE_TIME` timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `IDX_RECORD_STATUS` USING BTREE (`PROJECT_ID`, `USER_ID`, `STATUS`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户权限申请记录表';


SET FOREIGN_KEY_CHECKS = 1;
