import { defineStore } from 'pinia';
import type { IUser } from 'types/store';

export const useUser = defineStore('user', {
  state: () => ({
    user: {
      username: '',
      avatar_url: '',
    },
  }),
  actions: {
    setUser(user: IUser) {
      this.user = user;
    },
  },
});
