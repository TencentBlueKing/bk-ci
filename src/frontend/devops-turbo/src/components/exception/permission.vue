<template>
    <bk-exception
        class="exception-wrap-item"
        type="403"
    >
        <span>{{ message }}</span>
    </bk-exception>
</template>

<script>
    export default {
        props: {
            message: String
        }
    }
</script>

<style lang="scss" scoped>
    .exception-wrap-item {
        margin-top: calc(50vh - 300px);
        ::v-deep .bk-exception-text {
            margin-top: -30px;
            font-size: 20px;
        }
    }
</style>
