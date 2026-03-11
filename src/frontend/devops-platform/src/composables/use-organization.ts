import useDepartmentStore, { DeptInfo } from '@/store/Organizations';
import { computed, onMounted, ref, watch } from 'vue';

export default function useDepartment() {
  const departmentStore = useDepartmentStore();

  const departmentMap = ref<Map<string, DeptInfo>>(new Map());
  const organizationTree = computed(() => Array.from(departmentMap.value.values())
    .filter(department => department.id === '0'));
  const checkedDept = computed(() => getCheckedDept(organizationTree.value));

  watch(() => departmentStore.departmentMap, (deptMap) => {
    departmentMap.value = generateDeptTreeMap(deptMap);
  }, {
    deep: true,
  });

  onMounted(() => {
    if (departmentStore.departmentMap.size === 0) {
      departmentStore.getRootDept();
    } else {
      departmentMap.value = generateDeptTreeMap(departmentStore.departmentMap);
    }
  });

  function generateDeptTreeMap(deptMap?: Map<string, DeptInfo>) {
    const originDepartmentMap = deptMap ?? departmentStore.departmentMap;
    const deptList = Array.from(originDepartmentMap.values());
    const newDepartmentMap: Map<string, DeptInfo> = deptList.reduce((acc, department) => {
      const curDept = departmentMap.value?.get(department.id);
      const parent = departmentMap.value?.get(department.parentId);
      const isChecked = parent?.checked;
      acc.set(department.id, {
        ...department,
        ...(!department.leaf ? {
          children: [],
          async: true,
        } : {}),
        isOpen: curDept?.isOpen ?? false,
        checked: (isChecked || curDept?.checked) ?? false,
        indeterminate: curDept?.indeterminate ?? false,
      });
      return acc;
    }, new Map());

    Array.from(newDepartmentMap.values()).forEach((dept) => {
      const parent = newDepartmentMap.get(dept.parentId);
      if (Array.isArray(parent?.children)) {
        parent.children.push(dept);
        parent.loaded = true;
      }
    });

    return newDepartmentMap;
  }

  async function expandDepartment({ id }: Partial<DeptInfo>) {
    const dept = departmentMap.value.get(id!);
    if (dept) {
      if (!dept.leaf && !dept.loaded) {
        await departmentStore.expandNode(id!);
      }
      updateDepartment(id!, {
        isOpen: true,
      });
    }
  }

  function collapseDept({ id }: Partial<DeptInfo>) {
    updateDepartment(id!, {
      isOpen: false,
    });
  }

  function updateDepartment(id: string, params: Partial<DeptInfo>) {
    const dept = departmentMap.value.get(id);
    if (dept) {
      departmentMap.value.set(id, Object.assign(dept, params));
    }
  }

  function recursionCheckChildDept(
    list: DeptInfo[],
    checked: boolean,
  ) {
    return list.forEach((item) => {
      updateDepartment(item.id, {
        checked,
        indeterminate: false,
      });
      if (!item.leaf && item.loaded) {
        recursionCheckChildDept(item.children ?? [], checked);
      }
    });
  }

  function recursionCheckParentDept(
    id: string,
    checked: boolean,
  ): Record<string, DeptInfo> & void {
    const curDept = departmentMap.value.get(id);
    if (curDept) {
      const parent = departmentMap.value.get(curDept.parentId);
      if (parent) {
        const indeterminate = isHalf(parent.children ?? []);

        departmentMap.value.set(parent.id, Object.assign(parent, {
          checked: checked && !indeterminate,
          indeterminate,
        }));

        if (parent.parentId && parent.parentId !== '-1') {
          return recursionCheckParentDept(parent.id, checked);
        }
      }
    }
  }

  function isHalf(children: DeptInfo[]) {
    let checkedLength = 0;
    let halfLength = 0;
    children.forEach((item) => {
      if (item.checked) checkedLength += 1;
      if (item.indeterminate) halfLength += 1;
    });

    return (checkedLength > 0 && checkedLength < children.length) || (halfLength > 0);
  }

  function getCheckedDept(list = organizationTree.value) {
    return list.reduce((acc: string[], item: DeptInfo) => {
      if (item.checked && !item.indeterminate) {
        acc.push(item.id);
      } else if (item.indeterminate) {
        acc.push(...getCheckedDept(item.children));
      }
      return acc;
    }, []);
  }

  function handleCheck(checked: boolean, department: DeptInfo) {
    const fullDept = departmentMap.value.get(department.id);
    const { leaf, loaded, parentId, children } = fullDept!;
    updateDepartment(department.id, {
      checked: !!checked,
      indeterminate: false,
    });
    if (!leaf && loaded) {
      recursionCheckChildDept(
        children!,
        !!checked,
      );
    }
    if (parentId && parentId !== '-1') {
      recursionCheckParentDept(
        department.id,
        !!checked,
      );
    }
  }

  function getFullDeptName(id): string {
    let dept = departmentMap.value.get(id);
    const fullName: string[] = [];
    while (dept) {
      fullName.unshift(dept.name);
      dept = departmentMap.value.get(dept.parentId);
    }
    return fullName.join('/');
  }

  function clearCheckedDept() {
    checkedDept.value.forEach((id) => {
      const dept = departmentMap.value.get(id);
      if (dept) {
        handleCheck(false, dept);
      }
    });
  }

  return {
    organizationTree,
    departmentMap,
    handleCheck,
    expandDepartment,
    collapseDept,
    getFullDeptName,
    clearCheckedDept,
    checkedDept,
  };
}
