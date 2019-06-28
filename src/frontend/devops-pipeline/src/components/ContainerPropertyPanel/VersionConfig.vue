<template>
    <div class="build-params-comp">
        <ul v-bkloading="{ isLoading: !buildParams }" v-if="isExecDetail">
            <li class="param-item" v-for="param in buildParams" :key="param.key">
                <vuex-input :disabled="true" name="key" :value="param.key" />
                <span>=</span>
                <vuex-input :disabled="true" name="value" :value="param.value" />
            </li>
        </ul>
        <template v-else>
            <accordion show-checkbox :show-content="isShowVersionParams" is-version="true">
                <template slot="header">
                    <span>
                        推荐版本号
                        <bk-popover placement="right">
                            <i style="display:block;" class="bk-icon icon-info-circle"></i>
                            <div slot="content" style="white-space: pre-wrap;">
                                <div> 可以在插件中引用该变量,用于设置版本号或其他需要用到该变量的地方 </div>
                            </div>
                        </bk-popover>
                    </span>
                    <input class="accordion-checkbox" :disabled="disabled" type="checkbox" name="versions" :checked="showVersions" @click.stop @change="toggleVersions" />
                </template>
                <div slot="content">
                    <div class="params-flex-col" v-if="showVersions">
                        <!--<form-field v-for='v in versions' :key='v.id' :required='v.required' :label='versionConfig[v.id].label' :is-error='errors.has(v.id)' :errorMsg='errors.first(v.id)'>
                            <vuex-input :disabled='disabled' inputType='number' :name='v.id' :placeholder='versionConfig[v.id].placeholder' v-validate.initial='"required|numeric"' :value='v.defaultValue' :handleChange='handleVersionsChange' />
                        </form-field>-->
                        <form-field v-for="v in allVersionKeyList" :key="v" :required="v.required" :label="versionConfig[v].label" :is-error="errors.has(v)" :error-msg="errors.first(v)">
                            <vuex-input :disabled="disabled" input-type="number" :name="v" :placeholder="versionConfig[v].placeholder" v-validate.initial="&quot;required|numeric&quot;" :value="getVersionById(v).defaultValue" :handle-change="handleVersionsChange" />
                        </form-field>
                    </div>
                    <template v-if="buildNo">
                        <div class="params-flex-col">
                            <form-field :required="true" label="构建号" :is-error="errors.has(&quot;buildNo&quot;)" :error-msg="errors.first(&quot;buildNo&quot;)">
                                <vuex-input :disabled="disabled" input-type="number" name="buildNo" placeholder="BuildNo" v-validate.initial="&quot;required|numeric&quot;" :value="buildNo.buildNo" :handle-change="handleBuildNoChange" />
                            </form-field>
                            <form-field class="flex-colspan-2" :required="true" :is-error="errors.has(&quot;buildNoType&quot;)" :error-msg="errors.first(&quot;buildNoType&quot;)">
                                <enum-input :list="buildNoRules" :disabled="disabled" name="buildNoType" v-validate.initial="&quot;required|string&quot;" :value="buildNo.buildNoType" :handle-change="handleBuildNoChange" />
                            </form-field>
                        </div>
                    </template>
                </div>
            </accordion>

        </template>
    </div>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import { deepCopy } from '@/utils/util'
    import Accordion from '@/components/atomFormField/Accordion'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import validMixins from '../validMixins'
    import { isMultipleParam, DEFAULT_PARAM, STRING } from '@/store/modules/atom/paramsConfig'

    export default {
        name: 'version-config',
        components: {
            Accordion,
            VuexInput,
            FormField,
            EnumInput
        },
        mixins: [validMixins],
        props: {
            params: {
                type: Array,
                default: () => []
            },
            buildNo: {
                type: Object
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                isShowVersionParams: false,
                showTips: '若value为版本号,则不能包含“”""等符号；\n如果参数类型为复选框，选择多个值时将以a,b的方式传递给流水线'
            }
        },

        computed: {
            ...mapGetters('atom', [
                'buildNoRules',
                'defaultBuildNo'
            ]),
            ...mapState('atom', [
                'buildParamsMap'
            ]),
            allVersionKeyList () {
                return [
                    'MajorVersion',
                    'MinorVersion',
                    'FixVersion'
                ]
            },
            versionConfig () {
                return {
                    MajorVersion: {
                        label: '主版本',
                        type: 'STRING',
                        desc: '主版本（MajorVersion）',
                        default: '0',
                        placeholder: 'MajorVersion'
                    },
                    MinorVersion: {
                        label: '特性版本',
                        type: 'STRING',
                        desc: '特性版本（MinorVersion）',
                        default: '0',
                        placeholder: 'MinorVersion'
                    },
                    FixVersion: {
                        label: '修正版本',
                        type: 'STRING',
                        desc: '修正版本（FixVersion）',
                        default: '0',
                        placeholder: 'FixVersion'
                    }
                }
            },
            globalParams: {
                get () {
                    const allVersionKeyList = this.allVersionKeyList
                    return this.params.filter(p => !allVersionKeyList.includes(p.id))
                },
                set (params) {
                    this.updateContainerParams('params', params)
                }
            },
            versions () {
                const allVersionKeyList = this.allVersionKeyList
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            showVersions () {
                return this.versions.length !== 0
            },
            buildParams () {
                const { buildNo } = this.$route.params
                return this.buildParamsMap[buildNo]
            },
            isExecDetail () {
                const { buildNo } = this.$route.params
                return !!buildNo
            }
        },
        created () {
            const { projectId, pipelineId, buildNo: buildId } = this.$route.params
            if (buildId && !this.buildParamsMap[buildId]) {
                this.requestBuildParams({
                    projectId,
                    pipelineId,
                    buildId
                })
            }
            this.isShowVersionParams = this.versions.length !== 0
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'requestBuildParams'
            ]),
            getVersionById (id) {
                return this.versions.find(v => v.id === id) || {}
            },
            handleUpdateParam (key, value, paramIndex) {
                try {
                    const param = this.params[paramIndex]
                    if (isMultipleParam(param.type) && key === 'defaultValue') {
                        Object.assign(param, {
                            [key]: value.join(',')
                        })
                    } else if (param) {
                        Object.assign(param, {
                            [key]: value
                        })
                    }

                    this.handleChange(this.params)
                } catch (e) {
                    console.log('update error', e)
                }
            },

            editParam (index, isAdd) {
                const { globalParams, versions } = this
                if (isAdd) {
                    const param = {
                        ...deepCopy(DEFAULT_PARAM[STRING]),
                        id: `param${Math.floor(Math.random() * 100)}`
                    }
                    globalParams.splice(index + 1, 0, param)
                } else {
                    globalParams.splice(index, 1)
                }
                this.handleChange([
                    ...globalParams,
                    ...versions
                ])
            },
            handleChange (params) {
                this.updateContainerParams('params', params)
            },

            handleVersionsChange (name, value) {
                const version = this.versions.find(v => v.id === name)
                if (version && typeof version === 'object') {
                    Object.assign(version, {
                        defaultValue: value
                    })
                }
                this.handleChange([
                    ...this.versions,
                    ...this.globalParams
                ])
            },

            handleBuildNoChange (name, value) {
                this.updateContainerParams('buildNo', {
                    ...this.buildNo,
                    [name]: value
                })
            },

            toggleVersions (e) {
                const isShow = e.target.checked
                const allVersionKeyList = this.allVersionKeyList

                if (isShow) {
                    const newVersions = allVersionKeyList.map(v => ({
                        desc: this.versionConfig[v].desc,
                        defaultValue: this.versionConfig[v].default,
                        id: v,
                        required: false,
                        type: this.versionConfig[v].type
                    }))

                    this.updateContainerParams('params', [
                        ...this.globalParams,
                        ...newVersions
                    ])
                    this.updateContainerParams('buildNo', {
                        ...this.defaultBuildNo
                    })

                    this.isShowVersionParams = true
                } else {
                    this.updateContainerParams('params', this.globalParams)
                    this.updateContainerParams('buildNo', null)
                    this.isShowVersionParams = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .build-params-comp {
        margin: 20px 0;
        .params-flex-col {
            display: flex;
            .bk-form-item {
                flex: 1;
                padding-right: 8px;
                margin-top: 0;
                &:last-child {
                    padding-right: 0;
                }
                &+.bk-form-item {
                    margin-top: 0;
                }
                span.bk-form-help {
                    display: block;
                }
            }
            .flex-colspan-2 {
                flex: 2;
                .bk-form-radio {
                    margin-right: 16px;
                    &:last-child {
                        margin-right: 0;
                    }
                }
                .bk-form-content {
                    margin-top: 35px;
                }
                .atom-checkbox {
                    padding-right: 0;
                    padding-left: 8px;
                    input {
                        margin-right: 10px;
                    }
                }
            }
            .flex-colspan-7 {
                flex: 7;
            }
            .flex-bottom-text {
                .bk-form-content {
                    margin-top: 40px;
                }
            }
        }
        .content .text-link {
            font-size: 14px;
            cursor: pointer;
        }
    }
    .no-prop {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .param-option {
        display: flex;
        align-items: flex-start;
        margin: 0 0 10px 0;
        .option-input {
            flex: 1;
            &:first-child {
                margin-right: 10px;
            }
        }
        .bk-icon {
            font-size: 14px;
            padding: 10px  0 0 10px;
            cursor: pointer;
            &:disabled {
                cursor: auto;
                opacity: .5;
            }
        }
    }

    .param-header {
        display: flex;
        flex: 1;
        align-items: center;
        word-wrap: break-word;
        word-break: break-all;
        > span {
            flex: 1;
        }
        >.bk-icon {
            width: 24px;
            text-align: center;
            &.icon-plus {
                &:hover {
                    color: $primaryColor;
                }
            }
            &.icon-delete {
                &:hover {
                    color: $dangerColor;
                }
            }
        }
    }

    .params-help {
        color: $fontColor;
        font-size: 12px;
    }
    .sortable-ghost-atom {
        opacity: 0.5;
    }
    .sortable-chosen-atom {
        transform: scale(1.0);
    }

    .param-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;
        > span {
            margin: 0 10px;
        }
    }
</style>
