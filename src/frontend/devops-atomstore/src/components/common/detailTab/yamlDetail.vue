<template>
    <section>
        <codeSection v-bind="$props"></codeSection>
    </section>
</template>

<script>
    import codeSection from './codeSection'

    export default {
        components: {
            codeSection
        },

        props: {
            readOnly: {
                type: Boolean,
                default: true
            },
            limitHeight: {
                type: Boolean,
                default: true
            },
            cursorBlinkRate: {
                type: Number,
                default: 0
            },
            code: {
                type: String,
                require: true
            },
            name: String,
            currentTab: String,
            getDataFunc: {
                type: Function,
                default: null
            }
        }
    }
</script>

<style lang="scss" scoped>
    .yaml-title {
        margin: 20px 0 10px;
        line-height: 23px;
        height: 23px;
        color: #222222;
        font-size: 14px;
        font-weight: 500;
    }
</style>
