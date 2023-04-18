/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

USE devops_ci_metrics;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_metrics_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_metrics_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                      FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = db
                        AND TABLE_NAME = 'T_ERROR_CODE_INFO'
                        AND COLUMN_NAME = 'ATOM_CODE') THEN
    ALTER TABLE `T_ERROR_CODE_INFO`
        ADD COLUMN `ATOM_CODE` varchar(64) DEFAULT NULL COMMENT '关联插件代码';
    END IF;

    IF EXISTS(SELECT 1
                          FROM information_schema.statistics
                          WHERE TABLE_SCHEMA = db
                            AND TABLE_NAME = 'T_ERROR_CODE_INFO'
                            AND INDEX_NAME = 'UNI_TECI_TYPE_CODE') THEN
    ALTER TABLE `T_ERROR_CODE_INFO`
        DROP INDEX `UNI_TECI_TYPE_CODE`;
    END IF;

    IF NOT EXISTS(SELECT 1
                              FROM information_schema.statistics
                              WHERE TABLE_SCHEMA = db
                                AND TABLE_NAME = 'T_ERROR_CODE_INFO'
                                AND INDEX_NAME = 'T_ERROR_CODE_INFO_UN') THEN
    ALTER TABLE `T_ERROR_CODE_INFO`
        ADD INDEX `T_ERROR_CODE_INFO_UN`(ERROR_TYPE, ERROR_CODE, ATOM_CODE);
    END IF;

    IF NOT EXISTS(SELECT 1
                                  FROM information_schema.statistics
                                  WHERE TABLE_SCHEMA = db
                                    AND TABLE_NAME = 'T_ERROR_CODE_INFO'
                                    AND INDEX_NAME = 'INX_TECI_ATOM_CODE') THEN
    ALTER TABLE `T_ERROR_CODE_INFO`ADD INDEX `INX_TECI_ATOM_CODE` (`ATOM_CODE`);
    END IF;

    IF EXISTS(SELECT 1
                              FROM information_schema.statistics
                              WHERE TABLE_SCHEMA = db
                                AND TABLE_NAME = 'T_ERROR_CODE_INFO'
                                AND INDEX_NAME = 'T_ERROR_CODE_INFO_UN') THEN
    ALTER TABLE T_ERROR_CODE_INFO DROP INDEX T_ERROR_CODE_INFO_UN;
    END IF;

    IF NOT EXISTS(SELECT 1
                                  FROM information_schema.statistics
                                  WHERE TABLE_SCHEMA = db
                                    AND TABLE_NAME = 'T_ERROR_CODE_INFO') THEN
    ALTER TABLE T_ERROR_CODE_INFO ADD CONSTRAINT T_ERROR_CODE_INFO_UN UNIQUE KEY (ERROR_TYPE,ERROR_CODE,ATOM_CODE);
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_metrics_schema_update();
