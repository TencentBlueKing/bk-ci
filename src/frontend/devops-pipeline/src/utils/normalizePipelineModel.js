export const DOCKER_WORKSPACE_NO_MOUNT = '__NO_MOUNT__'

export function isDockerWorkspaceMounted (dispatchType) {
    if (dispatchType?.workspace === DOCKER_WORKSPACE_NO_MOUNT) return false
    return dispatchType?.dockerInfo?.options?.mountWorkspace !== false
}

export function dockerWorkspaceValue (dispatchType) {
    const workspace = dispatchType?.workspace
    return workspace === DOCKER_WORKSPACE_NO_MOUNT ? '' : (workspace || '')
}

export function normalizePipelineModel (model) {
    const traverse = (node) => {
        if (Array.isArray(node)) {
            node.forEach(traverse)
            return
        }
        if (!node || typeof node !== 'object') {
            return
        }
        if (node.dispatchType && typeof node.dispatchType === 'object') {
            normalizeDispatchType(node.dispatchType)
        }
        if (Array.isArray(node.params)) {
            node.params.forEach(normalizePipelineParam)
        }
        Object.values(node).forEach(traverse)
    }

    traverse(model)
}

export function normalizeDispatchType (dispatchType) {
    if (dispatchType.imageType === 'THIRD') {
        dispatchType.imageCode = ''
        dispatchType.imageVersion = ''
        dispatchType.imageName = ''
        dispatchType.recommendFlag = undefined
    }

    const dockerInfo = dispatchType.dockerInfo
    if (!dockerInfo || typeof dockerInfo !== 'object') {
        if (dispatchType.workspace === DOCKER_WORKSPACE_NO_MOUNT) {
            dispatchType.workspace = ''
        }
        return
    }

    if (dockerInfo.imageType === 'THIRD') {
        dockerInfo.storeImage = undefined
        dispatchType.recommendFlag = undefined
    } else if (dockerInfo.imageType === 'BKSTORE') {
        dockerInfo.image = ''
        dockerInfo.credential = ''
    }

    normalizeMountWorkspace(dispatchType, dockerInfo)
}

function normalizeMountWorkspace (dispatchType, dockerInfo) {
    const legacyNoMount = dispatchType.workspace === DOCKER_WORKSPACE_NO_MOUNT
    if (legacyNoMount) {
        dispatchType.workspace = ''
    }
    if (!dockerInfo || typeof dockerInfo !== 'object') return

    if (legacyNoMount) {
        dockerInfo.options = {
            ...(dockerInfo.options && typeof dockerInfo.options === 'object' ? dockerInfo.options : {}),
            mountWorkspace: false
        }
        return
    }

    const options = dockerInfo.options
    if (!options || typeof options !== 'object') return
    if (!Object.prototype.hasOwnProperty.call(options, 'mountWorkspace')) return
    if (options.mountWorkspace === false) return
    delete options.mountWorkspace
    if (!Object.keys(options).length) {
        dockerInfo.options = undefined
    }
}

export function normalizePipelineParam (param) {
    if (!param || typeof param !== 'object') {
        return
    }
    if (!['ENUM', 'MULTIPLE'].includes(param.type)) {
        return
    }

    const payload = param.payload
    if (payload?.type === 'remote') {
        param.options = []
        param.payload = {
            type: 'remote',
            url: payload.url || '',
            dataPath: payload.dataPath || '',
            paramId: payload.paramId || '',
            paramName: payload.paramName || ''
        }
        return
    }

    param.payload = payload?.type ? { type: payload.type } : undefined
}
