<template>
    <div class="build-params-comp">
        <template>
            <accordion show-checkbox :show-content="hasGlobalParams">
                <template slot="header">
                    <span>
                        {{ title }}
                        <bk-popover placement="right">
                            <i style="display:block;" class="bk-icon icon-info-circle"></i>
                            <div slot="content" style="white-space: pre-wrap;">
                                <div> 可以在插件中引用该变量 </div>
                            </div>
                        </bk-popover>
                    </span>
                </template>
                <template slot="content">
                    <div class="no-prop" v-if="!hasGlobalParams">
                        <bk-button theme="primary" :disabled="disabled" @click="editParam(null, true)">添加变量</bk-button>
                    </div>
                    <template v-else>
                        <draggable v-model="globalParams" :options="paramsDragOptions">
                            <accordion v-for="(param, index) in globalParams" :key="index" :is-error="errors.any(`param-${index}`)">
                                <header class="param-header" slot="header">
                                    <span>{{ param.id }}</span>
                                    <i v-if="!disabled && settingKey !== &quot;templateParams&quot;" @click.stop.prevent="editParamShow(index)" class="bk-icon" :class="[`${param.required ? 'icon-eye' : 'icon-eye-slash'}`]" />
                                    <i v-if="!disabled" class="bk-icon icon-move" />
                                    <i v-if="!disabled" @click.stop.prevent="editParam(index, false)" class="bk-icon icon-minus" />
                                </header>
                                <bk-form slot="content">
                                    <div class="params-flex-col">
                                        <bk-form-item label="变量类型" class="flex-colspan-7">
                                            <selector
                                                :data-vv-scope="`param-${index}`"
                                                :disabled="disabled"
                                                name="type"
                                                :list="paramsList"
                                                :handle-change="(name, value) => handleParamTypeChange(name, value, getParamIndex(param.id))"
                                                :value="param.type"
                                            />
                                        </bk-form-item>
                                        <bk-form-item class="flex-colspan-2" v-if="settingKey !== 'templateParams'">
                                            <atom-checkbox :disabled="disabled" text="执行时显示" :value="param.required" name="required" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" />
                                        </bk-form-item>
                                    </div>
                                    <div class="params-flex-col pt10">
                                        <bk-form-item label="名称" :is-error="errors.has(`param-${index}.id`)" :error-msg="errors.first(`param-${index}.id`)">
                                            <vuex-input :data-vv-scope="`param-${index}`" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" v-validate.initial="`required|unique:${validateParams.map(p => p.id).join(&quot;,&quot;)}`" name="id" placeholder="请输入名称" :value="param.id" />
                                        </bk-form-item>
                                        <bk-form-item label="默认值" :required="isBooleanParam(param.type)" :is-error="errors.has(`param-${index}.defaultValue`)" :error-msg="errors.first(`param-${index}.defaultValue`)" :desc="showTips">
                                            <selector
                                                v-if="isSelectorParam(param.type)"
                                                :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))"
                                                :list="transformOpt(param.options)"
                                                :multi-select="isMultipleParam(param.type)"
                                                name="defaultValue"
                                                :data-vv-scope="`param-${index}`"
                                                placeholder="请输入默认值"
                                                :disabled="disabled"
                                                :value="getSelectorDefaultVal(param)"
                                            >
                                            </selector>
                                            <enum-input
                                                style="line-height: 30px"
                                                v-if="isBooleanParam(param.type)"
                                                name="defaultValue"
                                                :list="boolList"
                                                :disabled="disabled"
                                                :data-vv-scope="`param-${index}`"
                                                :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))"
                                                :value="param.defaultValue">
                                            </enum-input>
                                            <vuex-input v-if="isStringParam(param.type) || isSvnParam(param.type)" :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" name="defaultValue" :data-vv-scope="`param-${index}`" placeholder="请输入默认值" :value="param.defaultValue" />
                                            <request-selector v-if="isCodelibParam(param.type)" :url="getCodeUrl(param.scmType)" v-bind="codelibOption" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" :data-vv-scope="`param-${index}`"></request-selector>
                                            <request-selector v-if="isBuildResourceParam(param.type)" :url="getBuildResourceUrl(param.containerType)" param-id="name" :disabled="disabled" name="defaultValue" :value="param.defaultValue" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" :data-vv-scope="`param-${index}`"></request-selector>
                                        </bk-form-item>
                                    </div>

                                    <bk-form-item v-if="isSelectorParam(param.type)" label="下拉选项" desc="用换行符分隔选项，选项不能包含英文逗号，重复的选项将只会显示一个" :is-error="errors.has(`param-${index}.options`)" :error-msg="errors.first(`param-${index}.options`)">
                                        <vuex-textarea v-validate.initial="&quot;excludeComma&quot;" :disabled="disabled" :handle-change="(name, value) => editOption(name, value, getParamIndex(param.id))" name="options" :data-vv-scope="`param-${index}`" placeholder="多行记录请换行输入" :value="getOptions(param)"></vuex-textarea>
                                    </bk-form-item>

                                    <bk-form-item v-if="isSvnParam(param.type)" label="请选择svn代码库" :is-error="errors.has(`param-${index}.repoHashId`)" :error-msg="errors.first(`param-${index}.repoHashId`)">
                                        <request-selector v-bind="svnPathOption" :disabled="disabled" name="repoHashId" :value="param.repoHashId" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" :data-vv-scope="`param-${index}`" v-validate.initial="&quot;required&quot;"></request-selector>
                                    </bk-form-item>

                                    <bk-form-item v-if="isSvnParam(param.type)" label="代码库相对目录" :is-error="errors.has(`param-${index}.relativePath`)" :error-msg="errors.first(`param-${index}.relativePath`)">
                                        <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" name="relativePath" :data-vv-scope="`param-${index}`" placeholder="请填写代码库相对目录,如为空则会拉取所选代码库的根路径下的子目录" :value="param.relativePath"></vuex-input>
                                    </bk-form-item>

                                    <bk-form-item v-if="isCodelibParam(param.type)" label="请选择代码库类型" :is-error="errors.has(`param-${index}.scmType`)" :error-msg="errors.first(`param-${index}.scmType`)">
                                        <selector :disabled="disabled" :list="codeTypeList" :handle-change="(name, value) => handleCodeTypeChange(name, value, getParamIndex(param.id))" name="scmType" :data-vv-scope="`param-${index}`" placeholder="" :value="param.scmType"></selector>
                                    </bk-form-item>

                                    <template v-if="isBuildResourceParam(param.type)">
                                        <bk-form-item label="请选择构建环境" :is-error="errors.has(`param-${index}.os`)" :error-msg="errors.first(`param-${index}.os`)">
                                            <selector :disabled="disabled" :list="baseOSList" :handle-change="(name, value) => handleBuildResourceChange(name, value, getParamIndex(param.id), param)" name="os" :data-vv-scope="`param-${index}`" placeholder="" :value="param.containerType.os"></selector>
                                        </bk-form-item>

                                        <bk-form-item label="请选择构建资源类型" :is-error="errors.has(`param-${index}.buildType`)" :error-msg="errors.first(`param-${index}.buildType`)">
                                            <selector :disabled="disabled" :list="getBuildTypeList(param.containerType.os)" setting-key="type" :handle-change="(name, value) => handleBuildResourceChange(name, value, getParamIndex(param.id), param)" name="buildType" :data-vv-scope="`param-${index}`" placeholder="" :value="param.containerType.buildType"></selector>
                                        </bk-form-item>
                                    </template>

                                    <bk-form-item label="描述">
                                        <vuex-input :disabled="disabled" :handle-change="(name, value) => handleUpdateParam(name, value, getParamIndex(param.id))" name="desc" placeholder="请输入参数描述" :value="param.desc" />
                                    </bk-form-item>
                                </bk-form>
                            </accordion>
                        </draggable>
                        <a class="text-link" v-if="!disabled" @click.stop.prevent="editParam(globalParams.length, true)">
                            <i class="bk-icon icon-plus-circle" />
                            <span>新增变量</span>
                        </a>
                    </template>
                </template>
            </accordion>

        </template>
    </div>
</template>

<script>
    import { mapGetters, mapState } from 'vuex'
    import { deepCopy } from '@/utils/util'
    import Accordion from '@/components/atomFormField/Accordion'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import validMixins from '../validMixins'
    import draggable from 'vuedraggable'
    import { STORE_API_URL_PREFIX, REPOSITORY_API_URL_PREFIX } from '@/store/constants'
    import {
        isStringParam,
        isBooleanParam,
        isBuildResourceParam,
        isEnumParam,
        isMultipleParam,
        isCodelibParam,
        isSvnParam,
        DEFAULT_PARAM,
        PARAM_LIST,
        STRING,
        SVN_PATH_OPTION,
        CODE_LIB_OPTION,
        CODE_LIB_TYPE
    } from '@/store/modules/atom/paramsConfig'

    const BOOLEAN = [
        {
            value: true,
            label: true
        },
        {
            value: false,
            label: false
        }
    ]

    export default {
        name: 'build-params',
        components: {
            Accordion,
            VuexInput,
            EnumInput,
            AtomCheckbox,
            Selector,
            draggable,
            VuexTextarea,
            RequestSelector
        },
        mixins: [validMixins],
        props: {
            settingKey: {
                type: String,
                default: 'params'
            },
            params: {
                type: Array,
                default: () => []
            },
            // 用于校验模板常量和流水线变量不能重名
            additionParams: {
                type: Array,
                default: () => []
            },
            disabled: {
                type: Boolean,
                default: false
            },
            updateContainerParams: {
                type: Function,
                required: true
            },
            title: {
                type: String,
                default: '全局参数'
            }
        },
        data () {
            return {
                showTips: '若value为版本号,则不能包含“”""等符号；\n如果参数类型为复选框，选择多个值时将以a,b的方式传递给流水线'
            }
        },

        computed: {
            ...mapState('atom', [
                'buildParamsMap'
            ]),
            ...mapGetters('atom', [
                'osList',
                'getBuildResourceTypeList'
            ]),
            versionConfig () {
                return {
                    MajorVersion: {},
                    MinorVersion: {},
                    FixVersion: {}
                }
            },
            baseOSList () {
                return this.osList.filter(os => os.value !== 'NONE').map(os => ({
                    id: os.value,
                    name: os.label
                }))
            },
            validateParams () {
                return this.params.concat(this.additionParams)
            },
            paramsList () {
                return PARAM_LIST
            },
            boolList () {
                return BOOLEAN
            },
            svnPathOption () {
                return SVN_PATH_OPTION
            },
            codelibOption () {
                return CODE_LIB_OPTION
            },
            codeTypeList () {
                return CODE_LIB_TYPE
            },
            globalParams: {
                get () {
                    const allVersionKeyList = Object.keys(this.versionConfig)
                    return this.params.filter(p => !allVersionKeyList.includes(p.id))
                },
                set (params) {
                    this.updateContainerParams(this.settingKey, [...params, ...this.versions])
                }
            },
            versions () {
                const allVersionKeyList = Object.keys(this.versionConfig)
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            hasGlobalParams () {
                return this.globalParams.length !== 0
            },
            paramsDragOptions () {
                return {
                    ghostClass: 'sortable-ghost-atom',
                    chosenClass: 'sortable-chosen-atom',
                    handle: '.icon-move',
                    animation: 200,
                    disabled: this.disabled
                }
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
        methods: {
            isStringParam,
            isBooleanParam,
            isMultipleParam,
            isSvnParam,
            isCodelibParam,
            isBuildResourceParam,
            isSelectorParam (type) {
                return isMultipleParam(type) || isEnumParam(type)
            },
            getBuildTypeList (os) {
                return this.getBuildResourceTypeList(os)
            },
            getSelectorDefaultVal ({ type, defaultValue }) {
                if (isMultipleParam(type)) {
                    return defaultValue && typeof defaultValue === 'string' ? defaultValue.split(',') : []
                }

                return defaultValue
            },
            handleParamTypeChange (key, value, paramIndex) {
                this.handleChange([
                    ...this.params.slice(0, paramIndex),
                    {
                        ...deepCopy(DEFAULT_PARAM[value]),
                        id: this.params[paramIndex].id
                    },
                    ...this.params.slice(paramIndex + 1)
                ])
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

                    this.handleChange([
                        ...this.params
                    ])
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
                    if (this.settingKey === 'templateParams') {
                        Object.assign(param, { 'required': false })
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

            editParamShow (paramIndex) {
                let isShow = false
                const param = this.params[paramIndex]
                if (param) {
                    isShow = param.required
                }
                this.handleUpdateParam('required', !isShow, paramIndex)
            },

            editOption (name, value, index) {
                try {
                    let opts = []
                    if (value && typeof value === 'string') {
                        opts = value.split('\n').map(opt => {
                            const v = opt.trim()
                            return {
                                key: v,
                                value: v
                            }
                        })
                    }

                    this.handleUpdateParam(name, opts, index)
                    const param = this.params[index]
                    if (typeof param.defaultValue === 'string' && (isMultipleParam(param.type) || isEnumParam(param.type))) { // 选项清除时，修改对应的默认值
                        const dv = param.defaultValue.split(',').filter(v => param.options.map(k => k.key).includes(v))
                        if (isMultipleParam(param.type)) {
                            this.handleUpdateParam('defaultValue', dv, index)
                        } else {
                            this.handleUpdateParam('defaultValue', dv.join(','), index)
                        }
                    }
                } catch (e) {
                    this.$showTips({
                        message: e.message,
                        theme: 'error'
                    })
                    return []
                }
            },

            handleBuildResourceChange (name, value, index, param) {
                const resetBuildType = name === 'os' ? { buildType: this.getBuildTypeList(value)[0].type } : {}

                this.handleUpdateParam('containerType', Object.assign({
                    ...param.containerType,
                    [name]: value
                }, resetBuildType), index)
                this.handleUpdateParam('defaultValue', '', index)
            },

            getBuildResourceUrl ({ os, buildType }) {
                return `/${STORE_API_URL_PREFIX}/user/pipeline/container/projects/${this.$route.params.projectId}/oss/${os}?buildType=${buildType}`
            },

            handleCodeTypeChange (name, value, index) {
                this.handleUpdateParam(name, value, index)
                this.handleUpdateParam('defaultValue', '', index)
            },

            getCodeUrl (type) {
                type = type || 'CODE_GIT'
                return `/${REPOSITORY_API_URL_PREFIX}/user/repositories/{projectId}/hasPermissionList?permission=USE&repositoryType=${type}&page=1&pageSize=100`
            },

            handleChange (params) {
                this.updateContainerParams(this.settingKey, params)
            },
            // 返回匹配的params下标
            getParamIndex (paramId) {
                return this.params.findIndex(item => item.id === paramId)
            },

            getOptions (param) {
                try {
                    return param.options.map(opt => opt.key).join('\n')
                } catch (e) {
                    return ''
                }
            },

            transformOpt (opts) {
                const uniqueMap = {}
                opts = opts.filter(opt => opt.key.length)
                return Array.isArray(opts) ? opts.filter(opt => {
                    if (!uniqueMap[opt.key]) {
                        uniqueMap[opt.key] = 1
                        return true
                    }
                    return false
                }).map(opt => ({ id: opt.key, name: opt.key })) : []
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
                    margin-right: 18px;
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
