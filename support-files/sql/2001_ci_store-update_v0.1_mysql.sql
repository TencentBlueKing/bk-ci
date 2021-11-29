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
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'PUB_TIME') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间' AFTER `VISIBILITY_LEVEL`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'PRIVATE_REASON') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `PRIVATE_REASON` varchar(256) DEFAULT NULL COMMENT '插件代码库不开源原因' AFTER `PUB_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除' AFTER `PRIVATE_REASON`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND INDEX_NAME = 'inx_ta_delete_flag') THEN
        ALTER TABLE T_ATOM
            ADD INDEX `inx_ta_delete_flag` (`DELETE_FLAG`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_BUILD_INFO'
                    AND COLUMN_NAME = 'SAMPLE_PROJECT_PATH') THEN
        ALTER TABLE T_ATOM_BUILD_INFO
            ADD COLUMN `SAMPLE_PROJECT_PATH` varchar(500) NOT NULL DEFAULT '' COMMENT '样例工程路径' AFTER `REPOSITORY_PATH`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_BUILD_INFO'
                    AND COLUMN_NAME = 'ENABLE') THEN
        ALTER TABLE T_ATOM_BUILD_INFO
            ADD COLUMN `ENABLE` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用' AFTER `SAMPLE_PROJECT_PATH`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'RECOMMEND_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `RECOMMEND_FLAG` bit(1) DEFAULT b'1' COMMENT '是否推荐' AFTER `UPDATE_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'PRIVATE_REASON') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `PRIVATE_REASON` varchar(256) DEFAULT NULL COMMENT '插件代码库不开源原因' AFTER `RECOMMEND_FLAG`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除' AFTER `PRIVATE_REASON`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE'
                    AND COLUMN_NAME = 'PUB_TIME') THEN
        ALTER TABLE T_TEMPLATE
            ADD COLUMN `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间' AFTER `UPDATE_TIME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_IMAGE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除' AFTER `AGENT_TYPE_SCOPE`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE_FEATURE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_IMAGE_FEATURE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' COMMENT '是否删除' AFTER `IMAGE_TYPE`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE_AGENT_TYPE'
                    AND COLUMN_NAME = 'IMAGE_CODE'
                    AND COLUMN_TYPE = 'varchar(64)') THEN
        ALTER TABLE T_IMAGE_AGENT_TYPE CHANGE COLUMN `IMAGE_CODE` `IMAGE_CODE` varchar(64) COMMENT '镜像代码';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DOCKER_FILE_TYPE') THEN
        ALTER TABLE T_IMAGE ADD COLUMN `DOCKER_FILE_TYPE` varchar(32) NOT NULL DEFAULT 'INPUT' COMMENT 'dockerFile类型（INPUT：手动输入，*_LINK：链接）';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DOCKER_FILE_CONTENT') THEN
        ALTER TABLE T_IMAGE ADD COLUMN `DOCKER_FILE_CONTENT` text COMMENT 'dockerFile内容';
    END IF;
	
	IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_VERSION_LOG'
                    AND INDEX_NAME = 'inx_tpavl_atom_id') THEN
        ALTER TABLE T_ATOM_VERSION_LOG DROP INDEX inx_tpavl_atom_id;
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_VERSION_LOG'
                    AND INDEX_NAME = 'uni_inx_tavl_atom_id') THEN
        ALTER TABLE T_ATOM_VERSION_LOG ADD UNIQUE INDEX uni_inx_tavl_atom_id (ATOM_ID); 
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'YAML_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE ADD COLUMN `YAML_FLAG` bit(1) DEFAULT b'0' COMMENT 'yaml可用标识';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND INDEX_NAME = 'inx_taf_yml_flag') THEN
        ALTER TABLE T_ATOM_FEATURE ADD INDEX inx_taf_yml_flag (YAML_FLAG); 
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REASON_REL'
                    AND COLUMN_NAME = 'STORE_TYPE') THEN
        ALTER TABLE T_REASON_REL ADD COLUMN `STORE_TYPE` TINYINT(4) NOT NULL DEFAULT '0' COMMENT 'STORE组件类型';
    ELSEIF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_REASON_REL'
                        AND COLUMN_NAME = 'STORE_TYPE'
                        AND COLUMN_TYPE = 'TINYINT(4)') THEN
        ALTER TABLE T_REASON_REL
            CHANGE `STORE_TYPE` `STORE_TYPE` TINYINT(4) NOT NULL DEFAULT '0' COMMENT 'STORE组件类型';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_REASON_REL'
                    AND INDEX_NAME = 'inx_trr_store_type') THEN
        ALTER TABLE T_REASON_REL ADD INDEX inx_trr_store_type (STORE_TYPE); 
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_ENV_INFO'
                    AND COLUMN_NAME = 'POST_ENTRY_PARAM') THEN
        ALTER TABLE T_ATOM_ENV_INFO ADD COLUMN `POST_ENTRY_PARAM` VARCHAR(64) COMMENT '入口参数';
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_ENV_INFO'
                    AND COLUMN_NAME = 'POST_CONDITION') THEN
        ALTER TABLE T_ATOM_ENV_INFO ADD COLUMN `POST_CONDITION` VARCHAR(1024) COMMENT '执行条件';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'QUALITY_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE ADD COLUMN `QUALITY_FLAG` BIT(1) DEFAULT b'0' COMMENT '质量红线可用标识';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();