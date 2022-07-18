USE devops_ci_dispatch_bcs;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_DISPATCH_MACHINE
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_BUILD_BUILDER_POOL_NO` (
    `BUILD_ID` varchar(64) NOT NULL COMMENT '构建ID',
    `VM_SEQ_ID` varchar(64) NOT NULL COMMENT 'VmSeqID',
    `BUILDER_NAME` varchar(128) DEFAULT NULL COMMENT '构建机名称',
    `POOL_NO` varchar(128) DEFAULT NULL COMMENT '构建机池编号',
    `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `EXECUTE_COUNT` int(11) DEFAULT '1' COMMENT '流水线重试次数',
    PRIMARY KEY (`BUILD_ID`,`VM_SEQ_ID`),
    KEY `inx_tpi_create_time` (`CREATE_TIME`),
    KEY `inx_tpi_build_id` (`BUILD_ID`),
    KEY `inx_tpi_vm_seq_id` (`VM_SEQ_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='buildId和builderName,poolNo的映射关系';

CREATE TABLE IF NOT EXISTS `T_BCS_PERFORMANCE_OPTIONS` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `CPU` float(11) DEFAULT '1' COMMENT 'CPU',
    `MEMORY` Int(11) DEFAULT '1024' COMMENT '内存 单位M',
    `DISK` Int(11) DEFAULT '100' COMMENT '磁盘 单位G',
    `DESCRIPTION` varchar(128) NOT NULL DEFAULT '' COMMENT '描述',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_idx` (`CPU`,`MEMORY`,`DISK`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BCS基础配额表';

CREATE TABLE IF NOT EXISTS `T_BCS_BUILD` (
    `PIPELINE_ID` varchar(34) NOT NULL,
    `VM_SEQ_ID` varchar(34) NOT NULL,
    `POOL_NO` int(11) NOT NULL,
    `PROJECT_ID` varchar(64) NOT NULL,
    `BUILDER_NAME` varchar(128) NOT NULL,
    `IMAGES` varchar(1024) NOT NULL,
    `STATUS` int(11) NOT NULL,
    `CREATED_TIME` timestamp NULL DEFAULT NULL,
    `UPDATE_TIME` timestamp NULL DEFAULT NULL,
    `USER_ID` varchar(34) NOT NULL,
    `DEBUG_STATUS` bit(1) DEFAULT b'0' COMMENT '是否处于debug状态',
    `DEBUG_TIME` timestamp NULL DEFAULT NULL COMMENT 'debug时间',
    `CPU` float(11) DEFAULT '1' COMMENT 'CPU',
    `MEMORY` Int(11) DEFAULT '1024' COMMENT '内存 单位M',
    `DISK` Int(11) DEFAULT '100' COMMENT '磁盘 单位G',
    PRIMARY KEY (`PIPELINE_ID`,`VM_SEQ_ID`,`POOL_NO`),
    KEY `idx_1` (`STATUS`,`DEBUG_STATUS`,`DEBUG_TIME`),
    KEY `idx_pipeline_vm_name` (`PIPELINE_ID`,`VM_SEQ_ID`,`BUILDER_NAME`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BCS流水线构建机信息';

CREATE TABLE IF NOT EXISTS `T_BCS_BUILD_HIS` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT,
    `PIPELINE_ID` varchar(64) NOT NULL  COMMENT 'pipeline id',
    `BUIDLD_ID` varchar(64) NOT NULL  COMMENT 'build id',
    `VM_SEQ_ID` varchar(64) NOT NULL  COMMENT 'vm seq id',
    `BUILDER_NAME` varchar(128)  COMMENT '构建机名称',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    `SECRET_KEY` varchar(64)  COMMENT '构建密钥',
    `POOL_NO` varchar(64)  COMMENT '并发构建池',
    `CPU` float(11) DEFAULT '1' COMMENT 'CPU',
    `MEMORY` Int(11) DEFAULT '1024' COMMENT '内存 单位M',
    `DISK` Int(11) DEFAULT '100' COMMENT '磁盘 单位G',
    `EXECUTE_COUNT` int(11) DEFAULT '1' COMMENT '流水线重试次数',
    `PROJECT_ID` varchar(128)  COMMENT '蓝盾项目ID',
    PRIMARY KEY (`ID`),
    KEY `idx_1` (`BUIDLD_ID`,`PIPELINE_ID`,`VM_SEQ_ID`),
    KEY `idx_pipeline_vm_modify` (`PIPELINE_ID`,`VM_SEQ_ID`,`GMT_CREATE`),
    KEY `IDX_BUILD_VM_COUNT` (`BUIDLD_ID`,`VM_SEQ_ID`,`EXECUTE_COUNT`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bcs 构建历史记录';

CREATE TABLE IF NOT EXISTS `T_BCS_PERFORMANCE_CONFIG` (
    `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `PROJECT_ID` varchar(64) NOT NULL COMMENT '蓝盾项目ID',
    `OPTION_ID` bigint(20) NOT NULL DEFAULT '0' COMMENT '基础配置ID',
    `GMT_CREATE` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `GMT_MODIFIED` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_idx_projectid` (`PROJECT_ID`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bcs配额表';

SET FOREIGN_KEY_CHECKS = 1;
