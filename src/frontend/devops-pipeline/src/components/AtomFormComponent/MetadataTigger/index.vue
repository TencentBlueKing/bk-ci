<template>
    <div class="sub-parameter">
        <div class="bk-form-content">
            <ul v-bkloading="{ isLoading }">
                <li
                    class="param-input"
                    v-for="(parameter, index) in paramValues"
                    :key="index"
                >
                    <select-input
                        class="input-com"
                        v-model="parameter.key"
                        :name="`metadata-tigger-key-${index}`"
                        :placeholder="$t('keyPlaceholder')"
                        :disabled="disabled"
                        :options="keyList"
                        type="text"
                        :handle-change="(name, value) => handleChangeKey(value, index)"
                    />
                    <bk-select
                        class="input-operator"
                        :disabled="disabled"
                        :clearable="false"
                        :value="parameter.operator"
                        @change="(val) => handleChangeOperator(val, index)"
                    >
                        <bk-option
                            v-for="option in DISPLAY_CONDITION_OPERATORS"
                            :key="option.id"
                            :id="option.id"
                            :name="option.name"
                        />
                    </bk-select>
                    <select-input
                        class="input-com"
                        v-model="parameter.value"
                        :name="`metadata-tigger-value-${index}`"
                        :placeholder="$t('valuePlaceholder')"
                        :disabled="disabled"
                        :options="getValueListByIndex(index)"
                        type="text"
                        :handle-change="(name, value) => handleChangeValue(value, index)"
                    />
                    <i
                        v-if="!disabled"
                        class="bk-icon icon-minus-circle minus-btn"
                        @click="cutParam(index)"
                    />
                </li>
            </ul>
            <span
                v-if="!disabled"
                class="add-params-btn"
                @click="addParam"
            >
                <i class="devops-icon icon-plus-circle"></i>
                {{ $t('editPage.append') }}
            </span>
        </div>
    </div>
</template>

<script>
    import { isObject, debounce } from '@/utils/util'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import mixins from '../mixins'
    import selectorMixins from '../selectorMixins'
    import {
        DEFAULT_DISPLAY_CONDITION_OPERATOR,
        DISPLAY_CONDITION_OPERATORS
    } from '@/store/modules/atom/paramsConfig'

    export default {
        name: 'metadata-tigger',
        components: {
            SelectInput
        },
        mixins: [mixins, selectorMixins],
        props: {
            default: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                isLoading: false,
                paramValues: [],
                optionList: [],
                DISPLAY_CONDITION_OPERATORS
            }
        },
        computed: {
            keyList () {
                const listSource = this.hasUrl ? this.optionList : this.options
                return listSource.map(item => ({
                    id: item.key,
                    name: item.key
                }))
            }
        },
        watch: {
            queryParams (newQueryParams, oldQueryParams) {
                if (this.isParamsChanged(newQueryParams, oldQueryParams)) {
                    this.debounceGetOptionList()
                }
            },
            value: {
                handler (newVal) {
                    if (newVal && newVal.length) {
                        const value = typeof newVal === 'string' ? JSON.parse(newVal) : newVal
                        this.paramValues = value.map(item => ({
                            key: item.key || '',
                            operator: item.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
                            value: item.value || ''
                        }))
                    } else {
                        this.paramValues = this.getDefaultParams()
                    }
                },
                immediate: true,
                deep: true
            }
        },
        created () {
            if (this.hasUrl) {
                this.getOptionList()
                this.debounceGetOptionList = debounce(this.getOptionList)
            }
        },
        methods: {
            getValueListByIndex (index) {
                const key = this.paramValues[index]?.key
                if (!key) return []

                const listSource = this.hasUrl ? this.optionList : this.options
                const keyItem = listSource.find(item => item.key === key)

                return (keyItem?.values || []).map(item => ({
                    id: item,
                    name: item
                }))
            },
            getDefaultParams () {
                if (this.default?.length) {
                    return this.default.map(item => ({
                        key: item.key || '',
                        operator: item.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
                        value: item.value || ''
                    }))
                }
                return []
            },
            addParam () {
                this.paramValues.push({
                    key: '',
                    operator: DEFAULT_DISPLAY_CONDITION_OPERATOR,
                    value: ''
                })
                this.updateParameters()
            },
            handleChangeKey (value, index) {
                this.paramValues[index].key = value
                if (this.paramValues[index].value) {
                    this.paramValues[index].value = ''
                }
                this.updateParameters()
            },
            handleChangeOperator (operator, index) {
                this.paramValues[index].operator = operator || DEFAULT_DISPLAY_CONDITION_OPERATOR
                this.updateParameters()
            },
            handleChangeValue (value, index) {
                this.paramValues[index].value = value
                this.updateParameters()
            },
            updateParameters () {
                const res = this.paramValues.map((parameter) => {
                    const key = parameter.key
                    const value = isObject(parameter.value) ? JSON.stringify(parameter.value) : parameter.value
                    return {
                        key,
                        operator: parameter.operator || DEFAULT_DISPLAY_CONDITION_OPERATOR,
                        value
                    }
                })
                this.handleChange(this.name, res)
            },
            cutParam (index) {
                this.paramValues.splice(index, 1)
                this.updateParameters()
            },
            async getOptionList () {
                if (this.isLackParam) {
                    if (this.value) {
                        this.paramValues = typeof this.value === 'string' ? JSON.parse(this.value) : this.value
                    }
                    this.optionList = []
                    return
                }
                try {
                    this.isLoading = true
                    const { mergedOptionsConf: { url, paramId, paramName, dataPath }, queryParams, urlParse, getResponseData } = this
                    const reqUrl = urlParse(url, queryParams)
                    const res = await this.$ajax.get(reqUrl)
                    const options = getResponseData(res, dataPath)
                    this.optionList = options.map(item => {
                        if (isObject(item)) {
                            return {
                                ...item,
                                key: item[paramId],
                                values: item[paramName]
                            }
                        }
                        return {
                            key: item,
                            values: []
                        }
                    })
                } catch (e) {
                    console.error(e)
                    const value = typeof this.value === 'string' ? JSON.parse(this.value) : this.value
                    this.paramValues = value || []
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .sub-parameter {
        display: grid;
        .add-params-btn {
            color: #3A84FF;
            cursor: pointer;
        }
        .param-input {
            margin-bottom: 10px;
            display: flex;
            align-items: self-start;

            .input-com {
                flex: 1;
            }
            .input-operator {
                flex-basis: 96px;
                margin: 0 6px;
                background-color: #fff;
            }
            .minus-btn {
                color: #c4c6cc;
                font-size: 14px;
                margin-left: 5px;
                cursor: pointer;
                line-height: 32px;
            }
        }
    }
</style>
