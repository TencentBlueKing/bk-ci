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
            <span class="manage-tabs">
                <span
                    v-for="panel in panels"
                    :key="panel.name"
                    :class="{
                        'manage-tab': true,
                        active: activePanel === panel.name
                    }"
                    @click="handleChangeTab(panel)"
                >
                    <i
                        :class="panel.icon"
                        class="panel-icon"
                    ></i>
                    <span class="panel-name">{{ panel.label }}</span>
                </span>
            </span>
            <span class="monitoring">
                <span
                    v-if="false"
                    class="enable-monitoring"
                >{{ $t('environment.enableBuildAgentMonitoring') }}</span>
                <!-- <p
                    v-else
                    class="enable-monitoring"
                >
                    <span>{{ $t('environment.buildAgentMonitoring') }}</span>
                    <span class="enabled">{{ $t('environment.enabled') }}</span>
                </p> -->
            </span>
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
            handleChangeTab (panel) {
                if (this.activePanel === panel.name) return

                const routeMap = {
                    envList: { name: 'envList' },
                    nodeList: { name: 'nodeList' }
                }
                
                this.$router.push(routeMap[panel.name])
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
        justify-content: space-between;
        width: 100%;
        height: 60px;
        line-height: 60px;
        padding: 0 24px;
        background-color: #FFFFFF;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid #eeeff3;
    }

    .environment-tit {
        img {
            vertical-align: middle;
        }
    }
    
    .manage-tabs {
        display: flex;
        align-items: center;
        
        .manage-tab {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 100%;
            padding: 0 24px;
            cursor: pointer;
            position: relative;
            
            .panel-icon {
                margin-right: 8px;
            }
            
            &.active {
                color: #3a84ff;
                background: rgba(225, 236, 255, 0.5);
                
                &::after {
                    content: '';
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    height: 4px;
                    background-color: #3a84ff;
                }
            }
        }
    }

    .monitoring {
        .enable-monitoring {
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
    }
}
</style>
