<template>
    <div class="environment-container">
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
    min-height: calc(100vh - 50px);
    overflow: hidden;

    .biz-header {
        position: relative;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 48px;
        box-sizing: border-box;
        line-height: 48px;
        padding: 0 24px;
        background-color: #FFFFFF;
        border-bottom: 1px solid rgb(220, 222, 229);
    }

    .environment-tit {
        position: absolute;
        left: 24px;
        img {
            vertical-align: middle;
        }
    }

    .tabs-manage {
        height: 48px;
    }
}
</style>
