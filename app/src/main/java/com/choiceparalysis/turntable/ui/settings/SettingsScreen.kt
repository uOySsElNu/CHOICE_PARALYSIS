package com.choiceparalysis.turntable.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsOfUse by remember { mutableStateOf(false) }
    var showOpenSourceLicenses by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // About section
        Text(
            text = "关于",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("版本") },
                    trailingContent = {
                        Text(
                            text = "1.3.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                ListItem(
                    headlineContent = { Text("隐私政策") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showPrivacyPolicy = true }
                )
                ListItem(
                    headlineContent = { Text("使用条款") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showTermsOfUse = true }
                )
                ListItem(
                    headlineContent = { Text("开源许可") },
                    leadingContent = {
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.clickable { showOpenSourceLicenses = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = {
                Text(
                    text = "隐私政策",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            最后更新日期：2026年5月14日

                            选择困难症助手（以下简称"本应用"）是一款开源软件，尊重并保护您的隐私。

                            1. 信息收集

                            本应用不收集任何个人身份信息。所有数据（包括选项列表、历史记录等）均存储在您的设备本地，不会上传到任何服务器。

                            2. 数据存储

                            • 选项列表：您创建的决策选项存储在设备本地
                            • 历史记录：决策历史记录存储在设备本地
                            • 设置偏好：应用设置存储在设备本地

                            3. 数据安全

                            所有数据均存储在应用私有目录中，其他应用无法访问。您可以随时通过应用内功能删除所有数据。

                            4. 第三方服务

                            本应用不集成任何第三方分析、广告或追踪服务。

                            5. 权限说明

                            本应用仅请求必要的存储权限，用于保存和读取用户自定义的图片资源。

                            6. 开源透明

                            本应用完全开源，源代码公开可审计。您可以在 GitHub 上查看完整源代码，确认本隐私政策的真实性。

                            7. 政策更新

                            我们可能会不时更新本隐私政策。更新后的政策将在应用内和 GitHub 仓库中发布。

                            8. 联系我们

                            如有任何隐私相关问题，请在 GitHub 上提交 Issue：
                            https://github.com/uOySsElNu/CHOICEPARALYSIS/issues
                        """.trimIndent()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) {
                    Text("确定")
                }
            }
        )
    }

    // Terms of Use Dialog
    if (showTermsOfUse) {
        AlertDialog(
            onDismissRequest = { showTermsOfUse = false },
            title = {
                Text(
                    text = "使用条款",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            最后更新日期：2026年5月14日

                            欢迎使用选择困难症助手。本应用是一款开源软件，使用 MIT 许可证发布。使用本应用即表示您同意以下条款：

                            1. 服务说明

                            本应用是一款决策辅助工具，提供转盘、硬币、骰子、是非选择等决策方式。本应用仅供娱乐和辅助决策使用，不保证决策结果的合理性或准确性。

                            2. 开源许可

                            本应用基于 MIT 许可证开源，您可以：
                            • 自由使用、复制和分发本软件
                            • 修改本软件并用于个人或商业目的
                            • 在 MIT 许可证条款下继续分发修改后的版本

                            3. 用户责任

                            • 您应合理使用本应用，不得将其用于任何非法目的
                            • 您应对自己的决策负责，本应用仅提供辅助参考

                            4. 免责声明

                            • 本应用按"现状"提供，不作任何明示或暗示的保证
                            • 我们不对因使用本应用而产生的任何损失承担责任
                            • 本应用的决策结果仅供娱乐参考，不构成任何建议

                            5. 条款更新

                            我们保留随时修改本使用条款的权利。继续使用本应用即表示您同意修改后的条款。

                            6. 源代码

                            本应用完整源代码可在以下地址获取：
                            https://github.com/uOySsElNu/CHOICEPARALYSIS

                            7. 适用法律

                            本条款受中华人民共和国法律管辖。
                        """.trimIndent()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsOfUse = false }) {
                    Text("确定")
                }
            }
        )
    }

    // Open Source Licenses Dialog
    if (showOpenSourceLicenses) {
        AlertDialog(
            onDismissRequest = { showOpenSourceLicenses = false },
            title = {
                Text(
                    text = "开源许可",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = """
                            本应用使用了以下开源库：

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Android Jetpack Compose
                            版权所有 © Android Open Source Project
                            许可证：Apache License 2.0

                            Jetpack Compose 是 Android 的现代工具包，用于构建原生 UI。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Material Design 3
                            版权所有 © Google LLC
                            许可证：Apache License 2.0

                            Material Design 是 Google 的设计系统，用于创建高质量的数字体验。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Coil
                            版权所有 © Coil Contributors
                            许可证：Apache License 2.0

                            Coil 是一个 Android 图片加载库，使用 Kotlin 协程构建。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Kotlin
                            版权所有 © JetBrains s.r.o.
                            许可证：Apache License 2.0

                            Kotlin 是一种现代、简洁、安全的编程语言。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Kotlinx Serialization
                            版权所有 © JetBrains s.r.o.
                            许可证：Apache License 2.0

                            Kotlinx Serialization 是 Kotlin 的序列化库。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            AndroidX DataStore
                            版权所有 © Android Open Source Project
                            许可证：Apache License 2.0

                            DataStore 是一种数据存储解决方案，用于替代 SharedPreferences。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            AndroidX Navigation
                            版权所有 © Android Open Source Project
                            许可证：Apache License 2.0

                            Navigation 组件用于处理应用内的导航。

                            ━━━━━━━━━━━━━━━━━━━━━━━━

                            Apache License 2.0 摘要：

                            您可以自由地：
                            • 使用、复制和分发本软件
                            • 修改本软件
                            • 在商业项目中使用本软件

                            条件：
                            • 保留版权声明和许可证
                            • 标注修改内容

                            详细许可证文本请访问：
                            https://www.apache.org/licenses/LICENSE-2.0
                        """.trimIndent()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showOpenSourceLicenses = false }) {
                    Text("确定")
                }
            }
        )
    }
}
