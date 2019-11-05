<template>
    <div class="atom-overview-wrapper">
        <div class="inner-header">
            <div class="title"> {{ $t('插件概览') }} </div>
        </div>

        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="atom-overview-container" v-if="showContent">
                <div class="info-content info-content-left">
                    <div class="overview-statistics">
                        <div class="info-title"> {{ $t('总体统计') }} </div>
                        <div class="atom-statistic">
                            <div class="total-item" v-for="(entry, index) in statisticList" :key="index">
                                <div class="item-value" :class="{ 'blue-theme': entry.name === 'commentCnt' || entry.name === 'score' }">{{ entry.value }}</div>
                                <div class="item-label">{{ entry.label }}</div>
                            </div>
                        </div>
                    </div>
                    <div class="trend-chart">
                        <div class="info-title"> {{ $t('趋势图') }} </div>
                        <div class="chart-content">
                            <div class="info-tab">
                                <span class="tab-item"> {{ $t('安装量') }} </span>
                            </div>
                            <div class="chart-item">
                                <img :src="image">
                                <p>{{ $t('功能正在建设中') }}···</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="info-content info-content-right">
                    <div class="atom-code-card">
                        <div class="card-title"> {{ $t('插件代码') }} </div>
                        <div class="card-content">
                            <div class="code-form-item">
                                <div class="info-label"> {{ $t('开发语言：') }} </div>
                                <div class="info-value">{{ codeForm.language }}</div>
                            </div>
                            <div class="code-form-item">
                                <div class="info-label"> {{ $t('已托管至：') }} </div>
                                <div class="info-value link-text" @click="toLink()"> {{ $t('工蜂') }} </div>
                            </div>
                            <div class="code-form-item codelib-form-item">
                                <div class="info-label"> {{ $t('代码库：') }} </div>
                                <div class="info-value codelib-url">
                                    <span class="codelib-text">{{ codeForm.codeSrc }}</span>
                                    <span class="link-text copy-btn" :data-clipboard-text="codeForm.codeSrc" @click="copyUrl"> {{ $t('复制') }} </span>
                                </div>
                            </div>
                            <div class="code-form-item codelib-form-item">
                                <div class="info-label"> {{ $t('授权人：') }} </div>
                                <div class="info-value codelib-url">
                                    {{codeForm.repositoryAuthorizer}}
                                    <bk-popover placement="top">
                                        <i class="bk-icon icon-info-circle"></i>
                                        <template slot="content">
                                            <p> {{ $t('在发布插件时，使用授权人的身份拉取插件代码自动构建打包，或设置插件可见范围') }} </p>
                                        </template>
                                    </bk-popover>
                                    <span class="link-text repo-outh"
                                        @click="modifyRepoMemInfo"
                                        :title="$t('将使用你的身份进行插件代码库相关操作')"
                                        v-if="userInfo.isProjectAdmin && userInfo.userName !== codeForm.repositoryAuthorizer"
                                    > {{ $t('重置授权') }} </span>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="atom-latest-news">
                        <div class="info-title"> {{ $t('最新动态') }} </div>
                        <div class="news-content">
                            <img :src="image">
                            <p>{{ $t('功能正在建设中') }}···</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import Clipboard from 'clipboard'
    import imgBuilding from '@/images/building.png'

    export default {
        data () {
            return {
                showContent: false,
                factoryUrl: 'https://git.code.oa.com/',
                image: imgBuilding,
                statisticList: [
                    { name: 'downloads', label: this.$t('安装量'), value: 0 },
                    { name: 'pipelineCnt', label: this.$t('流水线个数'), value: 0 },
                    { name: 'commentCnt', label: this.$t('评论数'), value: 0 },
                    { name: 'score', label: this.$t('评分'), value: 0 }
                ],
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            ...mapGetters('store', {
                'codeForm': 'getCurrentAtom',
                'userInfo': 'getUserInfo'
            }),
            atomId () {
                return this.$route.params.atomId
            },
            atomCode () {
                return this.$route.params.atomCode
            }
        },
        created () {
            this.initData()
        },

        methods: {
            initData () {
                this.loading.isLoading = true

                this.requestAtomStatistic().catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => {
                    this.loading.isLoading = false
                    this.showContent = true
                })
            },

            modifyRepoMemInfo () {
                const atomCode = this.atomCode
                const projectCode = this.codeForm.projectCode
                this.$store.dispatch('store/checkIsOAuth', { type: 'ATOM_REPOSITORY', atomCode }).then((res) => {
                    if (res.status === 403) {
                        window.open(res.url, '_self')
                        return
                    }

                    return this.$store.dispatch('store/modifyRepoMemInfo', { atomCode, projectCode }).then((res) => {
                        if (res) {
                            this.$set(this.codeForm, 'repositoryAuthorizer', this.userInfo.userName)
                            this.$bkMessage({ message: '重置授权成功', theme: 'success', limit: 1 })
                        }
                    })
                }).catch(err => this.$bkMessage({ message: err.message || err, theme: 'error' }))
            },

            requestAtomStatistic () {
                return this.$store.dispatch('store/requestAtomStatistic', {
                    atomCode: this.atomCode
                }).then((res) => {
                    this.statisticList.forEach(item => {
                        item.value = res[item.name]
                    })
                })
            },

            copyUrl () {
                this.clipboardInstance = new Clipboard('.copy-btn')
                this.clipboardInstance.on('success', e => {
                    this.$bkMessage({
                        theme: 'success',
                        message: '复制成功',
                        limit: 1
                    })
                })
            },

            toLink () {
                window.open(this.factoryUrl, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import './../../assets/scss/conf';
    .atom-overview-wrapper {
        overflow: auto;
        .inner-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                font-size: 16px;
            }
        }
        .atom-overview-container {
            display: flex;
            height: 100%;
            padding: 20px;
            overflow: auto;
        }
        .info-content-left {
            flex: 3;
            margin-right: 20px;
            .info-title {
                font-weight: bold;
            }
        }
        .info-content-right {
            flex:1;
            min-width: 328px;
        }
        .atom-statistic {
            display: flex;
            justify-content: space-around;
            align-items: center;
            width: 100%;
            height: 129px;
            margin-top: 8px;
            border: 1px solid $borderWeightColor;
            background: #fff;
            .total-item {
                width: 80px;
                text-align: -webkit-center;
            }
            .item-value {
                width: 50px;
                height: 50px;
                padding-top: 15px;
                border: 2px solid #7ED321;
                border-radius: 50%;
                font-size: 15px;
                font-weight: bold;
                color: #7ED321;
            }
            .item-label {
                margin-top: 14px;
                font-size: 12px;
                font-weight: bold;
            }
            .blue-theme {
                border-color: #3C96FF;
                color: #3C96FF;
            }
        }
        .atom-code-card {
            width: 100%;
            min-width: 328px;
            height: 173px;
            .card-title {
                font-weight: bold;
            }
            .card-content {
                height: 129px;
                margin-top: 8px;
                padding: 10px 20px;
                border: 1px solid $borderWeightColor;
                background: #fff;
                display: flex;
                flex-direction: column;
                justify-content: space-around;
                font-size: 12px;
            }
            .code-form-item {
                position: relative;
                display: flex;
                width: 100%;
                &:first-child {
                    margin-top: 0;
                }
            }
            .info-label {
                width: 70px;
                min-width: 70px;
                text-align: right;
            }
            .info-value {
                margin-left: 16px;
                color: #333C48;
            }
            .link-text {
                color: $primaryColor;
                cursor: pointer;
            }
            .codelib-url {
                position: relative;
                width: calc(100% - 84px);
                display: flex;
                align-items: center;
                .bk-icon {
                    vertical-align: middle;
                    margin-left: 4px;
                }
            }
            .codelib-text {
                display: inline-block;
                white-space: nowrap;
                flex: 1;
                overflow: hidden;
                color: $fontWeightColor;
                font-size: 12px;
                text-overflow: ellipsis;
            }
            .copy-btn {
                margin-left: 4px;
            }
            .repo-outh {
                margin-left: 14px;
            }
        }
        .trend-chart {
            margin-top: 24px;
            .info-title {
                font-weight: bold;
            }
        }
        .chart-content {
            width: 100%;
            height: 360px;
            margin-top: 10px;
            border: 1px solid $borderWeightColor;
            background-color: #fff;
            .info-tab {
                height: 44px;
                border-bottom: 1px solid #DDE4EB;
                background-color: #FAFBFD;
            }
            .tab-item {
                display: inline-block;
                height: 44px;
                line-height: 44px;
                padding: 0 12px;
                border-right: 1px solid $borderWeightColor;
                color: $primaryColor;
                font-weight: bold;
                background-color: #fff;
            }
            .chart-item {
                padding: 20px;

                // TODO
                text-align: center;
                img {
                    margin-top: 50px;
                    margin-bottom: 20px;
                    width: 280px;
                    height: 120px;
                }

            }
        }
        .atom-latest-news {
            margin-top: 7px;
            .info-title {
                font-weight: bold;
            }
            .news-content {
                margin-top: 10px;
                width: 100%;
                height: 361px;
                border: 1px solid $borderWeightColor;
                background-color: #fff;

                // TODO
                text-align: center;
                img {
                    margin-top: 100px;
                    margin-bottom: 20px;
                    width: 200px;
                    height: 90px;
                }
            }
        }
    }
</style>
