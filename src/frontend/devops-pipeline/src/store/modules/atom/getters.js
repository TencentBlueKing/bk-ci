/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import { buildEnvMap, jobConst, semverVersionKeySet, VERSION_STATUS_ENUM } from '@/utils/pipelineConst'
import Vue from 'vue'
import { getAtomModalKey, isCodePullAtom, isNewAtomTemplate, isNormalContainer, isTriggerContainer, isVmContainer } from './atomUtil'
import { buildNoRules, defaultBuildNo, platformList } from './constants'
import { VAR_MAX_LENGTH } from '@/store/constants'

function isSkip (status) {
    return status === 'SKIP'
}

export default {
    isCurPipelineLocked: state => {
        return state.pipelineInfo?.locked ?? false
    },
    hasDraftPipeline: state => {
        return state.pipelineInfo?.version !== state.pipelineInfo?.releaseVersion
    },
    getDraftBaseVersionName: (state, getters) => {
        return getters.hasDraftPipeline ? state.pipelineInfo?.baseVersionName : '--'
    },
    pipelineHistoryViewable: state => {
        return [
            VERSION_STATUS_ENUM.BRANCH,
            VERSION_STATUS_ENUM.RELEASED
        ].includes(state.pipelineInfo?.latestVersionStatus)
    },
    isBranchVersion: state => {
        return state.activePipelineVersion?.status === VERSION_STATUS_ENUM.BRANCH
    },
    isReleasePipeline: state => {
        return state.pipelineInfo?.latestVersionStatus === VERSION_STATUS_ENUM.RELEASED
    },
    onlyBranchPipeline: state => {
        return state.pipelineInfo?.latestVersionStatus === VERSION_STATUS_ENUM.BRANCH
    },
    isCommittingPipeline: state => {
        return state.pipelineInfo?.latestVersionStatus === VERSION_STATUS_ENUM.COMMITTING
    },
    isReleaseVersion: state => {
        return state.activePipelineVersion?.version === state.pipelineInfo?.releaseVersion && state.activePipelineVersion?.status === VERSION_STATUS_ENUM.RELEASED
    },
    isOutdatedVersion: state => {
        return state.activePipelineVersion?.version < state.pipelineInfo?.releaseVersion
    },
    isActiveDraftVersion: state => {
        return state.activePipelineVersion?.isDraft ?? false
    },
    pacEnabled: state => {
        return state.pipelineInfo?.pipelineAsCodeSettings?.enable ?? false
    },
    failIfVariableInvalid: state => {
        console.log(state?.pipelineSetting?.failIfVariableInvalid)
        return state?.pipelineSetting?.failIfVariableInvalid ?? false
    },
    yamlInfo: state => {
        return state.pipelineInfo?.yamlInfo
    },
    getPipelineSubscriptions: state => type => {
        return state.pipelineSetting?.[`${type}SubscriptionList`] ?? []
    },
    curPipelineParams: state => {
        const firstJob = state.pipeline?.stages?.[0]?.containers?.[0]
        return firstJob?.params?.filter(param => !semverVersionKeySet.has(param.id)) ?? []
    },
    curPipelineBuildNoConfig: state => {
        const firstJob = state.pipeline?.stages?.[0]?.containers?.[0]
        const semver = firstJob?.params?.filter(param => semverVersionKeySet.has(param.id))
        return firstJob?.buildNo
            ? {
                ...firstJob.buildNo,
                semver: semver.reduce((acc, cur) => ({
                    ...acc,
                    [cur.id]: cur.defaultValue
                }), {})
            }
            : null
    },
    fullPipeline: state => {
        return {
            ...state.pipeline,
            stages: [
                state.pipeline.stages[0],
                ...state.pipelineWithoutTrigger.stages
            ]
        }
    },
    getAtomCodeListByCategory: state => category => {
        return state.atomCodeList.filter(atomCode => {
            const atom = state.atomMap[atomCode]
            return atom.category === category
        })
    },
    classifyCodeListByCategory: state => category => {
        const isTrigger = category === 'TRIGGER'
        if (isTrigger) {
            return ['trigger']
        }
        return state.atomClassifyCodeList.filter(classifyCode => classifyCode !== 'trigger')
    },
    getAtomDisabled: (state, getters) => (list, os, category) => {
        list.forEach(atom => {
            atom.disabled = getters.isAtomDisabled({ os, atom, category })
        })
        return list
    },

    isAtomDisabled: state => ({ os, atom, category }) => {
        if (atom.category === 'TRIGGER') return atom.category !== category
        return (!os && atom.os.length > 0 && category !== 'TRIGGER') || (os && atom.os.length > 0 && !atom.os.includes(os)) || (os && atom.os.length === 0 && !atom.buildLessRunFlag) || false
    },
    getAtomModal: state => ({ atomCode, version }) => {
        const key = getAtomModalKey(atomCode, version)
        const atomModal = state.atomModalMap[key]
        return atomModal || null
    },
    getDefaultVersion: state => atomCode => {
        try {
            const atom = state.atomMap[atomCode]
            return atom.defaultVersion || '1.*'
        } catch (error) {
            return '1.*'
        }
    },
    getContainerModalByType: state => type => {
        const key = type.toUpperCase()
        const containerModal = state.containerModalMap[key]
        return containerModal
    },
    getAppEnvs: state => os => {
        const containerModal = state.containerModalMap[os]
        return Array.isArray(containerModal.apps)
            ? containerModal.apps.reduce((appEnvs, app) => {
                appEnvs[app.name] = app.env
                return appEnvs
            }, {})
            : {}
    },
    getBuildResourceTypeList: state => os => {
        try {
            const containerModal = state.containerModalMap[os]
            return containerModal.typeList
        } catch (error) {
            console.warn(error)
            return []
        }
    },
    getContainerModalId: state => os => {
        const containerModal = state.containerModalMap[os]
        return containerModal ? containerModal.id : ''
    },
    getContainerApps: state => os => {
        const containerModal = state.containerModalMap[os]
        return containerModal
            ? containerModal.apps.reduce((apps, item) => {
                apps[item.name] = item
                return apps
            }, {})
            : {}
    },
    osList: state => {
        return state.containerTypeList.filter(type => type !== 'TRIGGER').map(type => {
            return {
                value: type,
                label: type !== 'NONE' ? jobConst[type] : ((window.pipelineVue.$i18n && window.pipelineVue.$i18n.t(`storeMap.${jobConst[type]}`)) || jobConst[type])
            }
        })
    },
    getEditingElementPos: state => state.editingElementPos,
    isEditing: state => {
        return state.isPipelineEditing
    },
    checkPipelineInvalid: (state, getters) => (stages, pipelineSetting) => {
        try {
            let codeccCount = 0
            let manualTriggerCount = 0
            let remoteTriggerCount = 0

            if (pipelineSetting && !pipelineSetting.pipelineName) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('settings.emptyPipelineName'))
            }

            if (pipelineSetting && pipelineSetting.buildNumRule && !/^[\w-{}() +?.:$"]{1,256}$/.test(pipelineSetting.buildNumRule)) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('settings.correctBuildNumber'))
            }

            if (stages.length > state.pipelineLimit.stageLimit) {
                throw new Error(window.pipelineVue.$i18n && (window.pipelineVue.$i18n.t('storeMap.stageLimit') + state.pipelineLimit.stageLimit))
            }

            stages.forEach((stage, index) => {
                if (index !== 0 && stage.checkIn) {
                    const { notifyType = [], notifyGroup = [] } = stage && stage.checkIn
                    if (notifyType.length && notifyType.includes('WEWORK_GROUP') && !notifyGroup.length) {
                        Vue.set(stage.checkIn, 'isReviewError', true)
                        throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.correctPipeline'))
                    }
                }
            })

            if (stages.some(stage => stage.isError)) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.correctPipeline'))
            }

            if (stages.some(stage => stage.containers.length > state.pipelineLimit.jobLimit)) {
                throw new Error(window.pipelineVue.$i18n && (window.pipelineVue.$i18n.t('storeMap.jobLimit') + state.pipelineLimit.jobLimit))
            }

            

            const allContainers = getters.getAllContainers(stages)
            if (allContainers.length > 0 && pipelineSetting?.failIfVariableInvalid) {
                const invalidList = allContainers[0].params.filter(param => typeof param.defaultValue === 'string' && param.defaultValue.length > VAR_MAX_LENGTH)
                if (invalidList.length) {
                    invalidList.forEach(item => {
                        Vue.set(item, 'isInvalid', true)
                    })
                    throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.paramLengthLimitTips', [invalidList[0].id, invalidList[0].defaultValue.length, VAR_MAX_LENGTH]))
                }
                
            } else if (allContainers.length > 0) {
                allContainers[0].params.forEach(item => {
                    if (item.isInvalid) {
                        Vue.set(item, 'isInvalid', false)
                    }
                })
            }


            // 当前所有插件element
            const elementsMap = allContainers.reduce(function (prev, cur) {
                prev.push(...cur.elements)
                return prev
            }, [])

            if (elementsMap.some(element => !element.atomCode)) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.PleaseSelectAtom'))
            }

            if (allContainers.some(container => container.isError)) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.correctPipeline'))
            }

            if (allContainers.some(container => container.elements.length > state.pipelineLimit.atomLimit)) {
                throw new Error(window.pipelineVue.$i18n && (window.pipelineVue.$i18n.t('storeMap.atomLimit') + state.pipelineLimit.atomLimit))
            }

            const allElements = getters.getAllElements(stages)

            const elementValid = allElements.some(ele => {
                const atomCode = ele.atomCode || ele['@type']
                if (!atomCode) {
                    throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.PleaseSelectAtom'))
                }
                atomCode === 'linuxPaasCodeCCScript' && codeccCount++
                atomCode === 'CodeccCheckAtom' && codeccCount++
                atomCode === 'manualTrigger' && manualTriggerCount++
                atomCode === 'remoteTrigger' && remoteTriggerCount++

                return codeccCount > 1 || manualTriggerCount > 1 || remoteTriggerCount > 1 || ele.isError
            })

            if (codeccCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneCodecc'))
            } else if (manualTriggerCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneManualTrigger'))
            } else if (remoteTriggerCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneRemoteTrigger'))
            } else if (elementValid) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.correctPipeline'))
            }

            return {
                inValid: false,
                message: ''
            }
        } catch (e) {
            console.trace(e)
            return {
                message: e.message,
                inValid: true
            }
        }
    },
    hasBuildNo: state => stages => stages[0].containers[0].buildNo,
    hasFinallyStage: state => state.pipeline.stages[state.pipeline.stages.length - 1].finally === true,
    userParams: state => {
        return state.pipeline ? state.pipeline.stages[0].containers[0].params : []
    },
    getAllElements: state => stages => {
        const allElements = []
        stages.map(stage => stage.containers.map(container => allElements.splice(0, 0, ...container.elements)))
        return allElements
    },
    getAllContainers: state => stages => {
        return stages.reduce((acc, stage) => {
            acc = [
                ...acc,
                ...stage.containers
            ]
            return acc
        }, [])
    },
    getStage: state => (stages, stageIndex) => {
        const stage = Array.isArray(stages) ? stages[stageIndex] : null
        return stage
    },
    getContainers: state => stage => {
        return stage && Array.isArray(stage.containers) ? stage.containers : []
    },
    getContainer: (state, getters) => (containers, containerIndex, containerGroupIndex = undefined) => {
        let container = null
        try {
            if (containerGroupIndex !== undefined) {
                container = Array.isArray(containers) ? containers[containerIndex].groupContainers[containerGroupIndex] : null
            } else {
                container = Array.isArray(containers) ? containers[containerIndex] : null
            }
        } catch (_) {
            container = null
        }
        if (container) {
            if (isVmContainer(container['@type']) && !container.buildEnv) {
                Vue.set(container, 'buildEnv', {})
            }
            if (isVmContainer(container['@type']) && !container.dispatchType) {
                const previewEnvKey = Object.keys(buildEnvMap).find(key => container[key])
                const containerModal = getters.getContainerModalByType(container.baseOS)
                if (previewEnvKey && containerModal) {
                    const buildType = buildEnvMap[previewEnvKey]
                    Vue.set(container, 'dispatchType', {
                        buildType,
                        value: '',
                        workspace: container.thirdPartyWorkspace || ''
                    })
                    delete container[previewEnvKey]
                    delete container.thirdPartyWorkspace
                } else if (containerModal) {
                    Vue.set(container, 'dispatchType', {
                        buildType: containerModal.defaultBuildType
                    })
                }
            }
            if (isTriggerContainer(container['@type']) && !container.params) {
                Vue.set(container, 'params', [])
            }
            if (typeof container.isError === 'undefined') {
                Vue.set(container, 'isError', false)
            }
        }
        return container
    },
    getRealSeqId: state => (stages, stageIndex, containerIndex) => {
        return stages.slice(0, stageIndex).reduce((acc, stage) => {
            acc += stage.containers.length
            return acc
        }, 0) + containerIndex
    },
    isDockerBuildResource: state => container => {
        return container && ((container.dispatchType && container.dispatchType.buildType === 'DOCKER') || container.dockerBuildVersion)
    },
    isThirdPartyContainer: state => container => { // 是否是第三方构建机
        return container && container.dispatchType && typeof container.dispatchType.buildType === 'string' && container.dispatchType.buildType.indexOf('THIRD_PARTY_') > -1
    },
    isPublicResource: state => container => {
        return container && container.dispatchType && container.dispatchType.buildType === 'ESXi'
    },
    isPublicDevCloudContainer: state => container => { // 是否是第三方构建机
        return container && container.dispatchType && typeof container.dispatchType.buildType === 'string' && container.dispatchType.buildType === 'PUBLIC_DEVCLOUD'
    },
    isBcsContainer: state => container => { // 是否是第三方构建机
        return container && container.dispatchType && typeof container.dispatchType.buildType === 'string' && container.dispatchType.buildType === 'PUBLIC_BCS'
    },
    isThirdDockerContainer: state => container => {
        return container?.dispatchType?.buildType?.indexOf('THIRD_PARTY_') > -1 && container?.dispatchType?.dockerInfo && Object.keys(container?.dispatchType?.dockerInfo).length
    },
    checkShowDebugDockerBtn: (state, getters) => (container, routeName, execDetail) => {
        const isDocker = getters.isDockerBuildResource(container)
        const isPublicDevCloud = getters.isPublicDevCloudContainer(container)
        const isBcsContainer = getters.isBcsContainer(container)
        const isThirdDocker = getters.isThirdDockerContainer(container)
        const isLatestExecDetail = execDetail && execDetail.buildNum === execDetail.latestBuildNum

        return routeName !== 'templateEdit' && container.baseOS === 'LINUX' && (isDocker || isPublicDevCloud || isBcsContainer || isThirdDocker) && (['pipelinesEdit', 'pipelinesHistory'].includes(routeName) || container.status === 'RUNNING' || (routeName === 'pipelinesDetail' && isLatestExecDetail))
    },
    getElements: state => container => {
        return container && Array.isArray(container.elements)
            ? container.elements.map(element => {
                return Object.assign(element, {
                    atomCode: element.atomCode && element['@type'] !== element.atomCode ? element.atomCode : element['@type']
                })
            })
            : []
    },
    getElement: state => (container, index) => {
        const element = Array.isArray(container?.elements) ? container.elements[index] : null
        if (element) {
            typeof element.isError === 'undefined' && Vue.set(element, 'isError', false)
        }
        return element
    },
    buildNoRules: state => buildNoRules.map(rule => {
        return {
            ...rule,
            label: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t(`storeMap.${rule.label}`)) || rule.label
        }
    }),
    isVmContainer: state => container => isVmContainer(container['@type']),
    isTriggerContainer: state => container => isTriggerContainer(container?.['@type']),
    isCodePullAtom: state => atom => isCodePullAtom(atom['@type']),
    isNormalContainer: state => container => isNormalContainer(container['@type']),
    defaultBuildNo: state => defaultBuildNo,
    getPlatformList: state => platformList,
    getAtomModalKey: state => getAtomModalKey,
    isNewAtomTemplate: state => isNewAtomTemplate,
    atomVersionChangedKeys: state => state.atomVersionChangedKeys,
    getExecDetail: state => {
        if (!state.execDetail) return null
        if (!state.hideSkipExecTask) {
            return state.execDetail
        }

        const stages = state.execDetail.model?.stages?.filter(stage => !isSkip(stage.status)).map(stage => {
            const containers = stage.containers.filter((container) => !isSkip(container.status)).map(container => {
                const elements = container.elements.filter(
                    (element) => !isSkip(element.status)
                )
                if (container.matrixGroupFlag && Array.isArray(container.groupContainers)) {
                    return {
                        ...container,
                        elements,
                        groupContainers: container.groupContainers.filter(groupContainer => !isSkip(groupContainer.status)).map(groupContainer => {
                            const subElements = groupContainer.elements.filter(
                                (element, index) => !isSkip(element.status ?? elements[index]?.status)
                            )
                            return {
                                ...groupContainer,
                                elements: subElements
                            }
                        })
                    }
                }
                return {
                    ...container,
                    elements
                }
            })
            return {
                ...stage,
                containers
            }
        })
        return Object.assign({}, state.execDetail, {
            model: {
                ...state.execDetail.model,
                stages
            }
        })
    }
}
