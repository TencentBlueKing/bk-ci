USE devops_ci_repository;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_REPOSITORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY` (
  `REPOSITORY_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户ID',
  `ALIAS_NAME` varchar(255) NOT NULL COMMENT '别名',
  `URL` varchar(255) NOT NULL COMMENT 'url地址',
  `TYPE` varchar(20) NOT NULL COMMENT '类型',
  `REPOSITORY_HASH_ID` varchar(64) DEFAULT NULL COMMENT '哈希ID',
  `CREATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' COMMENT '创建时间',
  `UPDATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `IS_DELETED` bit(1) NOT NULL COMMENT '是否删除 0 可用 1删除',
  `UPDATED_USER` varchar(64) NULL DEFAULT NULL COMMENT '代码库最近修改人 ',
  `ATOM` bit(1) DEFAULT b'0' COMMENT '是否为插件库(插件库不得修改和删除)',
  `ENABLE_PAC` bit(1) NOT NULL DEFAULT false COMMENT '是否开启pac',
  `YAML_SYNC_STATUS` VARCHAR(10) NULL COMMENT 'pac同步状态',
  `SCM_CODE` varchar(64) default null comment '代码库标识',
  PRIMARY KEY (`REPOSITORY_ID`),
  KEY `PROJECT_ID` (`PROJECT_ID`),
  KEY `inx_alias_name` (`ALIAS_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码库表';

-- ----------------------------
-- Table structure for T_REPOSITORY_CODE_GIT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_CODE_GIT` (
  `REPOSITORY_ID` bigint(20) NOT NULL COMMENT '仓库ID',
  `PROJECT_NAME` varchar(255) NOT NULL COMMENT '项目名称',
  `USER_NAME` varchar(64) NOT NULL COMMENT '用户名称',
  `CREATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' COMMENT '创建时间',
  `UPDATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `CREDENTIAL_ID` varchar(64) NOT NULL COMMENT '凭据 ID',
  `AUTH_TYPE` varchar(8) DEFAULT NULL COMMENT '认证方式',
  `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID',
  `CREDENTIAL_TYPE` varchar(64) DEFAULT NULL COMMENT '凭证类型',
  PRIMARY KEY (`REPOSITORY_ID`),
  INDEX IDX_GIT_PROJECT_ID(`GIT_PROJECT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工蜂代码库明细表';

-- ----------------------------
-- Table structure for T_REPOSITORY_CODE_GITLAB
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_CODE_GITLAB` (
  `REPOSITORY_ID` bigint(20) NOT NULL COMMENT '仓库ID',
  `PROJECT_NAME` varchar(255) NOT NULL COMMENT '项目名称',
  `CREDENTIAL_ID` varchar(64) NOT NULL COMMENT '凭据 ID',
  `CREATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' COMMENT '创建时间',
  `UPDATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `USER_NAME` varchar(64) NOT NULL COMMENT '用户名称',
  `AUTH_TYPE` varchar(8) DEFAULT NULL COMMENT '凭证类型',
   `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID',
  PRIMARY KEY (`REPOSITORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='gitlab代码库明细表';

-- ----------------------------
-- Table structure for T_REPOSITORY_CODE_SVN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_CODE_SVN` (
  `REPOSITORY_ID` bigint(20) NOT NULL COMMENT '仓库ID',
  `REGION` varchar(255) NOT NULL COMMENT '地区',
  `PROJECT_NAME` varchar(255) NOT NULL COMMENT '项目名称',
  `USER_NAME` varchar(64) NOT NULL COMMENT '用户名称',
  `CREATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' COMMENT '创建时间',
  `UPDATED_TIME` timestamp NOT NULL DEFAULT '2019-08-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `CREDENTIAL_ID` varchar(64) NOT NULL COMMENT '凭据 ID',
  `SVN_TYPE` varchar(32) DEFAULT '' COMMENT '仓库类型',
  `CREDENTIAL_TYPE` varchar(64) DEFAULT NULL COMMENT '凭证类型',
  PRIMARY KEY (`REPOSITORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='svn代码库明细表';

-- ----------------------------
-- Table structure for T_REPOSITORY_COMMIT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_COMMIT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `BUILD_ID` varchar(34) DEFAULT NULL COMMENT '构建ID',
  `PIPELINE_ID` varchar(34) DEFAULT NULL COMMENT '流水线ID',
  `REPO_ID` bigint(20) DEFAULT NULL COMMENT '代码库ID',
  `TYPE` smallint(6) DEFAULT NULL COMMENT '1-svn, 2-git, 3-gitlab',
  `COMMIT` varchar(64) DEFAULT NULL COMMENT '提交',
  `COMMITTER` varchar(32) DEFAULT NULL COMMENT '提交者',
  `COMMIT_TIME` datetime DEFAULT NULL COMMENT '提交时间',
  `COMMENT` longtext COMMENT '评论',
  `ELEMENT_ID` varchar(34) DEFAULT NULL COMMENT '原子ID',
  `REPO_NAME` varchar(128) DEFAULT NULL COMMENT '代码库别名',
  `URL` varchar(255) DEFAULT NULL COMMENT '代码库URL',
  PRIMARY KEY (`ID`),
  KEY `IDX_BUILD_ID_TIME` (`BUILD_ID`, `COMMIT_TIME`),
  KEY `IDX_PIPE_ELEMENT_REPO_TIME` (`PIPELINE_ID`, `ELEMENT_ID`, `REPO_ID`, `COMMIT_TIME`),
  KEY `IDX_PIPE_ELEMENT_NAME_REPO_TIME` (`PIPELINE_ID`, `ELEMENT_ID`, `REPO_NAME`, `COMMIT_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码库变更记录';

-- ----------------------------
-- Table structure for T_REPOSITORY_GITHUB
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GITHUB` (
  `REPOSITORY_ID` bigint(20) NOT NULL COMMENT '仓库ID',
  `CREDENTIAL_ID` varchar(128) DEFAULT NULL COMMENT '凭据 ID',
  `PROJECT_NAME` varchar(255) NOT NULL COMMENT '项目名称',
  `USER_NAME` varchar(64) NOT NULL COMMENT '用户名称',
  `CREATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATED_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `GIT_PROJECT_ID` bigint(20) DEFAULT 0 COMMENT 'GIT项目ID',
  PRIMARY KEY (`REPOSITORY_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='github代码库明细表';

-- ----------------------------
-- Table structure for T_REPOSITORY_GITHUB_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GITHUB_TOKEN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `ACCESS_TOKEN` varchar(96) NOT NULL COMMENT '权限Token',
  `TOKEN_TYPE` varchar(64) NOT NULL COMMENT 'token类型',
  `SCOPE` text NOT NULL COMMENT '生效范围',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `TYPE` varchar(32) DEFAULT 'GITHUB_APP' COMMENT 'GitHub token类型（GITHUB_APP、OAUTH_APP）',
  `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `USER_ID` (`USER_ID`, `TYPE`),
  INDEX `IDX_REPOSITORY_GITHUB_TOKEN_OPERATOR`(`OPERATOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='github oauth token表';

-- ----------------------------
-- Table structure for T_REPOSITORY_GIT_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GIT_TOKEN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) DEFAULT NULL COMMENT '用户ID',
  `ACCESS_TOKEN` varchar(96) DEFAULT NULL COMMENT '权限Token',
  `REFRESH_TOKEN` varchar(96) DEFAULT NULL COMMENT '刷新token',
  `TOKEN_TYPE` varchar(64) DEFAULT NULL COMMENT 'token类型',
  `EXPIRES_IN` bigint(20) DEFAULT NULL COMMENT '过期时间',
  `CREATE_TIME` datetime DEFAULT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'token的创建时间',
  `UPDATE_TIME` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
  `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `USER_ID` (`USER_ID`),
  INDEX `IDX_REPOSITORY_GIT_TOKEN_OPERATOR`(`OPERATOR`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工蜂commit checker表';


-- ----------------------------
-- Table structure for T_REPOSITORY_GIT_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_GIT_CHECK` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `PIPELINE_ID` varchar(64) NOT NULL COMMENT '流水线ID',
  `BUILD_NUMBER` int(11) NOT NULL COMMENT '构建编号',
  `REPO_ID` varchar(64) DEFAULT NULL COMMENT '代码库ID',
  `COMMIT_ID` varchar(64) NOT NULL COMMENT '代码提交ID',
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `REPO_NAME` varchar(128) DEFAULT NULL COMMENT '代码库别名',
  `CONTEXT` varchar(255) DEFAULT NULL COMMENT '内容',
  `SOURCE` varchar(64) NOT NULL COMMENT '事件来源',
  `TARGET_BRANCH` varchar(1024) NOT NULL DEFAULT '' COMMENT '目标分支',
  PRIMARY KEY (`ID`),
  KEY `PIPELINE_ID_COMMIT_ID` (`PIPELINE_ID`,`COMMIT_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工蜂oauth token表';

-- ----------------------------
-- Table structure for T_REPOSITORY_GIT_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_CODE_P4` (
  `REPOSITORY_ID` BIGINT(20) NOT NULL,
  `PROJECT_NAME` VARCHAR(255) NOT NULL,
  `USER_NAME` VARCHAR(64) NOT NULL,
  `CREDENTIAL_ID` VARCHAR(64) NOT NULL,
  `CREATED_TIME` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `UPDATED_TIME` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`REPOSITORY_ID`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_TGIT_TOKEN`
(
    `ID`            bigint auto_increment comment '主键ID'
        primary key,
    `USER_ID`       varchar(64)                        null comment '用户ID',
    `ACCESS_TOKEN`  varchar(96)                        null comment '权限Token',
    `REFRESH_TOKEN` varchar(96)                        null comment '刷新token',
    `TOKEN_TYPE`    varchar(64)                        null comment 'token类型',
    `EXPIRES_IN`    bigint                             null comment '过期时间',
    `CREATE_TIME`   datetime default CURRENT_TIMESTAMP null comment 'token的创建时间',
    `OAUTH_USER_ID` varchar(64)                        not null comment '账户实际名称',
    constraint `USER_ID`
        unique (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 comment '外网工蜂OAUTH token表';

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_PIPELINE_REF`
(
    `ID`              bigint auto_increment,
    `PROJECT_ID`      varchar(64)  NOT NULL COMMENT '蓝盾项目ID',
    `PIPELINE_ID`     varchar(64)  NOT NULL COMMENT '流水线ID',
    `PIPELINE_NAME`   varchar(255) NOT NULL COMMENT '流水线名称',
    `REPOSITORY_ID`   bigint(20)   NOT NULL COMMENT '代码库ID',
    `TASK_ID`         varchar(64)  NOT NULL COMMENT '任务ID',
    `TASK_NAME`       varchar(128) NULL COMMENT '原子名称，用户是可以修改',
    `ATOM_CODE`       varchar(32)  NOT NULL DEFAULT '' COMMENT '插件的唯一标识',
    `ATOM_CATEGORY`   varchar(10)  NOT NULL DEFAULT '' COMMENT '插件类别',
    `TASK_PARAMS`     text         NOT NULL COMMENT '插件参数',
    `TASK_REPO_TYPE`     varchar(10)         NOT NULL COMMENT '插件代码库类型配置',
    `TASK_REPO_HASH_ID`     varchar(64)      NULL COMMENT '插件代码库hashId配置',
    `TASK_REPO_NAME`     varchar(255)      NULL COMMENT '插件代码库别名配置',
    `TRIGGER_TYPE`    varchar(64)  NULL COMMENT '触发类型',
    `EVENT_TYPE`      varchar(64)  NULL COMMENT '事件类型',
    `TRIGGER_CONDITION` text       NULL COMMENT '触发条件',
    `TRIGGER_CONDITION_MD5` varchar(64)  NULL COMMENT '触发条件md5',
    `CREATE_TIME`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CHANNEL` varchar(32) DEFAULT 'BS' COMMENT '流水线渠道',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UQE_PROJECT_PIPELINE_TASK` (`PROJECT_ID`, `PIPELINE_ID`, `TASK_ID`),
    INDEX `IDX_PROJECT_REPOSITORY` (`PROJECT_ID`, `REPOSITORY_ID`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '流水线引用代码库表';


-- ----------------------------
-- Table structure for T_REPOSITORY_WEBHOOK_REQUEST
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_WEBHOOK_REQUEST`
(
    `REQUEST_ID`      varchar(64)  NOT NULL COMMENT '请求ID',
    `EXTERNAL_ID`     varchar(255)                       DEFAULT NULL COMMENT '代码库平台ID',
    `REPOSITORY_TYPE` varchar(32)                        DEFAULT NULL COMMENT '触发类型',
    `EVENT_TYPE`      varchar(255)                       DEFAULT NULL COMMENT '事件类型',
    `TRIGGER_USER`    varchar(100)              NOT NULL COMMENT '触发用户',
    `EVENT_MESSAGE`   text                      NOT NULL COMMENT '事件信息',
    `REQUEST_HEADER`  text COMMENT '事件请求头',
    `REQUEST_PARAM`   text COMMENT '事件请求参数',
    `REQUEST_BODY`    text COMMENT '事件请求体',
    `CREATE_TIME`     timestamp                 NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`REQUEST_ID`, `CREATE_TIME`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='代码库WEBHOOK请求表';


-- ----------------------------
-- Table structure for T_REPOSITORY_SCM_TOKEN
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_SCM_TOKEN` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户名',
    `SCM_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '代码库类型',
    `APP_TYPE` varchar(64) NOT NULL DEFAULT '' COMMENT 'app类型',
    `ACCESS_TOKEN` varchar(256) DEFAULT NULL COMMENT 'access token 密文',
    `REFRESH_TOKEN` varchar(256) DEFAULT NULL COMMENT 'access refresh token',
    `EXPIRES_IN` bigint(20) DEFAULT NULL COMMENT '过期时间',
    `CREATE_TIME` datetime DEFAULT NULL COMMENT '创建时间',
    `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
    `OPERATOR` varchar(64) DEFAULT NULL COMMENT '操作人',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNIQ_USER_SCM_CODE_APP_TYPE` (`USER_ID`,`SCM_CODE`,`APP_TYPE`),
    INDEX `IDX_REPOSITORY_SCM_TOKEN_OPERATOR`(`OPERATOR`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='代码仓库token表';

CREATE TABLE IF NOT EXISTS `T_REPOSITORY_COPILOT_SUMMARY` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '蓝盾项目ID',
    `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
    `ELEMENT_ID` varchar(64) NOT NULL COMMENT '插件ID',
    `SCM_TYPE` varchar(32) DEFAULT NULL COMMENT '代码库类型',
    `PROJECT_NAME` varchar(255) DEFAULT NULL COMMENT '仓库唯一标识',
    `SOURCE_COMMIT` varchar(64) DEFAULT NULL COMMENT '源提交点',
    `TARGET_COMMIT` varchar(64) DEFAULT NULL COMMENT '目标提交点',
    `STATUS` int(11) DEFAULT NULL COMMENT '生成状态，1-生成中，3-生成失败, 5-生成成功',
    `SUMMARY` text COMMENT 'AI 摘要',
    `CREATE_TIME` timestamp NOT NULL COMMENT '创建时间',
    PRIMARY KEY (ID, CREATE_TIME),
    INDEX `IDX_PROJECT_BUILD_ELEMENT`(`PROJECT_ID`, `BUILD_ID`, `ELEMENT_ID`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4 COMMENT='代码库copilot摘要表';


-- ----------------------------
-- Table structure for T_REPOSITORY_SCM_PROVIDER
-- ----------------------------
CREATE TABLE IF NOT EXISTS T_REPOSITORY_SCM_PROVIDER
(
    `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `PROVIDER_CODE`     varchar(64)  not null comment '提供者标识',
    `PROVIDER_TYPE` varchar(32)  not null comment '提供者类型,如git,svn',
    `NAME`     varchar(255) not null comment '提供者名称',
    `DESC` varchar(255) null comment '提供者描述',
    `SCM_TYPE` varchar(32)  not null comment '源代码类型,如git,svn',
    `LOGO_URL` varchar(256) comment '提供者icon url',
    `DOC_URL` varchar(256) comment '文档链接',
    `CREDENTIAL_TYPE_LIST` varchar(256) not null comment '支持的授权类型',
    `API`  bit  not null default b'0' comment '是否支持api接口',
    `MERGE` bit  not null default b'0' comment '是否支持merge',
    `WEBHOOK` bit  not null default b'0' comment '是否支持webhook',
    `WEBHOOK_SECRET_TYPE` varchar(64)  null comment 'webhook鉴权类型,APP,REQUEST_HEADER',
    `WEBHOOK_PROPS` text         not null comment 'webhook配置',
    `PAC` bit  not null default b'0' comment '是否支持PAC',
    `CREATE_TIME`   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    `UPDATE_TIME`   datetime default CURRENT_TIMESTAMP not null comment '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE `UNI_CODE`(`PROVIDER_CODE`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='源代码管理提供者';

-- ----------------------------
-- Table structure for T_REPOSITORY_SCM_CONFIG
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REPOSITORY_SCM_CONFIG`
(
    `ID` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `SCM_CODE`            varchar(64)  not null comment '代码库标识',
    `NAME`            varchar(255) not null comment '代码库名称',
    `PROVIDER_CODE`   varchar(32)  not null comment '源代码平台提供者,如github、gitlab',
    `SCM_TYPE`        varchar(32)  not null comment '源代码类型,如git,svn',
    `HOSTS`           varchar(256) null comment '代码库域名,支持多个',
    `LOGO_URL`        varchar(256) not null comment 'logo链接',
    `CREDENTIAL_TYPE_LIST`  varchar(256) not null comment '支持的授权类型',
    `OAUTH_TYPE`      varchar(32) not null comment 'oauth类型, NEW-新增,REUSE-复用',
    `OAUTH_SCM_CODE`  varchar(64) null   comment 'oauth代码库标识,OAUTH_TYPE为REUSE有值',
    `STATUS`          varchar(32)  not null default 'SUCCEED' comment '状态, SUCCEED-配置成功,DEPLOYING-配置中,DISABLED-禁用',
    `OAUTH2_ENABLED`   bit          not null default b'0' comment '是否能够使用oauth2',
    `MERGE_ENABLED`   bit          not null default b'0' comment '是否能够使用merge功能',
    `PAC_ENABLED`     bit          not null default b'0' comment '是否能够使用PAC功能',
    `WEBHOOK_ENABLED` bit          not null default b'0' comment '是否能够使用webhook功能',
    `PROVIDER_PROPS`           text         not null comment '提供者属性',
    `CREATOR`         varchar(255) DEFAULT NULL COMMENT '创建者',
    `UPDATER`         varchar(255) DEFAULT NULL COMMENT '更新人',
    `CREATE_TIME`     datetime              default CURRENT_TIMESTAMP not null comment '创建时间',
    `UPDATE_TIME`     datetime              default CURRENT_TIMESTAMP not null comment '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE `UNI_CODE`(`SCM_CODE`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='源代码管理配置';

-- ----------------------------
-- Table structure for T_REPOSITORY_SCM_CONFIG_VISIBILITY
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_REPOSITORY_SCM_CONFIG_VISIBILITY` (
    `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `SCM_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '代码源标识',
    `DEPT_ID` int(11) NOT NULL COMMENT '所属机构节点ID',
    `DEPT_NAME` varchar(1024) NOT NULL COMMENT '所属机构节点名称，包含父级节点名称',
    `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `UNIQ_RSCV_UNIQUE` (`SCM_CODE`,`DEPT_ID`),
    KEY `IDX_RSCV_DEPT_ID` (`DEPT_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码源可见范围表';

SET FOREIGN_KEY_CHECKS = 1;
