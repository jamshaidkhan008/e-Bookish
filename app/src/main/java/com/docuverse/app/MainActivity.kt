package com.docuverse.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var pages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
                    var statusMessage by remember { mutableStateOf("No PDF selected yet") }

                    val pdfPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri: Uri? ->
                        if (uri != null) {
                            statusMessage = "Loading PDF pages..."
                            pages = emptyList()
                            Thread {
                                val rendered = renderPdfPages(context, uri)
                                runOnUiThread {
                                    pages = rendered
                                    statusMessage = if (rendered.isNotEmpty()) {
                                        ""
                                    } else {
                                        "Could not read PDF pages"
                                    }
                                }
                            }.start()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DocuVerse",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Button(onClick = {
                                pdfPickerLauncher.launch(arrayOf("application/pdf"))
                            }) {
                                Text("Select PDF")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (pages.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        } else {
                            val pagerState = rememberPagerState(pageCount = { pages.size })

                            Text(
                                text = "Page ${pagerState.currentPage + 1} of ${pages.size}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                                        bitmap = pages[pageIndex].asImageBitmap(),
                                        contentDescription = "PDF Page ${pageIndex + 1}",
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
                                    text = "${pagerState.currentPage + 1} / ${pages.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                OutlinedButton(
                                    onClick = {
                                        if (pagerState.currentPage < pages.size - 1) {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    },
                                    enabled = pagerState.currentPage < pages.size - 1
                                ) {
                                    Text("Next")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderPdfPages(context: Context, uri: Uri): List<Bitmap> {
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
