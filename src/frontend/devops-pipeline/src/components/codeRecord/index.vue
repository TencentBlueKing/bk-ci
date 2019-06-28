<template>
    <div class="code-record-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <!-- 代码变更记录 -->
        <vertical-tab v-if="codeCommitList.length && showContent" :tabs="tabs" :init-tab-index="initIndex">
        </vertical-tab>
            
        <div class="artifactory-empty" v-else-if="showContent && !codeCommitList.length">
            <div class="no-data-right">
                <img src="../../images/box.png">
                <p>暂时没有变更记录</p>
            </div>
        </div>
    </div>
</template>

<script>
    import VerticalTab from '../PipelineEditTabs/VerticalTab'
    export default {
        components: {
            VerticalTab
        },
        data () {
            return {
                initIndex: 0,
                showContent: false,
                codeCommitList: [],
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            buildNo () {
                return this.$route.params.buildNo
            },
            tabs () {
                return this.codeCommitList.map(item => ({
                    id: item.elementId,
                    name: item.name,
                    component: 'CodeRecordTable',
                    componentProps: {
                        commitList: item.records,
                        label: item.name
                    }
                }))
            }
        },
        watch: {
            buildNo () {
                this.init()
            }
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                const {
                    loading,
                    buildNo
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    const res = await this.$store.dispatch('soda/requestCommitList', {
                        buildId: buildNo
                    })

                    if (res.length) {
                        this.codeCommitList = [
                            ...res
                        ]
                        const initCurNavTab = this.$route.hash.slice(1)
                        const tabIndex = this.codeCommitList.findIndex(item => item.name === initCurNavTab)
                        this.initIndex = tabIndex !== -1 ? tabIndex : 0
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.showContent = true
                        this.loading.isLoading = false
                    }, 500)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    %flex {
        display: flex;
        align-items: center;
    }
    .code-record-wrapper {
        height: 100%;
        .code-factory-tab {
            border: none;
            height: 100%;
            background-color: $bgHoverColor;
            .prompt-tips {
                padding: 10px 0;
                color: $fontWeightColor;
                font-size: 14px;
            }
        }
        .artifactory-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                padding-top: 226px;
                p {
                    line-height: 60px;
                }
            }
        }
        .code-records-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                padding-top: 200px;
                p {
                    line-height: 60px;
                }
            }
        }
    }
</style>
