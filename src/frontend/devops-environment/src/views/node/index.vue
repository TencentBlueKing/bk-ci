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
        NODE_LIST_ASIDE_PANEL_TOGGLE
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
                isCollapsible: false
            }
        },
        computed: {
            initialDivide () {
                return Number(localStorage.getItem(NODE_LIST_ASIDE_WIDTH_CACHE)) || 240
            },
            mainWidth () {
                return this.isCollapsible ? this.containerWidth : this.containerWidth - this.initialDivide
            }
        },
        mounted () {
            if (localStorage.getItem(NODE_LIST_ASIDE_PANEL_TOGGLE) === 'true') {
                this.$refs.resizeLayout.setCollapse(true)
            }
        },
        methods: {
            handleCollapseChange (val) {
                this.isCollapsible = val
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
  height: calc(100% - 48px);
  overflow: hidden;
}
</style>
