<template>
    <div class="biz-container atom-detail-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <div class="biz-side-bar">
            <side-bar
                :nav="sideMenuNav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'atomLevel'">
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
                    isLoading: true,
                    title: ''
                },
                sideMenuNav: {
                    backUrl: 'atomList',
                    backType: 'atom',
                    icon: 'atom-story',
                    title: '',
                    url: ''
                },
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'overview',
                                name: '概览',
                                icon: 'icon-overview',
                                showChildren: false
                            },
                            {
                                id: 'detail',
                                name: '详情',
                                icon: 'icon-txt',
                                showChildren: false
                            },
                            {
                                id: 'settings',
                                name: '设置',
                                icon: 'icon-cog',
                                isOpen: false,
                                showChildren: true,
                                children: [
                                    {
                                        id: 'member',
                                        name: '成员管理',
                                        icon: ''
                                    },
                                    {
                                        id: 'private',
                                        name: '私有配置',
                                        icon: ''
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        },
        computed: {
            ...mapGetters('store', {
                'currentAtom': 'getCurrentAtom'
            }),
            routeName () {
                return this.$route.name
            },
            atomCode () {
                return this.$route.params.atomCode
            }
        },
        watch: {
            currentAtom (newVal) {
                this.sideMenuNav.title = newVal.name
                this.loading.isLoading = false
            }
        },
        created () {
            if (!this.currentAtom.atomCode && this.routeName !== 'overview') {
                this.initAtom()
            }
            if (this.routeName === 'visible' || this.routeName === 'member' || this.routeName === 'private') {
                this.sideMenuList[0].list[2].isOpen = true
            }
        },
        methods: {
            goBack () {
                this.$router.push({
                    name: 'atomList',
                    params: {
                        type: 'atom'
                    }
                })
            },
            async initAtom () {
                try {
                    const res = await this.$store.dispatch('store/requestAtom', {
                        atomCode: this.atomCode
                    })
                    this.codeForm = res
                    this.$store.dispatch('store/updateCurrentaAtom', { res })
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
    .atom-detail-wrapper {
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
