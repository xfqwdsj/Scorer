package xyz.xfqlittlefan.scorer.communication

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import xyz.xfqlittlefan.scorer.R
import xyz.xfqlittlefan.scorer.util.decodeFromJson
import xyz.xfqlittlefan.scorer.util.encodeToJson

/**
 * 服务器的启动器。
 *
 * @param seats 指定座位（[Seat]）列表。
 * @param password 指定连接到服务器所需的密码（0~65535）。如不填入或填入错误值会指定范围内的一个随机数。
 */
class RoomServerLauncher(
    private val seats: MutableMap<Int, Seat> = mutableMapOf(
        0 to Seat(R.string.player_east),
        1 to Seat(R.string.player_south),
        2 to Seat(R.string.player_west),
        3 to Seat(R.string.player_north)
    ), private var password: Int = -1
) {
    init {
        if (password < 0 || password > 65535) {
            password = (0..65535).random()
        }
    }

    /**
     * 每个座位的连接（[DefaultWebSocketServerSession]）。
     */
    private val connections = mutableMapOf<Int, DefaultWebSocketServerSession>()

    /**
     * 服务器。
     */
    val server = embeddedServer(Netty, 0) {
        //安装插件
        install(ContentNegotiation) {
            json()
        }
        install(Compression) {
            gzip()
        }
        install(WebSockets)

        routing {
            //协商服务器
            get("/join/{version}") {
                val clientVersion = call.parameters["version"]?.toIntOrNull() ?: -1

                call.respond(
                    if (clientVersion == CLIENT_VERSION) {
                        WebSocketServerInfo(password,
                            seats.filter { it.value.joinable }
                                .mapValues { entry -> entry.value.nameResource })
                    } else {
                        WebSocketServerInfo()
                    }
                )
            }

            //主服务器
            webSocket("/{password}/{player}") {
                val requestPassword = call.parameters["password"]?.toIntOrNull()
                if (requestPassword == password) {
                    val player = call.parameters["player"]?.toIntOrNull() ?: -1
                    if (seats[player]?.joinable == true && !connections.containsKey(player)) {
                        seats[player]!!.joinable = false
                        connections[player] = this
                        broadcast(
                            Message(
                                MessageCode.NewConnection, "New connection: $player", listOf(
                                    MessageParam(player)
                                )
                            )
                        )
                        try {
                            for (frame in incoming) {
                                val message =
                                    (frame as Frame.Text).readText().decodeFromJson<Message>()

                            }
                        } catch (e: ClosedReceiveChannelException) {
                            e.printStackTrace()
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        } finally {
                            connections -= player
                        }
                    } else {
                        close(
                            CloseReason(
                                CloseReason.Codes.CANNOT_ACCEPT, Message(
                                    MessageCode.SeatOccupied, "Connection closed with an error."
                                ).encodeToJson()
                            )
                        )
                    }
                } else {
                    close(
                        CloseReason(
                            CloseReason.Codes.CANNOT_ACCEPT, Message(
                                MessageCode.PasswordIncorrect, "Connection closed with an error."
                            ).encodeToJson()
                        )
                    )
                }
            }
        }
    }

    /**
     * 向每一个连接（[server]）发送广播。
     *
     * @param message 要发送的消息。
     */
    private suspend fun broadcast(message: Message) {
        connections.forEach { (_, session) ->
            session.send(message.encodeToJson())
        }
    }
}