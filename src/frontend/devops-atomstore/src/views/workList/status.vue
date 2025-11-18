<template>
    <bk-spin
        v-if="status === 'doing'"
        size="mini"
    ></bk-spin>
    <icon
        class="store-status"
        :name="`store-status-${status}`"
        size="14"
        v-else
    />
</template>

<script>
    export default {
        props: {
            status: String
        }
    }
</script>

<style lang="scss" scoped>
    .store-status {
        vertical-align: text-top;
        
        &.bk-spin-loading {
            display: inline-block !important;
            
        }
    }
</style>
