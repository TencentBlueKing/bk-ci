# 蓝盾分库分表开发指南
## 背景

 随着蓝盾的构建量快速增长， 快速增长的构建量带来的请求量和数据给蓝盾数据带来了巨大的压力 ，巨大的QPS量和数据量已经让单机数据库达到了瓶颈，蓝盾db支持分库分表势在必行。



## 开发步骤

#### 一、确定分库分表数量

​         根据流水线构建趋势图确定分库分表数量，保证分库分表数量合理。



#### 二、增加分库分表配置

#####   1、在devops_ci_project数据库的T_DATA_SOURCE表插入分区库的记录，T_TABLE_SHARDING_CONFIG表插入分区表的记录（只分库不分表的情况下可以不用配置），数据库表的结构如下： 

```
CREATE TABLE `T_DATA_SOURCE` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `MODULE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '模块标识',
  `DATA_SOURCE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '数据源名称',
  `FULL_FLAG` bit(1) DEFAULT b'0' COMMENT '容量是否满标识 true：是，false：否',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '集群名称',
  `DS_URL` varchar(1024) DEFAULT NULL COMMENT '数据源URL地址',
  `TAG` varchar(128) DEFAULT NULL COMMENT '数据源标签',
  `TYPE` varchar(32) NOT NULL DEFAULT 'DB' COMMENT '数据库类型，DB:普通数据库，ARCHIVE_DB:归档数据库',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TDS_CLUSTER_MODULE_TYPE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TYPE`,`DATA_SOURCE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模块数据源配置';

sql示例:
INSERT INTO devops_ci_project.T_DATA_SOURCE
(ID, MODULE_CODE, DATA_SOURCE_NAME, FULL_FLAG, CREATOR, MODIFIER, UPDATE_TIME, CREATE_TIME, CLUSTER_NAME, DS_URL, TAG, `TYPE`)
VALUES('eae3670d3716427881c93fde46e28532', 'PROCESS', 'ds_0', 0, 'system', 'system', '2022-01-11 15:42:20.280', '2022-01-11 15:42:20.280', 'prod', 'xxxxx', NULL, 'DB');

INSERT INTO devops_ci_project.T_DATA_SOURCE
(ID, MODULE_CODE, DATA_SOURCE_NAME, FULL_FLAG, CREATOR, MODIFIER, UPDATE_TIME, CREATE_TIME, CLUSTER_NAME, DS_URL, TAG, `TYPE`)
VALUES('eae3670d3716427881c93fde46e28533', 'PROCESS', 'ds_1', 0, 'system', 'system', '2022-01-19 11:04:45.529', '2022-01-11 15:42:20.280', 'prod', 'xxxxx', NULL, 'DB');

INSERT INTO devops_ci_project.T_DATA_SOURCE
(ID, MODULE_CODE, DATA_SOURCE_NAME, FULL_FLAG, CREATOR, MODIFIER, UPDATE_TIME, CREATE_TIME, CLUSTER_NAME, DS_URL, TAG, `TYPE`)
VALUES('eae3670d3716427881c93fde46e26537', 'PROCESS', 'archive_ds_0', 0, 'system', 'system', '2024-01-03 18:31:10.459', '2024-01-03 18:31:10.459', 'stream', 'xxxxx', 'archive', 'ARCHIVE_DB');

CREATE TABLE `T_TABLE_SHARDING_CONFIG` (
  `ID` varchar(32) NOT NULL DEFAULT '' COMMENT '主键ID',
  `CLUSTER_NAME` varchar(64) NOT NULL DEFAULT '' COMMENT '集群名称',
  `MODULE_CODE` varchar(64) NOT NULL DEFAULT '' COMMENT '模块标识',
  `TABLE_NAME` varchar(128) NOT NULL DEFAULT '' COMMENT '数据库表名称',
  `SHARDING_NUM` int(11) NOT NULL DEFAULT '5' COMMENT '分表数量',
  `CREATOR` varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建者',
  `MODIFIER` varchar(50) NOT NULL DEFAULT 'system' COMMENT '修改者',
  `UPDATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '修改时间',
  `CREATE_TIME` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `UNI_INX_TTSC_CLUSTER_MODULE_NAME` (`CLUSTER_NAME`,`MODULE_CODE`,`TABLE_NAME`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库表分片配置';
```



#####   2、在蓝盾对应微服务（以process为例）的yml文件增加分库分表数据源配置，模板如下（**db的序号不能随便更改，后续如果新增分区库需要往下按序号追加**）： 

```
spring:
  datasource:
    # 普通库数据源配置（勿随便变更配置项的顺序）
    dataSourceConfigs:
      - index: 0
        url: jdbc:mysql://xxx01:10000/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true
        username: ENC(xxxxx)
        password: ENC(xxxxx)
      - index: 1
        url: jdbc:mysql://xxx02:10000/devops_ci_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true
        username: ENC(xxxxx)
        password: ENC(xxxxx)
    # 归档库数据源配置（勿随便变更配置项的顺序）     
   archiveDataSourceConfigs:
      - index: 0
        url: jdbc:mysql://archivexxx01:10000/devops_ci_archive_process?useSSL=false&autoReconnect=true&serverTimezone=GMT%2B8&useUnicode=true&characterEncoding=utf8&allowMultiQueries=true
        username: ENC(xxxxx)
        password: ENC(xxxxx)     
```



##### 3、在蓝盾对应微服务（以process为例）的yml文件增加公共分片规则配置

```
# shardingsphere分片规则配置
sharding:
  databaseShardingStrategy:
    algorithmClassName: "com.tencent.devops.process.sharding.BkProcessDatabaseShardingAlgorithm" # 普通业务库分库路由算法实现类
    archiveAlgorithmClassName: "com.tencent.devops.process.sharding.BkProcessArchiveDatabaseShardingAlgorithm" # 归档库分库路由算法实现类
    shardingField: PROJECT_ID # 分库分片键
  tableShardingStrategy:
    archiveAlgorithmClassName: "com.tencent.devops.process.sharding.BkProcessArchiveTableShardingAlgorithm" # 归档库分表路由算法实现类
    shardingField: PROJECT_ID # 分表分片键
  archiveFlag: Y  # 是否使用归档库标识；Y：是，N：否  
  defaultFlag: Y  # 是否使用默认库标识；Y：是，N：否
  routing:
    cacheSize: 100000  # 缓存分片规则最大数量
  migration:
    timeout: 2 # 迁移项目超时时间，单位：小时
    maxProjectCount: 5 # 同时迁移项目最大数量
    processDbMicroServices: "process,engine,misc,lambda" #使用process数据库的微服务
    sourceDbDataDeleteFlag: false # 迁移成功后是否删除原库数据
  tableShardingStrategy:
    defaultShardingNum: 1 # 默认分表数量
```



##### 4、在蓝盾对应微服务（以process为例）的yml文件增表的分片规则配置

```
spring:
  profiles: prod
  datasource:
    tableRuleConfigs:      
     - index: 0 # 序号
       name: T_AUDIT_RESOURCE # 表名
       databaseShardingStrategy: SHARDING # 分库策略
       tableShardingStrategy: SHARDING  # 分表策略（可为空，不分表的情况下可以不配）
       shardingNum: 1 # 分表数量（可为空，不分表的情况下可以不配）
     - index: 1  # 序号
       name: T_PIPELINE_RULE # 表名
       broadcastFlag: true # 是否为广播表（所有分区库中该表的数据都一样）
```



##### 5、分布式ID配置

   蓝盾数据库表的ID分为UUID(**全局唯一**)和数据库自增长主键ID(**非全局唯一**)，我们为数据库自增长主键ID的表开发了基于号段模式实现的分布式ID方案 ，该方案需在devops_ci_project数据库的T_LEAF_ALLOC表插入业务ID生成方案配置的记录  ，数据库表的结构如下： 

```
CREATE TABLE `T_LEAF_ALLOC` (
  `BIZ_TAG` varchar(128) NOT NULL DEFAULT '' COMMENT '业务标签',
  `MAX_ID` bigint(20) NOT NULL DEFAULT '1' COMMENT '当前最大ID值',
  `STEP` int(11) NOT NULL COMMENT '步长,每一次请求获取的ID个数',
  `DESCRIPTION` varchar(256) DEFAULT NULL COMMENT '说明',
  `UPDATE_TIME` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`BIZ_TAG`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ID管理数据表';
```



##### 6、按照分表数量新建表（如果只分库不分表可以忽略该步骤）

示例：T_ATOM_OVERVIEW_DATA表分表数量为2，需要新建T_ATOM_OVERVIEW_DATA（**供jooq生成数据模型，不存数据**）、T_ATOM_OVERVIEW_DATA_0、T_ATOM_OVERVIEW_DATA_2 这三张表。



#### 三、业务逻辑开发

1、涉及分区表的sql需加上分片键，保证sql路由到目标db或者表执行。

```
xxxxxx WHERE PROJECT_ID = "xxx"
```



2、如果业务要用数据库自增长主键ID需要 调取project服务封装的分布式ID生成接口获取ID,调用代码模板如下： 

```
client.get(ServiceAllocIdResource::class).generateSegmentId(TEMPLATE_BIZ_TAG_NAME).data
```



#### 四：路由分配规则二次开发（非必须）

在为项目分片路由规则的时候默认采用的是随机分配算法，如果想保证各分区库或者分区表的访问流量、数据量相对比较均衡，可以根据自身业务特点对项目的路由规则进行二次开发（列如可以计算每个分区库或者分区表一段时间的总构建量，新建项目的时候将总构建量相对较小的db或者表分配给该项目）

    /**
         * 获取可用数据源名称
         * @param clusterName db集群名称
         * @param moduleCode 模块代码
         * @param ruleType 规则类型
         * @param dataSourceNames 数据源名称集合
         * @return 可用数据源名称
         */
        override fun getValidDataSourceName(
            clusterName: String,
            moduleCode: SystemModuleEnum,
            ruleType: ShardingRuleTypeEnum,
            dataSourceNames: List<String>
        ): String {
            // 从可用的数据源中随机选择一个分配给该项目
            val maxSizeIndex = dataSourceNames.size - 1
            val randomIndex = (0..maxSizeIndex).random()
            return dataSourceNames[randomIndex]
        }
    
        /**
         * 获取可用数据库表名称
         * @param ruleType 规则类型
         * @param dataSourceName 数据源名称
         * @param tableShardingConfig 分表配置
         * @return 可用数据库表名称
         */
        override fun getValidTableName(
            ruleType: ShardingRuleTypeEnum,
            dataSourceName: String,
            tableShardingConfig: TableShardingConfig
        ): String {
            // 从可用的数据库表中随机选择一个分配给该项目
            val tableName = tableShardingConfig.tableName
            val shardingNum = tableShardingConfig.shardingNum
            return if (shardingNum > 1) {
                val maxSizeIndex = shardingNum - 1
                val randomIndex = (0..maxSizeIndex).random()
                "${tableName}_$randomIndex"
            } else {
                tableName
            }
        }
##### 

#### 五：数据迁移（非必须）

在原来db的数据量比较大的情况下，我们会增加新的db组成新的db集群以实现扩容，如果想把原db的部分数据迁移到db以减轻原db的压力可以使用蓝盾迁移项目数据的接口实现，具体的步骤如下：

##### 1、为新增的db配置指定tag

示例：

INSERT INTO devops_ci_project.T_DATA_SOURCE
(ID, MODULE_CODE, DATA_SOURCE_NAME, FULL_FLAG, CREATOR, MODIFIER, UPDATE_TIME, CREATE_TIME, CLUSTER_NAME, DS_URL, TAG, `TYPE`)
VALUES('eae3670d3716427881c93fde46e28501', 'PROCESS', 'ds_3', 0, 'system', 'system', '2023-08-29 15:44:40.694', '2023-08-29 15:44:40.694', 'prod', 'xxx', '**tagxxx**', 'DB');



##### 2、在misc服务调用蓝盾迁移项目数据的接口

```
curl -X PUT "http://xxx/api/op/process/db/projects/{projectId}/data/migrate?cancelFlag=true&dataTag=tagxxx" -H "accept: application/json" -H "X-DEVOPS-UID: xxx"
```

参数说明：

| 参数名称     | 参数类型 | 说明                   |
| ------------ | -------- | ---------------------- |
| X-DEVOPS-UID | header   | 迁移触发人             |
| projectId    | path     | 迁移项目               |
| cancelFlag   | query    | 是否取消正在运行的构建 |
| dataTag      | query    | 迁移库对应的数据标签   |



**注意：**

1、迁移项目前需要向项目的负责人明确迁移时是否要取消当前正在运行的构建（**不允许取消构建可能存在迁移后数据不一致的情况**），如果用户允许迁移接口的cancelFlag字段传true，不允许cancelFlag字段传false。

2、迁移失败后该项目的流量还是处于锁定状态，如果不继续迁移了，需要调用project微服务的项目流量解锁接口进行解锁，该接口调用示例如下：

```
curl -X PUT "http://xxx/api/op/projects/{projectId}/pipeline/build/unlock" -H "accept: application/json" -H "X-DEVOPS-UID: xxx"
```



##### 3、把新增的db配置的tag置为null

把新增的db配置的tag置为null后，则说明新增db已经正式加入默认集群，为新项目创建分片路由规则时也可能会将新增的db分配给该项目使用。

UPDATE devops_ci_project.T_DATA_SOURCE  SET  TAG = **null** WHERE ID = 'xxxxxx';

