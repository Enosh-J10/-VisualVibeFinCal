package com.enosh.fincalc.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object QRUtils {
    fun generateQRCode(text: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun createFinCalcCard(context: Context, name: String, finCalcId: String, qrText: String): Bitmap {
        val width = 800
        val height = 1200
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        
        // Header / Accent
        paint.color = Color.parseColor("#00D1B2")
        canvas.drawRect(0f, 0f, width.toFloat(), 200f, paint)
        
        // App Name
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.isFakeBoldText = true
        val appName = "FinCalc"
        canvas.drawText(appName, 50f, 130f, paint)
        
        // Profile Circle
        paint.color = Color.parseColor("#F0F4F8")
        canvas.drawCircle(width / 2f, 350f, 120f, paint)
        paint.color = Color.parseColor("#00D1B2")
        paint.textSize = 100f
        val initial = if (name.isNotEmpty()) name.take(1).uppercase() else "U"
        val textBounds = Rect()
        paint.getTextBounds(initial, 0, 1, textBounds)
        canvas.drawText(initial, (width / 2f) - textBounds.centerX(), 350f - textBounds.centerY(), paint)
        
        // Name
        paint.color = Color.BLACK
        paint.textSize = 50f
        paint.isFakeBoldText = true
        val nameToDraw = if (name.length > 20) name.take(17) + "..." else name
        paint.getTextBounds(nameToDraw, 0, nameToDraw.length, textBounds)
        canvas.drawText(nameToDraw, (width / 2f) - textBounds.centerX(), 550f, paint)
        
        // FinCalc ID
        paint.color = Color.GRAY
        paint.textSize = 35f
        paint.isFakeBoldText = false
        val idText = "ID: $finCalcId"
        paint.getTextBounds(idText, 0, idText.length, textBounds)
        canvas.drawText(idText, (width / 2f) - textBounds.centerX(), 610f, paint)
        
        // QR Code
        val qrSize = 450
        val qrBitmap = generateQRCode(qrText, qrSize)
        canvas.drawBitmap(qrBitmap, (width - qrSize) / 2f, 700f, null)
        
        // Footer text
        paint.color = Color.GRAY
        paint.textSize = 30f
        val footer = "Scan to add me on FinCalc"
        paint.getTextBounds(footer, 0, footer.length, textBounds)
        canvas.drawText(footer, (width / 2f) - textBounds.centerX(), 1160f, paint)
        
        return bitmap
    }

    fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/share_card.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(cachePath, "share_card.png"))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
