package com.docuverse.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DocuVerseApp()
        }
    }
}

@Composable
fun DocuVerseApp() {
    val background = Color(0xFFF5F3EE)
    val green = Color(0xFF174D3A)

    MaterialTheme {
        Scaffold(
            containerColor = background,
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = { Text("⌂") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Text("▤") },
                        label = { Text("Library") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Text("▧") },
                        label = { Text("Scanner") }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = { Text("⚙") },
                        label = { Text("Settings") }
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                Text(
                    "DocuVerse",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = green
                )

                Text(
                    "Your world of books and documents",
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(25.dp))

                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search your books and PDFs") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(Modifier.height(25.dp))

                Text(
                    "Continue Reading",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = green
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "WELCOME TO DOCUVERSE",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Your personal digital library",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Read. Organize. Discover.",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                Text(
                    "My Library",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                val categories = listOf(
                    "All Books" to "▤",
                    "PDF Documents" to "📄",
                    "EPUB Books" to "📚",
                    "Favorites" to "☆"
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categories) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(15.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    item.second,
                                    fontSize = 25.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    item.first,
                                    fontWeight = FontWeight.SemiBold,
                                    color = green
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
