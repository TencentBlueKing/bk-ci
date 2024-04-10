<template>
    <section class="main-body">
        <section class="progress-approve">
            <img src="../../../images/progressApprove.svg" class="approve-image">
            <span class="approve-tip"> {{ $t('store.微扩展正在审核中，请耐心等待。紧急上架可企业微信私聊DevOps-helper咨询审核进度') }} </span>
        </section>
    </section>
</template>

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
