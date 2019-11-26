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
                const navConf = {
                    icon: 'ticket',
                    title: this.$t('ticket.credentialManage'),
                    url: `${DOCS_URL_PREFIX}/${this.$t('allService')}/${this.$t('ticket.credentialManage')}/summary.html`,
                    menu: [
                        {
                            id: 'credentialList',
                            name: this.$t('ticket.myCredential'),
                            icon: 'icon-id'
                        },
                        {
                            id: 'createCredential',
                            name: this.$t('ticket.createCredential'),
                            icon: 'icon-id',
                            showChildren: false,
                            group: true,
                            children: [
                                {
                                    id: 'editCredential',
                                    name: this.$t('ticket.editCredential'),
                                    icon: 'icon-id'
                                },
                                {
                                    id: 'createCredentialWithType',
                                    name: this.$t('ticket.createCredential'),
                                    icon: 'icon-id'
                                }
                            ]
                        },
                        {
                            id: 'certList',
                            name: this.$t('ticket.myCert'),
                            icon: 'icon-personal-cert'
                        },
                        {
                            id: 'createCert',
                            name: this.$t('ticket.createCert'),
                            icon: 'icon-personal-cert',
                            showChildren: false,
                            children: [
                                {
                                    id: 'editCert',
                                    name: this.$t('ticket.editCert'),
                                    icon: 'icon-personal-cert'
                                }
                            ]
                        }
                    ]
                }
                if (VERSION_TYPE === 'ee') {
                    navConf.menu.splice(2, 2)
                }
                return navConf
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
