package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long = System.currentTimeMillis(),
    val bowlerName: String,
    val sessionName: String,
    val totalDeliveries: Int = 0,
    val averageSpeed: Float = 0f,
    val economyRate: Float = 0f,
    val dotBallPercentage: Float = 0f,
    val boundaryPercentage: Float = 0f,
    val coachNotes: String = ""
)

@Entity(
    tableName = "deliveries",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class DeliveryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val deliveryNum: Int,
    val speedKmph: Float,
    val swingType: String, // "In-Swing", "Out-Swing", "Straight"
    val seamMovement: String, // "Leg-cutter", "Off-cutter", "None"
    val spinRateRpm: Int, // 0 for fast, or 1000-2500 for spinners
    val pitchLocationX: Float, // 0f to 1f (width of pitch)
    val pitchLocationY: Float, // 0f to 1f (length towards batsman stumps, 0 is crease, 1 is bowler end)
    val lineLengthClass: String, // e.g. "Good Length, Middle stump"
    val bounceAngle: Float, // degrees
    val isDrsReviewed: Boolean = false,
    val drsVerdict: String = "NOT_REVIEWED", // "OUT", "NOT_OUT", "UMPIRES_CALL"
    val drsConfidence: Float = 0f, // percentage
    val isEdgeDetected: Boolean = false,
    val wasBoundary: Boolean = false,
    val wasDotBall: Boolean = true,
    val audioWaveform: String = "" // comma-separated float amplitudes for Snicko
)
