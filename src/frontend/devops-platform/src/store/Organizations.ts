import http from '@/http/api';
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

export interface DeptInfo {
  id: string;
  name: string;
  parentId: string;
  type: string;
  leaf?: boolean;
  children?: DeptInfo[];
  checked?: boolean;
  indeterminate?: boolean;
  loaded: boolean;
  isOpen: boolean;
  loading?: boolean;
}

export interface DeptItem {
  deptId: string;
  deptName: string;
}

export default defineStore('organizations', () => {
  const { t } = useI18n();
  const departmentMap = ref<Map<string, DeptInfo>>(new Map());
  const orgTypeMap = {
    bg: 'dept',
    dept: 'center',
  };

  async function expandNode(id: string) {
    const parent = departmentMap.value.get(id);
    if (parent) {
      Object.assign(parent, {
        loading: true,
      });

      const orgType = orgTypeMap[parent.type] ?? 'bg';
      const res = await http.getOrganizationsList(orgType, parent.id);
      res.forEach((item) => {
        departmentMap.value.set(item.id, {
          ...item,
          leaf: !item.leaf,
          parentId: parent.id,
        });
      });
      if (parent) {
        Object.assign(parent, {
          loaded: true,
          loading: false,
        });
      }
    }
  };

  async function getRootDept() {
    try {
      const rootCompany: DeptInfo = {
        id: '0',
        name: t('腾讯公司'),
        parentId: '-1',
        type: 'root',
        leaf: false,
        children: [],
        isOpen: false,
        loaded: false,
      };
      departmentMap.value.set('0', rootCompany);

      const res = await http.getOrganizationsList('bg', '0');
      res.forEach((item) => {
        const deptWithParent: DeptInfo = {
          ...item,
          leaf: false,
          parentId: '0',
          isOpen: false,
        };
        departmentMap.value.set(item.id, deptWithParent);

        if (Array.isArray(rootCompany.children)) {
          rootCompany.children.push(deptWithParent);
        }
      });
      
      rootCompany.loaded = true;
      
    } catch (error) {
      console.error(error);
    }
  }

  return {
    departmentMap,
    getRootDept,
    expandNode,
  };
});
