<template>
    <div class="code-mode-check-selector bk-form-row bk-form">
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
                    <vuex-input v-if="gitPullModeType === 'COMMIT_ID'" :disabled="noPermission" :placeholder="gitPullModeList[gitPullModeType].placeholder" name="value" :handle-change="handleValueChange" :value="gitPullModeVal"></vuex-input>
                    <select-input v-else name="value" :value="gitPullModeVal" :disabled="noPermission" type="text" :placeholder="isLoading ? selectorLoadingTips : gitPullModeList[gitPullModeType].placeholder" v-bind="dataInputConfig" />
                </div>
            </template>
        </div>
    </div>
</template>

<script>
    import atomFieldMixin from '../atomFieldMixin'
    import EnumInput from '../EnumInput'
    import VuexInput from '../VuexInput'
    import SelectInput from '@/components/AtomFormComponent/SelectInput'

    export default {
        name: 'code-mode-selector',
        components: {
            EnumInput,
            VuexInput,
            SelectInput
        },
        mixins: [atomFieldMixin],
        props: {
            text: {
                type: String
            },
            repositoryType: {
                type: String,
                required: true
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
                selectorLoadingTips: this.$t('editPage.loadingData'),
                list: [],
                gitPullModeType: (this.value && this.value.type) || '',
                gitPullModeVal: (this.value && this.value.value) || '',
                gitPullModeList: {
                    BRANCH: {
                        label: this.$t('editPage.branch'),
                        placeholder: this.$t('editPage.branchTips'),
                        url: '/process/api/user/scm/{projectId}/{repositoryHashId}/branches',
                        default: 'master'
                    },
                    TAG: {
                        label: 'Tag',
                        placeholder: this.$t('editPage.tagTips'),
                        url: '/process/api/user/scm/{projectId}/{repositoryHashId}/tags',
                        default: ''
                    },
                    COMMIT_ID: {
                        label: 'CommitID',
                        placeholder: this.$t('editPage.commitIdTips'),
                        default: ''
                    }
                },
                gitPullModes: [
                    {
                        id: 'branch',
                        value: 'BRANCH',
                        label: this.$t('editPage.branchLabel')
                    },
                    {
                        id: 'tag',
                        value: 'TAG',
                        label: this.$t('editPage.tagLabel')
                    },
                    {
                        id: 'commitId',
                        value: 'COMMIT_ID',
                        label: this.$t('editPage.commitIdLabel')
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
                    options: this.list.map(item => ({ id: item, name: item })),
                    handleChange: this.onChange
                }
            }
        },
        watch: {
            'elementType' (value) {
                this.gitPullModeType = 'BRANCH'
                this.changeGitPullMode(true)
            },
            repositoryType (value) {
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
                if (this.repositoryHashId && this.gitPullModeType && this.gitPullModeType !== 'COMMIT_ID' && !this.noPermission && this.repositoryType !== 'NAME') {
                    this.freshList(this.gitPullModeList[this.gitPullModeType].url)
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
            },
            async freshList (url) {
                try {
                    const query = this.$route.params
                    const changeUrl = this.urlParse(url, {
                        bkPoolType: this?.container?.dispatchType?.buildType,
                        repositoryHashId: this.repositoryHashId,
                        ...query
                    })
                    this.isLoading = true
                    const res = await this.$ajax.get(changeUrl)

                    this.list = res.data || []
                } catch (e) {
                    this.$showTips({
                        message: e.message,
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
    .code-mode-check-selector {
        .bk-form-content .bk-form-radio {
            margin-right: 10px;
            line-height: 32px;
        }
        .bk-form-vertical {
            margin-bottom: 0;
        }
    }
</style>
