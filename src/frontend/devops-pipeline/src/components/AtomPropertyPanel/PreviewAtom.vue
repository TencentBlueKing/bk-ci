<template>
    <NormalAtomV2
        v-if="isCorrectFormat"
        :element="element"
        :atom-props-model="atomPropsModel"
    >
    </NormalAtomV2>
    <section v-else>
        <div class="empty-tips">{{ errorTips }}</div>
    </section>
</template>

<script>
    import NormalAtomV2 from './NormalAtomV2'

    export default {
        components: {
            NormalAtomV2
        },
        props: {
            element: {
                type: Object,
                required: true
            },
            atomPropsModel: {
                type: Object,
                required: true
            },
            errorTips: String
        },
        computed: {
            isCorrectFormat () {
                return this.atomPropsModel && typeof this.atomPropsModel === 'object' && this.atomPropsModel.input
            }
        }
    }
</script>

<style lang="scss">
    .atom-output-var-list {
        > h4,
        > p {
            margin: 0;
        }
        > p {
            line-height: 36px;
        }
    }
</style>
