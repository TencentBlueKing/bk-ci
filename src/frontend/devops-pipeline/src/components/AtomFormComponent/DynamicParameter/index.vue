<template>
    <ul class="param-main" v-bkloading="{ isLoading }">
        <li class="param-input" v-for="(parameter, paramIndex) in parameters" :key="paramIndex">
            <parameter-input v-for="(model, index) in parameter.paramModels"
                :style="{ maxWidth: `calc(${100 / parameter.paramModels.length}% - ${58 / parameter.paramModels.length}px)` }"
                :key="model.id"
                :class="[{ 'last-child': index === parameter.paramModels.length - 1 }, 'input-com']"
                @update-value="(newValue) => updateValue(model, newValue)"
                :param-values="paramValues"
                v-bind="model"
            ></parameter-input>
            <i class="bk-icon icon-plus-circle" @click="plusParam(parameter, paramIndex)"></i>
            <i class="bk-icon icon-minus-circle" v-if="parameters.length > 1" @click="minusParam(paramIndex)"></i>
        </li>
    </ul>
</template>

<script>
    import mixins from '../mixins'
    import parameterInput from './parameterInput'

    export default {
        name: 'dynamic-parameter',

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
                return {
                    bkPoolType: this?.container?.dispatchType?.buildType,
                    ...params,
                    ...atomValue
                }
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
            plusParam (parameter, index) {
                this.parameters.splice(index, 0, JSON.parse(JSON.stringify(parameter)))
                this.updateParameters()
            },

            minusParam (index) {
                this.parameters.splice(index, 1)
                this.updateParameters()
            },

            initData () {
                if (this.param.paramType === 'list') {
                    const list = this.param.parameters || []
                    this.parameters = JSON.parse(JSON.stringify(list))
                    this.setValue()
                    return
                }

                this.addParams()
            },

            addParams () {
                // let url = this.param.url || ''
                if (typeof this.param.url === 'string' && this.param.url !== '') {
                    const [url, queryKey] = this.generateReqUrl(this.param.url, this.paramValues)
                    this.queryKey = queryKey
                    // url = url.replace(/{([^\{\}]+)}/g, (str, key) => {
                    //     const value = this.paramValues[key]
                    //     this.queryKey.push(key)
                    //     if (typeof value === 'undefined') isErrorParam = true
                    //     return value
                    // })
                    if (!url) return
                    this.isLoading = true
                    this.$ajax.get(url).then((res) => {
                        this.parameters = this.getResponseData(res, this.param.dataPath || 'data.records')
                        this.setValue()
                    }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.isLoading = false))
                }
            },

            setValue () {
                let values = this.atomValue[this.name] || []
                if (!Array.isArray(values)) values = JSON.parse(values)

                if (values.length) {
                    this.parameters = values.map((value) => {
                        const modelId = value.id
                        const originModel = this.parameters.find(x => x.id === modelId)
                        if (!originModel) return undefined
                        const currentModel = JSON.parse(JSON.stringify(originModel))
                        const paramModels = currentModel.paramModels
                        const values = value.values
                        paramModels.forEach((model) => {
                            const currentValue = values.find(x => x.id === model.id) || {}
                            if (Array.isArray(currentValue.value)) currentValue.value = currentValue.value.filter(v => v !== '')
                            model.value = currentValue.value === undefined ? '' : currentValue.value
                        })
                        return currentModel
                    })
                    this.parameters = this.parameters.filter(x => x !== undefined)
                }

                this.updateParameters()
            },

            updateValue (model, newValue) {
                model.value = newValue
                this.updateParameters()
            },

            updateParameters () {
                const res = this.parameters.map((parameter) => {
                    const id = parameter.id
                    const paramModels = parameter.paramModels
                    const values = paramModels.map((model) => ({ id: model.id, value: model.value }))
                    return { id, values }
                })
                this.handleChange(this.name, String(JSON.stringify(res)))
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
                margin-right: 10px;
                &.last-child {
                    margin-right: 0;
                }
            }
        }
    }
    .bk-icon {
        margin-left: 5px;
        font-size: 14px;
        cursor: pointer;
    }
</style>
