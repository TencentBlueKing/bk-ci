<template>
    <h3 class="atom-card empty-card">
        <img class="empty-logo" :src="image" />
        <p class="empty-prompt">{{emptyData.str}}
            <router-link class="router-list" :to="{ name: 'atomList', params: { type: emptyData.type } }">{{emptyData.tip}}</router-link>
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
                const str = type === 'atom' ? '该分类下暂无流水线插件' : '该分类下暂无流水线模板'
                const tip = type === 'atom' ? '新增流水线插件' : '新增流水线模板'
                return { type, str, tip }
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
