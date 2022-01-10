/**
 * @file event bus
 * @author ielgnaw <wuji0223@gmail.com>
 */

import Vue from 'vue'

import Raven from 'raven-js'
import RavenVue from 'raven-js/plugins/vue'

Raven
    .config('http://8e69fed03d6d4a27b0b75aad2427ab7e@sentry.open.oa.com/33', { release: window.RELEASE_VERSION })
    .addPlugin(RavenVue, Vue)
    .install()

// Use a bus for components communication,
// see https://vuejs.org/v2/guide/components.html#Non-Parent-Child-Communication
export const bus = new Vue()
