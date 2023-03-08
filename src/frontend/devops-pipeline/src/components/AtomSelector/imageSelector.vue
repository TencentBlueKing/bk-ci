<template>
    <transition name="selector-slide">
        <section class="selector-popup" v-bk-clickoutside="closeImageSelect" v-show="isShow">
            <main class="selector-main">
                <header class="selector-header">
                    <h3>{{ $t('editPage.selectImage') }}<i @click="freshList(searchKey)" :class="[{ 'spin-icon': isLoading }, 'devops-icon', 'icon-refresh', 'fresh']" /></h3>
                    <bk-input class="search-input"
                        ref="searchStr"
                        :clearable="true"
                        :placeholder="$t('editPage.enterSearch')"
                        right-icon="icon-search"
                        :value="searchKey"
                        @input="handleClear"
                        @enter="handleSearch">
                    </bk-input>
                </header>
                <bk-tab v-if="!searchKey" size="small" ref="imageTab" :active.sync="currentTab" type="unborder-card" class="select-tab">
                    <bk-tab-panel
                        v-for="tab in tabList"
                        :key="tab.classifyCode"
                        :name="tab.classifyCode"
                        v-bkloading="{ isLoading }"
                    >
                        <span slot="label" @click="getInstallImageList(tab)" class="tab-label">{{ tab.classifyName }}</span>
                        <template v-if="!isLoading">
                            <ul v-if="tab.recommendData.length">
                                <card :current-item.sync="currentItem"
                                    :card="card"
                                    v-for="card in tab.recommendData"
                                    :key="card"
                                    :type="tab.classifyCode"
                                    :code="code"
                                    @choose="choose">
                                </card>
                            </ul>

                            <section v-if="tab.unRecommendData.length">
                                <h3 :class="[{ 'expand': tab.expandObtained }, 'search-title', 'gap-border', 'uninstall']" @click="tab.expandObtained = !tab.expandObtained">
                                    {{ $t('editPage.unRecommend') }}（{{tab.unRecommendData.length}}）
                                    <bk-popover placement="top">
                                        <i class="devops-icon icon-info-circle "></i>
                                        <div slot="content">
                                            {{ $t('editPage.unRecomReason') }}
                                        </div>
                                    </bk-popover>
                                </h3>
                                <ul v-if="tab.expandObtained">
                                    <card :current-item.sync="currentItem"
                                        :card="card"
                                        v-for="card in tab.unRecommendData"
                                        :key="card"
                                        :type="tab.classifyCode"
                                        :code="code"
                                        @choose="choose">
                                    </card>
                                </ul>
                            </section>

                            <p v-if="!tab.unRecommendData.length && !tab.recommendData.length" class="list-empty"></p>
                        </template>
                    </bk-tab-panel>
                </bk-tab>

                <section v-else class="search-result" v-bkloading="{ isLoading }">
                    <template v-if="!isLoading">
                        <template v-if="searchInstallList.length">
                            <h3 class="search-title">{{ $t('editPage.installed') }}</h3>
                            <card :current-item.sync="currentItem"
                                :card="card"
                                v-for="card in searchInstallList"
                                type="store"
                                :key="card"
                                :code="code"
                                @choose="choose">
                            </card>
                        </template>
                        
                        <template v-if="searchUninstallList.length">
                            <h3 class="search-title gap-border">{{ $t('editPage.unInstalled') }}</h3>
                            <card :current-item.sync="currentItem"
                                :card="card"
                                v-for="card in searchUninstallList"
                                type="store"
                                :key="card"
                                :code="code"
                                @choose="choose">
                            </card>
                        </template>
                        
                        <section v-if="searchUnrecomandList.length">
                            <h3 :class="[{ 'expand': searchExpandObtained }, 'search-title', 'gap-border', 'uninstall']" @click="searchExpandObtained = !searchExpandObtained">
                                {{ $t('editPage.unRecommend') }}
                                <bk-popover placement="top">
                                    <i class="devops-icon icon-info-circle "></i>
                                    <div slot="content">
                                        {{ $t('editPage.unRecomReason') }}
                                    </div>
                                </bk-popover>
                            </h3>
                            <ul v-if="searchExpandObtained">
                                <card :current-item.sync="currentItem"
                                    :card="card"
                                    v-for="card in searchUnrecomandList"
                                    :key="card"
                                    type="store"
                                    :code="code"
                                    @choose="choose">
                                </card>
                            </ul>
                        </section>

                        <p v-if="!searchInstallList.length && !searchUninstallList.length && !searchUnrecomandList.length" class="list-empty"></p>
                    </template>
                </section>
            </main>
        </section>
    </transition>
</template>

<script>
    import { mapActions } from 'vuex'
    import card from './imageCard'

    export default {
        components: {
            card
        },

        props: {
            code: {
                type: String,
                required: true
            },

            isShow: {
                type: Boolean,
                required: true
            },

            buildResourceType: {
                type: String,
                required: true
            }
        },

        data () {
            return {
                isLoading: false,
                isLoadingMore: false,
                searchExpandObtained: false,
                searchKey: '',
                currentTab: 'all',
                currentItem: this.code,
                tabList: [],
                searchInstallList: [],
                searchUninstallList: [],
                searchUnrecomandList: []
            }
        },

        watch: {
            buildResourceType (val) {
                if (['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(val)) {
                    this.isLoading = true
                    this.searchKey = ''
                    this.clearData()
                    this.initData()
                }
            },

            isShow (val) {
                const ele = this.$refs.imageTab.$el.querySelector('.bk-tab-section')
                const method = val ? 'addEventListener' : 'removeEventListener'
                ele[method]('scroll', this.scrollLoadMore, { passive: true })
            }
        },

        created () {
            this.initData()
        },

        methods: {
            ...mapActions('pipelines', [
                'requestImageClassifys',
                'requestInstallImageList',
                'requestStoreImageList',
                'requestMarketImage'
            ]),

            initData () {
                this.requestImageClassifys().then((res) => {
                    res.data.push({
                        classifyCode: 'store',
                        classifyName: this.$i18n && this.$t('editPage.store'),
                        id: 'store'
                    })
                    res.data.unshift({
                        classifyCode: 'all',
                        classifyName: this.$i18n && this.$t('editPage.all'),
                        id: ''
                    })
                    this.tabList = (res.data || []).map((item) => {
                        item.expandObtained = false
                        item.page = 1
                        item.pageSize = 50
                        item.recommendData = []
                        item.unRecommendData = []
                        item.loadEnd = false
                        return item
                    })
                    return this.getInstallImageList(this.tabList[0])
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err }))
            },

            getInstallImageList (tab) {
                if (tab.recommendData.length || tab.loadEnd) return
                this.isLoading = true
                Promise.all([this.getApiData(tab, false), this.getApiData(tab, true)]).finally(() => (this.isLoading = false))
            },

            getApiData (tab, recommendFlag) {
                this.isLoadingMore = true
                const postData = Object.assign({
                    projectCode: this.$route.params.projectId,
                    agentType: this.buildResourceType,
                    recommendFlag
                }, tab)

                const method = tab.classifyCode === 'store' ? 'requestStoreImageList' : 'requestInstallImageList'
                return this[method](postData).then((res) => {
                    const data = res.data || {}
                    if (recommendFlag) {
                        tab.page++
                        tab.recommendData = tab.recommendData.concat(data.records || [])
                        tab.loadEnd = tab.recommendData.length >= data.count
                    } else {
                        tab.unRecommendData = data.records || []
                    }
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => {
                    this.isLoadingMore = false
                })
            },

            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                const tab = this.tabList.find((item) => (item.classifyCode === this.currentTab))
                if (bottomDis <= 500 && !this.isLoadingMore && !tab.loadEnd) this.getApiData(tab, true)
            },

            closeImageSelect () {
                this.searchKey = ''
                this.currentTab = 'all'
                this.$emit('update:isShow', false)
            },

            freshList (searchKey) {
                if (searchKey) {
                    this.handleSearch(searchKey)
                } else {
                    this.isLoading = true
                    const tab = this.tabList.find((item) => (item.classifyCode === this.currentTab))
                    this.clearData()
                    this.getInstallImageList(tab)
                }
            },

            clearData () {
                this.tabList.forEach((item) => {
                    item.expandObtained = false
                    item.page = 1
                    item.pageSize = 50
                    item.recommendData = []
                    item.unRecommendData = []
                    item.loadEnd = false
                })
            },

            handleSearch (value) {
                this.searchKey = value.trim()
                this.searchImage(this.searchKey)
            },

            searchImage (keyword) {
                this.isLoading = true
                const params = this.$route.params || {}
                const projectCode = params.projectId
                const tab = this.tabList.find((item) => (item.classifyCode === this.currentTab))
                const postData = Object.assign({
                    keyword,
                    projectCode,
                    agentType: this.buildResourceType
                }, tab)

                Promise.all([
                    this.requestMarketImage(Object.assign(postData, { recommendFlag: true })),
                    this.requestMarketImage(Object.assign(postData, { recommendFlag: false }))
                ]).then(([recom, unRecom]) => {
                    const unReData = unRecom.data || {}
                    this.searchUnrecomandList = unReData.records || []

                    const reData = recom.data || {}
                    const reList = reData.records || []
                    this.searchInstallList = reList.filter(x => x.installedFlag)
                    this.searchUninstallList = reList.filter(x => !x.installedFlag)
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => (this.isLoading = false))
            },

            handleClear (str) {
                if (str === '') {
                    const input = this.$refs.searchStr || {}
                    input.curValue = ''
                    this.searchKey = ''
                }
            },

            choose (card) {
                this.closeImageSelect()
                this.$emit('choose', card)
            }
        }
    }
</script>

<style lang="scss">
    @import '../../scss/conf';
    .search-title {
        line-height:16px;
        font-weight:bold;
        font-size: 12px;
        margin: 9px 0;
        &.gap-border {
            padding-top: 10px;
            border-top: 1px solid #ebf0f5;
        }
    }

    .uninstall{
        position: relative;
        cursor: pointer;
        ::v-deep .bk-tooltip {
            vertical-align: bottom;
        }
        &:after {
            content: '';
            position: absolute;
            right: 4px;
            top: 13px;
            border-right: 1px solid $fontWeightColor;
            border-bottom: 1px solid $fontWeightColor;
            display: inline-block;
            height: 7px;
            width: 7px;
            transform: rotate(-45deg);
            transition: transform 200ms;
            transform-origin: 5.5px 5.5px;
        }
        &.expand:after {
            transform: rotate(45deg);
        }
    }

    .list-empty {
        background: url('../../images/no_result.png') center no-repeat;
        height: 500px;
    }

    .selector-popup {
        position: fixed;
        right: 660px;
        width: 600px;
        height: calc(100% - 20px);
        background: white;
        z-index: 2000;
        border: 1px solid $borderColor;
        border-radius: 5px;
        top: 0;
        margin: 10px 0;
        &:before {
            content: '';
            display: block;
            position: absolute;
            width: 10px;
            height: 10px;
            background: white;
            border: 1px solid $borderColor;
            border-left-color: white;
            border-bottom-color: white;
            transform: rotate(45deg);
            right: -6px;
            top: 136px;
        }
        .selector-main {
            height: 100%;
        }
        .selector-header {
            position: relative;
            height: 36px;
            display: flex;
            flex-direction: row;
            justify-content: space-between;
            align-items: center;
            margin: 24px 21px 10px 21px;
            .fresh {
                cursor: pointer;
                display: inline-block;
                font-size: 14px;
                padding: 4px;
                margin-left: 3px;
                color: $primaryColor;
                &.spin-icon {
                    color: $fontLighterColor
                }
            }
            > h3 {
                font-size: 14px;
                margin: 0;
            }
            .search-input {
                width: 200px;
            }
        }
        .search-result {
            height: calc(100% - 70px);
            overflow-y: auto;
            margin: 0 11px 0 21px;
            padding-right: 10px;
            padding-bottom: 10px;
            .search-title {
                line-height:16px;
                font-weight:bold;
                font-size: 12px;
                margin: 9px 0;
                &.gap-border {
                    padding-top: 10px;
                    border-top: 1px solid #ebf0f5;
                }
            }
        }
        .select-tab {
            height: calc(100% - 70px);
            border: 0;
            font-size: 12px;
            color: $fontWeightColor;
            font-weight: 500;
            padding: 0 21px 10px 21px;
            overflow: hidden;
            .tab-label {
                padding: 0 18px;
                line-height: 42px;
                display: inline-block;
            }
            div.bk-tab-section {
                height: calc(100% - 42px);
                overflow-y: auto;
                padding: 0;
                .bk-tab-content {
                    height: 100%;
                }
            }
            .bk-tab-header {
                padding: 0;
                background-image: linear-gradient(transparent 41px,#dcdee5 0);
                .bk-tab-label-wrapper {
                    .bk-tab-label-list {
                        .bk-tab-label-item {
                            min-width: auto;
                            margin: 0;
                            .bk-tab-label {
                                font-size: 12px;
                                font-weight: normal;
                                &.active {
                                    font-weight: bold;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    .empty-atom-list {
        display: flex;
        height: 100%;
        align-items: center;
        justify-content: center;
    }
    .selector-slide-enter-active, .selector-slide-leave-active {
        transition: transform .2s linear, opacity .2s cubic-bezier(1, -0.05, .94, .17);
    }

    .selector-slide-enter {
        -webkit-transform: translate3d(600px, 0, 0);
        transform: translateX(600px);
        opacity: 0;
    }

    .selector-slide-leave-active {
        display: none;
    }
</style>
