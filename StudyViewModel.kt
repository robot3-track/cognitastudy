package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.*
import com.example.data.repository.StudyRepository
import com.example.network.StudyGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed interface GenerationState {
    object Idle : GenerationState
    data class Loading(val stage: String) : GenerationState
    data class Success(val deckId: Int) : GenerationState
    data class Error(val message: String) : GenerationState
}

class StudyViewModel(
    application: Application,
    private val repository: StudyRepository
) : AndroidViewModel(application) {

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _selectedDeckId = MutableStateFlow<Int?>(null)
    val selectedDeckId: StateFlow<Int?> = _selectedDeckId.asStateFlow()

    // Observe all decks
    val decks: StateFlow<List<StudyDeck>> = repository.allDecks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Reactive streams for the selected deck contents
    val currentDeck: StateFlow<StudyDeck?> = _selectedDeckId
        .flatMapLatest { id ->
            if (id != null) {
                flow { emit(repository.getDeckById(id)) }
            } else {
                flowOf<StudyDeck?>(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentFlashcards: StateFlow<List<Flashcard>> = _selectedDeckId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getFlashcards(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentPracticeQuestions: StateFlow<List<PracticeQuestion>> = _selectedDeckId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getPracticeQuestions(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentQuizQuestions: StateFlow<List<QuizQuestion>> = _selectedDeckId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getQuizQuestions(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Native Text-to-Speech support
    private var tts: TextToSpeech? = null
    private val _isTtsPlaying = MutableStateFlow(false)
    val isTtsPlaying: StateFlow<Boolean> = _isTtsPlaying.asStateFlow()

    init {
        tts = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            } else {
                Log.e("StudyViewModel", "TTS Initialization failed!")
            }
        }
    }

    fun selectDeck(deckId: Int?) {
        _selectedDeckId.value = deckId
        stopAudioSummary()
    }

    fun setGenerationIdle() {
        _generationState.value = GenerationState.Idle
    }

    /**
     * Orchestrates the AI generation process.
     * Generates a study deck record, then sequentially makes individual API calls
     * to populate flashcards, test, quiz, and summary.
     */
    fun createStudyDeck(title: String, materials: String) {
        viewModelScope.launch {
            _generationState.value = GenerationState.Loading("Initializing study deck structure...")
            try {
                // Step 1: Insert core study deck
                val deck = StudyDeck(title = title, materials = materials)
                val deckId = repository.insertDeck(deck).toInt()

                // Step 2: Generate Flashcards
                _generationState.value = GenerationState.Loading("AI is crafting interactive flashcards...")
                val flashcards = StudyGenerator.generateFlashcards(deckId, materials)
                if (flashcards.isNotEmpty()) {
                    repository.insertFlashcards(flashcards)
                }

                // Step 3: Generate Practice Test
                _generationState.value = GenerationState.Loading("AI is configuring multiple-choice tests...")
                val practiceQuestions = StudyGenerator.generatePracticeQuestions(deckId, materials)
                if (practiceQuestions.isNotEmpty()) {
                    repository.insertPracticeQuestions(practiceQuestions)
                }

                // Step 4: Generate Quick Quizzes
                _generationState.value = GenerationState.Loading("AI is parsing rapid true/false quizzes...")
                val quizQuestions = StudyGenerator.generateQuizQuestions(deckId, materials)
                if (quizQuestions.isNotEmpty()) {
                    repository.insertQuizQuestions(quizQuestions)
                }

                // Step 5: Generate spoken Audio Summary transcription
                _generationState.value = GenerationState.Loading("AI is synthesizing audio voice summary...")
                val summary = StudyGenerator.generateSummary(materials)
                
                // Save fully processed deck package
                val updatedDeck = StudyDeck(id = deckId, title = title, materials = materials, summary = summary)
                repository.insertDeck(updatedDeck)

                _generationState.value = GenerationState.Success(deckId)
                _selectedDeckId.value = deckId
            } catch (e: Exception) {
                _generationState.value = GenerationState.Error(e.message ?: "Failed to generate study companion package.")
            }
        }
    }

    fun deleteDeck(deckId: Int) {
        viewModelScope.launch {
            repository.deleteDeckById(deckId)
            if (_selectedDeckId.value == deckId) {
                _selectedDeckId.value = null
                stopAudioSummary()
            }
        }
    }

    // --- Interactive student responses ---

    fun toggleFlashcardMastered(id: Int, isMastered: Boolean) {
        viewModelScope.launch {
            repository.updateFlashcardMastery(id, isMastered)
        }
    }

    fun selectPracticeOption(id: Int, option: String) {
        viewModelScope.launch {
            repository.updateSelectedOption(id, option)
        }
    }

    fun answerQuizQuestion(id: Int, answer: Boolean) {
        viewModelScope.launch {
            repository.updateQuizAnswer(id, answer)
        }
    }

    fun resetDeckProgress(deckId: Int) {
        viewModelScope.launch {
            repository.resetDeckData(deckId)
            stopAudioSummary()
        }
    }

    // --- Audio Summary Player ---

    fun playAudioSummary(text: String) {
        if (text.isEmpty()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "SummaryUtteranceID")
        _isTtsPlaying.value = true
        
        // Listen to spoken progress to track playing state
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isTtsPlaying.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isTtsPlaying.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isTtsPlaying.value = false
            }
        })
    }

    fun stopAudioSummary() {
        tts?.stop()
        _isTtsPlaying.value = false
    }

    override fun onCleared() {
        super.onCleared()
        tts?.shutdown()
    }
}

class StudyViewModelFactory(
    private val application: Application,
    private val repository: StudyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
