<template>
    <section class="main-body">
        <section class="progress-end">
            <img src="../../../images/progressSuccess.svg" class="end-image">
            <span class="end-tip"> {{ $t('store.恭喜，成功发布到商店!') }} </span>
            <p class="end-buttons">
                <bk-button @click="toWorkList">{{ $t('store.工作台') }}</bk-button>
                <bk-button @click="toStoreHome">{{ $t('store.研发商店') }}</bk-button>
            </p>
        </section>
    </section>
</template>

<script>
    export default {
        methods: {
            toWorkList () {
                this.$router.push({
                    name: 'serviceWork'
                })
            },

            toStoreHome () {
                this.$router.push({
                    name: 'atomHome',
                    query: {
                        pipeType: 'service'
                    }
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .progress-end {
        display: flex;
        justify-content: center;
        align-items: center;
        flex-direction: column;
        height: 100%;
        .end-image {
            width: 236px;
            height: 187px;
            margin-bottom: 16px;
        }
        .end-tip {
            color: $grayBlack;
        }
        .end-buttons {
            margin-top: 6px;
            button {
                width: 88px;
            }
        }
    }
</style>
