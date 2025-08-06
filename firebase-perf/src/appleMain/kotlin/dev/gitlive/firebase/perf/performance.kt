package dev.gitlive.firebase.perf

import cocoapods.FirebasePerformance.FIRPerformance
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import dev.gitlive.firebase.FirebaseException
import dev.gitlive.firebase.perf.metrics.Trace

public actual val Firebase.performance: FirebasePerformance get() =
    FirebasePerformance(FIRPerformance.sharedInstance())

public actual fun Firebase.performance(app: FirebaseApp): FirebasePerformance = FirebasePerformance(FIRPerformance.sharedInstance())

public actual class FirebasePerformance(public val apple: FIRPerformance) {

    public actual fun newTrace(traceName: String): Trace = Trace(apple.traceWithName(traceName))

    public actual fun isPerformanceCollectionEnabled(): Boolean = apple.isDataCollectionEnabled()

    public actual fun setPerformanceCollectionEnabled(enable: Boolean) {
        apple.dataCollectionEnabled = enable
    }
}

public actual open class FirebasePerformanceException(message: String) : FirebaseException(message)
