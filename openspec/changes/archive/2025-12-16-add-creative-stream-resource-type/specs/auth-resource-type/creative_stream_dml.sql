-- ============================================================
-- 创作流 (Creative Stream) 资源类型数据变更脚本
-- 数据库: devops_auth
-- 日期: 2025-12-16
-- 说明: 新增创作流资源类型及相关权限配置
-- ============================================================

USE devops_auth;
SET NAMES utf8mb4;

-- ============================================================
-- 第一部分: 新增数据 (INSERT)
-- ============================================================

-- ============================================================
-- 1. 新增资源类型 T_AUTH_RESOURCE_TYPE
-- ============================================================
-- 原有数据: ID 1-21 已被使用，新增使用 ID=22
REPLACE INTO T_AUTH_RESOURCE_TYPE (
    `ID`, RESOURCE_TYPE, NAME, ENGLISH_NAME, `DESC`, ENGLISH_DESC,
    PARENT, `SYSTEM`, CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME, `DELETE`
) VALUES (
    22,
    'creative_stream',
    '创作流',
    'Creative Stream',
    '创作流',
    'Creative Stream',
    'project',
    'bk_ci_rbac',
    'system',
    NOW(),
    'system',
    NOW(),
    0
);

-- ============================================================
-- 2. 新增操作定义 T_AUTH_ACTION (10个操作，与 pipeline 一致)
-- ============================================================
REPLACE INTO T_AUTH_ACTION(
    `ACTION`, RESOURCE_TYPE, RELATED_RESOURCE_TYPE, ACTION_NAME,
    ENGLISH_NAME, CREATE_USER, CREATE_TIME, UPDATE_TIME, `DELETE`, ACTION_TYPE
) VALUES
    -- 查看操作
    ('creative_stream_view', 'creative_stream', 'creative_stream',
     '查看创作流', 'Creative Stream View', 'system', NOW(), NOW(), 0, 'view'),

    -- 编辑操作
    ('creative_stream_edit', 'creative_stream', 'creative_stream',
     '编辑创作流', 'Creative Stream Edit', 'system', NOW(), NOW(), 0, 'edit'),

    -- 删除操作
    ('creative_stream_delete', 'creative_stream', 'creative_stream',
     '删除创作流', 'Creative Stream Delete', 'system', NOW(), NOW(), 0, 'delete'),

    -- 创建操作 (关联 project)
    ('creative_stream_create', 'creative_stream', 'project',
     '创建创作流', 'Creative Stream Create', 'system', NOW(), NOW(), 0, 'create'),

    -- 执行操作
    ('creative_stream_execute', 'creative_stream', 'creative_stream',
     '执行创作流', 'Creative Stream Execute', 'system', NOW(), NOW(), 0, 'execute'),

    -- 下载操作
    ('creative_stream_download', 'creative_stream', 'creative_stream',
     '下载制品', 'Creative Stream Download', 'system', NOW(), NOW(), 0, 'execute'),

    -- 分享操作
    ('creative_stream_share', 'creative_stream', 'creative_stream',
     '分享制品', 'Creative Stream Share', 'system', NOW(), NOW(), 0, 'execute'),

    -- 列表操作
    ('creative_stream_list', 'creative_stream', 'creative_stream',
     '创作流列表', 'Creative Stream List', 'system', NOW(), NOW(), 0, 'view'),

    -- 管理操作
    ('creative_stream_manage', 'creative_stream', 'creative_stream',
     '创作流权限管理', 'Creative Stream Manage', 'system', NOW(), NOW(), 0, 'edit'),

    -- 归档操作
    ('creative_stream_archive', 'creative_stream', 'creative_stream',
     '归档创作流', 'Creative Stream Archive', 'system', NOW(), NOW(), 0, 'edit');

-- ============================================================
-- 3. 新增创作流资源级用户组配置 T_AUTH_RESOURCE_GROUP_CONFIG
-- =====================================a=======================
-- 原有数据: ID 1-69 已被使用，新增使用 ID=70-73

-- 3.1 创作流 Manager 组 (ID=70)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    70,
    'creative_stream',
    'manager',
    '拥有者',
    0,
    0,
    '创作流拥有者，可以管理当前创作流的权限',
    '[{\"system\":\"#system#\",\"actions\":[{\"id\":\"project_visit\"}],\"resources\":[{\"system\":\"#system#\",\"type\":\"project\",\"paths\":[[{\"system\":\"#system#\",\"type\":\"project\",\"id\":\"#projectId#\",\"name\":\"#projectName#\"}]]}]},{\"system\":\"#system#\",\"actions\":[{\"id\":\"creative_stream_list\"},{\"id\":\"creative_stream_view\"},{\"id\":\"creative_stream_edit\"},{\"id\":\"creative_stream_delete\"},{\"id\":\"creative_stream_execute\"},{\"id\":\"creative_stream_download\"},{\"id\":\"creative_stream_share\"},{\"id\":\"creative_stream_manage\"},{\"id\":\"creative_stream_archive\"}],\"resources\":[{\"system\":\"#system#\",\"type\":\"creative_stream\",\"paths\":[[{\"system\":\"#system#\",\"type\":\"project\",\"id\":\"#projectId#\",\"name\":\"#projectName#\"},{\"system\":\"#system#\",\"type\":\"creative_stream\",\"id\":\"#resourceCode#\",\"name\":\"#resourceName#\"}]]}]}]',
    '["creative_stream_view","creative_stream_edit","creative_stream_delete","creative_stream_execute","creative_stream_list","creative_stream_download","creative_stream_share","creative_stream_manage","creative_stream_archive"]'
);

-- 3.2 创作流 Editor 组 (ID=71)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    71,
    'creative_stream',
    'editor',
    '编辑者',
    0,
    0,
    '创作流编辑者，拥有当前创作流除了权限管理之外的所有权限',
    '[{"system":"#system#","actions":[{"id":"project_visit"}],"resources":[{"system":"#system#","type":"project","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"}]]}]},{"system":"#system#","actions":[{"id":"creative_stream_view"},{"id":"creative_stream_edit"},{"id":"creative_stream_execute"},{"id":"creative_stream_list"},{"id":"creative_stream_download"},{"id":"creative_stream_share"}],"resources":[{"system":"#system#","type":"creative_stream","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"},{"system":"#system#","type":"creative_stream","id":"#resourceCode#","name":"#resourceName#"}]]}]}]',
    '["creative_stream_view","creative_stream_edit","creative_stream_execute","creative_stream_list","creative_stream_download","creative_stream_share"]'
);

-- 3.3 创作流 Executor 组 (ID=72)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    72,
    'creative_stream',
    'executor',
    '执行者',
    0,
    0,
    '创作流执行者，可以查看和执行创作流，下载或分享制品',
    '[{"system":"#system#","actions":[{"id":"project_visit"}],"resources":[{"system":"#system#","type":"project","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"}]]}]},{"system":"#system#","actions":[{"id":"creative_stream_view"},{"id":"creative_stream_execute"},{"id":"creative_stream_list"},{"id":"creative_stream_download"},{"id":"creative_stream_share"}],"resources":[{"system":"#system#","type":"creative_stream","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"},{"system":"#system#","type":"creative_stream","id":"#resourceCode#","name":"#resourceName#"}]]}]}]',
    '["creative_stream_view","creative_stream_execute","creative_stream_list","creative_stream_download","creative_stream_share"]'
);

-- 3.4 创作流 Viewer 组 (ID=73)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    73,
    'creative_stream',
    'viewer',
    '查看者',
    0,
    0,
    '创作流查看者，可以查看创作流，下载或分享制品',
    '[{"system":"#system#","actions":[{"id":"project_visit"}],"resources":[{"system":"#system#","type":"project","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"}]]}]},{"system":"#system#","actions":[{"id":"creative_stream_view"},{"id":"creative_stream_list"},{"id":"creative_stream_download"},{"id":"creative_stream_share"}],"resources":[{"system":"#system#","type":"creative_stream","paths":[[{"system":"#system#","type":"project","id":"#projectId#","name":"#projectName#"},{"system":"#system#","type":"creative_stream","id":"#resourceCode#","name":"#resourceName#"}]]}]}]',
    '["creative_stream_view","creative_stream_list","creative_stream_download","creative_stream_share"]'
);

-- ============================================================
-- 第二部分: 更新现有项目级用户组 (UPDATE)
-- 为 manager/developer/maintainer/tester 添加 creative_stream_create 权限
-- ============================================================

-- ============================================================
-- 5. 更新项目管理员组 (ID=1) - 更新 AUTHORIZATION_SCOPES 添加创作流资源的全部权限
-- ============================================================
-- 5.1 在 project 权限块中追加 creative_stream_create (因为 create 操作关联 project)
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_SET(
    AUTHORIZATION_SCOPES,
    '$[0].actions',
    JSON_ARRAY_APPEND(
        JSON_EXTRACT(AUTHORIZATION_SCOPES, '$[0].actions'),
        '$',
        JSON_OBJECT('id', 'creative_stream_create')
    )
)
WHERE ID = 1;

-- 5.2 新增 creative_stream 资源类型权限块
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_view'),
            JSON_OBJECT('id', 'creative_stream_edit'),
            JSON_OBJECT('id', 'creative_stream_delete'),
            JSON_OBJECT('id', 'creative_stream_execute'),
            JSON_OBJECT('id', 'creative_stream_list'),
            JSON_OBJECT('id', 'creative_stream_download'),
            JSON_OBJECT('id', 'creative_stream_share'),
            JSON_OBJECT('id', 'creative_stream_manage'),
            JSON_OBJECT('id', 'creative_stream_archive')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 1;

-- ============================================================
-- 6. 更新开发人员组 (ID=2) -- 更新 AUTHORIZATION_SCOPES 添加创作流列表权限
-- ============================================================
-- 6.1 在 project 权限块中追加 creative_stream_create
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_SET(
        AUTHORIZATION_SCOPES,
        '$[0].actions',
        JSON_ARRAY_APPEND(
                JSON_EXTRACT(AUTHORIZATION_SCOPES, '$[0].actions'),
                '$',
                JSON_OBJECT('id', 'creative_stream_create')
        )
)
WHERE ID = 2;

-- 6.2 新增 creative_stream 资源类型权限块
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list'),
            JSON_OBJECT('id', 'creative_stream_download'),
            JSON_OBJECT('id', 'creative_stream_share')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 2;

-- ============================================================
-- 7. 更新运维人员组 (ID=3) - 更新 AUTHORIZATION_SCOPES 添加创作流列表权限
-- ============================================================
-- 7.1 在 project 权限块中追加 creative_stream_create
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_SET(
    AUTHORIZATION_SCOPES,
    '$[0].actions',
    JSON_ARRAY_APPEND(
        JSON_EXTRACT(AUTHORIZATION_SCOPES, '$[0].actions'),
        '$',
        JSON_OBJECT('id', 'creative_stream_create')
    )
)
WHERE ID = 3;

-- 7.2 新增 creative_stream 资源类型权限块
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list'),
            JSON_OBJECT('id', 'creative_stream_download'),
            JSON_OBJECT('id', 'creative_stream_share')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 3;

-- ============================================================
-- 8. 更新测试人员组 (ID=5) - 更新 AUTHORIZATION_SCOPES 添加创作流列表权限
-- ============================================================
-- 8.1 在 project 权限块中追加 creative_stream_create
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_SET(
    AUTHORIZATION_SCOPES,
    '$[0].actions',
    JSON_ARRAY_APPEND(
        JSON_EXTRACT(AUTHORIZATION_SCOPES, '$[0].actions'),
        '$',
        JSON_OBJECT('id', 'creative_stream_create')
    )
)
WHERE ID = 5;

-- 8.2 新增 creative_stream 资源类型权限块
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list'),
            JSON_OBJECT('id', 'creative_stream_download'),
            JSON_OBJECT('id', 'creative_stream_share')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 5;

-- ============================================================
-- 9. 更新产品人员组 (ID=4) - 更新 AUTHORIZATION_SCOPES 添加创作流列表权限
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 4;

-- ============================================================
-- 10. 更新质管人员组 (ID=6) - 更新 AUTHORIZATION_SCOPES 添加创作流列表权限
-- ============================================================
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 6;

-- ============================================================
-- END OF SCRIPT
-- ============================================================
