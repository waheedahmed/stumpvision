package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

sealed class AppScreen {
    object SessionsList : AppScreen()
    object SetupGuide : AppScreen()
    object LiveCapture : AppScreen()
    object DeliveryDetail : AppScreen()
    object DrsReview : AppScreen()
    object Dashboard : AppScreen()
}

data class StumpUiState(
    val currentScreen: AppScreen = AppScreen.SessionsList,
    val sessionsList: List<SessionEntity> = emptyList(),
    val currentSession: SessionEntity? = null,
    val deliveries: List<DeliveryEntity> = emptyList(),
    val selectedDelivery: DeliveryEntity? = null,
    
    // Setup and Settings
    val bowlerNameInput: String = "Jimmy",
    val sessionNameInput: String = "Seam Practice",
    val bowlerType: String = "Fast", // "Spinner", "Fast"
    val stumpGridOpacity: Float = 0.6f,
    val calibrationOffset: Float = 0f,
    
    // Live detection overlay simulation states
    val isRecording: Boolean = true,
    val isAutoDetectEnabled: Boolean = true,
    val isSimulationActive: Boolean = false,
    val simProgress: Float = 0f, // 0f to 1f ball animation progress
    val showClipDetectedOverlay: Boolean = false,
    val detectedSpeed: Float = 0f,
    
    // AI Coach Insights state
    val coachFeedback: String = "",
    val isAnalyzing: Boolean = false
)

class StumpViewModel(application: Application) : AndroidViewModel(application) {
    private val db = StumpDatabase.getDatabase(application)
    private val repository = StumpRepository(db.sessionDao, db.deliveryDao)

    private val _uiState = MutableStateFlow(StumpUiState())
    val uiState: StateFlow<StumpUiState> = _uiState.asStateFlow()

    init {
        // Observe all sessions reactively
        viewModelScope.launch {
            repository.allSessions.collect { list ->
                _uiState.update { it.copy(sessionsList = list) }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
        
        // If navigating to Dashboard or SessionsList, refresh relevant flows
        if (screen is AppScreen.Dashboard) {
            val session = _uiState.value.currentSession
            if (session != null) {
                fetchDeliveries(session.id)
                fetchCoachingFeedback(session)
            }
        }
    }

    fun setBowlerName(name: String) {
        _uiState.update { it.copy(bowlerNameInput = name) }
    }

    fun setSessionName(name: String) {
        _uiState.update { it.copy(sessionNameInput = name) }
    }

    fun setBowlerType(type: String) {
        val defaultName = when (type) {
            "Fast" -> "Seam Practice"
            "Off Spinner" -> "Off Spin Practice"
            "Leg Spinner" -> "Leg Spin Practice"
            else -> "Bowl Practice"
        }
        _uiState.update { state ->
            val curName = state.sessionNameInput
            val shouldUpdateName = curName == "Seam Practice" || 
                                   curName == "Off Spin Practice" || 
                                   curName == "Leg Spin Practice" || 
                                   curName.isBlank()
            state.copy(
                bowlerType = type,
                sessionNameInput = if (shouldUpdateName) defaultName else curName
            )
        }
    }

    fun updateGridOpacity(opacity: Float) {
        _uiState.update { it.copy(stumpGridOpacity = opacity) }
    }

    fun updateCalibration(offset: Float) {
        _uiState.update { it.copy(calibrationOffset = offset) }
    }

    fun selectDelivery(delivery: DeliveryEntity) {
        _uiState.update { it.copy(selectedDelivery = delivery) }
    }

    fun selectSession(session: SessionEntity) {
        _uiState.update { it.copy(currentSession = session, coachFeedback = session.coachNotes) }
        fetchDeliveries(session.id)
    }

    private fun fetchDeliveries(sessionId: Int) {
        viewModelScope.launch {
            repository.getDeliveriesForSession(sessionId).collect { list ->
                _uiState.update { it.copy(deliveries = list) }
            }
        }
    }

    fun deleteSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.deleteSession(session)
            if (_uiState.value.currentSession?.id == session.id) {
                _uiState.update { it.copy(currentSession = null, deliveries = emptyList()) }
            }
        }
    }

    fun startNewSession() {
        viewModelScope.launch {
            val newSession = SessionEntity(
                bowlerName = _uiState.value.bowlerNameInput.ifBlank { "Unassigned Bowler" },
                sessionName = _uiState.value.sessionNameInput.ifBlank { "Bowl Practice" }
            )
            val insertedId = repository.insertSession(newSession)
            
            // Reload with newly created session
            val sessionWithId = newSession.copy(id = insertedId)
            _uiState.update { it.copy(currentSession = sessionWithId, deliveries = emptyList()) }
            fetchDeliveries(insertedId)
            navigateTo(AppScreen.SetupGuide)
        }
    }

    fun toggleCameraRecording() {
        val isNowRecording = !_uiState.value.isRecording
        _uiState.update { it.copy(isRecording = isNowRecording) }
    }

    fun toggleAutoDetect() {
        val isNowEnabled = !_uiState.value.isAutoDetectEnabled
        _uiState.update { it.copy(isAutoDetectEnabled = isNowEnabled) }
    }

    fun simulateAutomaticDelivery() {
        if (_uiState.value.isSimulationActive) return
        val currentSessionId = _uiState.value.currentSession?.id ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSimulationActive = true, simProgress = 0f, showClipDetectedOverlay = false) }
            
            // Step 1: Ball flight trajectory simulation progress (3D layout tracking)
            val flightDuration = 1200L // ms
            val increments = 24
            for (i in 1..increments) {
                delay(flightDuration / increments)
                _uiState.update { it.copy(simProgress = i.toFloat() / increments) }
            }
            
            // Step 2: Pitch Impact visual pop-up, auto-clip detection trigger
            val speed = if (_uiState.value.bowlerType == "Fast") {
                Random.nextDouble(128.0, 148.5).toFloat()
            } else {
                Random.nextDouble(78.0, 96.0).toFloat()
            }
            
            _uiState.update { 
                it.copy(
                    showClipDetectedOverlay = true,
                    isSimulationActive = false,
                    detectedSpeed = speed
                )
            }
            
            // Construct delivery results
            val deliveryNum = _uiState.value.deliveries.size + 1
            val isSpinner = _uiState.value.bowlerType.contains("Spinner", ignoreCase = true)
            
            val swingType = if (isSpinner) "Straight" else listOf("In-Swing", "Out-Swing", "Straight").random()
            val seamMovement = if (isSpinner) listOf("Off-break", "Leg-break").random() else listOf("Off-cutter", "Leg-cutter", "None").random()
            val spinRate = if (isSpinner) Random.nextInt(1400, 2400) else Random.nextInt(100, 350)
            
            // Landing point logic: X in (0.35 - 0.65) is stumps width. Y in (0.1 - 0.45) is ideal length.
            val pitchX = Random.nextDouble(0.32, 0.68).toFloat()
            val pitchY = Random.nextDouble(0.12, 0.48).toFloat()
            
            // Deduce Line / Length label
            val lengthStr = when {
                pitchY < 0.22f -> "Full Pitch / Yorker"
                pitchY < 0.35f -> "Good Length"
                else -> "Short Pitch / Bouncer"
            }
            val lineStr = when {
                pitchX < 0.42f -> "Off stump channel"
                pitchX > 0.58f -> "Leg-side line"
                else -> "Middle stump line"
            }
            val lineLengthLabel = "$lengthStr ($lineStr)"
            val angle = Random.nextDouble(6.0, 14.0).toFloat()
            
            // Handle DRS outcomes
            val isReviewable = Random.nextBoolean()
            val verdict = if (pitchX in 0.43f..0.57f && pitchY < 0.28f) "OUT (LBW)" else "NOT OUT (Leg Side / Missing)"
            val confidence = Random.nextDouble(85.0, 99.8).toFloat()
            
            // Edge trigger
            val isEdge = Random.nextDouble() < 0.35f
            
            // Audio wave synthetic spike
            val baseWave = Array(100) { Random.nextDouble(-0.1, 0.1).toFloat() }
            if (isEdge) {
                // Insert high spike at frame 45 (bat ball collision)
                baseWave[44] = 0.85f
                baseWave[45] = -0.92f
                baseWave[46] = 0.74f
                baseWave[47] = -0.55f
            }
            // Stump strike spike
            if (verdict.startsWith("OUT")) {
                baseWave[80] = 0.62f
                baseWave[81] = -0.73f
                baseWave[82] = 0.45f
            }
            val audioDataString = baseWave.joinToString(",") { String.format("%.2f", it) }

            val newDelivery = DeliveryEntity(
                sessionId = currentSessionId,
                deliveryNum = deliveryNum,
                speedKmph = speed,
                swingType = swingType,
                seamMovement = seamMovement,
                spinRateRpm = spinRate,
                pitchLocationX = pitchX,
                pitchLocationY = pitchY,
                lineLengthClass = lineLengthLabel,
                bounceAngle = angle,
                isDrsReviewed = isReviewable,
                drsVerdict = verdict,
                drsConfidence = confidence,
                isEdgeDetected = isEdge,
                wasBoundary = Random.nextDouble() < 0.12f,
                wasDotBall = !isEdge && Random.nextDouble() < 0.65f,
                audioWaveform = audioDataString
            )

            // Save inside database
            repository.insertDelivery(newDelivery)
            
            // Re-fetch calculations to update session summary metric blocks
            _uiState.update { state ->
                val updatedList = state.deliveries + newDelivery
                val avgSpeed = updatedList.map { it.speedKmph }.average().toFloat()
                
                val dotBalls = updatedList.count { it.wasDotBall }
                val boundaries = updatedList.count { it.wasBoundary }
                val dPercent = (dotBalls.toFloat() / updatedList.size) * 100f
                val bPercent = (boundaries.toFloat() / updatedList.size) * 100f
                
                // Estimate Economy (mocked calculations based on delivery samples)
                val totalRuns = updatedList.sumOf { 
                    if (it.wasBoundary) 4 else if (it.wasDotBall) 0 else 1 
                }
                val overs = updatedList.size / 6f
                val economy = if (overs > 0f) totalRuns / overs else 0f

                state.currentSession?.let { current ->
                    val updatedSession = current.copy(
                        totalDeliveries = updatedList.size,
                        averageSpeed = avgSpeed,
                        economyRate = economy,
                        dotBallPercentage = dPercent,
                        boundaryPercentage = bPercent
                    )
                    viewModelScope.launch {
                        repository.updateSession(updatedSession)
                    }
                    state.copy(currentSession = updatedSession, deliveries = updatedList)
                } ?: state
            }
            
            delay(1500)
            _uiState.update { it.copy(showClipDetectedOverlay = false) }
        }
    }

    fun fetchCoachingFeedback(session: SessionEntity) {
        if (_uiState.value.isAnalyzing) return
        _uiState.update { it.copy(isAnalyzing = true) }
        
        viewModelScope.launch(Dispatchers.IO) {
            val list = _uiState.value.deliveries
            if (list.isEmpty()) {
                _uiState.update { it.copy(coachFeedback = "Complete some bowling simulation deliveries to get real-time AI Coach analysis!", isAnalyzing = false) }
                return@launch
            }
            
            // Format deliveries summary
            val summary = list.joinToString("; ") { d ->
                "Ball ${d.deliveryNum}: ${String.format("%.1f", d.speedKmph)}km/h, ${d.swingType}, Seam: ${d.seamMovement}, Landing: ${d.lineLengthClass}, Edge: ${d.isEdgeDetected}, DRS: ${d.drsVerdict}"
            }
            
            val feedback = GeminiClient.fetchBowlingCoaching(summary, _uiState.value.bowlerType)
            
            // Save feedback within session notes
            val sessionWithNotes = session.copy(coachNotes = feedback)
            repository.updateSession(sessionWithNotes)
            
            _uiState.update { 
                it.copy(
                    currentSession = sessionWithNotes, 
                    coachFeedback = feedback, 
                    isAnalyzing = false
                ) 
            }
        }
    }
}
