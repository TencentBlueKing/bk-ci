<template>
    <bk-time-picker
        :value="paramValue[paramKey]"
        @change="changeParamValue"
        v-bind="$props"
        class="g-turbo-single-width"
    ></bk-time-picker>
</template>

<script>
    import mixin from './mixins.js'

    export default {
        mixins: [mixin]
    }
</script>
