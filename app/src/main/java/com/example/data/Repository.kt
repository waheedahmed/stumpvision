package com.example.data

import kotlinx.coroutines.flow.Flow

class StumpRepository(
    private val sessionDao: SessionDao,
    private val deliveryDao: DeliveryDao
) {
    val allSessions: Flow<List<SessionEntity>> = sessionDao.getAllSessions()

    fun getSessionById(sessionId: Int): Flow<SessionEntity?> {
        return sessionDao.getSessionById(sessionId)
    }

    fun getDeliveriesForSession(sessionId: Int): Flow<List<DeliveryEntity>> {
        return deliveryDao.getDeliveriesForSession(sessionId)
    }

    suspend fun insertSession(session: SessionEntity): Int {
        return sessionDao.insertSession(session).toInt()
    }

    suspend fun updateSession(session: SessionEntity) {
        sessionDao.updateSession(session)
    }

    suspend fun deleteSession(session: SessionEntity) {
        sessionDao.deleteSession(session)
    }

    suspend fun insertDelivery(delivery: DeliveryEntity): Int {
        return deliveryDao.insertDelivery(delivery).toInt()
    }

    suspend fun updateDelivery(delivery: DeliveryEntity) {
        deliveryDao.updateDelivery(delivery)
    }

    suspend fun deleteDelivery(delivery: DeliveryEntity) {
        deliveryDao.deleteDelivery(delivery)
    }
}
