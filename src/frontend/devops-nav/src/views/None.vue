<template>
	<div class="exception-box">
		<img src="https://magicbox.bk.tencent.com/static_api/v3/components/exception_building/images/building.png" alt="没找到页面！" />
		<i18n path="exception.404" tag="p">
			<a v-slot="action" href='/console/'>{{$t('exception.goBack')}}</a>
		</i18n>
	</div>
</template>

<script lang="ts">
	import Vue from 'vue'
	import { Component } from 'vue-property-decorator'
	@Component
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
