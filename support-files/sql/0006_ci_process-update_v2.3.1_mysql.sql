USE devops_ci_process;
SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for T_PIPELINE_BUILD_HIS_DATA_CLEAR
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_PIPELINE_BUILD_HIS_DATA_CLEAR` (
  `BUILD_ID` varchar(34) NOT NULL,
  `PIPELINE_ID` varchar(34) NOT NULL,
  `PROJECT_ID` varchar(64) NOT NULL,
  `DEL_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`BUILD_ID`),
  KEY `INX_PROJECT_PIPELINE` (`PROJECT_ID`,`PIPELINE_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;