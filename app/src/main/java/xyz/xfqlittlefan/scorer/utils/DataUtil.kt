package xyz.xfqlittlefan.scorer.utils

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> T.encodeToJson() = Json.encodeToString(this)

inline fun <reified T> String.decodeFromJson() = Json.decodeFromString<T>(this)