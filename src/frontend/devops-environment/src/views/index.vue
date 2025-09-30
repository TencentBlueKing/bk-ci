<template>
    <div
        class="environment-container"
        ref="environmentContainer"
    >
        <div class="biz-header">
            <p class="environment-tit">
                <img
                    :src="environmentUrl"
                    :width="24"
                    :height="24"
                />
                <span>{{ $t('environment.environmentManage') }}</span>
            </p>
            <bk-tab
                :active.sync="activePanel"
                type="unborder-card"
                @tab-change="handleChangeTab"
                ext-cls="tabs-manage"
                :label-height="48"
            >
                <bk-tab-panel
                    v-for="panel in panels"
                    v-bind="panel"
                    :key="panel.name"
                >
                </bk-tab-panel>
            </bk-tab>
            <span class="monitoring">
                <span
                    v-if="isEnableDashboard && activePanel === 'nodeList'"
                    class="enable-monitoring ml5"
                >
                    <i class="devops-icon icon-tiaozhuan jump-icon"></i>
                    <a
                        :href="jumpDashboardUrl"
                        target="_blank"
                    >{{ $t('environment.查看构建机监控') }}</a>
                </span>
                <!-- <span
                    class="enable-monitoring"
                >{{ $t('environment.enableBuildAgentMonitoring') }}</span>
                <p
                    class="enable-monitoring"
                >
                    <span>{{ $t('environment.buildAgentMonitoring') }}</span>
                    <span class="enabled">{{ $t('environment.enabled') }}</span>
                </p> -->
            </span>
        </div>

        <router-view :container-width="containerWidth"></router-view>
    </div>
</template>

<script>
    import environmentUrl from '@/scss/logo/environment.svg'
    import { mapActions, mapGetters, mapState } from 'vuex'

    export default {
        data () {
            return {
                environmentUrl,
                isEnableDashboard: false,
                bizId: 0,
                containerWidth: 0,
            }
        },

        computed: {
            ...mapState('environment', [
                'extensions'
            ]),
            ...mapGetters('environment', {
                hookIds: 'asideNavBarExtIds'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            extNav () {
                return this.extensions.map((ext) => ({
                    name: 'extPage',
                    label: ext.serviceName,
                    icon: 'devops-icon icon-placeholder',
                    params: {
                        itemId: ext.itemId,
                        serviceCode: ext.serviceCode
                    }
                }))
            },
            activePanel () {
                if (this.$route.name === 'extPage') {
                    return 'extPage'
                } else {
                    const routeMap = {
                        nodeList: 1,
                        nodeDetail: 1,
                        setNodeTag: 1,
                    }
                    return routeMap[this.$route.name] === 1 ? 'nodeList' : 'envList'
                }
            },
            panels () {
                return [
                    {
                        name: 'envList',
                        label: this.$t('environment.environment'),
                        icon: 'devops-icon icon-env'
                    },
                    {
                        name: 'nodeList',
                        label: this.$t('environment.node'),
                        icon: 'devops-icon icon-node'
                    },
                    ...this.extNav
                ]
            },
            projectCode () {
                return this.$route.params.projectId
            },
            jumpDashboardUrl () {
                return `https://bkm.woa.com/?bizId=${this.bizId}#/grafana/d/bT8qy3NVa`
            }
        },
        watch: {
            hookIds: {
                handler: function (hookIds) {
                    hookIds && this.getEnvironmentExtensions({
                        projectCode: this.projectCode,
                        hookIds: hookIds
                    })
                },
                immediate: true
            },
            projectCode: function (projectCode) {
                this.getEnvironmentExtensions({
                    projectCode,
                    hookIds: this.hookIds
                })
            }
        },
        created () {
            if (!this.$route.name) {
                this.$router.push({
                    name: 'envList'
                })
            }
        },
        async mounted () {
            this.updateContainerWidth()
            window.addEventListener('resize', this.updateContainerWidth)
            await this.getEnableDashboard()
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.updateContainerWidth)
        },
        methods: {
            ...mapActions('environment', [
                'getEnvironmentExtensions'
            ]),
            updateContainerWidth () {
                if (this.$refs.environmentContainer) {
                    this.containerWidth = this.$refs.environmentContainer.clientWidth
                }
            },
            async getEnableDashboard () {
                try {
                    const res = await this.$store.dispatch('environment/checkEnableDashboard', {
                        projectId: this.projectId
                    })
                    if (res) {
                        this.isEnableDashboard = res.result
                        this.bizId = res.bizId
                    }
                } catch (e) {
                    console.err(e)
                }
            },
            handleChangeTab (name) {
                if (this.activePanel === name) return
                const item = this.panels.find(navItem => navItem.name === name)

                const routeMap = {
                    envList: 'envList',
                    nodeList: 'nodeList',
                    extPage: 'extPage'
                }
                this.$router.push({
                    name: routeMap[name],
                    params: item.params
                })
            }
        }
    }
</script>

<style lang="scss">
.tabs-manage {
    .bk-tab-header {
        background-image: none !important;
    }
}
</style>

<style lang="scss" scoped>
.environment-container {
    width: 100%;
    box-sizing: border-box;
    min-height: calc(100% - 210px);
    overflow: hidden;
    .biz-header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        height: 48px;
        box-sizing: border-box;
        line-height: 48px;
        padding: 0 24px;
        background-color: #FFFFFF;
        border-bottom: 1px solid rgb(220, 222, 229);
    }
    .environment-tit {
        display: flex;
        align-items: center;
        img {
            vertical-align: middle;
        }
        span {
            margin-left: 4px;
        }
    }

    .tabs-manage {
        height: 48px;
    }
    .monitoring {
        width: 200px;
        text-align: right;
        .enable-monitoring, a {
            margin-left: auto;
            font-size: 12px;
            color: #3A84FF;
            cursor: pointer;
        }
        .enabled {
            display: inline-block;
            margin-left: 8px;
            width: 56px;
            height: 22px;
            line-height: 22px;
            background: #EDF4FF;
            text-align: center;
            border-radius: 2px;
        }

        .jump-icon {
            font-size: 18px;
            position: relative;
            top: 2px;
        }
    }
}
</style>
