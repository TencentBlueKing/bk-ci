<template>
    <div class="build-params-comp">
        <accordion show-checkbox :show-content="isShowVersionParams" is-version="true">
            <template slot="header">
                <span>
                    {{ $t('preview.introVersion') }}
                    <bk-popover placement="right" :max-width="200">
                        <i style="display:block;" class="bk-icon icon-info-circle"></i>
                        <div slot="content" style="white-space: pre-wrap;">
                            <div> {{ $t('editPage.introVersionTips') }} </div>
                        </div>
                    </bk-popover>
                </span>
                <input class="accordion-checkbox" :disabled="disabled" type="checkbox" name="versions" :checked="showVersions" @click.stop @change="toggleVersions" />
            </template>
            <div slot="content">
                <pipeline-versions-form ref="versionForm"
                    v-if="showVersions"
                    :build-no="buildNo"
                    :disabled="!showVersions || disabled"
                    :version-param-values="versionValues"
                    :handle-version-change="handleVersionsChange"
                    :handle-build-no-change="handleBuildNoChange"
                ></pipeline-versions-form>
                <form-field class="params-flex-col">
                    <atom-checkbox :disabled="disabled" :text="$t('editPage.showOnStarting')" :value="execuVisible" name="required" :handle-change="handleBuildNoChange" />
                </form-field>
            </div>
        </accordion>

    </div>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import { getParamsValuesMap } from '@/utils/util'
    import Accordion from '@/components/atomFormField/Accordion'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import validMixins from '../validMixins'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'

    export default {
        name: 'version-config',
        components: {
            Accordion,
            FormField,
            AtomCheckbox,
            PipelineVersionsForm
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
                isShowVersionParams: false
            }
        },

        computed: {
            ...mapGetters('atom', [
                'buildNoRules',
                'defaultBuildNo'
            ]),
            globalParams: {
                get () {
                    return this.params.filter(p => !allVersionKeyList.includes(p.id))
                },
                set (params) {
                    this.updateContainerParams('params', params)
                }
            },
            versions () {
                return this.params.filter(p => allVersionKeyList.includes(p.id))
            },
            versionValues () {
                return getParamsValuesMap(this.versions)
            },
            showVersions () {
                return this.versions.length !== 0
            },
            isExecDetail () {
                const { buildNo } = this.$route.params
                return !!buildNo
            },
            execuVisible () {
                return this.buildNo && this.buildNo.required ? this.buildNo.required : false
            }
        },
        created () {
            this.isShowVersionParams = this.versions.length !== 0
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer'
            ]),
            getVersionById (id) {
                return this.versions.find(v => v.id === id) || {}
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
                const versionConfig = getVersionConfig()

                if (isShow) {
                    const newVersions = allVersionKeyList.map(v => ({
                        desc: versionConfig[v].desc,
                        defaultValue: versionConfig[v].default,
                        id: v,
                        required: false,
                        type: versionConfig[v].type
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
            &:last-child {
                margin-top: 20px;
            }
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
        .devops-icon {
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
        >.devops-icon {
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
</style>
