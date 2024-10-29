<template>
    <div
        class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary store-status"
        v-if="status === 'doing'"
    >
        <div class="rotate rotate1"></div>
        <div class="rotate rotate2"></div>
        <div class="rotate rotate3"></div>
        <div class="rotate rotate4"></div>
        <div class="rotate rotate5"></div>
        <div class="rotate rotate6"></div>
        <div class="rotate rotate7"></div>
        <div class="rotate rotate8"></div>
    </div>
    <icon
        class="store-status"
        :name="`store-status-${status}`"
        size="12"
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
        margin-right: 2px;
        &.bk-spin-loading {
            display: inline-block !important;
            width: 14px;
            height: 14px;
        }
    }
</style>
