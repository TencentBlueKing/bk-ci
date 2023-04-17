import { defineStore } from 'pinia';

export const useGroup = defineStore('group', {
  state: () => ({
    resourceType: '',
    resourceCode: '',
    projectCode: '',
  }),
  actions: {
    setResourceType(type: string) {
      this.resourceType = type;
    },
    setResourceCode(code: string) {
      this.resourceCode = code;
    },
    setProjectCode(code: string) {
      this.projectCode = code;
    },
  },
});
