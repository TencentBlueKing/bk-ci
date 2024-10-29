<template>
    <div class="pipeline-build-version">
        <template v-for="(item, index) in versionInputs">
            <bk-input
                :key="index"
                :disabled="disabled"
                :value="item.value"
                @change="item.handler"
            />
            <span class="version-point-linker" :key="index" v-if="index < versionInputs.length - 1">.</span>
        </template>
    </div>
</template>

<script>
    import { allVersionKeyList } from '@/utils/pipelineConst'

    export default {
        props: {
            disabled: Boolean,
            value: {
                type: Object,
                default: () => ({})
            }
        },
        computed: {
            versionInputs () {
                return allVersionKeyList.map(item => ({
                    value: this.value[item],
                    name: item,
                    handler: (value) => {
                        this.updateVersion(item, value)
                    }
                }))
            }
        },
        methods: {
            updateVersion (name, value) {
                const version = {
                    ...this.value,
                    [name]: value
                }
                this.$emit('input', version)
                this.$emit('update:value', version)
            }
        }
    }
</script>
<style lang="scss">
    .pipeline-build-version {
        display: flex;
        align-items: center;
        .version-point-linker {
            margin: 0 6px;
        }
    }
</style>
