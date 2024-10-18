<template>
    <bk-switcher
        :value="paramValue[paramKey]"
        @change="changeParamValue"
        v-bind="$props"
    ></bk-switcher>
</template>

<script>
    import mixin from './mixins.js'

    export default {
        mixins: [mixin]
    }
</script>
