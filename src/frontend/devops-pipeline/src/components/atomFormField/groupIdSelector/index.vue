<template>
    <div class="code-mode-check bk-form">
        <div class="bk-form-item">
            <template>
                <label class="bk-label" v-if="label">{{ label }}：</label>
                <bk-popover theme="light" :placement="descDirection" v-if="desc">
                    <i class="bk-icon" :class="[iconClass ? iconClass : 'icon-info-circle']"></i>
                    <div slot="content" style="white-space: pre-wrap;font-weight: normal;">
                        <div>{{ desc }}</div>
                    </div>
                </bk-popover>
                <div class="bk-form-content">
                    <multiple-complete :name="name" type="text"
                        :value="value"
                        :disabled="disabled"
                        :placeholder="placeholder"
                        :config="dataInputConfig" />
                </div>
            </template>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import MultipleComplete from '@/components/common/multiple-complete'

    export default {
        name: 'group-id-selector',
        components: {
            MultipleComplete
        },
        mixins: [atomFieldMixin],
        props: {
            text: {
                type: String
            },
            repositoryHashId: {
                type: String,
                required: true
            },
            branchName: {
                type: String,
                default: ''
            },
            elementType: {
                type: String,
                default: ''
            },
            name: {
                type: String,
                default: ''
            },
            value: {
                type: String,
                default: ''
            },
            placeholder: {
                type: String
            },
            label: {
                type: String
            },
            desc: {
                type: String
            },
            iconClass: {
                type: String
            },
            descDirection: {
                type: String,
                default: 'right'
            }
        },
        data () {
            return {
                noPermission: true,
                isLoading: false,
                list: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            dataInputConfig () {
                return {
                    data: this.list || [],
                    onChange: this.onChange
                }
            }
        },
        mounted () {
            this.list = localStorage.getItem('groupIdStr') ? localStorage.getItem('groupIdStr').split(';').filter(item => item) : []
        },
        methods: {
            onChange (name, value) {
                this.value = value
                this.handleChange(name, value)
            }
        }
    }
</script>

<style lang="scss">
    .code-mode-check {
        .bk-form-content .bk-form-radio {
            margin-right: 10px;
            line-height: 22px;
        }
        .bk-form-vertical {
            margin-bottom: 0;
        }
    }
</style>
