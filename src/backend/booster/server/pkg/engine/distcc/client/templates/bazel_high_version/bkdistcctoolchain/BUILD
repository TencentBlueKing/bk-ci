# toolchain/BUILD
package(default_visibility = ['//visibility:public'])
load(":cc_toolchain_config.bzl", "cc_toolchain_config")
cc_toolchain_config(name = "cross_distcc_toolchain_config")

cc_toolchain_suite(
    name = "distcccompile",
    toolchains = {
         "cpudistcc": ":distcc_toolchain",
    },
)

filegroup(
    name = "all",
    srcs = [
        "wrapper_cc.sh",
        "wrapper_cxx.sh",
	    "wrapper_cc_link.sh",
    ],
)

cc_toolchain(
    name = "distcc_toolchain",
    toolchain_identifier = "cross-distcc-toolchain",
    toolchain_config = ":cross_distcc_toolchain_config",
    all_files = ":all",
    compiler_files = ":all",
    dwp_files = ":empty",
    linker_files = ":all",
    objcopy_files = ":empty",
    strip_files = ":empty",
    supports_param_files = 0,
)
