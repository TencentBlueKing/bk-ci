# toolchain/BUILD
package(default_visibility = ['//visibility:public'])

cc_toolchain_suite(
    name = "distcccompile",
    toolchains = {
         "cpudistcc": ":distcc_toolchain",
         "cpudistcc|distcccompile": ":distcc_toolchain",
    },
)

filegroup(
    name = "all",
    srcs = [
       "wrapper_cc.sh",
       "wrapper_cxx.sh",
    ],
)

cc_toolchain(
    name = "distcc_toolchain",
    toolchain_identifier = "cross-distcc-toolchain",
    all_files = ":all",
    compiler_files = ":all",
    cpu = "cpudistcc",
    dwp_files = ":empty",
    linker_files = ":all",
    objcopy_files = ":empty",
    strip_files = ":empty",
    supports_param_files = 0,
    static_runtime_libs = [":empty"],
    dynamic_runtime_libs = [":empty"],
)
