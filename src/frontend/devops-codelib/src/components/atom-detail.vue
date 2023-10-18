<template>
    <bk-sideslider
        :is-show.sync="isShow"
        :quick-close="true"
        width="640"
        :title="$t('codelib.触发器详情')"
    >
        <div slot="content">
            123123
        </div>
    </bk-sideslider>
</template>

<script>
    export default {
        props: {
            atom: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                isShow: false
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .bk-sideslider-content {
        padding: 20px;
        height: calc(100vh - 60px);
    }
</style>
