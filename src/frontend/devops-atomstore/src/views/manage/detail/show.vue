<template>
    <article class="manage-detail">
        <header class="manage-detail-header">
            <router-link :to="detail.editFlag ? { name: 'edit' } : ''"
                :class="{ disable: !detail.editFlag }"
                :title="!detail.editFlag && $t('store.只有处于审核驳回、已发布、上架中止和已下架的状态才允许修改基本信息')"
            >{{ $t('store.编辑') }}</router-link>
        </header>

        <main class="detail-main">
            <component :is="`${$route.params.type}Show`"
                class="detail-show"
                :detail="detail"
            ></component>
        </main>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomShow from '@/components/manage/detail/atom-detail/show.vue'
    import imageShow from '@/components/manage/detail/image-detail/show.vue'

    export default {
        components: {
            atomShow,
            imageShow
        },

        computed: {
            ...mapGetters('store', {
                'detail': 'getDetail'
            })
        }
    }
</script>

<style lang="scss" scoped>
    .manage-detail {
        background: #fff;
        .manage-detail-header {
            position: absolute;
            right: 32px;
            top: calc(-26px - 3.2vh);
            a {
                cursor: pointer;
                color: #1592ff;
            }
            .disable {
                cursor: not-allowed;
                color: #999;
            }
        }
        .detail-main {
            height: 100%;
        }
        .detail-show {
            padding: 3.2vh;
            height: 100%;
            overflow-y: auto;
        }
        /deep/ .show-detail {
            display: flex;
            align-items: flex-start;
            position: relative;
            .detail-img {
                width: 100px;
                height: 100px;
                margin-right: 32px;
            }
            .detail-items {
                flex: 1;
                max-width: calc(100% - 132px);
                overflow-x: hidden;
            }
            .detail-item {
                font-size: 14px;
                line-height: 18px;
                display: flex;
                align-items: flex-start;
                &:not(:nth-child(1)) {
                    margin-top: 18px;
                }
            }
            .detail-label {
                color: #999;
                min-width: 100px;
            }
            .item-name {
                font-size: 20px;
                line-height: 24px;
            }
            .overflow {
                max-height: 290px;
                overflow: hidden;
            }
            .summary-all {
                cursor: pointer;
                color: #1592ff;
                font-size: 14px;
                line-height: 20px;
                display: block;
                text-align: center;
                position: absolute;
                bottom: -22px;
                left: 50%;
                transform: translateX(-50%);
                &::before {
                    content: '';
                    position: absolute;
                    top: 4px;
                    left: calc(50% - 50px);
                    width: 6px;
                    height: 6px;
                    display: block;
                    transform: rotate(-45deg);
                    border-left: 2px solid #1592ff;
                    border-bottom: 2px solid #1592ff;
                }
            }
        }
    }
</style>
