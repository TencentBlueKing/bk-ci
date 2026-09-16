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
                  FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_LABEL_REL'
                    AND INDEX_NAME = 'UNI_LABEL_ID_PROJECT_ID') THEN
        DELETE t1
        FROM T_PROJECT_LABEL_REL t1
                 INNER JOIN T_PROJECT_LABEL_REL t2
                            ON t1.LABEL_ID = t2.LABEL_ID
                                AND t1.PROJECT_ID = t2.PROJECT_ID
                                AND t1.ID > t2.ID;
        ALTER TABLE `T_PROJECT_LABEL_REL`
            ADD UNIQUE KEY `UNI_LABEL_ID_PROJECT_ID` (`LABEL_ID`, `PROJECT_ID`);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;

INSERT IGNORE INTO `T_PROJECT_LABEL` (`ID`, `LABEL_NAME`)
VALUES ('6f1e2d3c4b5a49788e9d0c1b2a3f4e5d', 'COMMON_IMATE');
