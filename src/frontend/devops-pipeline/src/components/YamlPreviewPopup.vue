<template>
    <portal to="yaml-preview-popup">
        <transition name="selector-slide">
            <div class="yaml-preview-popup">
                <header>
                    {{ $t('previewYaml') }}
                    <i
                        class="devops-icon icon-close"
                        @click="close"
                    />
                </header>
                <YamlEditor
                    class="preview-yaml-content"
                    :value="yaml"
                    yaml-uri=".task.yml"
                    read-only
                />
            </div>
        </transition>
    </portal>
</template>

<script>
    import YamlEditor from '@/components/YamlEditor'
    export default {
        components: {
            YamlEditor
        },
        props: {
            yaml: {
                type: String,
                default: ''
            }
        },
        methods: {
            close () {
                this.$emit('close')
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
.yaml-preview-popup {
    position: absolute;
    right: 660px;
    width: 600px;
    top: 64px;
    height: calc(100% - 80px);
    background: white;
    z-index: 10000;
    border: 1px solid $borderColor;
    border-radius: 5px;
    padding: 18px;
    display: flex;
    flex-direction: column;
    &:before {
        content: '';
        display: block;
        position: absolute;
        width: 10px;
        height: 10px;
        background: white;
        border: 1px solid $borderColor;
        border-left-color: white;
        border-bottom-color: white;
        transform: rotate(45deg);
        right: -6px;
        top: 136px;
    }
    > header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        flex-shrink: 0;
        margin-bottom: 16px;
        font-size: 14px;
        color: #313238;
    }
    .preview-yaml-content {
        flex: 1;
        overflow: hidden;
    }
}
.selector-slide-enter-active, .selector-slide-leave-active {
    transition: transform .2s linear, opacity .2s cubic-bezier(1, -0.05, .94, .17);
}

.selector-slide-enter {
    -webkit-transform: translate3d(600px, 0, 0);
    transform: translateX(600px);
    opacity: 0;
}

.selector-slide-leave-active {
    display: none;
}
</style>
