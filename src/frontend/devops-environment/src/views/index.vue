<template>
    <div class="biz-container">
        <p class="environment-tit">
            <img
                :src="environmentUrl"
                :width="24"
                :height="24"
            />
            <span>{{ $t('environment.environmentManage') }}</span>
        </p>
        <bk-tab
            :active="activePanel"
            :label-height="60"
            type="unborder-card"
            :active-bar="activeBar"
            ext-cls="env-tab"
            @tab-change="handleTabChange"
            :validate-active="false"
        >
            <bk-tab-panel
                v-for="panel in panels"
                render-directive="if"
                :label="panel.label"
                :name="panel.name"
                :key="panel.name"
            >
                <template slot="label">
                    <i
                        :class="panel.icon"
                        class="panel-icon"
                    ></i>
                    <span class="panel-name">{{ panel.label }}</span>
                </template>
                <router-view :style="{ padding: activePanel !== 'nodeList' ? '20px' : '' }"></router-view>
            </bk-tab-panel>
    
            <template slot="setting">
                <span class="enable-monitoring">{{ $t('environment.开启构建机监控') }}</span>
                <!-- <p v-else class="enable-monitoring">
                    <span>{{ $t('environment.构建机监控') }}</span>
                    <span class="enabled">{{ $t('environment.已开启') }}</span>
                </p> -->
            </template>
        </bk-tab>
    </div>
</template>

<script>
    import environmentUrl from '@/scss/logo/environment.svg'
    import { mapState, mapGetters, mapActions } from 'vuex'

    export default {
        data () {
            return {
                environmentUrl,
                activeBar: {
                    position: 'top',
                    height: '4px'
                }
            }
        },

        computed: {
            ...mapState('environment', [
                'extensions'
            ]),
            ...mapGetters('environment', {
                hookIds: 'asideNavBarExtIds'
            }),
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
                const routeMap = {
                    envList: 'envList',
                    nodeList: 'nodeList',
                    createEnv: 'envList',
                    envDetail: 'envList',
                    nodeDetail: 'nodeList',
                    extPage: 'extPage'
                }
                
                return routeMap[this.$route.name] || 'envList'
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
        methods: {
            ...mapActions('environment', [
                'getEnvironmentExtensions'
            ]),
            handleTabChange (name) {
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
    .env-tab {
        .bk-tab-label-wrapper {
            text-align: center;
        }
        .bk-tab-section {
            padding: 0;
        }
        .bk-tab-header {
            background-color: #FFFFFF;
            box-shadow: 0 2px 5px 0 #333c4808;
        }
    }
</style>

<style lang="scss" scoped>
    .biz-container {
        position: relative;
        width: 100%;
        box-sizing: border-box;
        overflow: hidden;

        .environment-tit {
            position: absolute;
            left: 24px;
            line-height: 60px;
            z-index: 1;
            img {
                vertical-align: middle;
            }
        }
        .env-tab {
            .panel-icon{
                vertical-align: middle;
            }
            .enable-monitoring {
                margin-right: 24px;
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
        }
    }
</style>
