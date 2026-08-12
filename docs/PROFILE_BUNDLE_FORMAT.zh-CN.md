# Heimdall Profile 迁移包格式

[English](PROFILE_BUNDLE_FORMAT.md) | 简体中文

Heimdall 将 Profile 导出为自包含的 `.heimdall-profile` ZIP 迁移包。当前格式标识为 `heimdall-profile-bundle`，schema 版本为 `1`。

## 内容

- `manifest.json`：格式版本、Profile 元数据校验和与大小，以及完整资源清单。
- `profiles.json`：常规 Heimdall Profile 导出 JSON；已打包资源的引用会替换为 `heimdall-bundle:<sha256>` 占位符。
- `assets/<sha256>.<extension>`：按内容寻址的资源文件。

清单会记录每项资源的 SHA-256、字节大小、媒体类型、安全显示名称和准确 ZIP 路径。多个 Profile 字段引用相同内容时只存储一份；显示名称相同但内容不同的文件仍会分别保存。

## 包含的资源

导出器会收集受支持的 Profile 图标、地图、文件 Guide、用户导入的 Macro 图标和 Canvas 图片。内置 Macro 图标与在线链接继续作为普通结构化引用保存。任何必需资源缺失、不可读、不受支持或超出大小限制时，整个导出都会失败，不会产生部分可用的迁移包。

## 导入安全

导入采用 fail-closed 策略。Heimdall 会先把玩家选择的文件复制到私有暂存目录，在显示“追加”或“全部替换”选项之前完成校验。校验包括：

- 准确的格式与 schema 版本；
- 完整的 ZIP entry 集合和安全的固定路径；
- 清单大小、总量限制、SHA-256 和检测到的媒体类型；
- 每个 Profile 资源引用，以及不存在未引用 payload；
- 支持的图片尺寸范围和标准化 Macro 图标要求。

通过校验的资源会使用临时文件和原子重命名安装到 Heimdall 自有存储。Profile 引用会改写为当前包身份，因此 Debug 与公开版本不会依赖彼此的 provider authority。“全部替换”仍要求在覆盖当前 Profile 数据前成功建立恢复快照。导入失败不会替换当前有效的 Profile 集合；极晚阶段失败时，已安装但未引用的内容寻址文件可能无害地保留。

## 兼容性与限制

旧版 JSON 导出仍可导入，并会被明确标记为“仅配置”。它们无法恢复旧版导出器从未包含的外部源文件。

Schema 版本 1 将单个资源限制为 128 MB，未压缩资源总量限制为 512 MB，迁移包输入限制为 512 MB，资源数量限制为 512，Profile JSON 限制为 16 MB，清单限制为 1 MB。可存储扩展名包括 JPG、PNG、WebP、GIF、PDF、TXT、Markdown 和 HTML；每个引用字段仍可能应用更严格的规则。
