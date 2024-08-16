<template>
    <ul class="manage-code-repo">
        <li v-for="(code, index) in list" :key="index" class="code-item">
            <span class="item-name">{{ code.label }}</span>
            <span :class="[{ 'item-link': code.link }, 'item-value']" :title="code.value" @click="goToLink(code.link)">{{ code.value || '--' }}</span>
            <span v-if="code.tool && code.tool.show" class="item-tool">
                <span v-bk-tooltips="{ content: code.tool.info, placements: ['top'] }" v-if="code.tool.info">
                    <i class="bk-icon icon-info-circle"></i>
                </span>
                <span @click="code.tool.click()" class="item-tool-label item-link" :title="code.tool.title">{{ code.tool.label }}</span>
            </span>
        </li>
    </ul>
</template>

<script>
    import { copyString } from '@/utils/index'

    export default {
        props: {
            userInfo: Object,
            detail: Object,
            type: String
        },

        data () {
            return {
                list: []
            }
        },

        watch: {
            detail: {
                handler () {
                    this.initData()
                },
                immediate: true
            }
        },

        methods: {
            initData () {
                const methodGenerator = {
                    atom: this.getAtomData,
                    service: this.getServiceData
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod()
            },

            getAtomData () {
                this.list = [
                    { label: this.$t('store.开发语言：'), value: this.detail.language }
                ]

                if (VERSION_TYPE !== 'ee') {
                    this.list = [
                        { label: this.$t('store.开发语言：'), value: this.detail.language },
                        { label: this.$t('store.已托管至：'), value: this.$t('store.工蜂'), link: 'https://git.woa.com/' },
                        { label: this.$t('store.代码库：'), value: this.detail.codeSrc, tool: { show: true, label: this.$t('store.复制'), click: () => copyString(this.detail.codeSrc) } },
                        {
                            label: this.$t('store.授权人：'),
                            value: this.detail.repositoryAuthorizer,
                            tool: {
                                show: this.userInfo.isProjectAdmin && this.userInfo.userName !== this.detail.repositoryAuthorizer,
                                info: this.$t('store.在发布插件时，使用授权人的身份拉取插件代码自动构建打包，或设置插件可见范围'),
                                label: this.$t('store.重置授权'),
                                title: this.$t('store.将使用你的身份进行插件代码库相关操作'),
                                click: this.modifyRepoMemInfo
                            }
                        }
                    ]
                }
            },

            getServiceData () {
                this.list = []

                if (VERSION_TYPE !== 'ee') {
                    this.list = [
                        { label: this.$t('store.已托管至：'), value: this.$t('store.工蜂'), link: 'https://git.woa.com/' },
                        { label: this.$t('store.代码库：'), value: this.detail.codeSrc, tool: { show: true, label: this.$t('store.复制'), click: () => copyString(this.detail.codeSrc) } },
                        {
                            label: this.$t('store.授权人：'),
                            value: this.detail.repositoryAuthorizer,
                            tool: {
                                show: this.userInfo.isProjectAdmin && this.userInfo.userName !== this.detail.repositoryAuthorizer,
                                info: this.$t('store.在发布微扩展时，使用授权人的身份拉取微扩展代码自动构建打包，或设置微扩展可见范围'),
                                label: this.$t('store.重置授权'),
                                click: this.modifyRepoMemInfo
                            }
                        }
                    ]
                }
            },

            modifyRepoMemInfo () {
                const projectCode = this.detail.projectCode
                this.$store.dispatch('store/checkIsOAuth').then((res) => {
                    if (res.status === 403) {
                        window.open(res.url, '_self')
                        return
                    }
                    const methodMap = {
                        atom: () => this.$store.dispatch('store/modifyRepoMemInfo', { atomCode: this.detail.atomCode, projectCode }),
                        service: () => this.$store.dispatch('store/resetServiceGit', { serviceCode: this.detail.serviceCode, projectCode })
                    }
                    const type = this.$route.params.type

                    return methodMap[type]().then((res) => {
                        if (res) {
                            this.detail.repositoryAuthorizer = this.userInfo.userName
                            this.$store.dispatch('store/setDetail', { repositoryAuthorizer: this.detail.repositoryAuthorizer })
                            this.$bkMessage({ message: this.$t('store.重置授权成功'), theme: 'success', limit: 1 })
                        }
                    })
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' }))
            },

            goToLink (link) {
                if (link) window.open(link, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-code-repo {
        margin-top: 8px;
        font-size: 14px;
        line-height: 20px;
        .code-item {
            padding: 8px 0;
            display: flex;
            align-items: center;
            .item-name {
                display: inline-block;
                color: #999999;
                width: 77px;
                text-align: right;
            }
            .item-value {
                max-width: calc(3.81rem - 215px);
                display: inline-block;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .item-link {
                color: #1592ff;
                cursor: pointer;
            }
            .icon-info-circle {
                margin-left: 5px;
            }
        }
    }
</style>
