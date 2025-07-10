<template>
    <div class="biz-container">
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
                :label-height="60"
            >
                <bk-tab-panel
                    
                    v-for="panel in panels"
                    v-bind="panel"
                    :key="panel.name"
                >
                </bk-tab-panel>
            </bk-tab>
        </div>

        <router-view class="manage-main"></router-view>
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
            handleChangeTab (name) {
                if (this.activePanel === name) return

                const routeMap = {
                    envList: 'envList',
                    nodeList: 'nodeList'
                }
                
                this.$router.push({
                    name: routeMap[name]
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
.biz-container {
    width: 100%;
    box-sizing: border-box;
    overflow: hidden;

    .biz-header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 60px;
        line-height: 60px;
        padding: 0 24px;
        background-color: #FFFFFF;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid #eeeff3;
    }

    .environment-tit {
        position: absolute;
        left: 24px;
        img {
            vertical-align: middle;
        }
    }

    .tabs-manage {
        height: 60px;
    }
}
</style>
