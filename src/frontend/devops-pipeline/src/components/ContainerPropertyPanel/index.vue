<template>
    <bk-sideslider class="bkci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="container-panel-header" slot="header">
            {{ title }}
        </header>
        <container-content v-bind="$props" slot="content" ref="container"></container-content>
    </bk-sideslider>
</template>

<script>
    import { mapActions, mapState } from 'vuex'
    import ContainerContent from './ContainerContent'

    export default {
        name: 'container-property-panel',
        components: {
            ContainerContent
        },
        props: {
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            title: String
        },

        computed: {
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
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
    .container-panel-header {
        display: flex;
        margin-right: 20px;
        justify-content: space-between;
    }
</style>
