<template>
    <ul
        class="bk-scroll-load-list"
        @scroll.passive="handleScroll"
    >
        <li v-for="(item, index) in list" :key="index">
            <slot :data="item"></slot>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            list: Array,
            hasLoadEnd: Boolean,
            getDataMethod: Function,
            page: Number
        },

        data () {
            return {
                pageSize: 100,
                isLoadingMore: false
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
                this
                    .getDataMethod(this.pageSize)
                    .finally(() => {
                        this.isLoadingMore = false
                    })
            }
        }
    }
</script>

<style scoped>
    .bk-scroll-load-list {
        height: 100%;
        overflow: auto;
    }
</style>
