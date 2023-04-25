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
    KEY `uni_1` (`WORKSPACE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='KUBERNETES构建集群工作空间操作记录表';


SET FOREIGN_KEY_CHECKS = 1;
