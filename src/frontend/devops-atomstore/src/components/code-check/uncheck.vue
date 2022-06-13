<template>
    <section class="code-ckeck-status">
        <i class="bk-icon icon-info-circle status-icon"></i>
        <section class="code-check-summary">
            <h3 class="summary-head">{{ $t('store.未进行过检查') }}</h3>
            <h5 class="summary-desc">{{ $t('store.使用腾讯代码分析进行检查，将从代码缺陷、安全漏洞、代码规范、圈复杂度、重复率等角度进行扫描，为产品质量保驾护航。') }}</h5>
        </section>
        <bk-button theme="primary" class="code-check-button" @click="startCodeCC" :loading="startChecking">{{ $t('store.开始检查') }}</bk-button>
    </section>
</template>

<script>
    export default {
        props: {
            startChecking: Boolean,
            message: String
        },

        methods: {
            startCodeCC () {
                this.$emit('startCodeCC')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .icon-info-circle {
        color: #c4c6cc;
    }
</style>
