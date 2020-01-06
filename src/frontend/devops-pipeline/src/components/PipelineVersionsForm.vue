<template>
    <div style="text-align: left">
        <form class="bk-form" ref="versionParamsForm" onsubmit="return false;">
            <div class="params-flex-col">
                <form-field v-for="v in allVersionKeyList" :key="v" :required="v.required" :label="versionConfig[v].label" :is-error="errors.has(v)" :error-msg="errors.first(v)">
                    <vuex-input :disabled="disabled" input-type="number" :name="v" :placeholder="versionConfig[v].placeholder" v-validate.initial="&quot;required|numeric&quot;" :value="versionParamValues[v]" :handle-change="handleVersionChange" />
                </form-field>
            </div>
            <div class="params-flex-col">
                <form-field :required="true" :label="$t('buildNum')" :is-error="errors.has(&quot;buildNo&quot;)" :error-msg="errors.first(&quot;buildNo&quot;)">
                    <vuex-input :disabled="buildNo.buildNoType !== 'CONSISTENT'" input-type="number" name="buildNo" placeholder="BuildNo" v-validate.initial="&quot;required|numeric&quot;" :value="buildNo.buildNo" :handle-change="handleBuildNoChange" />
                </form-field>
                <form-field class="flex-colspan-2" :required="true" :is-error="errors.has(&quot;buildNoType&quot;)" :error-msg="errors.first(&quot;buildNoType&quot;)">
                    <enum-input :list="buildNoRules" :disabled="true" name="buildNoType" v-validate.initial="&quot;required|string&quot;" :value="buildNo.buildNoType" />
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

    export default {
        components: {
            EnumInput,
            VuexInput,
            FormField
        },
        props: {
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
        data () {
            return {
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules'
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
                        label: this.$t('preview.majorVersion'),
                        type: 'STRING',
                        default: '0',
                        placeholder: 'MajorVersion'
                    },
                    MinorVersion: {
                        label: this.$t('preview.minorVersion'),
                        type: 'STRING',
                        default: '0',
                        placeholder: 'MinorVersion'
                    },
                    FixVersion: {
                        label: this.$t('preview.fixVersion'),
                        type: 'STRING',
                        default: '0',
                        placeholder: 'FixVersion'
                    }
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
