/*
 * Tencent is pleased to support the open source community by making
 * 蓝鲸智云PaaS平台 (BlueKing PaaS) available.
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 * 蓝鲸智云PaaS平台 (BlueKing PaaS) is licensed under the MIT License.
 * License for 蓝鲸智云PaaS平台 (BlueKing PaaS):
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

export function applyInstancePacBinding (pipelineDetail, isCopy) {
    return {
        enabledPac: isCopy ? false : Boolean(pipelineDetail?.repoHashId),
        repoHashId: isCopy ? '' : (pipelineDetail?.repoHashId ?? ''),
        scmType: isCopy ? '' : (pipelineDetail?.scmType ?? ''),
        filePath: isCopy ? '' : (pipelineDetail?.filePath ?? '')
    }
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
