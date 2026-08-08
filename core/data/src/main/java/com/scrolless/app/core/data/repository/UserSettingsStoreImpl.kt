/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.core.data.repository

import com.scrolless.app.core.data.database.dao.UserSettingsDao
import com.scrolless.app.core.model.BlockOption
import com.scrolless.app.core.repository.UserSettingsStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A data repository implementation for [UserSettingsStore].
 */
class UserSettingsStoreImpl @Inject constructor(private val userSettingsDao: UserSettingsDao) : UserSettingsStore {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _activeBlockOption = MutableStateFlow(BlockOption.NothingSelected)
    private val _timeLimit = MutableStateFlow(0L)
    private val _intervalLength = MutableStateFlow(0L)
    private val _intervalWindowStart = MutableStateFlow(0L)
    private val _intervalUsage = MutableStateFlow(0L)
    private val _timerOverlayEnabled = MutableStateFlow(false)
    private val _timerOverlayPositionY = MutableStateFlow(0)
    private val _timerOverlayPositionX = MutableStateFlow(0)
    private val _waitingForAccessibility = MutableStateFlow(false)
    private val _hasSeenAccessibilityExplainer = MutableStateFlow(false)
    private val _hasSeenReviewPrompt = MutableStateFlow(false)
    private val _reviewPromptAttemptCount = MutableStateFlow(0)
    private val _reviewPromptLastAttemptAt = MutableStateFlow(0L)
    private val _pauseUntil = MutableStateFlow(0L)
    private val _firstLaunchAt = MutableStateFlow(-1L)
    private val _pauseDuration = MutableStateFlow(5 * 60 * 1000L)
    private val _exceptReelsSentByDm = MutableStateFlow(false)
    private val _partnerQuotaWindowKey = MutableStateFlow("")
    private val _partnerQuotaUsedMillis = MutableStateFlow(0L)
    private val _partnerQuotaGrantedMillis = MutableStateFlow(0L)
    private val _partnerQuotaAnchorWall = MutableStateFlow(0L)
    private val _partnerQuotaAnchorElapsed = MutableStateFlow(0L)
    private val _partnerQuotaAnchorBoot = MutableStateFlow(-1)
    private val _instagramFeedBlockingEnabled = MutableStateFlow(false)
    private val _strictUntilAt = MutableStateFlow(0L)
    private val _strictAnchorWall = MutableStateFlow(0L)
    private val _strictAnchorElapsed = MutableStateFlow(0L)
    private val _strictAnchorBoot = MutableStateFlow(-1)
    private val _minimalModeEnabled = MutableStateFlow(false)
    private val _minimalAnchorWall = MutableStateFlow(0L)
    private val _minimalAnchorElapsed = MutableStateFlow(0L)
    private val _minimalAnchorBoot = MutableStateFlow(-1)

    init {
        coroutineScope.launch {
            userSettingsDao.getActiveBlockOption().collect { _activeBlockOption.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimeLimit().collect { _timeLimit.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalLength().collect { _intervalLength.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalWindowStart().collect { _intervalWindowStart.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getFirstLaunchAt().collect { _firstLaunchAt.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getHasSeenReviewPrompt().collect { _hasSeenReviewPrompt.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getReviewPromptAttemptCount().collect { _reviewPromptAttemptCount.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getReviewPromptLastAttemptAt().collect { _reviewPromptLastAttemptAt.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getIntervalUsage().collect { _intervalUsage.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayEnabled().collect { _timerOverlayEnabled.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayPositionY().collect { _timerOverlayPositionY.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getTimerOverlayPositionX().collect { _timerOverlayPositionX.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getWaitingForAccessibility().collect { _waitingForAccessibility.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getHasSeenAccessibilityExplainer().collect { _hasSeenAccessibilityExplainer.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPauseUntil().collect { _pauseUntil.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPauseDuration().collect { _pauseDuration.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getExceptReelsSentByDm().collect { _exceptReelsSentByDm.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaWindowKey().collect { _partnerQuotaWindowKey.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaUsedMillis().collect { _partnerQuotaUsedMillis.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaGrantedMillis().collect { _partnerQuotaGrantedMillis.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaAnchorWall().collect { _partnerQuotaAnchorWall.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaAnchorElapsed().collect { _partnerQuotaAnchorElapsed.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getPartnerQuotaAnchorBoot().collect { _partnerQuotaAnchorBoot.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getInstagramFeedBlockingEnabled().collect { _instagramFeedBlockingEnabled.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getStrictUntil().collect { _strictUntilAt.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getStrictAnchorWall().collect { _strictAnchorWall.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getStrictAnchorElapsed().collect { _strictAnchorElapsed.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getStrictAnchorBoot().collect { _strictAnchorBoot.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getMinimalModeEnabled().collect { _minimalModeEnabled.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getMinimalAnchorWall().collect { _minimalAnchorWall.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getMinimalAnchorElapsed().collect { _minimalAnchorElapsed.value = it }
        }
        coroutineScope.launch {
            userSettingsDao.getMinimalAnchorBoot().collect { _minimalAnchorBoot.value = it }
        }
    }

    override fun getActiveBlockOption(): Flow<BlockOption> = _activeBlockOption

    override suspend fun setActiveBlockOption(blockOption: BlockOption) {
        _activeBlockOption.value = blockOption
        userSettingsDao.setActiveBlockOption(blockOption)
    }

    override fun getTimeLimit(): Flow<Long> = _timeLimit

    override suspend fun setTimeLimit(timeLimit: Long) {
        _timeLimit.value = timeLimit
        userSettingsDao.setTimeLimit(timeLimit)
    }

    override suspend fun setIntervalLength(intervalLength: Long) {
        _intervalLength.value = intervalLength
        userSettingsDao.setIntervalLength(intervalLength)
    }

    override fun getIntervalLength(): Flow<Long> = _intervalLength

    override fun getIntervalWindowStart(): Flow<Long> = _intervalWindowStart

    override suspend fun setIntervalWindowStart(windowStart: Long) {
        _intervalWindowStart.value = windowStart
        userSettingsDao.setIntervalWindowStart(windowStart)
    }

    override fun getIntervalUsage(): Flow<Long> = _intervalUsage

    override suspend fun setIntervalUsage(usage: Long) {
        _intervalUsage.value = usage
        userSettingsDao.setIntervalUsage(usage)
    }

    override suspend fun updateIntervalState(windowStart: Long, usage: Long) {
        _intervalWindowStart.value = windowStart
        _intervalUsage.value = usage
        userSettingsDao.updateIntervalState(windowStart, usage)
    }

    override suspend fun setTimerOverlayToggle(enabled: Boolean) {
        _timerOverlayEnabled.value = enabled
        userSettingsDao.setTimerOverlayEnabled(enabled)
    }

    override fun getTimerOverlayEnabled(): Flow<Boolean> = _timerOverlayEnabled

    override fun getTimerOverlayPositionY(): Flow<Int> = _timerOverlayPositionY

    override suspend fun setTimerOverlayPositionY(positionY: Int) {
        _timerOverlayPositionY.value = positionY
        userSettingsDao.setTimerOverlayPositionY(positionY)
    }

    override fun getTimerOverlayPositionX(): Flow<Int> = _timerOverlayPositionX

    override suspend fun setTimerOverlayPositionX(positionX: Int) {
        _timerOverlayPositionX.value = positionX
        userSettingsDao.setTimerOverlayPositionX(positionX)
    }

    override fun getWaitingForAccessibility(): Flow<Boolean> = _waitingForAccessibility

    override suspend fun setWaitingForAccessibility(waiting: Boolean) {
        _waitingForAccessibility.value = waiting
        userSettingsDao.setWaitingForAccessibility(waiting)
    }

    override fun getHasSeenAccessibilityExplainer(): Flow<Boolean> = _hasSeenAccessibilityExplainer

    override suspend fun setHasSeenAccessibilityExplainer(seen: Boolean) {
        _hasSeenAccessibilityExplainer.value = seen
        userSettingsDao.setHasSeenAccessibilityExplainer(seen)
    }

    override fun getPauseUntil(): Flow<Long> = _pauseUntil

    override suspend fun setPauseUntil(pauseUntil: Long) {
        _pauseUntil.value = pauseUntil
        userSettingsDao.setPauseUntil(pauseUntil)
    }

    override fun getPauseDuration(): Flow<Long> = _pauseDuration

    override suspend fun setPauseDuration(durationMillis: Long) {
        _pauseDuration.value = durationMillis
        userSettingsDao.setPauseDuration(durationMillis)
    }

    override fun getExceptReelsSentByDm(): Flow<Boolean> = _exceptReelsSentByDm

    override suspend fun setExceptReelsSentByDm(checked: Boolean) {
        _exceptReelsSentByDm.value = checked
        userSettingsDao.setExceptReelsSentByDm(checked)
    }

    override fun getFirstLaunchAt(): Flow<Long> = _firstLaunchAt

    override fun getFirstLaunchDate(): Flow<LocalDate?> = _firstLaunchAt.map { firstLaunchAt ->
        if (firstLaunchAt > 0L) {
            Instant.ofEpochMilli(firstLaunchAt).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            null
        }
    }

    override fun getHasSeenReviewPrompt(): Flow<Boolean> = _hasSeenReviewPrompt

    override suspend fun setHasSeenReviewPrompt(seen: Boolean) {
        _hasSeenReviewPrompt.value = seen
        userSettingsDao.setHasSeenReviewPrompt(seen)
    }

    override fun getReviewPromptAttemptCount(): Flow<Int> = _reviewPromptAttemptCount

    override suspend fun setReviewPromptAttemptCount(count: Int) {
        _reviewPromptAttemptCount.value = count
        userSettingsDao.setReviewPromptAttemptCount(count)
    }

    override fun getReviewPromptLastAttemptAt(): Flow<Long> = _reviewPromptLastAttemptAt

    override suspend fun setReviewPromptLastAttemptAt(timestamp: Long) {
        _reviewPromptLastAttemptAt.value = timestamp
        userSettingsDao.setReviewPromptLastAttemptAt(timestamp)
    }

    override fun getPartnerQuotaWindowKey(): Flow<String> = _partnerQuotaWindowKey

    override fun getPartnerQuotaUsedMillis(): Flow<Long> = _partnerQuotaUsedMillis

    override fun getPartnerQuotaGrantedMillis(): Flow<Long> = _partnerQuotaGrantedMillis

    override fun getPartnerQuotaAnchorWall(): Flow<Long> = _partnerQuotaAnchorWall

    override fun getPartnerQuotaAnchorElapsed(): Flow<Long> = _partnerQuotaAnchorElapsed

    override fun getPartnerQuotaAnchorBoot(): Flow<Int> = _partnerQuotaAnchorBoot

    override suspend fun updatePartnerQuotaState(
        windowKey: String,
        usedMillis: Long,
        grantedMillis: Long,
        anchorWallMillis: Long,
        anchorElapsedMillis: Long,
        anchorBootCount: Int,
    ) {
        _partnerQuotaWindowKey.value = windowKey
        _partnerQuotaUsedMillis.value = usedMillis
        _partnerQuotaGrantedMillis.value = grantedMillis
        _partnerQuotaAnchorWall.value = anchorWallMillis
        _partnerQuotaAnchorElapsed.value = anchorElapsedMillis
        _partnerQuotaAnchorBoot.value = anchorBootCount
        userSettingsDao.updatePartnerQuotaState(
            windowKey = windowKey,
            usedMillis = usedMillis,
            grantedMillis = grantedMillis,
            anchorWallMillis = anchorWallMillis,
            anchorElapsedMillis = anchorElapsedMillis,
            anchorBootCount = anchorBootCount,
        )
    }

    override suspend fun addPartnerQuotaGrant(deltaMillis: Long) {
        // DB is authoritative for the increment; the collector refreshes the cached flow.
        userSettingsDao.addPartnerQuotaGrant(deltaMillis)
    }

    override fun getInstagramFeedBlockingEnabled(): Flow<Boolean> = _instagramFeedBlockingEnabled

    override suspend fun setInstagramFeedBlockingEnabled(enabled: Boolean) {
        _instagramFeedBlockingEnabled.value = enabled
        userSettingsDao.setInstagramFeedBlockingEnabled(enabled)
    }

    override fun getStrictUntil(): Flow<Long> = _strictUntilAt

    override fun getStrictAnchorWall(): Flow<Long> = _strictAnchorWall

    override fun getStrictAnchorElapsed(): Flow<Long> = _strictAnchorElapsed

    override fun getStrictAnchorBoot(): Flow<Int> = _strictAnchorBoot

    override suspend fun updateStrictModeState(
        strictUntilAt: Long,
        anchorWallMillis: Long,
        anchorElapsedMillis: Long,
        anchorBootCount: Int,
    ) {
        _strictUntilAt.value = strictUntilAt
        _strictAnchorWall.value = anchorWallMillis
        _strictAnchorElapsed.value = anchorElapsedMillis
        _strictAnchorBoot.value = anchorBootCount
        userSettingsDao.updateStrictModeState(
            strictUntilAt = strictUntilAt,
            anchorWallMillis = anchorWallMillis,
            anchorElapsedMillis = anchorElapsedMillis,
            anchorBootCount = anchorBootCount,
        )
    }

    override fun getMinimalModeEnabled(): Flow<Boolean> = _minimalModeEnabled

    override suspend fun setMinimalModeEnabled(enabled: Boolean) {
        _minimalModeEnabled.value = enabled
        userSettingsDao.setMinimalModeEnabled(enabled)
    }

    override fun getMinimalAnchorWall(): Flow<Long> = _minimalAnchorWall

    override fun getMinimalAnchorElapsed(): Flow<Long> = _minimalAnchorElapsed

    override fun getMinimalAnchorBoot(): Flow<Int> = _minimalAnchorBoot

    override suspend fun anchorMinimalModeIfNeeded(anchorWallMillis: Long, anchorElapsedMillis: Long, anchorBootCount: Int) {
        // No cache write: the DB decides whether the anchor moves, and the init collectors
        // above mirror the result back. Pre-empting it here would defeat the whole point.
        userSettingsDao.anchorMinimalModeIfNeeded(
            anchorWallMillis = anchorWallMillis,
            anchorElapsedMillis = anchorElapsedMillis,
            anchorBootCount = anchorBootCount,
        )
    }
}
