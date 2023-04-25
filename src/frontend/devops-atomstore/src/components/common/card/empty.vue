<template>
    <h3 class="atom-card empty-card">
        <img class="empty-logo" :src="image" />
        <p class="empty-prompt">{{emptyData.str}}
            <router-link class="router-list" :to="{ name: `${emptyData.type}Work` }">{{emptyData.tip}}</router-link>
        </p>
    </h3>
</template>

<script>
    import imgEmpty from '@/images/box.png'

    export default {
        data () {
            return {
                image: imgEmpty
            }
        },

        computed: {
            emptyData () {
                const query = this.$route.query || {}
                const type = query.pipeType || 'atom'
                const emptyMap = {
                    atom: { str: this.$t('store.该分类下暂无流水线插件'), tip: this.$t('store.新增流水线插件') },
                    template: { str: this.$t('store.该分类下暂无流水线模板'), tip: this.$t('store.新增流水线模板') },
                    image: { str: this.$t('store.该分类下暂无容器镜像'), tip: this.$t('store.发布容器镜像') },
                    service: { str: this.$t('store.该分类下暂无微扩展'), tip: this.$t('store.发布微扩展') }
                }
                const tipObj = emptyMap[type]
                return Object.assign({ type }, tipObj)
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';
    .atom-card {
        float: left;
        height: 197px;
        width: 226px;
        background: $white;
        border-radius: 2px;
        margin-top: 12px;
        border:1px solid $lightGray;
        text-align: center;
        font-weight: normal;
        &:hover {
            box-shadow: 0 3px 8px 0 rgba(60, 150, 255, 0.2), 0 0 0 1px rgba(60, 150, 255, 0.08);
        }
        .empty-logo {
            display: inline-block;
            width: 72px;
            height: 42px;
            margin-top: 44px;
        }
        .empty-prompt {
            margin-top: 16px;
            font-size: 12px;
        }
        .router-list {
            display: block;
            margin-top: 8px;
            color: $primaryColor;
        }
    }
</style>
