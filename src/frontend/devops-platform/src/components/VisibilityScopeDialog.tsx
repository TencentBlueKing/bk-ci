import useOrganization from '@/composables/use-organization';
import { DeptInfo, DeptItem } from '@/store/Organizations';
import { Checkbox, Dialog, Input, Loading, Message, Tree } from 'bkui-vue';
import { defineComponent, ref } from 'vue';
import { AngleDown } from 'bkui-vue/lib/icon';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  name: 'VisibilityDialog',
  props: {
    isShow: {
      type: Boolean,
      required: true,
    },
  },
  emits: ['confirm', 'cancel'],
  setup(props, { emit }) {
    const { t } = useI18n();
    const searchKey = ref('');
    const isFormLoading = ref(false);
    const {
      organizationTree,
      departmentMap,
      expandDepartment,
      collapseDept,
      handleCheck,
      checkedDept,
      getFullDeptName,
      clearCheckedDept,
    } = useOrganization();

    /**
     * 弹窗信息提交时的事件
     */
    async function handleConfirm() {
      if (!checkedDept.value.length) {
        Message({
          theme: 'error',
          message: t('请选择部门'),
        });
        return;
      }
      try {
        isFormLoading.value = true;
        const confirmData: DeptItem[] = [];
        checkedDept.value.forEach((id) => {
          confirmData.push({
            deptId: id,
            deptName: getFullDeptName(id),
          });
        });

        emit('confirm', confirmData, () => {
          clearCheckedDept();
          isFormLoading.value = false;
        });
      } catch (e) {
        console.error(e);
        isFormLoading.value = false;
      } finally {
        searchKey.value = '';
      }
    };
    /**
     * 关闭弹窗
     * 如用户改变数据，关闭侧滑框时二次确认提醒
     */
    function handleCancel() {
      searchKey.value = '';
      clearCheckedDept();
      emit('cancel');
    };

    return () => (
      <Dialog
        width={800}
        height={560}
        is-show={props.isShow}
        title={t('添加范围')}
        theme={'primary'}
        confirmText={t('提交')}
        cancelText={t('取消')}
        isLoading={isFormLoading.value}
        onClosed={handleCancel}
        onConfirm={handleConfirm}
      >
        <Loading loading={isFormLoading.value} class="flex h-[476px]">
          <div class="w-[33%] px-[15px] pb-[40px] flex-1">
            <Input
              v-model={searchKey.value}
              placeholder={t('搜索组织架构')}
              type="search"
              class="mb-[12px]"
            />
            <Tree
              node-key="id"
              label="name"
              showNodeTypeIcon={false}
              autoOpenParentNode={false}
              search={searchKey.value}
              onNodeExpand={expandDepartment}
              onNodeCollapse={collapseDept}
              data={organizationTree.value}
            >
              {{
                nodeAction: ({ isOpen, loading, leaf }) => (
                  <span class="flex items-center size-[20px]">
                    {
                      !leaf && (
                        loading ? (
                          <Loading
                            mode="spin"
                            size="mini"
                            loading
                          />
                        ) : (
                            <AngleDown class={`text-[20px] ${isOpen ? '' : 'rotate-[-90deg]'}`} />
                        )
                      )
                    }
                  </span>
                ),
                node: (department: DeptInfo) => (
                  <span class="flex items-center" onClick={e => e.stopPropagation()}>
                    <Checkbox
                      modelValue={department.checked}
                      indeterminate={department.indeterminate}
                      onChange={checked => handleCheck(checked, department)}
                    />
                    <span class="ml-[8px]">{ department.name }</span>
                  </span>
                ),
              }}
            </Tree>
          </div>
          <div class="w-[33%] px-[15px] overflow-auto flex-1 bg-[#F5F7FA] pt-[8px]">
            <p class="text-[14px] text-[#313238] ">{t('结果预览')}({checkedDept.value.length})</p>
            <ul class="mt-[12px]">
              {
                checkedDept.value.map(id => (
                  <li
                    class="bg-white shadow-[0 1px 2px 0 #0000000f] rounded-[2px] mb-[2px] py-[6px] px-[8px] flex justify-between"
                    key={id}
                  >
                    {departmentMap.value.get(id)?.name ?? '--'}
                    <i class="bk-client-store-icon icon-close-circle right-[8px] mt-[4px]" onClick={() => {
                      handleCheck(false, departmentMap.value.get(id)!);
                    }} />
                  </li>
                ))
              }
            </ul>
          </div>
        </Loading>
      </Dialog>
    );
  },
});
