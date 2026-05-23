package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.theme.MasteryGreen
import kotlinx.coroutines.launch

sealed interface ActiveScreen {
    object Dashboard : ActiveScreen
    object Create : ActiveScreen
    data class DeckDetail(val deckId: Int) : ActiveScreen
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CognitaApp(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Dashboard) }
    val scope = rememberCoroutineScope()
    
    // Decks State
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    
    // Top App Bar Navigation Actions
    val currentDeck by viewModel.currentDeck.collectAsStateWithLifecycle()

    // Edge-to-edge root Scaffold
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (activeScreen is ActiveScreen.DeckDetail) {
                                currentDeck?.title ?: "Study Deck"
                            } else {
                                "Cognita Study"
                            },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    if (activeScreen !is ActiveScreen.Dashboard) {
                        IconButton(
                            onClick = {
                                if (activeScreen is ActiveScreen.DeckDetail) {
                                    viewModel.selectDeck(null)
                                }
                                activeScreen = ActiveScreen.Dashboard
                            },
                            modifier = Modifier.testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { /* Help context */ }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Help Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (activeScreen == ActiveScreen.Dashboard) {
                ExtendedFloatingActionButton(
                    text = { Text("Generate Study Deck") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        viewModel.setGenerationIdle()
                        activeScreen = ActiveScreen.Create
                    },
                    modifier = Modifier.testTag("generate_deck_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padValues ->
        // Root container respecting edge-to-edge windows
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val screen = activeScreen) {
                is ActiveScreen.Dashboard -> {
                    DashboardScreen(
                        decks = decks,
                        onSelectDeck = { deckId ->
                            viewModel.selectDeck(deckId)
                            activeScreen = ActiveScreen.DeckDetail(deckId)
                        },
                        onDeleteDeck = { deckId ->
                            viewModel.deleteDeck(deckId)
                        }
                    )
                }
                is ActiveScreen.Create -> {
                    CreateDeckScreen(
                        generationState = generationState,
                        onCreateDeck = { title, materials ->
                            viewModel.createStudyDeck(title, materials)
                        },
                        onNavigateBack = {
                            activeScreen = ActiveScreen.Dashboard
                        },
                        onViewGeneratedDeck = { deckId ->
                            activeScreen = ActiveScreen.DeckDetail(deckId)
                        }
                    )
                }
                is ActiveScreen.DeckDetail -> {
                    DeckDetailContainer(
                        viewModel = viewModel,
                        deckId = screen.deckId
                    )
                }
            }
        }
    }
}

// ==================== DASHBOARD SCREEN ====================

@Composable
fun DashboardScreen(
    decks: List<StudyDeck>,
    onSelectDeck: (Int) -> Unit,
    onDeleteDeck: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header Banner with gradient highlights
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.02f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your Companion, Cognita",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upload any lecture materials, PDF notes, or study text. Gemini AI generates full customized study flashcard blocks, practice MCQs, true/false reviews, and voice narration summary formats.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Library Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "My Study Library",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${decks.size} decks",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (decks.isEmpty()) {
            // High fidelity empty state with a helpful call-to-action hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryBooks,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Text(
                        text = "Your library is empty",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Click the launch FAB below to prompt and customize your very first database study workspace package!",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            // Render active Study Deck cards
            decks.forEach { deck ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDeck(deck.id) }
                        .testTag("deck_card_${deck.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deck.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Generated from notes",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { onDeleteDeck(deck.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("delete_deck_btn_${deck.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Short materials description snippet
                        Text(
                            text = deck.materials,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))

                        // Features indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudyFeatureBadge(imageVector = Icons.Default.Style, label = "Flashcards")
                            StudyFeatureBadge(imageVector = Icons.Default.ListAlt, label = "Tests")
                            StudyFeatureBadge(imageVector = Icons.Default.CheckCircle, label = "Quizzes")
                        }
                    }
                }
            }
            
            // Helpful spacer padding
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun StudyFeatureBadge(
    imageVector: ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

// ==================== GENERATE / CREATE DECK SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDeckScreen(
    generationState: GenerationState,
    onCreateDeck: (String, String) -> Unit,
    onNavigateBack: () -> Unit,
    onViewGeneratedDeck: (Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var materials by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (generationState !is GenerationState.Loading && generationState !is GenerationState.Success) {
            // Material input form
            Text(
                text = "Create Study Deck",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Study Deck Title") },
                placeholder = { Text("e.g. Cellular Biology Lecture 1") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("deck_title_input"),
                shape = RoundedCornerShape(12.dp),
                maxLines = 1,
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = { title = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            OutlinedTextField(
                value = materials,
                onValueChange = { materials = it },
                label = { Text("Study Materials / Notes / Text Source") },
                placeholder = { Text("Paste your study content here... Needs to be at least single sentences, paragraphs or a complete set of topics for Gemini to analyze!") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp)
                    .testTag("deck_materials_input"),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
            )

            if (generationState is GenerationState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Error: ${generationState.message}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank() && materials.isNotBlank()) {
                        submitted = true
                        onCreateDeck(title, materials)
                    }
                },
                enabled = title.isNotBlank() && materials.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_generation_btn"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Companions Package", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        } else if (generationState is GenerationState.Loading) {
            // Sleek generating progress indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(56.dp)
                        )
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Synthesizing AI Decks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = generationState.stage,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "Please keep the application open. Gemini 3.5 Flash is deep-scanning your study content and composing full flashcards, tests, and audio resources.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        } else if (generationState is GenerationState.Success) {
            // Generation successful summary screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MasteryGreen.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MasteryGreen,
                        modifier = Modifier.size(72.dp)
                    )

                    Text(
                        text = "Study Workspace Ready!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Gemini has finished analyzing and synthesized a complete copy of the requested study workspace with interactive visual flashcards, graded performance MCQ tests, quick true/false statement quizzes, and a native text-to-speech audio companion narrator player.",
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { onViewGeneratedDeck(generationState.deckId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("view_deck_after_gen_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MasteryGreen)
                    ) {
                        Text("Launch Study Workspace", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    TextButton(onClick = onNavigateBack) {
                        Text("Return to Library Dashboard")
                    }
                }
            }
        }
    }
}

// ==================== DECK DETAIL WORKSPACE CONTAINER ====================

enum class DeckTab {
    Flashcards, Test, Quizzes, AudioSummary, Notes
}

@Composable
fun DeckDetailContainer(
    viewModel: StudyViewModel,
    deckId: Int
) {
    var selectedTab by remember { mutableStateOf(DeckTab.Flashcards) }
    
    // Core state selectors
    val currentDeck by viewModel.currentDeck.collectAsStateWithLifecycle()
    val flashcards by viewModel.currentFlashcards.collectAsStateWithLifecycle()
    val practiceQuestions by viewModel.currentPracticeQuestions.collectAsStateWithLifecycle()
    val quizQuestions by viewModel.currentQuizQuestions.collectAsStateWithLifecycle()
    val isTtsPlaying by viewModel.isTtsPlaying.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Scrollable material tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 12.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DeckTabButton(
                tab = DeckTab.Flashcards,
                selected = selectedTab == DeckTab.Flashcards,
                icon = Icons.Default.Style,
                label = "Flashcards"
            ) { selectedTab = DeckTab.Flashcards }
            
            DeckTabButton(
                tab = DeckTab.Test,
                selected = selectedTab == DeckTab.Test,
                icon = Icons.Default.ListAlt,
                label = "Practice Test"
            ) { selectedTab = DeckTab.Test }
            
            DeckTabButton(
                tab = DeckTab.Quizzes,
                selected = selectedTab == DeckTab.Quizzes,
                icon = Icons.Default.CheckCircle,
                label = "Quick Quiz"
            ) { selectedTab = DeckTab.Quizzes }
            
            DeckTabButton(
                tab = DeckTab.AudioSummary,
                selected = selectedTab == DeckTab.AudioSummary,
                icon = Icons.Default.VolumeUp,
                label = "Audio Player"
            ) { selectedTab = DeckTab.AudioSummary }

            DeckTabButton(
                tab = DeckTab.Notes,
                selected = selectedTab == DeckTab.Notes,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                label = "My Notes"
            ) { selectedTab = DeckTab.Notes }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                DeckTab.Flashcards -> {
                    FlashcardsTabContent(
                        flashcards = flashcards,
                        onToggleMastery = { cardId, isMastered ->
                            viewModel.toggleFlashcardMastered(cardId, isMastered)
                        },
                        onResetProgress = { viewModel.resetDeckProgress(deckId) }
                    )
                }
                DeckTab.Test -> {
                    PracticeTestTabContent(
                        questions = practiceQuestions,
                        onSelectOption = { questionId, option ->
                            viewModel.selectPracticeOption(questionId, option)
                        },
                        onResetProgress = { viewModel.resetDeckProgress(deckId) }
                    )
                }
                DeckTab.Quizzes -> {
                    QuizTabContent(
                        quizzes = quizQuestions,
                        onSelectAnswer = { quizId, answer ->
                            viewModel.answerQuizQuestion(quizId, answer)
                        },
                        onResetProgress = { viewModel.resetDeckProgress(deckId) }
                    )
                }
                DeckTab.AudioSummary -> {
                    AudioSummaryTabContent(
                        summary = currentDeck?.summary ?: "No AI generated audio summary text found.",
                        isTtsPlaying = isTtsPlaying,
                        onPlayClick = { text ->
                            viewModel.playAudioSummary(text)
                        },
                        onStopClick = {
                            viewModel.stopAudioSummary()
                        }
                    )
                }
                DeckTab.Notes -> {
                    NotesTabContent(materials = currentDeck?.materials ?: "No study notes loaded.")
                }
            }
        }
    }
}

@Composable
fun DeckTabButton(
    tab: DeckTab,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("tab_button_${tab.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ==================== STUDY WORKSPACE COMPONENT TABS ====================

// --- 1. FLASHCARDS TAB ---
@Composable
fun FlashcardsTabContent(
    flashcards: List<Flashcard>,
    onToggleMastery: (Int, Boolean) -> Unit,
    onResetProgress: () -> Unit
) {
    if (flashcards.isEmpty()) {
        EmptyTabState(msg = "No flashcards generated for this workspace yet.")
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    var showBackSide by remember { mutableStateOf(false) }

    // Mastery progress calculations
    val masteredCount = flashcards.count { it.isMastered }
    val progress = if (flashcards.isNotEmpty()) masteredCount.toFloat() / flashcards.size else 0f
    
    // Rotation animation
    val currentCard = flashcards.getOrNull(currentIndex) ?: flashcards[0]
    
    // Key ensures that when index changes, state of showing side resets
    LaunchedEffect(currentIndex) {
        showBackSide = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Deck Mastery Progress", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$masteredCount of ${flashcards.size} mastered", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(36.dp),
                    color = MasteryGreen,
                    strokeWidth = 3.dp
                )
            }
        }

        // Animated Swipe / Action Flashcard Item
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .graphicsLayer {
                    // Slight perspective
                    cameraDistance = 8 * density
                }
                .clickable { showBackSide = !showBackSide }
                .testTag("flashcard_render_box"),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (showBackSide) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (currentCard.isMastered) MasteryGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.08f
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (showBackSide) "ANSWER / MEANING" else "CONCEPT / KEY TERM",
                        fontWeight = FontWeight.Bold,
                        color = if (showBackSide) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = if (showBackSide) currentCard.back else currentCard.front,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(Alignment.CenterVertically),
                        lineHeight = 24.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Tap Card to Flip",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (currentCard.isMastered) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MasteryGreen.copy(alpha = 0.03f))
                )
            }
        }

        // Flashcard Control Board
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        if (currentIndex > 0) currentIndex--
                    },
                    enabled = currentIndex > 0,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .testTag("flashcard_prev_btn")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Prev")
                }

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentIndex + 1} of ${flashcards.size}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = {
                        if (currentIndex < flashcards.size - 1) currentIndex++
                    },
                    enabled = currentIndex < flashcards.size - 1,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .testTag("flashcard_next_btn")
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                }
            }

            // Quick mastery checker button
            OutlinedButton(
                onClick = {
                    onToggleMastery(currentCard.id, !currentCard.isMastered)
                },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("mastery_indicator_btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (currentCard.isMastered) MasteryGreen else MaterialTheme.colorScheme.outline
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (currentCard.isMastered) MasteryGreen.copy(alpha = 0.08f) else Color.Transparent
                )
            ) {
                Icon(
                    imageVector = if (currentCard.isMastered) Icons.Default.Check else Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = if (currentCard.isMastered) MasteryGreen else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (currentCard.isMastered) "Mastered" else "Mark Mastered",
                    fontSize = 13.sp,
                    color = if (currentCard.isMastered) MasteryGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        TextButton(
            onClick = onResetProgress,
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Deck Progress", fontSize = 13.sp)
        }
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}

// --- 2. PRACTICE MCQ TEST TAB ---
@Composable
fun PracticeTestTabContent(
    questions: List<PracticeQuestion>,
    onSelectOption: (Int, String) -> Unit,
    onResetProgress: () -> Unit
) {
    if (questions.isEmpty()) {
        EmptyTabState(msg = "No practice test generated for this workspace yet.")
        return
    }

    // Interactive Grading Dashboard
    val answeredCount = questions.count { it.userSelectedOption != null }
    val correctCount = questions.count { it.userSelectedOption == it.correctAnswer }
    val scorePercentage = if (answeredCount > 0) (correctCount.toFloat() / answeredCount * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High fidelity test stats panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Practice Exam Performance", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$correctCount answers correct ($answeredCount of ${questions.size} completed)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                
                if (answeredCount > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (scorePercentage >= 70) MasteryGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(
                                    alpha = 0.1f
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$scorePercentage% Grade",
                            color = if (scorePercentage >= 70) MasteryGreen else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // MCQ Questions rendering list
        questions.forEachIndexed { qIdx, question ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Question Header
                    Text(
                        text = "QUESTION ${qIdx + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = question.question,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val answered = question.userSelectedOption != null

                    // Option Selection Fields
                    MCQOptionRow(
                        label = "A",
                        text = question.optionA,
                        selected = question.userSelectedOption == "A",
                        correct = question.correctAnswer == "A",
                        answered = answered,
                        onClick = { if (!answered) onSelectOption(question.id, "A") }
                    )
                    
                    MCQOptionRow(
                        label = "B",
                        text = question.optionB,
                        selected = question.userSelectedOption == "B",
                        correct = question.correctAnswer == "B",
                        answered = answered,
                        onClick = { if (!answered) onSelectOption(question.id, "B") }
                    )
                    
                    MCQOptionRow(
                        label = "C",
                        text = question.optionC,
                        selected = question.userSelectedOption == "C",
                        correct = question.correctAnswer == "C",
                        answered = answered,
                        onClick = { if (!answered) onSelectOption(question.id, "C") }
                    )
                    
                    MCQOptionRow(
                        label = "D",
                        text = question.optionD,
                        selected = question.userSelectedOption == "D",
                        correct = question.correctAnswer == "D",
                        answered = answered,
                        onClick = { if (!answered) onSelectOption(question.id, "D") }
                    )

                    // Post response AI explanations
                    if (answered) {
                        val isCorrect = question.userSelectedOption == question.correctAnswer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) {
                                    MasteryGreen.copy(alpha = 0.06f)
                                } else {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.04f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (isCorrect) MasteryGreen else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isCorrect) "Correct Option Selected!" else "Incorrect Choice. Correct is Option ${question.correctAnswer}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCorrect) MasteryGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = question.explanation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = onResetProgress,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Test Answers", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
fun MCQOptionRow(
    label: String,
    text: String,
    selected: Boolean,
    correct: Boolean,
    answered: Boolean,
    onClick: () -> Unit
) {
    val borderStrokeCol = when {
        answered && correct -> MasteryGreen.copy(alpha = 0.6f)
        answered && selected && !correct -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    val cardColor = when {
        answered && correct -> MasteryGreen.copy(alpha = 0.04f)
        answered && selected && !correct -> MaterialTheme.colorScheme.error.copy(alpha = 0.02f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardColor)
            .border(BorderStroke(1.dp, borderStrokeCol), RoundedCornerShape(12.dp))
            .clickable(enabled = !answered) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when {
                        answered && correct -> MasteryGreen
                        answered && selected && !correct -> MaterialTheme.colorScheme.error
                        selected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = if (selected || (answered && correct)) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- 3. RAPID TRUE / FALSE QUIZ TAB ---
@Composable
fun QuizTabContent(
    quizzes: List<QuizQuestion>,
    onSelectAnswer: (Int, Boolean) -> Unit,
    onResetProgress: () -> Unit
) {
    if (quizzes.isEmpty()) {
        EmptyTabState(msg = "No quick quiz statements generated for this workspace yet.")
        return
    }

    val totalCount = quizzes.size
    val answeredCount = quizzes.count { it.userAnswer != null }
    val correctCount = quizzes.count { it.userAnswer == it.correctAnswer }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick statistics indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Quiz Review Stats", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$correctCount / $totalCount Statements Verified", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Score accuracy: " + (if (answeredCount > 0) (correctCount * 100 / answeredCount) else 0) + "%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Render each true/false card
        quizzes.forEachIndexed { qIdx, quiz ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "STATEMENT ${qIdx + 1}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = quiz.question,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    val answered = quiz.userAnswer != null

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // True Button
                        OutlinedButton(
                            onClick = { onSelectAnswer(quiz.id, true) },
                            enabled = !answered,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(
                                1.dp,
                                if (quiz.userAnswer == true) {
                                    if (quiz.correctAnswer) MasteryGreen else MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (quiz.userAnswer == true) {
                                    if (quiz.correctAnswer) MasteryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.04f
                                    )
                                } else Color.Transparent,
                                contentColor = if (quiz.userAnswer == true) {
                                    if (quiz.correctAnswer) MasteryGreen else MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("True", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // False Button
                        OutlinedButton(
                            onClick = { onSelectAnswer(quiz.id, false) },
                            enabled = !answered,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            border = BorderStroke(
                                1.dp,
                                if (quiz.userAnswer == false) {
                                    if (!quiz.correctAnswer) MasteryGreen else MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (quiz.userAnswer == false) {
                                    if (!quiz.correctAnswer) MasteryGreen.copy(alpha = 0.06f) else MaterialTheme.colorScheme.error.copy(
                                        alpha = 0.04f
                                    )
                                } else Color.Transparent,
                                contentColor = if (quiz.userAnswer == false) {
                                    if (!quiz.correctAnswer) MasteryGreen else MaterialTheme.colorScheme.error
                                } else MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Text("False", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Answer rationale feedback
                    if (answered) {
                        val isCorrect = quiz.userAnswer == quiz.correctAnswer
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCorrect) MasteryGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.error.copy(
                                    alpha = 0.03f
                                )
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (isCorrect) "Indeed Correct!" else "Incorrect Rationale.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorrect) MasteryGreen else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = quiz.explanation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        TextButton(
            onClick = onResetProgress,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset Quizzes Progress", fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// --- 4. AUDIO SUMMARY PLAYER TAB ---
@Composable
fun AudioSummaryTabContent(
    summary: String,
    isTtsPlaying: Boolean,
    onPlayClick: (String) -> Unit,
    onStopClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // High quality player controls pane
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Audio Player Icon with ambient pulse visual mockup
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isTtsPlaying) Icons.Default.Hearing else Icons.Default.Podcasts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AI Voice Narration Summary",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isTtsPlaying) "Voice synthesis is reading loud..." else "Playback is ready",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Wave equalizer layout mockup when active
                Row(
                    modifier = Modifier
                        .height(24.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stepsCount = 12
                    for (i in 0 until stepsCount) {
                        val barHeight = if (isTtsPlaying) {
                            // Dynamic wavy height mockup based on index
                            remember { (10..24).random() }
                        } else {
                            5
                        }
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isTtsPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.2f
                                    )
                                )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isTtsPlaying) {
                        Button(
                            onClick = { onPlayClick(summary) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .fillMaxWidth(0.8f)
                                .testTag("tts_play_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Play Voice Summary", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onStopClick,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .fillMaxWidth(0.8f)
                                .testTag("tts_pause_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop Audio")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Stop Playback", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Script transcription panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "VOICE TRANSCRIPT",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = summary,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))
    }
}

// --- 5. NOTES TEXT VIEW TAB ---
@Composable
fun NotesTabContent(materials: String) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "My Raw Study Materials",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = materials,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

// ==================== COMMON UTILITY UI VIEWS ====================

@Composable
fun EmptyTabState(msg: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = msg,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
