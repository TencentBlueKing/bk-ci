
<template>
    <div class="biz-container">
        <aside-nav :nav="nav" :menu-click="menuClick">
            <router-view slot="content" style="width: 100%"></router-view>
        </aside-nav>
    </div>
</template>

<script>
    export default {
        computed: {
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
                                    icon: 'devops-icon icon-env',
                                    aaaa: 'envList'
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
                                    id: 'createNode',
                                    name: this.$t('environment.createNode'),
                                    icon: 'devops-icon icon-node'
                                },
                                {
                                    id: 'nodeDetail',
                                    name: this.$t('environment.nodeDetail'),
                                    icon: 'devops-icon icon-node'
                                }
                            ]
                        }
                    ]
                }
            }
        },
        methods: {
            menuClick (name) {
                this.$router.push({
                    name
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
