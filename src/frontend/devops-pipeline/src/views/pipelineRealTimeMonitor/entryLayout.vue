<template>
    <div class="metrics-wrapper">
        <div class="metrics-sidebar">
            <layout-aside />
        </div>
        <overView class="metrics-content"></overView>
        <!-- <router-view class="metrics-content"></router-view> -->
    </div>
</template>

<script>
    import layoutAside from './layoutAside.vue'
    import overView from './index.vue'
    import { defineComponent } from 'vue'
    export default defineComponent({
        name: 'entryLayout',
        components: {
            layoutAside,
            overView
        }
    })
</script>

<style lang="scss" scoped>
.metrics-wrapper{
    display: flex;
    flex-direction: row;
    height: 100%;
    width: 100%;
    
    .metrics-sidebar{
        width: 240px;
        flex-shrink: 0;
        height: 100%;
        background: #FFFFFF;
        box-shadow: 1px 0 0 0 #DCDEE5;
        overflow-y: auto;
    }
    
    .metrics-content{
        flex: 1;
        height: 100%;
        overflow-y: auto;
        min-width: 0;
    }
}
</style>