import {
  defineComponent,
  watch,
  ref,
} from 'vue';

export default defineComponent({
  props: {
    path: {
      type: String,
    },
    query: {
      type: Object,
    },
  },
  setup(props) {
    const commonQuery = {
      system_id: 'bk_ci_rbac',
      source: 'externalApp',
    };
    const iframeUrl = ref('');
    const isLoading = ref(true);

    // deep watch 数据变化，iframe 销毁重建
    watch(
      [
        () => props.path,
        () => props.query,
      ],
      () => {
        // 构造 url
        const url = new URL(`${window.BK_IAM_URL_PREFIX}/${props.path}`);
        const query = {
          ...commonQuery,
          ...props.query,
        };
        Object.keys(query).forEach((key) => {
          url.searchParams.append(key, query[key]);
        });
        iframeUrl.value = url.href;
        isLoading.value = true;
      },
      {
        immediate: true,
        deep: true,
      },
    );

    const onLoad = () => {
      isLoading.value = false;
    };

    return () => <bk-loading
        loading={isLoading.value}
      >
        <iframe
          width="100%"
          height="100%"
          frameborder={0}
          src={iframeUrl.value}
          onLoad={onLoad}
        ></iframe>
      </bk-loading>;
  },
});
