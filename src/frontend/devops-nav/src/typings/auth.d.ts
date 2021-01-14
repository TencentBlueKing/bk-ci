
export enum resourceMap {
  'CODE_REPERTORY' = 'repertory',
  'PIPELINE_DEFAULT' = 'pipeline',
  'TICKET_CREDENTIAL' = 'credential',
  'TICKET_CERT' = 'cert',
  'ENVIRONMENT_ENVIRONMENT' = 'environment',
  'ENVIRONMENT_ENV_NODE' = 'env_node',
  'PROJECT' = 'project',
  'QUALITY_RULE' = 'rule',
  'QUALITY_GROUP' = 'group'
}

export enum actionType {
  'CREATE' = 'create',
  'DEPLOY' = 'deploy',
  'DOWNLOAD' = 'download',
  'EDIT' = 'edit',
  'DELETE' = 'delete',
  'VIEW' = 'view',
  'MOVE' = 'move',
  'USE' = 'use',
  'SHARE' = 'share',
  'LIST' = 'list',
  'EXECUTE' = 'execute',
  'ENABLE' = 'enable',
  'MANAGE' = 'manage'
}

export type actionAlias = {
  'create': '创建',
  'deploy': '部署',
  'download': '下载',
  'edit': '编辑',
  'delete': '删除',
  'view': '查看',
  'move': '移动',
  'use': '使用',
  'share': '分享',
  'list': '列表',
  'execute': '执行',
  'enable': '停用/启用',
  'manage': '管理'
}
