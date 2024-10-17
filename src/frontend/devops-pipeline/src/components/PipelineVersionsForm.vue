<template>
    <bk-form
        :class="[{ 'is-not-Preview': !isPreview }, 'pipeline-execute-version-params']"
        :form-type="!isPreview ? 'vertical' : 'inline'"
    >
        <bk-form-item>
            <label class="pipeline-execute-version-label">
                <span>{{ $t('versionNum') }}</span>
                <span class="desc-text">{{ $t('mainMinorPatch') }}</span>
            </label>
            <div class="execute-build-version">
                <span
                    class="execute-build-version-input"
                    v-for="v in allVersionKeyList"
                    :key="v"
                >
                    <vuex-input
                        :disabled="disabled"
                        input-type="number"
                        :name="v"
                        :placeholder="versionConfig[v].placeholder"
                        v-validate.initial="'required|numeric'"
                        :value="versionParamValues[v]"
                        :handle-change="handleVersionChange"
                    />
                </span>
            </div>
        </bk-form-item>

        <div
            class="execute-buildno-params"
            v-if="!isPreview"
        >
            <form-field
                :required="true"
                :label="$t('buildNum')"
                :is-error="errors.has('buildNo')"
                :error-msg="errors.first('buildNo')"
            >
                <vuex-input
                    :disabled="(isPreview && buildNo.buildNoType !== 'CONSISTENT') || disabled"
                    input-type="number"
                    name="buildNo"
                    placeholder="BK_CI_BUILD_NO"
                    v-validate.initial="'required|numeric'"
                    :value="buildNo.buildNo"
                    :handle-change="handleBuildNoChange"
                />
            </form-field>
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
            ext-cls="preview-buildno"
            v-else
        >
            <label class="pipeline-execute-version-label">
                <span>{{ $t('buildNum') }}</span>
            </label>
            <div class="preview-buildno-params">
                <p class="build">
                    <span class="build-label">{{ $t('buildNoBaseline.baselineValue') }}</span>
                    <span class="build-value">{{ `${buildNo.buildNo} (${currentBuildNoType})` }}</span>
                </p>
                <p class="build">
                    <span class="build-label">{{ $t('buildNoBaseline.currentValue') }}</span>
                    <span class="build-value">{{ buildNo.currentBuildNo }}</span>
                </p>
            </div>
        </bk-form-item>
    </bk-form>
</template>

<script>
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            EnumInput,
            VuexInput,
            FormField
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
                default: () => () => { }
            },
            handleVersionChange: {
                type: Function,
                default: () => () => { }
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules'
            ]),
            allVersionKeyList () {
                return allVersionKeyList
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
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';

.pipeline-execute-version-params {
    grid-gap: 10px;

    .pipeline-execute-version-label {
        font-size: 12px;
        font-weight: 700;

        .desc-text {
            font-weight: normal;
            color: #979ba5;
        }
    }

    .execute-build-version {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        grid-gap: 16px;
        width: 222px;

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
    }
    .preview-buildno{
        margin-left: 20px;
        
        .preview-buildno-params {
            display: flex;
            
            .build {
                display: grid;
                grid-template-columns: auto auto;
                column-gap: 0;
                
                .build-label,
                .build-value {
                    font-size: 12px;
                    padding: 0 8px;
                    border: 1px solid #C4C6CC;
                    cursor: not-allowed;
                }

                .build-label {
                    background-color: #F5F7FA;
                    border-radius: 2px 0 0 2px;
                }
                .build-value {
                    margin-right: 16px;
                    border-left: none;
                    border-radius: 0 2px 2px 0;
                    background-color: #FAFBFD;
                }
            }
        }
    }
}

.is-not-Preview {
    display: grid;
}
</style>
