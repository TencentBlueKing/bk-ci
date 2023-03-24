<template>
    <section style="height: 100%">
        <bk-input
            v-model="keyWords"
            v-if="titleType === 'resourceCode'"
            class="search-input"
            behavior="simplicity"
            :placeholder="$t('请输入搜索')"
            @input="handleChangeKeyWords"
        >
        </bk-input>
        <ul
            :class="{
                'bk-scroll-load-list': true,
                'search-input-list': titleType === 'resourceCode'
            }"
            @scroll.passive="handleScroll"
        >
            <template v-if="!isLoading">
                <li
                    v-for="(item, index) in list"
                    :key="index"
                    class="list-item"
                    :title="item.name"
                >
                    <slot :data="item"></slot>
                </li>
            </template>
            <div v-else class="loading-panel">{{ $t('正在加载中...') }}</div>
            <div v-if="!isLoading && !list.length" class="loading-panel">{{ $t('查询无数据') }}</div>
        </ul>
    </section>
</template>

<script>
    export default {
        props: {
            list: Array,
            hasLoadEnd: Boolean,
            getDataMethod: Function,
            resourceType: String,
            showMenu: Boolean,
            titleType: String
        },

        data () {
            return {
                page: 1,
                pageSize: 6,
                isLoadingMore: false,
                isLoading: false,
                keyWords: ''
            }
        },

        watch: {
            resourceType (val) {
                this.keyWords = ''
                this.page = 1
                this.getListData()
            }
        },

        created () {
            this.getListData()
        },

        methods: {
            handleScroll (event) {
                const target = event.target
                const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
                if (bottomDis <= 300 && !this.hasLoadEnd && !this.isLoadingMore) this.getListData()
            },

            getListData () {
                this.isLoadingMore = true
                if (this.page === 1) {
                    this.isLoading = true
                }

                this
                    .getDataMethod(this.page, this.pageSize, this.keyWords)
                    .then(() => {
                        this.page += 1
                    })
                    .finally(() => {
                        this.isLoadingMore = false
                        this.isLoading = false
                    })
            },

            resetList () {
                this.page = 1
                this.getListData()
            },

            handleChangeKeyWords () {
                this.$emit('change')
                this.page = 1
                if (!this.isLoadingMore) {
                    this.getListData()
                }
            }
        }
    }
</script>

<style scoped lang="postcss">
    .bk-scroll-load-list {
        padding: 6px 0;
        max-height: 180px;
        height: 100%;
        overflow: auto;
        width: 160px !important;
        &::-webkit-scrollbar {
        width: 4px;
        height: 4px;
        &-thumb {
          border-radius: 20px;
          background: #a5a5a5;
          box-shadow: inset 0 0 6px hsla(0, 0%, 80%, .3);
        }
      }
    }
    .search-input-list {
        max-height: 148px;
        height: calc(100% - 32px);
    }
    .search-input {
        width: 160px;
    }
    .list-item {
        height: 32px;
        color: #63656e;
        padding: 0 15px !important;
        cursor: pointer;
        font-size: 12px;
        overflow: hidden;
        white-space: nowrap;
        display: inherit;
        text-overflow: ellipsis;
        line-height: 30px;
        &:hover,
        &.is-hover {
            background: #f5f7fa;
        }
    }
</style>
