# 发布配置

Release 工作流读取以下 GitHub Secrets：

| Secret | 用途 |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | 签名 JKS / PKCS12 文件的 Base64 内容 |
| `ANDROID_KEYSTORE_PASSWORD` | 密钥库密码 |
| `ANDROID_KEY_ALIAS` | 签名密钥别名 |
| `ANDROID_KEY_PASSWORD` | 签名密钥密码 |

生成签名密钥：

```bash
keytool -genkeypair \
  -v \
  -keystore bemfa-control.jks \
  -alias bemfa-control \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10950
```

生成 Base64 内容：

```bash
base64 -w 0 bemfa-control.jks
```

Windows PowerShell 可使用：

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("bemfa-control.jks"))
```

签名密钥不要提交到仓库。发布后必须永久保留同一密钥，否则新 APK 无法覆盖安装旧版本。
