package com.docuverse.app

import android.app.Activity
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
import android.text.Html
import android.view.WindowManager
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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
import java.nio.charset.Charset
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

enum class LibraryViewMode {
    GRID_3, GRID_2, LIST
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
    TRASH("Trash"),
    SETTINGS("Settings")
}

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original"),
    DOC_BW("Doc B&W"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale")
}

enum class EpubPageTheme(
    val label: String,
    val bgColor: ComposeColor,
    val textColor: ComposeColor,
    val chromeBg: ComposeColor
) {
    OLD_YELLOW("Old Yellow", ComposeColor(0xFFF3E5B5), ComposeColor(0xFF2B2114), ComposeColor(0xFFE6D49C)),
    LIGHT_RUSTY_YELLOW("Rusty Gold", ComposeColor(0xFFEED394), ComposeColor(0xFF291D0D), ComposeColor(0xFFDFC07C)),
    LIGHT_GREEN("Soft Mint", ComposeColor(0xFFDCEFD9), ComposeColor(0xFF17291B), ComposeColor(0xFFC8E2C4)),
    LIGHT_PURPLE("Soft Lilac", ComposeColor(0xFFE7DDF5), ComposeColor(0xFF221A33), ComposeColor(0xFFD6C8EC)),
    PAPER_WHITE("Day White", ComposeColor(0xFFFAF9F5), ComposeColor(0xFF1C1B18), ComposeColor(0xFFECEAE2)),
    WARM_SEPIA("Warm Sepia", ComposeColor(0xFFE8D8C3), ComposeColor(0xFF2E2013), ComposeColor(0xFFD7C3A8)),
    NIGHT_SLATE("Night Slate", ComposeColor(0xFF0E1A21), ComposeColor(0xFFDCE6EC), ComposeColor(0xFF152630))
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
    val filter: ScanFilterMode = ScanFilterMode.DOC_BW
) {
    fun renderEdited(): Bitmap {
        val cropped = cropSidesBitmap(rawBitmap, crop.left, crop.top, crop.right, crop.bottom)
        return applyScanFilter(cropped, filter)
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

private val LightVioletFallbackScheme = lightColorScheme(
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
                drawArc(
                    color = tint,
                    startAngle = 30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = stroke
                )
            }
            "BOOKS" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = Size(w * 0.68f, h * 0.64f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = stroke
                )
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
                drawCircle(
                    color = tint,
                    radius = w * 0.34f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.30f), w * 0.08f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.66f, h * 0.56f), w * 0.08f, StrokeCap.Round)
            }
            "HAVE_READ" -> {
                drawLine(tint, Offset(w * 0.20f, h * 0.52f), Offset(w * 0.42f, h * 0.74f), w * 0.09f, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.74f), Offset(w * 0.82f, h * 0.28f), w * 0.09f, StrokeCap.Round)
            }
            "AUTHORS" -> {
                drawCircle(tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.32f))
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.20f, h * 0.52f),
                    size = Size(w * 0.60f, h * 0.36f)
                )
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
                drawRect(tint, topLeft = Offset(w * 0.20f, h * 0.22f), size = Size(w * 0.14f, h * 0.56f))
                drawRect(tint, topLeft = Offset(w * 0.42f, h * 0.22f), size = Size(w * 0.14f, h * 0.56f))
                drawLine(tint, Offset(w * 0.66f, h * 0.24f), Offset(w * 0.82f, h * 0.76f), w * 0.11f, StrokeCap.Round)
            }
            "FORMATS" -> {
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.24f), Size(w * 0.60f, h * 0.18f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.46f), Size(w * 0.60f, h * 0.18f), CornerRadius(6f, 6f), style = stroke)
                drawRoundRect(tint, Offset(w * 0.20f, h * 0.68f), Size(w * 0.60f, h * 0.14f), CornerRadius(6f, 6f), style = stroke)
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
                drawCircle(tint, radius = w * 0.26f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(tint, radius = w * 0.09f, center = Offset(w * 0.5f, h * 0.5f))
            }
            else -> {
                drawRoundRect(tint, Offset(w * 0.18f, h * 0.22f), Size(w * 0.64f, h * 0.48f), CornerRadius(8f, 8f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.32f), Offset(w * 0.5f, h * 0.50f), w * 0.09f, StrokeCap.Round)
                drawCircle(tint, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.60f))
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }

            MaterialTheme(
                colorScheme = if (isDarkTheme) DarkVioletScheme else LightVioletFallbackScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VioletBaseDark
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

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

                    var selectionMode by remember { mutableStateOf(false) }
                    var selectedUris by remember { mutableStateOf(setOf<String>()) }

                    var searchQuery by remember { mutableStateOf("") }
                    var scanNotice by remember { mutableStateOf("") }
                    var isScanning by remember { mutableStateOf(false) }
                    var showDocScanner by remember { mutableStateOf(false) }

                    var epubPageTheme by remember { mutableStateOf(EpubPageTheme.OLD_YELLOW) }
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

                        var docStatus by remember { mutableStateOf("") }
                        var isProcessingDoc by remember { mutableStateOf(false) }
                        var lastExported by remember { mutableStateOf<ExportedScanResult?>(null) }

                        val cameraSnapLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.TakePicturePreview()
                        ) { bmp: Bitmap? ->
                            if (bmp != null) {
                                val newPage = EditableScanPage(
                                    rawBitmap = bmp,
                                    crop = SideCrop(),
                                    filter = defaultFilter
                                )
                                scanPages = scanPages + newPage
                                selectedIndex = scanPages.lastIndex
                                docStatus = "Captured page ${scanPages.size}."
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
                                            loadedPages.add(
                                                EditableScanPage(
                                                    rawBitmap = b,
                                                    crop = SideCrop(),
                                                    filter = defaultFilter
                                                )
                                            )
                                        }
                                    }
                                    runOnUiThread {
                                        if (loadedPages.isNotEmpty()) {
                                            val startIdx = scanPages.size
                                            scanPages = scanPages + loadedPages
                                            selectedIndex = startIdx.coerceIn(0, scanPages.lastIndex)
                                            docStatus = "Added ${loadedPages.size} gallery photo(s)."
                                        } else {
                                            docStatus = "Could not load selected gallery photos."
                                        }
                                        isProcessingDoc = false
                                    }
                                }.start()
                            }
                        }

                        fun updateSelectedPage(transform: (EditableScanPage) -> EditableScanPage) {
                            if (scanPages.isEmpty()) return
                            val idx = selectedIndex.coerceIn(0, scanPages.lastIndex)
                            scanPages = scanPages.mapIndexed { i, p -> if (i == idx) transform(p) else p }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            DarkVioletTextureBackground(modifier = Modifier.fillMaxSize())

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 28.dp)
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
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

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

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                        modifier = Modifier.weight(1.3f)
                                    ) {
                                        Text("Gallery Photos", maxLines = 1, softWrap = false)
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
                                            .height(230.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Live Cut Preview • Page ${safeIdx + 1}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = VioletInkLight
                                                )
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
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Image(
                                                bitmap = livePreviewBmp.asImageBitmap(),
                                                contentDescription = "Live Cut Page Preview",
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Fit
                                            )
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
                                                    ) {
                                                        Text("L -", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.left + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(left = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("L +", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
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
                                                    ) {
                                                        Text("R -", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.right + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(right = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("R +", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
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
                                                    ) {
                                                        Text("T -", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.top + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(top = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("T +", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
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
                                                    ) {
                                                        Text("B -", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            val v = (currentPage.crop.bottom + 0.05f).coerceIn(0f, 0.4f)
                                                            updateSelectedPage { it.copy(crop = it.crop.copy(bottom = v)) }
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("B +", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(18.dp))
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
                                                text = "Tap 'Gallery Photos' to pick from your phone gallery or 'Camera' to snap a page.",
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
                                    ) {
                                        Text("Save PDF", maxLines = 1, softWrap = false)
                                    }

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
                                    ) {
                                        Text("Save JPEG", maxLines = 1, softWrap = false)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            scanPages = listOf()
                                            selectedIndex = 0
                                            lastExported = null
                                            docStatus = "Cleared."
                                        },
                                        enabled = !isProcessingDoc && (scanPages.isNotEmpty() || lastExported != null),
                                        border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.55f)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = VioletCardElevated.copy(alpha = 0.9f),
                                            contentColor = VioletInkLight
                                        )
                                    ) {
                                        Text("Clear", maxLines = 1, softWrap = false)
                                    }
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
                                                text = "Scanned ${exported.format}: ${exported.title}",
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
                                                ) {
                                                    Text("Share", maxLines = 1, softWrap = false)
                                                }
                                                if (exported.bookItem != null) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val b = exported.bookItem
                                                            showDocScanner = false
                                                            openBook(b)
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text("Read PDF", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        lastExported = null
                                                        showDocScanner = false
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("Done", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
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
                            activeBook = null
                            readerPages = listOf()
                            textBookSections = listOf()
                            showBrightnessSlider = false
                        }

                        var showReaderBars by remember { mutableStateOf(false) }
                        var showAaPanel by remember { mutableStateOf(false) }

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
                                    val safeStart = if (currentBook.lastPage < 0) {
                                        0
                                    } else if (currentBook.lastPage > maxIdx) {
                                        maxIdx
                                    } else {
                                        currentBook.lastPage
                                    }
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
                                                fontFamily = epubFontStyle.family,
                                                fontSize = epubFontSizeSp.sp,
                                                lineHeight = computedLineSp
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.18f)
                                            .clickable {
                                                if (pagerState.currentPage > 0) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                    }
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.64f)
                                            .clickable {
                                                showReaderBars = !showReaderBars
                                                if (!showReaderBars) {
                                                    showAaPanel = false
                                                    showBrightnessSlider = false
                                                }
                                            }
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.18f)
                                            .clickable {
                                                if (pagerState.currentPage < textBookSections.size - 1) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
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
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        activeBook = null
                                                        textBookSections = listOf()
                                                        showBrightnessSlider = false
                                                    },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "← Shelf",
                                                        color = VioletBarText,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = currentBook.title,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = VioletBarText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${currentBook.format} • Section ${pagerState.currentPage + 1} of ${textBookSections.size}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = VioletBarText.copy(alpha = 0.85f),
                                                        maxLines = 1
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { showBrightnessSlider = !showBrightnessSlider },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "☀ ${(screenBrightness * 100).toInt()}%",
                                                        color = VioletBarText,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }

                                                OutlinedButton(
                                                    onClick = { showAaPanel = !showAaPanel },
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Aa",
                                                        color = VioletBarText,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
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
                                                    Text(
                                                        text = "Brightness: ${(screenBrightness * 100).toInt()}%",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = VioletInkLight
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness - 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("☀ -", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness + 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("☀ +", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = 0.75f
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("75%", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
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
                                                    Text(
                                                        text = "Page Background: ${epubPageTheme.label}",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = VioletBarText
                                                    )
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
                                                                Text(
                                                                    text = themeOpt.label,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = VioletBarText,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Font Size: ${epubFontSizeSp.toInt()}sp",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = VioletBarText
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubFontSizeSp = (epubFontSizeSp - 2f).coerceAtLeast(14f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("A-", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
                                                            OutlinedButton(
                                                                onClick = { epubFontSizeSp = (epubFontSizeSp + 2f).coerceAtMost(28f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("A+", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        for (fOpt in EpubFontStyleOption.entries) {
                                                            OutlinedButton(
                                                                onClick = { epubFontStyle = fOpt },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                modifier = Modifier.weight(1f),
                                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                                            ) {
                                                                Text(
                                                                    text = if (epubFontStyle == fOpt) "${fOpt.label} ✓" else fOpt.label,
                                                                    color = VioletBarText,
                                                                    fontFamily = fOpt.family,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Line Spacing: ${String.format(Locale.US, "%.2f", epubLineSpacingMult)}x",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = VioletBarText
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubLineSpacingMult = (epubLineSpacingMult - 0.15f).coerceAtLeast(1.1f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("Tighter", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
                                                            OutlinedButton(
                                                                onClick = { epubLineSpacingMult = (epubLineSpacingMult + 0.15f).coerceAtMost(2.0f) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("Wider", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Side Margins: ${epubSideMarginDp}dp",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = VioletBarText
                                                        )
                                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            OutlinedButton(
                                                                onClick = { epubSideMarginDp = (epubSideMarginDp - 6).coerceAtLeast(12) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("Narrow", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
                                                            OutlinedButton(
                                                                onClick = { epubSideMarginDp = (epubSideMarginDp + 6).coerceAtMost(48) },
                                                                border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                                            ) {
                                                                Text("Wide", color = VioletBarText, maxLines = 1, softWrap = false)
                                                            }
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
                                                        Text(
                                                            text = "Paragraph Indent: $indentLabel",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = VioletBarText
                                                        )
                                                        OutlinedButton(
                                                            onClick = {
                                                                epubParagraphIndentLevel = (epubParagraphIndentLevel + 1) % 3
                                                            },
                                                            border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                                        ) {
                                                            Text("Cycle Indent", color = VioletBarText, maxLines = 1, softWrap = false)
                                                        }
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
                                                            scope.launch {
                                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                                            }
                                                        }
                                                    },
                                                    enabled = pagerState.currentPage > 0,
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Prev Sec", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                                }

                                                Text(
                                                    text = "${pagerState.currentPage + 1} of ${textBookSections.size} ($pct%)",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletBarText
                                                )

                                                OutlinedButton(
                                                    onClick = {
                                                        if (pagerState.currentPage < textBookSections.size - 1) {
                                                            scope.launch {
                                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                            }
                                                        }
                                                    },
                                                    enabled = pagerState.currentPage < textBookSections.size - 1,
                                                    border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Next Sec", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                                }
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
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator(color = VioletAccentBright)
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("Opening book...", color = VioletInkLight)
                                        }
                                    }
                                } else if (readerPages.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Could not open this PDF file.", color = VioletInkLight)
                                    }
                                } else {
                                    val maxIdx = readerPages.size - 1
                                    val safeStart = if (currentBook.lastPage < 0) {
                                        0
                                    } else if (currentBook.lastPage > maxIdx) {
                                        maxIdx
                                    } else {
                                        currentBook.lastPage
                                    }
                                    val pagerState = rememberPagerState(
                                        initialPage = safeStart,
                                        pageCount = { readerPages.size }
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
                                    ) { pageIndex ->
                                        val pageBmp: Bitmap = readerPages[pageIndex]
                                        var pinScale by remember(pageIndex, currentBook.uriString) {
                                            mutableFloatStateOf(1f)
                                        }
                                        var panOffset by remember(pageIndex, currentBook.uriString) {
                                            mutableStateOf(Offset.Zero)
                                        }

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
                                                            Offset(
                                                                x = panOffset.x + pan.x,
                                                                y = panOffset.y + pan.y
                                                            )
                                                        }
                                                    }
                                                }
                                                .clickable {
                                                    showReaderBars = !showReaderBars
                                                    if (!showReaderBars) showBrightnessSlider = false
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
                                                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
                                                ) {
                                                    Text("← Shelf", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                                                }
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
                                                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "☀ ${(screenBrightness * 100).toInt()}%",
                                                        color = VioletBarText,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
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
                                                    Text(
                                                        text = "Brightness: ${(screenBrightness * 100).toInt()}%",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = VioletInkLight
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness - 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("☀ -", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = (screenBrightness + 0.1f).coerceIn(0.1f, 1.0f)
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("☀ +", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                screenBrightness = 0.75f
                                                                applyScreenBrightness(context, screenBrightness)
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                        ) {
                                                            Text("75%", color = VioletInkLight, maxLines = 1, softWrap = false)
                                                        }
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
                                                Text(
                                                    text = "${pagerState.currentPage + 1} of ${readerPages.size}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletBarText
                                                )
                                                Text(
                                                    text = "$pagePct%",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletBarText
                                                )
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text("☰ Menu", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
                                            }

                                            ReadEraGraphicIcon(
                                                kind = "BOOKS",
                                                tint = VioletAccentBright,
                                                modifier = Modifier.size(24.dp)
                                            )

                                            Text(
                                                text = section.label,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = VioletInkLight,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Box {
                                            OutlinedButton(
                                                onClick = { showOverflowMenu = true },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = VioletCardSurface,
                                                    contentColor = VioletInkLight
                                                ),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Text("⋮", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
                                            }

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
                                            ) {
                                                Text(if (formatFilter == fmt) "$fmt ✓" else fmt, maxLines = 1, softWrap = false)
                                            }
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
                                            ) {
                                                Text(if (authorFilter == auth) "$auth ✓" else auth, maxLines = 1, softWrap = false)
                                            }
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
                                            ) {
                                                Text(if (collectionFilter == col) "$col ✓" else col, maxLines = 1, softWrap = false)
                                            }
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
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("e-Bookish Reader & Library Settings", fontWeight = FontWeight.Bold, color = VioletInkLight)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                                    Text(if (isDarkTheme) "Violet: Deep" else "Violet: Plum", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
                                                OutlinedButton(onClick = { useDefaultCovers = !useDefaultCovers }) {
                                                    Text(if (useDefaultCovers) "Default Cover: ON" else "Default Cover: OFF", maxLines = 1, softWrap = false, color = VioletInkLight)
                                                }
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
                                    Text(
                                        text = scanNotice,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = VioletInkLight
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

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
                                                text = "Supports PDF, EPUB, MOBI, AZW/AZW3, FB2, TXT, and DOCX with embedded cover extraction, brightness control, and Document Scanner.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = VioletInkMuted
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            OutlinedButton(onClick = { showAboutDialog = false }) {
                                                Text("Close", maxLines = 1, softWrap = false, color = VioletInkLight)
                                            }
                                        }
                                    }
                                }

                                if (filteredBooks.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(18.dp),
                                            colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.94f)),
                                            border = BorderStroke(1.2.dp, VioletGrainGlow.copy(alpha = 0.4f)),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                ReadEraGraphicIcon(
                                                    kind = "BOOKS",
                                                    tint = VioletAccentBright,
                                                    modifier = Modifier.size(36.dp)
                                                )
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
                                            val rawProg = if (book.pageCount > 0) {
                                                (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                            } else 0f
                                            val progress = if (rawProg < 0f) 0f else if (rawProg > 1f) 1f else rawProg
                                            val percent = (progress * 100).toInt()
                                            val isChecked = book.uriString in selectedUris
                                            val coverBmp = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectionMode) {
                                                            selectedUris = if (isChecked) selectedUris - book.uriString else selectedUris + book.uriString
                                                        } else if (!book.isTrashed) {
                                                            openBook(book)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                                border = BorderStroke(1.dp, VioletGrainGlow.copy(alpha = 0.35f)),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (coverBmp != null) {
                                                        Image(
                                                            bitmap = coverBmp.asImageBitmap(),
                                                            contentDescription = book.title,
                                                            modifier = Modifier
                                                                .width(46.dp)
                                                                .height(64.dp)
                                                                .clip(RoundedCornerShape(6.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = (if (book.isFavorite) "★ " else "") + book.title,
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = VioletInkLight,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = "${book.format} • $percent% • ${book.pageCount}p",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = VioletInkMuted
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        LinearProgressIndicator(
                                                            progress = progress,
                                                            color = VioletAccentBright,
                                                            trackColor = VioletGrainGlow.copy(alpha = 0.25f),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(4.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                        )
                                                    }
                                                    if (book.isTrashed) {
                                                        Text(
                                                            text = "Restore",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = VioletAccentBright,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isTrashed = false) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (book.isFavorite) "Fav ✓" else "Fav",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = VioletAccentBright,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isFavorite = !it.isFavorite) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val cols = if (viewMode == LibraryViewMode.GRID_2) 2 else 3
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(cols),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 28.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(filteredBooks, key = { it.uriString }) { book ->
                                            val rawProg = if (book.pageCount > 0) {
                                                (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                            } else 0f
                                            val progress = if (rawProg < 0f) 0f else if (rawProg > 1f) 1f else rawProg
                                            val percent = (progress * 100).toInt()
                                            val isChecked = book.uriString in selectedUris
                                            val coverBmp = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])

                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        if (selectionMode) {
                                                            selectedUris = if (isChecked) selectedUris - book.uriString else selectedUris + book.uriString
                                                        } else if (!book.isTrashed) {
                                                            openBook(book)
                                                        }
                                                    },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = CardDefaults.cardColors(containerColor = VioletCardElevated.copy(alpha = 0.96f)),
                                                border = BorderStroke(
                                                    width = if (selectionMode && isChecked) 2.dp else 1.dp,
                                                    color = if (selectionMode && isChecked) VioletAccentBright else VioletGrainGlow.copy(alpha = 0.35f)
                                                ),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                                            ) {
                                                Column {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .aspectRatio(0.70f)
                                                            .background(VioletCardSurface),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (coverBmp != null) {
                                                            Image(
                                                                bitmap = coverBmp.asImageBitmap(),
                                                                contentDescription = book.title,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        } else {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                modifier = Modifier.padding(8.dp)
                                                            ) {
                                                                Text(
                                                                    text = book.format,
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = VioletAccentBright
                                                                )
                                                                Spacer(modifier = Modifier.height(6.dp))
                                                                Text(
                                                                    text = book.title,
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = VioletInkLight,
                                                                    maxLines = 3,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }

                                                    LinearProgressIndicator(
                                                        progress = progress,
                                                        color = VioletAccentBright,
                                                        trackColor = VioletGrainGlow.copy(alpha = 0.25f),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(3.dp)
                                                    )

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (book.isFavorite) "★ $percent%" else "$percent%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VioletInkLight,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isFavorite = !it.isFavorite) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
                                                        )
                                                        Text(
                                                            text = book.title,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = VioletInkLight,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(horizontal = 4.dp)
                                                        )
                                                        Text(
                                                            text = if (book.isTrashed) "Rest" else "Bin",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (book.isTrashed) VioletAccentBright else MaterialTheme.colorScheme.error,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.clickable {
                                                                val updated = books.map {
                                                                    if (it.uriString == book.uriString) it.copy(isTrashed = !it.isTrashed) else it
                                                                }
                                                                books = updated
                                                                saveBooks(context, updated)
                                                            }
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
                                Row(modifier = Modifier.fillMaxSize()) {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth(0.78f)
                                            .fillMaxHeight(),
                                        color = VioletCardElevated,
                                        shadowElevation = 14.dp
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .statusBarsPadding()
                                                .navigationBarsPadding()
                                                .verticalScroll(rememberScrollState())
                                                .padding(top = 20.dp, bottom = 18.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                ReadEraGraphicIcon(
                                                    kind = "BOOKS",
                                                    tint = VioletAccentBright,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Text(
                                                    text = "e-Bookish",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = VioletInkLight
                                                )
                                            }

                                            HorizontalDivider(color = VioletGrainGlow.copy(alpha = 0.3f))

                                            @Composable
                                            fun DrawerNavRow(
                                                label: String,
                                                iconKind: String,
                                                active: Boolean = false,
                                                onClick: () -> Unit
                                            ) {
                                                val rowTint = if (active) VioletAccentBright else VioletInkMuted
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(
                                                            if (active) VioletCardSurface else ComposeColor.Transparent
                                                        )
                                                        .clickable { onClick() }
                                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    ReadEraGraphicIcon(kind = iconKind, tint = rowTint)
                                                    Text(
                                                        text = label,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (active) VioletAccentBright else VioletInkLight,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            DrawerNavRow("Reading Now", "READING_NOW", section == ReadEraSection.READING_NOW) {
                                                section = ReadEraSection.READING_NOW
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Books & documents", "BOOKS", section == ReadEraSection.BOOKS_AND_DOCS) {
                                                section = ReadEraSection.BOOKS_AND_DOCS
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Favorites", "FAVORITES", section == ReadEraSection.FAVORITES) {
                                                section = ReadEraSection.FAVORITES
                                                showDrawer = false
                                            }
                                            DrawerNavRow("To Read", "TO_READ", section == ReadEraSection.TO_READ) {
                                                section = ReadEraSection.TO_READ
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Have Read", "HAVE_READ", section == ReadEraSection.HAVE_READ) {
                                                section = ReadEraSection.HAVE_READ
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Authors", "AUTHORS", section == ReadEraSection.AUTHORS) {
                                                section = ReadEraSection.AUTHORS
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Series", "SERIES", section == ReadEraSection.SERIES) {
                                                section = ReadEraSection.SERIES
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Collections", "COLLECTIONS", section == ReadEraSection.COLLECTIONS) {
                                                section = ReadEraSection.COLLECTIONS
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Formats", "FORMATS", section == ReadEraSection.FORMATS) {
                                                section = ReadEraSection.FORMATS
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Folders", "FOLDERS") {
                                                showDrawer = false
                                                startFolderScan()
                                            }
                                            DrawerNavRow("Downloads", "DOWNLOADS") {
                                                showDrawer = false
                                                addBookLauncher.launch(arrayOf("*/*"))
                                            }
                                            DrawerNavRow("Doc Scanner", "BOOKS") {
                                                showDrawer = false
                                                showDocScanner = true
                                            }
                                            DrawerNavRow("Trash", "TRASH", section == ReadEraSection.TRASH) {
                                                section = ReadEraSection.TRASH
                                                showDrawer = false
                                            }

                                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = VioletGrainGlow.copy(alpha = 0.3f))

                                            DrawerNavRow("Settings", "SETTINGS", section == ReadEraSection.SETTINGS) {
                                                section = ReadEraSection.SETTINGS
                                                showDrawer = false
                                            }
                                            DrawerNavRow("Send feedback", "FEEDBACK") {
                                                showDrawer = false
                                                val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("mailto:")
                                                    putExtra(Intent.EXTRA_SUBJECT, "e-Bookish Reader Feedback")
                                                }
                                                try {
                                                    context.startActivity(mailIntent)
                                                } catch (_: Exception) {
                                                    showAboutDialog = true
                                                }
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .background(ComposeColor.Black.copy(alpha = 0.52f))
                                            .clickable { showDrawer = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun extractEpubCoverBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            var fallbackImageBytes: ByteArray? = null
            var priorityImageBytes: ByteArray? = null

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val nameLower = entry.name.lowercase()
                            val isImg = nameLower.endsWith(".jpg") ||
                                nameLower.endsWith(".jpeg") ||
                                nameLower.endsWith(".png") ||
                                nameLower.endsWith(".webp")
                            if (isImg) {
                                val bytes = zip.readBytes()
                                if (bytes.size > 1500) {
                                    if (nameLower.contains("cover") || nameLower.contains("title") || nameLower.contains("front")) {
                                        if (priorityImageBytes == null || bytes.size > priorityImageBytes!!.size) {
                                            priorityImageBytes = bytes
                                        }
                                    } else if (fallbackImageBytes == null) {
                                        fallbackImageBytes = bytes
                                    }
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            val chosen = priorityImageBytes ?: fallbackImageBytes ?: return null
            decodeScaledCover(chosen)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractMobiAzwCoverBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.size < 32) return null

            // 1) Check embedded KF8/ZIP archive first
            val zipIdx = findSubsequence(bytes, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            if (zipIdx >= 0 && zipIdx < bytes.size - 16) {
                var bestZipImg: ByteArray? = null
                ZipInputStream(bytes.copyOfRange(zipIdx, bytes.size).inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val n = entry.name.lowercase()
                            if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) {
                                val imgBytes = zip.readBytes()
                                if (imgBytes.size > 2000 && (bestZipImg == null || imgBytes.size > bestZipImg!!.size)) {
                                    bestZipImg = imgBytes
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (bestZipImg != null) return decodeScaledCover(bestZipImg!!)
            }

            // 2) Scan binary payload for embedded JPEG (FF D8 FF) or PNG (89 50 4E 47) header
            val jpgStart = findSubsequence(bytes, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
            if (jpgStart >= 0 && jpgStart < bytes.size - 128) {
                val endMarker = findSubsequenceFrom(bytes, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart + 4)
                val endIdx = if (endMarker > jpgStart) endMarker + 2 else bytes.size
                val jpgSlice = bytes.copyOfRange(jpgStart, endIdx)
                decodeScaledCover(jpgSlice)?.let { return it }
            }

            val pngStart = findSubsequence(bytes, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
            if (pngStart >= 0 && pngStart < bytes.size - 128) {
                val pngSlice = bytes.copyOfRange(pngStart, bytes.size)
                decodeScaledCover(pngSlice)?.let { return it }
            }

            null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeScaledCover(bytes: ByteArray): Bitmap? {
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val maxSide = 480
        val w = raw.width
        val h = raw.height
        if (w <= 1 || h <= 1) return null
        if (w <= maxSide && h <= maxSide) return raw
        return if (w >= h) {
            val newH = ((h.toFloat() / w.toFloat()) * maxSide).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(raw, maxSide, newH, true)
        } else {
            val newW = ((w.toFloat() / h.toFloat()) * maxSide).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(raw, newW, maxSide, true)
        }
    }

    private fun findSubsequenceFrom(haystack: ByteArray, needle: ByteArray, startFrom: Int): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        val safeStart = startFrom.coerceAtLeast(0)
        for (i in safeStart..(haystack.size - needle.size)) {
            var match = true
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun shareExportedScan(context: Context, exported: ExportedScanResult) {
        try {
            if (exported.uris.size == 1) {
                val uri = exported.uris.first()
                val mime = if (exported.format == "PDF") "application/pdf" else "image/jpeg"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, exported.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ${exported.title}"))
            } else if (exported.uris.size > 1) {
                val uriList = ArrayList(exported.uris)
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/jpeg"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                    putExtra(Intent.EXTRA_SUBJECT, exported.title)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share ${exported.title}"))
            }
        } catch (_: Exception) {
        }
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return if (cleaned.isBlank()) "Scanned_Doc" else cleaned
    }

    private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val raw = BitmapFactory.decodeStream(stream) ?: return null
                val maxSide = 1600
                val w = raw.width
                val h = raw.height
                if (w <= maxSide && h <= maxSide) {
                    raw
                } else if (w >= h) {
                    val newH = ((h.toFloat() / w.toFloat()) * maxSide).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(raw, maxSide, newH, true)
                } else {
                    val newW = ((w.toFloat() / h.toFloat()) * maxSide).toInt().coerceAtLeast(1)
                    Bitmap.createScaledBitmap(raw, newW, maxSide, true)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun savePagesAsPdf(context: Context, baseName: String, pages: List<Bitmap>): Uri? {
        val pdfDoc = PdfDocument()
        try {
            for (i in pages.indices) {
                val bmp = pages[i]
                val pageW = bmp.width.coerceAtLeast(1)
                val pageH = bmp.height.coerceAtLeast(1)
                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                page.canvas.drawColor(Color.WHITE)
                page.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDoc.finishPage(page)
            }

            val fileName = "$baseName.pdf"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pdfDoc.writeTo(out)
                }
                return uri
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val outFile = File(dir, fileName)
                FileOutputStream(outFile).use { out ->
                    pdfDoc.writeTo(out)
                }
                return Uri.fromFile(outFile)
            }
        } catch (_: Exception) {
            return null
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun savePagesAsJpegsWithUris(context: Context, baseName: String, pages: List<Bitmap>): List<Uri> {
        val savedUris = mutableListOf<Uri>()
        for (i in pages.indices) {
            val bmp = pages[i]
            val suffix = if (pages.size == 1) "" else "_p${i + 1}"
            val fileName = "$baseName$suffix.jpg"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/e-Bookish")
                    }
                    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        savedUris.add(uri)
                    }
                } else {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "e-Bookish"
                    )
                    if (!dir.exists()) dir.mkdirs()
                    val outFile = File(dir, fileName)
                    FileOutputStream(outFile).use { out ->
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                    }
                    savedUris.add(Uri.fromFile(outFile))
                }
            } catch (_: Exception) {
            }
        }
        return savedUris
    }

    private fun collectBooksFromTreeFast(
        context: Context,
        treeUri: Uri,
        onProgress: (Int) -> Unit
    ): List<Pair<Uri, String>> {
        val found = mutableListOf<Pair<Uri, String>>()
        val queue = ArrayDeque<String>()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        queue.add(rootDocId)

        val skippedDirs = setOf("android", "dcim", "movies", "music", "cache", ".thumbnails")
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        var lastReported = 0
        while (queue.isNotEmpty() && found.size < 300) {
            val parentDocId = queue.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            try {
                context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)

                    while (cursor.moveToNext() && found.size < 300) {
                        val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                        val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            val dirLower = name.lowercase()
                            if (dirLower !in skippedDirs && !dirLower.startsWith(".")) {
                                queue.add(docId)
                            }
                        } else {
                            if (detectBookFormat(name, mime) != null) {
                                val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                found.add(Pair(docUri, name))
                                if (found.size - lastReported >= 5) {
                                    lastReported = found.size
                                    onProgress(found.size)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        onProgress(found.size)
        return found
    }

    private fun saveSavedFolderUri(context: Context, uriString: String) {
        val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
        prefs.edit().putString("scan_tree_uri", uriString).apply()
    }

    private fun loadSavedFolderUri(context: Context): String? {
        val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
        return prefs.getString("scan_tree_uri", null)
    }

    @Suppress("DEPRECATION")
    private fun extractEpubChapters(context: Context, uri: Uri): List<String> {
        val chapters = mutableListOf<String>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val lower = entry.name.lowercase()
                            if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                                val buffer = ByteArrayOutputStream()
                                val tmp = ByteArray(4096)
                                var read = zip.read(tmp)
                                while (read != -1) {
                                    buffer.write(tmp, 0, read)
                                    read = zip.read(tmp)
                                }
                                val rawHtml = buffer.toString("UTF-8")
                                val plainText = Html.fromHtml(rawHtml).toString()
                                    .replace(Regex("\n{3,}"), "\n\n")
                                    .trim()
                                if (plainText.isNotBlank()) {
                                    chapters.add(plainText)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        } catch (_: Exception) {
        }
        return chapters
    }

    private fun extractTxtSections(context: Context, uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val text = String(bytes, Charset.forName("UTF-8"))
                splitTextIntoSections(text)
            } ?: listOf()
        } catch (_: Exception) {
            listOf()
        }
    }

    @Suppress("DEPRECATION")
    private fun extractFb2Sections(context: Context, uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val rawXml = String(stream.readBytes(), Charset.forName("UTF-8"))
                val normalized = rawXml
                    .replace(Regex("</p>|</div>|</section>|</title>|</subtitle>|</stanza>|</v>"), "\n\n")
                    .replace(Regex("<br\\s*/?>"), "\n")
                val plain = Html.fromHtml(normalized).toString()
                splitTextIntoSections(plain)
            } ?: listOf()
        } catch (_: Exception) {
            listOf()
        }
    }

    private fun extractDocxSections(context: Context, uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var docXml = ""
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory && entry.name.equals("word/document.xml", ignoreCase = true)) {
                            docXml = String(zip.readBytes(), Charset.forName("UTF-8"))
                            break
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                    if (docXml.isBlank()) return listOf()
                    val cleaned = docXml
                        .replace(Regex("</w:p>"), "\n\n")
                        .replace(Regex("<w:tab\\s*/>"), "    ")
                        .replace(Regex("<w:br\\s*/?>"), "\n")
                        .replace(Regex("<[^>]+>"), "")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&apos;", "'")
                    splitTextIntoSections(cleaned)
                }
            } ?: listOf()
        } catch (_: Exception) {
            listOf()
        }
    }

    @Suppress("DEPRECATION")
    private fun extractMobiAzwSections(context: Context, uri: Uri): List<String> {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return listOf()
            if (bytes.isEmpty()) return listOf()

            val zipSignatureIdx = findSubsequence(bytes, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
            if (zipSignatureIdx >= 0 && zipSignatureIdx < bytes.size - 16) {
                val subBytes = bytes.copyOfRange(zipSignatureIdx, bytes.size)
                val embeddedChapters = mutableListOf<String>()
                ZipInputStream(subBytes.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val lower = entry.name.lowercase()
                            if (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")) {
                                val htmlStr = String(zip.readBytes(), Charset.forName("UTF-8"))
                                val plain = Html.fromHtml(htmlStr).toString()
                                    .replace(Regex("\n{3,}"), "\n\n")
                                    .trim()
                                if (plain.isNotBlank()) embeddedChapters.add(plain)
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                if (embeddedChapters.isNotEmpty()) return embeddedChapters
            }

            val sb = StringBuilder()
            var runStart = -1
            for (i in bytes.indices) {
                val b = bytes[i].toInt() and 0xFF
                val isReadable = (b in 0x20..0x7E) || b == 0x0A || b == 0x0D || b == 0x09 || b >= 0xC0
                if (isReadable) {
                    if (runStart == -1) runStart = i
                } else {
                    if (runStart != -1 && i - runStart >= 24) {
                        val chunk = String(bytes, runStart, i - runStart, Charset.forName("UTF-8"))
                            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]"), " ")
                            .replace(Regex("[ ]{3,}"), " ")
                            .trim()
                        if (chunk.length >= 24 && !chunk.startsWith("BOOKMOBI") && !chunk.startsWith("EXTH")) {
                            val decoded = if (chunk.contains("<") && chunk.contains(">")) {
                                Html.fromHtml(chunk).toString().trim()
                            } else {
                                chunk
                            }
                            if (decoded.length >= 20) {
                                if (sb.isNotEmpty()) sb.append("\n\n")
                                sb.append(decoded)
                            }
                        }
                    }
                    runStart = -1
                }
            }
            if (runStart != -1 && bytes.size - runStart >= 24) {
                val chunk = String(bytes, runStart, bytes.size - runStart, Charset.forName("UTF-8")).trim()
                if (chunk.length >= 20) {
                    if (sb.isNotEmpty()) sb.append("\n\n")
                    sb.append(chunk)
                }
            }

            splitTextIntoSections(sb.toString())
        } catch (_: Exception) {
            listOf()
        }
    }

    private fun findSubsequence(haystack: ByteArray, needle: ByteArray): Int {
        if (needle.isEmpty() || haystack.size < needle.size) return -1
        for (i in 0..(haystack.size - needle.size)) {
            var match = true
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun saveBooks(context: Context, books: List<BookItem>) {
        val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (b in books) {
            val obj = JSONObject()
            obj.put("uri", b.uriString)
            obj.put("title", b.title)
            obj.put("format", b.format)
            obj.put("pageCount", b.pageCount)
            obj.put("lastPage", b.lastPage)
            obj.put("isFavorite", b.isFavorite)
            obj.put("isTrashed", b.isTrashed)
            obj.put("author", b.author)
            obj.put("collection", b.collection)
            arr.put(obj)
        }
        prefs.edit().putString("books_json", arr.toString()).apply()
    }

    private fun loadBooks(context: Context): List<BookItem> {
        val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
        val raw = prefs.getString("books_json", null) ?: return listOf()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BookItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val title = obj.getString("title")
                val fmt = obj.optString("format", "PDF")
                list.add(
                    BookItem(
                        uriString = obj.getString("uri"),
                        title = title,
                        format = fmt,
                        pageCount = obj.getInt("pageCount"),
                        lastPage = obj.optInt("lastPage", 0),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isTrashed = obj.optBoolean("isTrashed", false),
                        author = obj.optString("author", if (title.contains(" - ")) title.substringBefore(" - ").trim() else "Library"),
                        collection = obj.optString(
                            "collection",
                            when (fmt) {
                                "EPUB", "MOBI", "AZW", "AZW3", "FB2" -> "Ebooks"
                                "TXT", "DOCX" -> "Notes & Docs"
                                else -> "Documents"
                            }
                        ),
                        cover = null,
                        metadataLoaded = false
                    )
                )
            }
            list
        } catch (_: Exception) {
            listOf()
        }
    }

    private fun getRawFileName(context: Context, uri: Uri): String {
        var name = "Untitled Book"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (_: Exception) {
        }
        return name
    }

    private fun getPdfPageCount(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { it.pageCount }
            } ?: 0
        } catch (_: Exception) {
            0
        }
    }

    private fun renderSinglePage(context: Context, uri: Uri, pageIndex: Int, scale: Float): Bitmap? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return null
                    renderer.openPage(pageIndex).use { page ->
                        val w = (page.width * scale).toInt().coerceAtLeast(1)
                        val h = (page.height * scale).toInt().coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bmp
                    }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun renderAllPages(context: Context, uri: Uri): List<Bitmap> {
        val result = mutableListOf<Bitmap>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return listOf()
            renderer = PdfRenderer(pfd)
            for (i in 0 until renderer.pageCount) {
                renderer.openPage(i).use { page ->
                    val scale = 1.5f
                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    result.add(bitmap)
                }
            }
        } catch (_: Exception) {
            return listOf()
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
        return result
    }
}

private fun cropSidesBitmap(
    src: Bitmap,
    leftFrac: Float,
    topFrac: Float,
    rightFrac: Float,
    bottomFrac: Float
): Bitmap {
    val w = src.width
    val h = src.height
    if (w <= 2 || h <= 2) return src

    val x0 = (w * leftFrac).toInt().coerceIn(0, w - 2)
    val y0 = (h * topFrac).toInt().coerceIn(0, h - 2)
    val x1 = (w * (1f - rightFrac)).toInt().coerceIn(x0 + 1, w)
    val y1 = (h * (1f - bottomFrac)).toInt().coerceIn(y0 + 1, h)
    val cropW = (x1 - x0).coerceAtLeast(1)
    val cropH = (y1 - y0).coerceAtLeast(1)
    return Bitmap.createBitmap(src, x0, y0, cropW, cropH)
}

private fun applyScanFilter(src: Bitmap, mode: ScanFilterMode): Bitmap {
    if (mode == ScanFilterMode.ORIGINAL) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val cm = ColorMatrix()
    when (mode) {
        ScanFilterMode.ORIGINAL -> Unit
        ScanFilterMode.DOC_BW -> {
            cm.setSaturation(0f)
            val contrast = 1.65f
            val brightness = 16f
            val bwMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(bwMatrix)
        }
        ScanFilterMode.MAGIC_COLOR -> {
            cm.setSaturation(1.35f)
            val contrast = 1.25f
            val brightness = 8f
            val colorMatrix = ColorMatrix(
                floatArrayOf(
                    contrast, 0f, 0f, 0f, brightness,
                    0f, contrast, 0f, 0f, brightness,
                    0f, 0f, contrast, 0f, brightness,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            cm.postConcat(colorMatrix)
        }
        ScanFilterMode.GRAYSCALE -> {
            cm.setSaturation(0f)
        }
    }
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(src, 0f, 0f, paint)
    return out
}
