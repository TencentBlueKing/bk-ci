<template>
    <section>
        <p class="code-check-tip"><i class="bk-icon icon-info-circle"></i>{{ $t('store.代码检查任务执行失败，请重新检查，或联系CodeCC助手') }}</p>
        <section class="code-ckeck-status">
            <i class="bk-icon icon-exclamation-circle status-icon"></i>
            <section class="code-check-summary">
                <h3 class="summary-head">{{ $t('store.代码检查出现异常') }}</h3>
                <h5 class="summary-desc">{{ message }}</h5>
            </section>
            <bk-button theme="primary" class="code-check-button" @click="startCodeCC" :loading="startChecking">{{ $t('store.重新检查') }}</bk-button>
            <bk-button class="code-check-button" :disabled="startChecking" @click="goToCodecc" v-if="codeccUrl">{{ $t('store.查看详情') }}</bk-button>
        </section>
    </section>
</template>

<script>
    export default {
        props: {
            codeccUrl: String,
            message: String,
            startChecking: Boolean
        },

        methods: {
            startCodeCC () {
                this.$emit('startCodeCC')
            },

            goToCodecc () {
                window.open(this.codeccUrl, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .icon-exclamation-circle {
        color: #ea3636;
    }
    .code-check-button {
        margin-left: 10px;
    }
</style>
