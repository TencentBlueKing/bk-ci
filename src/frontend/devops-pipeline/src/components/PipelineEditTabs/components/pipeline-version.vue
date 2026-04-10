<template>
    <section class="version-container">
        <constraint-wraper
            class="operate-version"
            :classify="CLASSIFY_ENUM.PARAM"
            field="buildNo"
            @toggleConstraint="toggleBuildNoConstraint"
        >
            <template #constraint-title="{ props: { isOverride } }">
                <div class="version-config-options">
                    <atom-checkbox
                        :disabled="disabled"
                        :text="$t('newui.enableVersions')"
                        :value="showVersions"
                        :handle-change="(name, value) => toggleVersions(name, value)"
                    />
                    <div>
                        <atom-checkbox
                            v-if="showVersions"
                            :disabled="disabled && !isOverride"
                            name="required"
                            :text="$t('newui.isBuildParam')"
                            :value="isRequired"
                            :handle-change="handleBuildNoChange"
                        />
                        <atom-checkbox
                            v-if="!!templateId && showVersions && isRequired"
                            name="asInstanceInput"
                            class="ml10"
                            :disabled="disabled && !isOverride"
                            :desc="$t('editPage.instanceRequiredTips')"
                            :text="$t('editPage.instanceRequired')"
                            :value="asInstanceInput"
                            :handle-change="handleBuildNoChange"
                        />
                    </div>
                </div>
            </template>
            <template #constraint-area="{ props: { isOverride } }">
                <bk-button
                    v-if="showVersions && !(disabled && !isOverride)"
                    @click="editVersions"
                >
                    {{ $t('newui.editVersions') }}
                </bk-button>
            </template>
        </constraint-wraper>
        <!-- 展示已有的versionlist -->
        <section
            v-show="showVersions"
            class="version-list"
        >
            <div
                v-for="param in renderVersions"
                :key="param.id"
                class="version-item"
            >
                <div class="version-con">
                    <div class="version-names">
                        <div
                            :class="{ 'baseline-build': param.isBuildNo }"
                            v-bk-tooltips="{
                                ...baselineTooltipContent,
                                disabled: !param.isBuildNo
                            }"
                        >
                            <span>{{ param.id }}</span>
                            <span>({{ param.isBuildNo ? param.desc : $t(param.desc) }})</span>
                        </div>
                        <div
                            id="baseline-tooltip-content"
                            v-if="param.isBuildNo"
                        >
                            <p
                                v-for="(tip, index) in buildNoBaselineTips"
                                :key="index"
                            >
                                {{ tip }}
                            </p>
                        </div>
                    </div>
                    <div class="value-row">
                        <span class="default-value">
                            <span v-if="param.isBuildNo">
                                {{ `${$t('buildNoBaseline.baselineValue')}${$t('colon')}${renderBuildNo.buildNo}（${getLabelByBuildType(renderBuildNo.buildNoType)}）` }}
                                <span
                                    class="dafault-value-current"
                                    v-if="pipelineModel && !isTemplate"
                                >
                                    {{ `${$t('buildNoBaseline.currentValue')}${$t('colon')}${buildNo.currentBuildNo}` }}
                                    <span
                                        class="dafault-value-reset"
                                        @click="goResetBuildNo"
                                    >{{ $t('buildNoBaseline.resetBuildNo') }}</span>
                                </span>
                            </span>
                            <span v-else>{{ versionValues[param.id] }}</span>
                        </span>
                        <div class="version-operate">
                            <div
                                class="operate-btns"
                                v-if="!disabled"
                            >
                                <i
                                    @click.stop="handleCopy(bkVarWrapper(param.id))"
                                    class="bk-icon icon-copy"
                                ></i>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>

        <!-- 编辑版本号 -->
        <div
            v-if="showEditVersion"
            class="current-edit-param-item"
        >
            <div class="edit-var-header">
                <bk-icon
                    style="font-size: 28px;"
                    type="arrows-left"
                    class="back-icon"
                    @click="cancelEditVersion"
                />
                {{ $t('newui.editVersions') }}
            </div>
            <div class="edit-var-content">
                <bk-form
                    form-type="vertical"
                    :label-width="400"
                    class="new-ui-form"
                >
                    <bk-form-item>
                        <div class="layout-label">
                            <label class="ui-inner-label">
                                <span class="bk-label-text">{{ $t('versionNum') }}</span>
                                <span class="bk-label-text desc-text">{{ $t('mainMinorPatch') }}</span>
                            </label>
                        </div>
                        <div class="version-options">
                            <form-field
                                :hide-colon="true"
                                class="version-form-field"
                                v-for="(v, index) in allVersionKeyList"
                                :key="v"
                                :required="v.required"
                                :is-error="errors.has(`pipelineVersion.${v}`)"
                                :error-msg="errors.first(`pipelineVersion.${v}`)"
                            >
                                <vuex-input
                                    v-validate.initial="'required|numeric'"
                                    :data-vv-scope="'pipelineVersion'"
                                    input-type="number"
                                    class="version-input"
                                    :disabled="disabled && !overrideConstraint"
                                    :name="v"
                                    :value="editVersionValues[v]"
                                    :handle-change="handleEditVersionChange"
                                />
                                <span
                                    v-if="index < allVersionKeyList.length - 1"
                                    class="version-dot"
                                >.</span>
                            </form-field>
                        </div>
                    </bk-form-item>

                    <bk-form-item
                        :label="$t('buildNoBaseline.buildNoBaseline')"
                        :desc="baselineTooltipContent"
                        required
                    >
                        <form-field
                            :hide-colon="true"
                            :required="true"
                            :is-error="errors.has('pipelineVersion.buildNo')"
                            :error-msg="errors.first('pipelineVersion.buildNo')"
                        >
                            <vuex-input
                                input-type="number"
                                name="buildNo"
                                placeholder="BK_CI_BUILD_NO"
                                style="width: 228px;"
                                v-validate.initial="'required|numeric'"
                                :data-vv-scope="'pipelineVersion'"
                                :disabled="disabled && !overrideConstraint"
                                :value="editBuildNo.buildNo"
                                :handle-change="handleEditBuildNoChange"
                            />
                        </form-field>
                        <span class="baseline-tips">
                            <Logo
                                size="14"
                                name="warning-circle-fill"
                            />
                            <span class="baseline-tips-text">{{ $t('buildNoBaseline.manualResetRequired') }}</span>
                        </span>
                        <div id="baseline-tooltip-content">
                            <p
                                v-for="(tip, index) in buildNoBaselineTips"
                                :key="index"
                            >
                                {{ tip }}
                            </p>
                        </div>
                    </bk-form-item>

                    <form-field
                        :hide-colon="true"
                        class="buildno-form-field"
                        :required="true"
                        :is-error="errors.has('pipelineVersion.buildNoType')"
                        :error-msg="errors.first('pipelineVersion.buildNoType')"
                    >
                        <enum-input
                            :list="buildNoRules"
                            :disabled="disabled && !overrideConstraint"
                            name="buildNoType"
                            v-validate.initial="'required|string'"
                            :data-vv-scope="'pipelineVersion'"
                            :value="editBuildNo.buildNoType"
                            :handle-change="handleEditBuildNoChange"
                        />
                    </form-field>
                </bk-form>
            </div>
            <div
                class="edit-var-footer"
                slot="footer"
            >
                <bk-button
                    theme="primary"
                    :disabled="disabled && !overrideConstraint"
                    @click="handleSaveVersion"
                >
                    {{ $t('confirm') }}
                </bk-button>
                <bk-button
                    style="margin-left: 8px;"
                    :disabled="disabled && !overrideConstraint"
                    @click="cancelEditVersion"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </div>
        </div>

        <bk-dialog
            width="480"
            footer-position="center"
            v-model="resetBuildNoDialog"
        >
            <template #header>
                <span class="delete-pipeline-warning-icon">
                    <i class="devops-icon icon-exclamation" />
                </span>
                <h3>{{ $t('buildNoBaseline.isSureReset') }}</h3>
            </template>
            <div>
                <p>{{ `${$t('buildNoBaseline.baselineValue')}${$t('colon')}${buildNo.buildNo}` }}</p>
                <p class="reset-tips">
                    <i18n
                        :path="`buildNoBaseline.${buildNo.buildNoType}`"
                        tag="p"
                        slot="title"
                    >
                        <span>{{ buildNo.buildNo }}</span>
                        <span v-if="buildNo.buildNoType !== 'CONSISTENT'">{{ resetBuildNo }}</span>
                    </i18n>
                </p>
            </div>
            <template slot="footer">
                <bk-button
                    theme="primary"
                    :loading="resetLoading"
                    @click="handleResetConfirm"
                >
                    {{ $t('buildNoBaseline.confirm') }}
                </bk-button>
                <bk-button
                    @click="handleCancelReset"
                >
                    {{ $t('cancel') }}
                </bk-button>
            </template>
        </bk-dialog>
    </section>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import ConstraintWraper from '@/components/ConstraintWraper.vue'
    import Logo from '@/components/Logo'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import { CLASSIFY_ENUM } from '@/hook/useTemplateConstraint'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'
    import { bkVarWrapper, copyToClipboard, getParamsValuesMap } from '@/utils/util'
    import { mapActions, mapGetters } from 'vuex'

    export default {
        components: {
            FormField,
            VuexInput,
            EnumInput,
            AtomCheckbox,
            Logo,
            ConstraintWraper
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
            },
            isDirectShowVersion: {
                type: Boolean,
                default: false
            },
            pipelineModel: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                CLASSIFY_ENUM,
                showVersions: false,
                showEditVersion: false,
                renderBuildNo: {},
                editBuildNo: {},
                editVersionValues: {},
                baselineTooltipContent: {
                    allowHTML: true,
                    width: 610,
                    content: '#baseline-tooltip-content'
                },
                resetBuildNoDialog: false,
                resetLoading: false,
                overrideConstraint: false
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules',
                'defaultBuildNo',
                'isTemplate'
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
                        desc: this.$t('buildNo'),
                        isBuildNo: true
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
                return this.container?.buildNo ?? {}
            },
            isRequired () {
                return !!this.buildNo?.required
            },
            asInstanceInput () {
                return !!this.buildNo?.asInstanceInput
            },
            buildNoBaselineTips () {
                return Array(7).fill(0).map((_, i) => this.$t(`buildNoBaseline.tips${i + 1}`))
            },
            resetBuildNo () {
                return this.buildNo.buildNo + 1
            },
            templateId () {
                return this.$route.params.templateId
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
        mounted () {
            if (this.isDirectShowVersion) {
                this.resetBuildNoDialog = true
            }
        },
        methods: {
            bkVarWrapper,
            ...mapActions('atom', ['updateBuildNo', 'fetchPipelineByVersion']),
            toggleBuildNoConstraint (isOverride) {
                this.overrideConstraint = isOverride
                this.$nextTick(() => {
                    if (!isOverride) {
                        this.renderBuildNo = this.buildNo
                        this.showVersions = this.versions.length !== 0
                    }
                })
            },
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
                // 创建新对象以触发响应式更新
                const newBuildNo = {
                    ...this.buildNo,
                    [name]: value
                }
                if (name === 'required') {
                    // 如果设置为入参，则更新默认为实例入参
                    newBuildNo.asInstanceInput = value
                }
                this.updateContainerParams('buildNo', newBuildNo)
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
                    this.renderBuildNo = Object.assign({}, {
                        ...this.defaultBuildNo,
                        required: false,
                        asInstanceInput: false
                    })

                    this.updateContainerParams('params', [
                        ...this.globalParams,
                        ...newVersions
                    ])
                    this.updateContainerParams('buildNo', {
                        ...this.renderBuildNo
                    })
                } else {
                    this.updateContainerParams('params', this.globalParams)
                    this.updateContainerParams('buildNo', null)
                }
            },
            editVersions () {
                this.editVersionValues = Object.assign({}, this.versionValues)
                this.editBuildNo = Object.assign({}, this.buildNo)
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
                            ...this.buildNo,
                            ...this.editBuildNo
                        })
                        this.renderBuildNo = Object.assign({}, this.buildNo, this.editBuildNo)
                        this.showEditVersion = false
                    }
                })
            },
            cancelEditVersion () {
                this.showEditVersion = false
            },
            goResetBuildNo () {
                this.resetBuildNoDialog = true
            },
            async handleResetConfirm () {
                try {
                    this.resetLoading = true

                    const { pipelineId, projectId, version } = this.$route.params
                    const { data } = await this.updateBuildNo({ projectId, pipelineId, currentBuildNo: this.buildNo.buildNo })

                    if (data) {
                        const pipelineRes = await this.fetchPipelineByVersion({ projectId, pipelineId, version })
                        
                        this.updateContainerParams('buildNo', {
                            ...pipelineRes.modelAndSetting.model.stages[0].containers[0].buildNo
                        })

                        this.$bkMessage({
                            theme: 'success',
                            message: '已成功重置构建号'
                        })
                    }
                    this.resetLoading = false
                    this.resetBuildNoDialog = false
                } catch (error) {
                    this.$bkMessage({
                        theme: 'error',
                        message: error.message
                    })
                }
            },
            handleCancelReset () {
                this.resetBuildNoDialog = false
            }
        }
    }
</script>

<style lang="scss" scoped>
    .operate-version {
        margin-top: 8px;
        display: flex;
        justify-content: space-between;
        grid-gap: 10px;
        & > :first-child {
            flex: 1;
        }
    }
    .version-config-options {
        display: flex;
        flex-direction: column;
        gap: 10px;
    }
    
    .version-list {
        width: 100%;
        border-bottom: none;
        margin-top: 10px;
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

                    .baseline-build {
                        display: inline-block;
                        border-bottom: 1px dashed #979BA5;
                    }
                }
                .value-row {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    .default-value {
                        color: #979BA5;
                        .dafault-value-current {
                            // margin-left: 16px;
                            .dafault-value-reset {
                                margin-left: 8px;
                                color: #3A84FF;
                                cursor: pointer;
                            }
                        }
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
    .current-edit-param-item {
        .baseline-tips {
            svg {
                vertical-align: middle;
            }
            .baseline-tips-text {
                font-size: 12px;
                color: #979BA5;
            }
        }
    }
    .delete-pipeline-warning-icon {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        background-color: #FFE8C3;
        color: #FF9C01;
        width: 42px;
        height: 42px;
        font-size: 24px;
        border-radius: 50%;
        flex-shrink: 0;
    }
    .reset-tips {
        width: 100%;
        padding: 12px 16px;
        margin: 16px 0;
        background-color: #F5F6FA;
    }

</style>
