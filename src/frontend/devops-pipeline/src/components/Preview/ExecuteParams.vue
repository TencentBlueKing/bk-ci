<template>
    <bk-collapse class="execute-params-collapse" v-model="activeName">
        <bk-collapse-item v-if="buildList.length" custom-trigger-area name="1">
            <header class="params-collapse-trigger">
                {{ $t('preview.build') }}
                <i class="devops-icon icon-arrow-right" />
            </header>
            <div slot="content" class="params-collapse-content">
                <pipeline-params-form
                    ref="buildForm"
                    :param-values="buildValues"
                    :handle-param-change="handleBuildChange"
                    :params="buildList"
                />
            </div>
        </bk-collapse-item>
        <bk-collapse-item name="2" custom-trigger-area v-if="isVisibleVersion">
            <header class="params-collapse-trigger">
                {{ $t('preview.introVersion') }}
            </header>
            <div slot="content" class="params-collapse-content">
                <pipeline-versions-form
                    ref="versionParamForm"
                    :build-no="buildNo"
                    :is-preview="true"
                    :version-param-values="versionParamValues"
                    :handle-version-change="handleVersionChange"
                    :handle-build-no-change="handleBuildNoChange"
                ></pipeline-versions-form>
            </div>
        </bk-collapse-item>
        <bk-collapse-item name="3" custom-trigger-area>
            <header class="params-collapse-trigger">
                {{ $t('template.pipelineVar') }}
                <span class="collapse-trigger-divider">|</span>
                <span v-if="isDebug" class="text-link" @click.stop="updateParams">
                    {{ $t('resetDefault') }}
                    <i class="devops-icon icon-question-circle" v-bk-tooltips="$t('debugParamsTips')" />
                </span>
                <span v-else class="text-link" @click.stop="updateParams('value')">
                    {{ $t('useLastParams') }}
                </span>
            </header>
            <div slot="content" class="params-collapse-content">
                <pipeline-params-form
                    ref="paramsForm"
                    :param-values="paramsValues"
                    :handle-param-change="handleParamChange"
                    :params="paramList"
                />
            </div>
        </bk-collapse-item>
    </bk-collapse>
</template>

<script>
    import { mapActions, mapGetters } from 'vuex'
    import { getParamsValuesMap } from '@/utils/util'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { bus } from '@/utils/bus'

    export default {
        components: {
            PipelineParamsForm,
            PipelineVersionsForm
        },
        props: {
            startupInfo: {
                type: Object,
                required: true
            },
            isDebug: Boolean
        },
        data () {
            return {
                activeName: [1, 2, 3],
                paramList: [],
                versionParamList: [],
                paramsValues: {},
                versionParamValues: {},
                buildNo: {},
                buildValues: {},
                buildList: []
            }
        },
        computed: {
            ...mapGetters('pipelines', [
                'getExecuteParams'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            }
        },
        watch: {
            startupInfo: {
                deep: true,
                immediate: true,
                handler (startupInfo) {
                    startupInfo && this.init(startupInfo)
                }
            }
        },
        mounted () {
            bus.$off('validate-execute-param-form')
            bus.$on('validate-execute-param-form', this.handleValidate)
        },
        beforeDestroy () {
            bus.$off('validate-execute-param-form', this.handleValidate)
        },
        methods: {
            ...mapActions('pipelines', [
                'setExecuteParams'
            ]),
            init (startupInfo) {
                if (startupInfo.canManualStartup) {
                    const values = this.getExecuteParams(this.pipelineId)
                    if (startupInfo.buildNo) {
                        this.buildNo = startupInfo.buildNo
                        this.isVisibleVersion = startupInfo.buildNo.required
                    }
                    this.paramList = startupInfo.properties.filter(p => p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD')
                    this.versionParamList = startupInfo.properties.filter(p => allVersionKeyList.includes(p.id))
                    this.buildList = startupInfo.properties.filter(p => p.propertyType === 'BUILD')
                    this.initParams(values)
                } else {
                    this.$bkMessage({
                        theme: 'error',
                        message: this.$t('newlist.withoutManualAtom')
                    })
                }
            },
            initParams (values) {
                this.paramsValues = getParamsValuesMap(this.paramList, 'defaultValue', values)
                this.versionParamValues = getParamsValuesMap(this.versionParamList, 'defaultValue', values)
                this.buildValues = getParamsValuesMap(this.buildList, 'defaultValue', values)
            },
            updateParams (valueKey = 'defaultValue') {
                this.paramsValues = getParamsValuesMap(this.paramList, valueKey)
                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    ...this.paramsValues
                })
            },
            toggleIcon (type) {
                if (type === 'version') this.isDropdownShowVersion = !this.isDropdownShowVersion
                else if (type === 'params') this.isDropdownShowParam = !this.isDropdownShowParam
                else this.isDropdownShowBuild = !this.isDropdownShowBuild
            },
            async handleValidate (done) {
                const result = await this.validateForm()
                if (!result) {
                    this.$showTips({
                        message: this.$t('preview.paramsInvalidMsg'),
                        theme: 'error'
                    })
                }
                done(result)
            },
            async validateForm (type) {
                switch (type) {
                    case 'versionParamForm':
                        return await this.$refs?.versionParamForm?.$validator?.validateAll?.() ?? true
                    case 'paramsForm':
                        return await this.$refs?.paramsForm?.$validator?.validateAll?.() ?? true
                    case 'buildForm':
                        return await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                    default: {
                        const versionValid = await this.$refs?.versionParamForm?.$validator?.validateAll?.() ?? true
                        const paramsFormValid = await this.$refs?.paramsForm?.$validator?.validateAll?.() ?? true
                        const buildFormValid = await this.$refs?.buildForm?.$validator?.validateAll?.() ?? true
                        return versionValid && paramsFormValid && buildFormValid
                    }
                }
            },
            handleChange (type, name, value) {
                this[`${type}Values`][name] = value
                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    ...this[`${type}Values`]
                })
            },
            handleBuildChange (...args) {
                this.handleChange('build', ...args)
            },
            handleParamChange (...args) {
                this.handleChange('params', ...args)
            },
            handleVersionChange (...args) {
                this.handleChange('versionParam', ...args)
            },
            handleBuildNoChange (name, value) {
                this.buildNo.buildNo = value

                this.setExecuteParams({
                    pipelineId: this.pipelineId,
                    buildNo: this.buildNo
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';

    .execute-params-collapse {
        margin: 24px;
        padding: 12px;
        background: white;
        box-shadow: 0 2px 2px 0 #00000026;
        flex: 1;
        overflow: auto;
        .params-collapse-trigger {
            display: flex;
            align-items: center;
            .collapse-trigger-divider {
                display: inline-block;
                margin: 0 10px;
                color: #DCDEE5;
            }
            .text-link .icon-question-circle {
                display: inline-block;
                color: #979BA5;
                margin-left: 4px;
            }
        }
        .params-collapse-content {
            padding-top: 16px;
            border-top: 1px solid #DCDEE5;
        }
    }
</style>
