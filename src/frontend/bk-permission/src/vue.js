let Vue;
let version;

try {
    Vue = require('vue');
    if (/^3/.test(Vue.version)) {
        version = 3;
    } else {
        Vue = require('@vue/composition-api');
        version = 2;
    }
} catch (error) {
    console.error(error)
}

Object.keys(Vue).forEach((key) => {
    exports[key] = Vue[key]
});

exports.version = version;
