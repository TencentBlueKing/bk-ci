<template>
    <div class="ci-paging clearfix">
        <bk-pagination
            :size="size"
            :current.sync="pagingConfig.curPage"
            :count="pageCountConfig.totalCount"
            :limit="pageCountConfig.perPageCountSelected"
            :align="align"
            :show-limit="showLimit"
            @change="pageChangeHandler"
            @limit-change="itemSelectedHandler">
        </bk-pagination>
    </div>
</template>

<script>
    export default {
        props: {
            size: {
                type: String,
                default: 'small'
            },
            align: {
                type: String,
                default: 'right'
            },
            showLimit: {
                type: Boolean,
                default: true
            },
            pageCountConfig: {
                type: Object,
                default () {
                    return {
                        list: [],
                        perPageCountSelected: 0,
                        totalCount: 0
                    }
                }
            },
            pagingConfig: {
                type: Object,
                default () {
                    return {
                        totalPage: 1,
                        curPage: 1
                    }
                }
            }
        },
        mounted () {
            const {
                pageCountConfig
            } = this

            this.$emit('update:pagingConfig', {
                totalPage: Math.ceil(pageCountConfig.totalCount / pageCountConfig.perPageCountSelected),
                curPage: 1
            })
        },
        methods: {
            /**
             *  每页条数下拉框改变值之后的回调函数
             */
            itemSelectedHandler (perPageCount) {
                const {
                    pageCountConfig
                } = this

                this.$emit('update:pageCountConfig', {
                    perPageCountSelected: perPageCount,
                    totalCount: pageCountConfig.totalCount
                })
                
                this.$nextTick(() => {
                    this.$emit('page-count-changed', perPageCount, this.changed())
                })
            },
            /**
             *  当前页码改变后的回调函数
             */
            pageChangeHandler (page) {
                this.$emit('page-changed', page, this.changed())
            },
            /**
             *  当前页码变化或每页显示数量变化后，列表需要显示的起止值
             */
            changed () {
                const {
                    pagingConfig: {
                        curPage
                    },
                    pageCountConfig: {
                        perPageCountSelected
                    }
                } = this
                const start = (curPage - 1) * perPageCountSelected
                const end = start + perPageCountSelected

                return {
                    start,
                    end
                }
            }
        }
    }
</script>
