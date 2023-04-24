USE devops_ci_project;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_project_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_project_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_DATA_SOURCE'
                        AND COLUMN_NAME = 'DS_URL') THEN
        ALTER TABLE T_DATA_SOURCE
            ADD DS_URL varchar(1024) NULL COMMENT '数据源URL地址' ;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NOTICE'
                        AND COLUMN_NAME = 'SERVICE_NAME') THEN
        ALTER TABLE T_NOTICE
            ADD SERVICE_NAME varchar(1024) NULL COMMENT '服务名称' ;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
                        AND COLUMN_NAME = 'CLUSTER_NAME') THEN
        ALTER TABLE T_SHARDING_ROUTING_RULE
            ADD `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT 'prod' COMMENT '集群名称' ;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
                        AND COLUMN_NAME = 'MODULE_CODE') THEN
        ALTER TABLE T_SHARDING_ROUTING_RULE
            ADD `MODULE_CODE` varchar(64) NOT NULL DEFAULT 'PROCESS' COMMENT '模块标识' ;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
                        AND COLUMN_NAME = 'TYPE') THEN
        ALTER TABLE T_SHARDING_ROUTING_RULE
            ADD `TYPE` varchar(32) NOT NULL DEFAULT 'DB' COMMENT '路由类型，DB:数据库，TABLE:数据库表' ;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
                        AND COLUMN_NAME = 'DATA_SOURCE_NAME') THEN
        ALTER TABLE T_SHARDING_ROUTING_RULE
            ADD `DATA_SOURCE_NAME` varchar(128) NOT NULL DEFAULT 'ds_0' COMMENT '数据源名称';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                    FROM information_schema.COLUMNS
                    WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
                        AND COLUMN_NAME = 'TABLE_NAME') THEN
        ALTER TABLE T_SHARDING_ROUTING_RULE
            ADD `TABLE_NAME` varchar(128) DEFAULT NULL COMMENT '数据库表名称';
    END IF;
	
	IF NOT EXISTS(SELECT 1
			  FROM information_schema.statistics
			  WHERE TABLE_SCHEMA = db
				 AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
				 AND INDEX_NAME = 'UNI_INX_TSRR_CLUSTER_MODULE_TYPE_NAME') THEN
		ALTER TABLE T_SHARDING_ROUTING_RULE ADD UNIQUE INDEX `UNI_INX_TSRR_CLUSTER_MODULE_TYPE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TYPE`,`ROUTING_NAME`,`TABLE_NAME`);
	END IF;
	
	IF EXISTS(SELECT 1
			  FROM information_schema.statistics
			  WHERE TABLE_SCHEMA = db
				 AND TABLE_NAME = 'T_SHARDING_ROUTING_RULE'
				 AND INDEX_NAME = 'uni_inx_tsrr_routting_name') THEN
		ALTER TABLE T_SHARDING_ROUTING_RULE DROP INDEX `uni_inx_tsrr_routting_name`;
	END IF;

	IF NOT EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PROJECT'
                AND COLUMN_NAME = 'properties') THEN
        ALTER TABLE T_PROJECT
            ADD `properties` text NULL COMMENT '项目其他配置';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_project_schema_update();
