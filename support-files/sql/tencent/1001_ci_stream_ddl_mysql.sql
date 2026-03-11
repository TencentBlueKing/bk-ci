USE devops_ci_stream;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;



-- ----------------------------
-- Table structure for T_GIT_BASIC_SETTING
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_BASIC_SETTING` (
    `ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `NAME` varchar(128) NOT NULL COMMENT '工蜂项目名',
    `URL` varchar(1024) NOT NULL COMMENT '工蜂项目URL',
    `HOME_PAGE` varchar(1024) NOT NULL COMMENT '工蜂项目HomePage',
    `GIT_HTTP_URL` varchar(1024) NOT NULL COMMENT '工蜂项目httpUrl',
    `GIT_SSH_URL` varchar(1024) NOT NULL COMMENT '工蜂项目sshUrl',
    `ENABLE_CI` bit(1) NOT NULL COMMENT '是否启用CI功能',
    `BUILD_PUSHED_BRANCHES` bit(1) NOT NULL COMMENT 'Build pushed branches',
    `BUILD_PUSHED_PULL_REQUEST` bit(1) NOT NULL COMMENT 'Build pushed pull request',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `PROJECT_CODE` varchar(128) NOT NULL COMMENT '蓝盾项目Code',
    `ENABLE_MR_BLOCK` bit(1) NOT NULL COMMENT '是否开启MR锁定',
    `ENABLE_USER_ID` varchar(32) NOT NULL COMMENT 'CI开启人，用于Oauth权限校验',
    `CREATOR_BG_NAME` varchar(128) DEFAULT NULL COMMENT 'CI开启人所在事业群',
    `CREATOR_DEPT_NAME` varchar(128) DEFAULT NULL COMMENT 'CI开启人所在部门',
    `CREATOR_CENTER_NAME` varchar(128) DEFAULT NULL COMMENT 'CI开启人所在中心',
    `GIT_PROJECT_DESC` varchar(1024)  NULL DEFAULT NULL COMMENT 'GIT项目的描述信息',
    `GIT_PROJECT_AVATAR` varchar(1024)  NULL DEFAULT NULL COMMENT 'GIT项目的头像信息',
    `LAST_CI_INFO` text  NULL DEFAULT NULL COMMENT '最后一次构建的CI信息',
    `OAUTH_OPERATOR` varchar(32) NOT NULL DEFAULT '' COMMENT 'OAUTH身份的修改者',
    `ENABLE_COMMIT_CHECK` bit(1) NOT NULL DEFAULT 1 COMMENT '项目中的构建是否发送commitcheck',
    `PATH_WITH_NAME_SPACE` varchar(1024)  NULL DEFAULT NULL COMMENT '带有名空间的项目路径',
    `NAME_WITH_NAME_SPACE` varchar(1024)  NOT NULL DEFAULT '' COMMENT '带有名空间的项目名称',
    `ENABLE_MR_COMMENT` bit(1) NOT NULL DEFAULT 1 COMMENT '项目中的MR是否发送评论',
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_GIT_CI_SERVICES_CONF
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_CI_SERVICES_CONF` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®',
    `IMAGE_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT 'é•œåƒåç§°',
    `IMAGE_TAG` varchar(64) NOT NULL DEFAULT '' COMMENT 'é•œåƒæ ‡ç­¾',
    `REPO_URL` varchar(64) NOT NULL DEFAULT '' COMMENT 'é•œåƒä»“åº“åœ°å€',
    `REPO_USERNAME` varchar(64) DEFAULT '' COMMENT 'é•œåƒä»“åº“åœ°å€',
    `REPO_PWD` varchar(64) DEFAULT '' COMMENT 'é•œåƒä»“åº“åœ°å€',
    `ENABLE` bit(1) DEFAULT b'0' COMMENT 'æ˜¯å¦å¯ç”¨',
    `ENV` text COMMENT 'çŽ¯å¢ƒå˜é‡ï¼Œjsonç»“æž„',
    `CREATE_USER` varchar(64) DEFAULT '' COMMENT 'åˆ›å»ºè€…',
    `UPDATE_USER` varchar(64) DEFAULT '' COMMENT 'æ›´æ–°è€…',
    `GMT_CREATE` datetime DEFAULT NULL COMMENT 'åˆ›å»ºæ—¶é—´',
    `GMT_MODIFIED` datetime DEFAULT NULL COMMENT 'ä¿®æ”¹æ—¶é—´',
    PRIMARY KEY (`ID`),
    KEY `IDX_IMAGE` (`IMAGE_NAME`,`IMAGE_TAG`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='GITCI servicesé…ç½®è®°å½•è¡¨';

-- ----------------------------
-- Table structure for T_GIT_PIPELINE_BRANCH
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_PIPELINE_BRANCH` (
    `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `PIPELINE_ID` varchar(128) NOT NULL COMMENT '蓝盾流水线ID',
    `BRANCH` varchar(1024) NOT NULL COMMENT 'GIT分支',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `PIPELINE_ID` (`PIPELINE_ID`) USING BTREE COMMENT '查找'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_GIT_PIPELINE_RESOURCE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_PIPELINE_RESOURCE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `FILE_PATH` varchar(600) CHARACTER SET utf8mb4 NOT NULL DEFAULT '.ci.yml' COMMENT '工程中yml文件路径',
    `PIPELINE_ID` varchar(34) CHARACTER SET utf8mb4 NOT NULL DEFAULT '' COMMENT '对应蓝盾流水线ID',
    `DISPLAY_NAME` varchar(255) CHARACTER SET utf8mb4 NOT NULL DEFAULT '.ci' COMMENT '工蜂CI流水线名称',
    `CREATOR` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '最初创建人',
    `ENABLED` bit(1) NOT NULL DEFAULT b'1' COMMENT '流水线启用状态',
    `LATEST_BUILD_ID` varchar(34) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '最新一次构建ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `VERSION` varchar(34) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'YAML版本号',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `PIPELINE_ID` (`PIPELINE_ID`),
    UNIQUE KEY `pipeline` (`GIT_PROJECT_ID`,`FILE_PATH`),
    KEY `projectId` (`GIT_PROJECT_ID`),
    KEY `pipelineId` (`PIPELINE_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='工蜂流水线资源表-存储流水线相关信息';

-- ----------------------------
-- Table structure for T_GIT_PROJECT_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_PROJECT_CONF` (
    `ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `NAME` varchar(128) NOT NULL COMMENT '工蜂项目名',
    `URL` varchar(1024) NOT NULL COMMENT '工蜂项目URL',
    `ENABLE` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否可以开启CI功能，默认false',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂项目灰度配置';

-- ----------------------------
-- Table structure for T_GIT_PROJECT_MESSAGE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_PROJECT_MESSAGE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `MESSAGE_ID` bigint(11) NOT NULL COMMENT '消息ID',
    `USER_ID` varchar(32) NOT NULL COMMENT '消息接收人',
    `PROJECT_ID` varchar(32) NOT NULL COMMENT '蓝盾项目ID',
    `MESSAGE_TYPE` varchar(32) NOT NULL COMMENT '消息类型',
    `MESSAGE_TITLE` varchar(255) NOT NULL COMMENT '消息标题',
    `MESSAGE_CONTENT` longtext NOT NULL COMMENT '消息内容',
    `HAVE_READ` bit(1) NOT NULL COMMENT '是否已读',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `FILTER` (`PROJECT_ID`,`USER_ID`,`MESSAGE_TYPE`,`HAVE_READ`) USING BTREE COMMENT '检索消息记录'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_GIT_PROJECT_PIPELINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_PROJECT_PIPELINE` (
    `ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `PROJECT_CODE` varchar(128) NOT NULL COMMENT '蓝盾项目Code',
    `PIPELINE_ID` varchar(128) NOT NULL COMMENT '蓝盾流水线ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `PROJECT_CODE` (`PROJECT_CODE`),
    KEY `PIPELINE_ID` (`PIPELINE_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI项目与流水线对应关系';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `OBJECT_KIND` varchar(128) NOT NULL COMMENT '触发类型：push,tag_push,merge_request,issue,note,',
    `OPERATION_KIND` varchar(128) DEFAULT NULL COMMENT 'operation_kind, create, delete, update',
    `EXTENSION_ACTION` varchar(128) DEFAULT NULL COMMENT 'open, close, reopen, update, push-update, merge',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `BRANCH` varchar(1024) NOT NULL COMMENT 'branch',
    `TARGET_BRANCH` varchar(1024) DEFAULT NULL COMMENT 'targetBranch',
    `COMMIT_ID` varchar(1024) DEFAULT NULL COMMENT 'commit id',
    `COMMIT_MSG` text,
    `COMMIT_TIMESTAMP` varchar(128) DEFAULT NULL COMMENT 'commit timestamp',
    `USER_NAME` varchar(1024) NOT NULL COMMENT 'user name',
    `TOTAL_COMMIT_COUNT` bigint(11) DEFAULT NULL COMMENT 'total_commits_count',
    `MERGE_REQUEST_ID` bigint(11) DEFAULT NULL COMMENT 'merge_request_id',
    `EVENT` longtext NOT NULL COMMENT 'event',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '描述',
    `MR_TITLE` varchar(1024) DEFAULT NULL,
    `SOURCE_GIT_PROJECT_ID` bigint(11) DEFAULT NULL COMMENT 'fork库工蜂项目ID',
    `COMMIT_MESSAGE` text CHARACTER SET utf8mb4,
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `idx_1` (`GIT_PROJECT_ID`),
    KEY `IDX_CREATE_TIME` (`CREATE_TIME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_20210811_101420_bak
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_20210811_101420_bak` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `OBJECT_KIND` varchar(128) NOT NULL COMMENT '触发类型：push,tag_push,merge_request,issue,note,',
    `OPERATION_KIND` varchar(128) DEFAULT NULL COMMENT 'operation_kind, create, delete, update',
    `EXTENSION_ACTION` varchar(128) DEFAULT NULL COMMENT 'open, close, reopen, update, push-update, merge',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `BRANCH` varchar(1024) NOT NULL COMMENT 'branch',
    `TARGET_BRANCH` varchar(1024) DEFAULT NULL COMMENT 'targetBranch',
    `COMMIT_ID` varchar(1024) DEFAULT NULL COMMENT 'commit id',
    `COMMIT_MSG` text,
    `COMMIT_TIMESTAMP` varchar(128) DEFAULT NULL COMMENT 'commit timestamp',
    `USER_NAME` varchar(1024) NOT NULL COMMENT 'user name',
    `TOTAL_COMMIT_COUNT` bigint(11) DEFAULT NULL COMMENT 'total_commits_count',
    `MERGE_REQUEST_ID` bigint(11) DEFAULT NULL COMMENT 'merge_request_id',
    `EVENT` longtext NOT NULL COMMENT 'event',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '描述',
    `MR_TITLE` varchar(1024) DEFAULT NULL,
    `SOURCE_GIT_PROJECT_ID` bigint(11) DEFAULT NULL COMMENT 'fork库工蜂项目ID',
    `COMMIT_MESSAGE` text CHARACTER SET utf8mb4,
    PRIMARY KEY (`ID`),
    KEY `idx_1` (`GIT_PROJECT_ID`),
    KEY `IDX_CREATE_TIME` (`CREATE_TIME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_BUILD` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `ORIGIN_YAML` longtext NOT NULL COMMENT '初始yaml',
    `NORMALIZED_YAML` longtext NOT NULL COMMENT '格式化后的yaml',
    `PIPELINE_ID` varchar(128) DEFAULT NULL COMMENT '蓝盾流水线ID',
    `BUILD_ID` varchar(128) DEFAULT NULL COMMENT '蓝盾流水线BuildId',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `BRANCH` varchar(1024) NOT NULL COMMENT 'branch',
    `OBJECT_KIND` varchar(128) NOT NULL COMMENT '触发类型：push,tag_push,merge_request,issue,note,',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '描述',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `TRIGGER_USER` varchar(64) DEFAULT NULL COMMENT '触发人',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `SOURCE_GIT_PROJECT_ID` bigint(11) DEFAULT NULL COMMENT '源分支的所属库ID(为了支持fork库)',
    `PARSED_YAML` longtext COMMENT '替换完模板的yaml',
    `BUILD_STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本号',
    `COMMIT_MESSAGE` text CHARACTER SET utf8mb4,
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `EVENT_ID` (`EVENT_ID`),
    KEY `PIPELINE_ID` (`PIPELINE_ID`),
    KEY `BUILD_ID` (`BUILD_ID`),
    KEY `BRANCH` (`BRANCH`),
    KEY `idx_2` (`GIT_PROJECT_ID`,`BUILD_ID`,`OBJECT_KIND`),
    KEY `OBJECT_KIND` (`OBJECT_KIND`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `TRIGGER_USER` (`TRIGGER_USER`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `SOURCE_GIT_PROJECT_ID` (`SOURCE_GIT_PROJECT_ID`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `DESCRIPTION` (`DESCRIPTION`) USING BTREE COMMENT '使用 like 多条件查询',
    KEY `BUILD_STATUS` (`BUILD_STATUS`) USING BTREE COMMENT '使用 in 多条件查询'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求触发的构建记录';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_BUILD_20210811_101436_bak
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_BUILD_20210811_101436_bak` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `ORIGIN_YAML` longtext NOT NULL COMMENT '初始yaml',
    `NORMALIZED_YAML` longtext NOT NULL COMMENT '格式化后的yaml',
    `PIPELINE_ID` varchar(128) DEFAULT NULL COMMENT '蓝盾流水线ID',
    `BUILD_ID` varchar(128) DEFAULT NULL COMMENT '蓝盾流水线BuildId',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `BRANCH` varchar(1024) NOT NULL COMMENT 'branch',
    `OBJECT_KIND` varchar(128) NOT NULL COMMENT '触发类型：push,tag_push,merge_request,issue,note,',
    `DESCRIPTION` varchar(1024) DEFAULT NULL COMMENT '描述',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `TRIGGER_USER` varchar(64) DEFAULT NULL COMMENT '触发人',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `SOURCE_GIT_PROJECT_ID` bigint(11) DEFAULT NULL COMMENT '源分支的所属库ID(为了支持fork库)',
    `PARSED_YAML` longtext COMMENT '替换完模板的yaml',
    `BUILD_STATUS` varchar(32) DEFAULT NULL COMMENT '构建状态',
    `VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本号',
    `COMMIT_MESSAGE` text CHARACTER SET utf8mb4,
    PRIMARY KEY (`ID`),
    KEY `EVENT_ID` (`EVENT_ID`),
    KEY `PIPELINE_ID` (`PIPELINE_ID`),
    KEY `BUILD_ID` (`BUILD_ID`),
    KEY `BRANCH` (`BRANCH`),
    KEY `idx_2` (`GIT_PROJECT_ID`,`BUILD_ID`,`OBJECT_KIND`),
    KEY `OBJECT_KIND` (`OBJECT_KIND`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `TRIGGER_USER` (`TRIGGER_USER`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `SOURCE_GIT_PROJECT_ID` (`SOURCE_GIT_PROJECT_ID`) USING BTREE COMMENT '使用 in 多条件查询',
    KEY `DESCRIPTION` (`DESCRIPTION`) USING BTREE COMMENT '使用 like 多条件查询',
    KEY `BUILD_STATUS` (`BUILD_STATUS`) USING BTREE COMMENT '使用 in 多条件查询'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求触发的构建记录';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_NOT_BUILD
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_NOT_BUILD` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `ORIGIN_YAML` longtext COMMENT '初始yaml',
    `NORMALIZED_YAML` longtext COMMENT '格式化后的yaml',
    `REASON` varchar(1024) DEFAULT NULL COMMENT '未触发构建原因',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `REASON_DETAIL` text,
    `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '匹配上的流水线ID',
    `FILE_PATH` varchar(1024) DEFAULT NULL COMMENT 'yml文件路径',
    `PARSED_YAML` longtext COMMENT '替换完模板的yaml',
    `VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本号',
    `BRANCH` varchar(1024) DEFAULT NULL COMMENT 'git分支',
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `EVENT_ID` (`EVENT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求未触发的记录';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_NOT_BUILD_20210811_101352_bak
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_NOT_BUILD_20210811_101352_bak` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `ORIGIN_YAML` longtext COMMENT '初始yaml',
    `NORMALIZED_YAML` longtext COMMENT '格式化后的yaml',
    `REASON` varchar(1024) DEFAULT NULL COMMENT '未触发构建原因',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `REASON_DETAIL` text,
    `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '匹配上的流水线ID',
    `FILE_PATH` varchar(1024) DEFAULT NULL COMMENT 'yml文件路径',
    `PARSED_YAML` longtext COMMENT '替换完模板的yaml',
    `VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本号',
    PRIMARY KEY (`ID`),
    KEY `EVENT_ID` (`EVENT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI WebHooks请求未触发的记录';

-- ----------------------------
-- Table structure for T_GIT_USER_MESSAGE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_USER_MESSAGE` (
    `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `USER_ID` varchar(32) NOT NULL COMMENT '消息接收人',
    `PROJECT_ID` varchar(32) DEFAULT NULL COMMENT '项目ID',
    `MESSAGE_TYPE` varchar(32) NOT NULL COMMENT '消息类型',
    `MESSAGE_ID` varchar(32) NOT NULL COMMENT '消息ID',
    `HAVE_READ` bit(1) NOT NULL COMMENT '是否已读',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `MESSAGE_TITLE` varchar(255) NOT NULL COMMENT '消息标题',
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `FILTER` (`PROJECT_ID`,`USER_ID`,`MESSAGE_TYPE`,`HAVE_READ`) USING BTREE COMMENT '检索消息记录',
    KEY `USER_ID` (`USER_ID`) USING BTREE COMMENT '通过user_id检索'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_GIT_USER_MESSAGE_20210811_101552_bak
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_USER_MESSAGE_20210811_101552_bak` (
    `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `USER_ID` varchar(32) NOT NULL COMMENT '消息接收人',
    `PROJECT_ID` varchar(32) DEFAULT NULL COMMENT '项目ID',
    `MESSAGE_TYPE` varchar(32) NOT NULL COMMENT '消息类型',
    `MESSAGE_ID` varchar(32) NOT NULL COMMENT '消息ID',
    `HAVE_READ` bit(1) NOT NULL COMMENT '是否已读',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `MESSAGE_TITLE` varchar(255) NOT NULL COMMENT '消息标题',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `FILTER` (`PROJECT_ID`,`USER_ID`,`MESSAGE_TYPE`,`HAVE_READ`) USING BTREE COMMENT '检索消息记录'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_GIT_WEB_STARTER_YAML
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_WEB_STARTER_YAML` (
    `NAME` varchar(32) NOT NULL,
    `DESCRIPTION` text,
    `ICON_NAME` varchar(64) DEFAULT NULL,
    `CATEGORIES` text,
    `YAML_CONTENT` text,
    `ICON_URL` text,
    `YAML_URL` text,
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`NAME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_REPOSITORY_CONF
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_CONF` (
    `ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `NAME` varchar(128) NOT NULL COMMENT '工蜂项目名',
    `URL` varchar(1024) NOT NULL COMMENT '工蜂项目URL',
    `HOME_PAGE` varchar(1024) NOT NULL COMMENT '工蜂项目HomePage',
    `GIT_HTTP_URL` varchar(1024) NOT NULL COMMENT '工蜂项目httpUrl',
    `GIT_SSH_URL` varchar(1024) NOT NULL COMMENT '工蜂项目sshUrl',
    `ENABLE_CI` bit(1) NOT NULL COMMENT '是否启用CI功能',
    `BUILD_PUSHED_BRANCHES` bit(1) NOT NULL COMMENT 'Build pushed branches',
    `LIMIT_CONCURRENT_JOBS` int(11) DEFAULT NULL COMMENT 'Limit concurrent jobs',
    `BUILD_PUSHED_PULL_REQUEST` bit(1) NOT NULL COMMENT 'Build pushed pull request',
    `AUTO_CANCEL_BRANCH_BUILDS` bit(1) NOT NULL COMMENT 'Auto cancel branch builds',
    `AUTO_CANCEL_PULL_REQUEST_BUILDS` bit(1) NOT NULL COMMENT 'Auto cancel pull request builds',
    `ENV` text COMMENT 'Environment variable',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `PROJECT_CODE` varchar(128) NOT NULL COMMENT '蓝盾项目Code',
    `RTX_CUSTOM_PROPERTY` longtext COMMENT '企业微信通知配置',
    `RTX_GROUP_PROPERTY` longtext COMMENT '企业微信群通知配置',
    `EMAIL_PROPERTY` longtext COMMENT '邮件通知配置',
    `ENABLE_MR_BLOCK` bit(1) DEFAULT NULL COMMENT '是否开启MR锁定',
    `ONLY_FAILED_NOTIFY` bit(1) DEFAULT NULL COMMENT '是否只在失败时通知',
    PRIMARY KEY (`ID`),
    KEY `PROJECT_CODE` (`PROJECT_CODE`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂CI项目配置';

-- ----------------------------
-- Table structure for T_STREAM_PIPELINE_BRANCH
-- ----------------------------    

CREATE TABLE IF NOT EXISTS `T_STREAM_PIPELINE_BRANCH` (
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '工蜂项目ID',
    `PIPELINE_ID` varchar(128) NOT NULL COMMENT '蓝盾流水线ID',
    `BRANCH` varchar(255) NOT NULL COMMENT 'GIT分支',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`GIT_PROJECT_ID`,`PIPELINE_ID`,`BRANCH`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_STREAM_TIMER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STREAM_TIMER` (
  `PROJECT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `CRONTAB` varchar(2048) NOT NULL,
  `GIT_PROJECT_ID` bigint(11) NOT NULL,
  `BRANCHS` varchar(256) DEFAULT NULL COMMENT '触发分支',
  `ALWAYS` bit(1) NOT NULL,
  `CREATOR` varchar(64) DEFAULT NULL,
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CHANNEL` varchar(32) NOT NULL DEFAULT 'GIT',
  `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
  `ORIGIN_YAML` longtext NOT NULL COMMENT '初始yaml',
  PRIMARY KEY (`PROJECT_ID`,`PIPELINE_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_STREAM_TIMER_BRANCH
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_STREAM_TIMER_BRANCH` (
  `PROJECT_ID` varchar(32) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `GIT_PROJECT_ID` bigint(11) NOT NULL,
  `BRANCH` varchar(100) NOT NULL,
  `REVISION` varchar(40) NOT NULL,
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`PIPELINE_ID`,`GIT_PROJECT_ID`,`BRANCH`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_STREAM_DELETE_EVENT`  (
  `PIPELINE_ID` varchar(34)  NOT NULL,
  `GIT_PROJECT_ID` bigint(11) NOT NULL,
  `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
  `ORIGIN_YAML` longtext  NOT NULL COMMENT '初始yaml',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `CREATOR` varchar(64)  NOT NULL,
  PRIMARY KEY (`PIPELINE_ID`,`GIT_PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- Table structure for T_GIT_PIPELINE_REPO_RESOURCE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_GIT_PIPELINE_REPO_RESOURCE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `SOURCE_GIT_PROJECT_PATH` varchar(255) charset utf8mb4                          not null comment '触发库工蜂项目ID',
    `TARGET_GIT_PROJECT_ID`   bigint(11)                                            not null comment '流水线主库工蜂项目ID',
    `PIPELINE_ID`             varchar(34) charset utf8mb4 default ''                not null comment '对应蓝盾流水线ID',
    `CREATE_TIME`             timestamp                   default CURRENT_TIMESTAMP not null comment '创建时间',
    `UPDATE_TIME`             timestamp                   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间',
    PRIMARY KEY (`ID`),
    KEY `idx_pipeline_id` (`PIPELINE_ID`),
    KEY `idx_source_project_path` (`SOURCE_GIT_PROJECT_PATH`),
    KEY `idx_target_project_id` (`TARGET_GIT_PROJECT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='工蜂流水线资源表-存储远程仓库流水线相关信息';
