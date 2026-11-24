package com.docuverse.app

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
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

enum class LibraryViewMode {
    GRID_2, GRID_3, LIST
}

enum class ScanFilterMode(val label: String) {
    ORIGINAL("Original"),
    DOC_BW("Doc B&W"),
    MAGIC_COLOR("Magic Color"),
    GRAYSCALE("Grayscale")
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
    val cover: Bitmap? = null,
    val metadataLoaded: Boolean = false
)

data class ExportedScanResult(
    val title: String,
    val format: String,
    val uris: List<Uri>,
    val bookItem: BookItem? = null
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()

                    var books by remember { mutableStateOf(loadBooks(context)) }
                    var viewMode by remember { mutableStateOf(LibraryViewMode.GRID_2) }
                    var searchQuery by remember { mutableStateOf("") }
                    var scanNotice by remember { mutableStateOf("") }
                    var isScanning by remember { mutableStateOf(false) }
                    var showDocScanner by remember { mutableStateOf(false) }

                    var activeBook by remember { mutableStateOf<BookItem?>(null) }
                    var readerPages by remember { mutableStateOf(listOf<Bitmap>()) }
                    var epubChapters by remember { mutableStateOf(listOf<String>()) }
                    var isLoadingReader by remember { mutableStateOf(false) }

                    LaunchedEffect(books.size) {
                        val pending = books.filter { !it.metadataLoaded }
                        if (pending.isNotEmpty()) {
                            Thread {
                                val working = books.toMutableList()
                                var changed = false
                                for (i in working.indices) {
                                    val b = working[i]
                                    if (!b.metadataLoaded) {
                                        val uri = Uri.parse(b.uriString)
                                        if (b.format == "EPUB") {
                                            val chapters = extractEpubChapters(context, uri)
                                            val count = if (chapters.isNotEmpty()) chapters.size else 1
                                            working[i] = b.copy(pageCount = count, metadataLoaded = true)
                                            changed = true
                                        } else {
                                            val count = getPdfPageCount(context, uri)
                                            val thumb = renderSinglePage(context, uri, 0, 0.45f)
                                            working[i] = b.copy(
                                                pageCount = if (count > 0) count else 1,
                                                cover = thumb,
                                                metadataLoaded = true
                                            )
                                            changed = true
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
                        val lower = rawName.lowercase()
                        val mime = context.contentResolver.getType(uri)?.lowercase() ?: ""
                        val isEpub = lower.endsWith(".epub") || mime.contains("epub")
                        val isPdf = lower.endsWith(".pdf") || mime.contains("pdf")

                        if (!isEpub && !isPdf) return null

                        val title = rawName
                            .removeSuffix(".pdf").removeSuffix(".PDF")
                            .removeSuffix(".epub").removeSuffix(".EPUB")

                        return BookItem(
                            uriString = uri.toString(),
                            title = title,
                            format = if (isEpub) "EPUB" else "PDF",
                            pageCount = 1,
                            lastPage = 0,
                            cover = null,
                            metadataLoaded = false
                        )
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
                        activeBook = book
                        isLoadingReader = true
                        readerPages = listOf()
                        epubChapters = listOf()
                        Thread {
                            if (book.format == "EPUB") {
                                val chapters = extractEpubChapters(context, Uri.parse(book.uriString))
                                runOnUiThread {
                                    epubChapters = chapters
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
                                docStatus = "Captured page ${scanPages.size}. Drag Left/Right/Top/Bottom sliders to live-cut."
                            } else {
                                docStatus = "Camera capture cancelled."
                            }
                        }

                        // Opens the phone's visual photo gallery directly
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
                                            docStatus = "Added ${loadedPages.size} gallery photo(s). Select a page and live-cut sides."
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

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { showDocScanner = false }) {
                                    Text("Shelf", maxLines = 1, softWrap = false)
                                }
                                Text(
                                    text = "DocuVerse Scan Studio",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = docTitle,
                                onValueChange = { docTitle = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Document Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { cameraSnapLauncher.launch(null) },
                                    enabled = !isProcessingDoc,
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
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Text("Gallery Photos", maxLines = 1, softWrap = false)
                                }
                            }

                            if (scanPages.isNotEmpty()) {
                                val safeIdx = selectedIndex.coerceIn(0, scanPages.lastIndex)
                                val currentPage = scanPages[safeIdx]
                                val livePreviewBmp = remember(currentPage) { currentPage.renderEdited() }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Page selector strip
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(scanPages.indices.toList()) { idx ->
                                        OutlinedButton(
                                            onClick = { selectedIndex = idx }
                                        ) {
                                            Text(
                                                text = if (idx == safeIdx) "Page ${idx + 1} ✓" else "Page ${idx + 1}",
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Filter buttons for selected page
                                Text(
                                    text = "Filter (Page ${safeIdx + 1})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
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
                                            }
                                        ) {
                                            Text(
                                                text = if (isSel) "${mode.label} ✓" else mode.label,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Live preview card showing real-time side cutting & filter
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Live Cut Preview • Page ${safeIdx + 1}",
                                                style = MaterialTheme.typography.labelLarge
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

                                Spacer(modifier = Modifier.height(8.dp))

                                // Live sliders to cut & set Left, Right, Top, Bottom sides like Adobe Scan
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Live Side Cut Controls",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    OutlinedButton(
                                        onClick = {
                                            updateSelectedPage { it.copy(crop = SideCrop()) }
                                        }
                                    ) {
                                        Text("Reset Sides", maxLines = 1, softWrap = false)
                                    }
                                }

                                Text(
                                    text = "Left: ${(currentPage.crop.left * 100).toInt()}%   •   Right: ${(currentPage.crop.right * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        value = currentPage.crop.left,
                                        onValueChange = { v ->
                                            val clamped = v.coerceIn(0f, 0.4f)
                                            updateSelectedPage { it.copy(crop = it.crop.copy(left = clamped)) }
                                        },
                                        valueRange = 0f..0.4f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Slider(
                                        value = currentPage.crop.right,
                                        onValueChange = { v ->
                                            val clamped = v.coerceIn(0f, 0.4f)
                                            updateSelectedPage { it.copy(crop = it.crop.copy(right = clamped)) }
                                        },
                                        valueRange = 0f..0.4f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Text(
                                    text = "Top: ${(currentPage.crop.top * 100).toInt()}%   •   Bottom: ${(currentPage.crop.bottom * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Slider(
                                        value = currentPage.crop.top,
                                        onValueChange = { v ->
                                            val clamped = v.coerceIn(0f, 0.4f)
                                            updateSelectedPage { it.copy(crop = it.crop.copy(top = clamped)) }
                                        },
                                        valueRange = 0f..0.4f,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Slider(
                                        value = currentPage.crop.bottom,
                                        onValueChange = { v ->
                                            val clamped = v.coerceIn(0f, 0.4f)
                                            updateSelectedPage { it.copy(crop = it.crop.copy(bottom = clamped)) }
                                        },
                                        valueRange = 0f..0.4f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Tap 'Gallery Photos' to pick from your phone gallery or 'Camera' to snap a page.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

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
                                    enabled = !isProcessingDoc && (scanPages.isNotEmpty() || lastExported != null)
                                ) {
                                    Text("Clear", maxLines = 1, softWrap = false)
                                }
                            }

                            if (docStatus.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = docStatus,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            val exported = lastExported
                            if (exported != null && exported.uris.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Scanned ${exported.format}: ${exported.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { shareExportedScan(context, exported) },
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
                                                    Text("Read PDF", maxLines = 1, softWrap = false)
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    lastExported = null
                                                    showDocScanner = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("Done", maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else if (activeBook != null) {
                        val currentBook = activeBook as BookItem
                        BackHandler {
                            activeBook = null
                            readerPages = listOf()
                            epubChapters = listOf()
                        }

                        var zoomScale by remember(currentBook.uriString) { mutableStateOf(1f) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = {
                                    activeBook = null
                                    readerPages = listOf()
                                    epubChapters = listOf()
                                }) {
                                    Text("Shelf", maxLines = 1, softWrap = false)
                                }
                                Text(
                                    text = currentBook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val next = zoomScale - 0.25f
                                        zoomScale = if (next < 0.75f) 0.75f else next
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Zoom -", maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val next = zoomScale + 0.25f
                                        zoomScale = if (next > 2.5f) 2.5f else next
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Zoom +", maxLines = 1, softWrap = false)
                                }
                                OutlinedButton(
                                    onClick = { zoomScale = 1f },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("${(zoomScale * 100).toInt()}%", maxLines = 1, softWrap = false)
                                }
                            }

                            if (isLoadingReader) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator()
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Opening book...")
                                    }
                                }
                            } else if (currentBook.format == "EPUB") {
                                if (epubChapters.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Could not read readable text from this EPUB.")
                                    }
                                } else {
                                    val maxIdx = epubChapters.size - 1
                                    val safeStart = if (currentBook.lastPage < 0) {
                                        0
                                    } else if (currentBook.lastPage > maxIdx) {
                                        maxIdx
                                    } else {
                                        currentBook.lastPage
                                    }
                                    val pagerState = rememberPagerState(
                                        initialPage = safeStart,
                                        pageCount = { epubChapters.size }
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

                                    Text(
                                        text = "Section ${pagerState.currentPage + 1} of ${epubChapters.size} (EPUB)",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            modifier = Modifier.fillMaxSize()
                                        ) { chapIndex ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                                            ) {
                                                Text(
                                                    text = epubChapters[chapIndex],
                                                    fontSize = (16f * zoomScale).sp,
                                                    lineHeight = (24f * zoomScale).sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.22f)
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
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .fillMaxWidth(0.22f)
                                                .clickable {
                                                    if (pagerState.currentPage < epubChapters.size - 1) {
                                                        scope.launch {
                                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                        }
                                                    }
                                                }
                                        )
                                    }

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
                                            enabled = pagerState.currentPage > 0
                                        ) {
                                            Text("Previous", maxLines = 1, softWrap = false)
                                        }

                                        Text(
                                            text = "${((pagerState.currentPage + 1) * 100) / epubChapters.size}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                if (pagerState.currentPage < epubChapters.size - 1) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            },
                                            enabled = pagerState.currentPage < epubChapters.size - 1
                                        ) {
                                            Text("Next", maxLines = 1, softWrap = false)
                                        }
                                    }
                                }
                            } else if (readerPages.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Could not open this PDF file.")
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

                                Text(
                                    text = "Page ${pagerState.currentPage + 1} of ${readerPages.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { pageIndex ->
                                        val pageBmp: Bitmap = readerPages[pageIndex]
                                        val imgWidthFraction = if (zoomScale < 0.5f) {
                                            0.5f
                                        } else if (zoomScale > 1f) {
                                            1f
                                        } else {
                                            zoomScale
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = pageBmp.asImageBitmap(),
                                                contentDescription = "Page ${pageIndex + 1}",
                                                modifier = Modifier.fillMaxWidth(imgWidthFraction),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.2f)
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
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .fillMaxWidth(0.2f)
                                            .clickable {
                                                if (pagerState.currentPage < readerPages.size - 1) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                    }
                                                }
                                            }
                                    )
                                }

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
                                        enabled = pagerState.currentPage > 0
                                    ) {
                                        Text("Previous", maxLines = 1, softWrap = false)
                                    }

                                    Text(
                                        text = "${((pagerState.currentPage + 1) * 100) / readerPages.size}%",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            if (pagerState.currentPage < readerPages.size - 1) {
                                                scope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        },
                                        enabled = pagerState.currentPage < readerPages.size - 1
                                    ) {
                                        Text("Next", maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    } else {
                        val filteredBooks = books.filter {
                            it.title.contains(searchQuery, ignoreCase = true)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 22.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DocuVerse Shelf",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${books.size} book(s) in library",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    addBookLauncher.launch(
                                        arrayOf("application/pdf", "application/epub+zip", "*/*")
                                    )
                                }) {
                                    Text("+ Add Book", maxLines = 1, softWrap = false)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { startFolderScan() },
                                    enabled = !isScanning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (isScanning) "Scanning..." else "Scan Folder",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showDocScanner = true },
                                    enabled = !isScanning,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Doc Scanner",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.GRID_2 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (viewMode == LibraryViewMode.GRID_2) "2-Grid ✓" else "2-Grid",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.GRID_3 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (viewMode == LibraryViewMode.GRID_3) "3-Grid ✓" else "3-Grid",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.LIST },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = if (viewMode == LibraryViewMode.LIST) "List ✓" else "List",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (scanNotice.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = scanNotice,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search PDFs and EPUBs...", maxLines = 1) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (filteredBooks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val emptyText = if (books.isEmpty()) {
                                        "Your shelf is empty.\nTap 'Doc Scanner', 'Scan Folder', or '+ Add Book'."
                                    } else {
                                        "No matching books found."
                                    }
                                    Text(
                                        text = emptyText,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else if (viewMode == LibraryViewMode.LIST) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredBooks, key = { it.uriString }) { book ->
                                        val rawProg = if (book.pageCount > 0) {
                                            (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                        } else {
                                            0f
                                        }
                                        val progress = if (rawProg < 0f) 0f else if (rawProg > 1f) 1f else rawProg
                                        val percent = (progress * 100).toInt()
                                        val unitLabel = if (book.format == "EPUB") "sec" else "pages"

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { openBook(book) },
                                            shape = RoundedCornerShape(10.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "[${book.format}] ${book.title}",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "$percent% read • ${book.pageCount} $unitLabel",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = progress,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Remove",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.clickable {
                                                        val updated = books.filter { it.uriString != book.uriString }
                                                        books = updated
                                                        saveBooks(context, updated)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val cols = if (viewMode == LibraryViewMode.GRID_3) 3 else 2
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(cols),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 24.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(filteredBooks, key = { it.uriString }) { book ->
                                        val rawProg = if (book.pageCount > 0) {
                                            (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                        } else {
                                            0f
                                        }
                                        val progress = if (rawProg < 0f) 0f else if (rawProg > 1f) 1f else rawProg
                                        val percent = (progress * 100).toInt()

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { openBook(book) },
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(0.72f)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val coverBmp = book.cover
                                                    if (coverBmp != null) {
                                                        Image(
                                                            bitmap = coverBmp.asImageBitmap(),
                                                            contentDescription = book.title,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(
                                                            text = book.format,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Text(
                                                    text = book.title,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(4.dp))

                                                LinearProgressIndicator(
                                                    progress = progress,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp)
                                                        .clip(RoundedCornerShape(2.dp))
                                                )

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "$percent% • ${book.pageCount}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Text(
                                                        text = "Del",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.clickable {
                                                            val updated = books.filter { it.uriString != book.uriString }
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
                    }
                }
            }
        }
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
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DocuVerse")
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
                        "DocuVerse"
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
                            val lower = name.lowercase()
                            if (lower.endsWith(".pdf") || lower.endsWith(".epub") ||
                                mime == "application/pdf" || mime.contains("epub")
                            ) {
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
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
        prefs.edit().putString("scan_tree_uri", uriString).apply()
    }

    private fun loadSavedFolderUri(context: Context): String? {
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
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
                                val plainText = Html.fromHtml(rawHtml).toString().trim()
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

    private fun saveBooks(context: Context, books: List<BookItem>) {
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (b in books) {
            val obj = JSONObject()
            obj.put("uri", b.uriString)
            obj.put("title", b.title)
            obj.put("format", b.format)
            obj.put("pageCount", b.pageCount)
            obj.put("lastPage", b.lastPage)
            arr.put(obj)
        }
        prefs.edit().putString("books_json", arr.toString()).apply()
    }

    private fun loadBooks(context: Context): List<BookItem> {
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
        val raw = prefs.getString("books_json", null) ?: return listOf()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BookItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookItem(
                        uriString = obj.getString("uri"),
                        title = obj.getString("title"),
                        format = obj.optString("format", "PDF"),
                        pageCount = obj.getInt("pageCount"),
                        lastPage = obj.optInt("lastPage", 0),
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
