<template>
    <article class="manage-detail">
        <header class="manage-detail-header">
            <span @click="$router.back()">{{ $t('store.取消编辑') }}</span>
        </header>
        <component :is="`${$route.params.type}Edit`" :detail="detail" class="edit-main" ref="edit"></component>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomEdit from '@/components/manage/detail/atom-detail/edit.vue'
    import imageEdit from '@/components/manage/detail/image-detail/edit.vue'

    export default {
        components: {
            atomEdit,
            imageEdit
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail'
            })
        },

        created () {
            window.onbeforeunload = () => {
                const vm = this.$refs.edit
                if (vm.hasChange) return this.$t('store.离开将会导致未保存信息丢失')
            }
        },

        destroyed () {
            window.onbeforeunload = null
        },

        beforeRouteLeave (to, from, next) {
            const vm = this.$refs.edit
            if (vm.hasChange) {
                this.$bkInfo({
                    title: this.$t('store.确认离开当前页？'),
                    subTitle: this.$t('store.离开将会导致未保存信息丢失'),
                    confirmFn: () => next(),
                    onClose: () => next(false)
                })
            } else {
                next()
            }
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
            span {
                cursor: pointer;
                color: #1592ff;
            }
            .disable {
                cursor: not-allowed;
                color: #999;
            }
        }
        .edit-main {
            overflow-y: auto;
            padding: 3.2vh;
            height: 100%;
            position: relative;
        }
        ::v-deep .manage-detail-edit {
            margin-right: 136px;
            .remark-input {
                min-width: 100%;
                height: 263px;
                border: 1px solid #c4c6cc;
                &.fullscreen {
                    height: auto;
                }
            }
            .edit-logo {
                position: absolute;
                right: 32px;
                top: 32px;
                margin-top: 0;
                .bk-label {
                    display: none;
                }
            }
        }
    }
</style>
