import { defineStore, acceptHMRUpdate } from 'pinia';

export const useUser = defineStore('user', {
  state: () => ({
    user: '',
  }),
  actions: {
    setUser(user) {
      this.user = user;
    },
  },
});

// hot update
if (import.meta.hot) {
  import.meta.hot.accept(acceptHMRUpdate(useUser, import.meta.hot));
}
