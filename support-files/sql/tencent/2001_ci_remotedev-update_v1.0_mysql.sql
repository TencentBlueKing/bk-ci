USE devops_ci_remotedev;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_remotedev_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_remotedev_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_WINDOWS_RESOURCE_ZONE'
                        AND COLUMN_NAME = 'PROVIDER') THEN
        ALTER TABLE T_WINDOWS_RESOURCE_ZONE
            ADD COLUMN `PROVIDER` varchar(32) NOT NULL DEFAULT 'IEG_BKCI'
            COMMENT '资源提供商，如 IEG_BKCI/CSIG/TEG_DEVCLOUD';

        -- 回填基础 provider（DEFAULT/INTERNAL_USE 已默认 IEG_BKCI，测试环境需由运维追加 TEST_ 前缀）
        UPDATE T_WINDOWS_RESOURCE_ZONE
            SET PROVIDER = 'CSIG'
            WHERE TYPE = 'CSIG_USE';
        UPDATE T_WINDOWS_RESOURCE_ZONE
            SET PROVIDER = 'TEG_DEVCLOUD'
            WHERE TYPE = 'DEVCLOUD';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_remotedev_schema_update();
