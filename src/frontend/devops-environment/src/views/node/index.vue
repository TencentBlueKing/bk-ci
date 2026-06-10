<template>
    <bk-resize-layout
        ref="resizeLayout"
        :initial-divide="initialDivide"
        collapsible
        :border="false"
        :min="240"
        :max="360"
        ext-cls="resize"
        @collapse-change="handleCollapseChange"
        @after-resize="afterResize"
    >
        <GroupAside slot="aside" />
        <router-view
            slot="main"
            :style="{ width: `${mainWidth}px` }"
            :main-width="mainWidth"
        ></router-view>
    </bk-resize-layout>
</template>

<script>
    import {
        NODE_LIST_ASIDE_WIDTH_CACHE,
        NODE_LIST_ASIDE_PANEL_TOGGLE,
        ENV_ACTIVE_NODE_TYPE
    } from '@/store/constants'
    import GroupAside from './group_aside.vue'

    export default {
        components: {
            GroupAside
        },
        props: {
            containerWidth: Number
        },
        data (){
            return {
                currentAsideWidth: 240,
                isCollapsible: false
            }
        },
        computed: {
            initialDivide () {
                return Number(localStorage.getItem(NODE_LIST_ASIDE_WIDTH_CACHE)) || 240
            },
            mainWidth () {
                return this.isCollapsible ? this.containerWidth : this.containerWidth - this.currentAsideWidth
            }
        },
        mounted () {
            this.currentAsideWidth = this.initialDivide
            if (localStorage.getItem(NODE_LIST_ASIDE_PANEL_TOGGLE) === 'true') {
                this.$refs.resizeLayout.setCollapse(true)
            }
        },
        beforeDestroy () {
            localStorage.removeItem(ENV_ACTIVE_NODE_TYPE)
        },
        methods: {
            handleCollapseChange (val) {
                this.isCollapsible = val
                localStorage.setItem(NODE_LIST_ASIDE_PANEL_TOGGLE, JSON.stringify(val))
            },
            afterResize (width) {
                this.currentAsideWidth = width
                localStorage.setItem(NODE_LIST_ASIDE_WIDTH_CACHE, JSON.stringify(width))
            }
        }
    }
</script>

<style lang="scss" scoped>
.resize {
  height: calc(100% - 48px);
  overflow: hidden;
}
</style>
