package xyz.xfqlittlefan.scorer.communication

import androidx.annotation.StringRes
import xyz.xfqlittlefan.scorer.R

enum class MessageCode(@StringRes resource: Int? = null) {
    NewConnection(R.string.connection_message_new_connection),
    SomeoneLeaving(R.string.connection_message_someone_leaving),
    SomeoneClosing(R.string.connection_message_someone_closing),
    ScoreChanged(R.string.connection_message_score_changed),
    PasswordIncorrect(R.string.connection_closing_reason_unknown),
    SeatOccupied(R.string.connection_closing_reason_seat_occupied),
    ChangeScore
}