<template>
    <section>
        <bk-alert type="info" :title="$t('可在插件中引用该变量，用于设置版本号或其他需要用到该变量的地方')" closable></bk-alert>
        
        <atom-checkbox
            style="margin-top: 16px;"
            :disabled="disabled"
            :text="$t('设置版本号')"
            :value="showVersions"
            :handle-change="(name, value) => toggleVersions(name, value)"
        />
        <bk-form v-if="showVersions" form-type="vertical" :label-width="400" class="new-ui-form" style="margin-top: 16px">
            <bk-form-item>
                <div class="layout-label">
                    <label class="ui-inner-label">
                        <span class="bk-label-text">{{ $t('版本号') }}</span>
                        <span class="bk-label-text desc-text">（{{ $t('主版本.特性版本.修正版本') }}）</span>
                    </label>
                </div>
                <div class="version-options">
                    <form-field class="version-form-field" v-for="(v, index) in allVersionKeyList" :key="v" :required="v.required" :is-error="errors.has(v)" :error-msg="errors.first(v)">
                        <vuex-input
                            v-validate.initial="'required|numeric'"
                            input-type="number"
                            class="version-input"
                            :disabled="disabled"
                            :name="v"
                            :value="versionValues[v]"
                            :handle-change="handleVersionChange"
                        />
                        <span v-if="index < allVersionKeyList.length - 1" class="version-dot">.</span>
                    </form-field>
                </div>
            </bk-form-item>

            <form-field class="buildno-form-field" :required="true" :label="$t('buildNum')" :is-error="errors.has('buildNo')" :error-msg="errors.first('buildNo')">
                <vuex-input
                    input-type="number"
                    name="buildNo"
                    placeholder="BK_CI_BUILD_NO"
                    style="width: 228px;"
                    v-validate.initial="'required|numeric'"
                    :disabled="(isPreview && buildNo.buildNoType !== 'CONSISTENT') || disabled"
                    :value="buildNo.buildNo"
                    :handle-change="handleBuildNoChange"
                />
            </form-field>

            <form-field class="buildno-form-field" :required="true" :is-error="errors.has('buildNoType')" :error-msg="errors.first('buildNoType')">
                <enum-input :list="buildNoRules" :disabled="disabled || isPreview" name="buildNoType" v-validate.initial="'required|string'" :value="buildNo.buildNoType" :handle-change="handleBuildNoChange" />
            </form-field>
        </bk-form>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import { allVersionKeyList, getVersionConfig } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'

    export default {
        components: {
            FormField,
            VuexInput,
            EnumInput,
            AtomCheckbox
        },
        props: {
            params: {
                type: Array,
                default: () => ([])
            },
            buildNo: {
                type: Object,
                default: () => ({})
            },
            updateContainerParams: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                showVersions: false
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
            allVersionKeyList () {
                return allVersionKeyList
            },
            versionConfig () {
                return getVersionConfig()
            }
        },
        created () {
            this.showVersions = this.versions.length !== 0
        },
        methods: {
            handleVersionChange (name, value) {
                const version = this.versions.find(v => v.id === name)
                if (version && typeof version === 'object') {
                    Object.assign(version, {
                        defaultValue: value
                    })
                }
                this.updateContainerParams('params', [
                    ...this.globalParams,
                    ...this.versions
                ])
            },
            handleBuildNoChange (name, value) {
                this.updateContainerParams('buildNo', {
                    ...this.buildNo,
                    [name]: value
                })
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
            }
        }
    }
</script>

<style lang="scss" scoped>
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
            background: #FAFBFD;
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
