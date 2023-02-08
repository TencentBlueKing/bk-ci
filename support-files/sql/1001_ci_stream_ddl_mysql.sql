USE devops_ci_stream;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;


-- ----------------------------
-- Table structure for T_GIT_BASIC_SETTING
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_BASIC_SETTING` (
    `ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
    `NAME` varchar(128) NOT NULL COMMENT 'GIT项目名',
    `URL` varchar(1024) NOT NULL COMMENT 'GIT项目URL',
    `HOME_PAGE` varchar(1024) NOT NULL COMMENT 'GIT项目HomePage',
    `GIT_HTTP_URL` varchar(1024) NOT NULL COMMENT 'GIT项目httpUrl',
    `GIT_SSH_URL` varchar(1024) NOT NULL COMMENT 'GIT项目sshUrl',
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
    `TRIGGER_REVIEW_SETTING` text NULL COMMENT 'pr、mr触发时的权限校验(存储为json字符串)',
    PRIMARY KEY (`ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_STREAM_SERVICES_CONF
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_STREAM_SERVICES_CONF` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `IMAGE_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '镜像名称',
    `IMAGE_TAG` varchar(64) NOT NULL DEFAULT '' COMMENT '镜像tag',
    `REPO_URL` varchar(64) NOT NULL DEFAULT '' COMMENT '镜像仓库地址',
    `REPO_USERNAME` varchar(64) DEFAULT '' COMMENT '镜像仓库用户名',
    `REPO_PWD` varchar(64) DEFAULT '' COMMENT '镜像仓库密码',
    `ENABLE` bit(1) DEFAULT b'0' COMMENT '是否可以使用',
    `ENV` text COMMENT '需要的环境变量',
    `CREATE_USER` varchar(64) DEFAULT '' COMMENT '创建人',
    `UPDATE_USER` varchar(64) DEFAULT '' COMMENT '修改人',
    `GMT_CREATE` datetime DEFAULT NULL COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT NULL COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `IDX_IMAGE` (`IMAGE_NAME`,`IMAGE_TAG`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Stream可以提供的容器service';

-- ----------------------------
-- Table structure for T_GIT_PIPELINE_RESOURCE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_PIPELINE_RESOURCE` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
    `FILE_PATH` varchar(600) CHARACTER SET utf8mb4 NOT NULL DEFAULT '.ci.yml' COMMENT '工程中yml文件路径',
    `PIPELINE_ID` varchar(34) CHARACTER SET utf8mb4 NOT NULL DEFAULT '' COMMENT '对应蓝盾流水线ID',
    `DISPLAY_NAME` varchar(255) CHARACTER SET utf8mb4 NOT NULL DEFAULT '.ci' COMMENT 'GITCI流水线名称',
    `CREATOR` varchar(64) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '最初创建人',
    `ENABLED` bit(1) NOT NULL DEFAULT b'1' COMMENT '流水线启用状态',
    `LATEST_BUILD_ID` varchar(34) CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '最新一次构建ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    `VERSION` varchar(34) COLLATE utf8mb4_bin DEFAULT NULL COMMENT 'YAML版本号',
    `DIRECTORY` varchar(512) NOT NULL DEFAULT '.ci/' COMMENT '文件子路径',
    `LAST_UPDATE_BRANCH` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL,
    `LAST_EDIT_MODEL_MD5` varchar(34) CHARACTER SET utf8mb4 NULL COMMENT '最后一次修改的model的MD5值',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `PIPELINE_ID` (`PIPELINE_ID`),
    UNIQUE KEY `pipeline` (`GIT_PROJECT_ID`,`FILE_PATH`),
    KEY `projectId` (`GIT_PROJECT_ID`),
    KEY `pipelineId` (`PIPELINE_ID`),
    KEY `IDX_DIRECTORY` (`DIRECTORY`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='GIT流水线资源表-存储流水线相关信息';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `OBJECT_KIND` varchar(128) NOT NULL COMMENT '触发类型：push,tag_push,merge_request,issue,note,',
    `OPERATION_KIND` varchar(128) DEFAULT NULL COMMENT 'operation_kind, create, delete, update',
    `EXTENSION_ACTION` varchar(128) DEFAULT NULL COMMENT 'open, close, reopen, update, push-update, merge',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
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
    `SOURCE_GIT_PROJECT_ID` bigint(11) DEFAULT NULL COMMENT 'fork库GIT项目ID',
    `COMMIT_MESSAGE` text CHARACTER SET utf8mb4,
    `CHANGE_YAML_LIST` text COMMENT 'yaml变更文件列表',
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `idx_1` (`GIT_PROJECT_ID`),
    KEY `IDX_CREATE_TIME` (`CREATE_TIME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='STREAM 接受到的WebHooks请求';

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
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='STREAM WebHooks请求触发的构建记录';
	
	-- ----------------------------
-- Table structure for T_GIT_REQUEST_REPO_EVENT
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_REPO_EVENT` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `PIPELINE_ID` varchar(128) NOT NULL COMMENT '蓝盾流水线ID',
    `BUILD_ID` varchar(128) DEFAULT NULL COMMENT '蓝盾流水线BuildId',
    `TARGET_GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '流水线主库projectId',
    `SOURCE_GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT '触发库projectId',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `EVENT_ID` (`EVENT_ID`),
    KEY `PIPELINE_ID` (`PIPELINE_ID`),
    KEY `BUILD_ID` (`BUILD_ID`),
    KEY `TARGET_GIT_PROJECT_ID` (`TARGET_GIT_PROJECT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='repo hook 构建记录表';

-- ----------------------------
-- Table structure for T_GIT_REQUEST_EVENT_NOT_BUILD
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_GIT_REQUEST_EVENT_NOT_BUILD` (
    `ID` bigint(11) NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `EVENT_ID` bigint(11) NOT NULL COMMENT 'EVENT_ID',
    `ORIGIN_YAML` longtext COMMENT '初始yaml',
    `NORMALIZED_YAML` longtext COMMENT '格式化后的yaml',
    `REASON` varchar(1024) DEFAULT NULL COMMENT '未触发构建原因',
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
    `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `REASON_DETAIL` text,
    `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '匹配上的流水线ID',
    `FILE_PATH` varchar(1024) DEFAULT NULL COMMENT 'yml文件路径',
    `PARSED_YAML` longtext COMMENT '替换完模板的yaml',
    `VERSION` varchar(34) DEFAULT NULL COMMENT 'YAML版本号',
    `BRANCH` varchar(1024) DEFAULT NULL COMMENT 'git分支',
    PRIMARY KEY (`ID`,`CREATE_TIME`),
    KEY `EVENT_ID` (`EVENT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='STREAM WebHooks请求未触发的记录';

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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='STREAM 为前端页面提供的展示模组';

-- ----------------------------
-- Table structure for T_STREAM_PIPELINE_BRANCH
-- ----------------------------    
CREATE TABLE IF NOT EXISTS `T_STREAM_PIPELINE_BRANCH` (
    `GIT_PROJECT_ID` bigint(11) NOT NULL COMMENT 'GIT项目ID',
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

-- ----------------------------
-- Table structure for T_STREAM_DELETE_EVENT
-- ----------------------------
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='GIT流水线资源表-存储远程仓库流水线相关信息';

-- ----------------------------
-- Table structure for T_STREAM_PIPELINE_TRIGGER
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_STREAM_PIPELINE_TRIGGER`  (
  `PROJECT_ID` varchar(64) NOT NULL COMMENT '蓝盾项目ID',
  `PIPELINE_ID` varchar(128) NOT NULL COMMENT '蓝盾流水线ID',
  `BRANCH` varchar(255) NOT NULL COMMENT 'GIT分支',
  `CI_FILE_BLOB_ID` varchar(64) NOT NULL COMMENT '当前流水线ci文件的blobid',
  `TRIGGER` text NULL COMMENT '缓存的触发器',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`PROJECT_ID`, `PIPELINE_ID`, `BRANCH`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='STREAM缓存流水线触发器表';
