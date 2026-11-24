package com.docuverse.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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

                    fun importBookUri(uri: Uri): Boolean {
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
                            val count = if (chapters.isNotEmpty()) chapters.size else 1
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
                            return true
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
                                return true
                            }
                        }
                        return false
                    }

                    val addBookLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenMultipleDocuments()
                    ) { uris: List<Uri> ->
                        if (uris.isNotEmpty()) {
                            Thread {
                                var imported = 0
                                for (u in uris) {
                                    if (importBookUri(u)) imported++
                                }
                                runOnUiThread {
                                    scanNotice = "Imported $imported of ${uris.size} selected file(s)."
                                }
                            }.start()
                        }
                    }

                    fun runPdfScanNow() {
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
                                    "Added $addedCount new PDF(s). Tap '+ Add Book' to pick more."
                                } else if (foundUris.isEmpty()) {
                                    "0 PDFs indexed by MediaStore. Tap '+ Add Book' to select PDFs directly."
                                } else {
                                    "All ${foundUris.size} scanned PDF(s) already in library."
                                }
                            }
                        }.start()
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        runPdfScanNow()
                    }

                    fun startScanWithPermissionCheck() {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            runPdfScanNow()
                        } else {
                            val readPerm = Manifest.permission.READ_EXTERNAL_STORAGE
                            val granted = ContextCompat.checkSelfPermission(context, readPerm) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                runPdfScanNow()
                            } else {
                                permissionLauncher.launch(arrayOf(readPerm))
                            }
                        }
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

                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) { chapIndex ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = epubChapters[chapIndex],
                                                fontSize = (16f * zoomScale).sp,
                                                lineHeight = (24f * zoomScale).sp
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

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
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
                                    onClick = { startScanWithPermissionCheck() },
                                    enabled = !isScanning,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isScanning) "Scanning Device PDFs..." else "Scan Device for PDFs",
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
                                        "Your shelf is empty.\nTap 'Scan Device for PDFs' or '+ Add Book' to import books."
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
