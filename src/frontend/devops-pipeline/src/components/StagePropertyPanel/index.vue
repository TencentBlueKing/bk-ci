<template>
    <bk-sideslider class="bkci-property-panel" :class="{ 'with-variable-open': showVariable }" :z-index="2016" width="640" :is-show.sync="visible" :quick-close="true">
        <header :title="stageTitle" class="stage-panel-header" slot="header">
            {{ stageTitle }}
        </header>
        <stage-content v-bind="$props" slot="content"></stage-content>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import StageContent from './StageContent'
    export default {
        name: 'container-property-panel',
        components: {
            StageContent
        },
        props: {
            stageIndex: Number,
            stage: Object,
            editable: Boolean
        },
        computed: {
            ...mapState('atom', [
                'showVariable',
                'isPropertyPanelVisible'
            ]),
            stageTitle () {
                return typeof this.stage !== 'undefined' ? this.stage.name : 'stage'
            },
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            }
        },
        methods: {
            ...mapActions('atom', [
                'togglePropertyPanel'
            ])
        }
    }
</script>

<style lang="scss">
    @import '../AtomPropertyPanel/propertyPanel';
    .bkci-property-panel {
        font-size: 14px;
        .stage-panel-header {
            @include ellipsis();
            width: 96%;
        }
    }
</style>
