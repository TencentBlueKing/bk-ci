<template>
    <bk-transfer
        :target-list="targetList"
        :source-list="list"
        :title="title"
        :empty-content="emptyContent"
        @change="change">
    </bk-transfer>
</template>
<script>
    export default {
        props: {
            visiable: {
                type: Boolean,
                default: true
            },
            list: {
                type: Array,
                default: []
            },
            targetList: {
                type: Array,
                default: []
            }
        },
        data () {
            return {
                title: ['不接收通知', '接收通知'],
                emptyContent: ['无数据', '未选择'],
                selectedList: []
            }
        },
        methods: {
            change (sourceList, targetList, tagetValueList) {
                this.selectedList = targetList
            }
        }
    }
</script>
