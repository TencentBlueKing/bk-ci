/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package static

var (
	// EncryptionKey describe the common.encrypt key, will be specific when compiling.
	EncryptionKey = "bb_default_key_length_24"

	// ServerCertPwd describe the encrypt password for server cert key file.
	// Will be specific when compiling.
	ServerCertPwd = ""

	// ClientCertPwd describe the encrypt password for client cert key file.
	// Will be specific when compiling.
	ClientCertPwd = ""
)
