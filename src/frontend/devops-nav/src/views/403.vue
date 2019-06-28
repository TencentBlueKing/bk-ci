<template>
    <div class="bk-exception bk-exception-center devops-403">
        <img :src='Img403'>
        <h2 class="exception-text">
            <slot></slot>
        </h2>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import * as Img403 from '../assets/static/403.png'
    
    @Component
    export default class Exception403 extends Vue {
        get Img403 (): string {
            return Img403
        }
    }
</script>

<style lang="scss">
    .devops-403 {
        text-align: center;
        width: 400px;
        margin: auto;
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);

        .exception-text {
            font-size: 20px;
            color: #979797;
            margin: 32px 0;
            font-weight: normal;
        }
    }
</style>
