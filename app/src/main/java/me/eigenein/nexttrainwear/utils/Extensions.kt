package me.eigenein.nexttrainwear.utils

import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import me.eigenein.nexttrainwear.exceptions.GoogleApiClientConnectionFailedException
import me.eigenein.nexttrainwear.exceptions.LocationRequestFailedException
import me.eigenein.nexttrainwear.exceptions.NoLocationException
import org.simpleframework.xml.transform.RegistryMatcher
import java.util.*

operator fun Date.minus(other: Date): Long {
    return this.time - other.time
}

/**
 * Make an observable from Google API client builder emitting the built client when connected.
 */
fun GoogleApiClient.Builder.asFlowable(): Flowable<GoogleApiClient> {
    return Flowable.create({ emitter ->
        var client: GoogleApiClient? = null
        client = this
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                    emitter.onNext(client!!)
                    emitter.onComplete()
                }
                override fun onConnectionSuspended(i: Int) = Unit
            })
            .addOnConnectionFailedListener { emitter.onError(GoogleApiClientConnectionFailedException(it.errorMessage)) }
            .build()
        client.connect()
    }, BackpressureStrategy.ERROR)
}

/**
 * Make an observable from the location request emitting location updates.
 */
fun LocationRequest.asFlowable(googleApiClient: GoogleApiClient): Flowable<Location> {
    return Flowable.create({ emitter ->
        val listener = { location: Location? ->
            if (location != null) {
                emitter.onNext(location)
            } else {
                emitter.onError(NoLocationException("Location is null"))
            }
        }
        LocationServices.FusedLocationApi
            .requestLocationUpdates(googleApiClient, this, listener)
            .setResultCallback {
                if (!it.isSuccess) {
                    emitter.onError(LocationRequestFailedException(it.statusMessage))
                }
            }
        emitter.setCancellable { LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, listener) }
    }, BackpressureStrategy.BUFFER)
}

fun Handler.asFlowable(delayMillis: Long, emitInstantly: Boolean): Flowable<Unit> {
    return Flowable.create({
        val runnable = object : Runnable {
            override fun run() {
                try {
                    it.onNext(Unit)
                } finally {
                    scheduleNext()
                }
            }

            fun scheduleNext() = this@asFlowable.postDelayed(this, delayMillis)
        }

        it.setCancellable { this.removeCallbacks(runnable) }

        if (emitInstantly) {
            runnable.run()
        } else {
            runnable.scheduleNext()
        }
    }, BackpressureStrategy.DROP)
}

fun <TViewHolder : RecyclerView.ViewHolder> RecyclerView.findFirstVisibleViewHolder() : TViewHolder? {
    val position = (this.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    if (position != RecyclerView.NO_POSITION) {
        @Suppress("UNCHECKED_CAST")
        return this.findViewHolderForAdapterPosition(position) as TViewHolder
    } else {
        return null
    }
}

fun <TViewHolder : RecyclerView.ViewHolder> RecyclerView.findVisibleViewHolders() : List<TViewHolder> {
    val firstPosition = (this.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
    val lastPosition = (this.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
    if (firstPosition != RecyclerView.NO_POSITION && lastPosition != RecyclerView.NO_POSITION) {
        return (firstPosition..lastPosition).map {
            @Suppress("UNCHECKED_CAST")
            this.findViewHolderForAdapterPosition(it) as TViewHolder
        }
    } else {
        return emptyList()
    }
}

/**
 * Bundle builder.
 */
fun bundle(init: Bundle.() -> Unit): Bundle {
    val bundle = Bundle()
    bundle.init()
    return bundle
}

/**
 * Matcher builder.
 */
fun registryMatcher(init: RegistryMatcher.() -> Unit): RegistryMatcher {
    val matcher = RegistryMatcher()
    matcher.init()
    return matcher
}
