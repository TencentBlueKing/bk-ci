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
    import serviceShow from '@/components/manage/detail/service-detail/show.vue'

    export default {
        components: {
            atomShow,
            imageShow,
            serviceShow
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
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
    }
</style>
