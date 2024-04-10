USE devops_ci_dispatch_windows;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

create table T_BUILD_HISTORY
(
    ID            bigint auto_increment
        primary key,
    PROJECT_ID    varchar(128) not null,
    BUILD_ID      varchar(128) not null,
    VM_SEQ_ID     varchar(128) not null,
    VM_IP         varchar(64)  not null,
    START_TIME    datetime     null,
    END_TIME      datetime     null,
    STATUS        varchar(64)  null,
    PIPELINE_ID   varchar(128) null,
    RESOURCE_TYPE varchar(64)  null,
    EXECUTE_COUNT int(64)      null,
    TASK_GUID     char(128)    not null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table T_VIRTUAL_MACHINE_TYPE
(
    ID             int auto_increment
        primary key,
    NAME           varchar(255) not null,
    SYSTEM_VERSION varchar(255) not null,
    CREATE_TIME    datetime     null,
    UPDATE_TIME    datetime     null,
    DISPLAY        bit          null
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;
