
import { useI18n } from 'vue-i18n';
import { defineComponent, ref } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import AngleDown from '@/css/svg/down.svg';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const route = useRoute();

    const showMenu = ref(true);
    const navList = [
      {
        name: t('代码源管理'),
        router: 'Config',
        icon: 'metrics-overview',
        meta: ['Config', 'ConfigForm']
      },
    ];

    function handleShowMenu() {
      showMenu.value = !showMenu.value;
    };

    const conventionActive = () => ['Convention'].includes(String(route.name));

    return () => (
      <article class="w-full h-full flex overflow-hidden">
        <aside class="w-[240px] bg-white border-r-[1px]">
          <h3 class="h-[52px] leading-[52px] pl-[24px] pr-[14px] text-[16px] text-[#333C48]">{t('平台管理')}</h3>
          <ul class="flex flex-col">
            <li onClick={handleShowMenu} class="flex justify-between h-[40px] leading-[40px] pl-[22px] pr-[14px] cursor-pointer">
              <div>
                <i class="permission-icon permission-icon-permission" />
                <span class="text-[#4D4F56] text-[14px]">{t('代码库服务')}</span>
              </div>
              <img src={AngleDown} alt="" class={`w-[16px] ${showMenu.value ? '' : 'transform -rotate-90'}`} />
            </li>
            {
              showMenu.value ?
                navList.map(nav => (
                  <RouterLink
                    to={{ name: nav.router }}
                    key={nav.name}
                    class={`${
                      nav.meta && nav.meta.includes(String(route.name))
                        ? 'bg-[#e1ecff] !text-[#3a84ff]'
                        : ''
                    } relative flex items-center px-[23px] h-[40px] leading-[40px] text-[#4D4F56] text-[14px]`}
                  >
                    <i class="text-[16px] mr-[8px]"></i>
                    {nav.name}
                  </RouterLink>
                )) : null
            }
            <RouterLink
              to={{ name: 'Convention' }}
              class={`${
                conventionActive()
                  ? 'bg-[#e1ecff] !text-[#3a84ff]'
                  : ''
              } relative flex items-center pl-[22px] pr-[23px] h-[40px] leading-[40px] text-[#4D4F56] text-[14px]`}
            >
              {t('蓝盾公约')}
            </RouterLink>
          </ul>
        </aside>
        <main class="h-full w-platformMainWidth">
          <router-view></router-view>
        </main>
      </article>
    );
  },
});
