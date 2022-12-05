<template>
    <article class="build-detail-home">
        <section class="section-box build-detail-header">
            <bk-breadcrumb class="build-detail-crumb" separator-class="bk-icon icon-angle-right">
                <bk-breadcrumb-item v-if="!menuPipelineId" :to="{ name: 'buildList', params: {}, query: $route.query }">{{$t('pipeline.allPipelines')}}</bk-breadcrumb-item>
                <bk-breadcrumb-item :to="{ name: 'buildList', params: { pipelineId }, query: $route.query }" @click.native="handleClickRoute">{{yml}}</bk-breadcrumb-item>
                <bk-breadcrumb-item>
                    <span class="build-num">
                        # {{buildNum}}
                        <span class="toggle-build-icon">
                            <icon name="angle-up-line" size="10" :class="{ click: latestBuildNum > buildNum }" @click.native="toggleBuild(1)"></icon>
                            <icon name="angle-down-line" size="10" :class="{ click: buildNum > 1 }" @click.native="toggleBuild(-1)"></icon>
                        </span>
                    </span>
                </bk-breadcrumb-item>
            </bk-breadcrumb>

            <bk-tab :active.sync="active" type="unborder-card" class="header-tab" @tab-change="changeTab">
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    :key="index">
                </bk-tab-panel>
            </bk-tab>
        </section>

        <router-view class="section-box build-detail-main"></router-view>
    </article>
</template>

<script>
    import { mapState } from 'vuex'
    import { modifyHtmlTitle } from '@/utils'
    import { pipelines } from '@/http'

    export default {
        data () {
            return {
                panels: [
                    { label: this.$t('pipeline.buildDetail'), name: 'buildDetail' },
                    { label: this.$t('pipeline.artifacts'), name: 'buildArtifacts' },
                    { label: this.$t('pipeline.buildReport'), name: 'buildReports' },
                    { label: this.$t('pipeline.buildConfig'), name: 'buildConfig' }
                ],
                active: 'buildDetail',
                buildNum: '',
                latestBuildNum: '',
                yml: ''
            }
        },

        computed: {
            ...mapState(['projectId', 'curPipeline', 'menuPipelineId']),
            pipelineId () {
                return this.$route.params.pipelineId
            }
        },

        watch: {
            '$route.name': {
                handler (name) {
                    this.active = name
                },
                immediate: true
            }
        },

        created () {
            this.initData()
        },

        methods: {
            changeTab (name) {
                this.$router.push({ name })
            },

            initData () {
                const params = {
                    pipelineId: this.$route.params.pipelineId,
                    buildId: this.$route.params.buildId
                }
                return pipelines.getPipelineBuildDetail(this.projectId, params).then((res = {}) => {
                    const { gitRequestEvent = {}, modelDetail = {}, gitProjectPipeline = {} } = res || {}
                    this.buildNum = modelDetail.buildNum
                    this.latestBuildNum = modelDetail.latestBuildNum
                    this.yml = gitProjectPipeline.displayName
                    this.setHtmlTitle(gitRequestEvent.buildTitle)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            toggleBuild (num) {
                const buildNum = this.buildNum + num
                if (buildNum <= 0 || buildNum > this.latestBuildNum) return

                const pipelineId = this.$route.params.pipelineId
                pipelines.getBuildInfoByBuildNum(this.projectId, pipelineId, buildNum).then((res) => {
                    const buildId = res.id
                    this.$router.push({
                        name: this.$route.name,
                        params: { buildId, pipelineId }
                    })
                    this.initData()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            setHtmlTitle (message) {
                const title = message + ' #' + this.buildNum
                modifyHtmlTitle(title)
            },

            handleClickRoute () {
                if (this.menuPipelineId) {
                    this.$nextTick(() => {
                        this.$parent.getPipelineDirList()
                    })
                }
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .build-detail-home {
        padding-left: 20px;
    }
    .build-detail-header {
        background: #fff;
    }
    .build-detail-crumb {
        height: 50px;
        line-height: 50px;
        padding: 0 27px;
        .build-num {
            display: flex;
            align-items: center;
        }
        .toggle-build-icon {
            display: flex;
            flex-direction: column;
            margin-left: 10px;
            font-size: 12px;
            color: #c3cdd7;
            .click {
                color: #3a84ff;
                cursor: pointer;
            }
        }
    }
    .header-tab {
        margin-top: -10px;
        /deep/ .bk-tab-header {
            padding: 0 14px;
            background-image: none !important;
        }
        /deep/ .bk-tab-section {
            padding: 0;
        }
    }
    .build-detail-main {
        margin-top: 16px;
        height: calc(100% - 106px);
        background: #fff;
    }
</style>
