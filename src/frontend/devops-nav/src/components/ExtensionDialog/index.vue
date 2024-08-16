<template>
    <bk-dialog
        v-model="show"
        theme="primary"
        :mask-close="false"
        header-position="left"
        :title="title"
        :close-icon="false"
        :show-footer="false"
        v-bind="options"
    >
        <div v-bkloading="{ isLoading: !loaded }">
            <iframe v-if="show" @load="onload" ref="extensionIframe" class="extention-dialog-content-iframe" :src="src" />
        </div>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import eventBus from '@/utils/eventBus'
    import { Component, Watch } from 'vue-property-decorator'
    @Component
    export default class ExtensionDialog extends Vue {
        src: string = ''
        show: boolean = false
        options: object | null = null
        title: string = 'Title'
        customData: object | null = null
        loaded: boolean = false

        updateProps (props) {
            Object.keys(props).forEach(prop => {
              this[prop] = props[prop]
            })
        }

        @Watch('customData')
        handleCustomDataChange (data) {
            this.syncData(data)
        }

        onload () {
            this.syncData(this.customData)
            this.loaded = true
        }

        mounted () {
            eventBus.$on('update-extension-dialog', this.updateProps)
        }
        
        beforeDestroy () {
            eventBus.$off('update-extension-dialog', this.updateProps)
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
    .extention-dialog-content-iframe {
        display: flex;
        width: 100%;
        overflow: auto;
        border: 0;
    }
</style>
