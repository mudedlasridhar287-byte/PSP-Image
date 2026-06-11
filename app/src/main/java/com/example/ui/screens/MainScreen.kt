package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.db.GeneratedImage
import com.example.viewmodel.GenerationState
import com.example.viewmodel.PSPViewModel
import com.example.viewmodel.UserProfile
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

// Cosmic Neon Palette
val ObsidianBg = Color(0xFF09080F)
val DeepIndigo = Color(0xFF130E29)
val CosmicPurple = Color(0xFF7A4BFE)
val AccentTeal = Color(0xFF00F5D4)
val GlassWhite = Color(0x12FFFFFF)
val GlassBorder = Color(0x1A7A4BFE)
val PremiumGold = Color(0xFFFFB703)

@Composable
fun BackgroundObsidianPurple(
    isDark: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) ObsidianBg else Color(0xFFFBF9FF))
            .drawBehind {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x287A4BFE), Color.Transparent),
                            center = Offset(size.width * 0.9f, size.height * 0.15f),
                            radius = size.width * 0.8f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x1A00F5D4), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.85f),
                            radius = size.width * 0.7f
                        )
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x107A4BFE), Color.Transparent),
                            center = Offset(size.width * 0.8f, size.height * 0.1f),
                            radius = size.width * 0.5f
                        )
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    isDark: Boolean = true,
    borderColor: Color = GlassBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val cardBg = if (isDark) Color(0x2B100B21) else Color(0xF5F6F3FC)

    val modifierWithClick = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier.clip(shape)
    }

    Column(
        modifier = modifierWithClick
            .background(cardBg)
            .border(1.dp, if (isDark) borderColor else Color(0x2B7A4BFE), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Base64Image(
    base64String: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap = remember(base64String) {
        try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF201B35)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = "Error Loading Artwork",
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

fun downloadImageToDevice(context: Context, image: GeneratedImage, onResult: (Boolean) -> Unit) {
    try {
        val imageBytes = Base64.decode(image.imageBytes, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return

        val filename = "PSP_Image_${image.id}_${System.currentTimeMillis()}.jpg"
        var outputStream: OutputStream? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PSPImage")
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            outputStream = imageUri?.let { resolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
            val dir = File(imagesDir, "PSPImage")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, filename)
            outputStream = FileOutputStream(file)
        }

        if (outputStream != null) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            onResult(true)
        } else {
            onResult(false)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        onResult(false)
    }
}

@Composable
fun MainScreen(viewModel: PSPViewModel) {
    val navController = rememberNavController()
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "home"

    BackgroundObsidianPurple(isDark = isDark) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = if (isDark) Color(0xF207050E) else Color(0xF2F4F0FC),
                    tonalElevation = 0.dp,
                    modifier = Modifier.border(
                        1.dp,
                        if (isDark) Color(0x137A4BFE) else Color(0x13A887FF),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                ) {
                    val tabs = listOf(
                        Triple("home", Icons.Outlined.Home, Icons.Filled.Home),
                        Triple("generate", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
                        Triple("gallery", Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary),
                        Triple("history", Icons.Outlined.History, Icons.Filled.History),
                        Triple("profile", Icons.Outlined.Person, Icons.Filled.Person),
                        Triple("settings", Icons.Outlined.Settings, Icons.Filled.Settings)
                    )

                    tabs.forEach { (route, outlineIcon, filledIcon) ->
                        val selected = currentRoute == route
                        NavigationBarItem(
                            selected = selected,
                            icon = {
                                Icon(
                                    imageVector = if (selected) filledIcon else outlineIcon,
                                    contentDescription = route,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = route.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = if (isDark) AccentTeal else CosmicPurple,
                                selectedTextColor = if (isDark) AccentTeal else CosmicPurple,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = if (isDark) Color(0xFF191338) else Color(0xFFE6DFF7)
                            ),
                            onClick = {
                                if (currentRoute != route) {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nav_tab_$route")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        HomeScreen(viewModel, onNavigateToGenerate = {
                            navController.navigate("generate")
                        })
                    }
                    composable("generate") {
                        GenerateScreen(viewModel)
                    }
                    composable("gallery") {
                        GalleryScreen(viewModel)
                    }
                    composable("history") {
                        HistoryScreen(viewModel)
                    }
                    composable("profile") {
                        ProfileScreen(viewModel)
                    }
                    composable("settings") {
                        SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: PSPViewModel, onNavigateToGenerate: () -> Unit) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val prompt by viewModel.promptInput.collectAsStateWithLifecycle()
    val isEnhancing by viewModel.isEnhancing.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val randomSuggestions = listOf(
        "A mystical glass fox running through glowing synthwave fog",
        "Overgrown cosmic portal of crystal ruins in a vibrant forest",
        "A cyberpunk astronaut meditating under deep-purple alien stardust"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(CosmicPurple, AccentTeal)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "PSP Image",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else ObsidianBg,
                    letterSpacing = 1.sp
                )
            }
        }

        item {
            Text(
                text = "Create Amazing\nAI Images Instantly",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
                color = if (isDark) Color.White else ObsidianBg,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Turn your imagination into stunning artwork with PSP Image.",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)
            )
        }

        item {
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                isDark = isDark
            ) {
                Text(
                    text = "ENTER YOUR PROMPT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) AccentTeal else CosmicPurple,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.promptInput.value = it },
                    placeholder = {
                        Text(
                            text = "Describe anything you want to create...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("prompt_input_field"),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDark) Color.White else ObsidianBg,
                        unfocusedTextColor = if (isDark) Color.White else ObsidianBg,
                        focusedBorderColor = CosmicPurple,
                        unfocusedBorderColor = if (isDark) Color(0x19FFFFFF) else Color(0x19000000),
                        focusedContainerColor = if (isDark) Color(0x0E000000) else Color(0x0E7A4BFE),
                        unfocusedContainerColor = if (isDark) Color(0x0E000000) else Color(0x0E7A4BFE)
                    ),
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.loadRandomPromptText() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x1400F5D4) else Color(0xFFEDFFF7),
                            contentColor = if (isDark) AccentTeal else Color(0xFF03B09A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Casino",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Surprise Me", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (prompt.isNotBlank()) {
                                viewModel.enhanceCurrentPrompt()
                            } else {
                                Toast.makeText(context, "Write a prompt first!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0x147A4BFE) else Color(0xFFF1EEFF),
                            contentColor = CosmicPurple
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        enabled = !isEnhancing
                    ) {
                        if (isEnhancing) {
                            CircularProgressIndicator(
                                color = CosmicPurple,
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Enhance",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnhancing) "Enhancing..." else "Enhance Prompt",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.generateImages()
                        onNavigateToGenerate()
                    } else {
                        Toast.makeText(context, "Please enter a prompt!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("generate_image_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CosmicPurple, Color(0xFF9E00FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Brush,
                            contentDescription = "Brush",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Image",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "PROMPT INSPIRATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                randomSuggestions.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x0CFFFFFF) else Color(0x07000000))
                            .clickable { viewModel.promptInput.value = text }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Lightbulb",
                            tint = if (isDark) AccentTeal else CosmicPurple,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = if (isDark) Color.LightGray else Color.DarkGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GenerateScreen(viewModel: PSPViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val sessionImages by viewModel.currentSessionImages.collectAsStateWithLifecycle()
    val promptInput by viewModel.promptInput.collectAsStateWithLifecycle()
    val currentRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var zoomImage by remember { mutableStateOf<GeneratedImage?>(null) }

    BackgroundObsidianPurple(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Generator Studio",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else ObsidianBg
                )
            }

            when (generationState) {
                is GenerationState.Idle -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "CloudQueue",
                            tint = Color.Gray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Ready to generate",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else ObsidianBg
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Write a prompt on the Home screen and hit generate!",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
                is GenerationState.Loading -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "loader")
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotate"
                        )

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .drawBehind {
                                    drawArc(
                                        brush = Brush.sweepGradient(
                                            colors = listOf(
                                                CosmicPurple,
                                                AccentTeal,
                                                CosmicPurple.copy(alpha = 0.2f)
                                            )
                                        ),
                                        startAngle = rotation,
                                        sweepAngle = 320f,
                                        useCenter = false,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                                            width = 8.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "Spark",
                                tint = AccentTeal,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = "Synthesizing Art...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color.White else ObsidianBg
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Invoking Google Gemini model engine...",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x1F7A4BFE) else Color(0x0B7A4BFE))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "\"$promptInput\"",
                                color = if (isDark) Color.LightGray else Color.DarkGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                fontStyle = FontStyle.Italic,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is GenerationState.Success -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "GENERATED CREATIONS (${sessionImages.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) AccentTeal else CosmicPurple,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(if (sessionImages.size > 1) 2 else 1),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(sessionImages) { image ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            when (currentRatio) {
                                                "16:9" -> 1.77f
                                                "9:16" -> 0.56f
                                                "3:2" -> 1.5f
                                                else -> 1f
                                            }
                                        )
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { zoomImage = image }
                                ) {
                                    Base64Image(
                                        base64String = image.imageBytes,
                                        contentDescription = "Artwork item",
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color(0xCC000000))
                                                )
                                            )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.toggleFavorite(image) },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0x99000000), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = if (image.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = "Starred",
                                                tint = if (image.isFavorite) Color.Red else Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Row {
                                            IconButton(
                                                onClick = { viewModel.shareImageNative(image) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0x99000000), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "Share",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            IconButton(
                                                onClick = {
                                                    downloadImageToDevice(context, image) { success ->
                                                        val msg = if (success) "Image stored in Pictures!" else "Download issue!"
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(Color(0x99000000), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.setGenerationStateToIdle() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPurple)
                        ) {
                            Text(text = "Generate New Art", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                is GenerationState.Error -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Synthesis Failed",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (generationState as GenerationState.Error).message,
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.generateImages() },
                            colors = ButtonDefaults.buttonColors(containerColor = CosmicPurple)
                        ) {
                            Text(text = "Retry Task")
                        }
                    }
                }
            }
        }
    }

    // Zoom Overlay Dialog
    zoomImage?.let { image ->
        Dialog(
            onDismissRequest = { zoomImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6050409))
                    .clickable { zoomImage = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    ) {
                        Base64Image(
                            base64String = image.imageBytes,
                            contentDescription = "Zoomed visual",
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        isDark = true
                    ) {
                        Text(
                            text = image.prompt,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spec: ${image.aspectRatio} // Qual: ${image.quality}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )

                            Row {
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(image) },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (image.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Fav",
                                        tint = if (image.isFavorite) Color.Red else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.shareImageNative(image) },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        downloadImageToDevice(context, image) { success ->
                                            val msg = if (success) "Image stored!" else "Failed!"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.deleteImage(image)
                                        zoomImage = null
                                    },
                                    modifier = Modifier
                                        .background(Color(0x1AFF0000), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
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

@Composable
fun GalleryScreen(viewModel: PSPViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val allImages by viewModel.allImages.collectAsStateWithLifecycle()
    val favoriteImages by viewModel.favoriteImages.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showFavoritesOnly by remember { mutableStateOf(false) }
    val displayList = if (showFavoritesOnly) favoriteImages else allImages
    var zoomImage by remember { mutableStateOf<GeneratedImage?>(null) }

    BackgroundObsidianPurple(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aesthetic Gallery",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else ObsidianBg
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x1DFFFFFF) else Color(0xFFF1EEF8))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!showFavoritesOnly) CosmicPurple else Color.Transparent)
                            .clickable { showFavoritesOnly = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "All",
                            color = if (!showFavoritesOnly) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showFavoritesOnly) CosmicPurple else Color.Transparent)
                            .clickable { showFavoritesOnly = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Favorites",
                            color = if (showFavoritesOnly) Color.White else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (displayList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (showFavoritesOnly) Icons.Default.FavoriteBorder else Icons.Default.PhotoLibrary,
                        contentDescription = "Visual Library",
                        tint = Color.Gray,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (showFavoritesOnly) "No Favorites Found" else "Gallery is Empty",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else ObsidianBg
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (showFavoritesOnly) "Heart some generated art in history to build your collection!" else "Synthesize some visual arts first to build your collection.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("gallery_art_grid"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList, key = { it.id }) { image ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (image.aspectRatio) {
                                        "16:9" -> 1.77f
                                        "9:16" -> 0.56f
                                        "3:2" -> 1.5f
                                        else -> 1.0f
                                    }
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { zoomImage = image }
                        ) {
                            Base64Image(
                                base64String = image.imageBytes,
                                contentDescription = "Gallery Art",
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color(0xAA000000))
                                        )
                                    )
                            )

                            Text(
                                text = image.prompt,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )

                            if (image.isFavorite) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Starred",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Zoom Dialog Overlay inside Gallery Screen
    zoomImage?.let { image ->
        Dialog(
            onDismissRequest = { zoomImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6050409))
                    .clickable { zoomImage = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clickable(enabled = false) {},
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                    ) {
                        Base64Image(
                            base64String = image.imageBytes,
                            contentDescription = "Zoomed artwork",
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            contentScale = ContentScale.FillWidth
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        isDark = true
                    ) {
                        Text(
                            text = image.prompt,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Spec: ${image.aspectRatio} // Qual: ${image.quality}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )

                            Row {
                                IconButton(
                                    onClick = { viewModel.toggleFavorite(image) },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (image.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Fav",
                                        tint = if (image.isFavorite) Color.Red else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { viewModel.shareImageNative(image) },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        downloadImageToDevice(context, image) { success ->
                                            val msg = if (success) "Saved to Pictures!" else "Failed download!"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .background(Color(0x33FFFFFF), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        viewModel.deleteImage(image)
                                        zoomImage = null
                                    },
                                    modifier = Modifier
                                        .background(Color(0x1AFF0000), CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red,
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

@Composable
fun HistoryScreen(viewModel: PSPViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val list by viewModel.searchedImages.collectAsStateWithLifecycle()
    val context = LocalContext.current

    BackgroundObsidianPurple(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Text(
                text = "Synthesis Logs",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else ObsidianBg,
                modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search logs by prompt name...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .testTag("history_search_input"),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = if (isDark) Color.White else ObsidianBg,
                    unfocusedTextColor = if (isDark) Color.White else ObsidianBg,
                    focusedBorderColor = CosmicPurple,
                    unfocusedBorderColor = if (isDark) Color(0x19FFFFFF) else Color(0x19000000),
                    focusedContainerColor = if (isDark) Color(0x0CFFFFFF) else Color(0x0C7A4BFE),
                    unfocusedContainerColor = if (isDark) Color(0x0CFFFFFF) else Color(0x0C7A4BFE)
                )
            )

            if (list.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Search Logs Icon",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No History Records Found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else ObsidianBg
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Your generated prompts will appear here.", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(list, key = { it.id }) { image ->
                        var isExpanded by remember { mutableStateOf(false) }
                        GlassmorphicCard(
                            isDark = isDark,
                            modifier = Modifier.fillMaxWidth().testTag("history_item_${image.id}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { isExpanded = !isExpanded }
                                ) {
                                    Base64Image(
                                        base64String = image.imageBytes,
                                        contentDescription = "Thumb",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = image.prompt,
                                        color = if (isDark) Color.White else ObsidianBg,
                                        fontSize = 14.sp,
                                        maxLines = if (isExpanded) 10 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Ratio: ${image.aspectRatio}",
                                            fontSize = 11.sp,
                                            color = if (isDark) AccentTeal else CosmicPurple,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Qual: ${image.quality}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(
                                            when (image.aspectRatio) {
                                                "16:9" -> 1.77f
                                                "9:16" -> 0.56f
                                                "3:2" -> 1.5f
                                                else -> 1f
                                            }
                                        )
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    Base64Image(
                                        base64String = image.imageBytes,
                                        contentDescription = "Expanded Visual",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = if (isDark) Color(0x11FFFFFF) else Color(0x11000000))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(image.timestamp)),
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )

                                Row {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Copied prompt", image.prompt)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Prompt copied to Clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(image) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (image.isFavorite) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = "Fav",
                                            tint = if (image.isFavorite) Color.Red else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { viewModel.deleteImage(image) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
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

@Composable
fun ProfileScreen(viewModel: PSPViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val allImages by viewModel.allImages.collectAsStateWithLifecycle()
    val favoriteImages by viewModel.favoriteImages.collectAsStateWithLifecycle()

    var showEditProfile by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }

    BackgroundObsidianPurple(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "My Digital Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else ObsidianBg,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            userProfile?.let { user ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (user.isPremium) listOf(DeepIndigo, Color(0xFF2C1654)) else listOf(Color(0xFF201B35), DeepIndigo)
                            )
                        )
                        .border(
                            1.dp,
                            if (user.isPremium) PremiumGold else CosmicPurple,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        colors = listOf(CosmicPurple, AccentTeal, CosmicPurple)
                                    )
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(ObsidianBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.username.take(2).uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isPremium) PremiumGold else AccentTeal
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = user.username,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = user.email,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (user.isPremium) PremiumGold else Color.Gray)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (user.isPremium) Icons.Default.WorkspacePremium else Icons.Default.Person,
                                    contentDescription = "Badge",
                                    tint = if (user.isPremium) ObsidianBg else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (user.isPremium) "PREMIUM TIER" else "STANDARD TIER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (user.isPremium) ObsidianBg else Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassmorphicCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    ) {
                        Text(text = "TOTAL GENERATIONS", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = allImages.size.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (isDark) Color.White else ObsidianBg)
                    }

                    GlassmorphicCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    ) {
                        Text(text = "FAVORITE ART", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = favoriteImages.size.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Red)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    isDark = isDark
                ) {
                    Text(
                        text = "ACCOUNT ACTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicPurple,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Button(
                        onClick = {
                            inputName = user.username
                            inputEmail = user.email
                            showEditProfile = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0x1A7A4BFE) else Color(0x337A4BFE), contentColor = if (isDark) Color.White else ObsidianBg),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Modify Profile Info", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.togglePremium() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (user.isPremium) Color(0x27FFB703) else Color(0xFFFFB703),
                            contentColor = if (user.isPremium) if (isDark) Color.White else ObsidianBg else Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (user.isPremium) "Downgrade to Standard" else "Upgrade to Premium Mode", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFF0000), contentColor = Color.Red),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Log Out Sessions", fontWeight = FontWeight.Bold)
                    }
                }
            } ?: run {
                var registerName by remember { mutableStateOf("") }
                var registerEmail by remember { mutableStateOf("") }

                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    isDark = isDark
                ) {
                    Text(
                        text = "LOGIN & PROFILE SET UP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = CosmicPurple,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = registerName,
                        onValueChange = { registerName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else ObsidianBg,
                            unfocusedTextColor = if (isDark) Color.White else ObsidianBg,
                            focusedBorderColor = CosmicPurple
                        )
                    )

                    OutlinedTextField(
                        value = registerEmail,
                        onValueChange = { registerEmail = it },
                        label = { Text("Google Account / Email Address") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDark) Color.White else ObsidianBg,
                            unfocusedTextColor = if (isDark) Color.White else ObsidianBg,
                            focusedBorderColor = CosmicPurple
                        )
                    )

                    Button(
                        onClick = { viewModel.login(registerName, registerEmail) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, contentDescription = "Sign in")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Connect with Google", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEditProfile) {
        Dialog(onDismissRequest = { showEditProfile = false }) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = isDark
            ) {
                Text(text = "Modify Profile details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else ObsidianBg)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Username") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = if (isDark) Color.White else ObsidianBg, focusedBorderColor = CosmicPurple)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    label = { Text("Email") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = if (isDark) Color.White else ObsidianBg, focusedBorderColor = CosmicPurple)
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showEditProfile = false }) {
                        Text(text = "Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            viewModel.login(inputName, inputEmail)
                            showEditProfile = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CosmicPurple)
                    ) {
                        Text(text = "Save changes")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: PSPViewModel) {
    val isDark by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currentRatio by viewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val currentQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()
    val currentCount by viewModel.generationCount.collectAsStateWithLifecycle()
    val currentApiKey by viewModel.geminiApiKey.collectAsStateWithLifecycle()

    BackgroundObsidianPurple(isDark = isDark) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(top = 24.dp, bottom = 40.dp)
        ) {
            Text(
                text = "PSP Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isDark) Color.White else ObsidianBg,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                isDark = isDark
            ) {
                Text(
                    text = "VISUAL APPEARANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Dark-Slate Futuristic Theme", fontSize = 14.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
                    Switch(
                        checked = isDark,
                        onCheckedChange = { viewModel.toggleDarkMode() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTeal,
                            checkedTrackColor = CosmicPurple
                        )
                    )
                }
            }

            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                isDark = isDark
            ) {
                Text(
                    text = "CREATION ASPECT RATIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val ratios = listOf(
                    Triple("1:1", Icons.Default.CropSquare, "Square profile art"),
                    Triple("16:9", Icons.Default.Tv, "Cinematic panoramic"),
                    Triple("9:16", Icons.Default.PhoneAndroid, "Stories & wallpaper"),
                    Triple("3:2", Icons.Default.Photo, "Classic landscape")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ratios.forEach { (ratio, icon, desc) ->
                        val selected = currentRatio == ratio
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) CosmicPurple else if (isDark) Color(0x1AFFFFFF) else Color(0x33A887FF))
                                .border(1.dp, if (selected) AccentTeal else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { viewModel.setAspectRatio(ratio) }
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = ratio,
                                tint = if (selected) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = ratio,
                                color = if (selected) Color.White else if (isDark) Color.LightGray else Color.DarkGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                isDark = isDark
            ) {
                Text(
                    text = "IMAGE RENDER QUALITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val qualities = listOf("Standard", "HD", "Ultra HD")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0x14FFFFFF) else Color(0x33A887FF))
                        .padding(4.dp)
                ) {
                    qualities.forEach { quality ->
                        val selected = currentQuality == quality
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) CosmicPurple else Color.Transparent)
                                .clickable { viewModel.setQuality(quality) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quality,
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                isDark = isDark
            ) {
                val premium = userProfile?.isPremium == true
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONCURRENT GENERATION COUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (premium) PremiumGold else Color.Gray,
                        letterSpacing = 1.sp
                    )

                    if (!premium) {
                        Text(
                            text = "PREMIUM",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ObsidianBg,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PremiumGold)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..4).forEach { count ->
                        val selected = currentCount == count
                        val isEnabled = count == 1 || premium

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) CosmicPurple
                                    else if (isDark) Color(0x14FFFFFF)
                                    else Color(0x33A887FF)
                                )
                                .border(
                                    1.dp,
                                    if (selected) AccentTeal else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = isEnabled) { viewModel.setGenerationCount(count) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = count.toString(),
                                color = if (selected) Color.White else if (isEnabled) Color.LightGray else Color.DarkGray.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (!premium) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Standard Tier is limited to 1 image. Upgrade your profile to enable parallel multi-art synthesis up to 4 images!",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp
                    )
                }
            }

            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = isDark
            ) {
                Text(
                    text = "ENGINE INFORMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentTeal,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Active Gemini Model", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "Models/gemini-3.1-flash-image-preview", fontSize = 12.sp, color = if (isDark) Color.White else ObsidianBg, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "API Key Service", fontSize = 12.sp, color = Color.Gray)
                    val keyActive = currentApiKey.isNotEmpty() && currentApiKey != "MY_GEMINI_API_KEY"
                    Text(
                        text = if (keyActive) "Live Secured Connection" else "Local Creative Synthesis Active",
                        fontSize = 12.sp,
                        color = if (keyActive) Color.Green else AccentTeal,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
