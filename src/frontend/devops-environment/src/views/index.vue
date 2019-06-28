<template>
    <div class="biz-container">
        <div class="biz-side-bar">
            <side-bar
                :nav="sideMenuNav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'credentialCertificate'">
            </side-bar>
        </div>
        <router-view style="width: 100%"></router-view>
    </div>
</template>

<script>
    import sideBar from '@/components/devops/side-nav'

    export default {
        components: {
            'side-bar': sideBar
        },
        data () {
            return {
                sideMenuNav: {
                    icon: 'environment',
                    title: '环境管理',
                    url: `${DOCS_URL_PREFIX}/所有服务/环境管理/summary.html`
                },
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'envList',
                                name: '环境',
                                icon: 'icon-env',
                                showChildren: false,
                                children: [
                                    {
                                        id: 'createEnv',
                                        name: '新增环境',
                                        icon: 'icon-env'
                                    },
                                    {
                                        id: 'envDetail',
                                        name: '环境详情',
                                        icon: 'icon-env'
                                    }
                                ]
                            },
                            {
                                id: 'nodeList',
                                name: '节点',
                                icon: 'icon-node',
                                showChildren: false,
                                children: [
                                    {
                                        id: 'nodeDetail',
                                        name: '节点详情',
                                        icon: 'icon-node'
                                    }
                                ]
                            }
                        ]
                    }
                ]
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
        overflow: auto;
    }
</style>
