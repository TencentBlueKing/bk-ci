<template>
    <div ref='container' class="devops-container">
        <router-view></router-view>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import None from '../../views/None.vue'
    @Component({
        components: {
            None
        }
    })
    export default DevopsContainer extend Vue {   
    }
</script>


<style lang="scss">
    .devops-container {
        flex: 1;
    }
</style>
