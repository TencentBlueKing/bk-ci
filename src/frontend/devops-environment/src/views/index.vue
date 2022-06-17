
<template>
    <div class="biz-container">
        <aside-nav :nav="nav" :menu-click="menuClick">
            <router-view slot="content" style="width: 100%"></router-view>
        </aside-nav>
    </div>
</template>

<script>
    import { mapState, mapGetters, mapActions } from 'vuex'
    export default {
        computed: {
            ...mapState('environment', [
                'extensions'
            ]),
            ...mapGetters('environment', {
                hookIds: 'asideNavBarExtIds'
            }),
            extNav () {
                return this.extensions.map((ext) => ({
                    id: 'extPage',
                    name: ext.serviceName,
                    icon: 'devops-icon icon-placeholder',
                    params: {
                        itemId: ext.itemId,
                        serviceCode: ext.serviceCode
                    }
                }))
            },
            nav () {
                return {
                    icon: 'environment',
                    title: this.$t('environment.environmentManage'),
                    menu: [
                        {
                            id: 'envList',
                            name: this.$t('environment.environment'),
                            icon: 'devops-icon icon-env',
                            showChildren: false,
                            children: [
                                {
                                    id: 'createEnv',
                                    name: this.$t('environment.createEnvrionment'),
                                    icon: 'devops-icon icon-env'
                                },
                                {
                                    id: 'envDetail',
                                    name: this.$t('environment.environmentDetail'),
                                    icon: 'devops-icon icon-env'
                                }
                            ]
                        },
                        {
                            id: 'nodeList',
                            name: this.$t('environment.node'),
                            icon: 'devops-icon icon-node',
                            showChildren: false,
                            children: [
                                {
                                    id: 'nodeDetail',
                                    name: this.$t('environment.nodeDetail'),
                                    icon: 'devops-icon icon-node'
                                }
                            ]
                        },
                        ...this.extNav
                    ]
                }
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
        methods: {
            ...mapActions('environment', [
                'getEnvironmentExtensions'
            ]),
            menuClick (name) {
                const item = this.nav.menu.find(navItem => navItem.id === name)
                this.$router.push({
                    name,
                    params: item.params
                })
            }
        }
    }
</script>

<style lang="scss">
    .credential-certificate-wrapper {
        height: 100%;
        .bk-table {
            th:first-child,
            td:first-child {
                padding-left: 20px;
            }
        }
    }
    .sub-view-port {
        height: calc(100% - 60px);
        padding: 20px;
        overflow: auto;
    }
</style>
