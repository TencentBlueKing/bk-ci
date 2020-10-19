<template>
    <ul class="label-content">
        <li v-for="(label, index) in labelList" :key="index" class="label-item">{{ label | formatter }}</li>
        <li class="empty-item" v-if="labelList.length <= 0">--</li>
    </ul>
</template>

<script>
    export default {
        props: {
            labelList: Array,
            formatter: {
                type: Function,
                default: (val) => val
            }
        }
    }
</script>

<style lang="scss" scoped>
    .label-content {
        width: 100%;
        margin-top: -6px;
        &::after {
            content: '';
            display: table;
            clear: both;
        }
        .label-item {
            float: left;
            background: #ffffff;
            border: 1px solid #dcdee5;
            border-radius: 9px;
            font-size: 12px;
            line-height: 18px;
            padding: 0 12px;
            margin-right: 8px;
            margin-top: 6px;
        }
        .empty-item {
            float: left;
            margin-top: 6px;
        }
    }
</style>
