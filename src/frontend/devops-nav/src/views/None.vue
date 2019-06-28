<template>
	<div class="exception-box">
		<img src="https://magicbox.bk.tencent.com/static_api/v3/components/exception_building/images/building.png" alt="没找到页面！" />
		<p>没找到页面！<a href='/console/'>返回蓝盾Devops首页</a></p>
	</div>
</template>

<script lang="ts">
	import Vue from 'vue'
	import { Component } from 'vue-property-decorator'
	
	export default class None extends Vue {
	}
</script>

<style lang="scss">
	.exception-box {
		text-align: center;
		margin: auto;
		img {
			width: 300px;
		}
	}
</style>
