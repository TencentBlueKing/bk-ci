<template>
    <transition-group name="list" tag="div" class="soda-stage-list">
        <Stage v-for="(stage, index) in stages"
            :key="index"
            class="list-item"
            :editable="editable"
            :is-preview="isPreview"
            :stage-index="index"
            :stage-length="stages.length"
            :containers="stage.containers">
        </Stage>
    </transition-group>
</template>

<script>
    import Stage from './Stage'
    export default {
        components: {
            Stage
        },
        props: {
            editable: {
                type: Boolean,
                default: true
            },
            isPreview: {
                type: Boolean,
                default: false
            },
            stages: {
                type: Array,
                default: []
            }
        },
        beforeDestroy () {
            window.showLinuxTipYet = false
        }
    }
</script>

<style lang="scss">
    .soda-stage-list {
        display: flex;
        padding-right: 120px;
        width: fit-content;
        position: relative;
    }

    .list-item {
        transition: transform .2s ease-out;
    }

    .list-enter, .list-leave-to
        /* .list-complete-leave-active for below version 2.1.8 */ {
        opacity: 0;
        transform: translateY(36px) scale(0, 1);
    }

    .list-leave-active {
        position: absolute !important;
    }

</style>
