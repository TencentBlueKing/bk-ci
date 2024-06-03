# 蓝盾数据清理
## 背景

 随着蓝盾的构建量快速增长， 蓝盾DB的数据量也越来越大，而蓝盾的DB的容量是有限的，故蓝盾需要一套DB数据清理方案来保证系统的稳定。



## 清理步骤

#### 一、确定表数据清理方案

​         蓝盾的表可以分为流水数据表和非流水线数据表这二种类型，流水数据表很早之前的数据用户并不关心，所以流水线数库表可以通过分区表的方式来定时清理数据；非流水线数据表的数据相对比较重要，没法简单按时间维度删除数据，故我们需要开发一个依据特定条件删除数据的定时任务。



#### 二、分区表数据清理

#####   1、确定数据库表是否能调整为分区表

**新增表：** 新表没有历史包袱，只需明确分区的字段和每个分区的大小就行，以下是按时间就行分区表的建表语句：

```
CREATE TABLE `T_REPOSITORY_COMMIT` (
  `ID` bigint(20) NOT NULL AUTO_INCREMENT,
  `BUILD_ID` varchar(34) DEFAULT NULL,
  `PIPELINE_ID` varchar(34) DEFAULT NULL,
  `REPO_ID` bigint(20) DEFAULT NULL,
  `TYPE` smallint(6) DEFAULT NULL COMMENT '1-svn, 2-git, 3-gitlab',
  `COMMIT` varchar(64) DEFAULT NULL,
  `COMMITTER` varchar(64) DEFAULT NULL,
  `COMMIT_TIME` datetime DEFAULT NULL,
  `COMMENT` longtext /*!99104 COMPRESSED */,
  `ELEMENT_ID` varchar(34) DEFAULT NULL,
  `REPO_NAME` varchar(128) DEFAULT NULL,
  `CREATE_TIME` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `URL` varchar(255) DEFAULT NULL COMMENT '代码库URL',
  PRIMARY KEY (`ID`,`CREATE_TIME`),
  KEY `BUILD_ID_INDEX` (`BUILD_ID`),
  KEY `IDX_PIPE_ELEMENT_REPO_TIME` (`PIPELINE_ID`,`ELEMENT_ID`,`REPO_ID`,`COMMIT_TIME`),
  KEY `IDX_BUILD_ID_TIME` (`BUILD_ID`,`COMMIT_TIME`),
  KEY `IDX_PIPE_ELEMENT_NAME_REPO_TIME` (`PIPELINE_ID`,`ELEMENT_ID`,`REPO_NAME`,`COMMIT_TIME`)
) ENGINE=InnoDB AUTO_INCREMENT=3170191500 DEFAULT CHARSET=utf8mb4
/*!50100 PARTITION BY RANGE (TO_DAYS(CREATE_TIME))
(PARTITION p20240424 VALUES LESS THAN (739366) ENGINE = InnoDB,
 PARTITION p20240501 VALUES LESS THAN (739373) ENGINE = InnoDB,
 PARTITION p20240508 VALUES LESS THAN (739380) ENGINE = InnoDB,
 PARTITION p20240515 VALUES LESS THAN (739387) ENGINE = InnoDB,
 PARTITION p20240522 VALUES LESS THAN (739394) ENGINE = InnoDB,
 PARTITION p20240529 VALUES LESS THAN (739401) ENGINE = InnoDB,
 PARTITION p20240605 VALUES LESS THAN (739408) ENGINE = InnoDB,
 PARTITION p20240612 VALUES LESS THAN (739415) ENGINE = InnoDB,
 PARTITION p20240619 VALUES LESS THAN (739422) ENGINE = InnoDB,
 PARTITION p20240626 VALUES LESS THAN (739429) ENGINE = InnoDB,
 PARTITION p20240703 VALUES LESS THAN (739436) ENGINE = InnoDB,
 PARTITION p20240710 VALUES LESS THAN (739443) ENGINE = InnoDB,
 PARTITION p20240717 VALUES LESS THAN (739450) ENGINE = InnoDB,
 PARTITION p20240724 VALUES LESS THAN (739457) ENGINE = InnoDB,
 PARTITION p20240731 VALUES LESS THAN (739464) ENGINE = InnoDB,
 PARTITION p20240807 VALUES LESS THAN (739471) ENGINE = InnoDB,
 PARTITION p20240814 VALUES LESS THAN (739478) ENGINE = InnoDB,
 PARTITION p20240821 VALUES LESS THAN (739485) ENGINE = InnoDB,
 PARTITION p20240828 VALUES LESS THAN (739492) ENGINE = InnoDB) */;
```

**存量表：**  存量表要在不影响现有业务的情况下调整为分区表相对比较麻烦点，以T_REPOSITORY_COMMIT表为例，以下是具体步骤：

1、创建临时表T_REPOSITORY_COMMIT_TMP(必须为分区表，分区键必须包含在主键内)

2、把T_REPOSITORY_COMMIT表的数据同步至T_REPOSITORY_COMMIT_TMP表并建立同步方案

3、执行 RENAME TABLE T_REPOSITORY_COMMIT TO T_REPOSITORY_COMMIT_BAK,T_REPOSITORY_COMMIT_TMP TO T_REPOSITORY_COMMIT ,T_REPOSITORY_COMMIT_BAK TO T_REPOSITORY_COMMIT_TMP; 该语句进行新旧表名互换。

4、删除T_REPOSITORY_COMMIT_TMP表



##### 2、为分区表创立自动创建分区和删除分区的执行计划



**各微服务数据库的分区表情况：**

| 数据库名称        | 表名                                | 分区字段        | 过期时间（单位：天） | 分区间隔（单位：天） |
| ----------------- | ----------------------------------- | --------------- | -------------------- | -------------------- |
| devops_process    | T_PIPELINE_TRIGGER_EVENT            | CREATE_TIME     | 35                   | 7                    |
| devops_process    | T_PIPELINE_TRIGGER_DETAIL           | CREATE_TIME     | 35                   | 7                    |
| devops_process    | T_PIPELINE_BUILD_VAR                | CREATE_TIME     | 42                   | 7                    |
| devops_process    | T_PIPELINE_BUILD_TASK               | CREATE_TIME     | 42                   | 7                    |
| devops_process    | T_PIPELINE_BUILD_STAGE              | CREATE_TIME     | 42                   | 7                    |
| devops_process    | T_PIPELINE_BUILD_CONTAINER          | CREATE_TIME     | 42                   | 7                    |
| devops_process    | T_PROJECT_PIPELINE_CALLBACK_HISTORY | CREATE_TIME     | 3                    | 3                    |
| devops_process    | T_PIPELINE_WEBHOOK_BUILD_LOG        | CREATE_TIME     | 3                    | 3                    |
| devops_process    | T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL | CREATE_TIME     | 3                    | 3                    |
| devops_repository | T_REPOSITORY_COMMIT                 | CREATE_TIME     | 35                   | 7                    |
| devops_repository | T_REPOSITORY_WEBHOOK_REQUEST        | CREATE_TIME     | 35                   | 7                    |
| devops_notify     | T_NOTIFY_WEWORK                     | CREATED_TIME    | 8                    | 1                    |
| devops_notify     | T_NOTIFY_EMAIL                      | CREATED_TIME    | 8                    | 1                    |
| devops_notify     | T_NOTIFY_RTX                        | CREATED_TIME    | 8                    | 1                    |
| devops_notify     | T_NOTIFY_SMS                        | CREATED_TIME    | 8                    | 1                    |
| devops_notify     | T_NOTIFY_WECHAT                     | CREATED_TIME    | 8                    | 1                    |
| devops_plugin     | T_PLUGIN_GIT_CHECK                  | CREATE_TIME     | 31                   | 1                    |
| devops_stream     | T_GIT_REQUEST_EVENT_BUILD           | CREATE_TIME     | 35                   | 7                    |
| devops_stream     | T_GIT_REQUEST_REPO_EVENT            | CREATE_TIME     | 35                   | 7                    |
| devops_stream     | T_GIT_REQUEST_EVENT_NOT_BUILD       | CREATE_TIME     | 35                   | 7                    |
| devops_stream     | T_GIT_USER_MESSAGE                  | CREATE_TIME     | 35                   | 7                    |
| devops_stream     | T_GIT_REQUEST_EVENT                 | CREATE_TIME     | 35                   | 7                    |
| devops_metrics    | T_PROJECT_THIRD_PLATFORM_DATA       | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_PIPELINE_FAIL_DETAIL_DATA         | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_PIPELINE_FAIL_SUMMARY_DATA        | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_ATOM_MONITOR_DATA_DAILY           | STATISTICS_TIME | 371                  | 7                    |
| devops_metrics    | T_ATOM_INDEX_STATISTICS_DAILY       | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_ATOM_FAIL_DETAIL_DATA             | CREATE_TIME     | 210                  | 7                    |
| devops_metrics    | T_PIPELINE_OVERVIEW_DATA            | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_ATOM_FAIL_SUMMARY_DATA            | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_PIPELINE_STAGE_OVERVIEW_DATA      | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_ATOM_OVERVIEW_DATA                | CREATE_TIME     | 371                  | 7                    |
| devops_metrics    | T_PROJECT_USER_DAILY                | THE_DATE        | 90                   | 15                   |
| devops_metrics    | T_PROJECT_BUILD_SUMMARY_DAILY       | THE_DATE        | 90                   | 15                   |



#### 三、非分区表数据清理

非分区表采用misc服务的定时任务依据时间、构建数量等条件进行清理，具体配置如下：

```
build:
  data:
    clear:
      switch: true # 是否开启自动清理构建历史数据；true:开启，false:不开启
      maxEveryProjectHandleNum: 5 #并发清理项目数量
      monthRange: -1 # 清理多少个月前的普通渠道（BS）流水线运行时数据，对于卡在 stage 审核的流程在清理后就无法继续审核。
      maxKeepNum: 10000 # 普通渠道（BS）流水线最大保留多少条构建历史记录 
      codeccDayRange: -14 # 清理多少天前的codecc渠道流水线运行时数据
      codeccMaxKeepNum: 14 # codecc渠道流水线最大保留多少条构建历史记录 
      otherMonthRange: -1 # 清理多少天前的其它渠道流水线运行时数据
      otherMaxKeepNum: 500 # 其它渠道流水线最大保留多少条构建历史记录 
      clearChannelCodes: "BS,PREBUILD,CI,CODECC" # 支持清理构建上数据的渠道类型
      maxThreadHandleProjectNum: 5 # 开启清理线程最大数量
```

