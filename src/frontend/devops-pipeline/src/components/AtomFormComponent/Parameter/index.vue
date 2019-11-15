<template>
    <ul class="param-main" v-bkloading="{ isLoading }">
        <li class="param-input" v-for="(parameter, index) in parameters" :key="index">
            <parameter-input class="input-com" @updateValue="(newValue) => updateValue(parameter, newValue, 'key')" :param-values="paramValues" :url-query="parameter.keyUrlQuery" :multiple="parameter.keyMultiple" :value="parameter.key" :disabled="parameter.keyDisable" :type="parameter.keyType" :list-type="parameter.keyListType" :url="parameter.keyUrl" :list="parameter.keyList"></parameter-input>
            <span class="input-seg">=</span>
            <parameter-input class="input-com" @updateValue="(newValue) => updateValue(parameter, newValue, 'value')" :param-values="paramValues" :url-query="parameter.valueUrlQuery" :multiple="parameter.valueMultiple" :value="parameter.value" :disabled="parameter.valueDisable" :type="parameter.valueType" :list-type="parameter.valueListType" :url="parameter.valueUrl" :list="parameter.valueList"></parameter-input>
            <i class="bk-icon icon-minus-circle" v-if="param.hasAddButton" @click="minusClick(index)"></i>
            <i class="bk-icon icon-plus-circle" v-if="param.hasAddButton" @click="addClick(parameter, index)"></i>
        </li>
    </ul>
</template>

<script>
    import mixins from '../mixins'
    import parameterInput from './parameterInput'

    export default {
        name: 'parameter',

        components: {
            parameterInput
        },
        mixins: [mixins],

        props: {
            param: {
                type: Object
            }
        },

        data () {
            return {
                parameters: [],
                isLoading: false,
                queryKey: []
            }
        },

        computed: {
            paramValues () {
                const { atomValue = {}, $route: { params = {} } } = this
                return { ...params, ...atomValue }
            }
        },

        watch: {
            paramValues: {
                handler (value, oldValue) {
                    const index = this.queryKey.findIndex((key) => value[key] !== oldValue[key])
                    if (index > -1) {
                        this.atomValue[this.name] = []
                        this.initData()
                    }
                },
                deep: true
            }
        },

        created () {
            this.initData()
        },

        methods: {
            initData () {
                if (this.param.paramType === 'list') {
                    const list = this.atomValue[this.name] || this.param.parameters || []
                    this.parameters = [...list]
                    this.setValue()
                    return
                }

                this.addParams()
            },

            addClick (parameter, index) {
                const newData = JSON.parse(JSON.stringify(parameter))
                Object.assign(newData, { key: '', value: '' })
                this.parameters.splice(index + 1, 0, newData)
                this.handleChange(this.name, [...this.parameters])
            },

            minusClick (index) {
                this.parameters.splice(index, 1)
                this.handleChange(this.name, [...this.parameters])
            },

            addParams () {
                let url = this.param.url || ''
                let isErrorParam = false

                url.replace(/{([^\{\}]+)}/g, (str, key) => {
                    const value = this.paramValues[key]
                    if (typeof value === 'undefined') isErrorParam = true
                    return value
                })

                const urlQuery = this.param.urlQuery || {}
                this.queryKey = []
                Object.keys(urlQuery).forEach((key, index) => {
                    this.queryKey.push(key)
                    const value = typeof this.paramValues[key] === 'undefined' ? urlQuery[key] : this.paramValues[key]
                    url += `${index <= 0 ? '?' : '&'}${key}=${value}`
                })

                if (isErrorParam) return
                this.isLoading = true
                this.$ajax.get(url).then((res) => {
                    const data = res.data || []
                    this.parameters = data
                    this.setValue()
                }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.isLoading = false))
            },

            setValue () {
                const values = this.atomValue[this.name] || []
                const defaultValues = this.param.default || []

                this.parameters.forEach((param) => {
                    const key = param.key
                    const value = values.find(x => x.key === key) || {}
                    const defaultValue = defaultValues.find(x => x.key === key) || {}
                    param.value = value.value || defaultValue.value || param.value
                })
            },

            updateValue (parameter, newValue, type) {
                parameter[type] = newValue
                this.handleChange(this.name, [...this.parameters])
            }
        }
    }
</script>

<style lang="scss" scoped>
    .param-main {
        margin-top: 8px;
        .param-title {
            font-size: 12px;
            line-height: 30px;
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
        }
    }
    .bk-icon {
        margin-left: 5px;
        cursor: pointer;
    }
</style>
