package xyz.xfqlittlefan.scorer.communication

/**
 * 内容协商时发送的消息。
 *
 * @param versionVerified 版本是否匹配。
 * @param password 服务器的密码，版本不匹配时为 null。
 * @param seats 服务器的座位集，值为名称，版本不匹配时为 null。
 */
@kotlinx.serialization.Serializable
data class WebSocketServerInfo(
    val versionVerified: Boolean, val password: Int? = null, val seats: Map<Int, String>? = null
) {
    constructor(password: Int, seats: Map<Int, String>) : this(true, password, seats)
    constructor() : this(false)
}

/**
 * 座位。
 *
 * @param name 座位的显示名称。
 * @param joinable 该座位当前是否可加入。
 */
@kotlinx.serialization.Serializable
data class Seat(val name: String, var joinable: Boolean = true)

/**
 * 消息。
 *
 * @param code 消息代码。
 * @param message 消息内容。
 * @param params 消息的额外参数。
 */
@kotlinx.serialization.Serializable
data class Message(
    val code: MessageCode, val message: String, val params: List<MessageParam>? = null
)

enum class MessageParamContentType { String, Int }

/**
 * 消息的额外参数。
 *
 * @param type 参数类型
 * @param intContent 整型内容
 * @param stringContent 字符串内容
 *
 * @see Message
 */
@kotlinx.serialization.Serializable
data class MessageParam(
    val type: MessageParamContentType,
    val intContent: Int? = null,
    val stringContent: String? = null
) {
    constructor(content: Int) : this(MessageParamContentType.Int, intContent = content)

    constructor(content: String) : this(MessageParamContentType.String, stringContent = content)
}

data class ScoreChangingData(val player: Int, val count: Int)

/**
 * 房间地址二维码信息。
 *
 * @param host 房间主机。
 * @param port 房间端口。
 */
@kotlinx.serialization.Serializable
data class RoomAddressQRCode(val host: String, val port: Int)