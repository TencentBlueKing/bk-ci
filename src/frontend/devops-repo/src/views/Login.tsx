/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
import store from '@/store';
import { LOGIN } from '@/store/constants';
import { Button, Input, Message } from 'bkui-vue';
import { computed, defineComponent, onMounted, reactive, ref } from 'vue';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  setup() {
    const LOGIN_TIME_KEY = 'login_time';
    const { t } = useI18n();
    const locked = ref(false);
    const loginFailedTip = ref();
    const loginFailCounter = ref(0);
    const submiting = ref(false);
    const waitSec = ref(0);
    const counter = ref<number>();
    const state = reactive({
      username: '',
      password: '',
    });
    const loginable = computed(() => state.username && state.password);

    onMounted(() => {
      countdown(true);
    });

    async function submitLogin() {
      if (!loginable.value || locked.value) return;
      try {
        submiting.value = true;
        const result = await store.dispatch(LOGIN, {
          username: state.username,
          pwd: state.password,
        });
        if (!result) throw Error(t('loginErrorTip'));
        Message({
          message: `${t('login')}${t('success')}`,
          theme: 'success',
        });
        const afterLoginUrl = sessionStorage.getItem('afterLogin');
        sessionStorage.removeItem('afterLogin');
        afterLoginUrl && window.open(afterLoginUrl, '_self');
        location.href = '';
        loginFailCounter.value = 0;
      } catch (e: any) {
        console.log(e);
        loginFailedTip.value = e.message ?? e;
        loginFailCounter.value += 1;
        if (loginFailCounter.value > 4) {
          countdown();
          locked.value = true;
        }
      } finally {
        submiting.value = false;
      }
    }

    function resetCountdown() {
      locked.value = false;
      loginFailedTip.value = false;
      loginFailCounter.value = 0;
      localStorage.removeItem(LOGIN_TIME_KEY);
    }

    function countdown(isInit = false) {
      const sec = 60;
      const now = Date.now();
      const lastLoginTime = localStorage.getItem(LOGIN_TIME_KEY);
      const startTime = !!lastLoginTime ? parseInt(lastLoginTime, 10) : now;
      const wait = now - startTime;

      if ((lastLoginTime && wait > sec * 1000) || (isInit && !lastLoginTime)) {
        resetCountdown();
        return;
      };

      if (counter.value) {
        clearInterval(counter.value);
      };
      loginFailedTip.value = true;
      locked.value = true;
      waitSec.value = sec - Math.floor(wait / 1000);
      if (!lastLoginTime) {
        localStorage.setItem(LOGIN_TIME_KEY, `${now}`);
      }
      counter.value = setInterval(() => {
        waitSec.value -= 1;
        if (waitSec.value < 1) {
          clearInterval(counter.value);
          resetCountdown();
        }
      }, 1000);
    }
    return () => (
      <div class="bk-repo-login">
        <img class="bg-login-img" src={`${process.env.BASE_URL}assets/bg_login.png`} />
        <section class="bk-repo-login-content">
          <img src={`${process.env.BASE_URL}assets/logo_login.png`} />
          <div class="bk-repo-login-form">
            {
              !!loginFailedTip.value && (
                <div class="flex-align-center login-error-tip">
                  <i class="mr5 bk-icon icon-exclamation-circle"></i>
                  { locked.value ? t('loginLockedTips', [waitSec.value]) : loginFailedTip.value }
              </div>
              )
            }
            <Input
              class="bk-repo-login-input"
              size='large'
              v-model={state.username}
              placeholder={t('userNamePlaceholder')}
              onEnter={submitLogin}
            />
            <Input
              class="bk-repo-login-input"
              size='large'
              v-model={state.password}
              placeholder={t('pwdPlaceholder')}
              onEnter={submitLogin}
            />
            <Button
              class="bk-repo-login-btn"
              theme='primary'
              size='large'
              disabled={!loginable.value || locked.value}
              loading={submiting.value}
              onClick={submitLogin}
            >
              {t('login')}
            </Button>
          </div>
        </section>
      </div>
    );
  },
});
