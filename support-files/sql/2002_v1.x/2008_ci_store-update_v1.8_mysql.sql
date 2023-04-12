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
			  FROM information_schema.statistics
			  WHERE TABLE_SCHEMA = db
				 AND TABLE_NAME = 'T_ATOM'
				 AND INDEX_NAME = 'inx_tpca_code_version_time') THEN
		ALTER TABLE T_ATOM ADD INDEX `inx_tpca_code_version_time` (`ATOM_CODE`,`VERSION`,`CREATE_TIME`); 
	END IF;
	
	IF EXISTS(SELECT 1
			  FROM information_schema.statistics
			  WHERE TABLE_SCHEMA = db
				 AND TABLE_NAME = 'T_ATOM'
				 AND INDEX_NAME = 'inx_tpca_atom_code') THEN
		ALTER TABLE T_ATOM DROP INDEX `inx_tpca_atom_code`;
	END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();