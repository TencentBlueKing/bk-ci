<template>
    <bk-resize-layout
        ref="resizeLayout"
        :initial-divide="initialDivide"
        collapsible
        :border="false"
        :min="240"
        :max="360"
        class="resize"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <GroupAside slot="aside" />
        <div slot="main">
            <router-view></router-view>
        </div>
    </bk-resize-layout>
</template>

<script>
    import {
        NODE_LIST_ASIDE_WIDTH_CACHE,
        NODE_LIST_ASIDE_PANEL_TOGGLE
    } from '@/store/constants'
    import GroupAside from './group_aside.vue'

    export default {
        components: {
            GroupAside
        },
        computed: {
            initialDivide () {
                return Number(localStorage.getItem(NODE_LIST_ASIDE_WIDTH_CACHE)) || 240
            }
        },
        mounted () {
            if (localStorage.getItem(NODE_LIST_ASIDE_PANEL_TOGGLE) === 'true') {
                this.$refs.resizeLayout.setCollapse(true)
            }
        },
        methods: {
            handleCollapseChange (val) {
                localStorage.setItem(NODE_LIST_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
            },
            afterResize (width) {
                localStorage.setItem(NODE_LIST_ASIDE_WIDTH_CACHE, JSON.stringify(width))
            }
        }
    }
</script>

<style lang="scss" scoped>
.resize {
  width: 100vw;
  height: calc(100vh - 110px);
}
</style>
