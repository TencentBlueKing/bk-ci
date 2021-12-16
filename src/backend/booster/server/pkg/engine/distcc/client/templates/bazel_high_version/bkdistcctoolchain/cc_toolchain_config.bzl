load("@bazel_tools//tools/cpp:cc_toolchain_config_lib.bzl", "tool_path", "action_config", "tool")
load("@bazel_tools//tools/build_defs/cc:action_names.bzl", "ACTION_NAMES")

def _impl(ctx):
    tool_paths = [
        tool_path(
            name = "gcc",
            path = "wrapper_cc.sh",
        ),
        tool_path(
            name = "g++",
            path = "wrapper_cxx.sh",
        ),
        tool_path(
            name = "ld",
            path = "/usr/bin/ld",
        ),
        tool_path(
            name = "ar",
            path = "/usr/bin/ar",
        ),
        tool_path(
            name = "cpp",
            path = "/usr/bin/cpp",
        ),
        tool_path(
            name = "gcov",
            path = "/usr/bin/gcov",
        ),
        tool_path(
            name = "nm",
            path = "/usr/bin/nm",
        ),
        tool_path(
            name = "objdump",
            path = "/usr/bin/objdump",
        ),
        tool_path(
            name = "strip",
            path = "/usr/bin/strip",
        ),
    ]
    
    action_configs = [    
	action_config (
            action_name = ACTION_NAMES.cpp_link_executable,
            enabled = True,
            tools = [
                tool (path = "wrapper_cc_link.sh"),
            ]
        ),
    ]
  
    return cc_common.create_cc_toolchain_config_info(
        ctx = ctx,
        toolchain_identifier = "cross-distcc-toolchain",
    	host_system_name = "i686-unknown-linux-gnu",
    	target_system_name = "distcc-unknown",
    	target_cpu = "cpudistcc",
    	target_libc = "unknown",
    	compiler = "distcccompile",
	    abi_version = "unknown",
        abi_libc_version = "unknown",
        tool_paths = tool_paths,
        action_configs = action_configs,
        cxx_builtin_include_directories = [
            "/usr/lib/gcc/",
            "/usr/local/include",
            "/usr/include/",
            "/usr/lib/gcc/x86_64-redhat-linux/7/include/",
	    ]
    )

cc_toolchain_config = rule(
    implementation = _impl,
    attrs = {},
    provides = [CcToolchainConfigInfo],
)
