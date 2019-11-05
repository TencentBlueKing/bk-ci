<template>
    <div class="biz-container atom-detail-wrapper" v-bkloading="{ isLoading }">
        <div class="biz-side-bar">
            <side-bar
                :nav="sideMenuNav"
                :side-menu-list="sideMenuList"
                :sub-system-name="'atomLevel'">
            </side-bar>
        </div>
        <router-view style="width: 100%" v-if="!isLoading"></router-view>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import sideBar from '@/components/side-nav'

    export default {
        components: {
            'side-bar': sideBar
        },
        data () {
            return {
                isLoading: true,
                sideMenuList: [
                    {
                        list: [
                            {
                                id: 'overview',
                                selectId: ['overview'],
                                name: this.$t('概览'),
                                icon: 'icon-overview',
                                showChildren: false
                            },
                            {
                                id: 'detail',
                                selectId: ['detail', 'edit'],
                                name: this.$t('详情'),
                                icon: 'icon-txt',
                                showChildren: false
                            },
                            {
                                id: 'approval',
                                selectId: ['approval'],
                                name: this.$t('审批'),
                                icon: 'icon-panel-permission',
                                showChildren: false
                            },
                            {
                                id: 'settings',
                                selectId: ['settings'],
                                name: this.$t('设置'),
                                icon: 'icon-cog',
                                isOpen: false,
                                showChildren: true,
                                children: [
                                    {
                                        id: 'member',
                                        selectId: ['member'],
                                        name: this.$t('成员管理'),
                                        icon: ''
                                    },
                                    {
                                        id: 'visible',
                                        selectId: ['visible'],
                                        name: this.$t('可见范围'),
                                        icon: ''
                                    },
                                    {
                                        id: 'private',
                                        selectId: ['private'],
                                        name: this.$t('私有配置'),
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
            },
            sideMenuNav () {
                return {
                    backUrl: 'atomList',
                    backType: 'atom',
                    icon: 'atom-story',
                    title: this.currentAtom.name,
                    url: ''
                }
            }
        },

        created () {
            this.initData()
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

            initData () {
                if (this.routeName === 'visible' || this.routeName === 'member' || this.routeName === 'private') {
                    this.sideMenuList[0].list[3].isOpen = true
                }
                Promise.all([this.getMemInfo(), this.requestAtomDetail()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            requestAtomDetail () {
                return this.$store.dispatch('store/requestAtom', {
                    atomCode: this.atomCode
                }).then(res => this.$store.dispatch('store/updateCurrentaAtom', { res }))
            },

            getMemInfo () {
                return this.$store.dispatch('store/getMemberInfo', this.atomCode).then((res = {}) => {
                    const userInfo = {
                        isProjectAdmin: res.type === 'ADMIN',
                        userName: res.userName
                    }
                    if (!userInfo.isProjectAdmin) this.sideMenuList[0].list.splice(2, 1)
                    this.$store.dispatch('store/updateUserInfo', userInfo)
                })
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
    .disable {
        cursor: not-allowed !important;
        color: #dfe0e5 !important;
    }
</style>
