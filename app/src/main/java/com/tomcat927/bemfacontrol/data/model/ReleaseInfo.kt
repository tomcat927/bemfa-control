package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRelease(
    val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
)

@Serializable
data class GitHubAsset(
    val name: String = "",
    val browserDownloadUrl: String = "",
    val size: Long = 0,
)

data class ReleaseInfo(
    val versionName: String,
    val downloadUrl: String,
    val proxyUrl: String,
    val sha256Url: String,
    val releaseNotes: String,
    val isNewer: Boolean,
)
