<template>
    <bk-form class="pipeline-execute-version-params" form-type="vertical">
        <div class="execute-build-version">
            <form-field v-for="v in allVersionKeyList" :key="v" :required="v.required" :label="versionLabel[v]" :is-error="errors.has(v)" :error-msg="errors.first(v)">
                <vuex-input :disabled="disabled" input-type="number" :name="v" :placeholder="versionConfig[v].placeholder" v-validate.initial="'required|numeric'" :value="versionParamValues[v]" :handle-change="handleVersionChange" />
            </form-field>
        </div>
        <div class="execute-buildno-params">
            <form-field :required="true" :label="$t('buildNum')" :is-error="errors.has('buildNo')" :error-msg="errors.first('buildNo')">
                <vuex-input :disabled="(isPreview && buildNo.buildNoType !== 'CONSISTENT') || disabled" input-type="number" name="buildNo" placeholder="BK_CI_BUILD_NO" v-validate.initial="'required|numeric'" :value="buildNo.buildNo" :handle-change="handleBuildNoChange" />
            </form-field>
            <form-field :required="true" :is-error="errors.has('buildNoType')" :error-msg="errors.first('buildNoType')">
                <enum-input :list="buildNoRules" :disabled="disabled || isPreview" name="buildNoType" v-validate.initial="'required|string'" :value="buildNo.buildNoType" :handle-change="handleBuildNoChange" />
            </form-field>
        </div>
    </bk-form>
</template>

<script>
    import { mapGetters } from 'vuex'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'

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
                default: () => () => {}
            },
            handleVersionChange: {
                type: Function,
                default: () => () => {}
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
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/scss/conf';
    .pipeline-execute-version-params {
        display: grid;
        grid-gap: 24px;
        &.bk-form.bk-form-vertical .bk-form-item+.bk-form-item {
            margin-top: 0 !important;
        }
        .execute-build-version {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            grid-gap: 12px;
            >:not(:last-child) {
                position: relative;
                &::after {
                    content: '.';
                    position: absolute;
                    right: -8px;
                    bottom: 0;
                }
            }
        }
        .execute-buildno-params {
            display: grid;
            grid-template-columns: 1fr auto;
            align-items: flex-end;
            grid-gap: 24px;

        }
    }
</style>
