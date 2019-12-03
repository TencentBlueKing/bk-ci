
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
                    url: `${DOCS_URL_PREFIX}/x/BIzm`,
                    menu: [
                        {
                            id: 'envList',
                            name: this.$t('environment.environment'),
                            icon: 'icon-env',
                            showChildren: false,
                            children: [
                                {
                                    id: 'createEnv',
                                    name: this.$t('environment.createEnvrionment'),
                                    icon: 'icon-env'
                                },
                                {
                                    id: 'envDetail',
                                    name: this.$t('environment.environmentDetail'),
                                    icon: 'icon-env'
                                }
                            ]
                        },
                        {
                            id: 'nodeList',
                            name: this.$t('environment.node'),
                            icon: 'icon-node',
                            showChildren: false,
                            children: [
                                {
                                    id: 'createNode',
                                    name: this.$t('environment.createNode'),
                                    icon: 'icon-node'
                                },
                                {
                                    id: 'nodeDetail',
                                    name: this.$t('environment.nodeDetail'),
                                    icon: 'icon-node'
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
