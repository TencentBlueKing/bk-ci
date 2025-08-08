import http from '@/http/api'
import { watch, computed, defineComponent, onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { providerConfig, repoConfigFromData, UploadLogoResponse } from '@/types/index';
import { Message, InfoBox } from 'bkui-vue';
import { deepCopy } from '@/utils/utils';
import useRepoConfigTable from "./useRepoConfigTable";
import arrowsLeft from '@/css/svg/arrows-left.svg';
import Info from '@/css/svg/info-line.svg';
import Plus from '@/css/svg/plus.svg';
import GitIcon from '@/css/image/git.png';
import SvnIcon from '@/css/image/svn.png';
import GitLabIcon from '@/css/image/gitlab.png';
import GithubIcon from '@/css/image/github.png';
import PlatformHeader from '@/components/platform-header';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const route = useRoute();
    const router = useRouter();
    const formRef = ref();
    const pageLoading = ref(false);
    const isSubmitLoading = ref(false);
    const providerList = ref<providerConfig[]>([]);
    const getDefaultFormData = (): repoConfigFromData => ({
      scmCode: '',
      name: '',
      hosts: '',
      providerCode: 'TGIT',
      scmType: 'SCM_GIT',
      logoUrl: '',
      credentialTypeList: [],
      oauthType: 'NEW',
      oauthScmCode: '',
      mergeEnabled: false,
      pacEnabled: false,
      webhookEnabled: false,
      props: {
        apiUrl: '',
        webUrl: '',
        clientId: '',
        webhookSecret: '',
        clientSecret: '',
        proxyEnabled: false,
      }
    });
    const configFormDataRules = {
      name: [
        {
          validator: value => /^[\u4e00-\u9fa5\w ]{1,20}$/.test(value),
          message: t('由中/英文字符、下划线组成，不超过20个字符'),
          trigger: 'change'
        }
      ],
      scmCode: [
        {
          validator: value => /^[A-Za-z_]{1,20}$/.test(value),
          message: t('由英文字母和下划线组成，不超过20个字符'),
          trigger: 'change'
        }
      ]
    };
    const configFormData = ref<repoConfigFromData>(getDefaultFormData());
    const initConfigFormData = ref({});
    const isFormDataChanged = computed(() => JSON.stringify(initConfigFormData.value) !== JSON.stringify(configFormData.value))
    const isCreate = computed(() => route.query?.action === 'create');
    const curProviderConfig = computed(() => providerList.value.find(i => i.providerCode === configFormData.value.providerCode) as providerConfig);
    const isNewOauthType = computed(() => configFormData.value.oauthType === 'NEW' || false)
    const logoFiles = computed(() => {
      const { logoUrl } = configFormData.value;
      const files = [] as any;
      if (logoUrl) {
        files.push({
          url: logoUrl
        });
      }
      return files;
    })

    const repoConfigStore = useRepoConfigTable();
    const {
      isLoading,
      repoConfigList,
      curConfig,
    } = storeToRefs(repoConfigStore)
    const {
      initPagination,
      handleScrollEnd,
      getRepoConfigList
    } = useRepoConfigTable()

    watch(() => configFormData.value.oauthType, async (val) => {
      if (val === 'REUSE') {
        await initPagination();
        await getRepoConfigList();
      }
    })

    const codeSourceList = [
      {
        icon: GitIcon,
        name: t('代码源名称'),
        egTip: t('代码源域名'),
        isActive: true
      },
      {
        icon: GitIcon,
        name: t('工蜂'),
      },
      {
        icon: SvnIcon,
        name: 'SVN',
      },
      {
        icon: GithubIcon,
        name: 'GitHub',
        egTip: 'github.com'
      },
      {
        icon: GitLabIcon,
        name: 'Gitlab',
        tip: t('基于 Gitlab 协议自建'),
        egTip: 'gitlab.com'
      }
    ]

    const handleSubmit = async () => {
      try {
        await formRef.value.validate();

        let res;
        const successMessage = isCreate.value ? t('新增代码源成功') : t('修改代码源成功');
        if (isCreate.value) {
            res = await http.createRepoConfig(configFormData.value);
        } else {
            res = await http.updateRepoConfig(configFormData.value.scmCode, configFormData.value);
        }
        if (res) {
            Message({
              theme: 'success',
              message: successMessage
          });
            router.push({
              name: 'Config'
            })
        }
      }
      catch (e) {
        console.error(e)
      } finally {
        isSubmitLoading.value = false
      }
    }

    const handleCancel = () => {
      goBack()
    }

    const goBack = () => {
      if (isFormDataChanged.value) {
        InfoBox({
          title: t('确认离开当前页？'),
          subTitle: t('离开将会导致未保存信息丢失'),
          confirmText: t('确定'),
          cancelText: t('取消'),
          onConfirm() {
            router.push({
              name: 'Config'
            })
          },
          headerAlign: 'center',
          footerAlign: 'center',
          contentAlign: 'center'
        })
      } else {
        router.push({
          name: 'Config'
        })
      }
    }

    const handleChangeProvider = (item) => {
      if (!isCreate.value) return
      configFormData.value = getDefaultFormData()
      configFormData.value.providerCode = item.providerCode
      configFormData.value.scmType = item.scmType
    }
    const getProviderList = async () => {
      try {
        pageLoading.value = true
        providerList.value = await http.fetchListProvider()
      } catch (e) {
        console.error(e)
      } finally {
        pageLoading.value = false
      }
    }

    const handleDeleteLogo = () => {
      configFormData.value.logoUrl = ''
    }

    const handleUploadLogo = async (res: UploadLogoResponse) => {
      const { file } = res;
      if (file) {
        if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
          Message({
            theme: 'danger',
            message: t('只允许上传png、jpg'),
          });
        } else if (file.size > (2 * 1024 * 1024)) {
          Message({
            theme: 'danger',
            message: t('大小不超过2M'),
          });
        }
        const formData = new FormData();
        formData.append('logo', file);
        await http.uploadConfigLog({
          formData,
        }).then((res) => {
          configFormData.value.logoUrl = res.url;
        })
      }
    }

    const handleChangeName = (value) => {
      configFormData.value.name = value.trim();
    }

    onMounted(async () => {
      await getProviderList()
      if (!isCreate.value) {
        configFormData.value = curConfig.value as repoConfigFromData
        configFormData.value.credentialTypeList = curConfig.value?.credentialTypeList?.map((i => i.credentialType))
      }
      initConfigFormData.value = deepCopy(configFormData.value)
    })
    return () => (
      <>
        <PlatformHeader>
          {{
            default: () => (
              <p class="flex" >
                <img src={arrowsLeft} alt="" width={16} class="mr-[9px]" onClick={goBack} />
                <span>{ isCreate.value ? t('新增代码源') : t('编辑代码源')}</span>
              </p>
            )
          }}
        </PlatformHeader>
        <bk-loading loading={pageLoading.value}  class="p-[24px] h-mainHeight">
          <bk-form
            ref={formRef}
            model={configFormData.value}
            rules={configFormDataRules}
            class="h-formHeight overflow-y-auto"
          >
            <div class="flex bg-white mb-[16px] rounded-[2px]">
              <div class="flex-1 pr-[68px] py-[16px] pl-[24px]">
                <p class="h-[44px] text-[14px] font-bold text-[#4D4F56]">{t('基本信息')}</p>
                <bk-form-item
                  label={t('代码源')}
                  property="providerCode"
                  required
                >
                  <div class="flex flex-wrap -mx-[8px]">
                    {
                      providerList.value.map(item => (
                        <div
                          onClick={() => handleChangeProvider(item)}
                          class={`flex items-center w-[200px] h-[60px] border pl-[12px] py-[8px] mx-[8px] mb-[16px]
                            ${configFormData.value.providerCode === item.providerCode
                              ? 'border-[#3A84FF] bg-[#E1ECFF]'
                              : 'border-[#EAEBF0] bg-[#F5F7FA]'
                            }
                            ${!isCreate.value ? 'cursor-not-allowed' : 'cursor-pointer'}`
                          }
                        >
                          <img src={item.logoUrl} alt="" class="h-[32px] pr-[12px]" />
                          <div>
                            <p class="text-[14px] h-[22px]">{item.name}</p>
                            <p class="text-[12px] text-[#979BA5]">{item.desc}</p>
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
                    v-model={configFormData.value.name}
                    placeholder={t('由中/英文字符、下划线、空格组成，不超过20个字符，如工蜂、GitHub')}
                    clearable
                    onInput={handleChangeName}
                  />
                </bk-form-item>
                <bk-form-item
                  label={t('代码源标识')}
                  property="scmCode"
                  required
                >
                  <bk-input
                    v-model={configFormData.value.scmCode}
                    disabled={!isCreate.value}
                    placeholder={t('由英文字母和下划线组成，不超过20个字符，作为平台服务内交互的唯一标识，如GIT、TGIT')}
                    clearable
                  />
                </bk-form-item>
                <bk-form-item
                  label={t('代码源域名')}
                  property="hosts"
                  required
                >
                  <bk-input
                    v-model={configFormData.value.hosts}
                    placeholder={t('如 github.com等，多个以英文逗号相隔')}
                    clearable
                  />
                </bk-form-item>
                <bk-form-item
                  label="Logo"
                  property="logoUrl"
                >
                  <div class="flex">
                    <bk-upload
                      files={logoFiles.value}
                      multiple={false}
                      theme="picture"
                      with-credentials
                      custom-request={handleUploadLogo}
                      onDelete={handleDeleteLogo}
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
                      codeSourceList.map(item => (
                        <li class={`flex items-center h-[32px] pl-[12px] py-[5px] text-[12px] ${item.isActive ? 'text-[#3A84FF] bg-[#E1ECFF]' : 'text-[#4D4F56]'}`}>
                          <img src={item.icon} alt="" class="h-[16px] mr-[4px]" />
                          <span>{item.name}</span>
                          <span class="text-[#979BA5] ml-[8px]">{item.egTip}</span>
                        </li>
                      ))
                    }
                  </ul>
                </div>
              </div>
            </div>
            <div class="bg-white px-[24px] py-[16px] rounded-[2px]">
              <p class="h-[44px] text-[14px] font-bold text-[#4D4F56]">{t('高级设置')}</p>
              <bk-form-item
                property="props.apiUrl"
                required
                label={'API URL'}
              >
                  <bk-input
                    class="max-w-[710px]"
                    v-model={configFormData.value.props.apiUrl}
                    clearable
                  />
              </bk-form-item>
              <bk-form-item
                property="credentialTypeList"
                required
                label={t('授权方式')}
              >
                <bk-checkbox-group v-model={configFormData.value.credentialTypeList}>
                  {
                    curProviderConfig.value?.credentialTypeList?.map(auth => (
                      <bk-checkbox
                        label={auth.credentialType}
                      >
                        {auth.name}
                      </bk-checkbox>
                    ))
                  }
                </bk-checkbox-group>
              </bk-form-item>
              {
                configFormData.value.credentialTypeList.includes('OAUTH') ? (
                  <div class="ml-[150px] mb-[20px]">
                    {
                      curProviderConfig.value?.credentialTypeList?.find(i => i.credentialType === 'OAUTH') && (
                        <div class="check-popper relative max-w-[710px] pt-[24px] pr-[120px] leading-[128px] border border-[#DCDEE5] bg-[#FAFBFD] mt-[10px]">
                          <bk-form-item
                            label={t('OAUTH类型')}
                            property="oauthType"
                            required
                          >
                            <bk-radio-group v-model={configFormData.value.oauthType}>
                              <bk-radio label="NEW">
                                {t('新建')}
                              </bk-radio>
                              <bk-radio label="REUSE">
                                {t('复用')}
                              </bk-radio>
                            </bk-radio-group>
                          </bk-form-item>
                          {
                            isNewOauthType.value ? (
                              <section>
                                <bk-form-item
                                  label="web url"
                                  property="props.webUrl"
                                  required
                                >
                                  <bk-input
                                    v-model={configFormData.value.props.webUrl}
                                    clearable
                                  />
                                </bk-form-item>
                                <bk-form-item
                                  label={t('应用 ID')}
                                  property="props.clientId"
                                  required
                                >
                                  <bk-input
                                    v-model={configFormData.value.props.clientId}
                                    placeholder={t('OAUTH 授权时和代码库提供方交互鉴权所需的client_id')}
                                    clearable
                                  />
                                </bk-form-item>
                                <bk-form-item
                                  label={t('应用 Secret')}
                                  property="props.clientSecret"
                                  required
                                >
                                  <bk-input
                                    v-model={configFormData.value.props.clientSecret}
                                    placeholder={t('OAUTH授权时和代码库提供方交互鉴权所需的client_secret')}
                                    clearable
                                  />
                                </bk-form-item>
                              </section>
                            ) : (
                              <bk-form-item
                                label={t('代码源标识')}
                                property="oauthScmCode"
                                required
                              >
                                <bk-select
                                  v-model={configFormData.value.oauthScmCode}
                                  scroll-loading={isLoading.value}
                                  filterable
                                  scroll-end={handleScrollEnd}
                                >
                                  {
                                    repoConfigList.value.map(repo => 
                                      (
                                        <bk-option
                                          id={repo.scmCode}
                                          key={repo.scmCode}
                                          name={repo.name}
                                        />
                                      )
                                    )
                                  }
                                </bk-select>
                              </bk-form-item>
                            )
                          }
                        </div>
                      )
                    }
                  </div>
                ) : null
              }
              {
                (curProviderConfig.value?.webhook && curProviderConfig.value?.webhookSecretType === 'APP') && (
                  <bk-form-item
                    label={t('Webhook 监听')}
                  >
                    <div class="flex items-center h-[32px]">
                      <bk-switcher
                        v-model={configFormData.value.webhookEnabled}
                        theme="primary"
                      />
                      {
                        configFormData.value.webhookEnabled && (
                          <bk-input
                            prefix={t('签名凭证')}
                            v-model={configFormData.value.props.webhookSecret}
                            class="ml-[24px] max-w-[650px]"
                            placeholder={t('Webhook回调所需的签名密钥')}
                          />
                        )
                      }
                    </div>
                  </bk-form-item>
                )
              }
              {
                curProviderConfig.value?.pac && (
                  <bk-form-item
                    label="PAC"
                  >
                    <div class="flex items-center">
                      <bk-switcher
                        v-model={configFormData.value.pacEnabled}
                        theme="primary"
                      />
                      <p class="flex items-center ml-[24px] text-[12px] text-[#4D4F56]">
                        <img src={Info} alt="" class="w-[14px] mr-[8px]" />
                        {t('Pipeline AsCode（PAC）模式下，可以使用代码库.ci目录下的YAML文件编排流水线，且YAML文件变更将自动同步到对应的蓝盾流水线。')}
                      </p>
                    </div>
                  </bk-form-item>
                )
              }
            </div>
          </bk-form>
          <div class="mt-[16px]">
            <bk-button
              native-type="button"
              theme="primary"
              loading={isSubmitLoading.value}
              onClick={handleSubmit}
            >
              {t('提交')}
            </bk-button>
            <bk-button
              style="margin-left: 8px"
              loading={isSubmitLoading.value}
              onClick={handleCancel}
            >
              {t('取消')}
            </bk-button>
          </div >
        </bk-loading>
      </>
    );
  },
});
