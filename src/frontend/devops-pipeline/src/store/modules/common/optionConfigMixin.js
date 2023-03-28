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

// 根据配置文件获取原子流程控制选项的默认值
const optionConfigMixin = {
    data () {
        return {
            ATOM_OPTION: {
                enable: {
                    rule: {},
                    type: 'boolean',
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.enableAtom'),
                    default: true
                },
                continueWhenFailed: {
                    isHidden: true,
                    default: false
                },

                retryWhenFailed: {
                    isHidden: true,
                    default: false
                },
                failControl: {
                    component: 'atom-checkbox-list',
                    label: this.$t('storeMap.WhenPluginFailed'),
                    default: [],
                    list: [
                        {
                            id: 'continueWhenFailed',
                            name: this.$t('storeMap.continueWhenFailed')
                        },
                        {
                            id: 'retryWhenFailed',
                            name: this.$t('storeMap.automaticRetry')
                        },
                        {
                            id: 'MANUAL_RETRY',
                            name: this.$t('storeMap.manualRetry')
                        }
                    ]
                },
                manualSkip: {
                    component: 'enum-input',
                    // label: this.$t('storeMap.skipType'),
                    default: false,
                    list: [{
                        value: false,
                        label: this.$t('storeMap.autoSkip')
                    }, {
                        value: true,
                        label: this.$t('storeMap.manualSkip')
                    }],
                    extCls: 'manual-skip-options',
                    isHidden: (element) => {
                        return !(element.additionalOptions && ((element.additionalOptions.failControl || []).includes('continueWhenFailed')))
                    }
                },
                manualRetry: {
                    default: false,
                    isHidden: true
                },
                retryCount: {
                    rule: { numeric: true, max_value: 5, min_value: 1 },
                    component: 'vuex-input',
                    label: this.$t('storeMap.retryCount'),
                    placeholder: this.$t('storeMap.retryCountPlaceholder'),
                    default: '1',
                    extCls: 'retry-count-input',
                    isHidden: (element) => {
                        return !(element.additionalOptions && ((element.additionalOptions.failControl || []).includes('retryWhenFailed')))
                    }
                },
                enableCustomEnv: {
                    rule: {},
                    type: 'boolean',
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.customEnv'),
                    default: false
                },
                pauseBeforeExec: {
                    rule: {},
                    type: 'boolean',
                    label: this.$t('storeMap.pauseConfLabel'),
                    desc: this.$t('storeMap.runManual'),
                    component: 'atom-checkbox',
                    text: this.$t('storeMap.pauseAtom'),
                    default: false,
                    extCls: 'pause-conf-options',
                    isHidden: (element) => {
                        return !(element.data && element.data.config && (element.data.config.canPauseBeforeRun === true))
                    }
                },
                subscriptionPauseUser: {
                    rule: {},
                    component: 'vuex-input',
                    label: this.$t('storeMap.pauseNotify'),
                    desc: this.$t('storeMap.pauseNotifyTip'),
                    default: this.$userInfo.username,
                    extCls: 'pause-conf-user',
                    isHidden: (element) => {
                        return !(element.additionalOptions && (element.additionalOptions.pauseBeforeExec === true))
                    }
                },
                timeoutVar: {
                    rule: { timeoutsRule: true },
                    component: 'vuex-input',
                    label: this.$t('storeMap.atomTimeout'),
                    desc: this.$t('storeMap.timeoutDesc'),
                    placeholder: this.$t('storeMap.timeoutPlaceholder'),
                    default: '900'
                },
                runCondition: {
                    rule: {},
                    component: 'selector',
                    label: this.$t('storeMap.atomRunCondition'),
                    default: 'PRE_TASK_SUCCESS',
                    list: [
                        {
                            id: 'PRE_TASK_SUCCESS',
                            name: this.$t('storeMap.atomPreSuc')
                        },
                        {
                            id: 'PRE_TASK_FAILED_BUT_CANCEL',
                            name: this.$t('storeMap.atomEvenFail')
                        },
                        {
                            id: 'PRE_TASK_FAILED_EVEN_CANCEL',
                            name: this.$t('storeMap.atomEvenCancel')
                        },
                        {
                            id: 'PRE_TASK_FAILED_ONLY',
                            name: this.$t('storeMap.atomOnlyFail')
                        },
                        {
                            id: 'CUSTOM_VARIABLE_MATCH',
                            name: this.$t('storeMap.varMatch')
                        },
                        {
                            id: 'CUSTOM_VARIABLE_MATCH_NOT_RUN',
                            name: this.$t('storeMap.varNotMatch')
                        },
                        // {
                        //     id: 'CUSTOM_CONDITION_MATCH',
                        //     name: this.$t('storeMap.customCondition')
                        // },
                        {
                            id: 'PARENT_TASK_CANCELED_OR_TIMEOUT',
                            name: this.$t('storeMap.userCancelExec')
                        }
                    ]
                },
                customVariables: {
                    rule: {},
                    component: 'key-value-normal',
                    default: [{ key: 'param1', value: '' }],
                    allowNull: false,
                    label: this.$t('storeMap.customVar'),
                    isHidden: (element) => {
                        return !(element.additionalOptions && (element.additionalOptions.runCondition === 'CUSTOM_VARIABLE_MATCH' || element.additionalOptions.runCondition === 'CUSTOM_VARIABLE_MATCH_NOT_RUN'))
                    }
                },
                customEnv: {
                    rule: {},
                    component: 'key-value-normal',
                    default: [{ key: 'param1', value: '' }],
                    allowNull: false,
                    label: this.$t('storeMap.customEnv'),
                    isHidden (element) {
                        return !(element.additionalOptions && element.additionalOptions.enableCustomEnv === true)
                    }
                },
                customCondition: {
                    rule: {},
                    component: 'vuex-input',
                    default: '',
                    allowNull: false,
                    label: this.$t('storeMap.customVar'),
                    isHidden: (element) => {
                        return !(element.additionalOptions && element.additionalOptions.runCondition === 'CUSTOM_CONDITION_MATCH')
                    }
                },
                otherTask: {
                    isHidden: true,
                    default: ''
                }
            }
        }
    },
    methods: {
        getAtomOptionDefault (additionalOptions) {
            const atomValues = Object.keys(this.ATOM_OPTION).reduce((formProps, key) => {
                if (typeof additionalOptions[key] !== 'undefined') {
                    formProps[key] = additionalOptions[key]
                } else if (this.ATOM_OPTION[key] && typeof this.ATOM_OPTION[key].default === 'object') {
                    formProps[key] = JSON.parse(JSON.stringify(this.ATOM_OPTION[key].default))
                } else {
                    formProps[key] = this.ATOM_OPTION[key].default
                }

                return formProps
            }, {})

            atomValues.failControl = [
                ...(atomValues.continueWhenFailed ? ['continueWhenFailed'] : []),
                ...(atomValues.retryWhenFailed ? ['retryWhenFailed'] : []),
                ...(atomValues.manualRetry ? ['MANUAL_RETRY'] : [])
            ]
            return atomValues
        }
    }
}

export default optionConfigMixin
