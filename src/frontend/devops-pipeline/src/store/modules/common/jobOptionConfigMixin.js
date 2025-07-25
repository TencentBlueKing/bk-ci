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

// 根据配置文件获取job流程控制选项的默认值
const jobOptionConfigMixin = {
    data () {
        return {
            JOB_MUTUAL: {
                enable: {
                    default: false
                },
                mutexGroupName: {
                    component: 'vuex-input',
                    label: this.$t('storeMap.mutualGroupName'),
                    placeholder: this.$t('storeMap.mutualGroupNamePlaceholder'),
                    default: '',
                    required: true
                },
                queueEnable: {
                    rule: {},
                    type: 'boolean',
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.queueEnable'),
                    default: false
                },
                timeoutVar: {
                    rule: { timeoutsRule: true },
                    component: 'vuex-input',
                    label: this.$t('storeMap.mutualTimeout'),
                    desc: this.$t('storeMap.timeoutDesc'),
                    placeholder: this.$t('storeMap.mutualTimeoutPlaceholder'),
                    default: '900',
                    required: true,
                    isHidden: (mutexGroup) => {
                        return !(mutexGroup && mutexGroup.queueEnable)
                    }
                },
                queue: {
                    rule: { numeric: true, max_value: 50, min_value: 1 },
                    component: 'vuex-input',
                    label: this.$t('storeMap.queueLabel'),
                    placeholder: this.$t('storeMap.queuePlaceholder'),
                    default: '5',
                    required: true,
                    isHidden: (mutexGroup) => {
                        return !(mutexGroup && mutexGroup.queueEnable)
                    }
                }
            },
            JOB_MATRIX: {
                strategyStr: {
                    required: true,
                    rule: {},
                    component: 'atom-ace-editor',
                    lang: 'yaml',
                    defaultHeight: 100,
                    label: this.$t('storeMap.strategy'),
                    desc: this.$t('storeMap.strategyDesc'),
                    default: ''
                },
                includeCaseStr: {
                    rule: {},
                    component: 'atom-ace-editor',
                    lang: 'yaml',
                    defaultHeight: 100,
                    label: this.$t('storeMap.includeCase'),
                    desc: this.$t('storeMap.includeCaseDesc'),
                    default: ''
                },
                excludeCaseStr: {
                    rule: {},
                    component: 'atom-ace-editor',
                    lang: 'yaml',
                    defaultHeight: 100,
                    label: this.$t('storeMap.excludeCase'),
                    desc: this.$t('storeMap.excludeCaseDesc'),
                    default: ''
                },
                fastKill: {
                    rule: {},
                    type: 'boolean',
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.fastKill'),
                    // desc: this.$t('storeMap.fastKillDesc'),
                    default: true
                },
                maxConcurrency: {
                    rule: { numeric: true, min_value: 1, max_value: 20 },
                    component: 'vuex-input',
                    required: true,
                    label: this.$t('storeMap.maxConcurrency'),
                    placeholder: this.$t('storeMap.maxConcurrencyDesc'),
                    default: '5'
                }
            },
            normalRunConditionList: [
                {
                    id: 'STAGE_RUNNING',
                    name: this.$t('storeMap.stageRunning')
                },
                {
                    id: 'CUSTOM_VARIABLE_MATCH',
                    name: this.$t('storeMap.varMatch')
                },
                {
                    id: 'CUSTOM_VARIABLE_MATCH_NOT_RUN',
                    name: this.$t('storeMap.varNotMatch')
                },
                {
                    id: 'CUSTOM_CONDITION_MATCH',
                    name: this.$t('storeMap.customCondition')
                }
            ],
            finallyRunConditionList: [
                {
                    id: 'STAGE_RUNNING',
                    name: this.$t('storeMap.stageRunning')
                },
                {
                    id: 'PREVIOUS_STAGE_SUCCESS',
                    name: this.$t('storeMap.preStageSuccess')
                },
                {
                    id: 'PREVIOUS_STAGE_FAILED',
                    name: this.$t('storeMap.preStageFail')
                },
                {
                    id: 'PREVIOUS_STAGE_CANCEL',
                    name: this.$t('storeMap.preStageCancel')
                }
            ]
        }
    },
    computed: {
        JOB_OPTION () {
            return {
                enable: {
                    rule: {},
                    type: 'boolean',
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.enableJob'),
                    default: true
                },
                // enableCustomEnv: {
                //     rule: {},
                //     type: 'boolean',
                //     component: 'atom-checkbox',
                //     text: this.$t('storeMap.customEnv'),
                //     default: false,
                //     clearValue: false,
                //     clearFields: ['customEnv']
                // },
                dependOnType: {
                    component: 'enum-input',
                    label: this.$t('storeMap.dependOn'),
                    desc: this.$t('storeMap.dependOnDesc'),
                    default: 'ID',
                    list: [
                        {
                            label: this.$t('storeMap.dependOnId'),
                            value: 'ID'
                        },
                        {
                            label: this.$t('storeMap.dependOnName'),
                            value: 'NAME'
                        }
                    ]
                },
                dependOnId: {
                    component: 'selector',
                    default: [],
                    multiSelect: true,
                    list: this.dependOnList,
                    isHidden: (container) => {
                        const jobOption = container.jobControlOption || {}
                        return !(jobOption && (jobOption.dependOnType === 'ID' || !jobOption.dependOnType))
                    }
                },
                dependOnName: {
                    rule: {},
                    component: 'vuex-input',
                    default: '',
                    placeholder: this.$t('storeMap.dependOnNamePlaceholder'),
                    isHidden: (container) => {
                        const jobOption = container.jobControlOption || {}
                        return !(jobOption && jobOption.dependOnType === 'NAME')
                    }
                },
                maxConcurrency: {
                    type: 'group',
                    label: this.$t('storeMap.maxConcurrency'),
                    isHidden: (container) => {
                        const dispatchType = container.dispatchType || {}
                        return dispatchType.buildType !== 'THIRD_PARTY_AGENT_ENV'
                    },
                    children: [
                        {
                            key: 'singleNodeConcurrency',
                            rule: { maxConcurrencyRule: true },
                            type: 'groupItem',
                            component: 'vuex-input',
                            labelWidth: 150,
                            label: this.$t('storeMap.singleNodeConcurrency'),
                            placeholder: this.$t('storeMap.singleNodeConcurrencyPlaceholder')
                        },
                        {
                            key: 'allNodeConcurrency',
                            rule: { maxConcurrencyRule: true },
                            type: 'groupItem',
                            component: 'vuex-input',
                            labelWidth: 150,
                            label: this.$t('storeMap.allNodeConcurrency'),
                            placeholder: this.$t('storeMap.allNodeConcurrencyPlaceholder')
                        }
                    ]
                },
                timeoutPeriod: {
                    type: 'group',
                    label: this.$t('storeMap.timeoutPeriod'),
                    isHidden: (container) => {
                        const dispatchType = container.dispatchType || {}
                        return dispatchType.buildType !== 'THIRD_PARTY_AGENT_ENV'
                    },
                    children: [
                        {
                            key: 'prepareTimeout',
                            type: 'groupItem',
                            rule: { timeoutsRule: true },
                            component: 'composite-input',
                            appendText: this.$t('storeMap.minutes'),
                            labelWidth: 90,
                            width: 150,
                            required: true,
                            label: this.$t('storeMap.queueTimeOut'),
                            iconDesc: this.$t('storeMap.prepareTimeoutDesc'),
                            default: '10'
                        },
                        {
                            key: 'timeoutVar',
                            type: 'groupItem',
                            rule: { timeoutsRule: true },
                            component: 'composite-input',
                            appendText: this.$t('storeMap.minutes'),
                            labelWidth: 90,
                            width: 150,
                            required: true,
                            label: this.$t('storeMap.execTimeOut'),
                            iconDesc: this.$t('storeMap.timeoutDesc'),
                            default: '900'
                        }
                    ]
                },
                timeoutVar: {
                    rule: { timeoutsRule: true },
                    component: 'vuex-input',
                    required: true,
                    label: this.$t('storeMap.jobTimeout'),
                    desc: this.$t('storeMap.timeoutDesc'),
                    placeholder: this.$t('storeMap.timeoutPlaceholder'),
                    default: '900',
                    isHidden: (container) => {
                        const dispatchType = container.dispatchType || {}
                        return dispatchType.buildType === 'THIRD_PARTY_AGENT_ENV'
                    }
                },
                runCondition: {
                    rule: {},
                    component: 'selector',
                    label: this.$t('storeMap.jobRunCondition'),
                    default: 'STAGE_RUNNING',
                    list: this.stage.finally ? this.finallyRunConditionList : this.normalRunConditionList
                },
                customVariables: {
                    rule: {},
                    component: 'key-value-normal',
                    default: [{ key: 'param1', value: '' }],
                    label: this.$t('storeMap.customVar'),
                    allowNull: false,
                    isHidden: (container) => {
                        const jobOption = container.jobControlOption || {}
                        return !(jobOption && (jobOption.runCondition === 'CUSTOM_VARIABLE_MATCH' || jobOption.runCondition === 'CUSTOM_VARIABLE_MATCH_NOT_RUN'))
                    }
                },
                customCondition: {
                    component: 'vuex-input',
                    default: '',
                    required: true,
                    label: this.$t('storeMap.customConditionExp'),
                    docsLink: this.customExpressionsDoc,
                    isHidden: (container) => {
                        return container?.jobControlOption?.runCondition !== 'CUSTOM_CONDITION_MATCH'
                    },
                    placeholder: this.$t('storeMap.customConditionExpPlaceholder')
                }
            }
        },
        dependOnList () {
            const list = []
            // if (!this.stage.containers || this.stage.containers.length <= 1) return list
            this.stage.containers && this.stage.containers.forEach((container, index) => {
                if (index !== this.containerIndex) {
                    list.push(
                        {
                            id: container.jobId || Math.random(),
                            name: `Job${this.stageIndex + 1}-${index + 1}${!container.jobId ? ' (该job未设置Job ID)' : ' (Job ID: ' + container.jobId + ')'} `,
                            disabled: !container.jobId
                        }
                    )
                }
            })
            return list
        },
        customExpressionsDoc () {
            return this.$pipelineDocs.CUSTOM_EXPRESSIONS_DOC
        }
    },
    methods: {
        getJobOptionDefault (model, values) {
            return Object.keys(model).reduce((formProps, key) => {
                if (!Object.prototype.hasOwnProperty.apply(values, [key])) {
                    formProps[key] = this.getFieldDefault(key, model)
                }
                return formProps
            }, {
                ...values
            })
        },
        getFieldDefault (key, model) {
            if (typeof model[key]?.default === 'object') {
                return JSON.parse(JSON.stringify(model[key].default))
            }
            return model[key].default
        }
    }
}

export default jobOptionConfigMixin
