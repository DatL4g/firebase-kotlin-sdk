package dev.gitlive.firebase.perf.metrics

import cocoapods.FirebasePerformance.FIRTrace

public actual class Trace internal constructor(public val apple: FIRTrace?) {

    public actual fun start() {
        apple?.start()
    }

    public actual fun stop() {
        apple?.stop()
    }

    public actual fun getLongMetric(metricName: String): Long = apple?.valueForIntMetric(metricName) ?: 0L

    public actual fun incrementMetric(metricName: String, incrementBy: Long) {
        apple?.incrementMetric(metricName, incrementBy)
    }

    public actual fun putMetric(metricName: String, value: Long) {
        apple?.setIntValue(value, metricName)
    }
}
