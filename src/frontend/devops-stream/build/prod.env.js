/**
 * @file production env
 * @author Blueking
 */

const NODE_ENV = JSON.stringify('production')

export default {
    'process.env': {
        NODE_ENV: NODE_ENV
    },
    NODE_ENV: NODE_ENV
}
