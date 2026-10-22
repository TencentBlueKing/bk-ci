USE devops_ci_environment;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'OPERATOR_STATUS') THEN
    ALTER TABLE T_NODE
            ADD COLUMN `OPERATOR_STATUS` tinyint(4) DEFAULT NULL COMMENT '操作人状态';
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'PROJECT_ID_OPERATOR_STATUS') THEN
    ALTER TABLE `T_NODE`
            ADD INDEX `PROJECT_ID_OPERATOR_STATUS` (`PROJECT_ID`, `OPERATOR_STATUS`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'PROJECT_ID_NODE_STATUS') THEN
    ALTER TABLE `T_NODE`
            ADD INDEX `PROJECT_ID_NODE_STATUS` (`PROJECT_ID`, `NODE_STATUS`);
    END IF;

    IF NOT EXISTS(SELECT 1
                   FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_ENV'
                     AND COLUMN_NAME = 'OS') THEN
    ALTER TABLE `T_ENV` ADD COLUMN `OS` varchar(16) NULL COMMENT '环境节点系统类型';
    END IF;

    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db AND TABLE_NAME = 'T_ENV_DISPATCH_STRATEGY'
                    AND COLUMN_NAME = 'RESOURCE_REMAINING') THEN
        ALTER TABLE `T_ENV_DISPATCH_STRATEGY`
            ADD COLUMN `RESOURCE_REMAINING` text DEFAULT NULL COMMENT '资源剩余阈值JSON';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();

CREATE TABLE IF NOT EXISTS `T_AGENT_RESOURCE_SNAPSHOT` (
  `AGENT_ID` bigint(20) NOT NULL COMMENT 'Agent ID',
  `PROJECT_ID` varchar(64) NOT NULL COMMENT 'Agent所属项目',
  `CPU_IDLE` double DEFAULT NULL COMMENT '整机CPU剩余百分比',
  `CPU_TIME` bigint(20) DEFAULT NULL COMMENT 'CPU采样毫秒时间戳',
  `MEMORY_AVAILABLE` double DEFAULT NULL COMMENT '可用内存百分比，含可回收缓存',
  `MEMORY_TIME` bigint(20) DEFAULT NULL COMMENT '内存采样毫秒时间戳',
  `DISK_FREE` double DEFAULT NULL COMMENT 'Agent工作目录所在盘剩余百分比',
  `DISK_TIME` bigint(20) DEFAULT NULL COMMENT '磁盘采样毫秒时间戳',
  `VALID_AFTER` bigint(20) NOT NULL DEFAULT 0 COMMENT '生命周期失效水位，拒绝旧采样',
  `UPDATED_TIME` bigint(20) NOT NULL DEFAULT 0 COMMENT '最后更新毫秒时间戳',
  PRIMARY KEY (`AGENT_ID`),
  KEY `IDX_UPDATED_TIME` (`UPDATED_TIME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent最新调度监控，不保存历史';

CREATE TABLE IF NOT EXISTS `T_ENV_DOCKER_CONFIG` (
  `PROJECT_ID` varchar(64) NOT NULL,
  `RESOURCE_ID` bigint(20) NOT NULL COMMENT '环境ID',
  `CPUS` varchar(64) DEFAULT NULL,
  `MEMORY` varchar(64) DEFAULT NULL,
  `UPDATED_USER` varchar(64) NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`PROJECT_ID`, `RESOURCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按需创建的环境容器默认上限';

CREATE TABLE IF NOT EXISTS `T_NODE_DOCKER_CONFIG` (
  `PROJECT_ID` varchar(64) NOT NULL,
  `RESOURCE_ID` bigint(20) NOT NULL COMMENT '节点ID',
  `CPUS` varchar(64) DEFAULT NULL,
  `MEMORY` varchar(64) DEFAULT NULL,
  `CONTAINER_RUNTIME` varchar(16) DEFAULT NULL COMMENT 'docker或podman，NULL为docker',
  `UPDATED_USER` varchar(64) NOT NULL,
  `UPDATED_TIME` datetime NOT NULL,
  PRIMARY KEY (`PROJECT_ID`, `RESOURCE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按需创建的节点容器配置';
