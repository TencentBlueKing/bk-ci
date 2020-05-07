USE devops_ci_process;
SET NAMES utf8mb4;

-- Stage预置标签
REPLACE INTO `T_PIPELINE_STAGE_TAG` (`ID`, `STAGE_TAG_NAME`, `WEIGHT`, `CREATOR`, `MODIFIER`, `CREATE_TIME`, `UPDATE_TIME`) VALUES
	('28ee946a59f64949a74f3dee40a1bda4','Build',99,'system','system','2020-03-03 18:07:12','2020-03-19 16:29:38'),
	('53b4d3f38e3e425cb1aaa97aa1b37857','Deploy',0,'system','system','2020-03-19 18:00:04','2020-03-19 18:00:04'),
	('d0a06f6986fa4670af65ccad7bb49d3a','Test',50,'system','system','2020-03-19 16:29:45','2020-03-19 16:29:45');