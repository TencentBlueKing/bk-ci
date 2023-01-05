/**
 * @file production env
 * @author Blueking
 */

const NODE_ENV = JSON.stringify('production')

export default {
    'process.env': {
        NODE_ENV: NODE_ENV
    },
    NODE_ENV: NODE_ENV,
    LOGIN_URL: JSON.stringify(''),
    AJAX_URL_PREFIX: JSON.stringify('/'),
    AJAX_MOCK_PARAM: JSON.stringify(''),
    WEBSOCKET_URL_PREFIX: JSON.stringify(''),
    USER_INFO_URL: JSON.stringify(''),
    STATIC_URL: JSON.stringify('/static')
}
