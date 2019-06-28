<template>
    <div ref='container' class="devops-container">
        <router-view></router-view>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import None from '../../views/None.vue'
    export default Vue.extend({
        name: 'devops-container',
        components: {
            None
        }
    })
</script>


<style lang="scss">
    .devops-container {
        flex: 1;
    }
</style>
