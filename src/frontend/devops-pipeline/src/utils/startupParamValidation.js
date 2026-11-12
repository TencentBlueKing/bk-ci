import { getVariableValueIssue, VAR_MAX_LENGTH } from '../../../common-lib/variableValue'

export function validateStartupValues (values = {}) {
    for (const [key, value] of Object.entries(values)) {
        const issue = getVariableValueIssue(value)
        if (issue) {
            const message = window.pipelineVue.$i18n.t(
                issue.reference ? 'storeMap.paramOverflowReferenceTips' : 'storeMap.paramLengthLimitTips',
                [key, issue.length, VAR_MAX_LENGTH]
            )
            throw new Error(message)
        }
    }
}

export function validateDraftDefaults (draft) {
    // Code-mode drafts are validated against the submitted YAML by the backend.
    if (draft.storageType === 'YAML') return
    const params = draft.modelAndSetting?.model?.stages?.[0]?.containers?.[0]?.params || []
    validateStartupValues(Object.fromEntries(params.map(param => [param.id, param.defaultValue])))
}
