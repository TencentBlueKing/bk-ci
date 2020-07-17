<template>
    <article class="g-store-main">
        <header class="g-store-title">
            <icon class="title-icon" name="color-logo-store" size="22" />
            <span class="banner-des quick-route" @click="goToStore"> {{ $t('store.研发商店') }} </span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des"> {{ $t('store.工作台') }} </span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des quick-route" @click="goToWorkList"> {{ type | typeFilter }}</span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des">{{ $route.params.code }}</span>
        </header>

        <transition-tab :panels="panels"
            @tab-change="tabChange"
            @child-tab-change="childTabChange"
        ></transition-tab>

        <main v-bkloading="{ isLoading }" class="g-store-body">
            <router-view v-if="Object.keys(detail).length > 0 && !isLoading" class="g-store-route" v-bind="routekey"></router-view>
        </main>
    </article>
</template>

<script>
    import api from '@/api'
    import { mapGetters } from 'vuex'
    import transitionTab from '@/components/transition-tab.vue'

    export default {
        components: {
            transitionTab
        },

        filters: {
            typeFilter (val) {
                const bkLocale = window.devops || {}
                let res = ''
                switch (val) {
                    case 'template':
                        res = bkLocale.$t('store.流水线模板')
                        break
                    case 'image':
                        res = bkLocale.$t('store.容器镜像')
                        break
                    default:
                        res = bkLocale.$t('store.流水线插件')
                        break
                }
                return res
            }
        },

        data () {
            return {
                activeTab: '',
                activeChildTab: '',
                panels: [],
                isLoading: true,
                type: '',
                routekey: {},
                panelMap: {
                    atom: [
                        { label: this.$t('store.概览'), name: 'overView' },
                        { label: this.$t('store.发布管理'), name: 'release', children: [{ label: this.$t('store.版本管理'), name: 'version' }], showChildTab: true },
                        { label: this.$t('store.协作审批'), name: 'approval' },
                        { label: this.$t('store.基本信息'), name: 'detail', children: [{ name: 'show' }, { name: 'edit' }], showChildTab: false },
                        { label: this.$t('store.基本设置'),
                          name: 'setting',
                          children: [
                              { label: this.$t('store.成员管理'), name: 'member' },
                              { label: this.$t('store.私有配置'), name: 'private' }
                          ],
                          showChildTab: true }
                    ],
                    image: [
                        { label: this.$t('store.发布管理'), name: 'release', children: [{ label: this.$t('store.版本管理'), name: 'version' }], showChildTab: true },
                        { label: this.$t('store.基本信息'), name: 'detail', children: [{ name: 'show' }, { name: 'edit' }], showChildTab: false },
                        { label: this.$t('store.基本设置'),
                          name: 'setting',
                          children: [
                              { label: this.$t('store.成员管理'), name: 'member' }
                          ],
                          showChildTab: true }
                    ],
                    template: [
                        { label: this.$t('store.基本设置'),
                          name: 'setting',
                          children: [],
                          showChildTab: true }
                    ]
                }
            }
        },

        computed: {
            ...mapGetters('store', {
                'detail': 'getDetail'
            }),

            storeType () {
                const storeTypeMap = {
                    atom: 'ATOM',
                    template: 'TEMPLATE',
                    image: 'IMAGE'
                }
                return storeTypeMap[this.type]
            }
        },

        created () {
            this.initStatus()
            this.initData()
        },

        methods: {
            initStatus () {
                const params = this.$route.params || {}
                this.type = params.type
                this.panels = this.panelMap[this.type]
            },

            tabChange (tabName) {
                this.routekey.key = undefined
                const currentPanel = this.panels.find((panel) => (panel.name === tabName)) || {}
                const panelChildren = currentPanel.children || []
                const name = panelChildren.length ? (panelChildren[0] || {}).name : currentPanel.name
                this.$router.push({ name })
            },

            childTabChange (name) {
                this.routekey.key = name
                this.$router.push({ name })
            },

            initData () {
                this.$store.dispatch('store/clearDetail')
                this.isLoading = true
                Promise.all([this.getMemInfo(), this.requestDetail()]).catch((err) => {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }).finally(() => (this.isLoading = false))
            },

            requestDetail () {
                const code = this.$route.params.code
                const methodUrl = {
                    atom: 'store/requestAtom',
                    template: 'store/requestTemplate',
                    image: 'store/requestImageDetailByCode'
                }
                const currentUrl = methodUrl[this.type]
                return this.$store.dispatch(currentUrl, code).then(res => this.$store.dispatch('store/setDetail', res))
            },

            getMemInfo () {
                const data = {
                    storeCode: this.$route.params.code,
                    storeType: this.storeType
                }
                return api.getMemberView(data).then((res = {}) => {
                    const userInfo = {
                        isProjectAdmin: res.type === 'ADMIN',
                        userName: res.userName
                    }
                    this.$store.dispatch('store/updateUserInfo', userInfo)
                })
            },

            goToWorkList () {
                const name = `${this.type}Work`
                this.$router.push({
                    name
                })
            },

            goToStore () {
                this.$router.push({
                    name: 'atomHome',
                    query: {
                        pipeType: this.type
                    }
                })
            }
        }
    }
</script>
