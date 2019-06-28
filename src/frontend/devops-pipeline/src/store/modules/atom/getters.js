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
            atomCodeList = atomCodeList.filter(atomCode => {
                const atom = state.atomMap[atomCode]
                return atom.name.toLowerCase().indexOf(searchKey.toLowerCase()) > -1
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
                classifyName: '所有',
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

        atomCodeList.map(atomCode => {
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

        Object.keys(atomTree).map(classify => { // 按disable排序
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
        return Array.isArray(containerModal.apps) ? containerModal.apps.reduce((appEnvs, app) => {
            appEnvs[app.name] = app.env
            return appEnvs
        }, {}) : {}
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
        return containerModal ? containerModal.apps.reduce((apps, item) => {
            apps[item.name] = item
            return apps
        }, {}) : {}
    },
    osList: state => {
        return state.containerTypeList.filter(type => type !== 'TRIGGER').map(type => {
            return {
                value: type,
                label: jobConst[type]
            }
        })
    },
    getEditingElementPos: state => state.editingElementPos,
    isEditing: state => {
        return state.pipeline && state.pipeline.editing
    },
    checkPipelineInvalid: (state, getters) => stages => {
        try {
            let codeccCount = 0
            let manualTriggerCount = 0
            let timerTriggerCount = 0
            let remoteTriggerCount = 0

            const allContainers = getters.getAllContainers(stages)

            if (allContainers.some(container => container.isError)) {
                throw new Error('请输入正确的流水线')
            }

            const allElements = getters.getAllElements(stages)

            const elementValid = allElements.some(ele => {
                ele['@type'] === 'linuxPaasCodeCCScript' && codeccCount++
                ele['@type'] === 'manualTrigger' && manualTriggerCount++
                ele['@type'] === 'timerTrigger' && timerTriggerCount++
                ele['@type'] === 'remoteTrigger' && remoteTriggerCount++

                return codeccCount > 1 || manualTriggerCount > 1 || timerTriggerCount > 1 || remoteTriggerCount > 1 || ele.isError
            })

            if (codeccCount > 1) {
                throw new Error('只允许一个代码扫描插件')
            } else if (manualTriggerCount > 1) {
                throw new Error('流水线不允许超过一个手动触发插件')
            } else if (timerTriggerCount > 1) {
                throw new Error('流水线不允许超过一个定时触发插件')
            } else if (remoteTriggerCount > 1) {
                throw new Error('流水线不允许超过一个远程触发插件')
            } else if (elementValid) {
                throw new Error('请输入正确的流水线')
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
        return Array.isArray(stages) ? stages[stageIndex] : null
    },
    getContainers: state => stage => {
        return stage && Array.isArray(stage.containers) ? stage.containers : []
    },
    getContainer: (state, getters) => (containers, containerIndex) => {
        const container = Array.isArray(containers) ? containers[containerIndex] : null
        if (container !== null) {
            if (isVmContainer(container['@type']) && !container.buildEnv) {
                Vue.set(container, 'buildEnv', {})
            }
            if (isVmContainer(container['@type']) && !container.dispatchType) {
                const previewEnvKey = Object.keys(buildEnvMap).find(key => container[key])
                const containerModal = getters.getContainerModalByType(container.baseOS)
                if (previewEnvKey && containerModal) {
                    const buildType = buildEnvMap[previewEnvKey]
                    const buildResourceId = container[previewEnvKey]
                    const buildResourceValue = getters.getBuildResourceNameById(containerModal, buildType, buildResourceId)
                    Vue.set(container, 'dispatchType', {
                        buildType,
                        value: buildResourceValue,
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
    getBuildResourceNameById: state => (containerModal, buildType, buildResourceId) => {
        try {
            const resource = containerModal.resources[buildType].resources.find(resource => resource.id === buildResourceId)
            return resource.name
        } catch (error) {
            console.warn(error, buildType, containerModal)
            return ''
        }
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
        return container && Array.isArray(container.elements) ? container.elements.map(element => {
            return Object.assign(element, {
                atomCode: element.atomCode && element['@type'] !== element.atomCode ? element.atomCode : element['@type']
            })
        }) : []
    },
    getElement: state => (container, index) => {
        const element = container && Array.isArray(container.elements) ? container.elements[index] : null
        if (element !== null) {
            typeof element.isError === 'undefined' && Vue.set(element, 'isError', false)
        }
        return element
    },
    isVmContainer: state => container => isVmContainer(container['@type']),
    isTriggerContainer: state => container => isTriggerContainer(container['@type']),
    isCodePullAtom: state => atom => isCodePullAtom(atom['@type']),
    isNormalContainer: state => container => isNormalContainer(container['@type']),
    buildNoRules: state => buildNoRules,
    defaultBuildNo: state => defaultBuildNo,
    getPlatformList: state => platformList,
    getAtomModalKey: state => getAtomModalKey,
    isNewAtomTemplate: state => isNewAtomTemplate
}
