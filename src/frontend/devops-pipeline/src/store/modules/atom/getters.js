/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

import Vue from 'vue'
import { buildNoRules, defaultBuildNo, platformList } from './constants'
import { getAtomModalKey, isVmContainer, isTriggerContainer, isNormalContainer, isCodePullAtom, isNewAtomTemplate } from './atomUtil'
import { jobConst, buildEnvMap } from '@/utils/pipelineConst'

export default {
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
    getAtomTree: (state, getters) => (os, category, searchKey) => {
        let atomCodeList = getters.getAtomCodeListByCategory(category)
        if (searchKey) {
            const searchStr = searchKey.toLowerCase()
            atomCodeList = atomCodeList.filter(atomCode => {
                const atom = state.atomMap[atomCode] || {}
                const name = (atom.name || '').toLowerCase()
                const summary = (atom.summary || '').toLowerCase()
                return name.indexOf(searchStr) > -1 || summary.indexOf(searchStr) > -1
            })
        }
        const classifyCodeList = getters.classifyCodeListByCategory(category)
        const { atomClassifyMap, atomMap } = state
        const atomTree = classifyCodeList.reduce((cMap, classifyCode) => {
            const classify = atomClassifyMap[classifyCode]
            if (classify) {
                cMap[classifyCode] = {
                    classifyCode,
                    classifyName: classify.classifyName,
                    level: 0,
                    children: []
                }
            }
            return cMap
        }, {
            all: {
                classifyCode: 'all',
                classifyName: (window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.all')) || 'all',
                level: 0,
                children: atomCodeList.map(atomCode => {
                    const atom = atomMap[atomCode]
                    return {
                        ...atom,
                        level: 1,
                        disabled: getters.isAtomDisabled({ os, atom, category })
                    }
                })
            }
        })

        atomCodeList.forEach(atomCode => {
            const atom = atomMap[atomCode]
            const parent = atomTree[atom.classifyCode]
            if (parent && Array.isArray(parent.children)) {
                parent.children.push({
                    ...atom,
                    level: parent.level + 1,
                    disabled: getters.isAtomDisabled({ os, atom, category })
                })
            }
        })

        Object.keys(atomTree).forEach(classify => { // 按disable排序
            if (atomTree[classify] && Array.isArray(atomTree[classify].children)) {
                atomTree[classify].children.sort((a, b) => a.disabled - b.disabled)
            }
        })

        return atomTree
    },
    isAtomDisabled: state => ({ os, atom, category }) => {
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
        return state.pipeline && state.pipeline.editing
    },
    checkPipelineInvalid: (state, getters) => (stages, pipelineSetting) => {
        try {
            let codeccCount = 0
            let manualTriggerCount = 0
            let timerTriggerCount = 0
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

            if (stages.some(stage => stage.isError)) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.correctPipeline'))
            }

            if (stages.some(stage => stage.containers.length > state.pipelineLimit.jobLimit)) {
                throw new Error(window.pipelineVue.$i18n && (window.pipelineVue.$i18n.t('storeMap.jobLimit') + state.pipelineLimit.jobLimit))
            }

            const allContainers = getters.getAllContainers(stages)

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
                ele['@type'] === 'linuxPaasCodeCCScript' && codeccCount++
                ele.atomCode === 'CodeccCheckAtom' && codeccCount++
                ele['@type'] === 'manualTrigger' && manualTriggerCount++
                ele['@type'] === 'timerTrigger' && timerTriggerCount++
                ele['@type'] === 'remoteTrigger' && remoteTriggerCount++

                return codeccCount > 1 || manualTriggerCount > 1 || timerTriggerCount > 1 || remoteTriggerCount > 1 || ele.isError
            })

            if (codeccCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneCodecc'))
            } else if (manualTriggerCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneManualTrigger'))
            } else if (timerTriggerCount > 1) {
                throw new Error(window.pipelineVue.$i18n && window.pipelineVue.$i18n.t('storeMap.oneTimerTrigger'))
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
        const allContainers = []
        stages.map(stage => allContainers.splice(0, 0, ...stage.containers))
        return allContainers
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
        if (container !== null) {
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
    isDockerBuildResource: state => container => {
        return container && ((container.dispatchType && container.dispatchType.buildType === 'DOCKER') || container.dockerBuildVersion)
    },
    isThirdPartyContainer: state => container => { // 是否是第三方构建机
        return container && container.dispatchType && typeof container.dispatchType.buildType === 'string' && container.dispatchType.buildType.indexOf('THIRD_PARTY_') > -1
    },
    isPublicResource: state => container => {
        return container && container.dispatchType && container.dispatchType.buildType === 'ESXi'
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
        const element = container && Array.isArray(container.elements) ? container.elements[index] : null
        if (element !== null) {
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
    isTriggerContainer: state => container => isTriggerContainer(container['@type']),
    isCodePullAtom: state => atom => isCodePullAtom(atom['@type']),
    isNormalContainer: state => container => isNormalContainer(container['@type']),
    defaultBuildNo: state => defaultBuildNo,
    getPlatformList: state => platformList,
    getAtomModalKey: state => getAtomModalKey,
    isNewAtomTemplate: state => isNewAtomTemplate,
    atomVersionChangedKeys: state => state.atomVersionChangedKeys
}
