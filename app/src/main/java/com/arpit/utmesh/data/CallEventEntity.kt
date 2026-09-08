package com.arpit.utmesh.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.arpit.utmesh.service.RfcommForegroundService.CallDirection
import com.arpit.utmesh.service.RfcommForegroundService.CallResult
import com.arpit.utmesh.service.RfcommForegroundService.CallEvent

@Entity(
    tableName = "call_events",
    indices = [Index(value = ["sessionCode", "timestampMillis"])]
)
data class CallEventEntity(
    @PrimaryKey val id: String,
    val sessionCode: String,
    val timestampMillis: Long,
    val direction: CallDirection,
    val result: CallResult,
    val durationMillis: Long?
)

fun CallEventEntity.toCallEvent(): CallEvent = CallEvent(
    id = id,
    sessionCode = sessionCode,
    timestampMillis = timestampMillis,
    direction = direction,
    result = result,
    durationMillis = durationMillis
)

fun CallEvent.toEntity(): CallEventEntity = CallEventEntity(
    id = id,
    sessionCode = sessionCode,
    timestampMillis = timestampMillis,
    direction = direction,
    result = result,
    durationMillis = durationMillis
)
