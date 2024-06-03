<template>
    <article v-bkloading="{ isLoading, opacity: 1 }">
        <h3 class="list-type" v-bk-clickoutside="closeOrderList">
            <span class="list-count"> {{ $t('store.总数 :') }} <strong>{{count}}</strong></span>
            <span class="list-sort"> {{ $t('store.排序：') }} </span>
            <span :class="[{ 'show-type': showOrderList }, 'list-order']" @click.stop="showOrderList = !showOrderList">{{ orderType.name }}</span>
            <ul class="list-menu" v-show="showOrderList">
                <li v-for="(order, index) in orderList" :key="index" @click.stop="chooseOrderType(order)">{{ order.name }}</li>
            </ul>
        </h3>

        <hgroup class="list-cards" v-if="!isLoading">
            <card v-for="card in cards" :key="card.atomCode" :atom="card" :has-summary="true" class="list-card"></card>
        </hgroup>
        <div class="g-empty list-empty" v-if="cards.length <= 0">
            <p style="margin-top: 50px;"> {{ $t('store.没找到相关结果') }} </p>
            <div class="empty-tips">
                {{ $t('store.可以尝试 调整关键词 或') }}
                <button class="bk-text-button" @click="handleClear">{{$t('store.清空筛选条件')}}</button>
            </div>
        </div>
    </article>
</template>

<script>
    import eventBus from '@/utils/eventBus.js'
    import card from '@/components/common/card'

    export default {
        components: {
            card
        },

        data () {
            return {
                pageSize: 40,
                page: 1,
                isLoadingMore: false,
                loadEnd: false,
                isLoading: true,
                cards: [],
                count: 0,
                orderType: { id: 'DOWNLOAD_COUNT', name: this.$t('store.按热度') },
                showOrderList: false
            }
        },

        computed: {
            orderList () {
                const orderMap = {
                    atom: [
                        { id: 'NAME', name: this.$t('store.按名称A-Z') },
                        { id: 'CREATE_TIME', name: this.$t('store.按创建时间') },
                        { id: 'UPDATE_TIME', name: this.$t('store.按修改时间') },
                        { id: 'PUBLISHER', name: this.$t('store.按发布者') },
                        { id: 'RECENT_EXECUTE_NUM', name: this.$t('store.按热度') }
                    ]
                }
                const defaultOrder = [
                    { id: 'NAME', name: this.$t('store.按名称A-Z') },
                    { id: 'CREATE_TIME', name: this.$t('store.按创建时间') },
                    { id: 'UPDATE_TIME', name: this.$t('store.按修改时间') },
                    { id: 'PUBLISHER', name: this.$t('store.按发布者') },
                    { id: 'DOWNLOAD_COUNT', name: this.$t('store.按热度') }
                ]
                const type = this.$route.query.pipeType || 'atom'
                return orderMap[type] || defaultOrder
            }
        },

        watch: {
            '$route.query': {
                handler () {
                    this.initData()
                    this.resetListData()
                },
                immediate: true
            }
        },

        mounted () {
            this.addScrollLoadMore()
        },

        beforeDestroy () {
            this.removeScrollLoadMore()
        },

        methods: {
            closeOrderList () {
                this.showOrderList = false
            },

            initData () {
                const { sortType, pipeType } = this.$route.query
                const defaultOrderTypeMap = {
                    atom: 'RECENT_EXECUTE_NUM'
                }
                const orderType = sortType || defaultOrderTypeMap[pipeType] || 'DOWNLOAD_COUNT'
                const order = this.orderList.find((order) => (order.id === orderType))
                this.orderType = order
            },

            resetListData () {
                this.page = 1
                this.isLoading = true
                this.loadEnd = false
                this.getListData(true)
            },

            getListData (isReset = false) {
                this.isLoadingMore = true
                const { searchStr, classifyKey, classifyValue, score, features, pipeType } = this.$route.query || {}

                const featureObj = {}
                if (features) {
                    const featuresArray = features.split(';')
                    featuresArray.forEach((feature) => {
                        feature = feature.split('-')
                        featureObj[feature[0]] = feature[1]
                    })
                }

                const postData = {
                    sortType: this.orderType.id,
                    score,
                    page: this.page,
                    pageSize: this.pageSize,
                    keyword: searchStr,
                    ...featureObj
                }
                if (classifyValue !== 'all') postData[classifyKey] = classifyValue

                const apiFun = {
                    atom: () => this.$store.dispatch('store/requestMarketAtom', postData),
                    template: () => this.$store.dispatch('store/requestMarketTemplate', postData),
                    image: () => this.$store.dispatch('store/requestMarketImage', postData)
                }

                apiFun[pipeType]().then((res) => {
                    this.cards = isReset ? res.records : this.cards.concat(res.records || [])
                    this.count = res.count || 0
                    this.page++
                    this.loadEnd = res.count <= this.cards.length
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                    this.isLoading = false
                    this.isLoadingMore = false
                })
            },

            chooseOrderType (order) {
                const oldId = this.orderType.id
                this.orderType = order
                this.$parent.filterData.sortType = this.orderType.id
                this.showOrderList = false
                if (oldId !== order.id) this.resetListData()
            },

            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 500 && !this.loadEnd && !this.isLoadingMore) this.getListData()
            },

            addScrollLoadMore () {
                const mainBody = document.querySelector('.store-main')
                if (mainBody) mainBody.addEventListener('scroll', this.scrollLoadMore, { passive: true })
            },

            removeScrollLoadMore () {
                const mainBody = document.querySelector('.store-main')
                if (mainBody) mainBody.removeEventListener('scroll', this.scrollLoadMore, { passive: true })
            },

            handleClear () {
                eventBus.$emit('clear')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .list-cards {
        margin-top: 8px;
        margin-bottom: 20px;
        display: flex;
        flex-wrap: wrap;
        .list-card {
            margin: 0 12px 12px 0;
            &:nth-child(4n) {
                margin-right: 0;
            }
        }
    }

    .list-empty {
        margin-top: 150px;
    }

    .list-type {
        position: relative;
        font-weight: normal;
        text-align: right;
        font-size: 12px;
        line-height: 16px;
        color: $fontLightBlack;
        .list-count {
            float: left;
            color: $fontLightColor;
            font-weight: bold;
            strong {
                color: $fontDarkColor;
            }
        }
        .list-sort {
            color: $fontWeightColor;
        }
        .list-order {
            display: inline-block;
            padding-right: 18px;
            cursor: pointer;
            &:after {
                content: '';
                position: absolute;
                right: 4px;
                top: 3px;
                border-right: 1px solid $fontWeightColor;
                border-bottom: 1px solid $fontWeightColor;
                display: inline-block;
                height: 7px;
                width: 7px;
                transform: rotate(45deg);
                transition: transform 200ms;
                transform-origin: 5.5px 5.5px;
            }
            &.show-type:after {
                transform: rotate(225deg);
            }
        }
        .list-menu {
            text-align: center;
            position: absolute;
            top: 16px;
            right: 0;
            z-index: 999;
            background: $white;
            border: 1px solid $borderWeightColor;
            border-radius: 2px;
            box-shadow: 0 3px 6px rgba(51, 60, 72, 0.12);
            li {
                min-width: 88px;
                line-height: 32px;
                border-bottom: 1px solid $borderWeightColor;
                padding: 0 14px;
                color: $fontWeightColor;
                white-space: nowrap;
                cursor: pointer;
                &:hover {
                    color: $primaryColor;
                }
                &:last-child {
                    border: 0;
                }
            }
        }
    }
</style>
