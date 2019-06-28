<template>
    <article v-bkloading="{ isLoading, opacity: 1 }">
        <h3 class="list-type" v-clickoutside="closeOrderList">
            <span class="list-sort">排序：</span>
            <span :class="[{ 'show-type': showOrderList }, 'list-order']" @click="showOrderList = !showOrderList">{{ orderType.name }}</span>
            <ul class="list-menu" v-show="showOrderList">
                <li v-for="(order, index) in orderList" :key="index" @click="chooseOrderType(order)">{{ order.name }}</li>
            </ul>
        </h3>

        <hgroup class="list-cards">
            <card v-for="(card, index) in cards" :key="card.atomCode + index" :atom="card" :has-summary="true" class="list-card"></card>
        </hgroup>
        <p class="g-empty list-empty" v-if="cards.length <= 0">没找到相关结果</p>
    </article>
</template>

<script>
    import card from '@/components/common/card'
    import clickoutside from '@/directives/clickoutside'

    export default {
        components: {
            card
        },

        directives: {
            clickoutside
        },

        data () {
            return {
                pageSize: 40,
                page: 1,
                isLoadingMore: false,
                loadEnd: false,
                isLoading: true,
                cards: [],
                orderType: { id: 'NAME', name: '按名称A-Z' },
                showOrderList: false,
                orderList: [
                    { id: 'NAME', name: '按名称A-Z' },
                    { id: 'CREATE_TIME', name: '按创建时间' },
                    { id: 'UPDATE_TIME', name: '按修改时间' },
                    { id: 'PUBLISHER', name: '按发布者' },
                    { id: 'DOWNLOAD_COUNT', name: '按热度' }
                ]
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
                const orderType = this.$route.query.sortType || 'NAME'
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
                const { searchStr, classifyKey, classifyValue, score, rdType, pipeType } = this.$route.query || {}

                const postData = {
                    sortType: this.orderType.id,
                    rdType,
                    score,
                    page: this.page,
                    pageSize: this.pageSize
                }
                if (classifyValue !== 'all') postData[classifyKey] = classifyValue

                const apiFun = { atom: () => this.getAtomList(postData, searchStr), template: () => this.getTemplateList(postData, searchStr) }

                apiFun[pipeType]().then((res) => {
                    this.cards = isReset ? res.records : this.cards.concat(res.records || [])
                    this.page++
                    this.loadEnd = res.count < this.pageSize
                }).catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' })).finally(() => {
                    this.isLoading = false
                    this.isLoadingMore = false
                })
            },

            getTemplateList (postData, searchStr) {
                postData.templateName = searchStr
                return this.$store.dispatch('store/requestMarketTemplate', postData)
            },

            getAtomList (postData, searchStr) {
                postData.atomName = searchStr
                return this.$store.dispatch('store/requestMarketAtom', postData)
            },

            chooseOrderType (order) {
                const oldId = this.orderType.id
                this.orderType = order
                this.showOrderList = false
                if (oldId !== order.id) this.resetListData()
            },

            scrollLoadMore (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 400 && !this.loadEnd && !this.isLoadingMore) this.getListData()
            },

            addScrollLoadMore () {
                const mainBody = document.querySelector('.store-main')
                if (mainBody) mainBody.addEventListener('scroll', this.scrollLoadMore, { passive: true })
            },

            removeScrollLoadMore () {
                const mainBody = document.querySelector('.store-main')
                if (mainBody) mainBody.removeEventListener('scroll', this.scrollLoadMore, { passive: true })
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
