<template>
    <div
        :class="['sub-parameter', {
            'sub-parameter-condition': hasOperatorList
        }]"
    >
        <label class="bk-label">
            <span>{{ title }}{{ hasOperatorList ? '' : '：' }}</span>
            <i
                v-if="desc && hasOperatorList"
                class="bk-icon icon-info-circle sub-params-desc-icon"
                v-bk-tooltips.top="{ content: desc, allowHTML: false }"
            />
            <span
                v-if="!disabled && !hasOperatorList"
                class="add-params-btn"
                @click="addParam"
            >
                <i class="devops-icon icon-plus-circle"></i>
                {{ displayAddBtnText }}
            </span>
        </label>
        <div
            v-if="desc && !hasOperatorList"
            class="sub-params-desc"
        >
            {{ desc }}
        </div>
        <div
            class="bk-form-content"
            v-if="parameters.length || hasOperatorList"
        >
            <ul
                v-if="parameters.length"
                v-bkloading="{ isLoading }"
            >
                <li
                    class="param-input"
                    v-for="(parameter, index) in parameters"
                    :key="parameter.id"
                    v-bk-tooltips="{
                        content: $t('notParamsTip'),
                        disabled: !parameter.disabled,
                        placements: ['left']
                    }"
                >
                    <template v-if="parameter.hasKey">
                        <bk-select
                            class="input-com"
                            :disabled="disabled"
                            :value="parameter.key"
                            :placeholder="keyPlaceholder"
                            @change="(val) => handleChangeKey(val, index)"
                        >
                            <bk-option
                                v-for="option in subParamsKeyList"
                                :key="option.key"
                                :id="option.key"
                                :name="option.key"
                                :disabled="parameters.find(i => i.key === option.key)"
                            />
                        </bk-select>
                    </template>
                    <template v-else>
                        <bk-input
                            v-model="parameter.key"
                            class="input-com param-not-key-input"
                            disabled
                            :title="parameter.key"
                        />
                    </template>
                    <bk-select
                        v-if="hasOperatorList"
                        class="input-operator"
                        :disabled="disabled || parameter.disabled"
                        :clearable="false"
                        :value="parameter.operator"
                        @change="(val) => handleChangeOperator(val, index)"
                    >
                        <bk-option
                            v-for="option in operatorList"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name"
                            :disabled="option.id === inOperator && !supportInOperator(parameter)"
                        />
                    </bk-select>
                    <span
                        v-else
                        class="input-seg"
                    >
                        =
                    </span>
                    <bk-select
                        v-if="isInOperator(parameter)"
                        v-model="parameter.value"
                        class="input-com"
                        :disabled="disabled || parameter.disabled"
                        :multiple="true"
                        :placeholder="valueSelectPlaceholder"
                        @change="(val) => handleChangeValue(val, index)"
                    >
                        <bk-option
                            v-for="option in getValueOptions(parameter)"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name"
                        />
                    </bk-select>
                    <bk-input
                        v-else
                        v-model="parameter.value"
                        :type="getInputType(parameter.type)"
                        :precision="0"
                        :class="['input-com', {
                            'param-not-key-input': parameter.disabled
                        }]"
                        :disabled="disabled || parameter.disabled"
                        :title="parameter.value"
                        :placeholder="valueInputPlaceholder"
                        @change="(val) => handleChangeValue(val, index)"
                    />
                    <i
                        v-if="!disabled"
                        class="bk-icon icon-minus-circle minus-btn"
                        @click="cutParam(index)"
                    />
                </li>
            </ul>
            <span
                v-if="!disabled && hasOperatorList"
                class="add-params-btn condition-add-btn"
                @click="addParam"
            >
                <i class="devops-icon icon-plus-circle"></i>
                {{ displayAddBtnText }}
            </span>
        </div>
    </div>
</template>

<script>
    import { isObject } from '@/utils/util'
    import { mapState } from 'vuex'
    import mixins from '../mixins'

    const DEFAULT_OPERATOR = '=='
    const IN_OPERATOR = 'IN'

    export default {
        name: 'sub-parameter',
        mixins: [mixins],
        props: {
            title: String,
            desc: String,
            param: Object,
            addBtnText: String,
            operatorList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                isLoading: false,
                parameters: [],
                subParamsKeyList: [],
                pipelineRequiredParams: {},
                paramIdCounter: 0
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo',
                'pipeline',
                'template'
            ]),
            paramValues () {
                const { atomValue = {}, $route: { params = {} } } = this
                const isTemplate = Object.prototype.hasOwnProperty.call(params, 'templateId')
                return {
                    bkPoolType: this?.container?.dispatchType?.buildType,
                    pipelineId: isTemplate ? params.templateId : '',
                    templateVersion: isTemplate ? this.template?.currentVersion?.version : '',
                    version: this.pipelineInfo?.version,
                    isTemplate,
                    ...params,
                    ...atomValue
                }
            },
            typeMap () {
                const map = new Map()
                this.subParamsKeyList.forEach(item => {
                    map.set(item.key, {
                        type: item.type,
                        defaultValue: item.value,
                        options: item.options || item.list || []
                    })
                })
                return map
            },
            container () {
                return this.pipeline?.stages[0]?.containers[0] || {}
            },
            hasOperatorList () {
                return this.operatorList.length > 0
            },
            inOperator () {
                return IN_OPERATOR
            },
            displayAddBtnText () {
                return this.addBtnText || this.$t('addParam')
            },
            keyPlaceholder () {
                return this.hasOperatorList ? this.$t('editPage.selectParamTips') : ''
            },
            valueInputPlaceholder () {
                return this.hasOperatorList ? this.$t('editPage.paramValueTips') : ''
            },
            valueSelectPlaceholder () {
                return this.hasOperatorList ? this.$t('editPage.selectParamValueTips') : this.$t('selectTips')
            },
            requiredParams () {
                const requiredParamList = this.container?.params?.filter(item => !item.constant && item.required) || []
                return requiredParamList.reduce((acc, current) => {
                    acc[current.id] = isObject(current.defaultValue) ? '' : current.defaultValue
                    acc[`variables.${current.id}`] = isObject(current.defaultValue) ? '' : current.defaultValue
                    if (isObject(current.defaultValue)) {
                        Object.keys(current.defaultValue).forEach(key => {
                            acc[`${current.id}.${key}`] = current.defaultValue[key]
                            acc[`variables.${current.id}.${key}`] = current.defaultValue[key]
                        })
                    }
                    return acc
                }, {})
            }
        },

        watch: {
            value: {
                handler (newVal, oldVal) {
                    if (newVal !== oldVal) {
                        this.$nextTick(() => {
                            this.initData()
                        })
                    }
                },
                deep: true
            },
            paramValues: {
                handler (value, oldValue) {
                    this.pipelineRequiredParams.subBranch = typeof value.subBranch === 'string' && this.getValidaVar(value.subBranch)
                        ? this.requiredParams[value.subBranch.extractBkVar()]
                        : value.subBranch
                    if (oldValue !== undefined && ((value?.subPip !== oldValue?.subPip) || (value?.subBranch !== oldValue?.subBranch))) {
                        this.atomValue[this.name] = []
                        this.getParametersList()
                        this.initData()
                    }
                },
                deep: true,
                immediate: true
            },
            subParamsKeyList (newVal) {
                if (newVal) {
                    this.$nextTick(() => {
                        this.initData()
                    })
                }
            }
        },
        created () {
            this.getParametersList()
            this.initData()
        },
        methods: {
            initData () {
                let values = this.atomValue[this.name] || this.value || []
                if (!Array.isArray(values)) values = JSON.parse(values)

                this.parameters = values.map((i, index) => {
                    const type = this.typeMap.get(i.key)?.type
                    const condition = this.parseConditionValue(i.value, i.operator)
                    // 如果 key 为空，说明是新增的参数，hasKey 应该为 true（可选择状态）
                    // 如果 key 不为空但在 typeMap 中找不到，说明是无效的 key，hasKey 为 false（禁用状态）
                    const hasKey = !i.key || !!type
                    const id = i.id || `param_${i.key || 'empty'}_${index}`
                    return {
                        ...i,
                        id,
                        type: type || i.key || 'text',
                        operator: condition.operator,
                        value: this.normalizeValue(condition.value, condition.operator),
                        hasKey,
                        disabled: !type && !!i.key
                    }
                })
            },
            addParam () {
                this.parameters.push({
                    id: `param_new_${this.paramIdCounter++}`,
                    key: '',
                    operator: DEFAULT_OPERATOR,
                    value: '',
                    hasKey: true
                })
            },
            cutParam (index) {
                this.parameters.splice(index, 1)
                this.updateParameters()
            },

            handleChangeKey (key, index) {
                this.parameters[index].key = isObject(key) ? JSON.stringify(key) : key
                const info = this.typeMap.get(key)
                const { type, defaultValue } = info || {}
                if (defaultValue) {
                    this.parameters[index].value = isObject(defaultValue) ? JSON.stringify(defaultValue) : defaultValue
                } else {
                    this.parameters[index].value = ''
                }
                this.parameters[index].type = type || 'text'
                if (this.isInOperator(this.parameters[index]) && !this.supportInOperator(this.parameters[index])) {
                    this.parameters[index].operator = DEFAULT_OPERATOR
                }
                this.updateParameters()
            },

            handleChangeOperator (operator, index) {
                this.parameters[index].operator = operator || DEFAULT_OPERATOR
                this.parameters[index].value = this.normalizeValue(
                    this.parameters[index].value,
                    this.parameters[index].operator
                )
                this.updateParameters()
            },

            handleChangeValue (val, index) {
                this.parameters[index].value = val
                this.updateParameters()
            },

            updateParameters () {
                const res = this.parameters.map((parameter) => {
                    const key = parameter.key
                    const value = isObject(parameter.value) ? JSON.stringify(parameter.value) : parameter.value
                    if (this.hasOperatorList) {
                        return {
                            key,
                            operator: parameter.operator || DEFAULT_OPERATOR,
                            value,
                            id: parameter.id
                        }
                    }
                    return { key, value, id: parameter.id }
                })
                this.handleChange(this.name, String(JSON.stringify(res)))
            },

            getParametersList () {
                if (this.param?.paramType === 'list' && Array.isArray(this.param.list)) {
                    this.subParamsKeyList = this.param.list
                    return
                }
                let [url] = this.generateReqUrl(this.param.url, this.paramValues)

                if (!url) return

                const urlQuery = this.param.urlQuery || {}
                Object.keys(urlQuery).forEach((key, index) => {
                    const value = typeof this.paramValues[key] === 'undefined'
                        ? urlQuery[key]
                        : this.pipelineRequiredParams[key] ?? this.paramValues[key]
                    url += `${index <= 0 ? '?' : '&'}${encodeURIComponent(key)}=${encodeURIComponent(value)}`
                })
                const pipelineInfoQuery = this.param.pipelineInfoQuery || {}
                this.pipelineInfo && Object.keys(pipelineInfoQuery).forEach(key => {
                    const value = typeof this.pipelineInfo[key] === 'undefined' ? pipelineInfoQuery[key] : this.pipelineInfo[key]
                    Object.keys(urlQuery).length ? url += `&${key}=${value}` : url += `?${key}=${value}`
                })
                this.isLoading = true
                this.$ajax.get(url).then((res) => {
                    this.subParamsKeyList = res.data?.properties || res.data || []
                }).catch(e => {
                    this.$bkMessage({
                        message: this.$createElement('li', {
                            class: 'sub-pipeline-check-error-list',
                            domProps: {
                                innerHTML: e.message
                            }
                        }),
                        theme: 'error',
                        ellipsisLine: 20
                    })
                }).finally(() => (this.isLoading = false))
            },
            getInputType (type) {
                const typeMap = {
                    textarea: 'textarea',
                    long: 'text',
                    input: 'text'
                }

                return typeMap[type] || 'text'
            },

            parseConditionValue (rawValue, operator) {
                if (!this.hasOperatorList) {
                    return {
                        operator: operator || DEFAULT_OPERATOR,
                        value: rawValue
                    }
                }

                try {
                    const condition = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
                    if (isObject(condition) && condition.operator) {
                        return {
                            operator: this.normalizeOperator(condition.operator),
                            value: condition.value ?? ''
                        }
                    }
                } catch (error) {
                    // 兼容旧版 displayCondition: { key: value }
                }

                return {
                    operator: this.normalizeOperator(operator),
                    value: rawValue
                }
            },

            normalizeOperator (operator) {
                const operatorMap = {
                    STARTWITH: 'STARTS_WITH',
                    ENDWITH: 'ENDS_WITH'
                }
                const value = String(operator || DEFAULT_OPERATOR).trim().toUpperCase()
                return operatorMap[value] || value
            },

            normalizeValue (value, operator) {
                if (operator === IN_OPERATOR) {
                    if (Array.isArray(value)) {
                        return value
                    }
                    return value ? [value] : []
                }
                if (Array.isArray(value)) {
                    return value[0] || ''
                }
                return isObject(value) ? JSON.stringify(value) : value
            },

            isInOperator (parameter) {
                return this.hasOperatorList && parameter.operator === IN_OPERATOR
            },

            supportInOperator (parameter) {
                return this.getValueOptions(parameter).length > 0
            },

            getValueOptions (parameter) {
                const options = this.typeMap.get(parameter.key)?.options || []
                return options.map(option => ({
                    id: option.id || option.key || option.value,
                    name: option.name || option.value || option.key || option.id
                })).filter(option => option.id)
            }
        }
    }
</script>

<style lang="scss" scoped>
    .sub-parameter {
        display: grid;
    }
    .sub-params-desc {
        display: inline-flex;
        color: #979BA5;
    }
    .sub-params-desc-icon {
        margin-left: 6px;
        color: #979BA5;
        font-size: 16px;
        cursor: pointer;
    }
    .add-params-btn {
        color: #3A84FF;
        cursor: pointer;
        display: inline-flex;
        align-items: center;

        .devops-icon {
            margin-right: 6px;
            font-size: 16px;
        }
    }
    .param-input {
        margin-bottom: 10px;
        display: flex;
        align-items: center;
      
        .input-com {
            flex: 1;
        }
        .input-seg {
            flex-basis: 20px;
            text-align: center;
        }
        .input-operator {
            flex-basis: 96px;
            margin: 0 6px;
        }
        .minus-btn {
            font-size: 14px;
            margin-left: 5px;
            cursor: pointer;
        }
    }
    .sub-parameter-condition {
        .bk-label {
            display: inline-flex;
            align-items: center;
            margin-bottom: 8px;
            line-height: 20px;
        }

        .bk-form-content {
            padding: 12px 16px;
            background: #F5F7FA;
        }

        ul {
            padding: 0;
            margin: 0;
        }

        .param-input {
            margin-bottom: 12px;
            gap: 0;

            &:last-child {
                margin-bottom: 0;
            }

            .input-com {
                flex: 1 1 0;
                min-width: 0;
            }

            .input-operator {
                flex: 0 0 88px;
                margin: 0;
            }

            .minus-btn {
                margin-left: 8px;
            }
        }

        .condition-add-btn {
            display: flex;
            width: fit-content;
            margin-top: 8px;
        }
    }
</style>

<style lang="scss">
    @import '@/scss/conf';
    .param-not-key-input {
        .bk-form-input {
            text-decoration: line-through !important;
        }
    }
    .sub-pipeline-check-error-list {
        a {
            color: $primaryColor;
            text-align: right;
        }
    }
</style>
