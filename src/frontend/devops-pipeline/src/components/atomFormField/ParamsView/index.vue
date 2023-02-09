<template>
    <div class="sub-build-params">
        <ul v-bkloading="{ isLoading: isLoading }">
            <template v-if="paramList.length">
                <li class="param-item" v-for="param in paramList" :key="param.id">
                    <div><vuex-input :disabled="true" name="key" :value="param.id" /></div>
                    <span>=</span>
                    <div>
                        <select-input
                            :name="param.id"
                            :value="paramValues[param.id]"
                            :disabled="disabled"
                            type="text"
                            :placeholder="$t('editPage.paramInputTips')"
                            v-bind="dataInputConfig(param)"
                        />
                    </div>
                </li>
            </template>
            <li v-else class="param-item-empty"><span>{{ $t('editPage.paramEmptyTips') }}</span></li>
        </ul>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import { deepCopy } from '@/utils/util'

    const booleanList = [{
        id: 'true',
        name: 'true'
    }, {
        id: 'false',
        name: 'false'
    }]

    export default {
        name: 'params-view',
        components: {
            VuexInput,
            SelectInput
        },
        mixins: [atomFieldMixin],
        props: {
            url: {
                type: String,
                default: ''
            },
            value: {
                type: Object
            }
        },
        data () {
            return {
                isLoading: false,
                paramValues: {},
                paramList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.element.subPipelineId
            }
        },
        watch: {
            pipelineId (val) {
                this.getParams()
            },
            paramValues: {
                deep: true,
                handler: function (val) {
                    this.handleChange(this.name, val)
                }
            }
        },
        async created () {
            const oldValues = (this.value && deepCopy(this.value)) || {}
            await this.getParams()

            Object.keys(oldValues).forEach((key) => {
                if (this.paramValues[key] !== undefined) {
                    this.paramValues[key] = oldValues[key]
                }
            })
        },
        methods: {
            dataInputConfig (param) {
                return {
                    options: param.type === 'BOOLEAN'
                        ? booleanList
                        : param.type === 'ENUM'
                            ? param.options.map(item => ({
                                id: item.key,
                                name: item.key
                            }))
                            : [],
                    handleChange: this.handleParamChange
                }
            },
            curItem (key) {
                return (this.paramList.length && this.paramList.filter(item => item.id === key)[0]) || []
            },
            handleParamChange (name, value) {
                const cur = this.curItem(name)
                if (cur.type === 'BOOLEAN') {
                    if (value === 'true') {
                        value = true
                    } else if (value === 'false') {
                        value = false
                    }
                }
                this.paramValues[name] = value
            },
            async getParams () {
                const {
                    url,
                    pipelineId,
                    projectId
                } = this
                if (pipelineId) {
                    try {
                        const changeUrl = this.urlParse(url, {
                            bkPoolType: this?.container?.dispatchType?.buildType,
                            pipelineId,
                            projectId
                        })
                        this.isLoading = true
                        this.paramList = []
                        const res = await this.$ajax.get(changeUrl)

                        const requiredList = res.data.properties.length && res.data.properties.filter(p => p.required)
                        if (requiredList.length) {
                            this.isDialogShow = true
                            this.paramList = requiredList.map(param => {
                                this.$set(this.paramValues, param.id, param.defaultValue)
                                return param
                            })
                        } else {
                            this.paramValues = {}
                        }
                    } catch (e) {
                        this.$showTips({
                            message: e.message,
                            theme: 'error'
                        })
                    } finally {
                        this.isLoading = false
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../../scss/conf.scss';
    .sub-build-params {
        .param-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
            > span {
                margin: 0 10px;
            }
            > div {
                flex: 1;
            }
        }
        .param-item-empty {
            text-align: center;
            color: $fontLighterColor;
        }
    }
</style>
