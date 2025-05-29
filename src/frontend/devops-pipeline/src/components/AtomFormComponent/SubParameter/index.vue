<template>
    <div class="sub-parameter">
        <label class="bk-label">
            {{ title }}：
            <span
                v-if="!disabled"
                class="add-params-btn"
                @click="addParam"
            >
                <i class="devops-icon icon-plus-circle"></i>
                {{ $t('addParam') }}
            </span>
        </label>
        <div class="sub-params-desc">{{ desc }}</div>
        <div
            class="bk-form-content"
            v-if="parameters.length"
        >
            <ul v-bkloading="{ isLoading }">
                <li
                    class="param-input"
                    v-for="(parameter, index) in parameters"
                    :key="parameter.key"
                >
                    <bk-select
                        class="input-com"
                        :disabled="disabled"
                        :value="parameter.key"
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
                    <span class="input-seg">=</span>
                    <bk-input
                        v-model="parameter.value"
                        :type="parameter.type === 'textarea' ? 'textarea' : 'text'"
                        class="input-com"
                        :disabled="disabled"
                        :title="parameter.value"
                        @change="(val) => handleChangeValue(val, index)"
                    />
                    <i
                        v-if="!disabled"
                        class="bk-icon icon-minus-circle minus-btn"
                        @click="cutParam(index)"
                    />
                </li>
            </ul>
        </div>
    </div>
</template>

<script>
    import mixins from '../mixins'
    import { isObject } from '@/utils/util'
    import { mapState } from 'vuex'
    export default {
        name: 'sub-parameter',
        mixins: [mixins],
        props: {
            title: String,
            desc: String,
            param: Object
        },
        data () {
            return {
                isLoading: false,
                parameters: [],
                subParamsKeyList: []
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            paramValues () {
                const { atomValue = {}, $route: { params = {} } } = this
                return {
                    bkPoolType: this?.container?.dispatchType?.buildType,
                    ...params,
                    ...atomValue
                }
            },
            typeMap () {
                const map = new Map()
                this.subParamsKeyList.forEach(item => {
                    map.set(item.key, {
                        type: item.type,
                        defaultValue: item.value
                    })
                })
                return map
            }
        },

        watch: {
            paramValues: {
                handler (value, oldValue) {
                    if (value.subPip !== oldValue.subPip) {
                        this.atomValue[this.name] = []
                        this.getParametersList()
                        this.initData()
                    }
                },
                deep: true
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

                this.parameters = values.map(i => {
                    return {
                        ...i,
                        type: this.typeMap.get(i.key).type || 'text',
                        value: isObject(i.value) ? JSON.stringify(i.value) : i.value
                    }
                })
            },
            addParam () {
                this.parameters.push({
                    key: '',
                    value: ''
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
                    return { key, value }
                })
                this.handleChange(this.name, String(JSON.stringify(res)))
            },

            getParametersList () {
                let [url] = this.generateReqUrl(this.param.url, this.paramValues)

                if (!url) return

                const urlQuery = this.param.urlQuery || {}
                Object.keys(urlQuery).forEach((key, index) => {
                    const value = typeof this.paramValues[key] === 'undefined' ? urlQuery[key] : this.paramValues[key]
                    url += `${index <= 0 ? '?' : '&'}${key}=${value}`
                })
                const pipelineInfoQuery = this.param.pipelineInfoQuery || {}
                Object.keys(pipelineInfoQuery).forEach(key => {
                    const value = typeof this.pipelineInfo[key] === 'undefined' ? pipelineInfoQuery[key] : this.pipelineInfo[key]
                    Object.keys(urlQuery).length ? url += `&${key}=${value}` : url += `?${key}=${value}`
                })
                this.isLoading = true
                this.$ajax.get(url).then((res) => {
                    this.subParamsKeyList = res.data?.properties || res.data || []
                }).catch(e => this.$showTips({ message: e.message, theme: 'error' })).finally(() => (this.isLoading = false))
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
    .add-params-btn {
        color: #3A84FF;
        cursor: pointer;
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
        .minus-btn {
            font-size: 14px;
            margin-left: 5px;
            cursor: pointer;
        }
    }
</style>
