USE devops_ci_project;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_ACTIVITY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_ACTIVITY` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `TYPE` varchar(32) NOT NULL COMMENT '类型',
  `NAME` varchar(128) NOT NULL COMMENT '名称',
  `ENGLISH_NAME` varchar(128) DEFAULT NULL COMMENT '英文名称',
  `LINK` varchar(1024) NOT NULL COMMENT '跳转链接',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `STATUS` varchar(32) NOT NULL COMMENT '状态',
  `CREATOR` varchar(32) NOT NULL COMMENT '创建者',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `NAME_TYPE` (`NAME`,`TYPE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

-- ----------------------------
-- Table structure for T_MESSAGE_CODE_DETAIL
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_MESSAGE_CODE_DETAIL` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `MESSAGE_CODE` varchar(128) NOT NULL COMMENT 'code码',
  `MODULE_CODE` char(2) NOT NULL COMMENT '模块代码',
  `MESSAGE_DETAIL_ZH_CN` varchar(500) NOT NULL COMMENT '中文简体描述信息',
  `MESSAGE_DETAIL_ZH_TW` varchar(500) DEFAULT NULL COMMENT '中文繁体描述信息',
  `MESSAGE_DETAIL_EN` varchar(500) DEFAULT NULL COMMENT '英文描述信息',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uni_inx_tmcd_message_code` (`MESSAGE_CODE`),
  KEY `inx_tmcd_module_code` (`MODULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='code码详情表';

-- ----------------------------
-- Table structure for T_USER
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_USER` (
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `NAME` varchar(64) NOT NULL COMMENT '名称',
  `BG_ID` int(11) NOT NULL COMMENT '事业群ID',
  `BG_NAME` varchar(256) NOT NULL COMMENT '事业群名称',
  `DEPT_ID` int(11) DEFAULT NULL COMMENT '项目所属二级机构ID',
  `DEPT_NAME` varchar(256) DEFAULT NULL COMMENT '项目所属二级机构名称',
  `CENTER_ID` int(11) DEFAULT NULL COMMENT '中心ID',
  `CENTER_NAME` varchar(256) DEFAULT NULL COMMENT '中心名字',
  `GROYP_ID` int(11) DEFAULT NULL COMMENT '用户组ID',
  `GROUP_NAME` varchar(256) DEFAULT NULL COMMENT '用户组名称',
  `CREATE_TIME` datetime NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `USER_TYPE` bit(1) NOT NULL DEFAULT b'0' COMMENT '用户类型0普通用户 1公共账号',
  PRIMARY KEY (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='用户表';

-- ----------------------------
-- Table structure for T_USER_DAILY_FIRST_AND_LAST_LOGIN
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_USER_DAILY_FIRST_AND_LAST_LOGIN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `DATE` date NOT NULL COMMENT '日期',
  `FIRST_LOGIN_TIME` datetime NOT NULL COMMENT '首次登录时间',
  `LAST_LOGIN_TIME` datetime NOT NULL COMMENT '最近登录时间',
  PRIMARY KEY (`ID`),
  KEY `DATE_AND_USER_ID` (`DATE`,`USER_ID`) USING BTREE,
  KEY `USER_ID_AND_DAE` (`USER_ID`,`DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='';

-- ----------------------------
-- Table structure for T_USER_DAILY_LOGIN
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_USER_DAILY_LOGIN` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `DATE` date NOT NULL COMMENT '日期',
  `LOGIN_TIME` datetime NOT NULL COMMENT '登录时间',
  `OS` varchar(32) NOT NULL COMMENT '操作系统',
  `IP` varchar(32) NOT NULL COMMENT 'ip地址',
  PRIMARY KEY (`ID`),
  KEY `USER_ID_AND_DATE` (`USER_ID`,`DATE`) USING BTREE,
  KEY `DATE_AND_USER_ID` (`DATE`,`USER_ID`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='';

-- ----------------------------
-- Table structure for T_FAVORITE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_FAVORITE` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
  `service_id` bigint(20) DEFAULT NULL COMMENT '服务id',
  `username` varchar(64) DEFAULT NULL COMMENT '用户',
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`service_id`,`username`),
  KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=109 DEFAULT CHARSET=utf8 COMMENT='关注收藏表';

-- ----------------------------
-- Table structure for T_PROJECTS
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PROJECT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `created_at` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `extra` text COMMENT '额外信息',
  `creator` varchar(32) DEFAULT NULL COMMENT '创建者',
  `description` text COMMENT '描述',
  `kind` int(10) DEFAULT NULL COMMENT '容器类型',
  `cc_app_id` bigint(20) DEFAULT NULL COMMENT '应用ID',
  `cc_app_name` varchar(64) DEFAULT NULL COMMENT '应用名称',
  `is_offlined` bit(1) DEFAULT b'0' COMMENT '是否停用',
  `PROJECT_ID` varchar(32) NOT NULL COMMENT '项目ID',
  `project_name` varchar(64) NOT NULL COLLATE utf8mb4_bin COMMENT '项目名称',
  `english_name` varchar(64) NOT NULL COMMENT '英文名称',
  `updator` varchar(32) DEFAULT NULL COMMENT '更新人',
  `project_type` int(10) DEFAULT NULL COMMENT '项目类型',
  `use_bk` bit(1) DEFAULT b'1' COMMENT '是否用蓝鲸',
  `deploy_type` text COMMENT '部署类型',
  `bg_id` bigint(20) DEFAULT NULL COMMENT '事业群ID',
  `bg_name` varchar(255) DEFAULT NULL COMMENT '事业群名称',
  `dept_id` bigint(20) DEFAULT NULL COMMENT '项目所属二级机构ID',
  `dept_name` varchar(255) DEFAULT NULL COMMENT '项目所属二级机构名称',
  `center_id` bigint(20) DEFAULT NULL COMMENT '中心ID',
  `center_name` varchar(255) DEFAULT NULL COMMENT '中心名字',
  `data_id` bigint(20) DEFAULT NULL COMMENT '数据ID',
  `is_secrecy` bit(1) DEFAULT b'0' COMMENT '是否保密',
  `is_helm_chart_enabled` bit(1) DEFAULT b'0' COMMENT '是否启用图表激活',
  `approval_status` int(10) DEFAULT '1' COMMENT '审核状态',
  `logo_addr` text COMMENT 'logo地址',
  `approver` varchar(32) DEFAULT NULL COMMENT '批准人',
  `remark` text COMMENT '评论',
  `approval_time` timestamp NULL DEFAULT NULL COMMENT '批准时间',
  `creator_bg_name` varchar(128) DEFAULT '' COMMENT '创建者事业群名称',
  `creator_dept_name` varchar(128) DEFAULT '' COMMENT '创建者项目所属二级机构名称',
  `creator_center_name` varchar(128) DEFAULT '' COMMENT '创建者中心名字',
  `hybrid_cc_app_id` bigint(20) DEFAULT NULL COMMENT '应用ID',
  `enable_external` bit(1) DEFAULT NULL COMMENT '是否支持构建机访问外网',
  `enable_idc` bit(1) DEFAULT NULL COMMENT '是否支持IDC构建机',
  `enabled` bit(1) DEFAULT NULL COMMENT '是否启用',
  `CHANNEL` varchar(32) NOT NULL DEFAULT 'BS' COMMENT '项目渠道',
  `pipeline_limit` int(10) DEFAULT 500 COMMENT '流水线数量上限',
  `router_tag` varchar(32) DEFAULT NULL COMMENT '网关路由tags',
  `relation_id` varchar(32) DEFAULT NULL COMMENT '扩展系统关联ID',
  `other_router_tags` varchar(128) DEFAULT NULL COMMENT '其他系统网关路由tags',
  `properties` text NULL COMMENT '项目其他配置',
  `SUBJECT_SCOPES` text DEFAULT NULL COMMENT '最大可授权人员范围',
  `AUTH_SECRECY` int(10) DEFAULT b'0' COMMENT '项目性质,0-公开，1-保密,2-机密',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `project_name` (`project_name`) USING BTREE,
  UNIQUE KEY `project_id` (`project_id`) USING BTREE,
  UNIQUE KEY `english_name` (`english_name`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目信息表';

-- ----------------------------
-- Table structure for T_SERVICE
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_SERVICE` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `name` varchar(64) DEFAULT NULL COMMENT '名称',
  `english_name` varchar(64) DEFAULT NULL COMMENT '英文名称',
  `service_type_id` bigint(20) DEFAULT NULL COMMENT '服务类型ID',
  `link` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `link_new` varchar(255) DEFAULT NULL COMMENT '新跳转链接',
  `inject_type` varchar(64) DEFAULT NULL COMMENT '注入类型',
  `iframe_url` varchar(255) DEFAULT NULL COMMENT 'iframe Url地址',
  `css_url` varchar(255) DEFAULT NULL COMMENT 'css Url地址',
  `js_url` varchar(255) DEFAULT NULL COMMENT 'js Url地址',
  `show_project_list` bit(1) DEFAULT NULL COMMENT '是否在页面显示',
  `show_nav` bit(1) DEFAULT NULL COMMENT 'show Nav',
  `project_id_type` varchar(64) DEFAULT NULL COMMENT '项目ID类型',
  `status` varchar(64) DEFAULT NULL COMMENT '状态',
  `created_user` varchar(64) DEFAULT NULL COMMENT '创建者',
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_user` varchar(64) DEFAULT NULL COMMENT '修改者',
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bit(1) DEFAULT NULL COMMENT '是否删除',
  `gray_css_url` varchar(255) DEFAULT NULL COMMENT '灰度css Url地址',
  `gray_js_url` varchar(255) DEFAULT NULL COMMENT '灰度js Url地址',
  `logo_url` varchar(256) DEFAULT NULL COMMENT 'logo地址',
  `web_socket` text COMMENT '支持webSocket的页面',
  `weight` int(11) DEFAULT NULL COMMENT '权值',
  `gray_iframe_url` varchar(255) DEFAULT NULL COMMENT '灰度iframe Url地址',
  `new_window` bit(1) DEFAULT b'0' COMMENT '是否打开新标签页',
  `new_windowUrl` varchar(200) DEFAULT '' COMMENT '新标签页地址',
  PRIMARY KEY (`id`),
  UNIQUE KEY `service_name` (`name`),
  UNIQUE KEY `IDX_UNIQUE_ENGLISH_NAME` (`english_name`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8 COMMENT='服务信息表';

-- ----------------------------
-- Table structure for T_SERVICE_TYPE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_SERVICE_TYPE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(64) DEFAULT NULL COMMENT '邮件标题',
  `english_title` varchar(64) DEFAULT NULL COMMENT '英文邮件标题',
  `created_user` varchar(64) DEFAULT NULL COMMENT '创建者',
  `created_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_user` varchar(64) DEFAULT NULL COMMENT '修改者',
  `updated_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` bit(1) DEFAULT NULL COMMENT '是否删除',
  `weight` int(11) DEFAULT NULL COMMENT '权值',
  PRIMARY KEY (`id`),
  UNIQUE KEY `title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务类型表';

CREATE TABLE IF NOT EXISTS `T_GRAY_TEST`
(
    `id`         bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键id',
    `service_id` bigint(20)  DEFAULT NULL COMMENT '服务id',
    `username`   varchar(64) DEFAULT NULL COMMENT '用户',
    `status`     varchar(64) DEFAULT NULL COMMENT '服务状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `service_name` (`service_id`, `username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `LABEL_NAME`  varchar(45) NOT NULL COMMENT '标签名称',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_inx_tmpl_name` (`LABEL_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_PROJECT_LABEL_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
    `LABEL_ID`    varchar(32) NOT NULL COMMENT '标签ID',
    `PROJECT_ID`  varchar(32) NOT NULL COMMENT '项目ID',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    KEY `inx_tmplr_label_id` (`LABEL_ID`),
    KEY `inx_tmplr_project_id` (`PROJECT_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_NOTICE` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NOTICE_TITLE` varchar(100) NOT NULL COMMENT '公告标题',
  `EFFECT_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '生效日期',
  `INVALID_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失效日期',
  `CREATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建日期',
  `UPDATE_DATE` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新日期',
  `NOTICE_CONTENT` text NOT NULL COMMENT '公告内容',
  `REDIRECT_URL` varchar(200) DEFAULT NULL COMMENT '跳转地址',
  `NOTICE_TYPE` tinyint(4) NOT NULL DEFAULT '0' COMMENT '消息类型:0.弹框 1.跑马灯',
  `SERVICE_NAME` varchar(1024) DEFAULT NULL COMMENT '服务名称',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tn_effect_date` (`EFFECT_DATE`) USING BTREE,
  KEY `inx_tn_invalid_date` (`INVALID_DATE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

CREATE TABLE IF NOT EXISTS `T_SHARDING_ROUTING_RULE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `ROUTING_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '路由名称',
  `ROUTING_RULE` varchar(256) NOT NULL DEFAULT '' COMMENT '路由规则',
  `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT 'prod' COMMENT '集群名称',
  `MODULE_CODE` varchar(64) NOT NULL DEFAULT 'PROCESS' COMMENT '模块标识',
  `TYPE` varchar(32) NOT NULL DEFAULT 'DB' COMMENT '路由类型，DB:数据库，TABLE:数据库表',
  `DATA_SOURCE_NAME` varchar(128) NOT NULL DEFAULT 'ds_0' COMMENT '数据源名称',
  `TABLE_NAME` varchar(128) DEFAULT NULL COMMENT '数据库表名称',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY  `UNI_INX_TSRR_CLUSTER_MODULE_TYPE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TYPE`, `ROUTING_NAME`, `TABLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DB分片路由规则';

CREATE TABLE IF NOT EXISTS `T_DATA_SOURCE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '集群名称',
  `MODULE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '模块标识',
  `DATA_SOURCE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '数据源名称',
  `FULL_FLAG` bit(1) DEFAULT b'0' COMMENT '容量是否满标识 true：是，false：否',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `DS_URL` varchar(1024) NULL COMMENT '数据源URL地址',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY  `uni_inx_tds_module_name` (`CLUSTER_NAME`, `MODULE_CODE`,`DATA_SOURCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块数据源配置';

CREATE TABLE IF NOT EXISTS `T_LEAF_ALLOC` (
  `BIZ_TAG` varchar(128) NOT NULL DEFAULT '' COMMENT '业务标签',
  `MAX_ID` bigint(20) NOT NULL DEFAULT '1' COMMENT '当前最大ID值',
  `STEP` int(11) NOT NULL COMMENT '步长,每一次请求获取的ID个数',
  `DESCRIPTION` varchar(256) DEFAULT NULL COMMENT '说明',
  `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`BIZ_TAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID管理数据表';

CREATE TABLE IF NOT EXISTS `T_TABLE_SHARDING_CONFIG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '集群名称',
  `MODULE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '模块标识',
  `TABLE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '数据库表名称',
  `SHARDING_NUM` int(11) NOT NULL DEFAULT 5 COMMENT '分表数量',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TTSC_CLUSTER_MODULE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TABLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库表分片配置';

CREATE TABLE IF NOT EXISTS `T_I18N_MESSAGE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `MODULE_CODE` varchar(64) NOT NULL COMMENT '模块标识',
  `LANGUAGE` varchar(64) NOT NULL DEFAULT '' COMMENT '国际化语言信息',
  `KEY` varchar(256) NOT NULL COMMENT '国际化变量名',
  `VALUE` text NOT NULL COMMENT '国际化变量值',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TIM_MODULE_KEY_LANGUAGE` (`MODULE_CODE`,`KEY`,`LANGUAGE`),
  KEY `INX_TIM_KEY` (`KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='国际化信息表';

CREATE TABLE IF NOT EXISTS `T_USER_LOCALE` (
 `USER_ID` varchar(64) NOT NULL DEFAULT '' COMMENT '用户ID',
 `LANGUAGE` varchar(64) NOT NULL DEFAULT '' COMMENT '国际化语言信息',
 `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
 `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
 PRIMARY KEY (`USER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户设置的国际化信息';

CREATE TABLE IF NOT EXISTS `T_PROJECT_APPROVAL` (
   `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
   `PROJECT_NAME` varchar(64) NOT NULL COLLATE utf8mb4_bin COMMENT '项目名称',
   `ENGLISH_NAME` varchar(64) NOT NULL COMMENT '英文名称',
   `DESCRIPTION` text COMMENT '描述',
   `CREATED_AT` timestamp NULL DEFAULT NULL COMMENT '创建时间',
   `UPDATED_AT` timestamp NULL DEFAULT NULL COMMENT '更新时间',
   `CREATOR` varchar(32) DEFAULT NULL COMMENT '创建者',
   `UPDATOR` varchar(32) DEFAULT NULL COMMENT '更新人',
   `BG_ID` bigint(20) DEFAULT NULL COMMENT '事业群ID',
   `BG_NAME` varchar(255) DEFAULT NULL COMMENT '事业群名称',
   `DEPT_ID` bigint(20) DEFAULT NULL COMMENT '项目所属二级机构ID',
   `DEPT_NAME` varchar(255) DEFAULT NULL COMMENT '项目所属二级机构名称',
   `CENTER_ID` bigint(20) DEFAULT NULL COMMENT '中心ID',
   `CENTER_NAME` varchar(255) DEFAULT NULL COMMENT '中心名字',
   `LOGO_ADDR` text COMMENT 'logo地址',
   `APPROVER` varchar(32) DEFAULT NULL COMMENT '批准人',
   `APPROVAL_STATUS` int(10) DEFAULT '1' COMMENT '审核状态',
   `APPROVAL_TIME` timestamp NULL DEFAULT NULL COMMENT '批准时间',
   `RELATION_ID` varchar(32) DEFAULT NULL COMMENT '扩展系统关联ID',
   `SUBJECT_SCOPES` text DEFAULT NULL COMMENT '最大可授权人员范围',
   `AUTH_SECRECY` int(10) DEFAULT b'0' COMMENT '项目性质,0-公开,1-保密,2-机密',
   `TIPS_STATUS` int(10) DEFAULT b'0' COMMENT '提示状态,0-不展示,1-展示创建成功,2-展示更新成功',
   `PROJECT_TYPE` int(10) DEFAULT NULL comment '项目类型',
   PRIMARY KEY (`ID`) USING BTREE,
   UNIQUE KEY `project_name` (`PROJECT_NAME`) USING BTREE,
   UNIQUE KEY `english_name` (`ENGLISH_NAME`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目审批表';

SET FOREIGN_KEY_CHECKS = 1;
