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
        return
    }

    if (dockerInfo.imageType === 'THIRD') {
        dockerInfo.storeImage = undefined
        dispatchType.recommendFlag = undefined
    } else if (dockerInfo.imageType === 'BKSTORE') {
        dockerInfo.image = ''
        dockerInfo.credential = ''
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
