<template>
    <div class="biz-container credential-certificate-wrapper">
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
                    icon: 'ticket',
                    title: '凭证管理',
                    url: `${DOCS_URL_PREFIX}/所有服务/凭证管理/summary.html`,
                    menu: [
                        {
                            id: 'credentialList',
                            name: '我的凭据',
                            icon: 'icon-id'
                        },
                        {
                            id: 'createCredential',
                            name: '新增凭据',
                            icon: 'icon-id',
                            showChildren: false,
                            group: true,
                            children: [
                                {
                                    id: 'editCredential',
                                    name: '编辑凭据',
                                    icon: 'icon-id'
                                },
                                {
                                    id: 'createCredentialWithType',
                                    name: '新增凭据',
                                    icon: 'icon-id'
                                }
                            ]
                        }
                        // {
                        //     id: 'certList',
                        //     name: '我的证书',
                        //     icon: 'icon-personal-cert'
                        // },
                        // {
                        //     id: 'createCert',
                        //     name: '新增证书',
                        //     icon: 'icon-personal-cert',
                        //     showChildren: false,
                        //     children: [
                        //         {
                        //             id: 'editCert',
                        //             name: '编辑证书',
                        //             icon: 'icon-personal-cert'
                        //         }
                        //     ]
                        // }
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
    .credential-certificate-content {
        height: 100%;
        overflow: hidden;
        .normal-tips {
            font-size: 12px;
            padding-top: 5px;
        }
    }
    .sub-view-port {
        height: calc(100% - 60px);
        padding: 20px;
        overflow: auto;
        
    }
</style>
