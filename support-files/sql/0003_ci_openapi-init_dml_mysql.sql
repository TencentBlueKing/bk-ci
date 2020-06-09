use devops_ci_openapi;
set names utf8mb4;

-- 质量红线指标和控制点初始化

INSERT IGNORE INTO `T_APP_CODE_GROUP` (`APP_CODE`, `BG_ID`, `BG_NAME`, `DEPT_ID`, `DEPT_NAME`, `CENTER_ID`, `CENTER_NAME`, `CREATOR`, `CREATE_TIME`, `UPDATER`, `UPDATE_TIME`) VALUES ('bk_ci', NULL, NULL, NULL, NULL, NULL, NULL, 'admin', '2020-05-06 00:00:00', 'admin', '2020-05-06 00:00:00');
INSERT IGNORE INTO `T_APP_CODE_PROJECT` (`APP_CODE`, `PROJECT_ID`, `CREATOR`, `CREATE_TIME`) values('bk_ci', 'demo', 'admin', '2020-05-06 00:00:00');
