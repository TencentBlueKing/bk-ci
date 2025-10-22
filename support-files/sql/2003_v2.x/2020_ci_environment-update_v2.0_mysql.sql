/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
                    AND COLUMN_NAME = 'SYSTEM_UPDATE_TIME') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `SYSTEM_UPDATE_TIME` timestamp null default null comment '系统任务更新数据时间';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'SERVER_ID') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `SERVER_ID` bigint(20) null default null comment '服务器id';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'HOST_ID') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `HOST_ID` bigint(20) default null comment 'CC的host_id';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'CLOUD_AREA_ID') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `CLOUD_AREA_ID` bigint(20) default null comment '云区域id，公司内为0';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'AGENT_VERSION') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `AGENT_VERSION` varchar(64) default null comment 'agent版本';
        UPDATE T_NODE
            JOIN T_ENVIRONMENT_THIRDPARTY_AGENT ON T_NODE.NODE_ID = T_ENVIRONMENT_THIRDPARTY_AGENT.NODE_ID
        SET T_NODE.AGENT_VERSION = T_ENVIRONMENT_THIRDPARTY_AGENT.MASTER_VERSION
        WHERE T_NODE.NODE_TYPE = 'THIRDPARTY';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND COLUMN_NAME = 'OS_TYPE') THEN
        ALTER TABLE `T_NODE`
            ADD COLUMN `OS_TYPE` varchar(64) default null comment '从CC中查到的os类型';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND INDEX_NAME = 'PROJECT_ID') THEN
        ALTER TABLE `T_NODE`
            ADD INDEX `PROJECT_ID` (`PROJECT_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND INDEX_NAME = 'HOST_ID') THEN
        ALTER TABLE `T_NODE`
            ADD INDEX `HOST_ID` (`HOST_ID`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND INDEX_NAME = 'NODE_IP') THEN
        ALTER TABLE `T_NODE`
            ADD INDEX `NODE_IP` (`NODE_IP`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ENV_NODE'
                    AND COLUMN_NAME = 'ENABLE_NODE') THEN
        ALTER TABLE `T_ENV_NODE`
            ADD COLUMN `ENABLE_NODE` bit(1) DEFAULT 1 NOT NULL COMMENT '是否启用节点';
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
