USE devops_ci_process;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_process_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_process_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- 1. 为 T_TEMPLATE_INSTANCE_BASE 表添加多个字段
    --    通过检查第一个字段`PAC`是否存在来判断是否需要执行整个ALTER语句
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_INSTANCE_BASE'
                    AND COLUMN_NAME = 'PAC') THEN
        ALTER TABLE `T_TEMPLATE_INSTANCE_BASE`
        ADD `PAC` bit(1) DEFAULT b'0' COMMENT '是否开启PAC',
        ADD `TARGET_ACTION` varchar(64) DEFAULT NULL COMMENT '代码库分支操作',
        ADD `TYPE` varchar(32) DEFAULT 'UPDATE' COMMENT '模版实例化类型,CREATE/UPDATE',
        ADD `REPO_HASH_ID` varchar(64) DEFAULT NULL COMMENT '代码库哈希ID',
        ADD `TARGET_BRANCH` varchar(256) DEFAULT NULL COMMENT '代码库分支',
        ADD `PULL_REQUEST_ID` bigint(11) DEFAULT NULL COMMENT '合并请求ID',
        ADD `PULL_REQUEST_URL` varchar(512) DEFAULT NULL COMMENT '合并请求链接',
        ADD `TEMPLATE_REF_TYPE` varchar(20) DEFAULT NULL COMMENT '模板引用类型,PATH/ID',
        ADD `TEMPLATE_REF` varchar(512) DEFAULT NULL COMMENT 'PATH引用时模版版本',
        ADD `DESCRIPTION` text COMMENT '提交描述';
    END IF;

    -- 2. 为 T_TEMPLATE_INSTANCE_ITEM 表删除索引
    --    通过检查索引`UNI_INX_TTI_PIPELINE_ID`是否存在来判断是否需要执行删除操作
    IF EXISTS(SELECT 1
              FROM information_schema.STATISTICS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_TEMPLATE_INSTANCE_ITEM'
                AND INDEX_NAME = 'UNI_INX_TTI_PIPELINE_ID') THEN
        ALTER TABLE `T_TEMPLATE_INSTANCE_ITEM` DROP INDEX `UNI_INX_TTI_PIPELINE_ID`;
    END IF;

    -- 3. 为 T_TEMPLATE_INSTANCE_ITEM 表添加多个字段
    --    通过检查第一个字段`ERROR_MESSAGE`是否存在来判断是否需要执行整个ALTER语句
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_INSTANCE_ITEM'
                    AND COLUMN_NAME = 'ERROR_MESSAGE') THEN
        ALTER TABLE `T_TEMPLATE_INSTANCE_ITEM`
        ADD `ERROR_MESSAGE` text COMMENT '错误信息',
        ADD `FILE_PATH` text COMMENT 'yaml文件路径',
        ADD `TRIGGER_CONFIGS` mediumtext COMMENT '触发器配置',
        ADD `OVERRIDE_TEMPLATE_FIELD` mediumtext COMMENT '覆盖模版字段',
        ADD `RESET_BUILD_NO` bit default 0 comment '重置实例推荐版本为基准值';
    END IF;

     -- 4. 为 T_TEMPLATE_PIPELINE 表添加多个字段
    --    通过检查第一个字段`STATUS`是否存在来判断是否需要执行整个ALTER语句
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE_PIPELINE'
                    AND COLUMN_NAME = 'STATUS') THEN
        ALTER TABLE `T_TEMPLATE_PIPELINE`
        ADD `STATUS` varchar(32) default 'UPDATED' not null comment '状态',
        ADD `PULL_REQUEST_URL` varchar(512) null comment '合并请求链接',
        ADD `PULL_REQUEST_ID` bigint null comment '合并请求ID';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY'
                AND COLUMN_NAME = 'VERSION_CHANGE') THEN
    ALTER TABLE T_PIPELINE_BUILD_HISTORY
        ADD`VERSION_CHANGE` BIT DEFAULT NULL comment '是否发生版本变更';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_BUILD_HISTORY_DEBUG'
                AND COLUMN_NAME = 'VERSION_CHANGE') THEN
    ALTER TABLE T_PIPELINE_BUILD_HISTORY_DEBUG
        ADD`VERSION_CHANGE` BIT DEFAULT NULL comment '是否发生版本变更';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_YAML_INFO'
                AND COLUMN_NAME = 'RESOURCE_ID') THEN
    ALTER TABLE `T_PIPELINE_YAML_INFO`
        ADD COLUMN `RESOURCE_ID` varchar(64) not null comment '资源ID, 流水线ID/模版ID',
        ADD COLUMN `RESOURCE_TYPE`  varchar(32) default 'PIPELINE' not null comment '资源类型,流水线/模版';
    END IF;

    IF NOT EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'T_PIPELINE_YAML_VERSION'
                AND COLUMN_NAME = 'RESOURCE_ID') THEN
    ALTER TABLE `T_PIPELINE_YAML_VERSION`
        ADD COLUMN `RESOURCE_ID` varchar(64) not null comment '资源ID, 流水线ID/模版ID',
        ADD COLUMN `RESOURCE_TYPE`  varchar(32) default 'PIPELINE' not null comment '资源类型,流水线/模版';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING'
                    AND COLUMN_NAME = 'BUILD_CANCEL_POLICY') THEN
        ALTER TABLE T_PIPELINE_SETTING
            ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION'
            COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有流水线管理权限可取消';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_SETTING_VERSION'
                    AND COLUMN_NAME = 'BUILD_CANCEL_POLICY') THEN
        ALTER TABLE T_PIPELINE_SETTING_VERSION
            ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION'
            COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有流水线管理权限可取消';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_SETTING_VERSION'
                    AND COLUMN_NAME = 'BUILD_CANCEL_POLICY') THEN
        ALTER TABLE T_PIPELINE_TEMPLATE_SETTING_VERSION
            ADD COLUMN `BUILD_CANCEL_POLICY` varchar(32) DEFAULT 'EXECUTE_PERMISSION'
            COMMENT '构建取消权限策略:EXECUTE_PERMISSION-执行权限用户可取消,RESTRICTED-仅触发人/拥有流水线管理权限可取消';
    END IF;

    -- 为 T_PIPELINE_BUILD_VAR 表添加 SENSITIVE 字段
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_BUILD_VAR'
                    AND COLUMN_NAME = 'SENSITIVE') THEN
        ALTER TABLE T_PIPELINE_BUILD_VAR
            ADD COLUMN `SENSITIVE` bit(1) DEFAULT NULL COMMENT '是否敏感';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PIPELINE_TEMPLATE_MIGRATION'
                    AND COLUMN_NAME = 'VALIDATION_DISCREPANCIES') THEN
    ALTER TABLE `T_PIPELINE_TEMPLATE_MIGRATION`
        ADD COLUMN `VALIDATION_DISCREPANCIES` mediumtext COMMENT '验证差异详情(JSON)';
    END IF;
COMMIT;

END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_process_schema_update();
