USE devops_ci_project;
SET NAMES utf8mb4;

-- 示例项目和构建机导入

DROP PROCEDURE IF EXISTS ci_demo_date_update;

DELIMITER <CI_UBF>

CREATE PROCEDURE ci_demo_date_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    -- create demo project
    IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = db AND TABLE_NAME = 't_project') THEN
        IF NOT EXISTS(SELECT 1 FROM t_project WHERE english_name = 'demo') THEN
            insert into t_project
            (id, created_at, project_id, project_name, english_name, project_type, approval_status)
            values (1, sysdate(), '84a83354d18143f1be9f86b64c967520', 'Demo', 'demo', 5, 2);
        END IF;
    END IF;

    IF EXISTS(SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = db AND TABLE_NAME = 'T_PROJECT') THEN
        IF NOT EXISTS(SELECT 1 FROM T_PROJECT WHERE english_name = 'demo') THEN
            insert into T_PROJECT
            (id, created_at, project_id, project_name, english_name, project_type, approval_status)
            values (1, sysdate(), '84a83354d18143f1be9f86b64c967520', 'Demo', 'demo', 5, 2);
        END IF;
    END IF;

    -- insert agent
    IF NOT EXISTS(SELECT 1 FROM devops_ci_environment.T_ENVIRONMENT_THIRDPARTY_AGENT WHERE ID = 1) THEN
        insert into devops_ci_environment.T_ENVIRONMENT_THIRDPARTY_AGENT
        (ID, PROJECT_ID, NODE_ID, OS, STATUS, SECRET_KEY, CREATED_USER, CREATED_TIME, GATEWAY)
        values (1, 'demo', 1, 'LINUX', 2, 'lWMvndEraCPXWsXnWRp6xUDlH5IhWfxWuGJRs/IcMIM', 'admin', sysdate(),
                'http://devops.bk.cloud.tencent.com');
    END IF;

    -- insert node
    IF NOT EXISTS(SELECT 1 FROM devops_ci_environment.T_NODE WHERE NODE_ID = 1) THEN
        insert into devops_ci_environment.T_NODE(NODE_ID, NODE_STRING_ID, PROJECT_ID, NODE_IP, NODE_NAME,
                                                 NODE_STATUS, NODE_TYPE, CREATED_USER, CREATED_TIME, OS_NAME,
                                                 DISPLAY_NAME, LAST_MODIFY_TIME, LAST_MODIFY_USER)
        values (1, 'demo_build_machine', 'demo', '127.0.0.1', 'demo',
                'NORMAL', 'THIRDPARTY', 'admin', sysdate(), 'linux',
                'demo_build_machine', sysdate(), 'admin');
    END IF;

    COMMIT;
END <CI_UBF>
DELIMITER ;
COMMIT;
# CALL ci_demo_date_update();

