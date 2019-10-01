<template>
    <div class="container-node-selector">
        <enum-input v-if="showAgentType"
            class="agent-type"
            name="agentType"
            :list="agentTypeList"
            :disabled="disabled"
            :handle-change="handleSelect"
            :value="agentType">
        </enum-input>
        <div :class="{ 'agent-name-select': true, 'abnormal-tip': abnormalSlave }" v-if="showAgentById">
            <selector
                name="value"
                :disabled="disabled"
                :handle-change="handleSelect"
                :list="nodeList"
                :value="value"
                :toggle-visible="toggleAgentList"
            >
                <template>
                    <div class="bk-selector-create-item cursor-pointer" @click.stop.prevent="addThridSlave">
                        <i class="bk-icon icon-plus-circle"></i>
                        <span class="text">新增第三方构建机</span>
                    </div>
                </template>
            </selector>
        </div>
        <div :class="{ 'alias-name-select': true, 'is-focus-selector': isFocus }" v-else>
            <span v-if="!isFocus" :class="{ 'build-resource-label': true, 'disabled': disabled, 'abnormal-tip': abnormalSlave }">{{ buildResource }}</span>
            <select-input
                name="value"
                :disabled="disabled"
                :is-loading="isLoading"
                :handle-change="handleSelect"
                :options="nodeList"
                :has-error="hasError"
                @focus="handleFocus"
                @blur="handleBlur"
                :value="value"
            >
            </select-input>
        </div>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import Selector from '@/components/atomFormField/Selector'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'

    export default {
        name: 'container-node-selector',
        components: {
            EnumInput,
            Selector,
            SelectInput
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
            addThridSlave: {
                type: Function,
                default: () => () => {}
            },
            hasError: {
                type: Boolean
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
                return !!(this.buildResource && this.buildResource.indexOf('（异常）') > 0) || false
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
            agentTypeList () {
                return [
                    { label: `按${this.isAgentId ? '节点' : '环境'}选择`, value: 'ID' },
                    { label: `按${this.isAgentId ? '节点' : '环境'}别名输入`, value: 'NAME' }
                ]
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
                this.handleChange(name, value)
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
                            name: resource.name + (resource.label ? resource.label : ''),
                            disalbed: !resource.name
                        }))
                    }

                    // 第三方构建机（节点/环境）选择添加无权限查看项
                    if (this.showAgentById && this.value !== '' && this.nodeList.filter(item => item.id === this.value).length === 0) {
                        this.nodeList.splice(0, 0, {
                            id: this.value,
                            name: '******（无权限查看）'
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
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    @import '../../scss/mixins/ellipsis';
    .container-node-selector {

        .alias-name-select {
            &:not(.is-focus-selector) {
                input,
                input[disabled] {
                    color: transparent !important;
                    background-color: transparent !important;
                }
                .build-resource-label.disabled {
                    background-color: #fafafa;
                    color: #aaaaaa;
                }
            }
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

        .agent-type {
            margin-bottom: 8px;
        }
    }
</style>
