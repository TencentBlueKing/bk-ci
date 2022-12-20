
import { defineStore } from 'pinia';

export const useGroup = defineStore('group', {
  state: () => ({
    resourceType: '',
    resourceCode: '',
  }),
  actions: {
    setResourceType(type: string) {
      this.resourceType = type;
    },
    setResourceCode(code: string) {
      this.resourceCode = code;
    },
  },
});
