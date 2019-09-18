const { src, dest, parallel } = require('gulp')
const Ora = require('ora')
const yargs = require('yargs')
const argv = yargs.alias('dist', 'd').default('dist', 'frontend', 'build output dist directory').argv
const dist = argv.dist

function copy () {
    return src(['common-lib/**', 'svg-sprites/**'], { 'base': '.' }).pipe(dest(`${dist}/`))
}

function build (cb) {
    const spinner = new Ora('Building library').start()
    require('child_process').exec(`yarn build -- -- --env.dist=${dist}`, (err, res) => {
        if (err) {
            console.log(err)
            process.exit(1)
        }
        spinner.succeed(`Finished building library`)
        cb()
    })
}
  
exports.default = parallel(copy, build)
