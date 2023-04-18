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
                    title: this.$t('ticket.credentialManage'),
                    menu: [
                        {
                            id: 'credentialList',
                            name: this.$t('ticket.myCredential'),
                            icon: 'devops-icon icon-id'
                        },
                        {
                            id: 'createCredential',
                            name: this.$t('ticket.createCredential'),
                            icon: 'devops-icon icon-id',
                            showChildren: false,
                            group: true,
                            children: [
                                {
                                    id: 'editCredential',
                                    name: this.$t('ticket.editCredential'),
                                    icon: 'devops-icon icon-id'
                                },
                                {
                                    id: 'createCredentialWithType',
                                    name: this.$t('ticket.createCredential'),
                                    icon: 'devops-icon icon-id'
                                }
                            ]
                        },
                        {
                            id: 'certList',
                            name: this.$t('ticket.myCert'),
                            icon: 'devops-icon icon-personal-cert'
                        },
                        {
                            id: 'createCert',
                            name: this.$t('ticket.createCert'),
                            icon: 'devops-icon icon-personal-cert',
                            showChildren: false,
                            children: [
                                {
                                    id: 'editCert',
                                    name: this.$t('ticket.editCert'),
                                    icon: 'devops-icon icon-personal-cert'
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
