<template>
    <section class="version-container">
        <div class="operate-version">
            <section>
                <atom-checkbox
                    :disabled="disabled"
                    :text="$t('newui.enableVersions')"
                    :value="showVersions"
                    :handle-change="(name, value) => toggleVersions(name, value)"
                />
                <atom-checkbox
                    style="margin-left: 40px;"
                    v-if="showVersions"
                    :disabled="disabled"
                    name="required"
                    :text="$t('newui.isBuildParam')"
                    :value="execuVisible"
                    :handle-change="handleBuildNoChange"
                />
            </section>
            <bk-button v-if="showVersions && !disabled" @click="editVersions">
                {{$t('newui.editVersions')}}
            </bk-button>
        </div>

        <!-- 展示已有的versionlist -->
        <section v-show="showVersions" class="version-list">
            <div v-for="param in renderVersions" :key="param.id" class="version-item">
                <div class="version-con">
                    <div class="version-names">
                        <span>{{ param.id }}</span>
                        <span>({{ param.id === 'BK_CI_BUILD_NO' ? param.desc : $t(param.desc) }})</span>
                    </div>
                    <div class="value-row">
                        <span class="default-value">
                            {{ param.id === 'BK_CI_BUILD_NO' ? `${renderBuildNo.buildNo}（${getLabelByBuildType(renderBuildNo.buildNoType)}）` : versionValues[param.id] }}
                        </span>
                        <div class="version-operate">
                            <div class="operate-btns" v-if="!disabled">
                                <i @click.stop="handleCopy(bkVarWrapper(param.id))" class="bk-icon icon-copy"></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- 编辑版本号 -->
        <div v-if="showEditVersion" class="current-edit-param-item">
            <div class="edit-var-header">
                <bk-icon style="font-size: 28px;" type="arrows-left" class="back-icon" @click="cancelEditVersion" />
                {{$t('newui.editVersions')}}
            </div>
            <div class="edit-var-content">
                <bk-form form-type="vertical" :label-width="400" class="new-ui-form">
                    <bk-form-item>
                        <div class="layout-label">
                            <label class="ui-inner-label">
                                <span class="bk-label-text">{{ $t('versionNum') }}</span>
                                <span class="bk-label-text desc-text">{{ $t('mainMinorPatch') }}</span>
                            </label>
                        </div>
                        <div class="version-options">
                            <form-field :hide-colon="true" class="version-form-field" v-for="(v, index) in allVersionKeyList" :key="v" :required="v.required" :is-error="errors.has(`pipelineVersion.${v}`)" :error-msg="errors.first(`pipelineVersion.${v}`)">
                                <vuex-input
                                    v-validate.initial="'required|numeric'"
                                    :data-vv-scope="'pipelineVersion'"
                                    input-type="number"
                                    class="version-input"
                                    :disabled="disabled"
                                    :name="v"
                                    :value="editVersionValues[v]"
                                    :handle-change="handleEditVersionChange"
                                />
                                <span v-if="index < allVersionKeyList.length - 1" class="version-dot">.</span>
                            </form-field>
                        </div>
                    </bk-form-item>

                    <form-field :hide-colon="true" class="buildno-form-field" :required="true" :label="$t('buildNum')" :is-error="errors.has('pipelineVersion.buildNo')" :error-msg="errors.first('pipelineVersion.buildNo')">
                        <vuex-input
                            input-type="number"
                            name="buildNo"
                            placeholder="BK_CI_BUILD_NO"
                            style="width: 228px;"
                            v-validate.initial="'required|numeric'"
                            :data-vv-scope="'pipelineVersion'"
                            :disabled="disabled"
                            :value="editBuildNo.buildNo"
                            :handle-change="handleEditBuildNoChange"
                        />
                    </form-field>

                    <form-field :hide-colon="true" class="buildno-form-field" :required="true" :is-error="errors.has('pipelineVersion.buildNoType')" :error-msg="errors.first('pipelineVersion.buildNoType')">
                        <enum-input :list="buildNoRules" :disabled="disabled" name="buildNoType" v-validate.initial="'required|string'" :data-vv-scope="'pipelineVersion'" :value="editBuildNo.buildNoType" :handle-change="handleEditBuildNoChange" />
                    </form-field>
                </bk-form>
            </div>
            <div class="edit-var-footer" slot="footer">
                <bk-button theme="primary" :disabled="disabled" @click="handleSaveVersion">
                    {{ $t('confirm') }}
                </bk-button>
                <bk-button style="margin-left: 8px;" :disabled="disabled" @click="cancelEditVersion">
                    {{ $t('cancel') }}
                </bk-button>
            </div>
        </div>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'
    import { getParamsValuesMap, bkVarWrapper, copyToClipboard } from '@/utils/util'

    export default {
        components: {
            FormField,
            VuexInput,
            EnumInput,
            AtomCheckbox
        },
        props: {
            disabled: {
                type: Boolean,
                default: false
            },
            params: {
                type: Array,
                default: () => ([])
            },
            container: {
                type: Object,
                required: true
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                showVersions: false,
                isRequired: false,
                showEditVersion: false,
                renderBuildNo: {},
                editBuildNo: {},
                editVersionValues: {}

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
            renderVersions () {
                return [
                    ...this.versions,
                    {
                        id: 'BK_CI_BUILD_NO',
                        desc: this.$t('buildNo')
                    }
                ]
            },
            versionValues () {
                return getParamsValuesMap(this.versions)
            },
            allVersionKeyList () {
                return allVersionKeyList
            },
            versionConfig () {
                return getVersionConfig()
            },
            buildNo () {
                return this.container?.buildNo || {}
            },
            execuVisible () {
                return this.buildNo && this.buildNo.required ? this.buildNo.required : false
            }
        },
        watch: {
            '$route.params.pipelineId' () {
                this.$nextTick(() => {
                    this.renderBuildNo = this.buildNo
                    this.showVersions = this.versions.length !== 0
                })
            }
        },
        created () {
            this.renderBuildNo = this.buildNo
            this.showVersions = this.versions.length !== 0
        },
        methods: {
            bkVarWrapper,
            handleCopy (con) {
                copyToClipboard(con)
                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('copySuc'),
                    limit: 1
                })
            },
            handleEditVersionChange (name, value) {
                Object.assign(this.editVersionValues, { [name]: value })
            },
            handleEditBuildNoChange (name, value) {
                Object.assign(this.editBuildNo, { [name]: value })
            },
            handleBuildNoChange (name, value) {
                Object.assign(this.renderBuildNo, { [name]: value })
                this.updateContainerParams('buildNo', {
                    ...this.renderBuildNo
                })
            },
            getLabelByBuildType (type) {
                const item = this.buildNoRules.find(item => item.value === type)
                return item?.label || ''
            },
            toggleVersions (name, value) {
                this.showVersions = value
                const versionConfig = getVersionConfig()

                if (this.showVersions) {
                    const newVersions = allVersionKeyList.map(v => ({
                        desc: versionConfig[v].desc,
                        defaultValue: versionConfig[v].default,
                        id: v,
                        required: false,
                        type: versionConfig[v].type
                    }))
                    this.renderBuildNo = Object.assign({}, this.defaultBuildNo)

                    this.updateContainerParams('params', [
                        ...this.globalParams,
                        ...newVersions
                    ])
                    this.updateContainerParams('buildNo', {
                        ...this.defaultBuildNo
                    })
                } else {
                    this.updateContainerParams('params', this.globalParams)
                    this.updateContainerParams('buildNo', null)
                }
            },
            editVersions () {
                this.editVersionValues = Object.assign({}, this.versionValues)
                this.editBuildNo = Object.assign({}, this.renderBuildNo)
                this.showEditVersion = true
            },
            handleSaveVersion () {
                this.$validator.validate('pipelineVersion.*').then((result) => {
                    if (result) {
                        const versionConfig = getVersionConfig()
                        const newVersions = allVersionKeyList.map(v => ({
                            desc: versionConfig[v].desc,
                            defaultValue: this.editVersionValues[v],
                            id: v,
                            required: false,
                            type: versionConfig[v].type
                        }))
                        this.updateContainerParams('params', [
                            ...this.globalParams,
                            ...newVersions
                        ])
                        this.updateContainerParams('buildNo', {
                            ...this.editBuildNo
                        })
                        this.renderBuildNo = Object.assign({}, this.editBuildNo)
                        this.showEditVersion = false
                    }
                })
            },
            cancelEditVersion () {
                this.showEditVersion = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    .operate-version {
        margin-top: 8px;
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    .version-list {
        width: 100%;
        border-bottom: none;
        margin-top: 24px;
        .version-item {
            width: 100%;
            position: relative;
            height: 64px;
            background: #fff;
            border: 1px solid #DCDEE5;
            margin-top: -1px;
            padding-left: 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
            &:hover {
                border-color: #C4C6CC;
                .version-operate {
                    display: block;
                }
            }
            .version-con {
                width: 100%;
                font-size: 12px;
                letter-spacing: 0;
                line-height: 20px;
                overflow: hidden;
                .version-names {
                    color: #313238;
                }
                .value-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    .default-value {
                        color: #979BA5;
                        max-width: 300px;
                    }
                }
            }
            .version-operate {
                display: none;
                .operate-btns {
                    display: flex;
                    align-items: flex-end;
                    padding: 0 18px;
                    i {
                        cursor: pointer;
                        font-size: 14px;
                        color: #63656E;
                    }
                }
            }
        }
    }
    .buildno-form-field {
        margin-top: 16px !important;
    }
    .desc-text {
        color: #979BA5;
    }
    .version-options {
        display: flex;
        .version-form-field {
            display: inline-block;
            margin-top: 0px !important;
        }
        .version-input {
            width: 64px;
            border: 1px solid #DCDEE5;
            border-radius: 2px;
        }
        .version-dot {
            color: #63656E;
            width: 12px;
            display: inline-block;
        }
    }

</style>
