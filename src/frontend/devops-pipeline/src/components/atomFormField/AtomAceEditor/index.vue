<template>
    <section style="pointer-events: all">
        <div :class="['ace-fullscreen', { 'active': isFullScreen }]" alt="全屏" title="全屏"
            @click="setFullScreen">
            <i class="bk-icon" :class="isFullScreen ? &quot;icon-un-full-screen&quot; : &quot;icon-full-screen&quot;"></i>
        </div>
        <ace
            class="ace-wrapper"
            :read-only="disabled"
            :value="value"
            :lang="lang"
            :name="name"
            :full-screen="isFullScreen"
            @input="handleScriptInput"
            :height="height"
            width="100%">
        </ace>
    </section>
</template>

<script>
    import Ace from '@/components/common/ace-editor'
    import atomFieldMixin from '../atomFieldMixin'
    import { getActualTop } from '@/utils/util'

    export default {
        name: 'atom-ace-editor',
        components: {
            Ace
        },
        mixins: [atomFieldMixin],
        props: {
            lang: {
                type: String,
                default: 'sh'
            }
        },
        data () {
            return {
                height: 360,
                isFullScreen: false
            }
        },
        watch: {
            isFullScreen (newVal) {
                const top = getActualTop(this.$el)
                const { clientHeight } = document.body
                if (newVal) {
                    this.height = Math.max(clientHeight - 10, 360)
                } else {
                    this.height = Math.max(clientHeight - top - 180, 360)
                }
            }
        },
        mounted () {
            const top = getActualTop(this.$el)
            const { clientHeight } = document.body
            this.height = Math.max(clientHeight - top - 180, 360)
        },
        methods: {
            handleScriptInput (content) {
                this.handleChange(this.name, content)
            },
            setFullScreen () {
                this.isFullScreen = !this.isFullScreen
            }
        }
    }
</script>

<style lang="scss">
    .ace-fullscreen {
        top: 10px;
        right: 10px;
        position: absolute;
        z-index: 999;
        color: #fff;
        cursor: pointer;
        &.active {
            position: fixed;
            z-index: 10005;
        }
    }
    .ace_editor:fullscreen {
        height: 100% !important;
        font-size: 14px;
    }
</style>
