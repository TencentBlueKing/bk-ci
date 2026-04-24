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
                    AND TABLE_NAME = 'T_PROJECT'
                    AND COLUMN_NAME = 'HIDDEN') THEN
ALTER TABLE T_PROJECT
    ADD COLUMN `HIDDEN` bit(1)
        DEFAULT b'0' COMMENT '是否隐藏';
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'KPI_CODE') THEN
ALTER TABLE `T_PROJECT_APPROVAL`
    ADD COLUMN `KPI_CODE` varchar(64) DEFAULT NULL COMMENT 'KPI产品编码' AFTER `PROPERTIES`;
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_APPROVAL'
                    AND COLUMN_NAME = 'KPI_NAME') THEN
ALTER TABLE `T_PROJECT_APPROVAL`
    ADD COLUMN `KPI_NAME` varchar(128) DEFAULT NULL COMMENT 'KPI产品名称' AFTER `KPI_CODE`;
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
                    AND COLUMN_NAME = 'BEFORE_KPI_CODE') THEN
ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
    ADD COLUMN `BEFORE_KPI_CODE` varchar(64) DEFAULT NULL COMMENT '变更前KPI产品编码' AFTER `AFTER_PRODUCT_ID`;
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
                    AND COLUMN_NAME = 'AFTER_KPI_CODE') THEN
ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
    ADD COLUMN `AFTER_KPI_CODE` varchar(64) DEFAULT NULL COMMENT '变更后KPI产品编码' AFTER `BEFORE_KPI_CODE`;
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
                    AND COLUMN_NAME = 'BEFORE_KPI_NAME') THEN
ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
    ADD COLUMN `BEFORE_KPI_NAME` varchar(128) DEFAULT NULL COMMENT '变更前KPI产品名称' AFTER `AFTER_KPI_CODE`;
END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
                    AND COLUMN_NAME = 'AFTER_KPI_NAME') THEN
ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
    ADD COLUMN `AFTER_KPI_NAME` varchar(128) DEFAULT NULL COMMENT '变更后KPI产品名称' AFTER `BEFORE_KPI_NAME`;
END IF;

END //
DELIMITER ;

CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;
