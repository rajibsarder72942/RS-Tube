package com.rstube.app

import org.junit.Assert.*
import org.junit.Test

class UpdateFlowTest {
    @Test fun checksOnFirstOpenAndAgainAfterForegroundThrottle() {
        assertTrue(UpdateFlow.shouldCheck(100_000, 0))
        assertFalse(UpdateFlow.shouldCheck(100_010, 100_000))
        assertTrue(UpdateFlow.shouldCheck(130_000, 100_000))
        assertTrue(UpdateFlow.shouldCheck(50_000, 100_000))
    }
    @Test fun installerRequiresVerifiedUserRequestedDownloadAndForeground() {
        assertTrue(UpdateFlow.shouldOfferInstaller(true, true, true))
        assertFalse(UpdateFlow.shouldOfferInstaller(false, true, true))
        assertFalse(UpdateFlow.shouldOfferInstaller(true, false, true))
        assertFalse(UpdateFlow.shouldOfferInstaller(true, true, false))
    }
}
