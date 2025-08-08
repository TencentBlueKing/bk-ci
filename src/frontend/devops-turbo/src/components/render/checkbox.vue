<template>
    <bk-checkbox-group
        :value="paramValue[paramKey]"
        @change="changeParamValue"
        v-bind="$props"
        class="g-turbo-single-width"
    >
        <bk-checkbox
            :disabled="disabled"
            :value="param.paramValue"
            v-for="(param, index) in paramEnum"
            :key="index"
            class="g-turbo-mr-10"
        >
            {{ param.paramName }}
        </bk-checkbox>
    </bk-checkbox-group>
</template>

<script>
    import mixin from './mixins.js'

    export default {
        mixins: [mixin]
    }
</script>
