USE devops_ci_image;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for T_UPLOAD_IMAGE_TASK
-- ----------------------------

CREATE TABLE IF NOT EXISTS `T_UPLOAD_IMAGE_TASK` (
  `TASK_ID` varchar(128) NOT NULL COMMENT '任务ID',
  `PROJECT_ID` varchar(128) NOT NULL COMMENT '项目ID',
  `OPERATOR` varchar(128) NOT NULL COMMENT '操作员',
  `CREATED_TIME` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `UPDATED_TIME` timestamp NULL DEFAULT NULL COMMENT '修改时间',
  `TASK_STATUS` varchar(32) NOT NULL COMMENT '任务状态',
  `TASK_MESSAGE` varchar(256) DEFAULT NULL COMMENT '任务消息',
  `IMAGE_DATA` longtext COMMENT '镜像列表',
  PRIMARY KEY (`TASK_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='';

SET FOREIGN_KEY_CHECKS = 1;
