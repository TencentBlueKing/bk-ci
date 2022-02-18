USE devops_ci_log;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_log_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_log_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_STATUS') THEN
        CREATE TABLE `T_LOG_STATUS`(
          `ID`            bigint(20)  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
          `BUILD_ID`      varchar(64) NOT NULL COMMENT '构建ID',
          `TAG`           varchar(64)          DEFAULT NULL COMMENT '日志tag',
          `SUB_TAG` varchar(256) DEFAULT NULL COMMENT '日志子tag',
          `JOB_ID`        varchar(64)          DEFAULT NULL COMMENT '日志jobId',
          `EXECUTE_COUNT` int(11)     NOT NULL COMMENT '执行次数',
          `FINISHED`      bit(1)      NOT NULL DEFAULT b'0' COMMENT 'build is finished or not',
          `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
          PRIMARY KEY (`ID`),
          UNIQUE KEY `BUILD_ID_2` (`BUILD_ID`, `TAG`, `SUB_TAG`, `EXECUTE_COUNT`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT='';
    END IF;

  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_LOG_SUBTAGS') THEN
      CREATE TABLE `T_LOG_SUBTAGS` (
        `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
        `TAG` varchar(64) NOT NULL DEFAULT '' COMMENT '插件标签',
        `SUB_TAGS` text NOT NULL COMMENT '插件子标签',
        `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
        PRIMARY KEY (`ID`),
        UNIQUE KEY `BUILD_ID_2` (`BUILD_ID`,`TAG`)
      ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_log_schema_update();
