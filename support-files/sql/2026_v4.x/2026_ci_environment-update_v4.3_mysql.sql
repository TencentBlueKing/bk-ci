USE devops_ci_environment;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_environment_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_environment_schema_update()
BEGIN
    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_AGENT_BATCH_INSTALL_TOKEN'
                AND COLUMN_NAME = 'TOKEN'
                AND CHARACTER_MAXIMUM_LENGTH < 128) THEN
        ALTER TABLE `T_AGENT_BATCH_INSTALL_TOKEN`
            MODIFY COLUMN `TOKEN` varchar(128) NOT NULL COMMENT 'Base64编码后TOKEN';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
