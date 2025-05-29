
import { useI18n } from 'vue-i18n';
import { defineComponent, ref } from 'vue';
import TimeLimit from '@/components/time-limit'
import AddFill from '@/css/svg/add-fill.svg';
import CloseSamll from '@/css/svg/close-samll.svg';
import EditLine from '@/css/svg/edit-line.svg';

export default defineComponent({
  props: {
    isShow: {
      type: Boolean,
      default: false
    },
    formData: {
      type: Object
    },
  },
  emits: ['confirm', 'closed', 'selected', 'changeTime', 'removeItem'],
  setup(props, { emit }) {
    const { t } = useI18n();
    const rules = {
      name: [
        {
          validator: value => value.length > 2,
          message: '姓名长度不能小于2',
          trigger: 'change',
        },
      ]
    };
    const addFormRef = ref();
    const periodRef = ref();

    function handleAddConfirm() {
      emit('confirm')
      if (periodRef.value) {
        periodRef.value.initTime();
      }
    };
    function handleAddClosed() {
      emit('closed')
      if (periodRef.value) {
        periodRef.value.initTime();
      }
    };
    function handleSelected(value) {
      emit('selected', value)
    };
    function handleChangeTime(value) {
      emit('changeTime', value, 'add')
    };
    function removeItem(id) {
      emit('removeItem', id)
    };

    return () => (
      <bk-dialog
        width={580}
        is-show={props.isShow}
        title={t('添加系统管理员')}
        onConfirm={handleAddConfirm}
        onClosed={handleAddClosed}
      >
        <bk-form
          ref={addFormRef}
          model={props.formData}
          rules={rules}
          label-width={90}
          class="h-formHeight overflow-y-auto"
        >
          <bk-form-item
            label={t('用户名')}
            property="name"
            required
          >
            <bk-input
              v-model={props.formData!.name}
              placeholder={t('请输入用户名')}
              clearable
            />
          </bk-form-item>
          <bk-form-item
            label={t('角色')}
            property="role"
            required
          >
            <bk-button-group>
              <bk-button
                selected={props.formData?.role === '管理员'}
                onClick={() => handleSelected('管理员')}
              >
                {t('管理员')}
              </bk-button>
              <bk-button
                selected={props.formData?.role === '观察者'}
                onClick={() => handleSelected('观察者')}
              >
                {t('观察者')}
              </bk-button>
            </bk-button-group>
          </bk-form-item>
          {
            props.formData?.role === '观察者' && (
              <bk-form-item
                label={t('观察范围')}
                property="scope"
                required
              >
                {
                  props.formData.scope.length ? (
                    <div>
                      <p class="flex">
                        <i18n-t keypath="已选择X个公司，X个组织，X个用户" tag="span">
                          <span class="text-[#3A84FF]">{1}</span>
                          <span class="text-[#299E56]">{2}</span>
                          <span class="text-[#F59500]">{3}</span>
                        </i18n-t>
                        <p class="flex items-center text-[#3A84FF]">
                          <img src={EditLine} alt="" class="w-[12px] mr-[6px] ml-[14px] align-middle" />
                          编辑
                        </p>
                      </p>
                      <ul class="max-h-[320px] overflow-y-auto">
                        <li class="flex items-center justify-between bg-[#F5F7FA] hover:bg-[#F0F1F5] group">
                          <p class="flex items-center">
                            <span class="inline-block w-[16px] h-[16px] border mr-[10px] ml-[40px]"></span>
                            腾讯
                          </p>
                          <img
                            src={CloseSamll}
                            onClick={() => removeItem(props.formData?.id)}
                            class="w-[14px] h-[14px] mr-[16px] hidden group-hover:block "
                          />
                        </li>
                      </ul>
                    </div>
                  ) : (
                    <p class="flex text-[#3A84FF] text-[14px] cursor-pointer">
                      <img src={AddFill} alt="" class="align-middle w-[14px]" />
                      <span class="ml-[5px]">{t('选择组织架构')}</span>
                    </p>
                  )
                }
              </bk-form-item>
            )
          }
          <bk-form-item
            label={t('有效期')}
            property="expiredAt"
            required
          >
            <TimeLimit ref={periodRef} onChangeTime={handleChangeTime} />
          </bk-form-item>
        </bk-form>
      </bk-dialog>
    );
  },
});