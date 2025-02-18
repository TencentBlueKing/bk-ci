<template>
    <div class="container-node-selector">
        <enum-input
            v-if="showAgentType"
            class="agent-type"
            name="agentType"
            :list="agentTypeList"
            :disabled="disabled"
            :handle-change="handleChange"
            :value="agentType"
        >
        </enum-input>
        <div
            :class="{ 'agent-name-select': true, 'abnormal-tip': abnormalSlave }"
            v-if="showAgentById"
        >
            <selector
                name="value"
                :disabled="disabled"
                :handle-change="handleSelect"
                :list="nodeList"
                :value="value"
                :toggle-visible="toggleAgentList"
            >
                <template
                    v-if="isAgentEnv"
                    v-slot:option-item="optionProps"
                >
                    <div class="env-option-item">
                        <span>{{ optionProps.name }}</span>
                        <span
                            v-if="optionProps.isShared"
                            class="env-info"
                        >{{ $t('editPage.shareEnvInfo', [optionProps.sharedProjectId, optionProps.sharedUserId]) }}</span>
                        <bk-link
                            target="_blank"
                            class="env-link"
                            :href="optionProps.envInfoHref"
                            theme="primary"
                        >
                            {{ $t('newlist.view') }}
                        </bk-link>
                    </div>
                </template>

                <template>
                    <div
                        class="env-import-entry cursor-pointer"
                        @click.stop.prevent="addThirdSlave"
                    >
                        <i class="devops-icon icon-plus-circle"></i>
                        <span class="text">{{ $t('editPage.addThirdSlave') }}</span>
                    </div>
                </template>
            </selector>
        </div>
        <div
            class="alias-name-select"
            v-else
        >
            <div class="env-alias-area">
                <form-field
                    :is-error="hasError"
                    :required="required"
                    class="env-alias-area-item"
                    :label="(isAgentEnv && !isReuseJob) ? $t('editPage.environment') : ''"
                >
                    <select-input
                        v-if="isReuseJob"
                        name="value"
                        :disabled="disabled"
                        :value="getReuseJobNameByValue(value)"
                        :options="reuseJobList"
                        :handle-change="handleSelect"
                    >
                    </select-input>
                    <devops-select
                        v-else
                        name="value"
                        :disabled="disabled"
                        :is-loading="isLoading"
                        :handle-change="handleChange"
                        :options="nodeList"
                        @focus="handleFocus"
                        @blur="handleBlur"
                        :value="value"
                    />
                </form-field>
                <form-field
                    v-if="isAgentEnv && !isReuseJob"
                    :required="false"
                    class="env-alias-area-item"
                    :label="$t('editPage.envProjectId')"
                    :desc="$t('editPage.envProjectTips')"
                >
                    <vuex-input
                        input-type="string"
                        name="envProjectId"
                        :disabled="disabled"
                        :placeholder="$t('editPage.envProjectPlaceholder')"
                        :value="envProjectId"
                        :handle-change="handleChange"
                    />
                </form-field>
            </div>
        </div>
    </div>
</template>

<script>
    import DevopsSelect from '@/components/AtomFormComponent/DevopsSelect'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'
    import { mapActions } from 'vuex'

    export default {
        name: 'container-node-selector',
        components: {
            EnumInput,
            FormField,
            VuexInput,
            Selector,
            SelectInput,
            DevopsSelect
        },
        props: {
            os: {
                type: String
            },
            name: {
                type: String
            },
            containerId: {
                type: String
            },
            value: {
                type: String,
                default: ''
            },
            envProjectId: {
                type: String,
                default: ''
            },
            buildResourceType: {
                type: String
            },
            buildImageType: {
                type: String,
                default: 'BKDEVOPS'
            },
            agentType: {
                type: String,
                default: 'ID'
            },
            disabled: {
                type: Boolean,
                default: false
            },
            handleChange: {
                type: Function,
                required: false
            },
            toggleVisible: {
                type: Function,
                default: () => () => {}
            },
            addThirdSlave: {
                type: Function,
                default: () => () => {}
            },
            hasError: {
                type: Boolean
            },
            required: Boolean,
            pipeline: {
                type: Object,
                default: () => () => {}
            },
            stageIndex: {
                type: Number
            },
            containerIndex: {
                type: Number
            },
            stage: {
                type: Object,
                default: () => () => {}
            }
        },
        data () {
            return {
                isLoading: false,
                isFocus: false,

                nodeList: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            buildResource () {
                const node = this.nodeList.find(node => node.name && node.id === this.value)
                const label = node && typeof node.label === 'string' ? node.label : ''
                return this.value ? this.value + label : this.value
            },
            abnormalSlave () {
                return !!(this.buildResource && this.buildResource.indexOf(`（${this.$t('editPage.addThirdSlave')}）`) > 0) || false
            },
            isAgentId () {
                return this.buildResourceType === 'THIRD_PARTY_AGENT_ID'
            },
            showAgentType () {
                return ['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV'].includes(this.buildResourceType)
            },
            showAgentById () {
                return this.showAgentType && this.agentType === 'ID'
            },
            isReuseJob () {
                return this.agentType === 'REUSE_JOB_ID'
            },
            isAgentEnv () {
                return this.buildResourceType === 'THIRD_PARTY_AGENT_ENV'
            },
            agentTypeList () {
                return this.isAgentId
                    ? [
                        { label: this.$t('editPage.selectSlave'), value: 'ID' },
                        { label: this.$t('editPage.inputSlave'), value: 'NAME' },
                        { label: this.$t('editPage.locksSlave'), value: 'REUSE_JOB_ID', tips: this.$t('editPage.locksSlaveTips') }
                    ]
                    : [
                        { label: this.$t('editPage.selectEnv'), value: 'ID' },
                        { label: this.$t('editPage.inputEnv'), value: 'NAME' },
                        { label: this.$t('editPage.locksSlave'), value: 'REUSE_JOB_ID', tips: this.$t('editPage.locksSlaveTips') }
                    ]
            },
            reuseJobList () {
                if (!this.pipeline) return []
                const list = []
                const curJobId = this.stage.containers[this.containerIndex]?.jobId || ''
                const isTrigger = this.pipeline?.stages[0]?.isTrigger
                this.pipeline.stages && this.pipeline.stages.forEach((stage, index) => {
                    if ((!isTrigger || index !== 0) && index <= this.stageIndex) {
                        stage && stage.containers.forEach((container, containerIndex) => {
                            list.push(
                                {
                                    id: container.jobId || Math.random(),
                                    name: `Job${index + 1}-${containerIndex + 1}${!container.jobId ? ' (该job未设置Job ID)' : ' (Job ID: ' + container.jobId + ')'} `,
                                    disabled: !container.jobId
                                }
                            )
                        })
                    }
                })

                return list.filter(i => i.id !== curJobId)
            }
        },
        watch: {
            value (nval, oldVal) {
                nval !== oldVal && this.$emit('input', nval)
            }
        },
        created () {
            this.getResourceList()
        },
        methods: {
            ...mapActions('atom', [
                'fetchBuildResourceByType'
            ]),
            handleSelect (name, value) {
                const node = this.nodeList.find(item => item.id === value)
                const sharedId = node?.sharedProjectId
                this.handleChange(name, value, sharedId)
            },
            handleBlur () {
                this.isFocus = false
            },
            handleFocus () {
                if (!this.isFocus) {
                    this.isFocus = true
                    this.getResourceList()
                }
            },
            toggleAgentList (isShow) {
                if (isShow) this.getResourceList()
            },
            async getResourceList () {
                try {
                    this.isLoading = true
                    this.nodeList = []
                    let { data: { resources } } = await this.fetchBuildResourceByType({
                        projectCode: this.projectId,
                        os: this.os,
                        buildType: this.buildResourceType,
                        containerId: this.containerId
                    })
                    if ((this.buildResourceType === 'DOCKER') && this.buildImageType === 'THIRD') resources = []
                    if (Array.isArray(resources)) {
                        this.nodeList = resources.map((resource, index) => ({
                            ...resource,
                            id: this.showAgentById ? resource.id : resource.name,
                            isShared: !!resource.sharedProjectId && !!resource.sharedUserId,
                            name: resource.name + (resource.label ? resource.label : ''),
                            envInfoHref: `${WEB_URL_PREFIX}/environment/${resource.sharedProjectId || this.projectId}/envDetail/${resource.id}/`,
                            disalbed: !resource.name
                        }))
                    }
                    console.log(this.nodeList, this.showAgentById, resources)

                    // 第三方构建机（节点/环境）选择添加无权限查看项
                    if (this.showAgentById && this.value !== '' && this.nodeList.filter(item => item.id === this.value).length === 0) {
                        this.nodeList.splice(0, 0, {
                            id: this.value,
                            name: `******（${this.$t('editPage.noPermToView')}）`
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            getReuseJobNameByValue (value) {
                return this.reuseJobList.find(i => i.id === value)?.name || value
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';
    .container-node-selector {

        .alias-name-select {

            .abnormal-tip {
                color: $dangerColor;
            }

            .build-resource-label {
                position: absolute;
                width: 100%;
                height: 32px;
                line-height: 32px;
                font-size: 12px;
                padding: 0 50px 0 10px;
                @include ellipsis();
            }
        }

        .abnormal-tip {
            .bk-select-name {
                color: $dangerColor;
            }
            .is-disabled .bk-select-name {
                color: #aaaaaa;
            }
        }
    }
    .env-option-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        span:first-child {
            flex: 1;
            @include ellipsis();
            margin-right: 16px;
        }
        .env-info {
            margin-right: 12px;
            color: $fontWeightColor;
        }
        .env-link .bk-link-text {
            font-size: 12px;
        }
    }
    .env-import-entry {
        display: flex;
        align-items: center;
        .text {
            margin-left: 8px;
        }
    }
    .env-alias-area {
        display: flex;
        .env-alias-area-item {
            margin-top: 0px !important;
            flex: 1;
        }
        .env-alias-area-item:first-child {
            margin-right: 12px;
        }
    }
</style>
