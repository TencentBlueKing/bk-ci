<template>
    <div class="biz-container credential-certificate-wrapper">
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
                    icon: 'ticket',
                    title: '凭证管理',
                    url: `${DOCS_URL_PREFIX}/所有服务/凭证管理/summary.html`
                },
                sideMenuList: [
                    {
                        title: '凭据',
                        list: [
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
                        ]
                    }
                    // {
                    //     title: '证书',
                    //     list: [
                    //         {
                    //             id: 'certList',
                    //             name: '我的证书',
                    //             icon: 'icon-personal-cert'
                    //         },
                    //         {
                    //             id: 'createCert',
                    //             name: '新增证书',
                    //             icon: 'icon-personal-cert',
                    //             showChildren: false,
                    //             children: [
                    //                 {
                    //                     id: 'editCert',
                    //                     name: '编辑证书',
                    //                     icon: 'icon-personal-cert'
                    //                 }
                    //             ]
                    //         }
                    //     ]
                    // }
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
