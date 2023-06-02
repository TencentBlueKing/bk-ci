import {
  defineComponent,
  ref,
  toRefs,
  watch,
  onMounted,
} from 'vue';

export default defineComponent({
  props: {
    idKey: {
      type: String,
      default: 'id',
    },
    nameKey: {
      type: String,
      default: 'name',
    },
    apiMethod: {
      type: Function,
    },
    selectValue: {
      type: [Number, String, Array],
    },
    placeholder: {
      type: String,
    },
    multiple: {
      type: Boolean,
    },
    atomCode: {
      type: String,
    }
  },

  emits: ['update:selectValue', 'change'],

  setup(props, { emit }) {
    // 状态
    const {
      idKey,
      nameKey,
      apiMethod,
      selectValue,
      placeholder,
      multiple,
      atomCode,
    } = toRefs(props);
    const list = ref([]);
    // 分页
    const pageData = ref({
      page: 1,
      pageSize: 100,
      keyword: '',
    });
    const isLoadEnd = ref(false);
    const isLoading = ref(false);
    const isLoadingMore = ref(false);

    // 事件
    const getList = () => {
      if (isLoadEnd.value) return;
      if (pageData.value.page <= 1) {
        list.value = []
        isLoading.value = true;
      } else {
        isLoadingMore.value = true;
      }
      apiMethod
        .value(pageData.value, atomCode)
        .then(({ records = [], count }) => {
          pageData.value.page += 1;
          list.value.push(...records);
          // 加载完全
          if (count <= list.value.length) {
            isLoadEnd.value = true;
          }
        })
        .finally(() => {
          isLoading.value = false;
          isLoadingMore.value = false;
        });
    };

    const handleToggle = (isOpen) => {
      if (isOpen) {
        handleInit();
      }
    };

    const handleInit = () => {
      pageData.value.keyword = '';
      pageData.value.page = 1;
      isLoadEnd.value = false;
      list.value = []
      getList();
    };

    const handleChange = (value) => {
      emit('change', value);
      emit('update:selectValue', value);
    };

    const remoteMethod = (keyword) => {
      pageData.value.keyword = keyword;
      pageData.value.page = 1;
      isLoadEnd.value = false;
      list.value = [];
      getList();
    };

    const selectProps = {
      filterable: true,
      modelValue: selectValue.value,
      loading: isLoading.value,
      scrollLoading: isLoadingMore.value,
      placeholder: placeholder.value,
      multiple: multiple.value,
      remoteMethod,
    };

    onMounted(handleInit);

    return () => (
      <bk-select
        { ...selectProps }
        modelValue={selectValue.value}
        onScrollEnd={getList}
        onChange={handleChange}
        onToggle={handleToggle}
      >
        {
          list.value.map(item => (
            <bk-option
              value={item[idKey.value]}
              label={item[nameKey.value]}
            ></bk-option>
          ))
        }
      </bk-select>
    );
  },
});
