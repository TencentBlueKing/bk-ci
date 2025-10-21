<template>
    <bk-form
        :class="[{ 'is-not-Preview': isTemplateEdit }, 'pipeline-execute-version-params']"
        :form-type="formType"
    >
        <bk-form-item>
            <label class="pipeline-execute-version-label">
                <span :class="{ 'deleted': isDelete }">{{ $t('versionNum') }}</span>
                <span class="desc-text">{{ $t('mainMinorPatch') }}</span>
            </label>
            <div class="execute-build-version">
                <form-field
                    :hide-colon="true"
                    class="execute-build-version-input"
                    v-for="v in renderVersionParamList"
                    :key="v.id"
                    :required="v.required"
                    :is-error="errors.has(v.id)"
                >
                    <vuex-input
                        :disabled="disabled || isFollowTemplate"
                        input-type="number"
                        :name="v.id"
                        :class="{
                            'is-diff-param': highlightChangedParam && v.isChanged,
                            'is-change-param': v.isChange,
                            'is-new-param': v.isNew,
                            'is-delete-param': v.isDelete
                        }"
                        :placeholder="v.placeholder"
                        v-validate.initial="'required|numeric'"
                        :value="versionParamValues[v.id]"
                        :handle-change="handleVersionChange"
                    />
                </form-field>
            </div>
        </bk-form-item>
        <div
            v-if="isTemplateEdit"
            class="execute-buildno-params"
        >
            <bk-form-item
                required
                :label="$t('buildNoBaseline.buildNoBaseline')"
                :desc="baselineTooltipContent"
            >
                <form-field
                    :required="true"
                    :is-error="errors.has('buildNo')"
                    :error-msg="errors.first('buildNo')"
                >
                    <vuex-input
                        :disabled="isPreviewAndLockedNo"
                        input-type="number"
                        name="buildNo"
                        placeholder="BK_CI_BUILD_NO"
                        v-validate.initial="'required|numeric'"
                        :value="buildNo.buildNo"
                        :handle-change="handleBuildNoChange"
                    />
                </form-field>
                <span class="baseline-tips">
                    <Logo
                        size="14"
                        name="warning-circle-fill"
                    />
                    <span class="baseline-tips-text">{{ $t('buildNoBaseline.templateManualResetRequired') }}</span>
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
                :required="true"
                :is-error="errors.has('buildNoType')"
                :error-msg="errors.first('buildNoType')"
            >
                <enum-input
                    :list="buildNoRules"
                    :disabled="disabled || isPreview"
                    name="buildNoType"
                    v-validate.initial="'required|string'"
                    :value="buildNo.buildNoType"
                    :handle-change="handleBuildNoChange"
                />
            </form-field>
        </div>
        <bk-form-item
            v-else
            ext-cls="preview-buildno"
        >
            <label class="pipeline-execute-version-label">
                <span>{{ $t('buildNum') }}</span>
                <bk-checkbox
                    v-if="isInstance && !isInitInstance"
                    class="instance_reset"
                    :disabled="disabled"
                    :value="resetBuildNo"
                    @change="handleCheckChange"
                >
                    {{ $t('buildNoBaseline.instanceBuildNo') }}
                </bk-checkbox>
            </label>
            <div class="preview-buildno-params">
                <div class="build">
                    <span class="build-label">{{ $t('buildNoBaseline.baselineValue') }}</span>
                    <span
                        class="build-value"
                        v-if="!isInstance"
                    >
                        {{ `${buildNo.buildNo} (${currentBuildNoType})` }}
                    </span>
                    <p
                        class="build-input"
                        v-else
                    >
                        <vuex-input
                            :disabled="isPreviewAndLockedNo || isFollowTemplate || disabled"
                            input-type="number"
                            name="buildNo"
                            placeholder="BK_CI_BUILD_NO"
                            v-validate.initial="'required|numeric'"
                            :value="buildNo.buildNo"
                            :handle-change="handleBuildNoChange"
                        />
                        <span class="bk-form-help is-danger">{{ errors.first('buildNo') }}</span>
                    </p>
                </div>
                <div
                    class="build"
                    v-if="isInstance"
                >
                    <span class="build-label">{{ $t('buildNoBaseline.strategy') }}</span>
                    <span class="build-value">
                        {{ currentBuildNoType }}
                    </span>
                </div>
                <div
                    class="build"
                    v-if="!isInitInstance"
                >
                    <span class="build-label">{{ $t('buildNoBaseline.currentValue') }}</span>
                    <p>
                        <vuex-input
                            :disabled="(isLockedNo && !isInstance) || isInstance || disabled"
                            input-type="number"
                            name="currentBuildNo"
                            placeholder="CURRENT_BUILD_NO"
                            v-validate.initial="'required|numeric'"
                            :value="buildNo.currentBuildNo"
                            :handle-change="handleBuildNoChange"
                            :class="{
                                'is-diff-param': highlightChangedParam && buildNo.isChanged,
                                'is-change-param': resetBuildNo
                            }"
                        />
                        <span class="bk-form-help is-danger">{{ errors.first('currentBuildNo') }}</span>
                    </p>
                </div>
            </div>
        </bk-form-item>
    </bk-form>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import Logo from '@/components/Logo'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            EnumInput,
            VuexInput,
            FormField,
            Logo
        },
        props: {
            isPreview: {
                type: Boolean
            },
            disabled: {
                type: Boolean,
                default: false
            },
            versionParamValues: {
                type: Object,
                default: () => ({})
            },
            buildNo: {
                type: Object,
                default: () => ({})
            },
            handleBuildNoChange: {
                type: Function,
                default: () => () => {}
            },
            handleVersionChange: {
                type: Function,
                default: () => () => {}
            },
            handleCheckChange: {
                type: Function,
                default: () => () => {}
            },
            isInstance: Boolean,
            isInitInstance: Boolean,
            resetBuildNo: Boolean,
            highlightChangedParam: Boolean,
            isFollowTemplate: Boolean,
            versionParamList: {
                type: Array,
                default: () => []
            }
        },
        data () {
            return {
                baselineTooltipContent: {
                    allowHTML: true,
                    width: 610,
                    content: '#baseline-tooltip-content'
                }
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules'
            ]),
            renderVersionParamList () {
                return this.versionParamList.length
                    ? this.versionParamList
                    : allVersionKeyList.map(v => ({
                        id: v,
                        value: this.versionParamValues[v],
                        placeholder: this.versionConfig[v].placeholder,
                        isChanged: false
                    }))
            },
            versionConfig () {
                return getVersionConfig()
            },
            versionLabel () {
                return {
                    BK_CI_MAJOR_VERSION: this.$t('preview.majorVersion'),
                    BK_CI_MINOR_VERSION: this.$t('preview.minorVersion'),
                    BK_CI_FIX_VERSION: this.$t('preview.fixVersion')
                }
            },
            currentBuildNoType () {
                const buildNoItem = this.buildNoRules.find(item => item.value === this.buildNo.buildNoType)
                return buildNoItem ? buildNoItem.label : undefined
            },
            buildNoBaselineTips () {
                return Array(7).fill(0).map((_, i) => this.$t(`buildNoBaseline.tips${i + 1}`))
            },
            formType () {
                return this.isTemplateEdit ? 'vertical' : 'inline'
            },
            isLockedNo () {
                return this.buildNo.buildNoType !== 'CONSISTENT'
            },
            isTemplateEdit () {
                return !this.isPreview && !this.isInstance
            },
            isPreviewAndLockedNo () {
                return (this.isLockedNo && this.isPreview) || this.disabled
            },
            isDelete () {
                return this.versionParamList.every(i => i?.isDelete)
            },
            isNew () {
                return this.versionParamList.every(i => i?.isNew)
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';

.pipeline-execute-version-params {
    grid-gap: 10px;

    .pipeline-execute-version-label {
        display: flex;
        align-items: center;
        font-size: 12px;
        font-weight: 700;
        .deleted {
            color: #a7a9ac !important;
            text-decoration: line-through;
        }
        .desc-text {
            font-weight: normal;
            color: #979ba5;
        }

        .instance_reset {
            font-weight: normal;
            margin-left: 18px;
        }
        .status-tag {
            padding: 0 8px;
            border-radius: 2px;
            margin-right: 40px;
            font-size: 12px;
            height: 16px;
            line-height: 16px;
            font-weight: 400;
            &.success {
                color: #299E56;
                background: #DAF6E5;
            }
            &.danger {
                color: #E71818;
                background: #FFEBEB;
            }
        }
    }

    .execute-build-version {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        grid-gap: 16px;
        width: 222px;
        margin-right: 20px;

        .execute-build-version-input:not(:last-child) {
            position: relative;

            &::after {
                content: '.';
                position: absolute;
                right: -8px;
                bottom: -6px;
            }
        }
    }

    .execute-buildno-params {
        display: grid;
        grid-gap: 8px;
        width: fit-content;
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
    .preview-buildno {
        margin-left: 0;
        .preview-buildno-params {
            display: flex;

            .build {
                display: grid;
                grid-template-columns: auto auto;
                column-gap: 0;

                .build-label,
                .build-value {
                    font-size: 12px;
                    padding: 0 12px;
                    border: 1px solid #dcdee5;
                    cursor: not-allowed;
                    height: 32px;
                }

                .build-label {
                    background-color: #F5F7FA;
                    border-radius: 2px 0 0 2px;
                }

                .build-value {
                    min-width: 153px;
                    margin-right: 16px;
                    border-left: none;
                    border-radius: 0 2px 2px 0;
                    background-color: #FAFBFD;
                }

                p {
                    position: relative;
                    display: flex;

                    .is-danger {
                        position: absolute;
                        white-space: nowrap;
                        top: 70%;
                        left: 0;
                    }

                    .reset-build-no {
                        display: flex;
                        align-items: center;
                        color: #3A84FF;
                        svg {
                            margin: 0 8px 0 16px;
                        }
                    }
                }

                .build-input {
                    margin-right: 16px;
                }
            }
        }
    }
}

.is-not-Preview {
    display: grid;
}
</style>
