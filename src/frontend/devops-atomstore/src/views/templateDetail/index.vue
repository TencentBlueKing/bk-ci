<template>
    <div class="biz-container template-detail-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <div class="biz-side-bar">
            <side-bar
                :nav="sideMenuNav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'templateLevel'">
            </side-bar>
        </div>
        <router-view style="width: 100%" v-show="!loading.isLoading"></router-view>
    </div>
</template>

<script>
    import sideBar from '@/components/side-nav'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            'side-bar': sideBar
        },
        data () {
            return {
                loading: {
                    isLoading: false,
                    title: ''
                },
                sideMenuNav: {
                    backUrl: 'atomList',
                    backType: 'template',
                    icon: 'atom-story',
                    title: '',
                    url: ''
                },
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'tplOverview',
                                name: '概览',
                                icon: 'icon-overview',
                                showChildren: false
                            }
                            // {
                            //     id: 'detail',
                            //     name: '详情',
                            //     icon: 'icon-txt',
                            //     showChildren: false
                            // },
                            // {
                            //     id: 'tplSettings',
                            //     name: '设置',
                            //     icon: 'icon-cog',
                            //     isOpen: false,
                            //     showChildren: true,
                            //     children: [
                            //         // {
                            //         //     id: 'member',
                            //         //     name: '成员管理',
                            //         //     icon: ''
                            //         // },
                            //         {
                            //             id: 'tplVisible',
                            //             name: '可见范围',
                            //             icon: ''
                            //         }
                            //     ]
                            // }
                        ]
                    }
                ]
            }
        },
        computed: {
            ...mapGetters('store', {
                'currentTemplate': 'getCurrentTemplate'
            }),
            routeName () {
                return this.$route.name
            },
            templateCode () {
                return this.$route.params.templateCode
            }
        },
        watch: {
            currentTemplate (newVal) {
                this.sideMenuNav.title = newVal.templateName
                this.loading.isLoading = false
            }
        },
        created () {
            if (!this.currentTemplate.templateCode && this.routeName !== 'tplOverview') {
                this.initTemplate()
            }
            if (this.routeName === 'tplVisible' || this.routeName === 'member') {
                this.sideMenuList[0].list[1].isOpen = true
            }
        },
        methods: {
            goBack () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'template'
                    }
                })
            },
            async initTemplate () {
                try {
                    const res = await this.$store.dispatch('store/requestTemplate', {
                        templateCode: this.templateCode
                    })
                    this.$store.dispatch('store/updateCurrentaTemplate', { res })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    .template-detail-wrapper {
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
        overflow: auto;
    }
</style>
