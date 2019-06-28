<template>
    <div class="ci-paging clearfix">
        <div class="fl clearfix paging-settings">
            <div class="fl paging-total-count">共计{{ pageCountConfig.totalCount }}条</div>
            <div class="fl paging-per-page">
                <bk-dropdown
                    :list="pageCountConfig.list"
                    :selected.sync="pageCountConfig.perPageCountSelected"
                    @item-selected="itemSelectedHandler">
                </bk-dropdown>
            </div>
        </div>
        <div class="fr paging-controller">
            <bk-paging
                :total-page="pagingConfig.totalPage"
                :cur-page.sync="pagingConfig.curPage"
                @page-change="pageChangeHandler">
            </bk-paging>
        </div>
    </div>
</template>

<script>
    export default {
        props: {
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
        methods: {
            /**
             *  每页条数下拉框改变值之后的回调函数
             */
            itemSelectedHandler (id, obj) {
                let {
                    pageCountConfig
                } = this

                this.$emit('update:pagingConfig', {
                    totalPage: Math.ceil(pageCountConfig.totalCount / pageCountConfig.perPageCountSelected),
                    curPage: 1
                })

                this.$nextTick(() => {
                    this.$emit('page-count-changed', id, obj, this.changed())
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
                let {
                    pagingConfig,
                    pagingConfig: {
                        curPage,
                        perPageCountList
                    },
                    pageCountConfig: {
                        perPageCountSelected
                    }
                } = this
                let start = (curPage - 1) * perPageCountSelected
                let end = start + perPageCountSelected

                return {
                    start,
                    end
                }
            }
        },
        watch: {
            'pageCountConfig.totalCount' (val) {
                this.$emit('update:pagingConfig', {
                    totalPage: Math.ceil(val / this.pageCountConfig.perPageCountSelected)
                })
            }
        },
        mounted () {
            let {
                pageCountConfig
            } = this

            this.$emit('update:pagingConfig', {
                totalPage: Math.ceil(pageCountConfig.totalCount / pageCountConfig.perPageCountSelected),
                curPage: 1
            })
        }
    }
</script>
