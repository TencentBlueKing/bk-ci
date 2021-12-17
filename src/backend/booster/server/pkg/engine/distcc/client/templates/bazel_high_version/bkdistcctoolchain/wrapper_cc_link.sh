#!/bin/bash
set -euo pipefail
gcc -fuse-ld=gold -Wl,-no-as-needed -Wl,-z,relro,-z,now -pass-exit-codes -lstdc++ -lm "$@"
