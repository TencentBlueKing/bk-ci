
export const resourceMap = {
    code: 'CODE_REPERTORY',
    pipeline: 'PIPELINE_DEFAULT',
    credential: 'TICKET_CREDENTIAL',
    cert: 'TICKET_CERT',
    environment: 'ENVIRONMENT_ENVIRONMENT',
    envNode: 'ENVIRONMENT_ENV_NODE',
    project: 'PROJECT',
    rule: 'QUALITY_RULE',
    ruleGroup: 'QUALITY_GROUP',
    experience: 'EXPERIENCE',
    experienceGroup: 'EXPERIENCE_GROUP',
    notifyGroup: 'NOTIFY_GROUP',
    artifactory: 'ARTIFACTORY'
}

export const resourceTypeMap = {
    CODE_REPERTORY: 'repertory',
    PIPELINE_DEFAULT: 'pipeline',
    TICKET_CREDENTIAL: 'credential',
    TICKET_CERT: 'cert',
    ENVIRONMENT_ENVIRONMENT: 'environment',
    ENVIRONMENT_ENV_NODE: 'env_node',
    PROJECT: 'project',
    QUALITY_RULE: 'rule',
    QUALITY_GROUP: 'group',
    EXPERIENCE: 'experience',
    EXPERIENCE_GROUP: 'experienceGroup',
    NOTIFY_GROUP: 'notifyGroup',
    ARTIFACTORY: 'artifactory'
}

export const resourceAliasMap = {
    CODE_REPERTORY: '代码库',
    PIPELINE_DEFAULT: '流水线',
    TICKET_CREDENTIAL: '凭据',
    TICKET_CERT: '证书',
    ENVIRONMENT_ENVIRONMENT: '环境',
    ENVIRONMENT_ENV_NODE: '节点',
    PROJECT: '项目',
    QUALITY_RULE: '质量规则',
    QUALITY_GROUP: '规则集',
    EXPERIENCE: '版本体验',
    EXPERIENCE_GROUP: '体验组',
    NOTIFY_GROUP: '通知组',
    ARTIFACTORY: '版本仓库'
}

export function isProjectResource (resourceId) {
    return resourceId === 'PROJECT'
}

export const actionMap = {
    create: 'CREATE',
    deploy: 'DEPLOY',
    download: 'DOWNLOAD',
    edit: 'EDIT',
    delete: 'DELETE',
    view: 'VIEW',
    move: 'MOVE',
    use: 'USE',
    share: 'SHARE',
    list: 'LIST',
    execute: 'EXECUTE',
    enable: 'ENABLE',
    manage: 'MANAGE'
}

export const actionAliasMap = {
    CREATE: {
        value: 'create',
        alias: '创建'
    },
    DEPLOY: {
        value: 'deploy',
        alias: '部署'
    },
    DOWNLOAD: {
        value: 'download',
        alias: '下载'
    },
    EDIT: {
        value: 'edit',
        alias: '编辑'
    },
    DELETE: {
        value: 'delete',
        alias: '删除'
    },
    VIEW: {
        value: 'view',
        alias: '查看'
    },
    MOVE: {
        value: 'move',
        alias: '移动'
    },
    USE: {
        value: 'use',
        alias: '使用'
    },
    SHARE: {
        value: 'share',
        alias: '分享'
    },
    LIST: {
        value: 'list',
        alias: '列表'
    },
    EXECUTE: {
        value: 'execute',
        alias: '执行'
    },
    ENABLE: {
        value: 'enable',
        alias: '停用/启用'
    },
    MANAGE: {
        value: 'manage',
        alias: '管理'
    }
}
