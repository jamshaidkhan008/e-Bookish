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
                        val needsCover = books.any { it.format == "PDF" && it.cover == null }
                        if (needsCover) {
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
                                val alreadyExists = currentList.any { it.uriString == uriStr }
                                if (!alreadyExists) {
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
                                    "Scan complete (${foundUris.size} found). Tap '+ Add Book' to select PDFs or EPUBs."
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
                                        text = "Section ${pagerState.currentPage + 1} of ${epubChapters.size} (EPUB)",
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
                                    text = "Page ${pagerState.currentPage + 1} of ${readerPages.size}",
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

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { scanDevicePdfs() },
                                    enabled = !isScanning
                                ) {
                                    Text(if (isScanning) "Scanning..." : "Scan PDFs")
                                }
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.GRID_2 }
                                ) {
                                    Text(if (viewMode == LibraryViewMode.GRID_2) "[2-Grid]" else "2-Grid")
                                }
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.GRID_3 }
                                ) {
                                    Text(if (viewMode == LibraryViewMode.GRID_3) "[3-Grid]" else "3-Grid")
                                }
                                OutlinedButton(
                                    onClick = { viewMode = LibraryViewMode.LIST }
                                ) {
                                    Text(if (viewMode == LibraryViewMode.LIST) "[List]" else "List")
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
                                placeholder = { Text("Search PDFs and EPUBs...") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (filteredBooks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (books.isEmpty()) {
                                            "Your shelf is empty.\nTap 'Scan PDFs' or '+ Add Book' to import PDFs and EPUBs."
                                        } else {
                                            "No matching books found."
                                        },
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
                                        val progress = if (book.pageCount > 0) {
                                            (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                        } else 0f
                                        val percent = (progress * 100).toInt().coerceIn(0, 100)

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
                                                        text = "$percent% read • ${book.pageCount} ${if (book.format == "EPUB") "sec" else "pages"}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    LinearProgressIndicator(
                                                        progress = progress.coerceIn(0f, 1f),
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
                                        val progress = if (book.pageCount > 0) {
                                            (book.lastPage + 1).toFloat() / book.pageCount.toFloat()
                                        } else 0f
                                        val percent = (progress * 100).toInt().coerceIn(0, 100)

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
                                                            style = MaterialTheme.typography.titleMedium
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
                                                    progress = progress.coerceIn(0f, 1f),
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
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "Del",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.error,
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

    private fun queryMediaStorePdfs(context: Context): List<Uri> {
        val result = mutableListOf<Uri>()
        try {
            val collection = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns._ID)
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR LOWER(${MediaStore.Files.FileColumns.DISPLAY_NAME}) LIKE ?"
            val selectionArgs = arrayOf("application/pdf", "%.pdf")
            context.contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                while (cursor.moveToNext() && idCol >= 0) {
                    val id = cursor.getLong(idCol)
                    result.add(Uri.withAppendedPath(collection, id.toString()))
                }
            }
        } catch (_: Exception) {
        }
        return result
    }

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
                                val plainText = Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY)
                                    .toString()
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
                        cover = null
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
