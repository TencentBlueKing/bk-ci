CREATE DATABASE IF NOT EXISTS `devops_ci_openapi` DEFAULT CHARACTER SET utf8mb4;
USE devops_ci_openapi;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_APP_CODE_GROUP
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_CODE_GROUP` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `APP_CODE` varchar(255) NOT NULL COMMENT 'APP编码',
  `BG_ID` int(11) DEFAULT NULL COMMENT '事业群ID',
  `BG_NAME` varchar(255) DEFAULT NULL COMMENT '事业群名称',
  `DEPT_ID` int(11) DEFAULT NULL COMMENT '项目所属二级机构ID',
  `DEPT_NAME` varchar(255) DEFAULT NULL COMMENT '项目所属二级机构名称',
  `CENTER_ID` int(11) DEFAULT NULL COMMENT '中心ID',
  `CENTER_NAME` varchar(255) DEFAULT NULL COMMENT '中心名字',
  `CREATOR` varchar(255) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `UPDATER` varchar(255) DEFAULT NULL COMMENT '跟新人',
  `UPDATE_TIME` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='app_code对应的组织架构';

-- ----------------------------
-- Table structure for T_APP_CODE_PROJECT
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_APP_CODE_PROJECT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `APP_CODE` varchar(255) NOT NULL COMMENT 'APP编码',
  `PROJECT_ID` varchar(255) NOT NULL COMMENT '项目ID',
  `CREATOR` varchar(255) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='app_code对应的蓝盾项目';

-- ----------------------------
-- Table structure for T_APP_USER_INFO
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_APP_USER_INFO`(
  `ID` INT(11) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `APP_CODE` VARCHAR(64) NOT NULL COMMENT 'APP编码',
  `MANAGER_ID` VARCHAR(64) NOT NULL COMMENT 'APP管理员ID',
  `IS_DELETE` BIT(1) NOT NULL COMMENT '是否删除',
  `CREATE_USER` VARCHAR(64) NOT NULL COMMENT '添加人员',
  `CREATE_TIME` DATETIME(3) NOT NULL COMMENT '添加时间',
  PRIMARY KEY (`ID`),
  INDEX `IDX_APP` (`APP_CODE`),
  UNIQUE INDEX `IDX_APP_USER` (`APP_CODE`, `MANAGER_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='app_code对应的管理员';

-- ----------------------------
-- Table structure for T_OPENAPI_METRICS_FOR_API
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_OPENAPI_METRICS_FOR_API`(
  `API` VARCHAR(64) NOT NULL COMMENT 'api接口代码',
  `KEY` VARCHAR(64) NOT NULL COMMENT 'APP编码/api请求用户',
  `SECOND_LEVEL_CONCURRENCY` int(11) NOT NULL COMMENT '秒级并发量',
  `PEAK_CONCURRENCY` int(11) NOT NULL COMMENT '峰值并发量',
  `CALL_5M` int(11) NOT NULL COMMENT '5min调用量',
  `CALL_1H` int(11) NOT NULL COMMENT '1h调用量',
  `CALL_24H` int(11) NOT NULL COMMENT '24h调用量',
  `CALL_7D` int(11) NOT NULL COMMENT '7d调用量',
  INDEX `IDX_KEY` (`KEY`),
  UNIQUE INDEX `IDX_API_KEY` (`API`, `KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='接口维度度量表';

-- ----------------------------
-- Table structure for T_OPENAPI_METRICS_FOR_PROJECT
-- ----------------------------
CREATE TABLE IF NOT EXISTS `T_OPENAPI_METRICS_FOR_PROJECT`(
  `PROJECT` VARCHAR(64) NOT NULL COMMENT '项目id',
  `API` VARCHAR(64) NOT NULL COMMENT 'api接口代码',
  `KEY` VARCHAR(64) NOT NULL COMMENT 'APP编码/api请求用户',
  `CALL_HISTORY` int(11) NOT NULL COMMENT '历史累计调用',
  INDEX `IDX_API` (`API`),
  INDEX `IDX_KEY` (`KEY`),
  UNIQUE INDEX `IDX_APP_USER` (`PROJECT`, `API`, `KEY`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目维度度量表';

SET FOREIGN_KEY_CHECKS = 1;
