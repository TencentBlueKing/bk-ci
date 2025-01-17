import { useI18n } from 'vue-i18n';
import { computed, defineComponent, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import arrowsLeft from '@/css/svg/arrows-left.svg';
import Info from '@/css/svg/info-line.svg';
import Plus from '@/css/svg/plus.svg';
import GitIcon from '@/css/image/git.png';
import GitLabIcon from '@/css/image/gitlab.png';
import PaltformHeader from '@/components/paltform-header';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const formData = ref({
      name: '',
      logo: [],
      checkboxvalue: ['OAUTH'],
      webhook: true
    });
    const rules = {};
    const activeSource = ref('git');
    const codeSourceList = computed(() => {
      return [
        {
          isShowEg: true,
          icon: GitIcon,
          name: t('代码源名称'),
          egTip: t('代码源域名'),
          value: 'codeSourceName'
        },
        {
          isShowEg: true,
          icon: GitIcon,
          name: t('工蜂'),
          tip: t('腾讯工蜂'),
          egTip: 'git.woa.com',
          value: 'git'
        },
        {
          isShowEg: true,
          icon: GitIcon,
          name: t('工蜂SVN'),
          tip: t('腾讯工蜂'),
          egTip: 'tc-svn.tencent.com ; git.woa.com ; svn-cd1.tencent.com',
          value: 'gitSVN'
        },
        {
          isShowEg: false,
          icon: GitIcon,
          name: 'Apache SVN',
          tip: t('基于 Apache SVN 协议…'),
          value: 'ApacheSVN'
        },
        {
          isShowEg: true,
          icon: GitIcon,
          name: 'GitHub',
          tip: 'github.com',
          egTip: 'github.com',
          value: 'GitHub'
        },
        {
          isShowEg: true,
          icon: GitIcon,
          name: 'Perforce',
          tip: t('基于 Perforce 协议自建'),
          egTip: t('无域名要求，能连通代码库服务即可'),
          value: 'Perforce'
        },
        {
          isShowEg: true,
          icon: GitLabIcon,
          name: 'Gitlab',
          tip: t('基于 Gitlab 协议自建'),
          egTip: 'gitlab.com',
          value: 'Gitlab'
        },
        {
          isShowEg: false,
          icon: GitIcon,
          name: 'Gitee',
          tip: 'gitee.com',
          value: 'Gitee'
        },
      ]
    });


    function handleSubmit() {
      console.log('提交');
    }

    function handleCancle() {
      console.log('取消');
    }

    function goBack() {
      router.back()
    }

    function handleRes(response) {
      if (response.id) {
        return true;
      }
      return false;
    }

    function handleDelete(file, fileList) {
      console.log(file, fileList, 'handleDelete');
    }

    function handleChange(val) {
      console.log("🚀 ~ handleChange ~ val:", val)
    }

    return () => (
      <>
        <PaltformHeader>
          {{
            default: () => (
              <p class="flex" >
                <img src={arrowsLeft} alt="" width={16} class="mr-[9px]" onClick={goBack} />
                <span>{t('新增代码源')}</span>
              </p>
            )
          }}
        </PaltformHeader>
        <div class="p-[24px] h-mainHeight">
          <bk-form
            ref="formRef"
            model={formData.value}
            rules={rules}
            class="h-formHeight overflow-y-auto"
          >
            <div class="flex bg-white mb-[16px] rounded-[2px]">
              <div class="flex-1 pr-[68px] py-[16px] pl-[24px]">
                <p class="h-[44px] text-[14px] font-bold text-[#4D4F56]">{t('基本信息')}</p>
                <bk-form-item
                  label={t('代码源')}
                  property="name"
                  required
                >
                  <div class="flex flex-wrap -mx-[8px]">
                    {
                      codeSourceList.value.filter(i => i.value !== 'codeSourceName').map(item => (
                        <div
                          onClick={() => activeSource.value = item.value}
                          class={`flex items-center w-[200px] h-[60px] border pl-[12px] py-[8px] mx-[8px] mb-[16px] cursor-pointer ${activeSource.value === item.value ? 'border-[#3A84FF] bg-[#E1ECFF]' : 'border-[#EAEBF0] bg-[#F5F7FA]'}`}
                        >
                          <img src={item.icon} alt="" class="h-[32px] pr-[12px]" />
                          <div>
                            <p class="text-[14px] h-[22px]">{item.name}</p>
                            <p class="text-[12px] text-[#979BA5]">{item.tip}</p>
                          </div>
                        </div>
                      ))
                    }
                  </div>
                </bk-form-item>
                <bk-form-item
                  label={t('代码源名称')}
                  property="name"
                  required
                >
                  <bk-input
                    v-model={formData.value.name}
                    placeholder={t('由中/英文字符、下划线组成，不超过20个字符，如工蜂、GitHub')}
                    clearable
                  />
                </bk-form-item>
                <bk-form-item
                  label={t('代码源标识')}
                  property="name"
                  required
                >
                  <bk-input
                    v-model={formData.value.name}
                    placeholder={t('由英文字母和下划线组成，不超过20个字符，作为平台服务内交互的唯一标识，如GIT、TGIT')}
                    clearable
                  />
                </bk-form-item>
                <bk-form-item
                  label={t('代码源域名')}
                  property="name"
                  required
                >
                  <bk-input
                    v-model={formData.value.name}
                    placeholder={t('如 github.com等，多个以英文逗号相隔')}
                    clearable
                  />
                </bk-form-item>
                <bk-form-item
                  label="Logo"
                  property="name"
                >
                  <div class="flex">
                    <bk-upload
                      files={formData.value.logo}
                      handle-res-code={handleRes}
                      multiple={false}
                      url="'https://jsonplaceholder.typicode.com/posts/'"
                      theme="picture"
                      with-credentials
                      onDelete={handleDelete}
                    />
                    <span class="text-[#979BA5] text-[12px]">{t('请上传png、jpg、尺寸为大于128*128的正方形Logo，大小不超过2M')}</span>
                  </div>
                </bk-form-item>
              </div>
              <div class="w-[500px] shadow-exalmple bg-[#FAFBFD] py-[16px] px-[24px]">
                <p class="h-[44px] text-[14px] font-bold text-[#4D4F56]">{t('入口示例')}</p>
                <div>
                  <bk-alert
                    theme="info"
                    class="mb-[24px]"
                    title={t('代码源接入后，用户可以关联对应的代码库到平台入口')}
                  />
                  <p class="flex px-[16px] py-[4px] bg-[#3A84FF] text-[14px] text-white w-[122px] rounded-[2px] mb-[4px]">
                    <img src={Plus} alt="" width={12} class="mr-[6px] align-middle" />
                    {t('关联代码库')}
                  </p>
                  <ul class="pt-[4px] bg-white shadow-eg rounded-[2px]">
                    {
                      codeSourceList.value.map(item => (
                        item.isShowEg ?
                          <li class={`flex items-center h-[32px] pl-[12px] py-[5px] text-[12px] ${item.value === 'codeSourceName' ? 'text-[#3A84FF] bg-[#E1ECFF]' : 'text-[#4D4F56]'}`}>
                            <img src={item.icon} alt="" class="h-[16px] mr-[4px]" />
                            <span>{item.value !== 'gitSVN' ? item.name : 'SVN'}</span>
                            <span class="text-[#979BA5] ml-[8px]">{item.egTip}</span>
                          </li> : null
                      ))
                    }
                  </ul>
                </div>
              </div>
            </div>
            <div class="bg-white mb-[16px] px-[24px] py-[16px] rounded-[2px]">
              <p class="h-[44px] text-[14px] font-bold text-[#4D4F56]">{t('高级设置')}</p>
              <bk-form-item
                property="checkboxvalue"
                required
                label={t('授权方式')}
              >
                <bk-checkbox-group v-model={formData.value.checkboxvalue} onChange={handleChange}>
                  <bk-checkbox label="OAUTH" />
                  <bk-checkbox label={t('SSH 私钥 + 私有 Token')} />
                  <bk-checkbox label={t('SSH 私钥')} />
                  <bk-checkbox label={t('访问令牌(AccessToken)')} />
                  <bk-checkbox label={t('用户名+密码')} />
                </bk-checkbox-group>
                {
                  formData.value.checkboxvalue.includes('OAUTH') && (
                    <div class="check-popper relative max-w-[710px] py-[24px] pr-[135px] leading-[128px] border border-[#DCDEE5] bg-[#FAFBFD] mt-[10px]">
                      <bk-form-item
                        label={t('应用 ID')}
                        property="name"
                        required
                      >
                        <bk-input
                          v-model={formData.value.name}
                          placeholder={t('OAUTH 授权时和代码库提供方交互鉴权所需的client_id')}
                          clearable
                        />
                      </bk-form-item>
                      <bk-form-item
                        label={t('应用 Secret')}
                        property="name"
                        required
                      >
                        <bk-input
                          v-model={formData.value.name}
                          placeholder={t('OAUTH授权时和代码库提供方交互鉴权所需的client_secret')}
                          clearable
                        />
                      </bk-form-item>
                    </div>
                  )
                }
              </bk-form-item>
              <bk-form-item label={t('Webhook 监听')}>
                <div class="flex items-center">
                  <bk-switcher
                    v-model={formData.value.webhook}
                    theme="primary"
                  />
                  {
                    formData.value.webhook && (
                      <bk-input
                        prefix={t('签名凭证')}
                        v-model={formData.value.name}
                        class="ml-[24px] max-w-[650px]"
                        placeholder={t('Webhook回调所需的签名密钥')}
                      />
                    )
                  }
                </div>
              </bk-form-item>
              <bk-form-item label="PAC">
                <div class="flex items-center">
                  <bk-switcher
                    v-model={formData.value.pac}
                    theme="primary"
                  />
                  <p class="flex items-center ml-[24px] text-[12px] text-[#4D4F56]">
                    <img src={Info} alt="" class="w-[14px] mr-[8px]" />
                    {t('Pipeline AsCode（PAC）模式下，可以使用代码库.ci目录下的YAML文件编排流水线，且YAML文件变更将自动同步到对应的蓝盾流水线。')}
                  </p>
                </div>
              </bk-form-item>
            </div>
          </bk-form>
          <div class="mt-[16px]">
            <bk-button
              native-type="button"
              theme="primary"
              onClick={handleSubmit}
            >
              {t('提交')}
            </bk-button>
            <bk-button
              style="margin-left: 8px"
              onClick={handleCancle}
            >
              {t('取消')}
            </bk-button>
          </div >
        </div >
      </>
    );
  },
});
