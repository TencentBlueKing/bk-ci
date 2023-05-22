USE devops_ci_store;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_PIPELINE_REL'
                    AND COLUMN_NAME = 'BUS_TYPE') THEN
    ALTER TABLE T_STORE_PIPELINE_REL ADD COLUMN `BUS_TYPE` varchar(32) NOT NULL DEFAULT 'BUILD' COMMENT '业务类型 BUILD:构建 INDEX:研发商店指标';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO') THEN
    ALTER TABLE T_STORE_ERROR_CODE_INFO COMMENT='store组件错误码信息';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                    AND COLUMN_NAME = 'STORE_CODE') THEN
    ALTER TABLE T_STORE_ERROR_CODE_INFO MODIFY COLUMN STORE_CODE varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '组件代码,为空则表示属于通用错误码';
    END IF;

    IF EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                    AND COLUMN_NAME = 'STORE_TYPE') THEN
    ALTER TABLE T_STORE_ERROR_CODE_INFO MODIFY COLUMN STORE_TYPE tinyint(4) NULL COMMENT '组件类型';
    END IF;

    IF NOT EXISTS(SELECT 1
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_STORE_DOCKING_PLATFORM'
                    AND COLUMN_NAME = 'ERROR_CODE_PREFIX') THEN
    ALTER TABLE T_STORE_DOCKING_PLATFORM ADD COLUMN `ERROR_CODE_PREFIX` int(3) NOT NULL COMMENT '平台所属错误码前缀';
    END IF;

    IF NOT EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_STORE_DOCKING_PLATFORM'
                            AND INDEX_NAME = 'UNI_INX_TSDP_ERROR') THEN
    ALTER TABLE T_STORE_DOCKING_PLATFORM ADD UNIQUE INDEX `UNI_INX_TSDP_ERROR` (ERROR_CODE_PREFIX);
    END IF;

    IF EXISTS(SELECT 1
                              FROM information_schema.statistics
                              WHERE TABLE_SCHEMA = db
                                AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                                AND INDEX_NAME = 'T_STORE_ERROR_CODE_INFO_UN') THEN
    ALTER TABLE T_STORE_ERROR_CODE_INFO DROP KEY `T_STORE_ERROR_CODE_INFO_UN`;
    END IF;

     IF NOT EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                            AND INDEX_NAME = 'UNI_TSECI_STORE_TYPE_ERROR') THEN
    ALTER TABLE T_STORE_ERROR_CODE_INFO ADD UNIQUE INDEX `UNI_TSECI_STORE_TYPE_ERROR` (`STORE_CODE`,`STORE_TYPE`,`ERROR_CODE`);
    END IF;

    IF NOT EXISTS(SELECT 1
                          FROM information_schema.COLUMNS
                          WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_STORE_STATISTICS_TOTAL'
                            AND COLUMN_NAME = 'HOT_FLAG') THEN
    ALTER TABLE T_STORE_STATISTICS_TOTAL ADD HOT_FLAG bit(1) DEFAULT b'0' NULL COMMENT '是否为受欢迎组件';
    END IF;
	
	IF EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                AND COLUMN_NAME = 'ERROR_MSG_ZH_CN') THEN
        ALTER TABLE T_STORE_ERROR_CODE_INFO DROP COLUMN `ERROR_MSG_ZH_CN`;
    END IF;
	
	IF EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                AND COLUMN_NAME = 'ERROR_MSG_ZH_TW') THEN
        ALTER TABLE T_STORE_ERROR_CODE_INFO DROP COLUMN `ERROR_MSG_ZH_TW`;
    END IF;
	
	IF EXISTS(SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_STORE_ERROR_CODE_INFO'
                AND COLUMN_NAME = 'ERROR_MSG_EN') THEN
        ALTER TABLE T_STORE_ERROR_CODE_INFO DROP COLUMN `ERROR_MSG_EN`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();
