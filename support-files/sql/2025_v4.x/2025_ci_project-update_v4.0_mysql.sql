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

-- 在 T_PROJECT_APPROVAL 表添加 KPI_CODE 和 KPI_NAME 字段
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS ci_project_schema_update()
BEGIN
    DECLARE COLUMN_NUM INT DEFAULT 0;

    -- 检查 KPI_CODE 字段是否存在
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_APPROVAL'
      AND COLUMN_NAME = 'KPI_CODE';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_APPROVAL`
            ADD COLUMN `KPI_CODE` varchar(64) DEFAULT NULL COMMENT 'KPI产品编码' AFTER `PROPERTIES`;
    END IF;

    -- 检查 KPI_NAME 字段是否存在
    SET COLUMN_NUM = 0;
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_APPROVAL'
      AND COLUMN_NAME = 'KPI_NAME';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_APPROVAL`
            ADD COLUMN `KPI_NAME` varchar(128) DEFAULT NULL COMMENT 'KPI产品名称' AFTER `KPI_CODE`;
    END IF;

    -- 检查 T_PROJECT_UPDATE_HISTORY 表的 BEFORE_KPI_CODE 字段是否存在
    SET COLUMN_NUM = 0;
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
      AND COLUMN_NAME = 'BEFORE_KPI_CODE';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
            ADD COLUMN `BEFORE_KPI_CODE` varchar(64) DEFAULT NULL COMMENT '变更前KPI产品编码' AFTER `AFTER_PRODUCT_ID`;
    END IF;

    -- 检查 T_PROJECT_UPDATE_HISTORY 表的 AFTER_KPI_CODE 字段是否存在
    SET COLUMN_NUM = 0;
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
      AND COLUMN_NAME = 'AFTER_KPI_CODE';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
            ADD COLUMN `AFTER_KPI_CODE` varchar(64) DEFAULT NULL COMMENT '变更后KPI产品编码' AFTER `BEFORE_KPI_CODE`;
    END IF;

    -- 检查 T_PROJECT_UPDATE_HISTORY 表的 BEFORE_KPI_NAME 字段是否存在
    SET COLUMN_NUM = 0;
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
      AND COLUMN_NAME = 'BEFORE_KPI_NAME';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
            ADD COLUMN `BEFORE_KPI_NAME` varchar(128) DEFAULT NULL COMMENT '变更前KPI产品名称' AFTER `AFTER_KPI_CODE`;
    END IF;

    -- 检查 T_PROJECT_UPDATE_HISTORY 表的 AFTER_KPI_NAME 字段是否存在
    SET COLUMN_NUM = 0;
    SELECT COUNT(1) INTO COLUMN_NUM FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'devops_ci_project'
      AND TABLE_NAME = 'T_PROJECT_UPDATE_HISTORY'
      AND COLUMN_NAME = 'AFTER_KPI_NAME';
    IF COLUMN_NUM = 0 THEN
        ALTER TABLE `T_PROJECT_UPDATE_HISTORY`
            ADD COLUMN `AFTER_KPI_NAME` varchar(128) DEFAULT NULL COMMENT '变更后KPI产品名称' AFTER `BEFORE_KPI_NAME`;
    END IF;

END //
DELIMITER ;

CALL ci_project_schema_update();
DROP PROCEDURE IF EXISTS ci_project_schema_update;
