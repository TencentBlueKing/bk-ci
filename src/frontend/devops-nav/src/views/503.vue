<template>
    <div class="bk-exception bk-exception-center devops-503">
        <img :src="Img503">
        <h2 class="exception-text">
            <slot>服务维护中，请稍候...</slot>
        </h2>
    </div>
</template>

<script lang="ts">
    import Vue from 'vue'
    import { Component } from 'vue-property-decorator'
    import * as Img503 from '../assets/static/503.png'
    
    @Component
    export default class Exception503 extends Vue {
        get Img503 (): string {
            return Img503
        }
    }
</script>

<style lang="scss">
    .devops-503 {
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
