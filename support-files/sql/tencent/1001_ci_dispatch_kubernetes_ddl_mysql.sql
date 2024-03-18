USE devops_ci_dispatch_kubernetes;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_DISPATCH_WORKSPACE
-- ----------------------------
CREATE TABLE IF NOT EXISTS T_DISPATCH_WORKSPACE
(
    ID              bigint(11) auto_increment comment '自增ID'
        primary key,
    USER_ID         varchar(64)   default ''                not null comment '用户',
    PROJECT_ID      varchar(64)   default ''                not null comment '项目ID',
    WORKSPACE_NAME  varchar(128)  default ''                not null comment '工作空间名称，唯一性',
    ENVIRONMENT_UID varchar(128)  default ''                not null comment 'DevCloud环境ID',
    TASK_ID         varchar(128)  default ''                not null comment '任务ID',
	REGION_ID 		int(11) 	  default 0 				not null comment '云区域ID' ,
    GIT_URL         varchar(1024) default ''                not null comment '工蜂项目地址',
    BRANCH          varchar(1024) default ''                not null comment '工蜂项目分支',
    IMAGE           varchar(256)  default ''                not null comment '工作空间镜像',
    STATUS          int           default 0                 not null comment '工作空间状态',
    CREATE_TIME     timestamp     default CURRENT_TIMESTAMP not null comment '创建时间',
    UPDATE_TIME     timestamp     default CURRENT_TIMESTAMP not null comment '修改时间',
    constraint NAME
        unique (WORKSPACE_NAME)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT 'KUBERNETES构建集群工作空间表';

-- ----------------------------
-- Table structure for T_DISPATCH_WORKSPACE_OP_HIS
-- ----------------------------
CREATE TABLE IF NOT EXISTS T_DISPATCH_WORKSPACE_OP_HIS
(
    ID              bigint auto_increment
        primary key,
    WORKSPACE_NAME  varchar(128) default ''                not null comment '工作空间名称',
    ENVIRONMENT_UID varchar(128) default ''                not null comment 'DevCloud环境ID',
    OPERATOR        varchar(64)  default ''                not null comment '操作人',
    ACTION          varchar(64)  default ''                not null comment '操作行为: CREATE, START, STOP, DELETE, SHARE',
    ACTION_MSG      varchar(256) default ''                not null comment '操作行为描述',
    CREATED_TIME    timestamp    default CURRENT_TIMESTAMP not null comment '创建时间',
    UID             varchar(128) default ''                not null comment 'task id',
    STATUS          varchar(32)  default ''                not null comment '操作状态',
    KEY `uni_1` (`WORKSPACE_NAME`),
    KEY `uni_2` (`UID`),
    KEY `uni_3` (`STATUS`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KUBERNETES构建集群工作空间操作记录表';

-- ----------------------------
-- Table structure for T_WINDOWS_GPU_POOL
-- ----------------------------
CREATE TABLE IF NOT EXISTS T_WINDOWS_GPU_POOL
(
    CGS_ID  varchar(32)  default ''                not null comment 'CGS ID',
    ZONE_ID varchar(32)  default ''                not null comment '区域ID，SZ3，NJ1等',
    CGS_IP  varchar(32)  default ''                not null comment 'ip',
    MACHINE_TYPE  varchar(32)  default ''          not null comment '机型',
    STATUS  int          default 0                 not null comment '0使用中 1待销毁 2销毁中 10注册中 11未使用',
    LOCKED  boolean      default false             not null comment '该资源是否锁定',
    USER_INSTANCE_List text NOT NULL COMMENT '拥有者或共享人详情',
    PROJECT_ID varchar(64)  DEFAULT '' NOT NULL COMMENT '项目ID',
    CPU varchar(16) NOT NULL DEFAULT '' COMMENT 'CPU',
    MEMORY varchar(16) NOT NULL DEFAULT '' COMMENT '内存',
    DISK varchar(64)  NULL COMMENT '磁盘',
    HDISK varchar(64)  NULL COMMENT '云磁盘',
    IMAGESTANDARD  boolean      default true             not null comment '是否基础镜像',
    NODE varchar(64)  DEFAULT '' NOT NULL COMMENT '母机IP',
    IMAGE varchar(256)  DEFAULT '' NOT NULL COMMENT '镜像地址',
    REGISTER_TIME  timestamp NULL DEFAULT NULL COMMENT '注册cgs时间',
    UNIQUE KEY `uni_1` (`ZONE_ID`,`CGS_IP`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='START云桌面的资源列表';

-- ----------------------------
-- Table structure for T_WINDOWS_VM_POOL
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_WINDOWS_VM_RESOURCE` (
    `ZONE_ID` varchar(32)  default '' not null comment '区域ID，SZ3，NJ1等',
    `MACHINE_TYPE`  varchar(32)  default '' not null comment '机型',
    `CAP` INT NOT NULL default 0 COMMENT '容量',
    `USED` INT NOT NULL default 0 COMMENT '已使用量',
    `FREE` INT NOT NULL default 0 COMMENT '空闲量',
    PRIMARY KEY (`ZONE_ID`, `MACHINE_TYPE`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='windows虚拟机gpu卡资源使用情况';

SET FOREIGN_KEY_CHECKS = 1;
