<template>
    <bk-sideslider :is-show.sync="show" :quick-close="quickClose" v-bind="asidePanelOption">
        <div slot="header">{{ header }}</div>
        <div slot="content" class="extention-aside-panel-content" v-bkloading="{ isLoading: !loaded }">
            <iframe v-if="show" @load="onload" ref="extensionIframe" class="extention-aside-panel-content-iframe" :src="src" />
        </div>
    </bk-sideslider>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import eventBus from '@/utils/eventBus'
    import { Component, Watch } from 'vue-property-decorator'
    @Component
    export default class ExtensionAdiePanel extends Vue {
        src: string = ''
        show: boolean = false
        options: object | null = null
        header: string = 'Title'
        quickClose: boolean = false
        customData: object | null = null
        loaded: boolean = false

        get asidePanelOption (): object {
            const { options = {} } = this
            return {
                width: 680,
                ...options
            }
        }

        @Watch('customData')
        handleCustomDataChange (data) {
            this.syncData(data)
        }

        mounted () {
            eventBus.$on('update-extension-aside-panel', this.updateProps)
        }
        
        beforeDestroy () {
            eventBus.$off('update-extension-aside-panel', this.updateProps)
        }

        updateProps (props) {
            Object.keys(props).forEach(prop => {
              this[prop] = props[prop]
            })
        }

        onload () {
            this.loaded = true
            this.syncData(this.customData)
        }

        syncData (data) {
            try {
                // @ts-ignore
                this.$refs.extensionIframe.contentWindow.postMessage({
                    action: 'syncCustomData',
                    params: JSON.stringify(data)
                }, '*')
            } catch (e) {
                console.warn('can not find extensionIframe')
            }
        }
    }
</script>

<style lang="scss">
    .extention-aside-panel-content {
        display: flex;
        height: calc(100vh - 60px);
        &-iframe {
            width: 100%;
            flex: 1;
            overflow: auto;
            border: 0;
        }
    }
</style>
