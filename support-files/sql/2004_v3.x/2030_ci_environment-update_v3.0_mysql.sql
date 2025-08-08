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
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_NODE'
                    AND INDEX_NAME = 'SERVER_ID') THEN
        ALTER TABLE `T_NODE`
            ADD INDEX `SERVER_ID` (`SERVER_ID`);
    END IF;
	
	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'SIZE') THEN
    ALTER TABLE T_NODE
            ADD COLUMN `SIZE` varchar(32) DEFAULT NULL COMMENT '机型';
    END IF;

	IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_NODE'
                        AND COLUMN_NAME = 'LAST_BUILD_PIPELINE_ID') THEN
    ALTER TABLE T_NODE
            ADD COLUMN `LAST_BUILD_PIPELINE_ID` varchar(64) DEFAULT NULL COMMENT '最近构建流水线ID';
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'DISPLAY_NAME') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `DISPLAY_NAME`(`DISPLAY_NAME`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'OS_NAME') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `OS_NAME`(`OS_NAME`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'NODE_STATUS') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `NODE_STATUS`(`NODE_STATUS`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'NODE_TYPE') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `NODE_TYPE`(`NODE_TYPE`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'CREATED_USER') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `CREATED_USER`(`CREATED_USER`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'LAST_MODIFY_USER') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `LAST_MODIFY_USER`(`LAST_MODIFY_USER`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'LAST_MODIFY_TIME') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `LAST_MODIFY_TIME`(`LAST_MODIFY_TIME`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'LAST_BUILD_PIPELINE_ID') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `LAST_BUILD_PIPELINE_ID`(`LAST_BUILD_PIPELINE_ID`);
    END IF;
	
    IF NOT EXISTS(SELECT 1
                   FROM information_schema.statistics
                   WHERE TABLE_SCHEMA = db
                     AND TABLE_NAME = 'T_NODE'
                     AND INDEX_NAME = 'LAST_BUILD_TIME') THEN
    ALTER TABLE `T_NODE`
        ADD INDEX `LAST_BUILD_TIME`(`LAST_BUILD_TIME`);
    END IF;
	
    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_environment_schema_update();
