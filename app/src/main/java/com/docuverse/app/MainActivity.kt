import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
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
​Spacer(modifier = Modifier.height(24.dp))
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
​var showReaderBars by remember { mutableStateOf(false) }
var showAaPanel by remember { mutableStateOf(false) }
var showThumbnailsStrip by remember { mutableStateOf(false) }
var showQuoteComposer by remember { mutableStateOf(false) }
var showDictLookup by remember { mutableStateOf(false) }
var showBookDetails by remember { mutableStateOf(false) }
​var quoteDraft by remember { mutableStateOf("") }
var quoteColorChoice by remember { mutableStateOf(QuoteColorOption.VIOLET) }
var dictWordInput by remember { mutableStateOf("") }
var dictDefInput by remember { mutableStateOf("") }
var readerNotice by remember { mutableStateOf("") }
​val activeCustomFont = remember(selectedCustomFontPath, customFonts) {
val match = customFonts.firstOrNull { it.localPath == selectedCustomFontPath }
if (match != null) loadCustomFontFamily(match) else null
}
val effectiveFontFamily = activeCustomFont ?: epubFontStyle.family
​if (isTextPagedFormat(currentBook.format)) {
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
​LaunchedEffect(pagerState.currentPage) {
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
​HorizontalPager(
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
​Box(
modifier = Modifier
.align(Alignment.CenterStart)
.fillMaxHeight()
.fillMaxWidth(0.16f)
.clickable {
if (pagerState.currentPage > 0) {
scope.launch {
pagerState.animateScrollToPage(pagerState.currentPage - 1)
}
}
}
)
​Box(
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
​Box(
modifier = Modifier
.align(Alignment.CenterEnd)
.fillMaxHeight()
.fillMaxWidth(0.16f)
.clickable {
if (pagerState.currentPage < textBookSections.size - 1) {
scope.launch {
pagerState.animateScrollToPage(pagerState.currentPage + 1)
}
}
}
)
​if (showReaderBars) {
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
) {
Text("← Shelf", color = VioletBarText, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
}
​Text(
text = currentBook.title,
style = MaterialTheme.typography.titleMedium,
color = VioletBarText,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
modifier = Modifier.weight(1f)
)
​OutlinedButton(
onClick = { showBrightnessSlider = !showBrightnessSlider },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) {
Text("☀ ${(screenBrightness * 100).toInt()}%", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}
​OutlinedButton(
onClick = { showAaPanel = !showAaPanel },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) {
Text("Aa", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}
}
​LazyRow(
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
) {
Text(if (isSpeaking) "⏹ Stop Audio" else "🔊 Play Audio", color = VioletInkLight, maxLines = 1, softWrap = false)
}
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
) {
Text("❝ Quote", color = VioletInkLight, maxLines = 1, softWrap = false)
}
}
item {
OutlinedButton(
onClick = { showDictLookup = !showDictLookup },
contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
) {
Text("A Dictionary", color = VioletInkLight, maxLines = 1, softWrap = false)
}
}
item {
OutlinedButton(
onClick = { showThumbnailsStrip = !showThumbnailsStrip },
contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
) {
Text("▦ Sections", color = VioletInkLight, maxLines = 1, softWrap = false)
}
}
item {
OutlinedButton(
onClick = { showBookDetails = !showBookDetails },
contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
) {
Text("ⓘ Details", color = VioletInkLight, maxLines = 1, softWrap = false)
}
}
}
​if (readerNotice.isNotEmpty()) {
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
​if (showBrightnessSlider) {
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
}
}
}
​if (showThumbnailsStrip) {
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
​if (showQuoteComposer) {
Column(
modifier = Modifier
.fillMaxWidth()
.background(VioletCardElevated)
.padding(12.dp),
verticalArrangement = Arrangement.spacedBy(8.dp)
) {
Text("Save Quote ({quoteColorChoice.label})", color = VioletInkLight, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
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
colors = OutlinedTextFieldDefaults.colors(
focusedTextColor = VioletInkLight,
unfocusedTextColor = VioletInkLight
)
)
Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
Button(
onClick = {
if (quoteDraft.isNotBlank()) {
val entry = QuoteEntry(
id = "{System.currentTimeMillis()}_${Random.nextInt(9999)}",
bookTitle = currentBook.title,
sectionIndex = pagerState.currentPage,
text = quoteDraft.trim(),
colorName = quoteColorChoice.name
)
val nextQ = listOf(entry) + quotes
quotes = nextQ
saveQuotes(context, nextQ)
showQuoteComposer = false
readerNotice = "Saved quote with ${quoteColorChoice.label} highlight."
}
},
colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
) { Text("Save Quote", color = VioletInkLight) }
OutlinedButton(onClick = { showQuoteComposer = false }) {
Text("Cancel", color = VioletInkLight)
}
}
}
}
​if (showDictLookup) {
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
definition = dictDefInput.trim().ifBlank { "Saved from {currentBook.title}" },
sourceBook = currentBook.title
)
val nextD = listOf(entry) + dictionaryEntries.filter { !it.word.equals(entry.word, true) }
dictionaryEntries = nextD
saveDictionary(context, nextD)
dictWordInput = ""
dictDefInput = ""
showDictLookup = false
readerNotice = "Saved '{entry.word}' to Dictionary."
}
},
colors = ButtonDefaults.buttonColors(containerColor = VioletAccentBright)
) { Text("Save Word", color = VioletInkLight) }
OutlinedButton(onClick = { showDictLookup = false }) {
Text("Close", color = VioletInkLight)
}
}
}
}
​if (showBookDetails) {
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
OutlinedButton(onClick = { showBookDetails = false }) {
Text("Close", color = VioletInkLight)
}
}
}
}
​if (showAaPanel) {
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
​Row(
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
​Row(
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
onClick = { customFontPicker.launch(arrayOf("/")) },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) {
Text("+ Font", color = VioletBarText, maxLines = 1, softWrap = false)
}
}
​if (customFonts.isNotEmpty()) {
LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
items(customFonts) { cf ->
val sel = selectedCustomFontPath == cf.localPath
OutlinedButton(
onClick = { selectedCustomFontPath = if (sel) null else cf.localPath },
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
) {
Text(if (sel) "${cf.name} ✓" else cf.name, color = VioletBarText, maxLines = 1, softWrap = false)
}
}
}
}
​Row(
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
​Row(
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
​val indentLabel = when (epubParagraphIndentLevel) {
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
​val pct = ((pagerState.currentPage + 1) * 100) / textBookSections.size
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
​Text(
text = "${pagerState.currentPage + 1} of ${textBookSections.size} ($pct%)",
style = MaterialTheme.typography.labelLarge,
fontWeight = FontWeight.Bold,
color = VioletBarText
)
​OutlinedButton(
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
​LaunchedEffect(pagerState.currentPage) {
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
​HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
val pageBmp: Bitmap = readerPages[pageIndex]
var pinScale by remember(pageIndex, currentBook.uriString) { mutableFloatStateOf(1f) }
var panOffset by remember(pageIndex, currentBook.uriString) { mutableStateOf(Offset.Zero) }
​Box(
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
​if (showReaderBars) {
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
​Text(
text = currentBook.title,
style = MaterialTheme.typography.titleMedium,
color = VioletBarText,
maxLines = 1,
overflow = TextOverflow.Ellipsis,
modifier = Modifier.weight(1f)
)
​OutlinedButton(
onClick = { showThumbnailsStrip = !showThumbnailsStrip },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) { Text("▦ Pages", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
​OutlinedButton(
onClick = { showBookDetails = !showBookDetails },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
) { Text("ⓘ", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
​OutlinedButton(
onClick = { showBrightnessSlider = !showBrightnessSlider },
border = BorderStroke(1.dp, VioletBarText.copy(alpha = 0.65f)),
contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
) {
Text("☀ ${(screenBrightness * 100).toInt()}%", color = VioletBarText, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
}
}
​if (showThumbnailsStrip) {
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
​if (showBookDetails) {
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
OutlinedButton(onClick = { showBookDetails = false }) {
Text("Close", color = VioletInkLight)
}
}
}
}
​if (showBrightnessSlider) {
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
}
}
}
}
​val pagePct = ((pagerState.currentPage + 1) * 100) / readerPages.size
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
​val sectionBaseList = when (section) {
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
ReadEraSection.QUOTES -> emptyList()
ReadEraSection.DICTIONARY -> emptyList()
ReadEraSection.TRASH -> trashedBooks
ReadEraSection.SETTINGS -> activeBooks
}
​val filteredBooks = sectionBaseList.filter {
it.title.contains(searchQuery, ignoreCase = true)
}
​Box(modifier = Modifier.fillMaxSize()) {
DarkVioletTextureBackground(modifier = Modifier.fillMaxSize())
​Column(
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
) {
Text("☰ Menu", maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
}
​Text(
text = section.label,
style = MaterialTheme.typography.titleLarge,
fontWeight = FontWeight.Bold,
color = VioletInkLight,
maxLines = 1,
overflow = TextOverflow.Ellipsis
)
}
​OutlinedButton(
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
) {
Text(viewMode.label, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
}
​Spacer(modifier = Modifier.width(6.dp))
​Box {
OutlinedButton(
onClick = { showOverflowMenu = true },
shape = RoundedCornerShape(12.dp),
border = BorderStroke(1.dp, VioletAccentBright.copy(alpha = 0.65f)),
colors = ButtonDefaults.outlinedButtonColors(
containerColor = VioletCardSurface,
contentColor = VioletInkLight
),
contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
) {
Text("⋮", maxLines = 1, softWrap = false, fontWeight = FontWeight.Bold)
}
​DropdownMenu(
expanded = showOverflowMenu,
onDismissRequest = { showOverflowMenu = false },
containerColor = VioletCardElevated
) {
DropdownMenuItem(
text = { Text("Import Books", color = VioletInkLight) },
onClick = {
showOverflowMenu = false
addBookLauncher.launch(arrayOf("/"))
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
​if (showSyncDialog) {
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
OutlinedButton(onClick = { syncImportLauncher.launch(arrayOf("application/json", "/")) }) {
Text("Import Sync", color = VioletInkLight, maxLines = 1, softWrap = false)
}
OutlinedButton(onClick = { showSyncDialog = false }) {
Text("Close", color = VioletInkLight, maxLines = 1, softWrap = false)
}
}
}
}
}
​if (section == ReadEraSection.FORMATS) {
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
OutlinedButton(onClick = { isDarkTheme = !isDarkTheme }) {
Text(if (isDarkTheme) "Violet: Deep" else "Violet: Plum", maxLines = 1, softWrap = false, color = VioletInkLight)
}
OutlinedButton(onClick = { useDefaultCovers = !useDefaultCovers }) {
Text(if (useDefaultCovers) "Default Cover: ON" else "Default Cover: OFF", maxLines = 1, softWrap = false, color = VioletInkLight)
}
OutlinedButton(onClick = { customFontPicker.launch(arrayOf("/")) }) {
Text("My Fonts (+)", maxLines = 1, softWrap = false, color = VioletInkLight)
}
}
if (customFonts.isNotEmpty()) {
Text("Installed custom fonts: ${customFonts.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall, color = VioletInkMuted)
}
}
}
}
​if (scanNotice.isNotEmpty()) {
Spacer(modifier = Modifier.height(8.dp))
Text(text = scanNotice, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = VioletInkLight)
}
​Spacer(modifier = Modifier.height(14.dp))
​if (section == ReadEraSection.QUOTES) {
LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
item {
OutlinedButton(onClick = { quoteColorFilter = "ALL" }) {
Text(if (quoteColorFilter == "ALL") "All ✓" else "All", color = VioletInkLight)
}
}
items(QuoteColorOption.entries) { qc ->
OutlinedButton(onClick = { quoteColorFilter = qc.name }) {
Text(if (quoteColorFilter == qc.name) "{qc.label} ✓" else qc.label, color = qc.swatch)
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
Text("“{q.text}”", color = VioletInkLight, style = MaterialTheme.typography.bodyMedium)
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
​Spacer(modifier = Modifier.height(18.dp))
​if (showAboutDialog) {
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
OutlinedButton(onClick = { showAboutDialog = false }) {
Text("Close", maxLines = 1, softWrap = false, color = VioletInkLight)
}
}
}
}
​if (filteredBooks.isEmpty()) {
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
val rawProg = if (book.pageCount > 0) {
(book.lastPage + 1).toFloat() / book.pageCount.toFloat()
} else 0f
val progress = rawProg.coerceIn(0f, 1f)
val percent = (progress * 100).toInt()
val isChecked = book.uriString in selectedUris
val coverBmp = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])
​Card(
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
Text(
text = if (book.isTrashed) "Restore" else if (book.isFavorite) "Fav ✓" else "Fav",
style = MaterialTheme.typography.labelMedium,
color = VioletAccentBright,
fontWeight = FontWeight.Bold,
modifier = Modifier.clickable {
val updated = books.map {
if (it.uriString == book.uriString) {
if (book.isTrashed) it.copy(isTrashed = false) else it.copy(isFavorite = !it.isFavorite)
} else it
}
books = updated
saveBooks(context, updated)
}
)
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
val progress = rawProg.coerceIn(0f, 1f)
val percent = (progress * 100).toInt()
val isChecked = book.uriString in selectedUris
val coverBmp = if (useDefaultCovers) null else (book.cover ?: coverBitmapCache[book.uriString])
​Card(
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
Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
Text(book.format, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VioletAccentBright)
Spacer(modifier = Modifier.height(6.dp))
Text(book.title, style = MaterialTheme.typography.bodySmall, color = VioletInkLight, maxLines = 3, overflow = TextOverflow.Ellipsis)
}
}
}
​LinearProgressIndicator(
progress = progress,
color = VioletAccentBright,
trackColor = VioletGrainGlow.copy(alpha = 0.25f),
modifier = Modifier.fillMaxWidth().height(3.dp)
)
​Row(
modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
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
modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
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
}
​if (showDrawer) {
BackHandler { showDrawer = false }
Row(modifier = Modifier.fillMaxSize()) {
Surface(
modifier = Modifier.fillMaxWidth(0.78f).fillMaxHeight(),
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
modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
verticalAlignment = Alignment.CenterVertically,
horizontalArrangement = Arrangement.spacedBy(10.dp)
) {
ReadEraGraphicIcon(kind = "BOOKS", tint = VioletAccentBright, modifier = Modifier.size(28.dp))
Text("e-Bookish", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VioletInkLight)
}
​HorizontalDivider(color = VioletGrainGlow.copy(alpha = 0.3f))
​@Composable
fun DrawerNavRow(label: String, iconKind: String, active: Boolean = false, onClick: () -> Unit) {
val rowTint = if (active) VioletAccentBright else VioletInkMuted
Row(
modifier = Modifier
.fillMaxWidth()
.background(if (active) VioletCardSurface else ComposeColor.Transparent)
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
​DrawerNavRow("Reading Now", "READING_NOW", section == ReadEraSection.READING_NOW) { section = ReadEraSection.READING_NOW; showDrawer = false }
DrawerNavRow("Books & documents", "BOOKS", section == ReadEraSection.BOOKS_AND_DOCS) { section = ReadEraSection.BOOKS_AND_DOCS; showDrawer = false }
DrawerNavRow("Favorites", "FAVORITES", section == ReadEraSection.FAVORITES) { section = ReadEraSection.FAVORITES; showDrawer = false }
DrawerNavRow("To Read", "TO_READ", section == ReadEraSection.TO_READ) { section = ReadEraSection.TO_READ; showDrawer = false }
DrawerNavRow("Have Read", "HAVE_READ", section == ReadEraSection.HAVE_READ) { section = ReadEraSection.HAVE_READ; showDrawer = false }
DrawerNavRow("Authors", "AUTHORS", section == ReadEraSection.AUTHORS) { section = ReadEraSection.AUTHORS; showDrawer = false }
DrawerNavRow("Series", "SERIES", section == ReadEraSection.SERIES) { section = ReadEraSection.SERIES; showDrawer = false }
DrawerNavRow("Collections", "COLLECTIONS", section == ReadEraSection.COLLECTIONS) { section = ReadEraSection.COLLECTIONS; showDrawer = false }
DrawerNavRow("Formats", "FORMATS", section == ReadEraSection.FORMATS) { section = ReadEraSection.FORMATS; showDrawer = false }
DrawerNavRow("Quotes", "QUOTES", section == ReadEraSection.QUOTES) { section = ReadEraSection.QUOTES; showDrawer = false }
DrawerNavRow("Dictionary", "DICTIONARY", section == ReadEraSection.DICTIONARY) { section = ReadEraSection.DICTIONARY; showDrawer = false }
DrawerNavRow("Synchronization", "SYNC") { showDrawer = false; showSyncDialog = true }
DrawerNavRow("Folders", "FOLDERS") { showDrawer = false; startFolderScan() }
DrawerNavRow("Downloads", "DOWNLOADS") { showDrawer = false; addBookLauncher.launch(arrayOf("/")) }
DrawerNavRow("Doc Scanner", "BOOKS") { showDrawer = false; showDocScanner = true }
DrawerNavRow("Trash", "TRASH", section == ReadEraSection.TRASH) { section = ReadEraSection.TRASH; showDrawer = false }
​HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = VioletGrainGlow.copy(alpha = 0.3f))
​DrawerNavRow("Settings", "SETTINGS", section == ReadEraSection.SETTINGS) { section = ReadEraSection.SETTINGS; showDrawer = false }
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
​Box(
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
​private fun importCustomFontFile(context: Context, uri: Uri, rawName: String): CustomFontEntry? {
return try {
val fontsDir = File(context.filesDir, "custom_fonts")
if (!fontsDir.exists()) fontsDir.mkdirs()
val safeName = sanitizeFileName(rawName)
val targetFile = File(fontsDir, safeName)
context.contentResolver.openInputStream(uri)?.use { input ->
FileOutputStream(targetFile).use { out -> input.copyTo(out) }
} ?: return null
if (targetFile.length() <= 100) return null
CustomFontEntry(name = safeName.substringBeforeLast("."), localPath = targetFile.absolutePath)
} catch (_: Exception) {
null
}
}
​private fun saveCustomFonts(context: Context, list: List<CustomFontEntry>) {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
val arr = JSONArray()
list.forEach { f ->
arr.put(JSONObject().apply {
put("name", f.name)
put("path", f.localPath)
})
}
prefs.edit().putString("custom_fonts_json", arr.toString()).apply()
}
​private fun loadCustomFonts(context: Context): List<CustomFontEntry> {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
val raw = prefs.getString("custom_fonts_json", null) ?: return listOf()
return try {
val arr = JSONArray(raw)
(0 until arr.length()).mapNotNull { i ->
val o = arr.getJSONObject(i)
val p = o.optString("path", "")
if (p.isNotBlank() && File(p).exists()) {
CustomFontEntry(name = o.optString("name", "CustomFont"), localPath = p)
} else null
}
} catch (_: Exception) {
listOf()
}
}
​private fun saveQuotes(context: Context, list: List<QuoteEntry>) {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
prefs.edit().putString("quotes_json", quotesToJsonArray(list).toString()).apply()
}
​private fun loadQuotes(context: Context): List<QuoteEntry> {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
val raw = prefs.getString("quotes_json", null) ?: return listOf()
return parseQuotesJsonArray(try { JSONArray(raw) } catch (_: Exception) { null })
}
​private fun quotesToJsonArray(list: List<QuoteEntry>): JSONArray {
val arr = JSONArray()
list.forEach { q ->
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
​private fun parseQuotesJsonArray(arr: JSONArray?): List<QuoteEntry> {
if (arr == null) return listOf()
return (0 until arr.length()).mapNotNull { i ->
val o = arr.optJSONObject(i) ?: return@mapNotNull null
QuoteEntry(
id = o.optString("id", "$i"),
bookTitle = o.optString("bookTitle", "Book"),
sectionIndex = o.optInt("sectionIndex", 0),
text = o.optString("text", ""),
colorName = o.optString("colorName", QuoteColorOption.VIOLET.name),
timestamp = o.optLong("timestamp", 0L)
)
}
}
​private fun saveDictionary(context: Context, list: List<DictionaryEntry>) {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
prefs.edit().putString("dictionary_json", dictionaryToJsonArray(list).toString()).apply()
}
​private fun loadDictionary(context: Context): List<DictionaryEntry> {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
val raw = prefs.getString("dictionary_json", null) ?: return listOf()
return parseDictionaryJsonArray(try { JSONArray(raw) } catch (_: Exception) { null })
}
​private fun dictionaryToJsonArray(list: List<DictionaryEntry>): JSONArray {
val arr = JSONArray()
list.forEach { d ->
arr.put(JSONObject().apply {
put("word", d.word)
put("definition", d.definition)
put("sourceBook", d.sourceBook)
put("timestamp", d.timestamp)
})
}
return arr
}
​private fun parseDictionaryJsonArray(arr: JSONArray?): List<DictionaryEntry> {
if (arr == null) return listOf()
return (0 until arr.length()).mapNotNull { i ->
val o = arr.optJSONObject(i) ?: return@mapNotNull null
DictionaryEntry(
word = o.optString("word", ""),
definition = o.optString("definition", ""),
sourceBook = o.optString("sourceBook", ""),
timestamp = o.optLong("timestamp", 0L)
)
}
}
​private fun extractEpubCoverBitmap(context: Context, uri: Uri): Bitmap? {
return try {
var fallbackImageBytes: ByteArray? = null
var priorityImageBytes: ByteArray? = null
​context.contentResolver.openInputStream(uri)?.use { inputStream ->
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
​private fun extractMobiAzwCoverBitmap(context: Context, uri: Uri): Bitmap? {
return try {
val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
if (bytes.size < 32) return null
​val zipIdx = findSubsequence(bytes, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
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
​val jpgStart = findSubsequence(bytes, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
if (jpgStart >= 0 && jpgStart < bytes.size - 128) {
val endMarker = findSubsequenceFrom(bytes, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpgStart + 4)
val endIdx = if (endMarker > jpgStart) endMarker + 2 else bytes.size
val jpgSlice = bytes.copyOfRange(jpgStart, endIdx)
decodeScaledCover(jpgSlice)?.let { return it }
}
​val pngStart = findSubsequence(bytes, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47))
if (pngStart >= 0 && pngStart < bytes.size - 128) {
val pngSlice = bytes.copyOfRange(pngStart, bytes.size)
decodeScaledCover(pngSlice)?.let { return it }
}
​null
} catch (_: Exception) {
null
}
}
​private fun decodeScaledCover(bytes: ByteArray): Bitmap? {
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
​private fun findSubsequenceFrom(haystack: ByteArray, needle: ByteArray, startFrom: Int): Int {
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
​private fun shareExportedScan(context: Context, exported: ExportedScanResult) {
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
​private fun sanitizeFileName(raw: String): String {
val cleaned = raw.trim().replace(Regex("[^a-zA-Z0-9.-]"), "")
return if (cleaned.isBlank()) "Scanned_Doc" else cleaned
}
​private fun decodeBitmapFromUri(context: Context, uri: Uri): Bitmap? {
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
​private fun savePagesAsPdf(context: Context, baseName: String, pages: List<Bitmap>): Uri? {
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
​val fileName = "$baseName.pdf"
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
} catch (: Exception) {
return null
} finally {
try {
pdfDoc.close()
} catch (: Exception) {
}
}
}
​private fun savePagesAsJpegsWithUris(context: Context, baseName: String, pages: List<Bitmap>): List<Uri> {
val savedUris = mutableListOf<Uri>()
for (i in pages.indices) {
val bmp = pages[i]
val suffix = if (pages.size == 1) "" else "p${i + 1}"
val fileName = "baseNamesuffix.jpg"
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
} catch (: Exception) {
}
}
return savedUris
}
​private fun collectBooksFromTreeFast(
context: Context,
treeUri: Uri,
onProgress: (Int) -> Unit
): List<Pair<Uri, String>> {
val found = mutableListOf<Pair<Uri, String>>()
val queue = ArrayDeque<String>()
val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
queue.add(rootDocId)
​val skippedDirs = setOf("android", "dcim", "movies", "music", "cache", ".thumbnails")
val projection = arrayOf(
DocumentsContract.Document.COLUMN_DOCUMENT_ID,
DocumentsContract.Document.COLUMN_DISPLAY_NAME,
DocumentsContract.Document.COLUMN_MIME_TYPE
)
​var lastReported = 0
while (queue.isNotEmpty() && found.size < 300) {
val parentDocId = queue.removeFirst()
val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
try {
context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
​while (cursor.moveToNext() && found.size < 300) {
val docId = if (idIdx >= 0) cursor.getString(idIdx) else continue
val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
val mime = if (mimeIdx >= 0) cursor.getString(mimeIdx) ?: "" else ""
​if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
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
​private fun saveSavedFolderUri(context: Context, uriString: String) {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
prefs.edit().putString("scan_tree_uri", uriString).apply()
}
​private fun loadSavedFolderUri(context: Context): String? {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
return prefs.getString("scan_tree_uri", null)
}
​@Suppress("DEPRECATION")
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
​private fun extractTxtSections(context: Context, uri: Uri): List<String> {
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
​@Suppress("DEPRECATION")
private fun extractFb2Sections(context: Context, uri: Uri): List<String> {
return try {
context.contentResolver.openInputStream(uri)?.use { stream ->
val rawXml = String(stream.readBytes(), Charset.forName("UTF-8"))
val normalized = rawXml
.replace(Regex("</p>|</div>|</section>|</title>|</subtitle>|</stanza>|</v>"), "\n\n")
.replace(Regex("<br\s*/?>"), "\n")
val plain = Html.fromHtml(normalized).toString()
splitTextIntoSections(plain)
} ?: listOf()
} catch (_: Exception) {
listOf()
}
}
​private fun extractDocxSections(context: Context, uri: Uri): List<String> {
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
.replace(Regex("<w:tab\s*/>"), "    ")
.replace(Regex("<w:br\s*/?>"), "\n")
.replace(Regex("<[^>]+>"), "")
.replace("&", "&")
.replace("<", "<")
.replace(">", ">")
.replace(""", """)
.replace("'", "'")
splitTextIntoSections(cleaned)
}
} ?: listOf()
} catch (_: Exception) {
listOf()
}
}
​@Suppress("DEPRECATION")
private fun extractMobiAzwSections(context: Context, uri: Uri): List<String> {
return try {
val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return listOf()
if (bytes.isEmpty()) return listOf()
​val zipSignatureIdx = findSubsequence(bytes, byteArrayOf(0x50, 0x4B, 0x03, 0x04))
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
​val sb = StringBuilder()
var runStart = -1
for (i in bytes.indices) {
val b = bytes[i].toInt() and 0xFF
val isReadable = (b in 0x20..0x7E) || b == 0x0A || b == 0x0D || b == 0x09 || b >= 0xC0
if (isReadable) {
if (runStart == -1) runStart = i
} else {
if (runStart != -1 && i - runStart >= 24) {
val chunk = String(bytes, runStart, i - runStart, Charset.forName("UTF-8"))
.replace(Regex("[\x00-\x08\x0B\x0C\x0E-\x1F]"), " ")
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
​splitTextIntoSections(sb.toString())
} catch (_: Exception) {
listOf()
}
}
​private fun findSubsequence(haystack: ByteArray, needle: ByteArray): Int {
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
​private fun booksToJsonArray(books: List<BookItem>): JSONArray {
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
return arr
}
​private fun parseBooksJsonArray(arr: JSONArray?): List<BookItem> {
if (arr == null) return listOf()
val list = mutableListOf<BookItem>()
for (i in 0 until arr.length()) {
val obj = arr.optJSONObject(i) ?: continue
val title = obj.optString("title", "Untitled")
val fmt = obj.optString("format", "PDF")
list.add(
BookItem(
uriString = obj.optString("uri", ""),
title = title,
format = fmt,
pageCount = obj.optInt("pageCount", 1),
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
return list.filter { it.uriString.isNotBlank() }
}
​private fun saveBooks(context: Context, books: List<BookItem>) {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
prefs.edit().putString("books_json", booksToJsonArray(books).toString()).apply()
}
​private fun loadBooks(context: Context): List<BookItem> {
val prefs = context.getSharedPreferences("ebookish_library", Context.MODE_PRIVATE)
val raw = prefs.getString("books_json", null) ?: return listOf()
return parseBooksJsonArray(try { JSONArray(raw) } catch (_: Exception) { null })
}
​private fun getRawFileName(context: Context, uri: Uri): String {
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
​private fun getPdfPageCount(context: Context, uri: Uri): Int {
return try {
context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
PdfRenderer(pfd).use { it.pageCount }
} ?: 0
} catch (_: Exception) {
0
}
}
​private fun renderSinglePage(context: Context, uri: Uri, pageIndex: Int, scale: Float): Bitmap? {
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
​private fun renderAllPages(context: Context, uri: Uri): List<Bitmap> {
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
} catch (: Exception) {
return listOf()
} finally {
try {
renderer?.close()
} catch (: Exception) {
}
try {
pfd?.close()
} catch (_: Exception) {
}
}
return result
}
}
​private fun cropSidesBitmap(
src: Bitmap,
leftFrac: Float,
topFrac: Float,
rightFrac: Float,
bottomFrac: Float
): Bitmap {
val w = src.width
val h = src.height
if (w <= 2 || h <= 2) return src
​val x0 = (w * leftFrac).toInt().coerceIn(0, w - 2)
val y0 = (h * topFrac).toInt().coerceIn(0, h - 2)
val x1 = (w * (1f - rightFrac)).toInt().coerceIn(x0 + 1, w)
val y1 = (h * (1f - bottomFrac)).toInt().coerceIn(y0 + 1, h)
val cropW = (x1 - x0).coerceAtLeast(1)
val cropH = (y1 - y0).coerceAtLeast(1)
return Bitmap.createBitmap(src, x0, y0, cropW, cropH)
}
​private fun applyScanFilter(src: Bitmap, mode: ScanFilterMode): Bitmap {
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
enum class ScanFilterMode {
    ORIGINAL,
    DOC_BW,
    MAGIC_COLOR,
    GRAYSCALE
}
