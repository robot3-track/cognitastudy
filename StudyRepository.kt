package com.example.data.repository

import com.example.data.dao.StudyDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {

    val allDecks: Flow<List<StudyDeck>> = studyDao.getAllDecks()

    suspend fun getDeckById(deckId: Int): StudyDeck? {
        return studyDao.getDeckById(deckId)
    }

    suspend fun insertDeck(deck: StudyDeck): Long {
        return studyDao.insertDeck(deck)
    }

    suspend fun deleteDeck(deck: StudyDeck) {
        studyDao.deleteDeck(deck)
    }

    suspend fun deleteDeckById(deckId: Int) {
        studyDao.deleteDeckById(deckId)
    }

    fun getFlashcards(deckId: Int): Flow<List<Flashcard>> {
        return studyDao.getFlashcardsByDeck(deckId)
    }

    suspend fun insertFlashcards(flashcards: List<Flashcard>) {
        studyDao.insertFlashcards(flashcards)
    }

    suspend fun updateFlashcardMastery(id: Int, isMastered: Boolean) {
        studyDao.updateFlashcardMastery(id, isMastered)
    }

    fun getPracticeQuestions(deckId: Int): Flow<List<PracticeQuestion>> {
        return studyDao.getPracticeQuestionsByDeck(deckId)
    }

    suspend fun insertPracticeQuestions(questions: List<PracticeQuestion>) {
        studyDao.insertPracticeQuestions(questions)
    }

    suspend fun updateSelectedOption(id: Int, option: String?) {
        studyDao.updateSelectedOption(id, option)
    }

    fun getQuizQuestions(deckId: Int): Flow<List<QuizQuestion>> {
        return studyDao.getQuizQuestionsByDeck(deckId)
    }

    suspend fun insertQuizQuestions(quizzes: List<QuizQuestion>) {
        studyDao.insertQuizQuestions(quizzes)
    }

    suspend fun updateQuizAnswer(id: Int, answer: Boolean?) {
        studyDao.updateQuizAnswer(id, answer)
    }

    suspend fun resetDeckData(deckId: Int) {
        studyDao.resetDeckFlashcards(deckId)
        studyDao.resetDeckPractice(deckId)
        studyDao.resetDeckQuizzes(deckId)
    }
}
