const { src, dest, parallel } = require('gulp')
const Ora = require('ora')
const yargs = require('yargs')
const argv = yargs.alias({
    'dist': 'd'
}).default({
    'dist': 'frontend'
}).describe({
    'dist': 'build output dist directory'
}).argv
const { dist } = argv

function copy () {
    return src(['common-lib/**', 'svg-sprites/**'], { 'base': '.' }).pipe(dest(`${dist}/`))
}

function build (cb) {
    const spinner = new Ora('building bk-ci frontend project').start()
    require('child_process').exec(`yarn build -- -- --env.dist=${dist}`, {
        maxBuffer: 5000 * 1024
    }, (err, res) => {
        if (err) {
            console.log(err)
            process.exit(1)
        }
        spinner.succeed(`Finished building bk-ci frontend project`)
        cb()
    })
}
  
exports.default = parallel(copy, build)
