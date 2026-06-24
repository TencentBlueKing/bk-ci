-- ============================================================
-- 创作流节点 (Creative Stream Node) 资源类型数据变更脚本
-- 数据库: devops_auth
-- 说明: 新增创作流节点资源类型及相关权限配置
-- ============================================================

USE devops_auth;
SET NAMES utf8mb4;

-- ============================================================
-- 第一部分: 新增数据 (INSERT)
-- ============================================================

-- ============================================================
-- 1. 新增资源类型 T_AUTH_RESOURCE_TYPE
-- ============================================================
REPLACE INTO T_AUTH_RESOURCE_TYPE (
    `ID`, RESOURCE_TYPE, NAME, ENGLISH_NAME, `DESC`, ENGLISH_DESC,
    PARENT, `SYSTEM`, CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME, `DELETE`
) VALUES (
    23,
    'creative_stream_node',
    '创作流节点',
    'Creative Stream Node',
    '创作流节点',
    'Creative Stream Node',
    'project',
    'bk_ci_rbac',
    'system',
    NOW(),
    'system',
    NOW(),
    0
);

-- ============================================================
-- 2. 新增操作定义 T_AUTH_ACTION (2个操作)
-- ============================================================
REPLACE INTO T_AUTH_ACTION(
    `ACTION`, RESOURCE_TYPE, RELATED_RESOURCE_TYPE, ACTION_NAME,
    ENGLISH_NAME, CREATE_USER, CREATE_TIME, UPDATE_TIME, `DELETE`, ACTION_TYPE
) VALUES
    ('creative_stream_node_view', 'creative_stream_node', 'creative_stream_node',
     '查看创作流节点', 'Creative Stream Node View', 'system', NOW(), NOW(), 0, 'view'),

    ('creative_stream_node_edit', 'creative_stream_node', 'creative_stream_node',
     '编辑创作流节点', 'Creative Stream Node Edit', 'system', NOW(), NOW(), 0, 'edit');

-- ============================================================
-- 3. 新增创作流节点资源级用户组配置 T_AUTH_RESOURCE_GROUP_CONFIG
-- ============================================================

-- 3.1 创作流节点 Manager 组 (ID=74)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    74,
    'creative_stream_node',
    'manager',
    '拥有者',
    0,
    0,
    '创作流节点拥有者，可以管理当前创作流节点的权限',
    '[{"system":"#system#","actions":[{"id":"project_visit"}],"resources":[{"system":"#system#","type":"project","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"}]]}]},{"system":"#system#","actions":[{"id":"creative_stream_node_view"},{"id":"creative_stream_node_edit"}],"resources":[{"system":"#system#","type":"creative_stream_node","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"},{"system":"#system#","type":"creative_stream_node","id":"#resourceCode#","name":"#resourceName#"}]]}]}]',
    '["creative_stream_node_view","creative_stream_node_edit"]'
);

-- ============================================================
-- 第二部分: 更新现有项目级用户组 (UPDATE)
-- ============================================================

-- ============================================================
-- 4. 更新项目管理员组 (ID=1) - 添加 creative_stream_node view + edit
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_node_view'),
            JSON_OBJECT('id', 'creative_stream_node_edit')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream_node',
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
-- 5. 更新开发人员组 (ID=2) - 添加 creative_stream_node view
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_node_view')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream_node',
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
WHERE ID = 2;

-- ============================================================
-- 6. 更新运维人员组 (ID=3) - 添加 creative_stream_node view
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_node_view')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream_node',
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
WHERE ID = 3;

-- ============================================================
-- END OF SCRIPT
-- ============================================================
