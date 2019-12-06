USE devops_ci_store;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `T_ATOM_APPROVE_REL`
(
    `ID`                varchar(32) NOT NULL,
    `ATOM_CODE`         varchar(64) NOT NULL,
    `TEST_PROJECT_CODE` varchar(32) NOT NULL,
    `APPROVE_ID`        varchar(32) NOT NULL,
    `CREATOR`           varchar(50) NOT NULL DEFAULT 'system',
    `MODIFIER`          varchar(50) NOT NULL DEFAULT 'system',
    `CREATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME`       datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_taar_atom_code` (`ATOM_CODE`),
    KEY `inx_taar_approve_id` (`APPROVE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_ATOM_DEV_LANGUAGE_ENV_VAR`
(
    `ID`              varchar(32)  NOT NULL COMMENT '主键',
    `LANGUAGE`        varchar(64)  NOT NULL COMMENT '插件开发语言',
    `ENV_KEY`         varchar(64)  NOT NULL COMMENT '环境变量key值',
    `ENV_VALUE`       varchar(256) NOT NULL COMMENT '环境变量value值',
    `BUILD_HOST_TYPE` varchar(32)  NOT NULL COMMENT '适用构建机类型 PUBLIC:公共构建机，THIRD:第三方构建机，ALL:所有',
    `BUILD_HOST_OS`   varchar(32)  NOT NULL COMMENT '适用构建机操作系统 WINDOWS:windows构建机，LINUX:linux构建机，MAC_OS:mac构建机，ALL:所有',
    `CREATOR`         varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`        varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`     datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `inx_taev_language` (`LANGUAGE`),
    KEY `inx_taev_key` (`ENV_KEY`),
    KEY `inx_taev_host_type` (`BUILD_HOST_TYPE`),
    KEY `inx_taev_host_os` (`BUILD_HOST_OS`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='插件环境变量信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_OPT_LOG`
(
    `ID`          varchar(32)  NOT NULL DEFAULT '',
    `STORE_CODE`  varchar(64)  NOT NULL DEFAULT '',
    `STORE_TYPE`  tinyint(4)   NOT NULL DEFAULT '0',
    `OPT_TYPE`    varchar(64)  NOT NULL DEFAULT '',
    `OPT_DESC`    varchar(512) NOT NULL DEFAULT '',
    `OPT_USER`    varchar(64)  NOT NULL DEFAULT '',
    `OPT_TIME`    datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `store_item` (`STORE_CODE`, `STORE_TYPE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_STORE_APPROVE`
(
    `ID`          varchar(32) NOT NULL,
    `STORE_CODE`  varchar(64) NOT NULL,
    `STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0',
    `CONTENT`     text        NOT NULL,
    `APPLICANT`   varchar(50) NOT NULL,
    `TYPE`        varchar(64)          DEFAULT NULL,
    `STATUS`      varchar(64)          DEFAULT NULL,
    `APPROVER`    varchar(50)          DEFAULT '',
    `APPROVE_MSG` varchar(256)         DEFAULT '',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`ID`),
    KEY `inx_tsa_applicant` (`APPLICANT`),
    KEY `inx_tsa_type` (`TYPE`),
    KEY `inx_tsa_status` (`STATUS`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_STORE_RELEASE`
(
    `ID`                  varchar(32) NOT NULL COMMENT '主键',
    `STORE_CODE`          varchar(64) NOT NULL COMMENT 'store组件代码',
    `FIRST_PUB_CREATOR`   varchar(64) NOT NULL COMMENT '首次发布人',
    `FIRST_PUB_TIME`      datetime    NOT NULL COMMENT '首次发布时间',
    `LATEST_UPGRADER`     varchar(64) NOT NULL COMMENT '最近升级人',
    `LATEST_UPGRADE_TIME` datetime    NOT NULL COMMENT '最近升级时间',
    `STORE_TYPE`          tinyint(4)  NOT NULL DEFAULT '0' COMMENT 'store组件类型 0：插件 1：模板 2：镜像 3：IDE插件',
    `CREATOR`             varchar(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`            varchar(64) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`         datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    UNIQUE KEY `uni_inx_tsr_code_type` (`STORE_CODE`, `STORE_TYPE`),
    KEY `inx_tsr_f_pub_creater` (`FIRST_PUB_CREATOR`),
    KEY `inx_tsr_f_pub_time` (`FIRST_PUB_TIME`),
    KEY `inx_tsr_latest_upgrader` (`LATEST_UPGRADER`),
    KEY `inx_tsr_latest_upgrade_time` (`LATEST_UPGRADE_TIME`),
    KEY `inx_tsr_code` (`STORE_CODE`),
    KEY `inx_tsr_type` (`STORE_TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='store组件发布升级信息表';

CREATE TABLE IF NOT EXISTS `T_STORE_PIPELINE_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `STORE_CODE`  varchar(64) NOT NULL DEFAULT '' COMMENT '商店组件编码',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `STORE_TYPE`  tinyint(4)  NOT NULL DEFAULT '0' COMMENT '商店组件类型 0：插件 1：模板 2：镜像 3：IDE插件',
    PRIMARY KEY (`ID`),
    KEY `inx_tspr_code` (`STORE_CODE`),
    KEY `inx_tspr_type` (`STORE_TYPE`),
    KEY `inx_tspr_pipeline_id` (`PIPELINE_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商店组件与与流水线关联关系表';

CREATE TABLE IF NOT EXISTS `T_STORE_PIPELINE_BUILD_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `STORE_ID`    varchar(32) NOT NULL DEFAULT '' COMMENT '商店组件ID',
    `PIPELINE_ID` varchar(34) NOT NULL COMMENT '流水线ID',
    `BUILD_ID`    varchar(34) NOT NULL COMMENT '构建ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`),
    KEY `inx_tspbr_atom_id` (`STORE_ID`),
    KEY `inx_tspbr_pipeline_id` (`PIPELINE_ID`),
    KEY `inx_tspbr_build_id` (`BUILD_ID`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='商店组件构建关联关系表';

CREATE TABLE IF NOT EXISTS `T_LOGO`
(
    `ID`          varchar(32)  NOT NULL,
    `TYPE`        varchar(16)  NOT NULL,
    `LOGO_URL`    varchar(512) NOT NULL DEFAULT '',
    `LINK`        varchar(512)          DEFAULT '',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system',
    `MODIFIER`    varchar(50)  NOT NULL DEFAULT 'system',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `UPDATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ORDER`       int(8)       NOT NULL DEFAULT '0',
    PRIMARY KEY (`ID`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS `T_REASON`
(
    `ID`          varchar(32)  NOT NULL COMMENT '主键',
    `TYPE`        varchar(32)  NOT NULL COMMENT '类型',
    `CONTENT`     varchar(512) NOT NULL DEFAULT '',
    `CREATOR`     varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `ENABLE`      bit(1)       NOT NULL DEFAULT b'1' COMMENT '是否启用',
    `ORDER`       int(8)       NOT NULL DEFAULT '0' COMMENT '显示顺序',
    PRIMARY KEY (`ID`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='原因定义表';

CREATE TABLE IF NOT EXISTS `T_REASON_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `TYPE`        varchar(32) NOT NULL COMMENT '类型',
    `STORE_CODE`  varchar(64) NOT NULL DEFAULT '' COMMENT '商城组件编码',
    `REASON_ID`   varchar(32) NOT NULL COMMENT '原因ID',
    `NOTE`        text COMMENT '原因说明',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`ID`),
    KEY `store_code` (`STORE_CODE`),
    KEY `type` (`TYPE`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT='原因和组件关联关系';

CREATE TABLE IF NOT EXISTS `T_IMAGE`
(
    `ID`                varchar(32)  NOT NULL COMMENT '主键',
    `IMAGE_NAME`        varchar(64)  NOT NULL COMMENT '镜像名称',
    `IMAGE_CODE`        varchar(64)  NOT NULL COMMENT '镜像代码',
    `CLASSIFY_ID`       varchar(32)  NOT NULL COMMENT '所属分类ID',
    `VERSION`           varchar(20)  NOT NULL COMMENT '版本号',
    `IMAGE_SOURCE_TYPE` varchar(20)  NOT NULL DEFAULT 'bkdevops' COMMENT '镜像来源，bkdevops：蓝盾源 third：第三方源',
    `IMAGE_REPO_URL`    varchar(256)          DEFAULT NULL COMMENT '镜像仓库地址',
    `IMAGE_REPO_NAME`   varchar(256) NOT NULL COMMENT '镜像在仓库名称',
    `TICKET_ID`         varchar(256)          DEFAULT NULL COMMENT 'ticket身份ID',
    `IMAGE_STATUS`      tinyint(4)   NOT NULL COMMENT '镜像状态，0：初始化|1：提交中|2：验证中|3：验证失败|4：测试中|5：审核中|6：审核驳回|7：已发布|8：上架中止|9：下架中|10：已下架',
    `IMAGE_STATUS_MSG`  varchar(1024)         DEFAULT NULL COMMENT '状态对应的描述，如上架失败原因',
    `IMAGE_SIZE`        varchar(20)  NOT NULL COMMENT '镜像大小',
    `IMAGE_TAG`         varchar(256)          DEFAULT NULL COMMENT '镜像tag',
    `LOGO_URL`          varchar(256)          DEFAULT NULL COMMENT 'logo地址',
    `ICON`              text COMMENT '镜像图标(BASE64字符串)',
    `SUMMARY`           varchar(256)          DEFAULT NULL COMMENT '镜像简介',
    `DESCRIPTION`       text COMMENT '镜像描述',
    `PUBLISHER`         varchar(50)  NOT NULL DEFAULT 'system' COMMENT '镜像发布者',
    `PUB_TIME`          datetime              DEFAULT NULL COMMENT '发布时间',
    `LATEST_FLAG`       bit(1)       NOT NULL COMMENT '是否为最新版本镜像， TRUE：最新 FALSE：非最新',
    `CREATOR`           varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`          varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`       datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `AGENT_TYPE_SCOPE`  varchar(64)  NOT NULL DEFAULT '[]' COMMENT '支持的构建机环境',
    `DELETE_FLAG`       BIT(1)       DEFAULT FALSE  COMMENT '删除标识 true：是，false：否',
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `uni_inx_ti_code_version` (`IMAGE_CODE`, `VERSION`) USING BTREE,
    KEY `inx_ti_image_name` (`IMAGE_NAME`) USING BTREE,
    KEY `inx_ti_image_code` (`IMAGE_CODE`) USING BTREE,
    KEY `inx_ti_source_type` (`IMAGE_SOURCE_TYPE`) USING BTREE,
    KEY `inx_ti_image_status` (`IMAGE_STATUS`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  ROW_FORMAT = DYNAMIC COMMENT ='镜像信息表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_CATEGORY_REL`
(
    `ID`          varchar(32) NOT NULL DEFAULT '' COMMENT '主键',
    `CATEGORY_ID` varchar(32) NOT NULL COMMENT '镜像范畴ID',
    `IMAGE_ID`    varchar(32) NOT NULL COMMENT '镜像ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_ticr_category_id` (`CATEGORY_ID`) USING BTREE,
    KEY `inx_ticr_image_id` (`IMAGE_ID`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像与范畴关联关系表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_FEATURE`
(
    `ID`                 varchar(32)  NOT NULL COMMENT '主键',
    `IMAGE_CODE`         varchar(256) NOT NULL COMMENT '镜像代码',
    `PUBLIC_FLAG`        bit(1)                DEFAULT b'0' COMMENT '是否为公共镜像， TRUE：是 FALSE：不是',
    `RECOMMEND_FLAG`     bit(1)                DEFAULT b'1' COMMENT '是否推荐， TRUE：是 FALSE：不是',
    `CREATOR`            varchar(50)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`           varchar(50)  NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`        datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `CERTIFICATION_FLAG` bit(1)                DEFAULT b'0' COMMENT '是否官方认证， TRUE：是 FALSE：不是',
    `WEIGHT`             int(11)               DEFAULT NULL COMMENT '权重（数值越大代表权重越高）',
    `IMAGE_TYPE`         tinyint(4)   NOT NULL DEFAULT '1' COMMENT '镜像类型：0：蓝鲸官方，1：第三方',
    `DELETE_FLAG`        BIT(1)       DEFAULT FALSE  COMMENT '删除标识 true：是，false：否',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_tif_image_code` (`IMAGE_CODE`) USING BTREE,
    KEY `inx_tif_public_flag` (`PUBLIC_FLAG`) USING BTREE,
    KEY `inx_tif_recommend_flag` (`RECOMMEND_FLAG`) USING BTREE,
    KEY `inx_tif_certification_flag` (`CERTIFICATION_FLAG`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像特性表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_LABEL_REL`
(
    `ID`          varchar(32) NOT NULL COMMENT '主键',
    `LABEL_ID`    varchar(32) NOT NULL COMMENT '模板标签ID',
    `IMAGE_ID`    varchar(32) NOT NULL COMMENT '镜像ID',
    `CREATOR`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`    varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME` datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    KEY `inx_tilr_label_id` (`LABEL_ID`) USING BTREE,
    KEY `inx_tilr_image_id` (`IMAGE_ID`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像与标签关联关系表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_VERSION_LOG`
(
    `ID`           varchar(32) NOT NULL COMMENT '主键',
    `IMAGE_ID`     varchar(32) NOT NULL COMMENT '镜像ID',
    `RELEASE_TYPE` tinyint(4)  NOT NULL COMMENT '发布类型，0：新上架 1：非兼容性升级 2：兼容性功能更新 3：兼容性问题修正',
    `CONTENT`      text        NOT NULL COMMENT '版本日志内容',
    `CREATOR`      varchar(50) NOT NULL DEFAULT 'system' COMMENT '创建人',
    `MODIFIER`     varchar(50) NOT NULL DEFAULT 'system' COMMENT '最近修改人',
    `CREATE_TIME`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `UPDATE_TIME`  datetime    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`ID`) USING BTREE,
    UNIQUE KEY `uni_inx_tivl_image_id` (`IMAGE_ID`) USING BTREE,
    KEY `inx_tivl_release_type` (`RELEASE_TYPE`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='镜像版本日志表';

CREATE TABLE IF NOT EXISTS `T_IMAGE_AGENT_TYPE` (
  `ID` varchar(32) NOT NULL COMMENT '主键',
  `IMAGE_CODE` varchar(32) NOT NULL COMMENT '镜像代码',
  `AGENT_TYPE` varchar(32) NOT NULL COMMENT '机器类型 PUBLIC_DEVNET，PUBLIC_IDC，PUBLIC_DEVCLOUD',
  PRIMARY KEY (`ID`) USING BTREE,
  KEY `inx_tiat_agent_type` (`AGENT_TYPE`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT = '镜像与机器类型关联表';

CREATE TABLE IF NOT EXISTS `T_BUSINESS_CONFIG` (
  `BUSINESS` varchar(64) NOT NULL COMMENT '业务名称',
  `FEATURE` varchar(64) NOT NULL COMMENT '要控制的功能特性',
  `BUSINESS_VALUE` varchar(255) NOT NULL COMMENT '业务取值',
  `CONFIG_VALUE` text NOT NULL COMMENT '配置值',
  `DESCRIPTION` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`ID`) USING BTREE,
  UNIQUE KEY `i_feature_business_businessValue` (`FEATURE`,`BUSINESS`,`BUSINESS_VALUE`) USING BTREE COMMENT '同一业务相同业务值的相同特性仅能有一个配置'
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS ci_store_schema_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_store_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'PUB_TIME') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `PUB_TIME` datetime DEFAULT NULL AFTER `VISIBILITY_LEVEL`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'PRIVATE_REASON') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `PRIVATE_REASON` varchar(256) DEFAULT NULL AFTER `PUB_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' AFTER `PRIVATE_REASON`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM'
                    AND INDEX_NAME = 'inx_ta_delete_flag') THEN
        ALTER TABLE T_ATOM
            ADD INDEX `inx_ta_delete_flag` (`DELETE_FLAG`);
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_BUILD_INFO'
                    AND COLUMN_NAME = 'SAMPLE_PROJECT_PATH') THEN
        ALTER TABLE T_ATOM_BUILD_INFO
            ADD COLUMN `SAMPLE_PROJECT_PATH` varchar(500) NOT NULL DEFAULT '' AFTER `REPOSITORY_PATH`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_BUILD_INFO'
                    AND COLUMN_NAME = 'ENABLE') THEN
        ALTER TABLE T_ATOM_BUILD_INFO
            ADD COLUMN `ENABLE` bit(1) NOT NULL DEFAULT b'1' AFTER `SAMPLE_PROJECT_PATH`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'RECOMMEND_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `RECOMMEND_FLAG` bit(1) DEFAULT b'1' AFTER `UPDATE_TIME`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'PRIVATE_REASON') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `PRIVATE_REASON` varchar(256) DEFAULT NULL AFTER `RECOMMEND_FLAG`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_ATOM_FEATURE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' AFTER `PRIVATE_REASON`;
    END IF;


    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_TEMPLATE'
                    AND COLUMN_NAME = 'PUB_TIME') THEN
        ALTER TABLE T_TEMPLATE
            ADD COLUMN `PUB_TIME` datetime DEFAULT NULL COMMENT '发布时间' AFTER `UPDATE_TIME`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' AFTER `PRIVATE_REASON`;
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'T_IMAGE_FEATURE'
                    AND COLUMN_NAME = 'DELETE_FLAG') THEN
        ALTER TABLE T_ATOM_FEATURE
            ADD COLUMN `DELETE_FLAG` bit(1) DEFAULT b'0' AFTER `PRIVATE_REASON`;
    END IF;


    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
CALL ci_store_schema_update();