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
    val visible = wallpaperList.filter { w ->
        (category == "All" || w.category == category) &&
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
                    } else Text(tab.uppercase(), color = Color.White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(18.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (tab == "Favorites") "Your Favorites" else "🔥  Trending Wallpapers",
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
                        listOf("Home", "Categories", "Favorites", "Downloads", "Profile").forEach { item ->
                            NavigationBarItem(selected = tab == item, onClick = {
                                tab = item
                                if (item == "Categories") category = "All"
                                if (item == "Downloads" || item == "Profile") {
                                    Toast.makeText(context, "$item section coming soon", Toast.LENGTH_SHORT).show(); tab = "Home"
                                }
                            }, icon = { Icon(when (item) {
                                "Home" -> Icons.Default.Home
                                "Categories" -> Icons.Default.GridView
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
    val width = 540; val height = 960
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val random = Random(id)
    val hue = (id * 47) % 360
    val top = AndroidColor.HSVToColor(floatArrayOf(hue.toFloat(), 0.72f, 0.34f))
    val bottom = AndroidColor.HSVToColor(floatArrayOf(((hue + 55) % 360).toFloat(), 0.88f, 0.10f))
    val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG)
    paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint); paint.shader = null
    repeat(150) {
        val x = random.nextFloat() * width; val y = random.nextFloat() * height * 0.68f
        paint.color = AndroidColor.argb(random.nextInt(80, 230), 235, 225, 255)
        canvas.drawCircle(x, y, random.nextFloat() * 2.2f + 0.4f, paint)
    }
    val cx = random.nextInt(90, width - 90).toFloat()
    val cy = random.nextInt(170, 430).toFloat()
    val radius = random.nextInt(48, 112).toFloat()
    paint.color = AndroidColor.HSVToColor(floatArrayOf(((hue + 35) % 360).toFloat(), 0.38f, 1f))
    paint.setShadowLayer(38f, 0f, 0f, paint.color); canvas.drawCircle(cx, cy, radius, paint); paint.clearShadowLayer()
    for (layer in 0..3) {
        val path = AndroidPath()
        val baseY = height * (0.58f + layer * 0.105f)
        path.moveTo(0f, height.toFloat()); path.lineTo(0f, baseY)
        var x = 0f
        while (x <= width + 100) {
            val peak = baseY - random.nextInt(35, 210).toFloat() * (1f - layer * 0.13f)
            path.lineTo(x + random.nextInt(15, 95), peak)
            x += random.nextInt(55, 125); path.lineTo(x, baseY + random.nextInt(-25, 30))
        }
        path.lineTo(width.toFloat(), height.toFloat()); path.close()
        paint.color = AndroidColor.HSVToColor(floatArrayOf(((hue + layer * 19 + 300) % 360).toFloat(),
            (0.42f + layer * 0.10f).coerceAtMost(0.9f), (0.28f - layer * 0.045f).coerceAtLeast(0.08f)))
        canvas.drawPath(path, paint)
    }
    paint.style = AndroidPaint.Style.STROKE; paint.strokeWidth = 2.2f
    paint.color = AndroidColor.argb(105, 220, 190, 255)
    repeat(8) { i -> val y = height * 0.79f + i * 23f; canvas.drawLine(0f, y, width.toFloat(), y + random.nextInt(-14, 15), paint) }
    paint.style = AndroidPaint.Style.FILL; paint.color = AndroidColor.argb(75, 255, 255, 255)
    val band = AndroidPath(); band.moveTo(0f, height * 0.88f)
    band.cubicTo(width * 0.3f, height * 0.78f, width * 0.65f, height * 0.98f, width.toFloat(), height * 0.84f)
    band.lineTo(width.toFloat(), height.toFloat()); band.lineTo(0f, height.toFloat()); band.close(); canvas.drawPath(band, paint)
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
