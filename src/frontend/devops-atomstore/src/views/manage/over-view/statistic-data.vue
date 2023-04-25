<template>
    <article class="manage-over-view">
        <section class="view-left">
            <section class="total-static manage-section">
                <h5 class="manage-title">{{ $t('store.总体统计') }}</h5>
                <total-static v-bind="componentData"></total-static>
            </section>
            <span class="manage-gap"></span>
            <section class="trend-pic manage-section">
                <h5 class="manage-title">{{ $t('store.趋势图')}}</h5>
                <trend v-bind="componentData"></trend>
            </section>
        </section>

        <section class="view-right">
            <section class="view-code manage-section">
                <h5 class="manage-title">{{ $t('store.代码') }}</h5>
                <code-repo v-bind="componentData"></code-repo>
            </section>
            <span class="manage-gap"></span>
            <section class="view-news manage-section">
                <h5 class="manage-title">{{ $t('store.最新动态') }}</h5>
                <news v-bind="componentData"></news>
            </section>
        </section>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import totalStatic from '@/components/manage/over-view/total-static'
    import news from '@/components/manage/over-view/news'
    import codeRepo from '@/components/manage/over-view/code-repo'
    import trend from '@/components/manage/over-view/trend'

    export default {
        components: {
            totalStatic,
            news,
            codeRepo,
            trend
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail',
                userInfo: 'getUserInfo'
            }),

            componentData () {
                return {
                    userInfo: this.userInfo,
                    detail: this.detail,
                    type: this.$route.params.type
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-over-view {
        height: 100%;
        box-shadow: none !important;
        .manage-gap {
            min-height: 2.3%;
            width: 100%;
            display: block;
        }
        .manage-section {
            padding: 16px 32px;
            background: #fff;
            box-shadow: 1px 2px 3px 0px rgba(0,0,0,0.05);
            .manage-title {
                color: #222;
                font-size: 20px;
                line-height: .28rem;
                font-weight: normal;
            }
        }
        .view-left {
            width: 10.54rem;
            margin-right: .24rem;
            float: left;
            height: 100%;
            .total-static {
                height: 22.4%;
            }
            .trend-pic {
                height: 75.3%;
            }
        }
        .view-right {
            width: 3.81rem;
            display: flex;
            flex-direction: column;
            height: 100%;
            .view-news {
                flex: 1;
                padding: 16px 0;
                overflow: hidden;
                .manage-title {
                    padding: 0 32px;
                }
            }
        }
    }
</style>
