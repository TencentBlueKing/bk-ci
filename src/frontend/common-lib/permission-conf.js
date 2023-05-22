
export const resourceMap = {
    code: 'CODE_REPERTORY',
    pipeline: 'PIPELINE_DEFAULT',
    credential: 'TICKET_CREDENTIAL',
    cert: 'TICKET_CERT',
    environment: 'ENVIRONMENT_ENVIRONMENT',
    envNode: 'ENVIRONMENT_ENV_NODE',
    project: 'PROJECT',
    rule: 'QUALITY_RULE',
    ruleGroup: 'QUALITY_GROUP'
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
    QUALITY_GROUP: 'group'
}

export const resourceAliasMap = {
    CODE_REPERTORY: 'codelib',
    PIPELINE_DEFAULT: 'pipeline',
    TICKET_CREDENTIAL: 'ticket',
    TICKET_CERT: 'cert',
    ENVIRONMENT_ENVIRONMENT: 'env',
    ENVIRONMENT_ENV_NODE: 'node',
    PROJECT: 'project',
    QUALITY_RULE: 'quality',
    QUALITY_GROUP: 'rules'
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
        i18nAlias: 'actions.create'
    },
    DEPLOY: {
        value: 'deploy',
        i18nAlias: 'actions.deploy'
    },
    DOWNLOAD: {
        value: 'download',
        i18nAlias: 'actions.download'
    },
    EDIT: {
        value: 'edit',
        i18nAlias: 'actions.edit'
    },
    DELETE: {
        value: 'delete',
        i18nAlias: 'actions.delete'
    },
    VIEW: {
        value: 'view',
        i18nAlias: 'actions.view'
    },
    MOVE: {
        value: 'move',
        i18nAlias: 'actions.move'
    },
    USE: {
        value: 'use',
        i18nAlias: 'actions.use'
    },
    SHARE: {
        value: 'share',
        i18nAlias: 'actions.share'
    },
    LIST: {
        value: 'list',
        i18nAlias: 'actions.list'
    },
    EXECUTE: {
        value: 'execute',
        i18nAlias: 'actions.execute'
    },
    ENABLE: {
        value: 'enable',
        i18nAlias: 'actions.enable/启用'
    },
    MANAGE: {
        value: 'manage',
        i18nAlias: 'actions.manage'
    }
}
