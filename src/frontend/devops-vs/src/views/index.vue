<template>
    <div class="biz-container vulnerability-scan-wrapper">
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
                    icon: 'vs',
                    title: '漏洞扫描-终端（金刚）',
                    url: '',
                    menu: [
                        {
                            id: 'vsList',
                            name: '扫描记录',
                            icon: 'devops-icon icon-txt',
                            showChildren: false,
                            children: [
                                {
                                    id: 'vsReport',
                                    name: '扫描报告',
                                    icon: 'devops-icon icon-list'
                                }
                            ]
                        },
                        {
                            id: 'createVs',
                            name: '新增扫描',
                            icon: 'devops-icon icon-create-vs',
                            showChildren: false
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
    @import './../scss/conf';

    .vulnerability-scan-wrapper {
        min-width: 1200px;
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
