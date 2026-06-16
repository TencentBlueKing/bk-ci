
// 流水线复制任务状态
export const PipelineBatchTaskStatus = {
    DRAFT: 'DRAFT', // 草稿-允许页面编辑
    PIPELINE_ANALYZING: 'PIPELINE_ANALYZING', // 流水线分析中-持续轮询获取状态
    PIPELINE_RESOURCE_ANALYZING: 'PIPELINE_RESOURCE_ANALYZING', // 流水线资源分析中-持续轮询获取状态
    EXECUTE_QUEUED: 'EXECUTE_QUEUED', // 执行排队中
    EXECUTING: 'EXECUTING', // 执行中
    // 以下表示页面仅查看
    SUCCESS: 'SUCCESS', // 成功
    FAILED: 'FAILED',   // 失败
    PARTIAL_FAILED: 'PARTIAL_FAILED',   // 部分失败
}

// 流水线ID策略
export const PipelineIdStrategy = {
    PIPELINE_CREATE_NEW_ID: 'PIPELINE_CREATE_NEW_ID',   // 自动创建新流水线ID
    PIPELINE_REUSE_SOURCE_ID: 'PIPELINE_REUSE_SOURCE_ID'   // 复用源流水线ID
}

// 流水线批量任务步骤
export const PipelineBatchTaskStep = {
    CONFIG: 'config',   // 配置复制范围
    RESOURCE_DEPEND: 'resourceDepend',   // 处理资源依赖
    EXECUTE: 'execute'   // 任务执行
}

// 代码库授权配置
export const RepositoryCopyResourceProperties = {
    authType: 'authType',   // 授权方式
    authInfo: 'authInfo',   // 授权信息
    repositoryType: 'repositoryType',   // 代码库协议
    repositoryUrl: 'repositoryUrl'  // 代码库URL
}

// 流水线批量任务明细状态
export const PipelineBatchTaskDetailStatus = {
    EXCLUDED: 'EXCLUDED',   // 已排除
    SUCCESS: 'SUCCESS',   // 成功
    FAILED: 'FAILED',   // 失败
    WAIT_COPY: 'WAIT_COPY'   // 待复制
}

// 流水线复制资源类型
export const PipelineCopyResourceType = {
    PIPELINE_TEMPLATE: 'PIPELINE_TEMPLATE',   // 流水线模板
    REPOSITORY: 'REPOSITORY',   // 代码库
    BUILD_ENV: 'BUILD_ENV',   // 构建环境
    BUILD_NODE: 'BUILD_NODE',   // 构建节点
    DEPLOY_ENV: 'DEPLOY_ENV',   // 部署环境
    DEPLOY_NODE: 'DEPLOY_NODE',   // 部署节点
    CREDENTIAL: 'CREDENTIAL',   // 凭证
    PIPELINE_LABEL: 'PIPELINE_LABEL',   // 流水线标签
    PIPELINE_GROUP: 'PIPELINE_GROUP',   // 流水线组
    PIPELINE: 'PIPELINE',   // 流水线冲突
}

// 流水线复制资源处理策略
export const PipelineCopyStrategy = {
    // 流水线模板
    PIPELINE_TEMPLATE_REUSE_SAME_NAME: 'PIPELINE_TEMPLATE_REUSE_SAME_NAME',   // 自动创建新流水线ID
    PIPELINE_TEMPLATE_CREATE_NEW: 'PIPELINE_TEMPLATE_CREATE_NEW',   // 复用源流水线ID
    // 代码库
    REPOSITORY_REUSE_SAME_NAME_PROTOCOL: 'REPOSITORY_REUSE_SAME_NAME_PROTOCOL',   // 复用目标项目同名同协议代码库
    REPOSITORY_CREATE_NEW: 'REPOSITORY_CREATE_NEW',   // 创建新代码库
    // 构建环境
    BUILD_ENV_REUSE_SAME_NAME: 'BUILD_ENV_REUSE_SAME_NAME',   // 复用目标项目同名环境
    BUILD_ENV_CREATE_WITHOUT_NODE: 'BUILD_ENV_CREATE_WITHOUT_NODE',   // 新建环境不带节点
    BUILD_ENV_CREATE_AND_MOVE_NODE: 'BUILD_ENV_CREATE_AND_MOVE_NODE',   // 新建环境并转移节点
    // 构建节点
    BUILD_NODE_REUSE_SAME_NAME: 'BUILD_NODE_REUSE_SAME_NAME',   // 复用目标项目同名构建节点
    BUILD_NODE_MOVE_TO_TARGET_PROJECT: 'BUILD_NODE_MOVE_TO_TARGET_PROJECT',   // 转移构建节点到目标项目
    // 部署环境
    DEPLOY_ENV_REUSE_SAME_NAME: 'DEPLOY_ENV_REUSE_SAME_NAME',   // 复用目标项目同名部署环境
    DEPLOY_ENV_CREATE_WITHOUT_NODE: 'DEPLOY_ENV_CREATE_WITHOUT_NODE',   // 新建部署环境不带节点
    DEPLOY_ENV_CREATE_AND_MOVE_NODE: 'DEPLOY_ENV_CREATE_AND_MOVE_NODE',   // 新建部署环境并转移节点
    // 部署节点
    DEPLOY_NODE_REUSE_SAME_NAME: 'DEPLOY_NODE_REUSE_SAME_NAME',   // 复用目标项目同名部署节点
    DEPLOY_NODE_MOVE_TO_TARGET_PROJECT: 'DEPLOY_NODE_MOVE_TO_TARGET_PROJECT',   // 转移部署节点到目标项目
    // 凭证
    CREDENTIAL_REUSE_SAME_NAME: 'CREDENTIAL_REUSE_SAME_NAME',   // 复用目标项目同名凭证
    CREDENTIAL_REPLACE_TARGET: 'CREDENTIAL_REPLACE_TARGET',   // 替换为目标项目其他凭证
    CREDENTIAL_CREATE_NEW: 'CREDENTIAL_CREATE_NEW',    // 创建新凭证
    // 流水线标签
    LABEL_AUTO_REUSE_OR_CREATE: 'LABEL_AUTO_REUSE_OR_CREATE',   // 自动复用标签，不存在则创建
    LABEL_IGNORE: 'LABEL_IGNORE',   // 忽略标签
    // 流水线组
    PIPELINE_GROUP_AUTO_REUSE_OR_CREATE: 'PIPELINE_GROUP_AUTO_REUSE_OR_CREATE',   // 自动复用流水线组，不存在则创建
    PIPELINE_GROUP_IGNORE: 'PIPELINE_GROUP_IGNORE',   // 忽略流水线组
    // 流水线冲突
    PIPELINE_AUTO_RESOLVE_CONFLICT: 'PIPELINE_AUTO_RESOLVE_CONFLICT',   // 自动解决冲突
    PIPELINE_SKIP: 'PIPELINE_SKIP'   // 跳过,本次不处理
}

// 流水线复制任务资源状态
export const PipelineCopyResourceStatus = {
    UNPROCESSED: 'UNPROCESSED',   // 待处理
    PROCESSED: 'PROCESSED',   // 已处理
    SUCCESS: 'SUCCESS',   // 复制成功
    FAILED: 'FAILED'   // 复制失败
}

// 高风险资源类型 → 对应的高风险策略值 映射
export const HIGH_RISK_STRATEGIES = {
    [PipelineCopyResourceType.BUILD_ENV]: PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE,
    [PipelineCopyResourceType.DEPLOY_ENV]: PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE,
    [PipelineCopyResourceType.BUILD_NODE]: PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT,
    [PipelineCopyResourceType.DEPLOY_NODE]: PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT,
    [PipelineCopyResourceType.CREDENTIAL]: PipelineCopyStrategy.CREDENTIAL_CREATE_NEW
}

// 流水线复制动作
export const PipelineCopyAction = {
    PIPELINE: 'PIPELINE',   // 流水线
    NEED_COMPLETION: 'NEED_COMPLETION',   // 资源需要补齐
    NEED_TRANSFER: 'NEED_TRANSFER',   // 资源需要迁移
    AUTO_FINISH: 'AUTO_FINISH',   // 自动完成
}

// 流水线复制资源类型显示映射
export const PipelineCopyResourceTypeMap = {
    PIPELINE_TEMPLATE: '流水线模板',
    REPOSITORY: '代码库',
    BUILD_ENV: '构建环境',
    BUILD_NODE: '构建节点',
    DEPLOY_ENV: '部署环境',
    DEPLOY_NODE: '部署节点',
    CREDENTIAL: '凭据',
    PIPELINE_LABEL: '标签',
    PIPELINE_GROUP: '流水线组',
    PIPELINE: '流水线'
}

// 流水线复制资源处理策略显示映射
export const PipelineCopyStrategyMap = {
    // 流水线模板
    [PipelineCopyStrategy.PIPELINE_TEMPLATE_REUSE_SAME_NAME]: '复用同名模板',
    [PipelineCopyStrategy.PIPELINE_TEMPLATE_CREATE_NEW]: '复用源流水线ID',
    // 代码库
    [PipelineCopyStrategy.REPOSITORY_REUSE_SAME_NAME_PROTOCOL]: '复用同名同协议代码库',
    [PipelineCopyStrategy.REPOSITORY_CREATE_NEW]: '创建新代码库',
    // 构建环境
    [PipelineCopyStrategy.BUILD_ENV_REUSE_SAME_NAME]: '复用同名环境',
    [PipelineCopyStrategy.BUILD_ENV_CREATE_WITHOUT_NODE]: '新建环境(不带节点)',
    [PipelineCopyStrategy.BUILD_ENV_CREATE_AND_MOVE_NODE]: '新建环境并转移节点',
    // 构建节点
    [PipelineCopyStrategy.BUILD_NODE_REUSE_SAME_NAME]: '复用同名构建节点',
    [PipelineCopyStrategy.BUILD_NODE_MOVE_TO_TARGET_PROJECT]: '转移构建节点到目标项目',
    // 部署环境
    [PipelineCopyStrategy.DEPLOY_ENV_REUSE_SAME_NAME]: '复用同名部署环境',
    [PipelineCopyStrategy.DEPLOY_ENV_CREATE_WITHOUT_NODE]: '新建部署环境(不带节点)',
    [PipelineCopyStrategy.DEPLOY_ENV_CREATE_AND_MOVE_NODE]: '新建部署环境并转移节点',
    // 部署节点
    [PipelineCopyStrategy.DEPLOY_NODE_REUSE_SAME_NAME]: '复用同名部署节点',
    [PipelineCopyStrategy.DEPLOY_NODE_MOVE_TO_TARGET_PROJECT]: '转移部署节点到目标项目',
    // 凭证
    [PipelineCopyStrategy.CREDENTIAL_REUSE_SAME_NAME]: '复用同名凭证',
    [PipelineCopyStrategy.CREDENTIAL_REPLACE_TARGET]: '替换为目标项目其他凭证',
    [PipelineCopyStrategy.CREDENTIAL_CREATE_NEW]: '创建新凭证',
    // 流水线标签
    [PipelineCopyStrategy.LABEL_AUTO_REUSE_OR_CREATE]: '自动复用或创建标签',
    [PipelineCopyStrategy.LABEL_IGNORE]: '忽略标签',
    // 流水线组
    [PipelineCopyStrategy.PIPELINE_GROUP_AUTO_REUSE_OR_CREATE]: '自动复用或创建流水线组',
    [PipelineCopyStrategy.PIPELINE_GROUP_IGNORE]: '忽略流水线组',
    // 流水线冲突
    [PipelineCopyStrategy.PIPELINE_AUTO_RESOLVE_CONFLICT]: '自动解决冲突',
    [PipelineCopyStrategy.PIPELINE_SKIP]: '跳过(本次不处理)'
}

export const PipelineBatchTaskDetailErrorType = {
    DEPENDENCY_CREATE_FAILED: 'DEPENDENCY_CREATE_FAILED',   // 依赖创建错误
    PIPELINE_CREATE_FAILED: 'PIPELINE_CREATE_FAILED',   // 流水线创建错误
    SYSTEM_ERROR: 'SYSTEM_ERROR'   // 系统错误
}
