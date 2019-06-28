<template>
    <div :class="{ 'container-node-selector': true, 'is-focus-selector': isFocus }">
        <span v-if="!isFocus" :class="{ 'build-resource-label': true, 'disabled': disabled }">{{ buildResource }}</span>
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
</template>

<script>
    import { mapActions } from 'vuex'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'

    export default {
        name: 'container-node-selector',
        components: {
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
            async getResourceList () {
                try {
                    this.isLoading = true
                    this.nodeList = []
                    const { data: { resources } } = await this.fetchBuildResourceByType({
                        projectCode: this.projectId,
                        os: this.os,
                        buildType: this.buildResourceType,
                        containerId: this.containerId
                    })
                    if (Array.isArray(resources)) {
                        this.nodeList = resources.map((resource, index) => ({
                            ...resource,
                            id: resource.name,
                            name: resource.name + (resource.label ? resource.label : ''),
                            disalbed: !resource.name
                        }))
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
</style>
