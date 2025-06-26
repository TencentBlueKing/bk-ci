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
            :label-height="48"
            type="unborder-card"
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
                <router-view></router-view>
            </bk-tab-panel>
    
            <template slot="setting">
                <span class="enable-monitoring">{{ $t('开启构建机监控') }}</span>
            </template>
        </bk-tab>
    </div>
</template>

<script>
    import environmentUrl from '@/scss/logo/environment.svg'

    export default {
        data () {
            return {
                environmentUrl
            }
        },

        computed: {
            activePanel () {
                const routeMap = {
                    envList: 'envList',
                    nodeList: 'nodeList',
                    createEnv: 'envList',
                    envDetail: 'envList',
                    nodeDetail: 'nodeList'
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
                    }
                ]
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
            handleTabChange (name) {
                if (this.activePanel === name) return

                const routeMap = {
                    envList: { name: 'envList' },
                    nodeList: { name: 'nodeList' }
                }
                
                this.$router.push(routeMap[name])
            }
        }
    }
</script>

<style lang="scss">
.env-tab {
    .bk-tab-label-wrapper {
        text-align: center;
    }
}
</style>

<style lang="scss" scoped>
.biz-container {
    position: relative;
    width: 100%;

    .environment-tit {
        position: absolute;
        left: 24px;
        line-height: 48px;
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
    }
}
</style>
