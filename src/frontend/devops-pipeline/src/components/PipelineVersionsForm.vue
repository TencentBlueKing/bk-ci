<template>
    <div style="text-align: left">
        <form class="bk-form" ref="versionParamsForm" onsubmit="return false;">
            <div class="params-flex-col">
                <form-field v-for="v in allVersionKeyList" :key="v" :required="v.required" :label="versionLabel[v]" :is-error="errors.has(v)" :error-msg="errors.first(v)">
                    <vuex-input :disabled="disabled" input-type="number" :name="v" :placeholder="versionConfig[v].placeholder" v-validate.initial="'required|numeric'" :value="versionParamValues[v]" :handle-change="handleVersionChange" />
                </form-field>
            </div>
            <div class="params-flex-col">
                <form-field :required="true" :label="$t('buildNum')" :is-error="errors.has('buildNo')" :error-msg="errors.first('buildNo')">
                    <vuex-input :disabled="(isPreview && buildNo.buildNoType !== 'CONSISTENT') || disabled" input-type="number" name="buildNo" placeholder="BK_CI_BUILD_NO" v-validate.initial="'required|numeric'" :value="buildNo.buildNo" :handle-change="handleBuildNoChange" />
                </form-field>
                <form-field class="flex-colspan-2 build-no-group" :required="true" :is-error="errors.has('buildNoType')" :error-msg="errors.first('buildNoType')">
                    <enum-input :list="buildNoRules" :disabled="disabled || isPreview" name="buildNoType" v-validate.initial="'required|string'" :value="buildNo.buildNoType" :handle-change="handleBuildNoChange" />
                </form-field>
            </div>
        </form>
    </div>
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
    .params-flex-col {
        display: flex;
    }
    .version-option {
        .bk-form-item {
            margin-left: 0;
            margin-right: 20px;
        }
    }
    .flex-colspan-2 {
        .bk-form-radio {
            margin-right: 16px;
            &:last-child {
                margin-right: 0;
            }
        }
        .bk-form-content {
            margin-top: 42px;
        }

        .atom-checkbox {
            padding-right: 0;
            padding-left: 8px;
            input {
                margin-right: 10px;
            }
        }
    }
</style>
