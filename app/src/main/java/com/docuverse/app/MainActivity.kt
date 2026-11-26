package com.docuverse.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.speech.tts.TextToSpeech
import android.text.Html
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import kotlin.math.sin
import kotlin.random.Random

private val VioletBarColor = ComposeColor(0xFF2B1147)
private val VioletBarText = ComposeColor(0xFFF6EEFF)

private val VioletBaseDark = ComposeColor(0xFF160A24)
private val VioletMidDark = ComposeColor(0xFF221038)
private val VioletGrainLine = ComposeColor(0xFF47246E)
private val VioletGrainGlow = ComposeColor(0xFF6B39A3)
private val VioletInkLight = ComposeColor(0xFFF7F0FF)
private val VioletInkMuted = ComposeColor(0xFFCBB6E6)
private val VioletCardSurface = ComposeColor(0xFF26123E)
private val VioletCardElevated = ComposeColor(0xFF311850)
private val VioletAccentBright = ComposeColor(0xFF9D5CFF)

private val coverBitmapCache = mutableMapOf<String, Bitmap>()

enum class LibraryViewMode(val label: String) {
    GRID_3("3x"),
    GRID_2("2x"),
    LIST("List")
}

enum class ReadEraSection(val label: String) {
    READING_NOW("Reading Now"),
    BOOKS_AND_DOCS("Books & documents"),
    FAVORITES("Favorites"),
    TO_READ("To Read"),
    HAVE_READ("Have Read"),
    AUTHORS("Authors"),
    SERIES("Series"),
    COLLECTIONS("Collections"),
    FORMATS("Formats"),
    QUOTES("Quotes"),
    DICTIONARY("Dictionary"),
    TRASH("Trash"),
    SETTINGS("Settings")
}

enum class QuoteColorOption(val label: String, val swatch: ComposeColor) {
    VIOLET("Violet", ComposeColor(0xFF9D5CFF)),
    MINT("Mint", ComposeColor(0xFF34D399)),
    ROSE("Rose", ComposeColor(0xFFF472B6)),
    SKY("Sky", ComposeColor(0xFF38BDF8))
}

data class QuoteEntry(
    val id: String,
    val bookTitle: String,
    val sectionIndex: Int,
    val text: String,
    val colorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DictionaryEntry(
    val word: String,
    val definition: String,
    val sourceBook: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CustomFontEntry(
    val name: String,
    val localPath: String
)

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original"),
    DOC_BW("Doc B&W"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale")
}

enum class ScanFeatureMode(val label: String, val badge: String? = null) {
    SCAN("Scan"),
    ID_CARDS("ID Cards"),
    BOOK("Book"),
    TIMESTAMP("Timestamp"),
    MEGA_SCAN("Mega Scan", "NEW"),
    SIGN("Sign", "HOT"),
    EXTRACT_TEXT("Extract Text"),
    TRANSLATE("Translate"),
    FORMULA("Formula"),
    ID_PHOTO("ID Photo Maker"),
    SMART_ERASE("Smart Erase"),
    TO_WORD("To Word"),
    TO_EXCEL("To Excel"),
    PDF_TO_IMAGES("PDF to Images"),
    PDF_TO_LONG("PDF to Long Image")
}

enum class EpubPageTheme(
    val label: String,
    val bgColor: ComposeColor,
    val textColor: ComposeColor
) {
    VIOLET_DUSK("Violet Dusk", ComposeColor(0xFF1B0E2E), ComposeColor(0xFFF4ECFF)),
    SOFT_LILAC("Soft Lilac", ComposeColor(0xFFE7DDF5), ComposeColor(0xFF221A33)),
    SOFT_MINT("Soft Mint", ComposeColor(0xFFDCEFD9), ComposeColor(0xFF17291B)),
    OLD_YELLOW("Old Yellow", ComposeColor(0xFFF3E5B5), ComposeColor(0xFF2B2114)),
    PAPER_WHITE("Day White", ComposeColor(0xFFFAF9F5), ComposeColor(0xFF1C1B18)),
    WARM_SEPIA("Warm Sepia", ComposeColor(0xFFE8D8C3), ComposeColor(0xFF2E2013)),
    NIGHT_SLATE("Night Slate", ComposeColor(0xFF0E1A21), ComposeColor(0xFFDCE6EC))
}

enum class EpubFontStyleOption(val label: String, val family: FontFamily) {
    SERIF("Serif", FontFamily.Serif),
    SANS("Sans", FontFamily.SansSerif),
    MONO("Mono", FontFamily.Monospace)
}

data class SideCrop(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
)

data class EditableScanPage(
    val rawBitmap: Bitmap,
    val crop: SideCrop = SideCrop(),
    val filter: ScanFilterMode = ScanFilterMode.DOC_BW,
    val timestampLabel: String? = null,
    val signaturePoints: List<List<Offset>> = emptyList(),
    val erasePatch: Offset? = null
) {
    fun renderEdited(): Bitmap {
        val cropped = cropSidesBitmap(rawBitmap, crop.left, crop.top, crop.right, crop.bottom)
        val filtered = applyScanFilter(cropped, filter)
        val out = filtered.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        if (erasePatch != null) {
            val px = (erasePatch.x * out.width).coerceIn(0f, out.width.toFloat())
            val py = (erasePatch.y * out.height).coerceIn(0f, out.height.toFloat())
            val radius = (minOf(out.width, out.height) * 0.08f).coerceAtLeast(18f)
            val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(px, py, radius, erasePaint)
        }

        if (signaturePoints.isNotEmpty()) {
            val sigPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(34, 16, 86)
                style = Paint.Style.STROKE
                strokeWidth = (out.width * 0.008f).coerceAtLeast(3f)
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            for (strokePts in signaturePoints) {
                if (strokePts.size >= 2) {
                    val p = android.graphics.Path()
                    p.moveTo(strokePts.first().x * out.width, strokePts.first().y * out.height)
                    for (i in 1 until strokePts.size) {
                        p.lineTo(strokePts[i].x * out.width, strokePts[i].y * out.height)
                    }
                    canvas.drawPath(p, sigPaint)
                }
            }
        }

        if (!timestampLabel.isNullOrBlank()) {
            val boxW = (out.width * 0.44f).coerceAtLeast(180f)
            val boxH = (out.height * 0.055f).coerceAtLeast(38f)
            val left = out.width - boxW - (out.width * 0.03f)
            val top = out.height - boxH - (out.height * 0.03f)
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(205, 22, 10, 36)
                style = Paint.Style.FILL
            }
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(255, 225, 110)
                textSize = (boxH * 0.52f).coerceAtLeast(14f)
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }
            canvas.drawRoundRect(left, top, left + boxW, top + boxH, 10f, 10f, bgPaint)
            canvas.drawText(timestampLabel, left + 14f, top + boxH * 0.68f, textPaint)
        }

        return out
    }
}

data class BookItem(
    val uriString: String,
    val title: String,
    val format: String = "PDF",
    val pageCount: Int,
    val lastPage: Int = 0,
    val isFavorite: Boolean = false,
    val isTrashed: Boolean = false,
    val author: String = "Unknown Author",
    val collection: String = "General",
    val cover: Bitmap? = null,
    val metadataLoaded: Boolean = false
)

data class ExportedScanResult(
    val title: String,
    val format: String,
    val uris: List<Uri>,
    val bookItem: BookItem? = null
)

private val DarkVioletScheme = darkColorScheme(
    primary = VioletAccentBright,
    onPrimary = VioletInkLight,
    background = VioletBaseDark,
    onBackground = VioletInkLight,
    surface = VioletCardSurface,
    onSurface = VioletInkLight,
    surfaceVariant = VioletCardElevated,
    onSurfaceVariant = VioletInkMuted,
    error = ComposeColor(0xFFFF6B7A)
)

private fun isTextPagedFormat(format: String): Boolean {
    return format in setOf("EPUB", "MOBI", "AZW", "AZW3", "FB2", "TXT", "DOCX")
}

private fun detectBookFormat(fileName: String, mimeType: String): String? {
    val lower = fileName.lowercase()
    val mime = mimeType.lowercase()
    return when {
        lower.endsWith(".pdf") || mime.contains("pdf") -> "PDF"
        lower.endsWith(".epub") || mime.contains("epub") -> "EPUB"
        lower.endsWith(".azw3") -> "AZW3"
        lower.endsWith(".azw") -> "AZW"
        lower.endsWith(".mobi") || mime.contains("mobipocket") -> "MOBI"
        lower.endsWith(".fb2") || mime.contains("fictionbook") -> "FB2"
        lower.endsWith(".docx") || mime.contains("wordprocessingml") -> "DOCX"
        lower.endsWith(".txt") || mime.startsWith("text/plain") -> "TXT"
        else -> null
    }
}

private fun stripBookExtension(rawName: String): String {
    return rawName
        .removeSuffix(".pdf").removeSuffix(".PDF")
        .removeSuffix(".epub").removeSuffix(".EPUB")
        .removeSuffix(".mobi").removeSuffix(".MOBI")
        .removeSuffix(".azw3").removeSuffix(".AZW3")
        .removeSuffix(".azw").removeSuffix(".AZW")
        .removeSuffix(".fb2").removeSuffix(".FB2")
        .removeSuffix(".txt").removeSuffix(".TXT")
        .removeSuffix(".docx").removeSuffix(".DOCX")
}

private fun applyScreenBrightness(context: Context, level: Float) {
    val activity = context as? Activity ?: return
    val clamped = level.coerceIn(0.08f, 1.0f)
    val lp = activity.window.attributes
    lp.screenBrightness = clamped
    activity.window.attributes = lp
}

private fun loadCustomFontFamily(entry: CustomFontEntry): FontFamily? {
    return try {
        val file = File(entry.localPath)
        if (!file.exists()) return null
        val tf = Typeface.createFromFile(file)
        FontFamily(tf)
    } catch (_: Exception) {
        null
    }
}

private fun splitBookSpreadBitmap(src: Bitmap): Pair<Bitmap, Bitmap>? {
    if (src.width < 8 || src.height < 4) return null
    val halfW = src.width / 2
    val leftPage = Bitmap.createBitmap(src, 0, 0, halfW, src.height)
    val rightPage = Bitmap.createBitmap(src, halfW, 0, (src.width - halfW).coerceAtLeast(1), src.height)
    return Pair(leftPage, rightPage)
}

private fun makeIdPhotoBitmap(src: Bitmap): Bitmap {
    val targetW = 480
    val targetH = 600
    val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.rgb(238, 242, 250))

    val scaled = if (src.width > src.height) {
        val nh = ((src.height.toFloat() / src.width.toFloat()) * targetW).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(src, targetW, nh, true)
    } else {
        val nw = ((src.width.toFloat() / src.height.toFloat()) * targetH).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(src, nw, targetH, true)
    }
    val dx = (targetW - scaled.width) / 2f
    val dy = (targetH - scaled.height) / 2f
    canvas.drawBitmap(scaled, dx, dy, null)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(157, 92, 255)
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRect(3f, 3f, targetW - 3f, targetH - 3f, borderPaint)
    return out
}

private fun stitchLongImageBitmap(pages: List<Bitmap>): Bitmap? {
    if (pages.isEmpty()) return null
    val width = pages.maxOf { it.width.coerceAtLeast(1) }
    val totalHeight = pages.sumOf { it.height.coerceAtLeast(1) }
    val out = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(Color.WHITE)
    var yCursor = 0f
    for (p in pages) {
        val xOffset = ((width - p.width) / 2f).coerceAtLeast(0f)
        canvas.drawBitmap(p, xOffset, yCursor, null)
        yCursor += p.height.toFloat()
    }
    return out
}

@Composable
private fun DarkVioletTextureBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    VioletBaseDark,
                    VioletMidDark,
                    VioletBaseDark,
                    VioletMidDark
                )
            )
        )
    ) {
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bandCount = 8
            val bandH = h / bandCount

            for (b in 0 until bandCount) {
                val topY = b * bandH
                val phase = b * 1.2f
                for (g in 0 until 5) {
                    val baseY = topY + bandH * ((g + 0.5f) / 5f)
                    val path = Path()
                    val steps = 24
                    for (s in 0..steps) {
                        val x = (w * s) / steps
                        val wave = sin((x / w) * 6.283f + phase + g * 0.8f) * (bandH * 0.04f)
                        val y = baseY + wave
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = if (g % 2 == 0) VioletGrainLine.copy(alpha = 0.28f) else VioletGrainGlow.copy(alpha = 0.14f),
                        style = Stroke(width = 1.6f, cap = StrokeCap.Round)
                    )
                }
                if (b > 0) {
                    drawLine(
                        color = VioletGrainLine.copy(alpha = 0.42f),
                        start = Offset(0f, topY),
                        end = Offset(w, topY),
                        strokeWidth = 1.5f
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadEraGraphicIcon(
    kind: String,
    tint: ComposeColor,
    modifier: Modifier = Modifier.size(22.dp)
) {
    ComposeCanvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            "READING_NOW" -> {
                drawArc(tint, 30f, 120f, false, Offset(w * 0.16f, h * 0.16f), Size(w * 0.68f, h * 0.68f), style = stroke)
                drawArc(tint, 210f, 120f, false, Offset(w * 0.16f, h * 0.16f), Size(w * 0.68f, h * 0.68f), style = stroke)
            }
            "BOOKS" -> {
                drawRoundRect(tint, Offset(w * 0.16f, h * 0.18f), Size(w * 0.68f, h * 0.64f), CornerRadius(w * 0.08f, w * 0.08f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), w * 0.08f)
            }
            "FAVORITES" -> {
                val star = Path().apply {
                    moveTo(w * 0.5f, h * 0.14f)
                    lineTo(w * 0.62f, h * 0.38f)
                    lineTo(w * 0.88f, h * 0.41f)
                    lineTo(w * 0.69f, h * 0.59f)
                    lineTo(w * 0.74f, h * 0.85f)
                    lineTo(w * 0.5f, h * 0.72f)
                    lineTo(w * 0.26f, h * 0.85f)
                    lineTo(w * 0.31f, h * 0.59f)
                    lineTo(w * 0.12f, h * 0.41f)
                    lineTo(w * 0.38f, h * 0.38f)
                    close()
                }
                drawPath(star, color = tint, style = stroke)
            }
            "TO_READ" -> {
                drawCircle(tint, w * 0.34f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.30f), w * 0.08f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.66f, h * 0.56f), w * 0.08f, StrokeCap.Round)
            }
            "HAVE_READ" -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.52f), Offset(w * 0.42f, h * 0.74f), w * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.82f, h * 0.28f), w * 0.09f, StrokeCap.Round)
            }
            "AUTHORS" -> {
                drawCircle(tint, w * 0.18f, Offset(w * 0.5f, h * 0.32f))
                drawArc(tint, 180f, 180f, true, Offset(w * 0.20f, h * 0.52f), Size(w * 0.60f, h * 0.36f))
            }
            "SERIES" -> {
                val tag = Path().apply {
                    moveTo(w * 0.22f, h * 0.20f)
                    lineTo(w * 0.56f, h * 0.20f)
                    lineTo(w * 0.82f, h * 0.50f)
                    lineTo(w * 0.56f, h * 0.80f)
                    lineTo(w * 0.22f, h * 0.80f)
                    close()
                }
                drawPath(tag, color = tint, style = stroke)
            }
            "COLLECTIONS" -> {
                drawRect(tint, Offset(w * 0.20f, h * 0.22f), Size(w * 0.14f, h * 0.56f))
                drawRect(tint, Offset(w * 0.42f, h * 0.22f), Size(w * 0.14f, h * 0.56f))
                drawLine(tint, Offset(w * 0.66f, h * 0.24f), Offset(w * 0.82f, h * 0.76f), w * 0.11f, StrokeCap.Round)
            }
            "FORMATS" -> {
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.24f), Size(w * 0.60f, h * 0.18f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.46f), Size(w * 0.60f, h * 0.18f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.68f), Size(w * 0.60f, h * 0.14f), CornerRadius(6f, 6f), style = stroke)
            }
            "QUOTES" -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.28f), Offset(w * 0.80f, h * 0.28f), w * 0.08f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.80f, h * 0.50f), w * 0.08f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.20f, h * 0.72f), Offset(w * 0.60f, h * 0.72f), w * 0.08f, StrokeCap.Round)
            }
            "DICTIONARY" -> {
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), CornerRadius(6f, 6f), style = stroke)
                drawLine(tint, Offset(w * 0.34f, h * 0.62f), Offset(w * 0.50f, h * 0.28f), w * 0.08f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.50f, h * 0.28f), Offset(w * 0.66f, h * 0.62f), w * 0.08f, StrokeCap.Round)
            }
            "SYNC" -> {
                drawArc(tint, 20f, 130f, false, Offset(w * 0.20f, h * 0.20f), Size(w * 0.60f, h * 0.60f), style = stroke)
                drawArc(tint, 200f, 130f, false, Offset(w * 0.20f, h * 0.20f), Size(w * 0.60f, h * 0.60f), style = stroke)
            }
            "FOLDERS" -> {
                val folder = Path().apply {
                    moveTo(w * 0.16f, h * 0.26f)
                    lineTo(w * 0.42f, h * 0.26f)
                    lineTo(w * 0.50f, h * 0.36f)
                    lineTo(w * 0.84f, h * 0.36f)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(folder, color = tint)
            }
            "DOWNLOADS" -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.20f), Offset(w * 0.5f, h * 0.62f), w * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.30f, h * 0.44f), Offset(w * 0.5f, h * 0.64f), w * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.44f), Offset(w * 0.5f, h * 0.64f), w * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, h * 0.80f), Offset(w * 0.78f, h * 0.80f), w * 0.09f, StrokeCap.Round)
            }
            "TRASH" -> {
                drawRoundRect(tint, Offset(w * 0.26f, h * 0.30f), Size(w * 0.48f, h * 0.52f), CornerRadius(6f, 6f), style = stroke)
                drawLine(tint, Offset(w * 0.18f, h * 0.24f), Offset(w * 0.82f, h * 0.24f), w * 0.08f, StrokeCap.Round)
            }
            "SETTINGS" -> {
                drawCircle(tint, w * 0.26f, Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, w * 0.09f, Offset(w * 0.5f, h * 0.5f))
            }
            else -> {
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.22f), Size(w * 0.64f, h * 0.48f), CornerRadius(8f, 8f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.50f), w * 0.09f, StrokeCap.Round)
                drawCircle(tint, w * 0.055f, Offset(w * 0.5f, h * 0.60f))
            }
        }
    }
}

private fun applyParagraphIndent(text: String, indentLevel: Int): String {
    if (indentLevel <= 0) return text
    val prefix = "    ".repeat(indentLevel)
    return text.split("\n\n").joinToString("\n\n") { para ->
        if (para.isBlank()) para else prefix + para.trimStart()
    }
}

private fun splitTextIntoSections(rawText: String, chunkSize: Int = 3600): List<String> {
    val cleaned = rawText.replace("\r\n", "\n").replace(Regex("\n{3,}"), "\n\n").trim()
    if (cleaned.isEmpty()) return listOf()
    val paragraphs = cleaned.split("\n\n")
    val sections = mutableListOf<String>()
    val sb = StringBuilder()

    for (p in paragraphs) {
        val part = p.trim()
        if (part.isEmpty()) continue
        if (part.length > chunkSize) {
            if (sb.isNotEmpty()) {
                sections.add(sb.toString().trim())
                sb.clear()
            }
            var idx = 0
            while (idx < part.length) {
                val end = (idx + chunkSize).coerceAtMost(part.length)
                sections.add(part.substring(idx, end).trim())
                idx = end
            }
        } else {
            if (sb.length + part.length + 2 > chunkSize && sb.isNotEmpty()) {
                sections.add(sb.toString().trim())
                sb.clear()
            }
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(part)
        }
    }
    if (sb.isNotEmpty()) {
        sections.add(sb.toString().trim())
    }
    return sections
}

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                ttsReady = true
            }
        }

        setContent {
            MaterialTheme(colorScheme = DarkVioletScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VioletBaseDark
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    DisposableEffect(Unit) {
                        onDispose {
                            try {
                                tts?.stop()
                                tts?.shutdown()
                            } catch (_: Exception) {
                            }
                        }
                    }

                    var isSpeaking by remember { mutableStateOf(false) }
                    fun stopSpeaking() {
                        try {
                            tts?.stop()
                        } catch (_: Exception) {
                        }
                        isSpeaking = false
                    }
                    fun speakText(text: String) {
                        if (!ttsReady || tts == null || text.isBlank()) return
                        val clipped = if (text.length > 3900) text.take(3900) else text
                        tts?.speak(clipped, TextToSpeech.QUEUE_FLUSH, null, "ebookish_tts")
                        isSpeaking = true
                    }

                    var books by remember { mutableStateOf(loadBooks(context)) }
                    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID_3) }
                    var section by remember { mutableStateOf(ReadEraSection.BOOKS_AND_DOCS) }
                    var formatFilter by remember { mutableStateOf("ALL") }
                    var authorFilter by remember { mutableStateOf("ALL") }
                    var collectionFilter by remember { mutableStateOf("ALL") }
                    var useDefaultCovers by remember { mutableStateOf(false) }
                    var showDrawer by remember { mutableStateOf(false) }
                    var showOverflowMenu by remember { mutableStateOf(false) }
                    var showAboutDialog by remember { mutableStateOf(false) }

                    var quotes by remember { mutableStateOf(loadQuotes(context)) }
                    var quoteColorFilter by remember { mutableStateOf("ALL") }
                    var dictionaryEntries by remember { mutableStateOf(loadDictionary(context)) }
                    var newDictWord by remember { mutableStateOf("") }
                    var newDictDef by remember { mutableStateOf("") }

                    var customFonts by remember { mutableStateOf(loadCustomFonts(context)) }
                    var selectedCustomFontPath by remember { mutableStateOf<String?>(null) }

                    var showSyncDialog by remember { mutableStateOf(false) }
                    var syncStatusText by remember { mutableStateOf("Local backup & restore ready.") }

                    var selectionMode by remember { mutableStateOf(false) }
                    var selectedUris by remember { mutableStateOf(setOf<String>()) }

                    var searchQuery by remember { mutableStateOf("") }
                    var scanNotice by remember { mutableStateOf("") }
                    var isScanning by remember { mutableStateOf(false) }
                    var showDocScanner by remember { mutableStateOf(false) }

                    var epubPageTheme by remember { mutableStateOf(EpubPageTheme.VIOLET_DUSK) }
                    var epubFontSizeSp by remember { mutableFloatStateOf(19f) }
                    var epubFontStyle by remember { mutableStateOf(EpubFontStyleOption.SERIF) }
                    var epubLineSpacingMult by remember { mutableFloatStateOf(1.45f) }
                    var epubSideMarginDp by remember { mutableIntStateOf(24) }
                    var epubParagraphIndentLevel by remember { mutableIntStateOf(1) }

                    var screenBrightness by remember { mutableFloatStateOf(0.75f) }
                    var showBrightnessSlider by remember { mutableStateOf(false) }

                    var activeBook by remember { mutableStateOf<BookItem?>(null) }
                    var readerPages by remember { mutableStateOf(listOf<Bitmap>()) }
                    var textBookSections by remember { mutableStateOf(listOf<String>()) }
                    var isLoadingReader by remember { mutableStateOf(false) }

                    fun exportSyncSnapshot() {
                        try {
                            val root = JSONObject().apply {
                                put("exportedAt", System.currentTimeMillis())
                                put("books", booksToJsonArray(books))
                                put("quotes", quotesToJsonArray(quotes))
                                put("dictionary", dictionaryToJsonArray(dictionaryEntries))
                            }
                            val fileName = "ebookish_sync_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".json"
                            val outUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                val cv = ContentValues().apply {
                                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                }
                                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                            } else {
                                null
                            }
                            val outFile = if (outUri == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                if (!dir.exists()) dir.mkdirs()
                                File(dir, fileName)
                            } else {
                                null
                            }
                            val syncBytes = root.toString(2).toByteArray(Charsets.UTF_8)
                            if (outUri != null) {
                                context.contentResolver.openOutputStream(outUri)?.use { stream ->
                                    stream.write(syncBytes)
                                }
                                syncStatusText = "Exported to Downloads/$fileName"
                            } else if (outFile != null) {
                                FileOutputStream(outFile).use { stream ->
                                    stream.write(syncBytes)
                                }
                                syncStatusText = "Exported to Downloads/$fileName"
                            } else {
                                syncStatusText = "Could not create sync file."
                            }
                        } catch (_: Exception) {
                            syncStatusText = "Sync export failed."
                        }
                    }

                    val syncImportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            try {
                                val raw = context.contentResolver.openInputStream(uri)?.use {
                                    String(it.readBytes(), Charsets.UTF_8)
                                } ?: return@rememberLauncherForActivityResult
                                val obj = JSONObject(raw)
                                val importedBooks = parseBooksJsonArray(obj.optJSONArray("books"))
                                val importedQuotes = parseQuotesJsonArray(obj.optJSONArray("quotes"))
                                val importedDict = parseDictionaryJsonArray(obj.optJSONArray("dictionary"))

                                if (importedBooks.isNotEmpty()) {
                                    val byUri = (importedBooks + books).associateBy { it.uriString }.values.toList()
                                    books = byUri
                                    saveBooks(context, byUri)
                                }
                                if (importedQuotes.isNotEmpty()) {
                                    val byId = (importedQuotes + quotes).associateBy { it.id }.values.toList()
                                    quotes = byId
                                    saveQuotes(context, byId)
                                }
                                if (importedDict.isNotEmpty()) {
                                    val byWord = (importedDict + dictionaryEntries).associateBy { it.word.lowercase() }.values.toList()
                                    dictionaryEntries = byWord
                                    saveDictionary(context, byWord)
                                }
                                syncStatusText = "Imported ${importedBooks.size} books, ${importedQuotes.size} quotes, ${importedDict.size} words."
                            } catch (_: Exception) {
                                syncStatusText = "Invalid sync file."
                            }
                        }
                    }

                    val customFontPicker = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            val rawName = getRawFileName(context, uri)
                            val imported = importCustomFontFile(context, uri, rawName)
                            if (imported != null) {
                                val nextList = listOf(imported) + customFonts.filter { it.localPath != imported.localPath }
                                customFonts = nextList
                                saveCustomFonts(context, nextList)
                                selectedCustomFontPath = imported.localPath
                                scanNotice = "Added custom font: ${imported.name}"
                            } else {
                                scanNotice = "Could not load font file."
                            }
                        }
                    }

                    fun loadSectionsForBook(book: BookItem): List<String> {
                        val uri = Uri.parse(book.uriString)
                        return when (book.format) {
                            "EPUB" -> extractEpubChapters(context, uri)
                            "FB2" -> extractFb2Sections(context, uri)
                            "TXT" -> extractTxtSections(context, uri)
                            "DOCX" -> extractDocxSections(context, uri)
                            "MOBI", "AZW", "AZW3" -> extractMobiAzwSections(context, uri)
                            else -> listOf()
                        }
                    }

                    fun resolveBookCover(book: BookItem, scale: Float = 0.5f): Bitmap? {
                        coverBitmapCache[book.uriString]?.let { return it }
                        val uri = Uri.parse(book.uriString)
                        val bmp = when (book.format) {
                            "PDF" -> renderSinglePage(context, uri, 0, scale)
                            "EPUB" -> extractEpubCoverBitmap(context, uri)
                            "MOBI", "AZW", "AZW3" -> extractMobiAzwCoverBitmap(context, uri)
                            else -> null
                        }
                        if (bmp != null) {
                            coverBitmapCache[book.uriString] = bmp
                        }
                        return bmp
                    }

                    fun rebuildAllCovers() {
                        scanNotice = "Rebuilding book covers..."
                        coverBitmapCache.clear()
                        Thread {
                            val working = books.toMutableList()
                            for (i in working.indices) {
                                val b = working[i]
                                if (b.format == "PDF") {
                                    val uri = Uri.parse(b.uriString)
                                    val count = getPdfPageCount(context, uri)
                                    val thumb = resolveBookCover(b, 0.5f)
                                    working[i] = b.copy(
                                        pageCount = if (count > 0) count else b.pageCount.coerceAtLeast(1),
                                        cover = thumb,
                                        metadataLoaded = true
                                    )
                                } else {
                                    val secs = loadSectionsForBook(b)
                                    val thumb = resolveBookCover(b, 0.5f)
                                    working[i] = b.copy(
                                        pageCount = if (secs.isNotEmpty()) secs.size else b.pageCount.coerceAtLeast(1),
                                        cover = thumb,
                                        metadataLoaded = true
                                    )
                                }
                            }
                            val finalList = working.toList()
                            saveBooks(context, finalList)
                            runOnUiThread {
                                books = finalList
                                useDefaultCovers = false
                                scanNotice = "Rebuilt book covers."
                            }
                        }.start()
                    }

                    LaunchedEffect(books.size) {
                        val pending = books.filter { !it.metadataLoaded || (it.cover == null && !useDefaultCovers) }
                        if (pending.isNotEmpty()) {
                            Thread {
                                val working = books.toMutableList()
                                var changed = false
                                for (i in working.indices) {
                                    val b = working[i]
                                    if (!b.metadataLoaded || b.cover == null) {
                                        val uri = Uri.parse(b.uriString)
                                        val thumb = b.cover ?: resolveBookCover(b, 0.45f)
                                        if (isTextPagedFormat(b.format)) {
                                            val count = if (b.metadataLoaded) b.pageCount else {
                                                val secs = loadSectionsForBook(b)
                                                if (secs.isNotEmpty()) secs.size else 1
                                            }
                                            if (thumb != null || !b.metadataLoaded) {
                                                working[i] = b.copy(
                                                    pageCount = count,
                                                    cover = thumb,
                                                    metadataLoaded = true
                                                )
                                                changed = true
                                            }
                                        } else {
                                            val count = if (b.metadataLoaded) b.pageCount else {
                                                val c = getPdfPageCount(context, uri)
                                                if (c > 0) c else 1
                                            }
                                            if (thumb != null || !b.metadataLoaded) {
                                                working[i] = b.copy(
                                                    pageCount = count,
                                                    cover = thumb,
                                                    metadataLoaded = true
                                                )
                                                changed = true
                                            }
                                        }
                                        if (changed && (i + 1) % 3 == 0) {
                                            val snapshot = working.toList()
                                            runOnUiThread { books = snapshot }
                                        }
                                    }
                                }
                                if (changed) {
                                    val finalSnapshot = working.toList()
                                    saveBooks(context, finalSnapshot)
                                    runOnUiThread { books = finalSnapshot }
                                }
                            }.start()
                        }
                    }

                    fun quickBookItem(uri: Uri, knownName: String? = null): BookItem? {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }

                        val rawName = knownName ?: getRawFileName(context, uri)
                        val mime = context.contentResolver.getType(uri) ?: ""
                        val detectedFormat = detectBookFormat(rawName, mime) ?: return null
                        val title = stripBookExtension(rawName)
                        val defaultCollection = when (detectedFormat) {
                            "PDF" -> "Documents"
                            "TXT", "DOCX" -> "Notes & Docs"
                            else -> "Ebooks"
                        }

                        val initialItem = BookItem(
                            uriString = uri.toString(),
                            title = title,
                            format = detectedFormat,
                            pageCount = 1,
                            lastPage = 0,
                            isFavorite = false,
                            isTrashed = false,
                            author = if (title.contains(" - ")) title.substringBefore(" - ").trim() else "Library",
                            collection = defaultCollection,
                            cover = null,
                            metadataLoaded = false
                        )
                        return initialItem.copy(cover = resolveBookCover(initialItem, 0.45f))
                    }

                    fun addBookToShelf(item: BookItem) {
                        val updated = listOf(item) + books.filter { it.uriString != item.uriString }
                        books = updated
                        saveBooks(context, updated)
                    }

                    val addBookLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenMultipleDocuments()
                    ) { uris: List<Uri> ->
                        if (uris.isNotEmpty()) {
                            Thread {
                                val addedItems = mutableListOf<BookItem>()
                                for (u in uris) {
                                    val item = quickBookItem(u)
                                    if (item != null) addedItems.add(item)
                                }
                                runOnUiThread {
                                    val existingUris = addedItems.map { it.uriString }.toSet()
                                    val updated = addedItems + books.filter { it.uriString !in existingUris }
                                    books = updated
                                    saveBooks(context, updated)
                                    scanNotice = "Imported ${addedItems.size} book(s)."
                                }
                            }.start()
                        }
                    }

                    fun scanTreeUri(treeUri: Uri) {
                        isScanning = true
                        scanNotice = "Fast scanning folder..."
                        Thread {
                            val discovered = collectBooksFromTreeFast(context, treeUri) { countSoFar ->
                                runOnUiThread {
                                    scanNotice = "Scanning... found $countSoFar book(s)"
                                }
                            }
                            val newBooks = mutableListOf<BookItem>()
                            val currentUris = books.map { it.uriString }.toMutableSet()

                            for (pair in discovered) {
                                val docUri = pair.first
                                val docName = pair.second
                                val uriStr = docUri.toString()
                                if (uriStr !in currentUris) {
                                    val item = quickBookItem(docUri, docName)
                                    if (item != null) {
                                        newBooks.add(item)
                                        currentUris.add(uriStr)
                                    }
                                }
                            }

                            val updated = newBooks + books
                            saveBooks(context, updated)
                            runOnUiThread {
                                books = updated
                                isScanning = false
                                scanNotice = if (newBooks.isNotEmpty()) {
                                    "Added ${newBooks.size} book(s) quickly!"
                                } else {
                                    "Found ${discovered.size} book(s) (all already in library)."
                                }
                            }
                        }.start()
                    }

                    val scanFolderLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocumentTree()
                    ) { treeUri: Uri? ->
                        if (treeUri != null) {
                            try {
                                contentResolver.takePersistableUriPermission(
                                    treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: Exception) {
                            }
                            saveSavedFolderUri(context, treeUri.toString())
                            scanTreeUri(treeUri)
                        }
                    }

                    fun startFolderScan() {
                        val savedTree = loadSavedFolderUri(context)
                        if (savedTree != null) {
                            val uri = Uri.parse(savedTree)
                            val hasPersisted = contentResolver.persistedUriPermissions.any {
                                it.uri == uri && it.isReadPermission
                            }
                            if (hasPersisted) {
                                scanTreeUri(uri)
                                return
                            }
                        }
                        scanFolderLauncher.launch(null)
                    }

                    fun openBook(book: BookItem) {
                        stopSpeaking()
                        showDrawer = false
                        showBrightnessSlider = false
                        activeBook = book
                        isLoadingReader = true
                        readerPages = listOf()
                        textBookSections = listOf()
                        Thread {
                            if (isTextPagedFormat(book.format)) {
                                val secs = loadSectionsForBook(book)
                                runOnUiThread {
                                    textBookSections = secs
                                    isLoadingReader = false
                                }
                            } else {
                                val rendered = renderAllPages(context, Uri.parse(book.uriString))
                                runOnUiThread {
                                    readerPages = rendered
                                    isLoadingReader = false
                                }
                            }
                        }.start()
                    }

                    if (showDocScanner) {
                        BackHandler { showDocScanner = false }

                        var docTitle by remember {
                            mutableStateOf(
                                "Scan_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            )
                        }
                        var scanPages by remember { mutableStateOf(listOf<EditableScanPage>()) }
                        var selectedIndex by remember { mutableIntStateOf(0) }
                        var defaultFilter by remember { mutableStateOf(ScanFilterMode.DOC_BW) }
                        var activeTool by remember { mutableStateOf(ScanFeatureMode.SCAN) }
                        var batchMode by remember { mutableStateOf(true) }
                        var showAllFeaturesSheet by remember { mutableStateOf(false) }

                        var extractedText by remember { mutableStateOf("") }
                        var translatedText by remember { mutableStateOf("") }
                        var targetLang by remember { mutableStateOf("Urdu") }
                        var signDraftPoints by remember { mutableStateOf(listOf<List<Offset>>()) }
                        var docStatus by remember { mutableStateOf("") }
                        var isProcessingDoc by remember { mutableStateOf(false) }
                        var lastExported by remember { mutableStateOf<ExportedScanResult?>(null) }

                        val cameraSnapLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.TakePicturePreview()
                        ) { bmp: Bitmap? ->
                            if (bmp != null) {
                                val stamp = if (activeTool == ScanFeatureMode.TIMESTAMP) {
                                    SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
                                } else null

                                val workingBmp = if (activeTool == ScanFeatureMode.ID_PHOTO) {
                                    makeIdPhotoBitmap(bmp)
                                } else {
                                    bmp
                                }

                                val newPages = if (activeTool == ScanFeatureMode.BOOK) {
                                    val split = splitBookSpreadBitmap(workingBmp)
                                    if (split != null) {
                                        listOf(
                                            EditableScanPage(split.first, filter = defaultFilter, timestampLabel = stamp),
                                            EditableScanPage(split.second, filter = defaultFilter, timestampLabel = stamp)
                                        )
                                    } else {
                                        listOf(EditableScanPage(workingBmp, filter = defaultFilter, timestampLabel = stamp))
                                    }
                                } else {
                                    listOf(EditableScanPage(workingBmp, filter = defaultFilter, timestampLabel = stamp))
                                }

                                scanPages = if (batchMode || activeTool == ScanFeatureMode.MEGA_SCAN || activeTool == ScanFeatureMode.ID_CARDS) {
                                    scanPages + newPages
                                } else {
                                    newPages
                                }
                                selectedIndex = scanPages.lastIndex.coerceAtLeast(0)
                                docStatus = "Added ${newPages.size} page(s) in ${activeTool.label} mode."
                            } else {
                                docStatus = "Camera capture cancelled."
                            }
                        }

                        val galleryPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickMultipleVisualMedia()
                        ) { uris: List<Uri> ->
                            if (uris.isNotEmpty()) {
                                isProcessingDoc = true
                                docStatus = "Loading ${uris.size} photo(s) from gallery..."
                                Thread {
                                    val loadedPages = mutableListOf<EditableScanPage>()
                                    for (u in uris) {
                                        val b = decodeBitmapFromUri(context, u)
                                        if (b != null) {
                                            val processed = if (activeTool == ScanFeatureMode.ID_PHOTO) makeIdPhotoBitmap(b) else b
                                            val stamp = if (activeTool == ScanFeatureMode.TIMESTAMP) {
                                                SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
                                            } else null
                                            if (activeTool == ScanFeatureMode.BOOK) {
                                                val split = splitBookSpreadBitmap(processed)
                                                if (split != null) {
                                                    loadedPages.add(EditableScanPage(split.first, filter = defaultFilter, timestampLabel = stamp))
                                                    loadedPages.add(EditableScanPage(split.second, filter = defaultFilter, timestampLabel = stamp))
                                                } else {
                                                    loadedPages.add(EditableScanPage(processed, filter = defaultFilter, timestampLabel = stamp))
                                                }
                                            } else {
                                                loadedPages.add(EditableScanPage(processed, filter = defaultFilter, timestampLabel = stamp))
                                            }
                                        }
                                    }
                                    runOnUiThread {
                                        if (loadedPages.isNotEmpty()) {
                                            val startIdx = scanPages.size
                                            scanPages = scanPages + loadedPages
                                            selectedIndex = startIdx.coerceIn(0, scanPages.lastIndex)
                                            docStatus = "Added ${loadedPages.size} gallery page(s)."
                                        } else {
                                            docStatus = "Could not load selected gallery photos."
                                        }
                                        isProcessingDoc = false
                                    }
                                }.start()
                            }
                        }

                        val importPdfForConvertLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                isProcessingDoc = true
                                docStatus = "Rendering PDF pages..."
                                Thread {
                                    val rendered = renderAllPages(context, uri)
                                    runOnUiThread {
                                        isProcessingDoc = false
                                        if (rendered.isNotEmpty()) {
                                            scanPages = scanPages + rendered.map {
                                                EditableScanPage(rawBitmap = it, filter = ScanFilterMode.ORIGINAL)
                                            }
                                            selectedIndex = scanPages.lastIndex
                                            docStatus = "Imported ${rendered.size} PDF page(s) into Scan Studio."
                                        } else {
                                            docStatus = "Could not read PDF pages."
                                        }
                                    }
                                }.start()
                            }
                        }

                        fun updateSelectedPage(transform: (EditableScanPage) -> EditableScanPage) {
                            if (scanPages.isEmpty()) return
                            val idx = selectedIndex.coerceIn(0, scanPages.lastIndex)
                            scanPages = scanPages.mapIndexed { i, p -> if (i == idx) transform(p) else p }
                        }

                        fun saveTextExport(extension: String, mimeType: String, content: String) {
                            val cleanName = sanitizeFileName(docTitle)
                            val fileName = "$cleanName.$extension"
                            try {
                                val bytes = content.toByteArray(Charsets.UTF_8)
                                val outUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val cv = ContentValues().apply {
                                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                    }
                                    context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                                } else {
                                    null
                                }
                                val writtenUri: Uri = if (outUri != null) {
                                    context.contentResolver.openOutputStream(outUri)?.use { stream ->
                                        stream.write(bytes)
                                    }
                                    outUri
                                } else {
                                    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    if (!dir.exists()) dir.mkdirs()
                                    val f = File(dir, fileName)
                                    FileOutputStream(f).use { stream ->
                                        stream.write(bytes)
                                    }
                                    Uri.fromFile(f)
                                }
                                lastExported = ExportedScanResult(
                                    title = fileName,
                                    format = extension.uppercase(),
                                    uris = listOf(writtenUri),
                                    bookItem = null
                                )
                                docStatus = "Exported $fileName to Downloads."
                            } catch (_: Exception) {
                                docStatus = "Could not export $fileName."
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            DarkVioletTextureBackground(modifier = Modifier.fillMaxSize())

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 24.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = VioletCardElevated.copy(alpha = 0.96f),
                                    border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.45f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { showDocScanner = false },
                                            border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = VioletCardSurface,
                                                contentColor = VioletInkLight
                                            )
                                        ) {
                                            Text("← Shelf", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "e-Bookish Scan Studio",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = VioletInkLight,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = { showAllFeaturesSheet = !showAllFeaturesSheet },
                                            border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.75f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = VioletCardSurface,
                                                contentColor = VioletAccentBright
                                            )
                                        ) {
                                            Text("All Tools", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(ScanFeatureMode.entries) { tool ->
                                        val isSelected = activeTool == tool
                                        OutlinedButton(
                                            onClick = {
                                                activeTool = tool
                                                if (tool == ScanFeatureMode.MEGA_SCAN) {
                                                    batchMode = true
                                                }
                                                docStatus = "Mode: ${tool.label}"
                                            },
                                            border = BorderStroke(
                                                width = if (isSelected) 1.5.dp else 1.dp,
                                                color = if (isSelected) VioletAccentBright else VioletGrainGlow.copy(alpha = 0.45f)
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) VioletCardElevated else VioletCardSurface.copy(alpha = 0.88f),
                                                contentColor = VioletInkLight
                                            )
                                        ) {
                                            val tag = tool.badge?.let { " [$it]" } ?: ""
                                            Text(
                                                text = if (isSelected) "${tool.label}$tag ✓" else "${tool.label}$tag",
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                if (showAllFeaturesSheet) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated),
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("All Scan & Document Features", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                                Text("Close ✕", color = VioletInkMuted, modifier = Modifier.clickable { showAllFeaturesSheet = false })
                                            }

                                            Text("Scan Documents & Enhance", style = MaterialTheme.typography.labelLarge, color = VioletAccentBright)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        activeTool = ScanFeatureMode.ID_CARDS
                                                        showAllFeaturesSheet = false
                                                        cameraSnapLauncher.launch(null)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("ID Cards", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                OutlinedButton(
                                                    onClick = {
                                                        activeTool = ScanFeatureMode.BOOK
                                                        showAllFeaturesSheet = false
                                                        cameraSnapLauncher.launch(null)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Book Split", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                OutlinedButton(
                                                    onClick = {
                                                        activeTool = ScanFeatureMode.TIMESTAMP
                                                        showAllFeaturesSheet = false
                                                        cameraSnapLauncher.launch(null)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Timestamp", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                            }

                                            Text("Convert Formats", style = MaterialTheme.typography.labelLarge, color = VioletAccentBright)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                OutlinedButton(
                                                    onClick = {
                                                        showAllFeaturesSheet = false
                                                        val body = extractedText.ifBlank { "Scanned document: $docTitle\nPages: ${scanPages.size}" }
                                                        saveTextExport("doc", "application/msword", body)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("To Word", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                OutlinedButton(
                                                    onClick = {
                                                        showAllFeaturesSheet = false
                                                        val lines = (extractedText.ifBlank { "Page,Title\n1,$docTitle" }).lines()
                                                        val csv = lines.joinToString("\n") { "\"${it.replace("\"", "\"\"")}\"" }
                                                        saveTextExport("csv", "text/csv", csv)
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("To Excel", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                OutlinedButton(
                                                    onClick = {
                                                        showAllFeaturesSheet = false
                                                        importPdfForConvertLauncher.launch(arrayOf("application/pdf"))
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("PDF→Img", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    showAllFeaturesSheet = false
                                                    if (scanPages.isNotEmpty()) {
                                                        val stitched = stitchLongImageBitmap(scanPages.map { it.renderEdited() })
                                                        if (stitched != null) {
                                                            val uris = savePagesAsJpegsWithUris(context, "${sanitizeFileName(docTitle)}_Long", listOf(stitched))
                                                            if (uris.isNotEmpty()) {
                                                                lastExported = ExportedScanResult("${docTitle}_Long", "JPEG", uris)
                                                                docStatus = "Exported stitched long image."
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) { Text("PDF / Pages to Long Image", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = docTitle,
                                    onValueChange = { docTitle = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Document Name", color = VioletInkMuted) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = VioletCardElevated.copy(alpha = 0.96f),
                                        unfocusedContainerColor = VioletCardSurface.copy(alpha = 0.94f),
                                        focusedBorderColor = VioletAccentBright,
                                        unfocusedBorderColor = VioletGrainGlow.copy(alpha = 0.45f),
                                        focusedTextColor = VioletInkLight,
                                        unfocusedTextColor = VioletInkLight
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { cameraSnapLauncher.launch(null) },
                                        enabled = !isProcessingDoc,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VioletAccentBright,
                                            contentColor = VioletInkLight
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Camera", maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            galleryPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        enabled = !isProcessingDoc,
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = VioletCardElevated.copy(alpha = 0.92f),
                                            contentColor = VioletInkLight
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1.15f)
                                    ) {
                                        Text("Import Images", maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            batchMode = !batchMode
                                            docStatus = if (batchMode) "Batch capture: ON" else "Single capture: ON"
                                        },
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (batchMode) VioletCardElevated else VioletCardSurface,
                                            contentColor = VioletInkLight
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (batchMode) "Batch" else "Single", maxLines = 1, softWrap = false)
                                    }
                                }

                                if (scanPages.isNotEmpty()) {
                                    val safeIdx = selectedIndex.coerceIn(0, scanPages.lastIndex)
                                    val currentPage = scanPages[safeIdx]
                                    val livePreviewBmp = remember(currentPage) { currentPage.renderEdited() }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(scanPages.indices.toList()) { idx ->
                                            OutlinedButton(
                                                onClick = { selectedIndex = idx },
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (idx == safeIdx) VioletCardElevated else VioletCardSurface.copy(alpha = 0.85f),
                                                    contentColor = VioletInkLight
                                                )
                                            ) {
                                                Text(
                                                    text = if (idx == safeIdx) "Page ${idx + 1} ✓" else "Page ${idx + 1}",
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Filter (Page ${safeIdx + 1})",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VioletInkLight
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(ScanFilterMode.entries) { mode ->
                                            val isSel = currentPage.filter == mode
                                            OutlinedButton(
                                                onClick = {
                                                    defaultFilter = mode
                                                    updateSelectedPage { it.copy(filter = mode) }
                                                },
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (isSel) VioletCardElevated else VioletCardSurface.copy(alpha = 0.85f),
                                                    contentColor = VioletInkLight
                                                )
                                            ) {
                                                Text(
                                                    text = if (isSel) "${mode.label} ✓" else mode.label,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                        border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.4f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(240.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Live Preview • Page ${safeIdx + 1} (${activeTool.label})",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = VioletInkLight
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    if (activeTool == ScanFeatureMode.SIGN || activeTool == ScanFeatureMode.SMART_ERASE) {
                                                        Text(
                                                            text = "Clear Mark",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = VioletAccentBright,
                                                            modifier = Modifier.clickable {
                                                                signDraftPoints = emptyList()
                                                                updateSelectedPage { it.copy(signaturePoints = emptyList(), erasePatch = null) }
                                                            }
                                                        )
                                                    }
                                                    Text(
                                                        text = "Delete Page",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.clickable {
                                                            val nextList = scanPages.filterIndexed { i, _ -> i != safeIdx }
                                                            scanPages = nextList
                                                            selectedIndex = 0
                                                        }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .pointerInput(activeTool, safeIdx) {
                                                        if (activeTool == ScanFeatureMode.SMART_ERASE) {
                                                            detectDragGestures { change, _ ->
                                                                change.consume()
                                                                val nx = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                val ny = (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                updateSelectedPage { it.copy(erasePatch = Offset(nx, ny)) }
                                                            }
                                                        } else if (activeTool == ScanFeatureMode.SIGN) {
                                                            detectDragGestures(
                                                                onDragStart = { start ->
                                                                    val pt = Offset(
                                                                        (start.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                                        (start.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                    )
                                                                    signDraftPoints = signDraftPoints + listOf(listOf(pt))
                                                                },
                                                                onDrag = { change, _ ->
                                                                    change.consume()
                                                                    val pt = Offset(
                                                                        (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                                                                        (change.position.y / size.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                                                                    )
                                                                    if (signDraftPoints.isNotEmpty()) {
                                                                        val updatedStrokes = signDraftPoints.dropLast(1) + listOf(signDraftPoints.last() + pt)
                                                                        signDraftPoints = updatedStrokes
                                                                        updateSelectedPage { it.copy(signaturePoints = updatedStrokes) }
                                                                    }
                                                                }
                                                            )
                                                        }
                                                    }
                                            ) {
                                                Image(
                                                    bitmap = livePreviewBmp.asImageBitmap(),
                                                    contentDescription = "Live Scan Page Preview",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Fit
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardSurface.copy(alpha = 0.95f)),
                                        border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("Doc Tools & Quick Actions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = VioletInkLight)
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val stamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).format(Date())
                                                            updateSelectedPage { it.copy(timestampLabel = if (it.timestampLabel == null) stamp else null) }
                                                            docStatus = "Toggled timestamp on Page ${safeIdx + 1}."
                                                        }
                                                    ) { Text("Timestamp", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val split = splitBookSpreadBitmap(currentPage.rawBitmap)
                                                            if (split != null) {
                                                                val p1 = EditableScanPage(split.first, filter = currentPage.filter)
                                                                val p2 = EditableScanPage(split.second, filter = currentPage.filter)
                                                                val nextList = scanPages.flatMapIndexed { i, p -> if (i == safeIdx) listOf(p1, p2) else listOf(p) }
                                                                scanPages = nextList
                                                                docStatus = "Split Page ${safeIdx + 1} into two book pages."
                                                            }
                                                        }
                                                    ) { Text("Split Book", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val idBmp = makeIdPhotoBitmap(currentPage.rawBitmap)
                                                            updateSelectedPage { it.copy(rawBitmap = idBmp) }
                                                            docStatus = "Formatted Page ${safeIdx + 1} as ID Photo."
                                                        }
                                                    ) { Text("ID Photo", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (extractedText.isBlank()) {
                                                                extractedText = "Document: ${docTitle.ifBlank { "Scan" }} (Page ${safeIdx + 1}, ${currentPage.filter.label})"
                                                            }
                                                            val note = if (activeTool == ScanFeatureMode.FORMULA) {
                                                                "Formula / Equation Note (${docTitle}):\n$extractedText"
                                                            } else {
                                                                "Extracted Note (${docTitle}):\n$extractedText"
                                                            }
                                                            extractedText = note
                                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                            cm?.setPrimaryClip(ClipData.newPlainText("e-Bookish OCR", note))
                                                            docStatus = "Extracted text & copied to clipboard."
                                                        }
                                                    ) { Text("Extract Text", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                            }

                                            OutlinedTextField(
                                                value = extractedText,
                                                onValueChange = { extractedText = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                maxLines = 3,
                                                placeholder = { Text("Extracted text, formula, or translation notes...", color = VioletInkMuted) },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = VioletInkLight,
                                                    unfocusedTextColor = VioletInkLight
                                                )
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        targetLang = if (targetLang == "Urdu") "English" else "Urdu"
                                                        val src = extractedText.ifBlank { docTitle }
                                                        translatedText = "[$targetLang] $src"
                                                        docStatus = "Prepared $targetLang translation note."
                                                    }
                                                ) { Text("Translate → $targetLang", maxLines = 1, softWrap = false, color = VioletInkLight) }

                                                if (translatedText.isNotBlank()) {
                                                    Text(
                                                        text = translatedText,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = VioletInkMuted,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardSurface.copy(alpha = 0.95f)),
                                        border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Live Side Cut Controls",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = VioletInkLight
                                                )
                                                OutlinedButton(
                                                    onClick = { updateSelectedPage { it.copy(crop = SideCrop()) } },
                                                    border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f))
                                                ) {
                                                    Text("Reset Sides", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Left: ${(currentPage.crop.left * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = VioletInkLight)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.left - 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(left = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("L -", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.left + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(left = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("L +", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Right: ${(currentPage.crop.right * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = VioletInkLight)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.right - 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(right = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("R -", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.right + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(right = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("R +", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Top: ${(currentPage.crop.top * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = VioletInkLight)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.top - 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(top = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("T -", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.top + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(top = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("T +", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Bottom: ${(currentPage.crop.bottom * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = VioletInkLight)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.bottom - 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(bottom = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("B -", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.bottom + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(bottom = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("B +", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.94f)),
                                        border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.4f)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(175.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Active mode: ${activeTool.label}. Tap 'Import Images' or 'Camera' to start scanning, or tap 'All Tools' for ID Cards, Book Split, Timestamp, To Word, To Excel, and Long Image.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = VioletInkLight,
                                                modifier = Modifier.padding(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (scanPages.isEmpty()) {
                                                docStatus = "Pick or snap at least 1 page first."
                                            } else {
                                                isProcessingDoc = true
                                                docStatus = "Generating PDF..."
                                                val renderedList = scanPages.map { it.renderEdited() }
                                                val cleanName = sanitizeFileName(docTitle)
                                                Thread {
                                                    val savedUri = savePagesAsPdf(context, cleanName, renderedList)
                                                    runOnUiThread {
                                                        isProcessingDoc = false
                                                        if (savedUri != null) {
                                                            val thumb = renderSinglePage(context, savedUri, 0, 0.5f)
                                                            val book = BookItem(
                                                                uriString = savedUri.toString(),
                                                                title = cleanName,
                                                                format = "PDF",
                                                                pageCount = renderedList.size,
                                                                lastPage = 0,
                                                                isFavorite = false,
                                                                isTrashed = false,
                                                                author = "Scanned Doc",
                                                                collection = "Scans",
                                                                cover = thumb,
                                                                metadataLoaded = true
                                                            )
                                                            addBookToShelf(book)
                                                            lastExported = ExportedScanResult(
                                                                title = cleanName,
                                                                format = "PDF",
                                                                uris = listOf(savedUri),
                                                                bookItem = book
                                                            )
                                                            docStatus = "Saved PDF: $cleanName.pdf (added to Shelf)"
                                                        } else {
                                                            docStatus = "Could not create PDF."
                                                        }
                                                    }
                                                }.start()
                                            }
                                        },
                                        enabled = !isProcessingDoc && scanPages.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VioletAccentBright,
                                            contentColor = VioletInkLight
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Save PDF", maxLines = 1, softWrap = false) }

                                    Button(
                                        onClick = {
                                            if (scanPages.isEmpty()) {
                                                docStatus = "Pick or snap at least 1 page first."
                                            } else {
                                                isProcessingDoc = true
                                                docStatus = "Saving JPEG(s)..."
                                                val renderedList = scanPages.map { it.renderEdited() }
                                                val cleanName = sanitizeFileName(docTitle)
                                                Thread {
                                                    val savedUris = savePagesAsJpegsWithUris(context, cleanName, renderedList)
                                                    runOnUiThread {
                                                        isProcessingDoc = false
                                                        if (savedUris.isNotEmpty()) {
                                                            lastExported = ExportedScanResult(
                                                                title = cleanName,
                                                                format = "JPEG",
                                                                uris = savedUris,
                                                                bookItem = null
                                                            )
                                                            docStatus = "Saved ${savedUris.size} JPEG file(s)!"
                                                        } else {
                                                            docStatus = "Could not save JPEG."
                                                        }
                                                    }
                                                }.start()
                                            }
                                        },
                                        enabled = !isProcessingDoc && scanPages.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = VioletAccentBright,
                                            contentColor = VioletInkLight
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Save JPEG", maxLines = 1, softWrap = false) }

                                    OutlinedButton(
                                        onClick = {
                                            scanPages = listOf()
                                            selectedIndex = 0
                                            lastExported = null
                                            signDraftPoints = emptyList()
                                            docStatus = "Cleared."
                                        },
                                        enabled = !isProcessingDoc && (scanPages.isNotEmpty() || lastExported != null),
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = VioletCardElevated.copy(alpha = 0.9f),
                                            contentColor = VioletInkLight
                                        )
                                    ) { Text("Clear", maxLines = 1, softWrap = false) }
                                }

                                if (docStatus.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = docStatus,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = VioletInkLight
                                    )
                                }

                                val exported = lastExported
                                if (exported != null && exported.uris.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Exported ${exported.format}: ${exported.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = VioletInkLight
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { shareExportedScan(context, exported) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = VioletAccentBright,
                                                        contentColor = VioletInkLight
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Share", maxLines = 1, softWrap = false) }
                                                if (exported.bookItem != null) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val b = exported.bookItem
                                                            showDocScanner = false
                                                            openBook(b)
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) { Text("Read PDF", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        lastExported = null
                                                        showDocScanner = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Done", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    } else if (activeBook != null) {
                        val currentBook = activeBook as BookItem
                        BackHandler {
                            stopSpeaking()
                            activeBook = null
                            readerPages = listOf()
                            textBookSections = listOf()
                            showBrightnessSlider = false
                        }

                        var showReaderBars by remember { mutableStateOf(false) }
                        var showAaPanel by remember { mutableStateOf(false) }
                        var showThumbnailsStrip by remember { mutableStateOf(false) }
                        var showQuoteComposer by remember { mutableStateOf(false) }
                        var showDictLookup by remember { mutableStateOf(false) }
                        var showBookDetails by remember { mutableStateOf(false) }

                        var quoteDraft by remember { mutableStateOf("") }
                        var quoteColorChoice by remember { mutableStateOf(QuoteColorOption.VIOLET) }
                        var dictWordInput by remember { mutableStateOf("") }
                        var dictDefInput by remember { mutableStateOf("") }
                        var readerNotice by remember { mutableStateOf("") }

                        val activeCustomFont = remember(selectedCustomFontPath, customFonts) {
                            val match = customFonts.firstOrNull { it.localPath == selectedCustomFontPath }
                            if (match != null) loadCustomFontFamily(match) else null
                        }
                        val effectiveFontFamily = activeCustomFont ?: epubFontStyle.family

                        if (isTextPagedFormat(currentBook.format)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(epubPageTheme.bgColor)
                            ) {
                                if (isLoadingReader) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = epubPageTheme.textColor)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text("Opening ${currentBook.format}...", color = epubPageTheme.textColor)
                                        }
                                    }
                                } else if (textBookSections.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Could not extract readable text from this ${currentBook.format} file.",
                                            color = epubPageTheme.textColor,
                                            modifier = Modifier.padding(20.dp)
                                        )
                                    }
                                } else {
                                    val maxIdx = textBookSections.size - 1
                                    val safeStart = currentBook.lastPage.coerceIn(0, maxIdx)
                                    val pagerState = rememberPagerState(
                                        initialPage = safeStart,
                                        pageCount = { textBookSections.size }
                                    )

                                    LaunchedEffect(pagerState.currentPage) {
                                        val updatedBooks = books.map {
                                            if (it.uriString == currentBook.uriString) {
                                                it.copy(lastPage = pagerState.currentPage)
                                            } else {
                                                it
                                            }
                                        }
                                        books = updatedBooks
                                        saveBooks(context, updatedBooks)
                                    }

                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { chapIndex ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = epubSideMarginDp.dp, vertical = 22.dp)
                                        ) {
                                            val computedLineSp = (epubFontSizeSp * epubLineSpacingMult).sp
                                            val formattedChapter = remember(textBookSections[chapIndex], epubParagraphIndentLevel) {
                                                applyParagraphIndent(textBookSections[chapIndex], epubParagraphIndentLevel)
                                            }
                                            Text(
                                                text = formattedChapter,
                                                color = epubPageTheme.textColor,
                                                fontFamily = effectiveFontFamily,
                                                fontSize = epubFontSizeSp.sp,
                                                lineHeight = computedLineSp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.16f)
                                            .clickable {
                                                if (pagerState.currentPage > 0) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.68f)
                                            .clickable {
                                                showReaderBars = !showReaderBars
                                                if (!showReaderBars) {
                                                    showAaPanel = false
                                                    showBrightnessSlider = false
                                                    showThumbnailsStrip = false
                                                    showQuoteComposer = false
                                                    showDictLookup = false
                                                    showBookDetails = false
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.16f)
                                            .clickable {
                                                if (pagerState.currentPage < textBookSections.size - 1) {
                                                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                }
                                            }
                                    )

                                    if (showReaderBars) {
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .fillMaxWidth()
                                                .background(VioletBarColor)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        stopSpeaking()
                                                        activeBook = null
                                                        textBookSections = listOf()
                                                        showBrightnessSlider = false
                                                    },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("← Shelf", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }

                                                Text(
                                                    text = currentBook.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = VioletBarText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedButton(
                                                    onClick = { showBrightnessSlider = !showBrightnessSlider },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("☀ ${(screenBrightness * 100).toInt()}%", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }

                                                OutlinedButton(
                                                    onClick = { showAaPanel = !showAaPanel },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("Aa", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                                            }

                                            LazyRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(VioletCardSurface)
                                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (isSpeaking) stopSpeaking()
                                                            else speakText(textBookSections[pagerState.currentPage])
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text(if (isSpeaking) "⏹ Stop Audio" else "🔊 Play Audio", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (quoteDraft.isBlank()) {
                                                                quoteDraft = textBookSections[pagerState.currentPage].take(220)
                                                            }
                                                            showQuoteComposer = !showQuoteComposer
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("❝ Quote", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = { showDictLookup = !showDictLookup },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("A Dictionary", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = { showThumbnailsStrip = !showThumbnailsStrip },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("▦ Sections", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                }
                                                item {
                                                    OutlinedButton(
                                                        onClick = { showBookDetails = !showBookDetails },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) { Text("ⓘ Details", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                }
                                            }

                                            if (readerNotice.isNotEmpty()) {
                                                Text(
                                                    text = readerNotice,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = VioletInkLight,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(horizontal = 14.dp, vertical = 4.dp)
                                                )
                                            }

                                            if (showBrightnessSlider) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Brightness: ${(screenBrightness * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = VioletInkLight)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness - 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("☀ -", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness + 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("☀ +", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = 0.75f
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("75%", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                    }
                                                }
                                            }

                                            if (showThumbnailsStrip) {
                                                LazyRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(textBookSections.indices.toList()) { idx ->
                                                        Card(
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (idx == pagerState.currentPage) VioletAccentBright else VioletCardSurface
                                                            ),
                                                            modifier = Modifier
                                                                .width(120.dp)
                                                                .height(84.dp)
                                                                .clickable {
                                                                    scope.launch { pagerState.scrollToPage(idx) }
                                                                    showThumbnailsStrip = false
                                                                }
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp)) {
                                                                Text("Sec ${idx + 1}", fontWeight = FontWeight.Bold, color = VioletInkLight, style = MaterialTheme.typography.labelSmall)
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(textBookSections[idx], color = VioletInkMuted, maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (showQuoteComposer) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("Save Quote (${quoteColorChoice.label})", color = VioletInkLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        QuoteColorOption.entries.forEach { qc ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(28.dp)
                                                                    .clip(CircleShape)
                                                                    .background(qc.swatch)
                                                                    .border(
                                                                        width = if (quoteColorChoice == qc) 3.dp else 1.dp,
                                                                        color = VioletInkLight,
                                                                        shape = CircleShape
                                                                    )
                                                                    .clickable { quoteColorChoice = qc }
                                                            )
                                                        }
                                                    }
                                                    OutlinedTextField(
                                                        value = quoteDraft,
                                                        onValueChange = { quoteDraft = it },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        maxLines = 3,
                                                        placeholder = { Text("Quote text...", color = VioletInkMuted) },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VioletInkLight, unfocusedTextColor = VioletInkLight)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = {
                                                                if (quoteDraft.isNotBlank()) {
                                                                    val entry = QuoteEntry(
                                                                        id = "${System.currentTimeMillis()}_${Random.nextInt(9999)}",
                                                                        bookTitle = currentBook.title,
                                                                        sectionIndex = pagerState.currentPage,
                                                                        text = quoteDraft.trim(),
                                                                        colorName = quoteColorChoice.name
                                                                    )
                                                                    val nextQ = listOf(entry) + quotes
                                                                    quotes = nextQ
                                                                    saveQuotes(context, nextQ)
                                                                    showQuoteComposer = false
                                                                    readerNotice = "Saved quote with ${quoteColorChoice.label} tag."
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
                                                        ) { Text("Save Quote", color = VioletInkLight) }
                                                        OutlinedButton(onClick = { showQuoteComposer = false }) { Text("Cancel", color = VioletInkLight) }
                                                    }
                                                }
                                            }

                                            if (showDictLookup) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("Dictionary Note / Lookup", color = VioletInkLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                                    OutlinedTextField(
                                                        value = dictWordInput,
                                                        onValueChange = { dictWordInput = it },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Word or term...", color = VioletInkMuted) },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VioletInkLight, unfocusedTextColor = VioletInkLight)
                                                    )
                                                    OutlinedTextField(
                                                        value = dictDefInput,
                                                        onValueChange = { dictDefInput = it },
                                                        singleLine = true,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        placeholder = { Text("Definition / note...", color = VioletInkMuted) },
                                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VioletInkLight, unfocusedTextColor = VioletInkLight)
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        Button(
                                                            onClick = {
                                                                if (dictWordInput.isNotBlank()) {
                                                                    val entry = DictionaryEntry(
                                                                        word = dictWordInput.trim(),
                                                                        definition = dictDefInput.trim().ifBlank { "Saved from ${currentBook.title}" },
                                                                        sourceBook = currentBook.title
                                                                    )
                                                                    val nextD = listOf(entry) + dictionaryEntries.filter { !it.word.equals(entry.word, true) }
                                                                    dictionaryEntries = nextD
                                                                    saveDictionary(context, nextD)
                                                                    dictWordInput = ""
                                                                    dictDefInput = ""
                                                                    showDictLookup = false
                                                                    readerNotice = "Saved '${entry.word}' to Dictionary."
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
                                                        ) { Text("Save Word", color = VioletInkLight) }
                                                        OutlinedButton(onClick = { showDictLookup = false }) { Text("Close", color = VioletInkLight) }
                                                    }
                                                }
                                            }

                                            if (showBookDetails) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = VioletCardElevated)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Book Details", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                                        Text("Title: ${currentBook.title}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Author: ${currentBook.author}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Format: ${currentBook.format} • Sections: ${textBookSections.size}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Collection: ${currentBook.collection}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        OutlinedButton(onClick = { showBookDetails = false }) { Text("Close", color = VioletInkLight) }
                                                    }
                                                }
                                            }

                                            if (showAaPanel) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletBarColor.copy(alpha = 0.98f))
                                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("Page Background: ${epubPageTheme.label}", style = MaterialTheme.typography.labelLarge, color = VioletBarText)
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                        items(EpubPageTheme.entries) { themeOpt ->
                                                            val isCurr = epubPageTheme == themeOpt
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                modifier = Modifier.clickable { epubPageTheme = themeOpt }
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(34.dp)
                                                                        .clip(CircleShape)
                                                                        .background(themeOpt.bgColor)
                                                                        .border(
                                                                            width = if (isCurr) 3.dp else 1.dp,
                                                                            color = if (isCurr) VioletBarText else VioletBarText.copy(alpha = 0.5f),
                                                                            shape = CircleShape
                                                                        )
                                                                )
                                                                Spacer(modifier = Modifier.height(2.dp))
                                                                Text(themeOpt.label, style = MaterialTheme.typography.labelSmall, color = VioletBarText, maxLines = 1)
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Font Size: ${epubFontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelLarge, color = VioletBarText)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubFontSizeSp = (epubFontSizeSp - 2f).coerceAtLeast(14f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                            ) { Text("A-", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                            OutlinedButton(
                                                                onClick = { epubFontSizeSp = (epubFontSizeSp + 2f).coerceAtMost(28f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                            ) { Text("A+", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        for (fOpt in EpubFontStyleOption.entries) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    selectedCustomFontPath = null
                                                                    epubFontStyle = fOpt
                                                                },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                modifier = Modifier.weight(1f),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                                            ) {
                                                                val isSel = selectedCustomFontPath == null && epubFontStyle == fOpt
                                                                Text(
                                                                    text = if (isSel) "${fOpt.label} ✓" else fOpt.label,
                                                                    color = VioletBarText,
                                                                    fontFamily = fOpt.family,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                        OutlinedButton(
                                                            onClick = { customFontPicker.launch(arrayOf("*/*")) },
                                                            border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                        ) { Text("+ Font", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                    }

                                                    if (customFonts.isNotEmpty()) {
                                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            items(customFonts) { cf ->
                                                                val sel = selectedCustomFontPath == cf.localPath
                                                                OutlinedButton(
                                                                    onClick = { selectedCustomFontPath = if (sel) null else cf.localPath },
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                ) { Text(if (sel) "${cf.name} ✓" else cf.name, color = VioletBarText, maxLines = 1, softWrap = false) }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Line Spacing: ${String.format(Locale.US, "%.2f", epubLineSpacingMult)}x", style = MaterialTheme.typography.labelLarge, color = VioletBarText)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubLineSpacingMult = (epubLineSpacingMult - 0.15f).coerceAtLeast(1.1f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) { Text("Tighter", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                            OutlinedButton(
                                                                onClick = { epubLineSpacingMult = (epubLineSpacingMult + 0.15f).coerceAtMost(2.0f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) { Text("Wider", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Side Margins: ${epubSideMarginDp}dp", style = MaterialTheme.typography.labelLarge, color = VioletBarText)
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubSideMarginDp = (epubSideMarginDp - 6).coerceAtLeast(12) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) { Text("Narrow", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                            OutlinedButton(
                                                                onClick = { epubSideMarginDp = (epubSideMarginDp + 6).coerceAtMost(48) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) { Text("Wide", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                        }
                                                    }

                                                    val indentLabel = when (epubParagraphIndentLevel) {
                                                        0 -> "Off"
                                                        1 -> "1 Tab"
                                                        else -> "2 Tabs"
                                                    }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("Paragraph Indent: $indentLabel", style = MaterialTheme.typography.labelLarge, color = VioletBarText)
                                                        OutlinedButton(
                                                            onClick = { epubParagraphIndentLevel = (epubParagraphIndentLevel + 1) % 3 },
                                                            border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) { Text("Cycle Indent", color = VioletBarText, maxLines = 1, softWrap = false) }
                                                    }
                                                }
                                            }
                                        }

                                        val pct = ((pagerState.currentPage + 1) * 100) / textBookSections.size
                                        val progFrac = ((pagerState.currentPage + 1).toFloat() / textBookSections.size.toFloat()).coerceIn(0f, 1f)
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(VioletBarColor)
                                                .navigationBarsPadding()
                                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = progFrac,
                                                color = VioletAccentBright,
                                                trackColor = VioletBarText.copy(alpha = 0.25f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        if (pagerState.currentPage > 0) {
                                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                                                        }
                                                    },
                                                    enabled = pagerState.currentPage > 0,
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) { Text("Prev Sec", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }

                                                Text(
                                                    text = "${pagerState.currentPage + 1} of ${textBookSections.size} ($pct%)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletBarText
                                                )

                                                OutlinedButton(
                                                    onClick = {
                                                        if (pagerState.currentPage < textBookSections.size - 1) {
                                                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                                                        }
                                                    },
                                                    enabled = pagerState.currentPage < textBookSections.size - 1,
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) { Text("Next Sec", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(VioletBaseDark)
                            ) {
                                if (isLoadingReader) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = VioletAccentBright)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Opening book...", color = VioletInkLight)
                                        }
                                    }
                                } else if (readerPages.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Could not open this PDF file.", color = VioletInkLight)
                                    }
                                } else {
                                    val maxIdx = readerPages.size - 1
                                    val safeStart = currentBook.lastPage.coerceIn(0, maxIdx)
                                    val pagerState = rememberPagerState(initialPage = safeStart, pageCount = { readerPages.size })

                                    LaunchedEffect(pagerState.currentPage) {
                                        val updatedBooks = books.map {
                                            if (it.uriString == currentBook.uriString) {
                                                it.copy(lastPage = pagerState.currentPage)
                                            } else {
                                                it
                                            }
                                        }
                                        books = updatedBooks
                                        saveBooks(context, updatedBooks)
                                    }

                                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                                        val pageBmp: Bitmap = readerPages[pageIndex]
                                        var pinScale by remember(pageIndex, currentBook.uriString) { mutableFloatStateOf(1f) }
                                        var panOffset by remember(pageIndex, currentBook.uriString) { mutableStateOf(Offset.Zero) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(pageIndex) {
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        val newScale = (pinScale * zoom).coerceIn(1f, 3.5f)
                                                        pinScale = newScale
                                                        panOffset = if (newScale <= 1f) {
                                                            Offset.Zero
                                                        } else {
                                                            Offset(x = panOffset.x + pan.x, y = panOffset.y + pan.y)
                                                        }
                                                    }
                                                }
                                                .clickable {
                                                    showReaderBars = !showReaderBars
                                                    if (!showReaderBars) {
                                                        showBrightnessSlider = false
                                                        showThumbnailsStrip = false
                                                        showBookDetails = false
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = pageBmp.asImageBitmap(),
                                                contentDescription = "Page ${pageIndex + 1}",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = pinScale
                                                        scaleY = pinScale
                                                        translationX = panOffset.x
                                                        translationY = panOffset.y
                                                    },
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    if (showReaderBars) {
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .fillMaxWidth()
                                                .background(VioletBarColor)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        activeBook = null
                                                        readerPages = listOf()
                                                        showBrightnessSlider = false
                                                    },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) { Text("← Shelf", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }

                                                Text(
                                                    text = currentBook.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = VioletBarText,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedButton(
                                                    onClick = { showThumbnailsStrip = !showThumbnailsStrip },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("▦ Pages", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }

                                                OutlinedButton(
                                                    onClick = { showBookDetails = !showBookDetails },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) { Text("ⓘ", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }

                                                OutlinedButton(
                                                    onClick = { showBrightnessSlider = !showBrightnessSlider },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                                                ) { Text("☀ ${(screenBrightness * 100).toInt()}%", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                                            }

                                            if (showThumbnailsStrip) {
                                                LazyRow(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(readerPages.indices.toList()) { idx ->
                                                        Card(
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(
                                                                width = if (idx == pagerState.currentPage) 2.dp else 1.dp,
                                                                color = if (idx == pagerState.currentPage) VioletAccentBright else VioletGrainGlow.copy(alpha = 0.4f)
                                                            ),
                                                            modifier = Modifier
                                                                .width(64.dp)
                                                                .height(90.dp)
                                                                .clickable {
                                                                    scope.launch { pagerState.scrollToPage(idx) }
                                                                    showThumbnailsStrip = false
                                                                }
                                                        ) {
                                                            Image(
                                                                bitmap = readerPages[idx].asImageBitmap(),
                                                                contentDescription = "Thumb ${idx + 1}",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (showBookDetails) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp),
                                                    colors = CardDefaults.cardColors(containerColor = VioletCardElevated)
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Book Details", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                                        Text("Title: ${currentBook.title}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Author: ${currentBook.author}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Format: ${currentBook.format} • Pages: ${readerPages.size}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        Text("Collection: ${currentBook.collection}", color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                        OutlinedButton(onClick = { showBookDetails = false }) { Text("Close", color = VioletInkLight) }
                                                    }
                                                }
                                            }

                                            if (showBrightnessSlider) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(VioletCardElevated)
                                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Brightness: ${(screenBrightness * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = VioletInkLight)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness - 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("☀ -", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness + 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("☀ +", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = 0.75f
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) { Text("75%", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                    }
                                                }
                                            }
                                        }

                                        val pagePct = ((pagerState.currentPage + 1) * 100) / readerPages.size
                                        val pageFrac = ((pagerState.currentPage + 1).toFloat() / readerPages.size.toFloat()).coerceIn(0f, 1f)
                                        Column(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .background(VioletBarColor)
                                                .navigationBarsPadding()
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = pageFrac,
                                                color = VioletAccentBright,
                                                trackColor = VioletBarText.copy(alpha = 0.25f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("${pagerState.currentPage + 1} of ${readerPages.size}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = VioletBarText)
                                                Text("$pagePct%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = VioletBarText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        val activeBooks = books.filter { !it.isTrashed }
                        val trashedBooks = books.filter { it.isTrashed }

                        val sectionBaseList = when (section) {
                            ReadEraSection.READING_NOW -> activeBooks.filter {
                                it.lastPage > 0 && it.lastPage < (it.pageCount - 1).coerceAtLeast(0)
                            }
                            ReadEraSection.BOOKS_AND_DOCS -> activeBooks
                            ReadEraSection.FAVORITES -> activeBooks.filter { it.isFavorite }
                            ReadEraSection.TO_READ -> activeBooks.filter { it.lastPage == 0 }
                            ReadEraSection.HAVE_READ -> activeBooks.filter {
                                it.pageCount > 0 && it.lastPage >= it.pageCount - 1
                            }
                            ReadEraSection.AUTHORS -> {
                                if (authorFilter == "ALL") activeBooks
                                else activeBooks.filter { it.author.equals(authorFilter, ignoreCase = true) }
                            }
                            ReadEraSection.SERIES -> activeBooks.filter {
                                it.title.contains("1") || it.title.contains("2") ||
                                    it.title.contains("3") || it.title.contains("Vol", ignoreCase = true) ||
                                    it.title.contains("Part", ignoreCase = true)
                            }.ifEmpty { activeBooks }
                            ReadEraSection.COLLECTIONS -> {
                                if (collectionFilter == "ALL") activeBooks
                                else activeBooks.filter { it.collection.equals(collectionFilter, ignoreCase = true) }
                            }
                            ReadEraSection.FORMATS -> {
                                if (formatFilter == "ALL") activeBooks
                                else activeBooks.filter { it.format.equals(formatFilter, ignoreCase = true) }
                            }
                            ReadEraSection.QUOTES -> emptyList<BookItem>()
                            ReadEraSection.DICTIONARY -> emptyList<BookItem>()
                            ReadEraSection.TRASH -> trashedBooks
                            ReadEraSection.SETTINGS -> activeBooks
                        }

                        val filteredBooks = sectionBaseList.filter {
                            it.title.contains(searchQuery, ignoreCase = true)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            DarkVioletTextureBackground(modifier = Modifier.fillMaxSize())

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 18.dp, vertical = 28.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = VioletCardElevated.copy(alpha = 0.96f),
                                    border = BorderStroke(1.2.dp, VioletGrainGlow.copy(alpha = 0.45f)),
                                    shadowElevation = 6.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedButton(
                                                onClick = { showDrawer = !showDrawer },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = VioletCardSurface,
                                                    contentColor = VioletInkLight
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) { Text("☰ Menu", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold) }

                                            Text(
                                                text = section.label,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = VioletInkLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewMode = when (viewMode) {
                                                    LibraryViewMode.GRID_3 -> LibraryViewMode.GRID_2
                                                    LibraryViewMode.GRID_2 -> LibraryViewMode.LIST
                                                    LibraryViewMode.LIST -> LibraryViewMode.GRID_3
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = VioletCardSurface,
                                                contentColor = VioletInkLight
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                        ) { Text(viewMode.label, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold) }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        Box {
                                            OutlinedButton(
                                                onClick = { showOverflowMenu = true },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = VioletCardSurface,
                                                    contentColor = VioletInkLight
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                            ) { Text("⋮", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold) }

                                            DropdownMenu(
                                                expanded = showOverflowMenu,
                                                onDismissRequest = { showOverflowMenu = false },
                                                containerColor = VioletCardElevated
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Import Books", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        addBookLauncher.launch(arrayOf("*/*"))
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Scan Folder", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        startFolderScan()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Doc Scanner", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        showDocScanner = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Synchronization", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        showSyncDialog = true
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Open a random book", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        if (activeBooks.isNotEmpty()) {
                                                            openBook(activeBooks[Random.nextInt(activeBooks.size)])
                                                        } else {
                                                            scanNotice = "No active books to open."
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("Rebuild book covers", color = VioletInkLight) },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        rebuildAllCovers()
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = if (selectionMode) "Cancel selection" else "Select all",
                                                            color = VioletInkLight
                                                        )
                                                    },
                                                    onClick = {
                                                        showOverflowMenu = false
                                                        if (!selectionMode) {
                                                            selectionMode = true
                                                            selectedUris = sectionBaseList.map { it.uriString }.toSet()
                                                        } else {
                                                            selectionMode = false
                                                            selectedUris = emptySet()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (showSyncDialog) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated),
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Synchronization", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                            Text(syncStatusText, style = MaterialTheme.typography.bodySmall, color = VioletInkMuted)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = { exportSyncSnapshot() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
                                                ) { Text("Export Sync", color = VioletInkLight, maxLines = 1, softWrap = false) }
                                                OutlinedButton(onClick = { syncImportLauncher.launch(arrayOf("application/json", "*/*")) }) {
                                                    Text("Import Sync", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                }
                                                OutlinedButton(onClick = { showSyncDialog = false }) {
                                                    Text("Close", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (section == ReadEraSection.FORMATS) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("ALL", "PDF", "EPUB", "MOBI", "AZW", "AZW3", "FB2", "TXT", "DOCX")) { fmt ->
                                            OutlinedButton(
                                                onClick = { formatFilter = fmt },
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (formatFilter == fmt) VioletCardElevated else VioletCardSurface.copy(alpha = 0.88f),
                                                    contentColor = VioletInkLight
                                                )
                                            ) { Text(if (formatFilter == fmt) "$fmt ✓" else fmt, maxLines = 1, softWrap = false) }
                                        }
                                    }
                                } else if (section == ReadEraSection.AUTHORS) {
                                    val authorList = listOf("ALL") + activeBooks.map { it.author }.distinct()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(authorList) { auth ->
                                            OutlinedButton(
                                                onClick = { authorFilter = auth },
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (authorFilter == auth) VioletCardElevated else VioletCardSurface.copy(alpha = 0.88f),
                                                    contentColor = VioletInkLight
                                                )
                                            ) { Text(if (authorFilter == auth) "$auth ✓" else auth, maxLines = 1, softWrap = false) }
                                        }
                                    }
                                } else if (section == ReadEraSection.COLLECTIONS) {
                                    val colList = listOf("ALL", "General", "Documents", "Ebooks", "Notes & Docs", "Scans")
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(colList) { col ->
                                            OutlinedButton(
                                                onClick = { collectionFilter = col },
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (collectionFilter == col) VioletCardElevated else VioletCardSurface.copy(alpha = 0.88f),
                                                    contentColor = VioletInkLight
                                                )
                                            ) { Text(if (collectionFilter == col) "$col ✓" else col, maxLines = 1, softWrap = false) }
                                        }
                                    }
                                } else if (section == ReadEraSection.SETTINGS) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                        border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("e-Bookish Settings & My Fonts", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { useDefaultCovers = !useDefaultCovers }) {
                                                    Text(if (useDefaultCovers) "Default Cover: ON" else "Default Cover: OFF", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
                                                OutlinedButton(onClick = { customFontPicker.launch(arrayOf("*/*")) }) {
                                                    Text("My Fonts (+)", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
                                            }
                                            if (customFonts.isNotEmpty()) {
                                                Text("Installed custom fonts: ${customFonts.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall, color = VioletInkMuted)
                                            }
                                        }
                                    }
                                }

                                if (selectionMode) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${selectedUris.size} selected",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = VioletInkLight,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                val updated = if (section == ReadEraSection.TRASH) {
                                                    books.filter { it.uriString !in selectedUris }
                                                } else {
                                                    books.map {
                                                        if (it.uriString in selectedUris) it.copy(isTrashed = true) else it
                                                    }
                                                }
                                                books = updated
                                                saveBooks(context, updated)
                                                selectionMode = false
                                                selectedUris = emptySet()
                                            }
                                        ) {
                                            Text(
                                                text = if (section == ReadEraSection.TRASH) "Delete Forever" else "Move to Trash",
                                                maxLines = 1,
                                                softWrap = false,
                                                color = VioletInkLight
                                            )
                                        }
                                    }
                                }

                                if (scanNotice.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = scanNotice, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = VioletInkLight)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                if (section == ReadEraSection.QUOTES) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        item {
                                            OutlinedButton(onClick = { quoteColorFilter = "ALL" }) {
                                                Text(if (quoteColorFilter == "ALL") "All ✓" else "All", color = VioletInkLight)
                                            }
                                        }
                                        items(QuoteColorOption.entries) { qc ->
                                            OutlinedButton(onClick = { quoteColorFilter = qc.name }) {
                                                Text(if (quoteColorFilter == qc.name) "${qc.label} ✓" else qc.label, color = qc.swatch)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val visibleQuotes = quotes.filter {
                                        (quoteColorFilter == "ALL" || it.colorName == quoteColorFilter) &&
                                            (searchQuery.isBlank() || it.text.contains(searchQuery, true) || it.bookTitle.contains(searchQuery, true))
                                    }
                                    if (visibleQuotes.isEmpty()) {
                                        Card(colors = CardDefaults.cardColors(containerColor = VioletCardElevated), modifier = Modifier.fillMaxWidth()) {
                                            Text("No saved quotes yet. Open any text book, tap the screen, and tap ❝ Quote.", color = VioletInkLight, modifier = Modifier.padding(16.dp))
                                        }
                                    } else {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                            items(visibleQuotes, key = { it.id }) { q ->
                                                val qSwatch = QuoteColorOption.entries.firstOrNull { it.name == q.colorName }?.swatch ?: VioletAccentBright
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = VioletCardElevated),
                                                    border = BorderStroke(1.5.dp, qSwatch),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("“${q.text}”", color = VioletInkLight, style = MaterialTheme.typography.bodyMedium)
                                                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                            Text("${q.bookTitle} • Sec ${q.sectionIndex + 1}", color = VioletInkMuted, style = MaterialTheme.typography.labelSmall)
                                                            Text("Remove", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable {
                                                                val nextQ = quotes.filter { it.id != q.id }
                                                                quotes = nextQ
                                                                saveQuotes(context, nextQ)
                                                            })
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else if (section == ReadEraSection.DICTIONARY) {
                                    Card(colors = CardDefaults.cardColors(containerColor = VioletCardElevated), modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Add Dictionary Word", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                            OutlinedTextField(
                                                value = newDictWord,
                                                onValueChange = { newDictWord = it },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Word...", color = VioletInkMuted) },
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VioletInkLight, unfocusedTextColor = VioletInkLight)
                                            )
                                            OutlinedTextField(
                                                value = newDictDef,
                                                onValueChange = { newDictDef = it },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                placeholder = { Text("Meaning / definition...", color = VioletInkMuted) },
                                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = VioletInkLight, unfocusedTextColor = VioletInkLight)
                                            )
                                            Button(
                                                onClick = {
                                                    if (newDictWord.isNotBlank() && newDictDef.isNotBlank()) {
                                                        val entry = DictionaryEntry(newDictWord.trim(), newDictDef.trim(), "Manual")
                                                        val nextD = listOf(entry) + dictionaryEntries.filter { !it.word.equals(entry.word, true) }
                                                        dictionaryEntries = nextD
                                                        saveDictionary(context, nextD)
                                                        newDictWord = ""
                                                        newDictDef = ""
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
                                            ) { Text("Save Definition", color = VioletInkLight) }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val visibleDict = dictionaryEntries.filter {
                                        searchQuery.isBlank() || it.word.contains(searchQuery, true) || it.definition.contains(searchQuery, true)
                                    }
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                                        items(visibleDict, key = { it.word }) { d ->
                                            Card(colors = CardDefaults.cardColors(containerColor = VioletCardElevated), modifier = Modifier.fillMaxWidth()) {
                                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(d.word, fontWeight = FontWeight.Bold, color = VioletInkLight)
                                                        Text(d.definition, color = VioletInkMuted, style = MaterialTheme.typography.bodySmall)
                                                    }
                                                    Text("Delete", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.clickable {
                                                        val nextD = dictionaryEntries.filter { it.word != d.word }
                                                        dictionaryEntries = nextD
                                                        saveDictionary(context, nextD)
                                                    })
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Search in ${section.label}...", maxLines = 1, color = VioletInkMuted) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = VioletCardElevated.copy(alpha = 0.96f),
                                            unfocusedContainerColor = VioletCardSurface.copy(alpha = 0.94f),
                                            focusedBorderColor = VioletAccentBright,
                                            unfocusedBorderColor = VioletGrainGlow.copy(alpha = 0.45f),
                                            focusedTextColor = VioletInkLight,
                                            unfocusedTextColor = VioletInkLight
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    if (showAboutDialog) {
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = VioletCardElevated),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 14.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp)) {
                                                Text("e-Bookish", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Supports PDF, EPUB, MOBI, AZW/AZW3, FB2, TXT, and DOCX with embedded cover extraction, brightness control, background audio, quotes, dictionary, thumbnails, custom fonts, and Document Scanner.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = VioletInkMuted
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(onClick = { showAboutDialog = false }) { Text("Close", maxLines = 1, softWrap = false, color = VioletInkLight) }
                                            }
                                        }
                                    }

                                    if (filteredBooks.isEmpty()) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Card(
                                                shape = RoundedCornerShape(18.dp),
                                                colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.94f)),
                                                border = BorderStroke(1.2.dp, VioletGrainGlow.copy(alpha = 0.4f)),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    ReadEraGraphicIcon(kind = "BOOKS", tint = VioletAccentBright, modifier = Modifier.size(36.dp))
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Text(
                                                        text = if (section == ReadEraSection.TRASH) {
                                                            "Trash is empty."
                                                        } else if (activeBooks.isEmpty()) {
                                                            "No books yet.\nTap Menu -> Folders or Downloads to add PDF, EPUB, MOBI, AZW/AZW3, FB2, TXT, or DOCX files."
                                                        } else {
                                                            "No books in '${section.label}'."
                                                        },
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = VioletInkLight
                                                    )
                                                }
                                            }
                                        }
                                     } else if (viewMode == LibraryViewMode.LIST) {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 28.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredBooks, key = { it.uriString }) { book ->
                                            val rawProgList = if (book.pageCount > 0) {
                                                (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                            } else 0f
                                            val progressList = rawProgList.coerceIn(0f, 1f)
                                            val percentList = (progressList * 100).toInt()
                                            val isCheckedList = book.uriString in selectedUris
                                            val coverBmpList = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectionMode) {
                                                            selectedUris = if (isCheckedList) selectedUris - book.uriString else selectedUris + book.uriString
                                                        } else if (!book.isTrashed) {
                                                            openBook(book)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                                border = BorderStroke(
                                                    width = if (isCheckedList) 1.5.dp else 1.dp,
                                                    color = if (isCheckedList) VioletAccentBright else VioletGrainGlow.copy(alpha = 0.35f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (selectionMode) {
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(end = 8.dp)
                                                                .size(24.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isCheckedList) VioletAccentBright else VioletCardSurface)
                                                                .border(1.dp, VioletGrainGlow, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            if (isCheckedList) {
                                                                Text("✓", color = VioletInkLight, style = MaterialTheme.typography.labelMedium)
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .width(52.dp)
                                                            .height(74.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(VioletCardSurface)
                                                            .border(1.dp, VioletGrainGlow.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (coverBmpList != null) {
                                                            Image(
                                                                bitmap = coverBmpList.asImageBitmap(),
                                                                contentDescription = book.title,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            ReadEraGraphicIcon(
                                                                kind = "BOOKS",
                                                                tint = VioletAccentBright.copy(alpha = 0.7f),
                                                                modifier = Modifier.size(26.dp)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = book.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = VioletInkLight,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${book.author} • ${book.format} • ${book.collection}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VioletInkMuted,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        LinearProgressIndicator(
                                                            progress = progressList,
                                                            color = VioletAccentBright,
                                                            trackColor = VioletGrainLine.copy(alpha = 0.4f),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(4.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = if (book.isTrashed) "In Trash" else "$percentList% • Page ${book.lastPage + 1} of ${book.pageCount}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (book.isTrashed) MaterialTheme.colorScheme.error else VioletInkMuted
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = if (book.isFavorite) "★" else "☆",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = if (book.isFavorite) VioletAccentBright else VioletInkMuted,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isFavorite = !book.isFavorite) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
                                                        )
                                                        Text(
                                                            text = if (book.isTrashed) "Restore" else "Trash",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (book.isTrashed) VioletAccentBright else MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isTrashed = !book.isTrashed) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
                                                        )
                                                        if (book.isTrashed) {
                                                            Text(
                                                                text = "Delete",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.clickable {
                                                                    val updated = books.filter { it.uriString != book.uriString }
                                                                    books = updated
                                                                    saveBooks(context, updated)
                                                                    coverBitmapCache.remove(book.uriString)
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val gridCols = if (viewMode == LibraryViewMode.GRID_2) 2 else 3
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(gridCols),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 28.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredBooks, key = { it.uriString }) { book ->
                                            val rawProgGrid = if (book.pageCount > 0) {
                                                (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                            } else 0f
                                            val progressGrid = rawProgGrid.coerceIn(0f, 1f)
                                            val percentGrid = (progressGrid * 100).toInt()
                                            val isCheckedGrid = book.uriString in selectedUris
                                            val coverBmpGrid = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectionMode) {
                                                            selectedUris = if (isCheckedGrid) selectedUris - book.uriString else selectedUris + book.uriString
                                                        } else if (!book.isTrashed) {
                                                            openBook(book)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                                border = BorderStroke(
                                                    width = if (isCheckedGrid) 1.5.dp else 1.dp,
                                                    color = if (isCheckedGrid) VioletAccentBright else VioletGrainGlow.copy(alpha = 0.35f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
                                            ) {
                                                Column {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(0.68f)
                                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 0.dp))
                                                            .background(VioletCardSurface)
                                                    ) {
                                                        if (coverBmpGrid != null) {
                                                            Image(
                                                                bitmap = coverBmpGrid.asImageBitmap(),
                                                                contentDescription = book.title,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Box(
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                ReadEraGraphicIcon(
                                                                    kind = "BOOKS",
                                                                    tint = VioletAccentBright.copy(alpha = 0.7f),
                                                                    modifier = Modifier.size(34.dp)
                                                                )
                                                            }
                                                        }

                                                        Text(
                                                            text = if (book.isTrashed) "TRASH" else "$percentGrid%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VioletBarText,
                                                            modifier = Modifier
                                                                .align(Alignment.TopStart)
                                                                .padding(6.dp)
                                                                .background(VioletBarColor.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )

                                                        Text(
                                                            text = if (book.isFavorite) "★" else "☆",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = if (book.isFavorite) VioletAccentBright else VioletBarText,
                                                            modifier = Modifier
                                                                .align(Alignment.TopEnd)
                                                                .padding(6.dp)
                                                                .background(VioletBarColor.copy(alpha = 0.7f), CircleShape)
                                                                .clickable {
                                                                    val updated = books.map {
                                                                        if (it.uriString == book.uriString) it.copy(isFavorite = !book.isFavorite) else it
                                                                    }
                                                                    books = updated
                                                                    saveBooks(context, updated)
                                                                }
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )

                                                        if (selectionMode) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .align(Alignment.BottomStart)
                                                                    .padding(6.dp)
                                                                    .size(22.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isCheckedGrid) VioletAccentBright else VioletBarColor.copy(alpha = 0.7f))
                                                                    .border(1.dp, VioletBarText, CircleShape),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (isCheckedGrid) {
                                                                    Text("✓", color = VioletBarText, style = MaterialTheme.typography.labelSmall)
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Column(modifier = Modifier.padding(8.dp)) {
                                                        Text(
                                                            text = book.title,
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = VioletInkLight,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${book.author} • ${book.format}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VioletInkMuted,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (showDrawer) {
                                BackHandler { showDrawer = false }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ComposeColor(0x99000000))
                                        .clickable { showDrawer = false }
                                )

                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .fillMaxHeight()
                                        .width(300.dp),
                                    color = VioletBarColor,
                                    shadowElevation = 12.dp
                                ) {
                                    Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ReadEraGraphicIcon(
                                                kind = "BOOKS",
                                                tint = VioletAccentBright,
                                                modifier = Modifier.size(30.dp)
                                            )
                                            Text(
                                                text = "e-Bookish",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = VioletBarText
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Library sections",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = VioletBarText.copy(alpha = 0.7f)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = VioletBarText.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(6.dp))

                                        LazyColumn(modifier = Modifier.weight(1f)) {
                                            items(ReadEraSection.entries) { sec ->
                                                val isCurrent = section == sec
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isCurrent) VioletCardElevated else ComposeColor.Transparent)
                                                        .clickable {
                                                            section = sec
                                                            showDrawer = false
                                                            selectionMode = false
                                                            selectedUris = emptySet()
                                                            formatFilter = "ALL"
                                                            authorFilter = "ALL"
                                                            collectionFilter = "ALL"
                                                        }
                                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    ReadEraGraphicIcon(
                                                        kind = drawerIconFor(sec),
                                                        tint = if (isCurrent) VioletAccentBright else VioletBarText,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = sec.label,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isCurrent) VioletAccentBright else VioletBarText,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        HorizontalDivider(color = VioletBarText.copy(alpha = 0.2f))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    showDrawer = false
                                                    showAboutDialog = true
                                                }
                                                .padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ReadEraGraphicIcon(
                                                kind = "SETTINGS",
                                                tint = VioletBarText,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "About e-Bookish",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = VioletBarText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drawerIconFor(sec: ReadEraSection): String {
    return when (sec) {
        ReadEraSection.READING_NOW -> "READING_NOW"
        ReadEraSection.BOOKS_AND_DOCS -> "BOOKS"
        ReadEraSection.FAVORITES -> "FAVORITES"
        ReadEraSection.TO_READ -> "TO_READ"
        ReadEraSection.HAVE_READ -> "HAVE_READ"
        ReadEraSection.AUTHORS -> "AUTHORS"
        ReadEraSection.SERIES -> "SERIES"
        ReadEraSection.COLLECTIONS -> "COLLECTIONS"
        ReadEraSection.FORMATS -> "FORMATS"
        ReadEraSection.QUOTES -> "QUOTES"
        ReadEraSection.DICTIONARY -> "DICTIONARY"
        ReadEraSection.TRASH -> "TRASH"
        ReadEraSection.SETTINGS -> "SETTINGS"
    }
}
}

// ---------------------------------------------------------------------------
// Persistence helpers (SharedPreferences-backed JSON)
// ---------------------------------------------------------------------------

private fun appPrefs(context: Context): android.content.SharedPreferences {
    return context.getSharedPreferences("ebookish_prefs", Context.MODE_PRIVATE)
}

private fun loadBooks(context: Context): List<BookItem> {
    val arr = try {
        JSONArray(appPrefs(context).getString("books_json", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }
    return parseBooksJsonArray(arr)
}

private fun saveBooks(context: Context, list: List<BookItem>) {
    appPrefs(context).edit().putString("books_json", booksToJsonArray(list).toString()).apply()
}

private fun booksToJsonArray(list: List<BookItem>): JSONArray {
    val arr = JSONArray()
    for (b in list) {
        arr.put(JSONObject().apply {
            put("uriString", b.uriString)
            put("title", b.title)
            put("format", b.format)
            put("pageCount", b.pageCount)
            put("lastPage", b.lastPage)
            put("isFavorite", b.isFavorite)
            put("isTrashed", b.isTrashed)
            put("author", b.author)
            put("collection", b.collection)
        })
    }
    return arr
}

private fun parseBooksJsonArray(arr: JSONArray?): List<BookItem> {
    val out = mutableListOf<BookItem>()
    if (arr == null) return out
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val uri = o.optString("uriString")
        if (uri.isBlank()) continue
        out.add(
            BookItem(
                uriString = uri,
                title = o.optString("title", "Untitled"),
                format = o.optString("format", "PDF"),
                pageCount = o.optInt("pageCount", 1),
                lastPage = o.optInt("lastPage", 0),
                isFavorite = o.optBoolean("isFavorite", false),
                isTrashed = o.optBoolean("isTrashed", false),
                author = o.optString("author", "Unknown Author"),
                collection = o.optString("collection", "General"),
                cover = null,
                metadataLoaded = false
            )
        )
    }
    return out
}

private fun loadQuotes(context: Context): List<QuoteEntry> {
    val arr = try {
        JSONArray(appPrefs(context).getString("quotes_json", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }
    return parseQuotesJsonArray(arr)
}

private fun saveQuotes(context: Context, list: List<QuoteEntry>) {
    appPrefs(context).edit().putString("quotes_json", quotesToJsonArray(list).toString()).apply()
}

private fun quotesToJsonArray(list: List<QuoteEntry>): JSONArray {
    val arr = JSONArray()
    for (q in list) {
        arr.put(JSONObject().apply {
            put("id", q.id)
            put("bookTitle", q.bookTitle)
            put("sectionIndex", q.sectionIndex)
            put("text", q.text)
            put("colorName", q.colorName)
            put("timestamp", q.timestamp)
        })
    }
    return arr
}

private fun parseQuotesJsonArray(arr: JSONArray?): List<QuoteEntry> {
    val out = mutableListOf<QuoteEntry>()
    if (arr == null) return out
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        out.add(
            QuoteEntry(
                id = o.optString("id", "quote_$i"),
                bookTitle = o.optString("bookTitle", "Unknown Book"),
                sectionIndex = o.optInt("sectionIndex", 0),
                text = o.optString("text", ""),
                colorName = o.optString("colorName", QuoteColorOption.VIOLET.name),
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        )
    }
    return out
}

private fun loadDictionary(context: Context): List<DictionaryEntry> {
    val arr = try {
        JSONArray(appPrefs(context).getString("dictionary_json", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }
    return parseDictionaryJsonArray(arr)
}

private fun saveDictionary(context: Context, list: List<DictionaryEntry>) {
    appPrefs(context).edit().putString("dictionary_json", dictionaryToJsonArray(list).toString()).apply()
}

private fun dictionaryToJsonArray(list: List<DictionaryEntry>): JSONArray {
    val arr = JSONArray()
    for (d in list) {
        arr.put(JSONObject().apply {
            put("word", d.word)
            put("definition", d.definition)
            put("sourceBook", d.sourceBook)
            put("timestamp", d.timestamp)
        })
    }
    return arr
}

private fun parseDictionaryJsonArray(arr: JSONArray?): List<DictionaryEntry> {
    val out = mutableListOf<DictionaryEntry>()
    if (arr == null) return out
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val word = o.optString("word")
        if (word.isBlank()) continue
        out.add(
            DictionaryEntry(
                word = word,
                definition = o.optString("definition", ""),
                sourceBook = o.optString("sourceBook", "Manual"),
                timestamp = o.optLong("timestamp", System.currentTimeMillis())
            )
        )
    }
    return out
}

private fun loadCustomFonts(context: Context): List<CustomFontEntry> {
    val arr = try {
        JSONArray(appPrefs(context).getString("custom_fonts_json", "[]"))
    } catch (_: Exception) {
        JSONArray()
    }
    val out = mutableListOf<CustomFontEntry>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        val name = o.optString("name")
        val path = o.optString("localPath")
        if (name.isNotBlank() && path.isNotBlank() && File(path).exists()) {
            out.add(CustomFontEntry(name, path))
        }
    }
    return out
}

private fun saveCustomFonts(context: Context, list: List<CustomFontEntry>) {
    val arr = JSONArray()
    for (f in list) {
        arr.put(JSONObject().apply {
            put("name", f.name)
            put("localPath", f.localPath)
        })
    }
    appPrefs(context).edit().putString("custom_fonts_json", arr.toString()).apply()
}

private fun loadSavedFolderUri(context: Context): String? {
    return appPrefs(context).getString("saved_folder_uri", null)
}

private fun saveSavedFolderUri(context: Context, uriString: String) {
    appPrefs(context).edit().putString("saved_folder_uri", uriString).apply()
}

// ---------------------------------------------------------------------------
// File / document helpers
// ---------------------------------------------------------------------------

private fun getRawFileName(context: Context, uri: Uri): String {
    var name = uri.lastPathSegment ?: "document"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) {
                c.getString(idx)?.let { name = it }
            }
        }
    } catch (_: Exception) {
    }
    return name
}

private fun sanitizeFileName(raw: String): String {
    val cleaned = raw
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .trim()
        .trimEnd('.')
    return cleaned.ifBlank { "document" }
}

private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input)
        }
    } catch (_: Exception) {
        null
    }
}

private fun importCustomFontFile(context: Context, uri: Uri, rawName: String): CustomFontEntry? {
    return try {
        val lower = rawName.lowercase()
        val ext = when {
            lower.endsWith(".otf") -> ".otf"
            else -> ".ttf"
        }
        val displayName = rawName.substringBeforeLast(".").ifBlank { "CustomFont" }
        val destDir = File(context.filesDir, "fonts")
        if (!destDir.exists()) destDir.mkdirs()
        val outFile = File(destDir, "${sanitizeFileName(displayName)}$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        } ?: return null
        if (!outFile.exists() || outFile.length() == 0L) return null
        CustomFontEntry(name = displayName, localPath = outFile.absolutePath)
    } catch (_: Exception) {
        null
    }
}

private fun collectBooksFromTreeFast(
    context: Context,
    treeUri: Uri,
    onProgress: (Int) -> Unit
): List<Pair<Uri, String>> {
    val found = mutableListOf<Pair<Uri, String>>()
    val supportedExt = setOf("pdf", "epub", "mobi", "azw", "azw3", "fb2", "txt", "docx")

    fun walk(docId: String, depth: Int) {
        if (depth > 12) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                val idCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = c.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (c.moveToNext()) {
                    val id = c.getString(idCol) ?: continue
                    val name = c.getString(nameCol) ?: continue
                    val mime = c.getString(mimeCol) ?: ""
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        walk(id, depth + 1)
                    } else {
                        val ext = name.substringAfterLast('.', "").lowercase()
                        if (ext in supportedExt) {
                            found.add(DocumentsContract.buildDocumentUriUsingTree(treeUri, id) to name)
                            onProgress(found.size)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    val rootId = DocumentsContract.getTreeDocumentId(treeUri)
    walk(rootId, 0)
    return found
}

// ---------------------------------------------------------------------------
// PDF helpers
// ---------------------------------------------------------------------------

private fun getPdfPageCount(context: Context, uri: Uri): Int {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            0
        } else {
            renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            renderer = null
            count
        }
    } catch (_: Exception) {
        0
    } finally {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
    }
}

private fun renderSinglePage(context: Context, uri: Uri, pageIndex: Int, scale: Float = 1f): Bitmap? {
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    return try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            null
        } else {
            renderer = PdfRenderer(pfd)
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                null
            } else {
                val page = renderer.openPage(pageIndex)
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()
                bmp
            }
        }
    } catch (_: Exception) {
        null
    } finally {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
    }
}

private fun renderAllPages(context: Context, uri: Uri): List<Bitmap> {
    val out = mutableListOf<Bitmap>()
    var pfd: ParcelFileDescriptor? = null
    var renderer: PdfRenderer? = null
    try {
        pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return out
        renderer = PdfRenderer(pfd)
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val scale = if (page.width > 1440) 1440f / page.width else 1f
            val w = (page.width * scale).toInt().coerceAtLeast(1)
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()
            out.add(bmp)
        }
    } catch (_: Exception) {
    } finally {
        try {
            renderer?.close()
        } catch (_: Exception) {
        }
        try {
            pfd?.close()
        } catch (_: Exception) {
        }
    }
    return out
}

// ---------------------------------------------------------------------------
// Scan editing helpers
// ---------------------------------------------------------------------------

private fun cropSidesBitmap(src: Bitmap, left: Float, top: Float, right: Float, bottom: Float): Bitmap {
    val cl = (src.width * left).toInt().coerceIn(0, src.width - 1)
    val ct = (src.height * top).toInt().coerceIn(0, src.height - 1)
    val cr = (src.width * right).toInt().coerceIn(0, src.width - 1)
    val cb = (src.height * bottom).toInt().coerceIn(0, src.height - 1)
    val w = src.width - cl - cr
    val h = src.height - ct - cb
    if (w < 1 || h < 1) return src
    return Bitmap.createBitmap(src, cl, ct, w, h)
}

private fun applyScanFilter(src: Bitmap, mode: ScanFilterMode): Bitmap {
    return when (mode) {
        ScanFilterMode.ORIGINAL -> src
        ScanFilterMode.GRAYSCALE -> {
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            val m = ColorMatrix().apply { setSaturation(0f) }
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(m)
            }
            Canvas(out).drawBitmap(src, 0f, 0f, p)
            out
        }
        ScanFilterMode.DOC_BW -> {
            val base = src.copy(Bitmap.Config.ARGB_8888, true)
            val grayM = ColorMatrix().apply { setSaturation(0f) }
            val grayP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(grayM)
            }
            val c = Canvas(base)
            c.drawBitmap(src, 0f, 0f, grayP)
            val w = base.width
            val h = base.height
            val px = IntArray(w * h)
            base.getPixels(px, 0, w, 0, 0, w, h)
            for (i in px.indices) {
                val col = px[i]
                val lum = (Color.red(col) + Color.green(col) + Color.blue(col)) / 3
                px[i] = if (lum > 185) Color.WHITE else Color.BLACK
            }
            base.setPixels(px, 0, w, 0, 0, w, h)
            base
        }
        ScanFilterMode.MAGIC_COLOR -> {
            val out = src.copy(Bitmap.Config.ARGB_8888, true)
            val sat = ColorMatrix().apply { setSaturation(1.7f) }
            val contrast = ColorMatrix(
                floatArrayOf(
                    1.25f, 0f, 0f, 0f, -20f,
                    0f, 1.25f, 0f, 0f, -20f,
                    0f, 0f, 1.25f, 0f, -20f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            sat.postConcat(contrast)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(sat)
            }
            Canvas(out).drawBitmap(src, 0f, 0f, p)
            out
        }
    }
}

private fun savePagesAsPdf(context: Context, baseName: String, pages: List<Bitmap>): Uri? {
    if (pages.isEmpty()) return null
    val doc = PdfDocument()
    return try {
        for ((i, bmp) in pages.withIndex()) {
            val w = bmp.width.coerceAtLeast(1)
            val h = bmp.height.coerceAtLeast(1)
            val info = PdfDocument.PageInfo.Builder(w, h, i + 1).create()
            val page = doc.startPage(info)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
        }
        val fileName = "${sanitizeFileName(baseName)}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            if (uri == null) {
                null
            } else {
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                uri
            }
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val f = File(dir, fileName)
            FileOutputStream(f).use { doc.writeTo(it) }
            Uri.fromFile(f)
        }
    } catch (_: Exception) {
        null
    } finally {
        doc.close()
    }
}

private fun savePagesAsJpegsWithUris(context: Context, baseName: String, pages: List<Bitmap>): List<Uri> {
    val out = mutableListOf<Uri>()
    val clean = sanitizeFileName(baseName)
    for ((i, bmp) in pages.withIndex()) {
        try {
            val fileName = if (pages.size == 1) "$clean.jpg" else "${clean}_${i + 1}.jpg"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/eBookish")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    out.add(uri)
                }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "eBookish")
                if (!dir.exists()) dir.mkdirs()
                val f = File(dir, fileName)
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                out.add(Uri.fromFile(f))
            }
        } catch (_: Exception) {
        }
    }
    return out
}

private fun shareExportedScan(context: Context, result: ExportedScanResult) {
    try {
        val mime = if (result.format == "PDF") "application/pdf" else "image/jpeg"
        val sendIntent = if (result.uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, result.uris.first())
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mime
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(result.uris))
            }
        }
        sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(sendIntent, "Share ${result.title}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

// ---------------------------------------------------------------------------
// Ebook text extraction helpers (EPUB / FB2 / TXT / DOCX / MOBI-AZW)
// ---------------------------------------------------------------------------

private fun readZipEntries(input: java.io.InputStream, maxEntryBytes: Long = 64L * 1024 * 1024): Map<String, ByteArray> {
    val map = mutableMapOf<String, ByteArray>()
    ZipInputStream(input).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val buf = ByteArrayOutputStream()
                val tmp = ByteArray(16 * 1024)
                var total = 0L
                var read = zis.read(tmp)
                while (read != -1 && total < maxEntryBytes) {
                    buf.write(tmp, 0, read)
                    total += read
                    read = zis.read(tmp)
                }
                map[entry.name] = buf.toByteArray()
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
    return map
}

private fun htmlToPlainText(htmlRaw: String): String {
    val noScript = Regex("(?is)<(script|style).*?</\\1>").replace(htmlRaw, " ")
    val noTags = Regex("(?s)<[^>]+>").replace(noScript, " ")
    val decoded = Html.fromHtml(noTags).toString()
    return decoded
        .replace(" ", " ")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex(" ?\\n ?"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
}

private fun extractEpubChapters(context: Context, uri: Uri): List<String> {
    return try {
        val entries = context.contentResolver.openInputStream(uri)?.use { readZipEntries(it) }
            ?: return emptyList()
        val container = entries["META-INF/container.xml"]?.toString(Charsets.UTF_8)
            ?: return emptyList()
        val opfPath = Regex("full-path=\"([^\"]+)\"").find(container)?.groupValues?.get(1)
            ?: return emptyList()
        val opf = entries[opfPath]?.toString(Charsets.UTF_8) ?: return emptyList()
        val opfDir = opfPath.substringBeforeLast("/", "")

        fun resolve(href: String): String {
            val cleanHref = href.substringBefore('#')
            return if (opfDir.isEmpty()) cleanHref else "$opfDir/$cleanHref"
        }

        val manifestHrefs = Regex("<item[^>]*id=\"([^\"]+)\"[^>]*href=\"([^\"]+)\"")
            .findAll(opf)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val spineIds = Regex("<itemref[^>]*idref=\"([^\"]+)\"")
            .findAll(opf)
            .map { it.groupValues[1] }
            .toList()

        val sb = StringBuilder()
        for (id in spineIds) {
            val href = manifestHrefs[id] ?: continue
            val data = entries[resolve(href)] ?: continue
            val text = htmlToPlainText(data.toString(Charsets.UTF_8))
            if (text.isNotBlank()) {
                sb.append("\n\n").append(text)
            }
        }
        splitTextIntoSections(sb.toString())
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractEpubCoverBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val entries = context.contentResolver.openInputStream(uri)?.use { readZipEntries(it) }
            ?: return null
        val container = entries["META-INF/container.xml"]?.toString(Charsets.UTF_8)
            ?: return null
        val opfPath = Regex("full-path=\"([^\"]+)\"").find(container)?.groupValues?.get(1)
            ?: return null
        val opf = entries[opfPath]?.toString(Charsets.UTF_8) ?: return null
        val opfDir = opfPath.substringBeforeLast("/", "")

        fun resolve(href: String): String {
            return if (opfDir.isEmpty()) href else "$opfDir/$href"
        }

        val candidates = mutableListOf<String>()
        Regex("<item[^>]*properties=\"[^\"]*cover-image[^\"]*\"[^>]*href=\"([^\"]+)\"")
            .find(opf)?.groupValues?.get(1)?.let { candidates.add(resolve(it)) }
        Regex("<item[^>]*id=\"cover\"[^>]*href=\"([^\"]+)\"")
            .find(opf)?.groupValues?.get(1)?.let { candidates.add(resolve(it)) }
        candidates.addAll(
            entries.keys.filter {
                it.startsWith(opfDir) && Regex("cover\\.(jpg|jpeg|png|webp)$", RegexOption.IGNORE_CASE).containsMatchIn(it)
            }
        )

        for (key in candidates.distinct()) {
            val bytes = entries[key] ?: continue
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) return bmp
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun extractFb2Sections(context: Context, uri: Uri): List<String> {
    return try {
        val xml = context.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            ?: return emptyList()
        val bodyXml = Regex("(?s)<body.*?</body>").find(xml)?.value ?: xml
        val plain = htmlToPlainText(bodyXml)
        splitTextIntoSections(plain)
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractTxtSections(context: Context, uri: Uri): List<String> {
    return try {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()
        splitTextIntoSections(String(raw, Charsets.UTF_8))
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractDocxSections(context: Context, uri: Uri): List<String> {
    return try {
        val entries = context.contentResolver.openInputStream(uri)?.use { readZipEntries(it) }
            ?: return emptyList()
        val xmlBytes = entries["word/document.xml"] ?: return emptyList()
        var s = xmlBytes.toString(Charsets.UTF_8)
        s = s.replace("</w:p>", "\n\n")
        s = Regex("(?s)<[^>]+>").replace(s, " ")
        val plain = Html.fromHtml(s).toString()
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        splitTextIntoSections(plain)
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractMobiAzwSections(context: Context, uri: Uri): List<String> {
    return try {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return emptyList()
        if (raw.size > 60 * 1024 * 1024 || raw.size < 4096) return emptyList()
        val sb = StringBuilder()
        var run = StringBuilder()
        for (b in raw) {
            val c = b.toInt() and 0xFF
            if (c == 0x09 || c == 0x0A || c == 0x0D || (c in 0x20..0x7E) || (c in 0xC0..0xFF)) {
                run.append(c.toChar())
            } else {
                if (run.length >= 40) {
                    sb.append(run).append("\n\n")
                }
                run = StringBuilder()
            }
        }
        if (run.length >= 40) {
            sb.append(run)
        }
        splitTextIntoSections(sb.toString())
    } catch (_: Exception) {
        emptyList()
    }
}

private fun indexOfMagic(data: ByteArray, magic: ByteArray): Int {
    if (data.size < magic.size) return -1
    outer@ for (i in 0..(data.size - magic.size)) {
        for (j in magic.indices) {
            if (data[i + j] != magic[j]) continue@outer
        }
        return i
    }
    return -1
}

private fun extractMobiAzwCoverBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        if (raw.size > 80 * 1024 * 1024 || raw.size < 4096) return null

        val jpgIdx = indexOfMagic(raw, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        if (jpgIdx >= 0) {
            val bmp = BitmapFactory.decodeByteArray(raw, jpgIdx, raw.size - jpgIdx)
            if (bmp != null) return bmp
        }

        val pngIdx = indexOfMagic(raw, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
        if (pngIdx >= 0) {
            val bmp = BitmapFactory.decodeByteArray(raw, pngIdx, raw.size - pngIdx)
            if (bmp != null) return bmp
        }
        null
    } catch (_: Exception) {
        null
    }
}
