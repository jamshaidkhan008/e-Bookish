package com.docuverse.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Html
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

enum class LibraryViewMode {
    GRID_2, GRID_3, LIST
}

data class BookItem(
    val uriString: String,
    val title: String,
    val format: String = "PDF",
    val pageCount: Int,
    val lastPage: Int = 0,
    val cover: Bitmap? = null
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

                    var activeBook by remember { mutableStateOf<BookItem?>(null) }
                    var readerPages by remember { mutableStateOf(listOf<Bitmap>()) }
                    var epubChapters by remember { mutableStateOf(listOf<String>()) }
                    var isLoadingReader by remember { mutableStateOf(false) }

                    LaunchedEffect(books.size) {
                        val needsCover = books.filter { it.format == "PDF" && it.cover == null }
                        if (needsCover.isNotEmpty()) {
                            Thread {
                                val updatedList = books.map { b ->
                                    if (b.format == "PDF" && b.cover == null) {
                                        val thumb = renderSinglePage(context, Uri.parse(b.uriString), 0, 0.7f)
                                        b.copy(cover = thumb)
                                    } else {
                                        b
                                    }
                                }
                                runOnUiThread {
                                    books = updatedList
                                }
                            }.start()
                        }
                    }

                    fun importBookUri(uri: Uri) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }

                        val rawName = getRawFileName(context, uri)
                        val isEpub = rawName.lowercase().endsWith(".epub") ||
                            (context.contentResolver.getType(uri)?.contains("epub") == true)
                        val title = rawName
                            .removeSuffix(".pdf").removeSuffix(".PDF")
                            .removeSuffix(".epub").removeSuffix(".EPUB")

                        if (isEpub) {
                            val chapters = extractEpubChapters(context, uri)
                            val count = chapters.size.coerceAtLeast(1)
                            val newBook = BookItem(
                                uriString = uri.toString(),
                                title = title,
                                format = "EPUB",
                                pageCount = count,
                                lastPage = 0,
                                cover = null
                            )
                            runOnUiThread {
                                val updated = listOf(newBook) + books.filter { it.uriString != newBook.uriString }
                                books = updated
                                saveBooks(context, updated)
                            }
                        } else {
                            val count = getPdfPageCount(context, uri)
                            val thumb = renderSinglePage(context, uri, 0, 0.7f)
                            if (count > 0) {
                                val newBook = BookItem(
                                    uriString = uri.toString(),
                                    title = title,
                                    format = "PDF",
                                    pageCount = count,
                                    lastPage = 0,
                                    cover = thumb
                                )
                                runOnUiThread {
                                    val updated = listOf(newBook) + books.filter { it.uriString != newBook.uriString }
                                    books = updated
                                    saveBooks(context, updated)
                                }
                            }
                        }
                    }

                    val addBookLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenMultipleDocuments()
                    ) { uris: List<Uri> ->
                        if (uris.isNotEmpty()) {
                            Thread {
                                for (u in uris) {
                                    importBookUri(u)
                                }
                                runOnUiThread {
                                    scanNotice = "Imported ${uris.size} file(s) into library."
                                }
                            }.start()
                        }
                    }

                    fun scanDevicePdfs() {
                        isScanning = true
                        scanNotice = "Scanning device for PDFs..."
                        Thread {
                            val foundUris = queryMediaStorePdfs(context)
                            var addedCount = 0
                            val currentList = books.toMutableList()
                            for (u in foundUris) {
                                val uriStr = u.toString()
                                if (currentList.none { it.uriString == uriStr }) {
                                    val title = getRawFileName(context, u)
                                        .removeSuffix(".pdf").removeSuffix(".PDF")
                                    val count = getPdfPageCount(context, u)
                                    if (count > 0) {
                                        val thumb = renderSinglePage(context, u, 0, 0.7f)
                                        currentList.add(
                                            0,
                                            BookItem(
                                                uriString = uriStr,
                                                title = title,
                                                format = "PDF",
                                                pageCount = count,
                                                lastPage = 0,
                                                cover = thumb
                                            )
                                        )
                                        addedCount++
                                    }
                                }
                            }
                            runOnUiThread {
                                books = currentList
                                saveBooks(context, currentList)
                                isScanning = false
                                scanNotice = if (addedCount > 0) {
                                    "Added $addedCount new PDF(s). Tap '+ Add Book' to pick from any folder."
                                } else {
                                    "Scan done (${foundUris.size} found). Tap '+ Add Book' to select PDFs or EPUBs from any folder."
                                }
                            }
                        }.start()
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

                    if (activeBook != null) {
                        val currentBook = activeBook as BookItem
                        BackHandler {
                            activeBook = null
                            readerPages = listOf()
                            epubChapters = listOf()
                        }

                        var zoomScale by remember(currentBook.uriString) { mutableStateOf(1f) }
                        var panOffset by remember(currentBook.uriString) { mutableStateOf(Offset.Zero) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(onClick = {
                                    activeBook = null
                                    readerPages = listOf()
                                    epubChapters = listOf()
                                }) {
                                    Text("<- Shelf")
                                }
                                Text(
                                    text = currentBook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Zoom controls row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val nextZoom = (zoomScale - 0.25f).coerceIn(0.75f, 3.5f)
                                        zoomScale = nextZoom
                                        if (nextZoom <= 1f) panOffset = Offset.Zero
                                    }
                                ) {
                                    Text("Zoom -")
                                }
                                Text(
                                    text = "${(zoomScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                OutlinedButton(
                                    onClick = {
                                        zoomScale = (zoomScale + 0.25f).coerceIn(0.75f, 3.5f)
                                    }
                                ) {
                                    Text("Zoom +")
                                }
                                OutlinedButton(
                                    onClick = {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    }
                                ) {
                                    Text("Reset")
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
                                    val startChapter = currentBook.lastPage.coerceIn(0, epubChapters.size - 1)
                                    val pagerState = rememberPagerState(
                                        initialPage = startChapter,
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
                                        text = "Section ${pagerState.currentPage + 1} of${epubChapters.size} (EPUB)",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    HorizontalPager(
                                        state = pagerState,
                                        userScrollEnabled = zoomScale <= 1.01f,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) { chapIndex ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .pointerInput(zoomScale) {
                                                    detectTransformGestures { _, pan, zoom, _ ->
                                                        val newZoom = (zoomScale * zoom).coerceIn(0.75f, 3.5f)
                                                        zoomScale = newZoom
                                                        if (newZoom > 1f) {
                                                            panOffset = panOffset + pan
                                                        } else {
                                                            panOffset = Offset.Zero
                                                        }
                                                    }
                                                }
                                                .graphicsLayer(
                                                    scaleX = zoomScale,
                                                    scaleY = zoomScale,
                                                    translationX = panOffset.x,
                                                    translationY = panOffset.y
                                                )
                                                .verticalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = epubChapters[chapIndex],
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
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
                                            Text("Previous")
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
                                            Text("Next")
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
                                val startPage = currentBook.lastPage.coerceIn(0, readerPages.size - 1)
                                val pagerState = rememberPagerState(
                                    initialPage = startPage,
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
                                    text = "Page ${pagerState.currentPage + 1} of${readerPages.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = zoomScale <= 1.01f,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) { pageIndex ->
                                    val pageBmp: Bitmap = readerPages[pageIndex]
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .pointerInput(zoomScale) {
                                                detectTransformGestures { _, pan, zoom, _ ->
                                                    val newZoom = (zoomScale * zoom).coerceIn(0.75f, 3.5f)
                                                    zoomScale = newZoom
                                                    if (newZoom > 1f) {
                                                        panOffset = panOffset + pan
                                                    } else {
                                                        panOffset = Offset.Zero
                                                    }
                                                }
                                            }
                                            .graphicsLayer(
                                                scaleX = zoomScale,
                                                scaleY = zoomScale,
                                                translationX = panOffset.x,
                                                translationY = panOffset.y
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = pageBmp.asImageBitmap(),
                                            contentDescription = "Page ${pageIndex + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
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
                                        Text("Previous")
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
                                        Text("Next")
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
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "DocuVerse Shelf",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${books.size} book(s) in library",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Button(onClick = {
                                    addBookLauncher.launch(
                                        arrayOf("application/pdf", "application/epub+zip", "*/*")
                                    )
                                }) {
                                    Text("+ Add Book")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Scanner + View Mode switcher row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
