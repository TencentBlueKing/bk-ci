<template>
    <section class="codelib-detail" v-bkloading="{ isLoading }">
        <div class="detail-header">
            <div
                v-if="!isEditing"
                class="codelib-name"
            >
                <span class="mr5">{{ repoInfo.aliasName }}</span>
                <span @click="handleEditName">
                    <Icon
                        name="edit2"
                        size="12"
                        class="edit-icon"
                    />
                </span>
                <span>
                    <Icon
                        name="delete"
                        size="12"
                        class="delete-icon"
                        @click="handleDeleteCodeLib"
                    />
                </span>
            </div>
            <div v-else>
                <bk-input
                    class="aliasName-input"
                    ref="aliasNameInput"
                    @blur="handleBlur"
                    @enter="handleBlur"
                >
                </bk-input>
                <bk-button
                    class="ml5 mr5"
                    text
                    @click="handleSave"
                >
                    {{ $t('codelib.save') }}
                </bk-button>
                <bk-button
                    text
                    @click="handleCancelEdit"
                >
                    {{ $t('codelib.cancel') }}
                </bk-button>
            </div>
            <div class="address-content">
                <a
                    class="codelib-address"
                    v-bk-overflow-tips
                    @click="handleToRepo(repoInfo.url)"
                >
                    {{ repoInfo.url }}
                </a>
                <span @click="handleCopy">
                    <Icon
                        name="copy"
                        size="14"
                        class="copy-icon"
                    />
                </span>
            </div>
        </div>
        <bk-tab :active.sync="active" type="unborder-card">
            <bk-tab-panel
                v-for="(panel, index) in panels"
                v-bind="panel"
                :key="index">
                <component
                    :is="componentName"
                    :repo-info="repoInfo"
                    :type="repoInfo['@type']"
                >
                </component>
            </bk-tab-panel>
        </bk-tab>
    </section>
</template>
<script>
    import {
        REPOSITORY_API_URL_PREFIX
    } from '../../store/constants'
    import basicSetting from './basic-setting.vue'
    export default {
        name: 'CodeLibDetail',
        components: {
            basicSetting
        },
        props: {
            curRepoId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                isEditing: false,
                isLoading: false,
                panels: [
                    { name: 'basic', label: this.$t('codelib.basicSetting') }
                ],
                active: 'basic',
                repoInfo: {}
            }
        },
        computed: {
            componentName () {
                const comMap = {
                    basic: 'basicSetting'
                }
                return comMap[this.active]
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            curRepoId: {
                handler (val) {
                    this.fetchRepoDetail(val)
                },
                immediate: true
            }
        },
        created () {
        },
        methods: {
            async fetchRepoDetail (id) {
                this.isLoading = true
                await this.$ajax.get(`${REPOSITORY_API_URL_PREFIX}/user/repositories/${this.projectId}/${id}?repositoryType=ID`)
                    .then((res) => {
                        this.repoInfo = res
                        console.log(res, 123)
                    }).finally(() => {
                        this.isLoading = false
                    })
            },
            handleEditName () {
                this.isEditing = true
                setTimeout(() => {
                    this.$refs.aliasNameInput.focus()
                })
            },
            handleSave () {
                this.isEditing = false
            },

            handleCancelEdit () {
                this.isEditing = false
            },

            handleCopy () {
                const textarea = document.createElement('textarea')
                document.body.appendChild(textarea)
                textarea.value = this.repoInfo.aliasName
                textarea.select()
                if (document.execCommand('copy')) {
                    document.execCommand('copy')
                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('codelib.copySuccess')
                    })
                }
                document.body.removeChild(textarea)
            },

            handleToRepo (url) {
                window.open(url, '__blank')
            }
        }
    }
</script>
<style lang='scss' scoped>
    .codelib-detail {
        height: 100%;
        .detail-header {
            display: flex;
            justify-content: space-between;
            flex: 1;
            height: 48px;
            line-height: 48px;
            background: #FAFBFD;
            padding: 0 24px;
        }
        .codelib-name {
            font-size: 16px;
            color: #313238;
            margin-right: 30px;
            max-width: 450px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            &:hover {
                .edit-icon,
                .delete-icon {
                    display: inline;
                }
            }
        }

        .aliasName-input {
            width: 400px;
            line-height: 48px;
        }

        .edit-icon,
        .delete-icon {
            cursor: pointer;
            margin-left: 5px;
            display: none;
        }
        .address-content {
            white-space: nowrap;
        }
        .codelib-address {
            display: inline-block;
            max-width: 480px;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
        }
        .copy-icon {
            margin-left: 10px;
            position: relative;
            top: -16px;
            cursor: pointer;
        }
    }
</style>
