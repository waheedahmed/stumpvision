package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.DeliveryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.hypot
import kotlin.random.Random

object VideoExportHelper {

    suspend fun exportDeliveryVideo(
        context: Context,
        delivery: DeliveryEntity,
        bowlerName: String,
        bowlerType: String,
        onProgress: (Float) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val width = 480
        val height = 640
        val frameRate = 30
        val totalFrames = 75 // 2.5 seconds video duration

        val tempFile = File(context.cacheDir, "temp_tracked_video_${delivery.id}.mp4")
        if (tempFile.exists()) tempFile.delete()

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null

        try {
            onProgress(0.05f)

            // 1. Prepare Media Encoder Formats
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height)
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            format.setInteger(MediaFormat.KEY_BIT_RATE, 1200000) // 1.2 Mbps
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)

            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            // 2. Trajectory coordinate vectors calculation
            val center = width / 2f
            
            // 3D Pitch boundaries mapping
            val pitchLeft0 = center - 60f
            val pitchRight0 = center + 60f
            val pitchLeft1 = center - 160f
            val pitchRight1 = center + 160f
            
            val pitchTopY = 200f
            val pitchBotY = 520f

            // Ball flight parameters
            val x0 = center + 30f // bowler release point slightly offset
            val y0 = 530f
            val z0 = 110f // release height

            // landing coordinates
            val xb = center + (delivery.pitchLocationX - 0.5f) * 180f
            val yb = pitchTopY + (delivery.pitchLocationY * (pitchBotY - pitchTopY))
            val zb = 0f // impact turf

            // exit coordinates
            val deflection = if (delivery.swingType == "In-Swing") -35f else if (delivery.swingType == "Out-Swing") 35f else 0f
            val xe = xb + deflection
            val ye = 120f // behind stumps
            val ze = 55f // bounce height

            val bounceIndex = (totalFrames * 0.65f).toInt() // Frame 48 bounce

            // 3. Thread frame rendering pipeline
            val bufferInfo = MediaCodec.BufferInfo()

            for (i in 0 until totalFrames) {
                // Render Canvas
                val canvas = inputSurface.lockCanvas(null)
                try {
                    drawFrameOnCanvas(
                        canvas, width, height, i, totalFrames, bounceIndex,
                        x0, y0, z0, xb, yb, zb, xe, ye, ze,
                        delivery, bowlerName, bowlerType, center,
                        pitchLeft0, pitchRight0, pitchLeft1, pitchRight1, pitchTopY, pitchBotY
                    )
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }

                // Retrieve encoded bytes
                while (true) {
                    val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 2000L)
                    if (outputBufferId >= 0) {
                        val encodedData = codec.getOutputBuffer(outputBufferId) ?: break
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                throw IllegalStateException("Muxer hasn't started yet")
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            // Write frame timestamp
                            bufferInfo.presentationTimeUs = (i * 1000000L / frameRate)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outputBufferId, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else {
                        break
                    }
                }

                if (i % 5 == 0) {
                    onProgress(0.05f + 0.70f * (i.toFloat() / totalFrames))
                }
            }

            // Signal End of Stream
            codec.signalEndOfInputStream()
            
            // Drain remaining frames
            var droneTries = 0
            while (droneTries < 30) {
                val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000L)
                if (outputBufferId >= 0) {
                    val encodedData = codec.getOutputBuffer(outputBufferId)
                    if (encodedData != null && bufferInfo.size != 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                    }
                    codec.releaseOutputBuffer(outputBufferId, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                } else {
                    droneTries++
                }
            }

            onProgress(0.85f)

            // Safe shutdown
            try {
                codec.stop()
            } catch (e: Exception) {}
            try {
                codec.release()
            } catch (e: Exception) {}
            codec = null

            try {
                muxer.stop()
                muxer.release()
            } catch (e: Exception) {}
            muxer = null

            onProgress(0.90f)

            // 4. Save file to the media content resolver (Public Gallery)
            val savedUri = saveToGallery(context, tempFile, delivery)
            onProgress(1.0f)
            return@withContext savedUri

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        } finally {
            try {
                codec?.stop()
                codec?.release()
            } catch (e: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {}
            inputSurface?.release()
        }
    }

    private fun drawFrameOnCanvas(
        canvas: Canvas, width: Int, height: Int, frameIndex: Int, totalFrames: Int, bounceIndex: Int,
        x0: Float, y0: Float, z0: Float, xb: Float, yb: Float, zb: Float, xe: Float, ye: Float, ze: Float,
        delivery: DeliveryEntity, bowlerName: String, bowlerType: String, center: Float,
        pitchLeft0: Float, pitchRight0: Float, pitchLeft1: Float, pitchRight1: Float, pitchTopY: Float, pitchBotY: Float
    ) {
        // Paints background with deep navy analytics workspace color
        val bgPaint = Paint().apply { color = Color.parseColor("#0F172A") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle tech grid lines
        val gridPaint = Paint().apply {
            color = Color.parseColor("#1F2937")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        for (gx in 40 until width step 40) {
            canvas.drawLine(gx.toFloat(), 0f, gx.toFloat(), height.toFloat(), gridPaint)
        }
        for (gy in 40 until height step 40) {
            canvas.drawLine(0f, gy.toFloat(), width.toFloat(), gy.toFloat(), gridPaint)
        }

        // Draw green turf pitch Trapezoid
        val turfPaint = Paint().apply {
            color = Color.parseColor("#065F46")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val pitchPath = Path().apply {
            moveTo(pitchLeft0, pitchTopY)
            lineTo(pitchRight0, pitchTopY)
            lineTo(pitchRight1, pitchBotY)
            lineTo(pitchLeft1, pitchBotY)
            close()
        }
        canvas.drawPath(pitchPath, turfPaint)

        // Pitch white boundary lines shape
        val linesPaint = Paint().apply {
            color = Color.WHITE
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        canvas.drawPath(pitchPath, linesPaint)

        // Popping creases white lines
        // Batsman crease: near y=230f
        val batCreaseL = center - 80f
        val batCreaseR = center + 80f
        canvas.drawLine(batCreaseL, 240f, batCreaseR, 240f, linesPaint)

        // Bowler crease: near y=500f
        val bowlCreaseL = center - 145f
        val bowlCreaseR = center + 145f
        canvas.drawLine(bowlCreaseL, 490f, bowlCreaseR, 490f, linesPaint)

        // 3D Ocular Eye Reticle/Iris representing "StumpVision" concentric trackers enclosing batsman crease stumps
        val reticlePaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        
        // Outer cyber-circle
        canvas.drawCircle(center, pitchTopY, 52f, reticlePaint)
        
        // Dash styled circle
        val dashPaint = Paint(reticlePaint).apply {
            pathEffect = DashPathEffect(floatArrayOf(5f, 6f), 0f)
            color = Color.parseColor("#1D4ED8")
        }
        canvas.drawCircle(center, pitchTopY, 46f, dashPaint)

        // Eyeball pupil shading inside reticle
        val pupilPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(center, pitchTopY, 34f, pupilPaint)

        // 3D Batsman's Wickets (Stumps) standing at y=pitchTopY (200f)
        val stumpPaint = Paint().apply {
            color = Color.parseColor("#F59E0B")
            strokeWidth = 4f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val stumpHighlight = Paint().apply {
            color = Color.parseColor("#FEF08A")
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val stumpY0 = pitchTopY
        val stumpHeight = 48f
        val stumpY1 = pitchTopY - stumpHeight
        val stumpSpacing = 11f

        // Render left, center, right stumps
        for (stIdx in -1..1) {
            val sx = center + stIdx * stumpSpacing
            canvas.drawLine(sx, stumpY0, sx, stumpY1, stumpPaint)
            canvas.drawLine(sx + 1f, stumpY0, sx + 1f, stumpY1, stumpHighlight)
        }

        // Bails on top
        val bailPaint = Paint().apply {
            color = Color.parseColor("#B45309")
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        canvas.drawLine(center - stumpSpacing - 2f, stumpY1 - 2f, center - 1f, stumpY1 - 2f, bailPaint)
        canvas.drawLine(center + 1f, stumpY1 - 2f, center + stumpSpacing + 2f, stumpY1 - 2f, bailPaint)

        // Trajectory History Paths up to current frame index
        val pathPrePaint = Paint().apply {
            color = Color.parseColor("#06B6D4") // Cyan
            strokeWidth = 3.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val pathPostPaint = Paint().apply {
            color = Color.parseColor("#EF4444") // Red
            strokeWidth = 3.5f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val prePath = Path()
        val postPath = Path()
        var hasPre = false
        var hasPost = false

        for (idx in 0..frameIndex) {
            val progress = idx.toFloat() / (totalFrames - 1)
            val cx: Float
            val cy: Float
            val cz: Float

            if (idx <= bounceIndex) {
                val t = idx.toFloat() / bounceIndex
                cx = x0 + t * (xb - x0)
                cy = y0 + t * (yb - y0)
                cz = z0 * (1f - t) + zb * t + 100f * t * (1f - t)
                
                val px = cx
                val py = cy - cz
                if (!hasPre) {
                    prePath.moveTo(px, py)
                    hasPre = true
                } else {
                    prePath.lineTo(px, py)
                }
            } else {
                val t = (idx - bounceIndex).toFloat() / (totalFrames - 1 - bounceIndex)
                cx = xb + t * (xe - xb)
                cy = yb + t * (ye - yb)
                cz = zb * (1f - t) + ze * t + 50f * t * (1f - t)

                val px = cx
                val py = cy - cz
                if (!hasPost) {
                    postPath.moveTo(px, py)
                    hasPost = true
                } else {
                    postPath.lineTo(px, py)
                }
            }
        }

        if (hasPre) canvas.drawPath(prePath, pathPrePaint)
        if (hasPost) canvas.drawPath(postPath, pathPostPaint)

        // Landing Impact Spot concentric indicators on pitch
        if (frameIndex >= bounceIndex) {
            val impactPaint = Paint().apply {
                color = Color.parseColor("#F97316") // Orange
                strokeWidth = 1.5f
                style = Paint.Style.STROKE
                isAntiAlias = true
            }
            canvas.drawCircle(xb, yb, 10f, impactPaint)
            
            val pulseRadius = 10f + (frameIndex - bounceIndex) * 1.5f
            if (pulseRadius < 34f) {
                impactPaint.alpha = ((1f - (pulseRadius - 10f) / 24f) * 255).toInt()
                canvas.drawCircle(xb, yb, pulseRadius, impactPaint)
            }
            
            // Text annotation of bounce spot
            val tagTextPaint = Paint().apply {
                color = Color.parseColor("#FB923C")
                textSize = 10f
                typeface = Typeface.MONOSPACE
                isAntiAlias = true
            }
            canvas.drawText("PITCH", xb + 15f, yb + 3f, tagTextPaint)
        }

        // Draw White Leather Cricket Ball moving
        val currentProgress = frameIndex.toFloat() / (totalFrames - 1)
        val ballX: Float
        val ballY: Float
        val ballZ: Float

        if (frameIndex <= bounceIndex) {
            val t = frameIndex.toFloat() / bounceIndex
            ballX = x0 + t * (xb - x0)
            ballY = y0 + t * (yb - y0)
            ballZ = z0 * (1f - t) + zb * t + 100f * t * (1f - t)
        } else {
            val t = (frameIndex - bounceIndex).toFloat() / (totalFrames - 1 - bounceIndex)
            ballX = xb + t * (xe - xb)
            ballY = yb + t * (ye - yb)
            ballZ = zb * (1f - t) + ze * t + 50f * t * (1f - t)
        }

        val drawBallX = ballX
        val drawBallY = ballY - ballZ
        
        // Ball size scales down based on 3D distance progress (away from camera)
        val ballRadius = 14f * (1f - 0.45f * currentProgress)

        // 1. Solid pure white ball base
        val bWhitePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(drawBallX, drawBallY, ballRadius, bWhitePaint)

        // 2. Translucent sphere shading overlay (shadow on bottom-right)
        val bShadowPaint = Paint().apply {
            color = Color.parseColor("#33475569")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(drawBallX, drawBallY, ballRadius, bShadowPaint)

        // 3. Stitched diagonal Crimson Red seam line across ball
        val bSeamPaint = Paint().apply {
            color = Color.parseColor("#991B1B") // Crimson
            strokeWidth = ballRadius * 0.15f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        // Slightly curved seam path relative to the ball center
        val seamPath = Path().apply {
            moveTo(drawBallX - ballRadius * 0.85f, drawBallY - ballRadius * 0.3f)
            quadTo(drawBallX, drawBallY + ballRadius * 0.7f, drawBallX + ballRadius * 0.85f, drawBallY - ballRadius * 0.3f)
        }
        canvas.drawPath(seamPath, bSeamPaint)

        // 4. White reflection glossy spot on top-left of the ball shoulder
        val bHighlightPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(drawBallX - ballRadius * 0.32f, drawBallY - ballRadius * 0.32f, ballRadius * 0.22f, bHighlightPaint)

        // Header Analytics Board
        val barPaint = Paint().apply { color = Color.parseColor("#1E293B") }
        canvas.drawRect(0f, 0f, width.toFloat(), 68f, barPaint)

        // Neon RED rec dot blinking
        if ((frameIndex / 5) % 2 == 0) {
            val recDotPaint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(30f, 32f, 7f, recDotPaint)
        }
        
        val headerTitle = Paint().apply {
            color = Color.parseColor("#10B981") // Cricket Green/Emerald
            textSize = 13f
            style = Paint.Style.FILL
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("STUMPVISION HUD PLAYBACK", 48f, 36f, headerTitle)

        val headerSub = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText("PRO VIDEO EXPORT • UTC TIME ANALYSIS", 48f, 52f, headerSub)

        // Telemetry Statistics Dashboard Card at bottom of screen
        val footerBg = Paint().apply { color = Color.parseColor("#111827") }
        canvas.drawRect(0f, height - 90f, width.toFloat(), height.toFloat(), footerBg)

        // Line separator
        val sepPaint = Paint().apply {
            color = Color.parseColor("#FBBF24") // Solid Gold
            strokeWidth = 2.5f
        }
        canvas.drawLine(0f, height - 90f, width.toFloat(), height - 90f, sepPaint)

        // Paint Delivery speed and bowler particulars
        val speedValuePaint = Paint().apply {
            color = Color.WHITE
            textSize = 25f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${String.format("%.1f", delivery.speedKmph)} km/h", 24f, height - 54f, speedValuePaint)

        val speedLabelPaint = Paint().apply {
            color = Color.parseColor("#FBBF24") // Amber Gold
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("BALL SPEED • ${String.format("%.1f", delivery.speedKmph * 0.621371f)} MPH", 24f, height - 36f, speedLabelPaint)

        val typePaint = Paint().apply {
            color = Color.parseColor("#EF4444")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("BOWLER: $bowlerName (${delivery.swingType})", 230f, height - 60f, typePaint)

        val lengthPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText(delivery.lineLengthClass, 230f, height - 44f, lengthPaint)

        val drsVerdictPaint = Paint().apply {
            color = if (delivery.drsVerdict.startsWith("OUT")) Color.parseColor("#EF4444") else Color.parseColor("#10B981")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("DRS DECISION: ${delivery.drsVerdict}", 230f, height - 28f, drsVerdictPaint)

        // Fine Watermark
        val waterPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 9f
            typeface = Typeface.MONOSPACE
            isAntiAlias = true
        }
        canvas.drawText("STUMPVISION GRAPHICS ENGINE • VER 3.1", 24f, height - 16f, waterPaint)
    }

    private fun saveToGallery(context: Context, tempFile: File, delivery: DeliveryEntity): Uri? {
        val resolver = context.contentResolver
        val filename = "StumpVision_Ball_${delivery.deliveryNum}_Track.mp4"

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/StumpVision")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (videoUri != null) {
            try {
                resolver.openOutputStream(videoUri).use { outStream ->
                    if (outStream != null) {
                        FileInputStream(tempFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(videoUri, values, null, null)
                }
                
                return videoUri
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return null
    }
}
