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

USE devops_ci_stream;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_stream_schema_update;

DELIMITER <CI_UBF>
CREATE PROCEDURE ci_stream_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
SELECT DATABASE() INTO db;

IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_BASIC_SETTING'
                    AND COLUMN_NAME = 'TRIGGER_REVIEW_SETTING') THEN
ALTER TABLE `T_GIT_BASIC_SETTING`
    ADD COLUMN `TRIGGER_REVIEW_SETTING` text null COMMENT 'pr、mr触发时的权限校验(存储为json字符串)';
	
END IF;

IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_GIT_REQUEST_EVENT'
                    AND COLUMN_NAME = 'CHANGE_YAML_LIST') THEN
ALTER TABLE `T_GIT_REQUEST_EVENT`
    ADD COLUMN `CHANGE_YAML_LIST` text COMMENT 'yaml变更文件列表';

END IF;

COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;

CALL ci_stream_schema_update();
