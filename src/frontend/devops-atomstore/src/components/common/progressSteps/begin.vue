<template>
    <section class="main-body">
        <section class="progress-approve">
            <img src="../../../images/progressApprove.svg" class="approve-image">
            <span class="approve-tip"> {{ $t('store.正在初始化，请耐心等候') }} </span>
        </section>

        <footer class="main-footer">
            <bk-button theme="primary" :disabled="currentStep.status !== 'success'" @click="nextStep"> {{ $t('store.下一步') }} </bk-button>
        </footer>
    </section>
</template>

<script>
    export default {
        props: {
            currentStep: {
                type: Object
            }
        },

        methods: {
            nextStep () {
                const parent = this.$parent || {}
                parent.currentStepIndex++
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .progress-approve {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        height: 100%;
        .approve-image {
            width: 236px;
            height: 187px;
            margin-bottom: 16px;
        }
        .approve-tip {
            color: $grayBlack;
        }
    }
</style>
