<template>
    <div class="code-repo-recycle">
        <app-exception :type="'maintaining'"></app-exception>
    </div>
</template>

<script>
    import appException from '@/components/common/exception'

    export default {
        components: {
            'app-exception': appException
        }
    }
</script>

<style lang="scss" scoped>
    .code-repo-recycle {
        flex: 1;
        display: flex;
        align-items: center;
        justify-content: center;
    }
</style>
