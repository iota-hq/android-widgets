package com.papercut.collage.segmentation

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * On-device background removal via ML Kit Subject Segmentation.
 *
 * Input: a source bitmap. Output: an ARGB_8888 bitmap where the subject is
 * opaque and everything else is transparent. This is the clean matte that the
 * [com.papercut.collage.render.PaperEdgeProcessor] then turns into a torn
 * paper cutout.
 *
 * The model downloads once via Play services (see manifest DEPENDENCIES meta).
 * Keep this interface model-agnostic so a bundled U²-Net/MODNet path can be
 * dropped in later behind a "High quality" toggle (see spec §4).
 */
class SubjectSegmenter {

    private val client by lazy {
        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
        SubjectSegmentation.getClient(options)
    }

    /** Returns the subject as a transparent-background bitmap, or null on failure. */
    suspend fun cutout(source: Bitmap): Bitmap? =
        suspendCancellableCoroutine { cont ->
            val input = InputImage.fromBitmap(source, 0)
            client.process(input)
                .addOnSuccessListener { result ->
                    cont.resume(result.foregroundBitmap)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

    fun close() = client.close()
}
