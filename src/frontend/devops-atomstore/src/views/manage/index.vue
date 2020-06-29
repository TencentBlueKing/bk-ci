<template>
    <article class="store-manage">
        <header class="manage-title">
            <span class="banner-des quick-route" @click="goToStore"> {{ $t('store.研发商店') }} </span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des quick-route" @click="goToWorkList"> {{ $t('store.工作台') }}</span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des quick-route" @click="goToDetail">{{ type | typeFilter }}</span>
            <i class="right-arrow banner-arrow"></i>
            <span class="banner-des">{{ $route.params.code }}</span>
        </header>

        <bk-tab :active.sync="activeTab" type="unborder-card" class="manage-tabs" @tab-change="tabChange">
            <bk-tab-panel v-for="(panel, index) in panels" v-bind="panel" :key="index">
                <transition name="fade">
                    <ul v-if="activeTab === panel.name && panel.showChildTab" class="manage-child-tabs">
                        <li v-for="childPanel in panel.children.filter(x => !x.hidden)"
                            :key="childPanel.name"
                            @click="tabChange(childPanel.name)"
                            :class="['manage-child-tab', { active: activeChildTab === childPanel.name }]"
                        >{{ childPanel.label }}</li>
                    </ul>
                </transition>
            </bk-tab-panel>
        </bk-tab>

        <main v-bkloading="{ isLoading }" class="manage-main">
            <transition :name="transitionName">
                <router-view v-if="Object.keys(detail).length > 0 && !isLoading" class="manage-route" v-bind="routekey"></router-view>
            </transition>
        </main>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'

    export default {
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
                transitionName: '',
                panelMap: {
                    atom: [
                        { label: this.$t('store.概览'), name: 'overView' },
                        { label: this.$t('store.详情'), name: 'detail', children: [{ name: 'show' }, { name: 'edit' }], showChildTab: false },
                        { label: this.$t('store.审批'), name: 'approval' },
                        { label: this.$t('store.设置'),
                          name: 'setting',
                          children: [
                              { label: this.$t('store.成员管理'), name: 'member' },
                              { label: this.$t('store.私有配置'), name: 'private' }
                          ],
                          showChildTab: true }
                    ],
                    image: [
                        { label: this.$t('store.详情'), name: 'detail', children: [{ name: 'show' }, { name: 'edit' }], showChildTab: false },
                        { label: this.$t('store.设置'),
                          name: 'setting',
                          children: [
                              { label: this.$t('store.成员管理'), name: 'member' }
                          ],
                          showChildTab: true }
                    ],
                    template: [
                        { label: this.$t('store.设置'),
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

            routekey () {
                const res = {}
                if (this.activeChildTab) res.key = this.activeChildTab
                return res
            }
        },

        watch: {
            '$route.name' (val, oldVal) {
                const calcIndex = (name) => {
                    let res = ''
                    this.panels.forEach((panel, index) => {
                        if (name === panel.name) res = index + '0'
                        if (panel.children) {
                            panel.children.forEach((childPanel, childIndex) => {
                                if (!panel.showChildTab) childIndex = 1
                                if (name === childPanel.name) res = `${index}${childIndex}`
                            })
                        }
                    })
                    return +res
                }
                const diff = calcIndex(val) - calcIndex(oldVal)
                this.transitionName = diff > 0 ? 'g-slide-left' : diff === 0 ? 'atom-fade' : 'g-slide-right'
                this.calcActiveTab()
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
                this.calcActiveTab()
            },

            tabChange (tabName) {
                const currentPanel = this.panels.find((panel) => (panel.name === tabName || (panel.children && panel.children.some(x => x.name === tabName)))) || {}
                const panelChildren = currentPanel.children
                const name = panelChildren && tabName === currentPanel.name ? (panelChildren[0] || {}).name : tabName
                this.$router.push({ name })
            },

            calcActiveTab (val) {
                const name = val || this.$route.name
                const currentPanel = this.panels.find((panel) => (panel.name === name || (panel.children && panel.children.some(x => x.name === name)))) || {}
                this.activeTab = currentPanel.name
                this.activeChildTab = currentPanel.name !== name && name
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
                const code = this.$route.params.code
                const methodGenerator = {
                    atom: () => this.$store.dispatch('store/getMemberInfo', code),
                    template: () => Promise.resolve(),
                    image: () => this.$store.dispatch('store/requestGetMemInfo', code)
                }
                const currentMethod = methodGenerator[this.type]

                return currentMethod().then((res = {}) => {
                    const userInfo = {
                        isProjectAdmin: res.type === 'ADMIN',
                        userName: res.userName
                    }
                    this.$store.dispatch('store/updateUserInfo', userInfo)
                })
            },

            goToWorkList () {
                this.$router.push({
                    name: 'workList',
                    params: {
                        type: this.type
                    }
                })
            },

            goToStore () {
                this.$router.push({ name: 'atomHome' })
            },

            goToDetail () {
                let defaultPage = this.panels[0]
                if (defaultPage.children && defaultPage.children.length > 0) defaultPage = defaultPage.children[0]
                this.$router.push({ name: defaultPage.name })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .store-manage {
        background-color: #f1f2f3;
        height: 100vh;
        color: #222;
        display: flex;
        max-height: 100vh;
        padding-bottom: 40px;
        flex-direction: column;
        overflow: hidden;
        .manage-title {
            height: 56px;
            line-height: 56px;
            display: flex;
            align-items: center;
            padding-left: .32rem;
            margin-bottom: 32px;
            background-color: #fff;
            color: #999;
            .right-arrow {
                height: 56px;
                &::after {
                    margin-top: -2.5px;
                    border-color: #252935;
                }
            }
            .quick-route {
                color: #222;
                cursor: pointer;
            }
        }
        .manage-tabs {
            width: 14.6rem;
            margin: 0 auto;
            box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
            .manage-child-tabs {
                height: 47px;
                line-height: 47px;
                padding: 12px 16px;
                background-color: #fff;
                &::after {
                    content: '';
                    display: table;
                    clear: both;
                }
                .manage-child-tab {
                    float: left;
                    padding: 0 16px;
                    font-size: 16px;
                    line-height: 22px;
                    color: #666;
                    cursor: pointer;
                    &.active {
                        color: #1a6df3;
                    }
                    &:not(:last-child) {
                        border-right: 1px solid #ebedf0;
                    }
                }
            }
            /deep/ .bk-tab-header {
                background-color: #fff;
                height: 64px;
                line-height: 64px;
                background-image: linear-gradient(transparent 63px,#dcdee5 0);
                .bk-tab-label-list {
                    height: 64px;
                    .bk-tab-label-item {
                        line-height: 64px;
                        color: #666;
                        &::after {
                            height: 3px;
                            width: 64px;
                            left: 18px;
                        }
                        &.active {
                            color: #3a84ff;
                        }
                        .bk-tab-label {
                            font-size: 16px;
                        }
                    }
                }
            }
            /deep/ .bk-tab-section {
                padding: 0;
            }
        }
        .manage-main {
            width: 14.6rem;
            margin: 16px auto 0;
            position: relative;
            flex: 1;
            height: 0;
            .manage-route {
                height: 100%;
                box-shadow: 1px 2px 3px 0 rgba(0,0,0,0.05);
            }
        }
    }
</style>
