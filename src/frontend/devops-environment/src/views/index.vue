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
        </div>
        <router-view :container-width="containerWidth"></router-view>
    </div>
</template>

<script>
    import environmentUrl from '@/scss/logo/environment.svg'
    import { SERVICE_RESOURCE_TYPE } from '@/store/constants'
    const RES_TYPE_STORAGE_KEY = 'bk_devops_environment_res_type'
    
    export default {
        data () {
            return {
                environmentUrl,
                containerWidth: 0,
                currentResType: SERVICE_RESOURCE_TYPE.PIPELINE
            }
        },

        computed: {
            activePanel () {
                const routeMap = {
                    nodeList: 1,
                    nodeDetail: 1,
                    setNodeTag: 1,
                }
                return routeMap[this.$route.name] === 1 ? 'nodeList' : 'envList'
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
            // 初始化资源类型
            this.initResType()
        },
        mounted () {
            this.updateContainerWidth()
            window.addEventListener('resize', this.updateContainerWidth)
        },
        beforeDestroy () {
            window.removeEventListener('resize', this.updateContainerWidth)
        },
        methods: {
            updateContainerWidth () {
                if (this.$refs.environmentContainer) {
                    this.containerWidth = this.$refs.environmentContainer.clientWidth
                }
            },
            /**
             * 初始化资源类型
             * 优先级：URL参数 > 根据最近访问服务判断 > localStorage缓存 > 默认值
             */
            initResType () {
                // 1. 从URL参数获取
                const urlResType = this.$route.params.resType
                if (urlResType && [SERVICE_RESOURCE_TYPE.PIPELINE, SERVICE_RESOURCE_TYPE.FLOW].includes(urlResType)) {
                    this.currentResType = urlResType
                    return
                }

                // 2. 根据用户最近访问的服务判断
                const recentVisitService = localStorage.getItem('recentVisitService')
                if (recentVisitService) {
                    try {
                        const recentVisitServiceList = JSON.parse(recentVisitService)
                        console.log(recentVisitServiceList)
                        // 获取最近访问的服务（按访问时间排序，第二个是上一次访问的服务）
                        if (recentVisitServiceList.length > 1) {
                            const resType = recentVisitServiceList[1]?.key
                            console.log(resType, 'resTyperesType')
                            if ([SERVICE_RESOURCE_TYPE.PIPELINE, SERVICE_RESOURCE_TYPE.FLOW].includes(resType)) {
                                this.handleResTypeChange(resType)
                                this.currentResType = resType
                                return
                            }
                        }
                    } catch (e) {
                        console.warn('解析recentVisitService失败', e)
                    }
                }

                // 3. 从localStorage缓存获取
                const cachedResType = localStorage.getItem(RES_TYPE_STORAGE_KEY)
                if (cachedResType && [SERVICE_RESOURCE_TYPE.PIPELINE, SERVICE_RESOURCE_TYPE.FLOW].includes(cachedResType)) {
                    this.handleResTypeChange(cachedResType)
                    this.currentResType = cachedResType
                    return
                }

                // 4. 如果上述条件都不成立，默认选中pipeline
                this.handleResTypeChange(SERVICE_RESOURCE_TYPE.PIPELINE)
                this.currentResType = SERVICE_RESOURCE_TYPE.PIPELINE
            },

            /**
             * 处理资源类型切换
             */
            handleResTypeChange (resType) {
                // 保存选择到localStorage
                localStorage.setItem(RES_TYPE_STORAGE_KEY, resType)
                
                // 更新路由参数
                this.$router.push({
                    name: this.$route.name || 'envList',
                    params: {
                        ...this.$route.params,
                        resType: resType
                    }
                })
            },
            handleChangeTab (name) {
                if (this.activePanel === name) return

                const routeMap = {
                    envList: {
                        name: 'envDetail',
                        params: {
                            ...this.$route.params,
                            resType: this.currentResType,
                            envType: 'ALL', // 默认环境类型
                            envId: undefined, // 由 useEnvAside 设置默认的 envId
                            tabName: undefined // 由 env_detail 设置默认的 tabName
                        }
                    },
                    nodeList: {
                        name: 'nodeList',
                        params: {
                            ...this.$route.params,
                            resType: this.currentResType,
                            nodeType: 'allNode' // 节点页面的默认类型
                        }
                    }
                }
                
                this.$router.push({
                    name: routeMap[name].name,
                    params: routeMap[name].params
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
        display: flex;
        align-items: center;
        position: absolute;
        left: 24px;
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
}
</style>
