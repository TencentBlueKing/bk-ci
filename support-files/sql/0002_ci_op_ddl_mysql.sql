USE devops_ci_op;

SET FOREIGN_KEY_CHECKS=0;

--
-- Table structure for table `SPRING_SESSION_ATTRIBUTES`
--

CREATE TABLE IF NOT EXISTS  `SPRING_SESSION_ATTRIBUTES` (
  `SESSION_ID` char(36) NOT NULL,
  `ATTRIBUTE_NAME` varchar(200) NOT NULL,
  `ATTRIBUTE_BYTES` blob,
  PRIMARY KEY (`SESSION_ID`,`ATTRIBUTE_NAME`),
  KEY `SPRING_SESSION_ATTRIBUTES_IX1` (`SESSION_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `dept_info`
--


CREATE TABLE IF NOT EXISTS  `dept_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `create_time` datetime DEFAULT NULL,
  `dept_id` int(11) NOT NULL,
  `dept_name` varchar(100) NOT NULL,
  `level` int(11) NOT NULL,
  `parent_dept_id` int(11) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bd8ig9ecbopp3592f9fcpb99p` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `project_info`
--

CREATE TABLE IF NOT EXISTS  `project_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `approval_status` int(11) DEFAULT NULL,
  `approval_time` datetime DEFAULT NULL,
  `approver` varchar(100) DEFAULT NULL,
  `cc_app_id` int(11) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `creator` varchar(100) DEFAULT NULL,
  `creator_bg_name` varchar(100) DEFAULT NULL,
  `creator_center_name` varchar(100) DEFAULT NULL,
  `creator_dept_name` varchar(100) DEFAULT NULL,
  `english_name` varchar(255) DEFAULT NULL,
  `is_offlined` bit(1) DEFAULT NULL,
  `is_secrecy` bit(1) DEFAULT NULL,
  `project_bg_id` int(11) DEFAULT NULL,
  `project_bg_name` varchar(100) DEFAULT NULL,
  `project_center_id` varchar(50) DEFAULT NULL,
  `project_center_name` varchar(100) DEFAULT NULL,
  `project_dept_id` int(11) DEFAULT NULL,
  `project_dept_name` varchar(100) DEFAULT NULL,
  `project_id` varchar(100) DEFAULT NULL,
  `project_name` varchar(100) DEFAULT NULL,
  `project_type` int(11) DEFAULT NULL,
  `use_bk` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_bvtnw8dekf2y9gbxt7thib8vj` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `role`
--

CREATE TABLE IF NOT EXISTS  `role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `ch_name` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_8sewwnpamngi6b1dwaa88askk` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `role_permission`
--

CREATE TABLE IF NOT EXISTS  `role_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `expire_time` datetime DEFAULT NULL,
  `role_id` int(11) DEFAULT NULL,
  `url_action_id` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa6jx8n8xkesmjmv6jqug6bg68` (`role_id`),
  KEY `FKij92vnr0qkd97skbk7yt3mk32` (`url_action_id`),
  CONSTRAINT `FKa6jx8n8xkesmjmv6jqug6bg68` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `FKgg1vfrini4olsrbjhubgrggam` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`),
  CONSTRAINT `FKij92vnr0qkd97skbk7yt3mk32` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `schema_version`
--


CREATE TABLE IF NOT EXISTS  `schema_version` (
  `installed_rank` int(11) NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int(11) DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int(11) NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `schema_version_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `spring_session`
--

CREATE TABLE IF NOT EXISTS  `spring_session` (
  `SESSION_ID` char(36) NOT NULL,
  `CREATION_TIME` bigint(20) NOT NULL,
  `LAST_ACCESS_TIME` bigint(20) NOT NULL,
  `MAX_INACTIVE_INTERVAL` int(11) NOT NULL,
  `PRINCIPAL_NAME` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`SESSION_ID`),
  KEY `SPRING_SESSION_IX1` (`LAST_ACCESS_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


--
-- Table structure for table `t_user_token`
--


CREATE TABLE IF NOT EXISTS  `t_user_token` (
  `user_Id` varchar(255) NOT NULL,
  `access_Token` varchar(255) DEFAULT NULL,
  `expire_Time_Mills` bigint(20) NOT NULL,
  `last_Access_Time_Mills` bigint(20) NOT NULL,
  `refresh_Token` varchar(255) DEFAULT NULL,
  `user_Type` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`user_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Table structure for table `url_action`
--

CREATE TABLE IF NOT EXISTS  `url_action` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `action` varchar(255) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `url` varchar(255) NOT NULL,
  `create_time` datetime DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user`
--


CREATE TABLE IF NOT EXISTS  `user` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `chname` varchar(255) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `lang` varchar(255) DEFAULT NULL,
  `last_login_time` datetime DEFAULT NULL,
  `phone` varchar(255) DEFAULT NULL,
  `username` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_permission`
--

CREATE TABLE IF NOT EXISTS  `user_permission` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `expire_time` datetime DEFAULT NULL,
  `url_action_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9ng630d8o1q73hhvyr73fjg8j` (`url_action_id`),
  KEY `FK7c2x74rinbtf33lhdcyob20sh` (`user_id`),
  CONSTRAINT `FK3f7ym8s6w14282n8lwuukwglt` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`),
  CONSTRAINT `FK7c2x74rinbtf33lhdcyob20sh` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FK9ng630d8o1q73hhvyr73fjg8j` FOREIGN KEY (`url_action_id`) REFERENCES `url_action` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_role`
--

CREATE TABLE IF NOT EXISTS  `user_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `role_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `modify_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa68196081fvovjhkek5m97n3y` (`role_id`),
  KEY `FK859n2jvi8ivhui0rl0esws6o` (`user_id`),
  CONSTRAINT `FK859n2jvi8ivhui0rl0esws6o` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKa68196081fvovjhkek5m97n3y` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS=1;