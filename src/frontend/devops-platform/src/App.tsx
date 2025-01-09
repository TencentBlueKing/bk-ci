import { ConfigProvider } from 'bkui-vue';
import { computed, defineComponent } from 'vue';
import { RouterView } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { EN_LOCALE } from './i18n';
import enLang from 'bkui-vue/dist/locale/en.esm';
import zhLang from 'bkui-vue/dist/locale/zh-cn.esm';

export default defineComponent({
    name: 'App',
    setup() {
      const { locale } = useI18n();
      const lang = computed(() => {
        if (locale.value === EN_LOCALE) {
          return enLang;
        }
        return zhLang;
      });
      return () => (
        <div>123</div>
      );
    },
});
  