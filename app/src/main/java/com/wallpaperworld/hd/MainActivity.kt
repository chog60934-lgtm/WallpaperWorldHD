package com.wallpaperworld.hd

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Color as AndroidColor

private val bg = Color(0xFF070810)
private val card = Color(0xFF171827)
private val purple = Color(0xFFB12CFF)
private val lavender = Color(0xFF9C83FF)
private data class Wallpaper(val title: String, val category: String, val image: String)

private val wallpaperThemes = listOf(
    "Nature" to "nature landscape",
    "Anime" to "anime illustration",
    "Sci-Fi" to "science fiction futuristic",
    "Space" to "galaxy nebula planet",
    "Cyberpunk" to "cyberpunk neon city",
    "Fantasy" to "fantasy castle dragon",
    "Abstract" to "abstract colorful art",
    "Minimal" to "minimalist geometric",
    "Cars" to "sports car supercar",
    "Architecture" to "modern architecture",
    "Animals" to "wild animals",
    "Ocean" to "ocean underwater",
    "Dark" to "dark moody art",
    "Love" to "romantic flowers",
    "Myanmar" to "Myanmar pagoda landscape",
    "Robots" to "robot futuristic",
    "Gaming" to "gaming setup RGB",
    "Technology" to "technology digital art",
    "Fantasy Art" to "surreal fantasy art",
    "Aesthetic" to "aesthetic dreamy"
)

// Exactly 5,000 offline-generated wallpaper entries across 20 themes.
private val wallpaperList = List(5000) { index ->
    val theme = wallpaperThemes[index % wallpaperThemes.size]
    val number = index + 1
    Wallpaper(
        title = "${theme.first} #$number",
        category = theme.first,
        image = "local:$number"
    )
}

// A completely separate catalogue: exactly 500 dedicated live wallpapers.
private val liveWallpaperList = List(500) { index ->
    val theme = wallpaperThemes[index % wallpaperThemes.size]
    val number = index + 1
    Wallpaper(
        title = "Live ${theme.first} #$number",
        category = "LIVE • ${theme.first}",
        image = "live:$number"
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppContext.context = applicationContext
        setContent { WallpaperWorldApp() }
    }
}
private object AppContext { var context: android.content.Context? = null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WallpaperWorldApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf("All") }
    var tab by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Wallpaper?>(null) }
    val preferences = remember(context) { context.getSharedPreferences("wallpaper_world_preferences", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()) }
    LaunchedEffect(favorites) {
        preferences.edit().putStringSet("favorites", favorites.toSet()).apply()
    }
    val visible = (if (tab == "Live") liveWallpaperList else wallpaperList).filter { w ->
        (tab == "Live" || category == "All" || w.category == category) &&
        (tab != "Favorites" || w.image in favorites) &&
        (query.isBlank() || w.title.contains(query, true) || w.category.contains(query, true))
    }
    MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = card, primary = purple,
        onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = bg) {
            if (selected != null) {
                WallpaperDetail(selected!!, selected!!.image in favorites, { selected = null }, {
                    favorites = if (selected!!.image in favorites) favorites - selected!!.image else favorites + selected!!.image
                }, {
                    scope.launch { Toast.makeText(context, saveImage(selected!!.image), Toast.LENGTH_LONG).show() }
                }, {
                    scope.launch { Toast.makeText(context, setDeviceWallpaper(selected!!.image), Toast.LENGTH_LONG).show() }
                }, {
                    val id = selected!!.image.substringAfter(":").toIntOrNull() ?: 1
                    context.getSharedPreferences("wallpaper_world_preferences", Context.MODE_PRIVATE).edit().putInt("live_wallpaper_id", id).apply()
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(context, LiveWallpaperService::class.java))
                    }
                    try { context.startActivity(intent) } catch (_: Exception) { Toast.makeText(context, "Live Wallpaper preview unavailable", Toast.LENGTH_LONG).show() }
                })
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(purple, Color(0xFF3920A8)))), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Landscape, null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Wallpaper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            Row {
                                Text("World ", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                                Text("HD", color = purple, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                            }
                            Text("EXPLORE  •  DOWNLOAD  •  SET", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { Toast.makeText(context, "VIP collection coming soon", Toast.LENGTH_SHORT).show() }) {
                            Text("👑 VIP", color = Color(0xFFFFC342), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (searchOpen) {
                        OutlinedTextField(value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            placeholder = { Text("Search wallpapers...") }, singleLine = true,
                            trailingIcon = { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null) } },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = purple, unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    } else {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(18.dp))
                            .background(card).clickable { searchOpen = true }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color.LightGray)
                            Spacer(Modifier.width(12.dp)); Text("Search Wallpapers...", color = Color.Gray)
                        }
                    }
                    if (tab == "Home") {
                        HeroBanner { selected = wallpaperList[4] }
                        Text("CATEGORIES", color = Color.LightGray, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 8.dp))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            (listOf("All") + wallpaperThemes.map { it.first }).forEach { item ->
                                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = purple,
                                        selectedLabelColor = Color.White, labelColor = Color.LightGray))
                            }
                        }
                    } else Text(if (tab == "Live") "LIVE WALLPAPERS • 500 EXCLUSIVE" else tab.uppercase(), color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(18.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tab == "Favorites") "Your Favorites" else if (tab == "Live") "✨  Dedicated Live Collection" else "🔥  Trending Wallpapers",
                            color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${visible.size} items", color = lavender, style = MaterialTheme.typography.labelMedium)
                    }
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(visible) { wall ->
                            Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(18.dp)).clickable { selected = wall }) {
                                Image(generatedWallpaper(wall.image), wall.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))))
                                Column(Modifier.align(Alignment.BottomStart).padding(11.dp)) {
                                    Text(wall.title, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(wall.category.uppercase(), color = lavender, style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { favorites = if (wall.image in favorites) favorites - wall.image else favorites + wall.image },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).background(Color(0x88000000), RoundedCornerShape(50))) {
                                    Icon(if (wall.image in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                                        tint = if (wall.image in favorites) Color(0xFFFF5B9A) else Color.White)
                                }
                            }
                        }
                    }
                    NavigationBar(containerColor = Color(0xFF11121C)) {
                        listOf("Home", "Categories", "Live", "Favorites", "Downloads", "Profile").forEach { item ->
                            NavigationBarItem(selected = tab == item, onClick = {
                                tab = item
                                if (item == "Categories") category = "All"
                                if (item == "Downloads" || item == "Profile") {
                                    Toast.makeText(context, "$item section coming soon", Toast.LENGTH_SHORT).show(); tab = "Home"
                                }
                            }, icon = { Icon(when (item) {
                                "Home" -> Icons.Default.Home
                                "Categories" -> Icons.Default.GridView
                                "Live" -> Icons.Default.PlayCircle
                                "Favorites" -> Icons.Default.Favorite
                                "Downloads" -> Icons.Default.Download
                                else -> Icons.Default.Person
                            }, null) }, label = { Text(item, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = purple, selectedTextColor = purple,
                                    unselectedIconColor = Color.LightGray, unselectedTextColor = Color.LightGray,
                                    indicatorColor = Color(0xFF292039)))
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun HeroBanner(onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(190.dp)
        .clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick)) {
        Image(generatedWallpaper("local:4"), "Purple mountain night sky", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xDD09051B), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(20.dp)) {
            Text("Beautiful", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Wallpapers", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("in Your Hand", color = Color(0xFFE3D9FF))
            Spacer(Modifier.height(12.dp))
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = purple)) { Text("Explore Now  →", color = Color.White) }
        }
    }
}
@Composable
private fun WallpaperDetail(wall: Wallpaper, isFavorite: Boolean, onBack: () -> Unit,
    onFavorite: () -> Unit, onDownload: () -> Unit, onSet: () -> Unit, onLive: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }
            Text(wall.title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onFavorite) {
                Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null,
                    tint = if (isFavorite) Color(0xFFFF5B9A) else Color.White)
            }
        }
        Image(generatedWallpaper(wall.image), wall.title, Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onDownload, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                Icon(Icons.Default.Download, null); Spacer(Modifier.width(6.dp)); Text("Download")
            }
            OutlinedButton(onClick = onSet, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Wallpaper, null); Spacer(Modifier.width(6.dp)); Text("Set Wallpaper")
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onLive, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF342052))) {
            Icon(Icons.Default.PlayCircle, null); Spacer(Modifier.width(8.dp)); Text("Set as Live Wallpaper")
        }
    }
}

private val wallpaperBitmapCache = android.util.LruCache<Int, Bitmap>(24)
private fun generatedWallpaper(key: String): androidx.compose.ui.graphics.ImageBitmap {
    val id = key.substringAfter(":").toIntOrNull() ?: 1
    val bitmap = wallpaperBitmapCache.get(id) ?: makeWallpaperBitmap(id).also { wallpaperBitmapCache.put(id, it) }
    return bitmap.asImageBitmap()
}
private fun makeWallpaperBitmap(id: Int): Bitmap {
    val width = 540
    val height = 960
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val random = Random(id)
    val scene = (id - 1).mod(20)
    val hue = ((scene * 31 + id * 7) % 360).toFloat()
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    val palettes = arrayOf(
        intArrayOf(0xFFFF9A9E.toInt(), 0xFF34205F.toInt()), intArrayOf(0xFF081B46.toInt(), 0xFF050817.toInt()),
        intArrayOf(0xFF651B9A.toInt(), 0xFF080D2B.toInt()), intArrayOf(0xFF071D50.toInt(), 0xFF020817.toInt()),
        intArrayOf(0xFF30104A.toInt(), 0xFF080817.toInt()), intArrayOf(0xFF9B70E8.toInt(), 0xFF14234C.toInt()),
        intArrayOf(0xFFEE79B8.toInt(), 0xFF25134D.toInt()), intArrayOf(0xFF171A38.toInt(), 0xFF050711.toInt()),
        intArrayOf(0xFF4B235F.toInt(), 0xFF100D24.toInt()), intArrayOf(0xFF5B9BD5.toInt(), 0xFF101B39.toInt()),
        intArrayOf(0xFF153F52.toInt(), 0xFF06121E.toInt()), intArrayOf(0xFF087E9B.toInt(), 0xFF03152B.toInt()),
        intArrayOf(0xFF151326.toInt(), 0xFF03040B.toInt()), intArrayOf(0xFFFF6B83.toInt(), 0xFF30133D.toInt()),
        intArrayOf(0xFFFFB36B.toInt(), 0xFF302052.toInt()), intArrayOf(0xFF183A65.toInt(), 0xFF050B20.toInt()),
        intArrayOf(0xFF25166D.toInt(), 0xFF050817.toInt()), intArrayOf(0xFF073B70.toInt(), 0xFF030817.toInt()),
        intArrayOf(0xFF6B4A9C.toInt(), 0xFF10132C.toInt()), intArrayOf(0xFFEF9BCB.toInt(), 0xFF25204E.toInt())
    )
    val colors = palettes[scene]
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colors[0], colors[1], Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null

    // Deep sky, stars and glowing celestial body.
    repeat(230) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height * 0.72f
        paint.color = AndroidColor.argb(random.nextInt(90, 235), 225, 225, 255)
        canvas.drawCircle(x, y, random.nextFloat() * 2.0f + 0.35f, paint)
    }
    val moonX = if (scene % 3 == 0) width * 0.72f else width * (0.32f + random.nextFloat() * 0.38f)
    val moonY = height * (0.16f + random.nextFloat() * 0.12f)
    val moonR = 65f + random.nextInt(25, 65)
    val glow = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    glow.color = AndroidColor.argb(45, 180, 130, 255)
    canvas.drawCircle(moonX, moonY, moonR * 1.8f, glow)
    paint.color = if (scene == 0 || scene == 13 || scene == 14) 0xFFFFD6B0.toInt() else 0xFFE4E7FF.toInt()
    canvas.drawCircle(moonX, moonY, moonR, paint)

    // Aurora / nebula ribbons for space and night themes.
    if (scene == 1 || scene == 3 || scene == 11 || scene == 16 || scene == 17 || scene == 18) {
        paint.style = AndroidPaint.Style.STROKE
        for (i in 0..7) {
            paint.strokeWidth = 8f + i * 2f
            paint.color = AndroidColor.argb(22 + i * 4, 70 + i * 15, 100 + i * 8, 255)
            val path = AndroidPath()
            path.moveTo(-40f, height * (0.25f + i * 0.025f))
            path.cubicTo(width * 0.25f, height * (0.02f + i * 0.03f), width * 0.58f,
                height * (0.45f - i * 0.02f), width + 40f, height * (0.18f + i * 0.035f))
            canvas.drawPath(path, paint)
        }
        paint.style = AndroidPaint.Style.FILL
    }

    // Distant glowing city silhouettes for cyberpunk, architecture and technology.
    if (scene == 2 || scene == 4 || scene == 9 || scene == 15 || scene == 16 || scene == 17) {
        var x = 0f
        while (x < width) {
            val w = random.nextInt(24, 62).toFloat()
            val h = random.nextInt(100, 360).toFloat()
            paint.color = if (scene == 4 || scene == 16) 0xFF17133E.toInt() else 0xFF101D3A.toInt()
            canvas.drawRect(x, height * 0.68f - h, x + w, height * 0.82f, paint)
            paint.color = if (random.nextBoolean()) 0xFF25D9FF.toInt() else 0xFFFF4BCB.toInt()
            for (wy in (height * 0.68f - h + 18).toInt()..(height * 0.78f).toInt() step 28) {
                if (random.nextBoolean()) canvas.drawRect(x + 7, wy.toFloat(), x + 12, wy + 9f, paint)
                if (random.nextBoolean()) canvas.drawRect(x + w - 15, wy.toFloat(), x + w - 10, wy + 9f, paint)
            }
            x += w - 3f
        }
        paint.color = 0xFF15142C.toInt()
        canvas.drawRect(0f, height * 0.82f, width.toFloat(), height.toFloat(), paint)
        paint.style = AndroidPaint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = 0x9957CFFF.toInt()
        repeat(5) { i -> canvas.drawLine(0f, height * (0.84f + i * 0.035f), width.toFloat(), height * (0.84f + i * 0.035f), paint) }
        paint.style = AndroidPaint.Style.FILL
    } else {
        // Layered mountain ranges, reflective lake and a cinematic horizon.
        for (layer in 0..4) {
            val path = AndroidPath()
            val baseY = height * (0.59f + layer * 0.075f)
            path.moveTo(0f, height.toFloat())
            path.lineTo(0f, baseY)
            var x = 0f
            while (x < width + 90) {
                val peak = baseY - random.nextInt(45, 220).toFloat() * (1f - layer * 0.12f)
                path.lineTo(x + random.nextInt(25, 100), peak)
                x += random.nextInt(45, 105)
                path.lineTo(x, baseY + random.nextInt(-18, 24))
            }
            path.lineTo(width.toFloat(), height.toFloat())
            path.close()
            paint.color = AndroidColor.HSVToColor(floatArrayOf((hue + layer * 13f + 300f) % 360f,
                (0.48f + layer * 0.07f).coerceAtMost(0.88f), (0.34f - layer * 0.045f).coerceAtLeast(0.09f)))
            canvas.drawPath(path, paint)
        }
        paint.color = 0x665DA8FF
        canvas.drawRect(0f, height * 0.79f, width.toFloat(), height.toFloat(), paint)
        paint.color = 0x55FFFFFF
        repeat(18) { i ->
            val y = height * 0.81f + i * 9f
            val half = random.nextInt(20, 170).toFloat()
            canvas.drawLine(moonX - half, y, moonX + half, y, paint)
        }
        // Tiny tree silhouettes along the shore.
        paint.color = 0xFF080D20.toInt()
        repeat(26) {
            val x = random.nextInt(width).toFloat()
            val y = height * (0.69f + random.nextFloat() * 0.18f)
            val h = random.nextInt(25, 90).toFloat()
            canvas.drawRect(x - 2, y, x + 2, y + h, paint)
            val p = AndroidPath()
            p.moveTo(x, y - h * 0.55f); p.lineTo(x - h * 0.28f, y + 5); p.lineTo(x + h * 0.28f, y + 5); p.close()
            canvas.drawPath(p, paint)
        }
    }

    // Floating petals / light motes create depth and animate naturally in live mode.
    repeat(if (scene == 0 || scene == 13 || scene == 19) 75 else 38) {
        val x = random.nextFloat() * width
        val y = random.nextFloat() * height
        paint.color = AndroidColor.argb(random.nextInt(65, 170),
            if (scene == 0 || scene == 13 || scene == 19) 255 else 160,
            if (scene == 0 || scene == 13 || scene == 19) 145 else 190, 255)
        canvas.drawOval(x, y, x + random.nextInt(3, 9), y + random.nextInt(7, 17), paint)
    }
    return bitmap
}
private suspend fun getBitmap(key: String): Bitmap? = withContext(Dispatchers.Default) {
    try { makeWallpaperBitmap(key.substringAfter(":").toIntOrNull() ?: 1) } catch (_: Exception) { null }
}
private suspend fun saveImage(key: String): String = withContext(Dispatchers.IO) {
    val bitmap = getBitmap(key) ?: return@withContext "Image generation failed"
    try {
        val ctx = AppContext.context ?: return@withContext "App context unavailable"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "WallpaperWorld_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/WallpaperWorld")
        }
        val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext "Could not save image"
        ctx.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        "Saved to Pictures/WallpaperWorld"
    } catch (_: Exception) { "Save failed" }
}
private suspend fun setDeviceWallpaper(key: String): String = withContext(Dispatchers.IO) {
    val bitmap = getBitmap(key) ?: return@withContext "Image generation failed"
    try {
        val ctx = AppContext.context ?: return@withContext "App context unavailable"
        WallpaperManager.getInstance(ctx).setBitmap(bitmap); "Wallpaper set successfully"
    } catch (_: Exception) { "Could not set wallpaper" }
}

