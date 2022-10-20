CREATE DATABASE IF NOT EXISTS `devops_ci_kubernetes_manager` DEFAULT CHARACTER SET utf8mb4;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE devops_ci_kubernetes_manager;

-- ----------------------------
-- Table structure for T_KUBERNETES_MANAGER_TASK
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_KUBERNETES_MANAGER_TASK`
(
    `ID`          bigint(20)  NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `TASK_ID`     varchar(63) NOT NULL COMMENT '任务ID',
    `TASK_KEY`    varchar(63) NOT NULL COMMENT '当前任务绑定的唯一key，例如工作负载名称',
    `TASK_BELONG` varchar(16) NOT NULL COMMENT '当前任务所属的资源，例如Builder或者Job',
    `ACTION`      varchar(16) NOT NULL COMMENT '当前任务执行的操作',
    `STATUS`      varchar(16) NOT NULL COMMENT '当前任务状态',
    `MESSAGE`     text        NULL COMMENT '结束任务的信息',
    `ACTION_TIME` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作开始时间',
    `UPDATE_TIME` timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务详情修改时间',
    PRIMARY KEY (`ID`),
    KEY `IDX_TASK_ID` (`TASK_ID`) USING BTREE COMMENT 'TASK_ID 索引',
    KEY `IDX_WORKLOAD_NAME` (`TASK_KEY`) USING BTREE COMMENT 'WORKLOAD_NAME索引',
    KEY `IDX_UPDATE_TIME` (`UPDATE_TIME`) USING BTREE COMMENT 'UPDATE_TIME索引，为了删除性能'
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Kubernetes manager 任务表';

CREATE TABLE IF NOT EXISTS `T_KUBERNETES_MANAGER_BUILDER_SCHEDULED_INFO`
(
    `BUILDER_NAME`     varchar(64) NOT NULL COMMENT '构建机名称',
    `NODE_HISTORY`     json        NULL COMMENT '调度过的json节点',
    `RESOURCE_HISTORY` json        NULL COMMENT '调度过的cpu，mem等资源使用量',
    `CREATE_TIME`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`      timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`BUILDER_NAME`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Kubernetes manager 调度历史信息表';

SET FOREIGN_KEY_CHECKS = 1;