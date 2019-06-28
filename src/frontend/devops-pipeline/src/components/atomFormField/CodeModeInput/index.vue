<template>
    <div class="code-mode-check bk-form-row bk-form">
        <div class="bk-form-inline-item" :class="{ 'is-required': required }">
            <label class="bk-label">{{ text }}：</label>
            <div class="bk-form-content">
                <enum-input :list="gitPullModes" :disabled="noPermission" name="type" :handle-change="handleModeChange" :value="gitPullModeType" />
            </div>
        </div>
        <div class="bk-form-inline-item" :class="{ 'is-required': required }">
            <template v-if="gitPullModeType">
                <label class="bk-label">{{ gitPullModeList[gitPullModeType].label }}：</label>
                <div class="bk-form-content">
                    <vuex-input :disabled="noPermission" :placeholder="gitPullModeList[gitPullModeType].placeholder" name="value" :handle-change="handleValueChange" :value="gitPullModeVal"></vuex-input>
                </div>
            </template>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import EnumInput from '../EnumInput'
    import VuexInput from '../VuexInput'

    export default {
        name: 'code-mode-selector',
        components: {
            EnumInput,
            VuexInput
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
            value: {
                type: Object
            },
            noUsePermission: {
                type: Boolean
            }
        },
        data () {
            return {
                noPermission: false,
                isLoading: false,
                list: [],
                gitPullModeType: (this.value && this.value.type) || '',
                gitPullModeVal: (this.value && this.value.value) || '',
                gitPullModeList: {
                    BRANCH: {
                        label: '分支名称',
                        placeholder: '请输入分支',
                        default: 'master'
                    },
                    TAG: {
                        label: 'Tag',
                        placeholder: '请输入Tag',
                        default: ''
                    },
                    COMMIT_ID: {
                        label: 'CommitID',
                        placeholder: '请输入指定CommitID',
                        default: ''
                    }
                },
                gitPullModes: [
                    {
                        id: 'branch',
                        value: 'BRANCH',
                        label: '按分支'
                    },
                    {
                        id: 'tag',
                        value: 'TAG',
                        label: '按TAG'
                    },
                    {
                        id: 'commitId',
                        value: 'COMMIT_ID',
                        label: '按CommitID'
                    }
                ]
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
        watch: {
            'elementType' (value) {
                this.gitPullModeType = 'BRANCH'
                this.changeGitPullMode(true)
            },
            gitPullModeType (value) {
                this.changeGitPullMode(true)
            },
            repositoryHashId (value) {
                this.noPermission = false
                this.changeGitPullMode(true)
            },
            noUsePermission (value) {
                this.noPermission = value
                if (!value) {
                    this.changeGitPullMode(false)
                }
            }
        },
        created () {
            if (this.gitPullModeType) {
                this.changeGitPullMode(false)
            } else {
                this.gitPullModeType = 'BRANCH'
                this.$nextTick(() => {
                    this.branchName && (this.gitPullModeVal = this.branchName)
                    this.handleElementChange()
                })
            }
        },
        methods: {
            onChange (name, value) {
                this.handleValueChange('value', value)
            },
            changeGitPullMode (clear) {
                this.list = []
                if (clear) {
                    this.handleValueChange('value', this.gitPullModeList[this.gitPullModeType].default)
                }
            },
            handleElementChange () {
                this.handleChange('gitPullMode', {
                    type: this.gitPullModeType,
                    value: this.gitPullModeVal
                })
            },
            handleModeChange (name, value) {
                this.gitPullModeType = value
                this.gitPullModeVal = ''
                this.handleElementChange()
            },
            handleValueChange (name, value) {
                this.gitPullModeVal = value
                this.handleElementChange()
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
