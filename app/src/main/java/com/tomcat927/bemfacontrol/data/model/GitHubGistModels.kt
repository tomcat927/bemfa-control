package com.tomcat927.bemfacontrol.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GitHubGistFile(
    val content: String? = null,
    val truncated: Boolean? = null,
)

@Serializable
data class GitHubGist(
    val id: String? = null,
    val files: Map<String, GitHubGistFile> = emptyMap(),
)

@Serializable
data class GitHubGistFileRequest(
    val content: String,
)

@Serializable
data class GitHubGistRequest(
    val description: String? = null,
    val public: Boolean = false,
    val files: Map<String, GitHubGistFileRequest>,
)
