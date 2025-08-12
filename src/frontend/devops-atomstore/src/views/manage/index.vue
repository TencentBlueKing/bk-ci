<template>
    <article class="g-store-main">
        <bread-crumbs
            :bread-crumbs="navList"
            :type="type"
        ></bread-crumbs>

        <transition-tab
            :panels="panels"
            :active-tab="currentTabName"
            @tab-change="tabChange"
            @child-tab-change="childTabChange"
        />

        <main
            v-bkloading="{ isLoading }"
            class="g-store-body"
        >
            <router-view
                v-if="Object.keys(detail).length > 0 && !isLoading"
                class="g-store-route"
            ></router-view>
        </main>
    </article>
</template>

<script>
    import api from '@/api'
    import breadCrumbs from '@/components/bread-crumbs.vue'
    import transitionTab from '@/components/transition-tab.vue'
    import { TYPE_ENUM } from '@/utils/constants'
    import { mapActions, mapGetters } from 'vuex'

    export default {
        components: {
            transitionTab,
            breadCrumbs
        },

        data () {
            return {
                isLoading: true
            }
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            }),
            currentTabName () {
                return this.$route.name
            },
            type () {
                return this.$route.params.type
            },
            itemName () {
                return this.detail?.[`${this.type}Name`] ?? this.$route.params.code
            },
            panels () {
                return [
                    ...(this.type === TYPE_ENUM.atom ? [{ label: this.$t('store.概览'), name: 'statisticData' }] : []),
                    ...(this.type !== TYPE_ENUM.image ? [{ label: this.$t('store.发布管理'), name: 'releaseManage' }] : []),
                    ...(this.type === TYPE_ENUM.atom ? [{ label: this.$t('store.协作审批'), name: 'approval' }] : []),
                    ...(this.type !== TYPE_ENUM.template ? [{ label: this.$t('store.基本信息'), name: 'show' }] : []),
                    { label: this.$t('store.基本设置'), name: 'setting' }
                ]
            },

            navList () {
                const labelMap = {
                    template: this.$t('store.流水线模板'),
                    image: this.$t('store.容器镜像'),
                    atom: this.$t('store.流水线插件')
                }
                return [
                    { name: this.$t('store.工作台') },
                    { name: labelMap[this.type], to: { name: `${this.type}Work` } },
                    { name: this.itemName }
                ]
            }
           
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions('store', [
                'requestAtom',
                'requestTemplateDetail',
                'requestImageDetailByCode',
                'setDetail',
                'clearDetail',
                'updateUserInfo'
            ]),
            
            tabChange (tabName) {
                const currentPanel = this.panels.find((panel) => (panel.name === tabName))

                if (currentPanel && currentPanel.name !== this.$route.name) {
                    this.$router.push({
                        name: currentPanel.name
                    })
                }
            },

            async initData () {
                try {
                    this.clearDetail()
                    this.isLoading = true
                    await Promise.all([
                        this.getMemInfo(),
                        this.requestDetail()
                    ])
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                } finally {
                    this.isLoading = false
                }
            },

            async requestDetail () {
                const code = this.$route.params.code
                const methodUrl = {
                    atom: this.requestAtom,
                    template: this.requestTemplateDetail,
                    image: this.requestImageDetailByCode
                }
                const res = await methodUrl[this.type](code)
                
                this.setDetail(res)
            },

            async getMemInfo () {
                const data = {
                    storeCode: this.$route.params.code,
                    storeType: this.type.toUpperCase()
                }
                const res = await api.getMemberView(data)
                if (res) {
                    this.updateUserInfo({
                        isProjectAdmin: res.type === 'ADMIN',
                        userName: res.userName
                    })
                }
            }
        }
    }
</script>
