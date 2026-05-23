package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    
    // --- Study Decks ---
    @Query("SELECT * FROM study_decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<StudyDeck>>

    @Query("SELECT * FROM study_decks WHERE id = :deckId LIMIT 1")
    suspend fun getDeckById(deckId: Int): StudyDeck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: StudyDeck): Long

    @Delete
    suspend fun deleteDeck(deck: StudyDeck)

    @Query("DELETE FROM study_decks WHERE id = :deckId")
    suspend fun deleteDeckById(deckId: Int)

    // --- Flashcards ---
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY id ASC")
    fun getFlashcardsByDeck(deckId: Int): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Query("UPDATE flashcards SET isMastered = :isMastered WHERE id = :id")
    suspend fun updateFlashcardMastery(id: Int, isMastered: Boolean)

    // --- Practice Questions ---
    @Query("SELECT * FROM practice_questions WHERE deckId = :deckId ORDER BY id ASC")
    fun getPracticeQuestionsByDeck(deckId: Int): Flow<List<PracticeQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeQuestions(questions: List<PracticeQuestion>)

    @Update
    suspend fun updatePracticeQuestion(question: PracticeQuestion)

    @Query("UPDATE practice_questions SET userSelectedOption = :option WHERE id = :id")
    suspend fun updateSelectedOption(id: Int, option: String?)

    // --- Quiz Questions ---
    @Query("SELECT * FROM quiz_questions WHERE deckId = :deckId ORDER BY id ASC")
    fun getQuizQuestionsByDeck(deckId: Int): Flow<List<QuizQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(quizzes: List<QuizQuestion>)

    @Update
    suspend fun updateQuizQuestion(quiz: QuizQuestion)

    @Query("UPDATE quiz_questions SET userAnswer = :answer WHERE id = :id")
    suspend fun updateQuizAnswer(id: Int, answer: Boolean?)

    // --- Reset Status ---
    @Query("UPDATE flashcards SET isMastered = 0 WHERE deckId = :deckId")
    suspend fun resetDeckFlashcards(deckId: Int)

    @Query("UPDATE practice_questions SET userSelectedOption = NULL WHERE deckId = :deckId")
    suspend fun resetDeckPractice(deckId: Int)

    @Query("UPDATE quiz_questions SET userAnswer = NULL WHERE deckId = :deckId")
    suspend fun resetDeckQuizzes(deckId: Int)
}
