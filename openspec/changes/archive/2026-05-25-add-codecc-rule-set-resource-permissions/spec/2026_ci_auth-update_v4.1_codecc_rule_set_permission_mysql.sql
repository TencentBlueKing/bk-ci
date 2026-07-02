-- ============================================================
-- CodeCC 规则集 (CodeCC Rule Set) 资源权限模型数据变更脚本
-- 数据库: devops_ci_auth
-- 说明: 补齐规则集 edit/manage 操作、资源级 owner 组和项目管理员兜底授权
-- ============================================================

USE devops_ci_auth;
SET NAMES utf8mb4;

-- ============================================================
-- 第一部分: 新增数据 (INSERT)
-- ============================================================

-- ============================================================
-- 1. 新增操作定义 T_AUTH_ACTION (2个操作)
-- ============================================================
REPLACE INTO T_AUTH_ACTION(
    `ACTION`, RESOURCE_TYPE, RELATED_RESOURCE_TYPE, ACTION_NAME,
    ENGLISH_NAME, CREATE_USER, `DELETE`, ACTION_TYPE
) VALUES
    ('codecc_rule_set_edit', 'codecc_rule_set', 'codecc_rule_set',
     '规则集编辑', 'CodeCC Rule Set Edit', 'system', 0, 'edit'),
    ('codecc_rule_set_manage', 'codecc_rule_set', 'codecc_rule_set',
     '规则集权限管理', 'CodeCC Rule Set Manage', 'system', 0, 'edit');

-- ============================================================
-- 2. 新增规则集资源级用户组配置 T_AUTH_RESOURCE_GROUP_CONFIG
-- ============================================================
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    75,
    'codecc_rule_set',
    'manager',
    '拥有者',
    0,
    0,
    '规则集拥有者，可以管理当前规则集的权限',
    '[{\"system\":\"#system#\",\"actions\":[{\"id\":\"project_visit\"},{\"id\":\"codecc_rule_set_list\"}],\"resources\":[{\"system\":\"#system#\",\"type\":\"project\",\"paths\":[[{\"system\":\"#system#\",\"type\":\"project\",\"id\":\"#projectId#\",\"name\":\"#projectName#\"}]]}]},{\"system\":\"#system#\",\"actions\":[{\"id\":\"codecc_rule_set_edit\"},{\"id\":\"codecc_rule_set_manage\"}],\"resources\":[{\"system\":\"#system#\",\"type\":\"codecc_rule_set\",\"paths\":[[{\"system\":\"#system#\",\"type\":\"project\",\"id\":\"#projectId#\",\"name\":\"#projectName#\"},{\"system\":\"#system#\",\"type\":\"codecc_rule_set\",\"id\":\"#resourceCode#\",\"name\":\"#resourceName#\"}]]}]}]',
    '["codecc_rule_set_edit","codecc_rule_set_manage"]'
);

-- ============================================================
-- 第二部分: 更新现有项目级用户组 (UPDATE)
-- ============================================================

-- ============================================================
-- 3. 更新项目管理员组 (ID=1) - 添加 codecc_rule_set edit + manage
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'codecc_rule_set_edit'),
            JSON_OBJECT('id', 'codecc_rule_set_manage')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'codecc_rule_set',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT(
                            'system', '#system#',
                            'type', 'project',
                            'id', '#projectId#',
                            'name', '#projectName#'
                        )
                    )
                )
            )
        )
    )
)
WHERE ID = 1;


-- ============================================================
-- 第三部分: 回调地址
-- ============================================================
INSERT INTO devops_auth.T_AUTH_IAM_CALLBACK
(ID, GATEWAY, `PATH`, DELETE_FLAG, RESOURCE, `SYSTEM`)
VALUES(19, 'gateway', '/ms/openapi/api/open/v2/callback/codecc_rule_set/instances/list',
       0, 'codecc_rule_set', 'codecc');

-- ============================================================
-- END OF SCRIPT
-- ============================================================
