package com.docuverse.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BookItem(
    val uriString: String,
    val title: String,
    val pageCount: Int,
    val lastPage: Int = 0
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
                    var covers by remember { mutableStateOf>(emptyMap()) }
                    var searchQuery by remember { mutableStateOf("") }
                    var activeBook by remember { mutableStateOf<BookItem?>(null) }
                    var readerPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
                    var isLoadingReader by remember { mutableStateOf(false) }

                    // Load cover thumbnails for saved books
                    LaunchedEffect(books) {
                        val missing = books.filter { !covers.containsKey(it.uriString) }
                        if (missing.isNotEmpty()) {
                            Thread {
                                val newMap = covers.toMutableMap()
                                for (book in missing) {
                                    val thumb = renderSinglePage(context, Uri.parse(book.uriString), 0, 0.7f)
                                    if (thumb != null) {
                                        newMap[book.uriString] = thumb
                                    }
                                }
                                runOnUiThread {
                                    covers = newMap
                                }
                            }.start()
                        }
                    }

                    val addBookLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            } catch (_: Exception) {
                            }

                            Thread {
                                val title = getFileName(context, uri)
                                val count = getPdfPageCount(context, uri)
                                if (count > 0) {
                                    val newBook = BookItem(
                                        uriString = uri.toString(),
                                        title = title,
                                        pageCount = count,
                                        lastPage = 0
                                    )
                                    runOnUiThread {
                                        val updated = listOf(newBook) + books.filter { it.uriString != newBook.uriString }
                                        books = updated
                                        saveBooks(context, updated)
                                    }
                                }
                            }.start()
                        }
                    }

                    fun openBook(book: BookItem) {
                        activeBook = book
                        isLoadingReader = true
                        readerPages = emptyList()
                        Thread {
                            val rendered = renderAllPages(context, Uri.parse(book.uriString))
                            runOnUiThread {
                                readerPages = rendered
                                isLoadingReader = false
                            }
                        }.start()
                    }

                    if (activeBook != null) {
                        val currentBook = activeBook!!
                        BackHandler {
                            activeBook = null
                            readerPages = emptyList()
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = {
                                    activeBook = null
                                    readerPages = emptyList()
                                }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back to Shelf"
                                    )
                                }
                                Text(
                                    text = currentBook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) { pageIndex ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = readerPages[pageIndex].asImageBitmap(),
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
                        // Moon+ Reader style Bookshelf Screen
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
                                    addBookLauncher.launch(arrayOf("application/pdf"))
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Book"
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Book")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search your bookshelf...") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search")
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (filteredBooks.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (books.isEmpty()) {
                                            "Your shelf is empty.\nTap '+ Add Book' to import a PDF."
                                        } else {
                                            "No matching books found."
                                        },
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                                                    val coverBitmap = covers[book.uriString]
                                                    if (coverBitmap != null) {
                                                        Image(
                                                            bitmap = coverBitmap.asImageBitmap(),
                                                            contentDescription = book.title,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "PDF",
                                                            style = MaterialTheme.typography.titleLarge
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Text(
                                                    text = book.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                Spacer(modifier = Modifier.height(6.dp))

                                                LinearProgressIndicator(
                                                    progress = { progress.coerceIn(0f, 1f) },
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
                                                        text = "$percent% • ${book.pageCount}p",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            val updated = books.filter { it.uriString != book.uriString }
                                                            books = updated
                                                            saveBooks(context, updated)
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Remove",
                                                            modifier = Modifier.size(16.dp)
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
    }

    private fun saveBooks(context: Context, books: List<BookItem>) {
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (b in books) {
            val obj = JSONObject()
            obj.put("uri", b.uriString)
            obj.put("title", b.title)
            obj.put("pageCount", b.pageCount)
            obj.put("lastPage", b.lastPage)
            arr.put(obj)
        }
        prefs.edit().putString("books_json", arr.toString()).apply()
    }

    private fun loadBooks(context: Context): List<BookItem> {
        val prefs = context.getSharedPreferences("docuverse_library", Context.MODE_PRIVATE)
        val raw = prefs.getString("books_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<BookItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookItem(
                        uriString = obj.getString("uri"),
                        title = obj.getString("title"),
                        pageCount = obj.getInt("pageCount"),
                        lastPage = obj.optInt("lastPage", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "Untitled PDF"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } catch (_: Exception) {
        }
        return name.removeSuffix(".pdf").removeSuffix(".PDF")
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
            pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
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
            return emptyList()
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
