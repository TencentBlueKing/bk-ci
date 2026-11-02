export function applyInstancePacBinding (pipelineDetail, isCopy) {
    return {
        enabledPac: isCopy ? false : Boolean(pipelineDetail?.repoHashId),
        repoHashId: isCopy ? '' : (pipelineDetail?.repoHashId ?? ''),
        scmType: isCopy ? '' : (pipelineDetail?.scmType ?? ''),
        filePath: isCopy ? '' : (pipelineDetail?.filePath ?? '')
    }
}

export function resolveReleasePipelineId (pipelineId, isCopy) {
    return isCopy ? '' : pipelineId
}

export function isInstancePublishAsCreate ({
    isTemplateInstanceMode,
    isInstanceCreateType,
    routeType,
    copyType = 'copy'
}) {
    if (!isTemplateInstanceMode) return false
    if (isInstanceCreateType) return true
    return routeType === copyType
}

export function isPacSwitcherDisabled ({
    isTemplateInstanceMode,
    isPublishAsCreate,
    templateRefType,
    templateInstanceEnablePac,
    pacEnabled
}) {
    if (!isTemplateInstanceMode) return pacEnabled
    if (isPublishAsCreate) return templateRefType === 'PATH'
    return templateInstanceEnablePac
}

export function isScmTypeDisabled ({
    isTemplateInstanceMode,
    isPublishAsCreate,
    templateInstanceEnablePac,
    pacEnabled,
    hasYamlInfo
}) {
    if (isTemplateInstanceMode) {
        if (isPublishAsCreate) return false
        return templateInstanceEnablePac
    }
    return pacEnabled || !!hasYamlInfo
}

export function isYamlCodeLibDisabled ({
    isTemplateInstanceMode,
    isPublishAsCreate,
    templateInstanceEnablePac,
    pacEnabled
}) {
    if (!isTemplateInstanceMode) return pacEnabled
    if (isPublishAsCreate) return false
    return templateInstanceEnablePac
}

export function shouldSyncInheritedPac ({ isTemplateInstanceMode, isPublishAsCreate }) {
    return isTemplateInstanceMode && !isPublishAsCreate
}

export function shouldBackfillInstanceRepo ({ isTemplateInstanceMode, enablePac, isPublishAsCreate }) {
    return isTemplateInstanceMode && enablePac && !isPublishAsCreate
}
