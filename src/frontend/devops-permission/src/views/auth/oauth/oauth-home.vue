<script setup>
import card from './oauth-card.vue'
import { useI18n } from 'vue-i18n';
import http from '@/http/api';
import {
  ref,
  onMounted,
} from 'vue';
const { t } = useI18n();
const isLoading = ref(false);
const oauthList = ref([]);

const fetchOauthList = async () => {
	isLoading.value = true;
	await http.getOauthResource().then((res) => {
		oauthList.value = res
		isLoading.value = false;
	})
}

onMounted(() => {
	fetchOauthList();
});
</script>

<template>
	<bk-loading class="oauth-home" :loading="isLoading">
		<card
			v-for="card in oauthList" :key="card.id"
			:oauth="card"
		/>
	</bk-loading>
</template>

<style lang="scss" scoped>
	.oauth-home {
		display: ruby;
		background: #F4F5F9;
		padding: 20px 0;
		overflow: auto;
		&::-webkit-scrollbar-thumb {
      background-color: #c4c6cc !important;
      border-radius: 5px !important;
      &:hover {
        background-color: #979ba5 !important;
      }
    }
    &::-webkit-scrollbar {
      width: 8px !important;
      height: 8px !important;
    }
	}
</style>