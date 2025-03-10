import api from '@/http/api';
import { TargetNetBehavior } from '@/types';
import { defineStore } from 'pinia';
import { ref } from 'vue';

export default defineStore('userStore', () => {
  const isLoading = ref(false);
  const user = ref<TargetNetBehavior>();

  async function fetchUserInfo() {
    try {
      isLoading.value = true;
      const res = await api.getUser();
      user.value = res;
    } catch (error) {

    } finally {
      isLoading.value = false;
    }
  }

  function logout() {
    // TODO: Logout
  }

  return {
    user,
    isLoading,
    fetchUserInfo,
    logout,
  };
});
