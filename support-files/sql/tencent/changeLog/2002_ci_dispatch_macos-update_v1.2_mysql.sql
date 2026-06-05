USE devops_ci_dispatch_macos;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS ci_dispatch_macos_schema_update_v1_2;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_dispatch_macos_schema_update_v1_2()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.TABLES
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_DEBUG_HISTORY') THEN
        CREATE TABLE `T_DEBUG_HISTORY` (
            `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
            `PROJECT_ID` varchar(128) NOT NULL COMMENT '项目ID',
            `PIPELINE_ID` varchar(128) NOT NULL COMMENT '流水线ID',
            `BUILD_ID` varchar(128) NOT NULL COMMENT '构建ID',
            `VM_SEQ_ID` varchar(128) NOT NULL COMMENT '虚拟机序列ID',
            `EXECUTE_COUNT` int(11) NOT NULL DEFAULT 1 COMMENT '执行次数',
            `TASK_ID` varchar(128) NOT NULL DEFAULT '' COMMENT '调试使用的taskId',
            `NEW_CREATED_VM` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否是新创建的VM（Done状态下新建的VM，需要在stopDebug时关机）',
            `USER_ID` varchar(128) NOT NULL COMMENT '用户ID',
            `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `UPDATE_TIME` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            `STATUS` varchar(64) NOT NULL DEFAULT 'DEBUGGING' COMMENT '调试状态：DEBUGGING/STOPPED',
            PRIMARY KEY (`ID`) USING BTREE,
            UNIQUE KEY `UNI_BUILD_VMSEQ_EXECCOUNT` (`BUILD_ID`, `VM_SEQ_ID`, `EXECUTE_COUNT`),
            KEY `IDX_PIPELINE_VMSEQ_STATUS` (`PIPELINE_ID`, `VM_SEQ_ID`, `STATUS`)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='MacOS调试历史记录表';
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_BUILD_HISTORY'
                    AND COLUMN_NAME = 'OS') THEN
        ALTER TABLE `T_BUILD_HISTORY`
            ADD COLUMN `OS` varchar(128) DEFAULT '' COMMENT '操作系统版本' AFTER `TASK_ID`,
            ADD COLUMN `XCODE` varchar(128) DEFAULT '' COMMENT 'Xcode版本' AFTER `OS`,
            ADD COLUMN `MACOS_HW_SPEC` varchar(128) DEFAULT '' COMMENT 'MacOS硬件规格' AFTER `XCODE`,
            ADD COLUMN `SOURCE` varchar(64) DEFAULT '' COMMENT '来源（landun/gongfeng）' AFTER `MACOS_HW_SPEC`;
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_dispatch_macos_schema_update_v1_2();
