USE devops_ci_dispatch;
SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for T_DISPATCH_MACHINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_DISPATCH_PIPELINE_DOCKER_DEV_CLUSTER`
(
    `CLUSTER_ID`     char(32)     NOT NULL PRIMARY KEY,
    `CLUSTER_NAME`   varchar(128) NOT NULL,
    `ENABLE`      bit(1)       DEFAULT b'1',
    `CREATE_USER` varchar(255) DEFAULT NULL,
    `CREATE_TIME` datetime     DEFAULT NOW(),
    `UPDATE_USER` varchar(255) DEFAULT NULL,
    `UPDATE_TIME` datetime     DEFAULT NOW()
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
