USE devops_ci_auth;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_AUTH_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP` (
  `ID` varchar(64) NOT NULL COMMENT '主健ID',
  `GROUP_NAME` varchar(32) NOT NULL DEFAULT '""' COMMENT '用户组名称',
  `GROUP_CODE` varchar(32) NOT NULL COMMENT '用户组标识 默认用户组标识一致',
  `GROUP_TYPE` tinyint(2) NOT NULL DEFAULT '0' COMMENT '用户组类型:0.默认 1.用户自定义',
  `PROJECT_CODE` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组所属项目',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '添加人',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改人',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `GROUP_NAME+PROJECT_CODE` (`GROUP_NAME`,`PROJECT_CODE`),
  KEY `PROJECT_CODE` (`PROJECT_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_AUTH_GROUP_PERSSION
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP_PERSSION` (
  `ID` varchar(64) NOT NULL COMMENT '主健ID',
  `AUTH_ACTION` varchar(64) NOT NULL DEFAULT '""' COMMENT '权限动作',
  `GROUP_CODE` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组编号 默认7个内置组编号固定 自定义组编码随机',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '创建人',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改人',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '创建时间',
  `UPDATE_TIME` datetime(3) DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_AUTH_GROUP_USER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_GROUP_USER` (
  `ID` varchar(64) NOT NULL COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户ID',
  `GROUP_ID` varchar(64) NOT NULL DEFAULT '""' COMMENT '用户组ID',
  `CREATE_USER` varchar(64) NOT NULL DEFAULT '""' COMMENT '添加用户',
  `CREATE_TIME` datetime(3) NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Table structure for T_AUTH_STRATEGY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_STRATEGY` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '策略主键ID',
  `STRATEGY_NAME` varchar(32) NOT NULL COMMENT '策略名称',
  `STRATEGY_BODY` varchar(2000) NOT NULL COMMENT '策略内容',
  `IS_DELETE` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除 0未删除 1删除',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `CREATE_USER` varchar(32) NOT NULL COMMENT '创建人',
  `UPDATE_USER` varchar(32) DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_AUTH_MANAGER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_MANAGER` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `NAME` varchar(32) NOT NULL COMMENT '名称',
  `ORGANIZATION_ID` int(11) NOT NULL COMMENT '组织ID',
  `LEVEL` int(11) NOT NULL COMMENT '层级ID',
  `STRATEGYID` int(11) NOT NULL COMMENT '权限策略ID',
  `IS_DELETE` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否删除',
  `CREATE_USER` varchar(11) NOT NULL DEFAULT '""' COMMENT '创建用户',
  `UPDATE_USER` varchar(11) DEFAULT '""' COMMENT '修改用户',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_AUTH_MANAGER_USER
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_MANAGER_USER` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `MANAGER_ID` int(11) NOT NULL COMMENT '管理员权限ID',
  `START_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '权限生效起始时间',
  `END_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '权限生效结束时间',
  `CREATE_USER` varchar(64) NOT NULL COMMENT '创建用户',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改用户',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `USER_ID+MANGER_ID` (`USER_ID`,`MANAGER_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_AUTH_MANAGER_USER_HISTORY
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_MANAGER_USER_HISTORY` (
  `ID` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
  `MANAGER_ID` int(11) NOT NULL COMMENT '管理员权限ID',
  `START_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '权限生效起始时间',
  `END_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '权限生效结束时间',
  `CREATE_USER` varchar(64) NOT NULL COMMENT '创建用户',
  `UPDATE_USER` varchar(64) DEFAULT NULL COMMENT '修改用户',
  `CREATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  KEY `MANGER_ID` (`MANAGER_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_AUTH_MANAGER_WHITELIST
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_MANAGER_WHITELIST` (
   `ID` int(11) NOT NULL AUTO_INCREMENT,
   `MANAGER_ID` int(11) NOT NULL COMMENT '管理策略ID',
   `USER_ID` varchar(64) NOT NULL COMMENT '用户ID',
   PRIMARY KEY (`ID`),
   KEY `idx_manager` (`MANAGER_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for T_AUTH_MANAGER_APPROVAL
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_MANAGER_APPROVAL`
(
    `ID`           int(11)                             NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `USER_ID`      varchar(64)                         NOT NULL COMMENT '用户ID',
    `MANAGER_ID`   int                                 NOT NULL COMMENT '管理员权限ID',
    `EXPIRED_TIME` timestamp                           NOT NULL COMMENT '权限过期时间',
    `START_TIME`   timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '审批单生效时间',
    `END_TIME`     timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '审批单失效时间',
    `STATUS`       int(2)                              NOT NULL COMMENT '发送状态 0-审核流程中 ,1-用户拒绝续期,2-用户同意续期,3-审批人拒绝续期，4-审批人同意续期',
    `CREATE_TIME`  timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `UPDATE_TIME`  timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '修改时间',
    INDEX `IDX_USER_ID` (`USER_ID`),
    INDEX `IDX_MANAGER_ID` (`MANAGER_ID`),
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB
DEFAULT CHARSET = utf8mb4 COMMENT '蓝盾超级管理员权限续期审核表';

-- ----------------------------
-- Table structure for T_AUTH_ACTION
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_ACTION` (
    `actionId` varchar(64) NOT NULL COMMENT '操作ID',
    `resourceType` varchar(64) NOT NULL COMMENT '资源类型',
    `actionName` varchar(64) NOT NULL COMMENT '操作名称',
    `englishName` varchar(64) DEFAULT NULL  COMMENT '动作英文名称',
    `creator` varchar(32) DEFAULT NULL  COMMENT '创建者',
    `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '修改时间',
    `delete` bit(1) DEFAULT NULL  COMMENT '是否删除',
    `actionType` varchar(32) DEFAULT NULL  COMMENT '操作类型',
    PRIMARY KEY (`actionId`),
    UNIQUE INDEX `UNI_INX_RESOURCE_TYPE_ACTION_ID`(`resourceType`, `actionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限操作表';

-- ----------------------------
-- Table structure for T_AUTH_RESOURCE_TYPE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_AUTH_RESOURCE_TYPE` (
    `ID` int(11) NOT NULL COMMENT 'ID',
    `RESOURCE_TYPE` varchar(64) NOT NULL  COMMENT '资源类型',
    `NAME` varchar(64) NOT NULL  COMMENT '资源名称',
    `ENGLISH_NAME` varchar(64) DEFAULT NULL  COMMENT '资源英文名称',
    `DESC` varchar(255) NOT NULL  COMMENT '资源描述',
    `ENGLISH_DESC` varchar(255) DEFAULT NULL  COMMENT '资源英文描述',
    `PARENT` varchar(255) DEFAULT NULL  COMMENT '父类资源',
    `SYSTEM` varchar(255) NOT NULL  COMMENT '所属系统',
    `CREATE_USER` varchar(32) NOT NULL  COMMENT '创建者',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `UPDATE_USER` varchar(32) DEFAULT NULL  COMMENT '更新者',
    `UPDATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '修改时间',
    `DELETE` bit(1) DEFAULT NULL  COMMENT '是否删除',
    UNIQUE KEY `ID` (`ID`),
    PRIMARY KEY (`RESOURCE_TYPE`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='权限资源类型表';

SET FOREIGN_KEY_CHECKS = 1;
