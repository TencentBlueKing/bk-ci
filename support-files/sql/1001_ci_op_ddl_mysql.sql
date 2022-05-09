USE devops_ci_op;

SET FOREIGN_KEY_CHECKS=0;

--
-- Table structure for table `SPRING_SESSION_ATTRIBUTES`
--

CREATE TABLE IF NOT EXISTS  `SPRING_SESSION_ATTRIBUTES` (
  `SESSION_ID` char(36) NOT NULL COMMENT 'SESSION ID',
  `ATTRIBUTE_NAME` varchar(200) NOT NULL COMMENT '属性名称',
  `ATTRIBUTE_BYTES` blob COMMENT '属性字节',
  PRIMARY KEY (`SESSION_ID`,`ATTRIBUTE_NAME`),
  KEY `SPRING_SESSION_ATTRIBUTES_IX1` (`SESSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `dept_info`
--


CREATE TABLE IF NOT EXISTS  `dept_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `dept_id` int(11) NOT NULL COMMENT '项目所属二级机构ID',
  `dept_name` varchar(100) NOT NULL COMMENT '项目所属二级机构名称',
  `level` int(11) NOT NULL COMMENT '层级ID',
  `parent_dept_id` int(11) DEFAULT NULL COMMENT '',
  `UPDATE_TIME` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bd8ig9ecbopp3592f9fcpb99p` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='';

--
-- Table structure for table `project_info`
--

CREATE TABLE IF NOT EXISTS  `project_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `approval_status` int(11) DEFAULT NULL COMMENT '审核状态',
  `approval_time` datetime DEFAULT NULL COMMENT '批准时间',
  `approver` varchar(100) DEFAULT NULL COMMENT '批准人',
  `cc_app_id` int(11) DEFAULT NULL COMMENT '应用ID',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `creator` varchar(100) DEFAULT NULL COMMENT '创建者',
  `creator_bg_name` varchar(100) DEFAULT NULL COMMENT '创建者事业群名称',
  `creator_center_name` varchar(100) DEFAULT NULL COMMENT '创建者中心名字',
  `creator_dept_name` varchar(100) DEFAULT NULL COMMENT '创建者项目所属二级机构名称',
  `english_name` varchar(255) DEFAULT NULL COMMENT '英文名称',
  `is_offlined` bit(1) DEFAULT NULL COMMENT '是否停用',
  `is_secrecy` bit(1) DEFAULT NULL COMMENT '是否保密',
  `project_bg_id` int(11) DEFAULT NULL COMMENT '事业群ID',
  `project_bg_name` varchar(100) DEFAULT NULL COMMENT '事业群名称',
  `project_center_id` varchar(50) DEFAULT NULL COMMENT '中心ID',
  `project_center_name` varchar(100) DEFAULT NULL COMMENT '中心名字',
  `project_dept_id` int(11) DEFAULT NULL COMMENT '机构ID',
  `project_dept_name` varchar(100) DEFAULT NULL COMMENT '项目所属二级机构名称',
  `project_id` varchar(100) DEFAULT NULL COMMENT '项目ID',
  `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
  `project_type` int(11) DEFAULT NULL COMMENT '项目类型',
  `use_bk` bit(1) DEFAULT NULL COMMENT '是否用蓝鲸',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bvtnw8dekf2y9gbxt7thib8vj` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='';

--
-- Table structure for table `role`
--

CREATE TABLE IF NOT EXISTS  `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `name` varchar(255) NOT NULL COMMENT '名称',
  `ch_name` varchar(255) DEFAULT NULL COMMENT '分支名',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8sewwnpamngi6b1dwaa88askk` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `role_permission`
--

CREATE TABLE IF NOT EXISTS  `role_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `role_id` int(11) DEFAULT NULL COMMENT '角色ID',
  `url_action_id` int(11) DEFAULT NULL COMMENT '',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `FKa6jx8n8xkesmjmv6jqug6bg68` (`role_id`),
  KEY `FKij92vnr0qkd97skbk7yt3mk32` (`url_action_id`),
  CONSTRAINT `FKa6jx8n8xkesmjmv6jqug6bg68` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `FKgg1vfrini4olsrbjhubgrggam` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`),
  CONSTRAINT `FKij92vnr0qkd97skbk7yt3mk32` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `schema_version`
--


CREATE TABLE IF NOT EXISTS  `schema_version` (
  `installed_rank` int(11) NOT NULL COMMENT '',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号',
  `description` varchar(200) NOT NULL COMMENT '描述',
  `type` varchar(20) NOT NULL COMMENT '类型',
  `script` varchar(1000) NOT NULL COMMENT '打包脚本',
  `checksum` int(11) DEFAULT NULL COMMENT '校验和',
  `installed_by` varchar(100) NOT NULL COMMENT '安装者',
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
  `execution_time` int(11) NOT NULL COMMENT '执行时间',
  `success` tinyint(1) NOT NULL COMMENT '是否成功',
  PRIMARY KEY (`installed_rank`),
  KEY `schema_version_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `spring_session`
--

CREATE TABLE IF NOT EXISTS  `spring_session` (
  `SESSION_ID` char(36) NOT NULL COMMENT 'SESSION ID',
  `CREATION_TIME` bigint(20) NOT NULL COMMENT '创建时间',
  `LAST_ACCESS_TIME` bigint(20) NOT NULL COMMENT '',
  `MAX_INACTIVE_INTERVAL` int(11) NOT NULL COMMENT '',
  `PRINCIPAL_NAME` varchar(100) DEFAULT NULL COMMENT '',
  PRIMARY KEY (`SESSION_ID`),
  KEY `SPRING_SESSION_IX1` (`LAST_ACCESS_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';


--
-- Table structure for table `t_user_token`
--


CREATE TABLE IF NOT EXISTS  `t_user_token` (
  `user_Id` varchar(255) NOT NULL COMMENT '用户ID',
  `access_Token` varchar(255) DEFAULT NULL COMMENT '权限Token',
  `expire_Time_Mills` bigint(20) NOT NULL COMMENT '过期时间',
  `last_Access_Time_Mills` bigint(20) NOT NULL COMMENT '最近鉴权时间',
  `refresh_Token` varchar(255) DEFAULT NULL COMMENT '刷新token',
  `user_Type` varchar(255) DEFAULT NULL COMMENT '用户类型',
  PRIMARY KEY (`user_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COMMENT='';

--
-- Table structure for table `url_action`
--

CREATE TABLE IF NOT EXISTS  `url_action` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `action` varchar(255) NOT NULL COMMENT '操作',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `url` varchar(255) NOT NULL COMMENT 'url地址',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `user`
--


CREATE TABLE IF NOT EXISTS  `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `chname` varchar(255) DEFAULT NULL COMMENT '',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `email` varchar(255) DEFAULT NULL COMMENT 'email',
  `lang` varchar(255) DEFAULT NULL COMMENT '语言',
  `last_login_time` datetime DEFAULT NULL COMMENT '最近登录时间',
  `phone` varchar(255) DEFAULT NULL COMMENT '电话',
  `username` varchar(255) NOT NULL COMMENT '用户名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `user_permission`
--

CREATE TABLE IF NOT EXISTS  `user_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `url_action_id` int(11) DEFAULT NULL COMMENT '',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `FK9ng630d8o1q73hhvyr73fjg8j` (`url_action_id`),
  KEY `FK7c2x74rinbtf33lhdcyob20sh` (`user_id`),
  CONSTRAINT `FK3f7ym8s6w14282n8lwuukwglt` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`),
  CONSTRAINT `FK7c2x74rinbtf33lhdcyob20sh` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9ng630d8o1q73hhvyr73fjg8j` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

--
-- Table structure for table `user_role`
--

CREATE TABLE IF NOT EXISTS  `user_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` int(11) DEFAULT NULL COMMENT '角色ID',
  `user_id` int(11) DEFAULT NULL COMMENT '用户ID',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `modify_time` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `FKa68196081fvovjhkek5m97n3y` (`role_id`),
  KEY `FK859n2jvi8ivhui0rl0esws6o` (`user_id`),
  CONSTRAINT `FK859n2jvi8ivhui0rl0esws6o` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKa68196081fvovjhkek5m97n3y` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

SET FOREIGN_KEY_CHECKS=1;