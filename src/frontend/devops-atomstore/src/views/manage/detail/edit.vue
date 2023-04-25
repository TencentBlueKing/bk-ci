<template>
    <article class="manage-detail">
        <header class="manage-detail-header">
            <span @click="$router.back()">{{ $t('store.取消编辑') }}</span>
        </header>
        <component :is="`${$route.params.type}Edit`" :user-info="userInfo" :detail="detail" class="edit-main" ref="edit"></component>
    </article>
</template>

<script>
    import { mapGetters } from 'vuex'
    import atomEdit from '@/components/manage/detail/atom-detail/edit.vue'
    import imageEdit from '@/components/manage/detail/image-detail/edit.vue'
    import serviceEdit from '@/components/manage/detail/service-detail/edit.vue'

    export default {
        components: {
            atomEdit,
            imageEdit,
            serviceEdit
        },

        computed: {
            ...mapGetters('store', {
                detail: 'getDetail',
                userInfo: 'getUserInfo'
            })
        },

        created () {
            window.onbeforeunload = () => {
                const vm = this.$refs.edit
                if (vm.hasChange) return this.$t('store.有修改的数据未保存，是否离开当前页面')
            }
        },

        destroyed () {
            window.onbeforeunload = null
        },

        beforeRouteLeave (to, from, next) {
            const vm = this.$refs.edit
            if (vm.hasChange) {
                this.$bkInfo({
                    title: this.$t('store.确定离开？'),
                    subTitle: this.$t('store.有修改的数据未保存，是否离开当前页面'),
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
            .radio-group {
                .bk-form-radio {
                    margin-right: 10px;
                }
            }
        }
    }
</style>
