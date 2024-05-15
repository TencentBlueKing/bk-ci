<template>
    <div ref="container" class="devops-container">
        <router-view></router-view>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'

    @Component
    export default class DevopsContainer extends Vue {
    }
</script>

<style lang="scss">
    .devops-container {
        flex: 1;
    }
</style>
