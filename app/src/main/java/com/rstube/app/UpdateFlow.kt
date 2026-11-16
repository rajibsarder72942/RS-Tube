package com.rstube.app

/** User consent and foreground visibility gate the automatic installer handoff. */
object UpdateFlow {
    fun shouldCheck(now: Long, previousAttempt: Long): Boolean =
        previousAttempt == 0L || now < previousAttempt || now - previousAttempt >= 30_000L

    fun shouldOfferInstaller(verified: Boolean, requestedDownload: Boolean, foreground: Boolean): Boolean =
        verified && requestedDownload && foreground
}
