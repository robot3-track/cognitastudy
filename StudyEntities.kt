package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "study_decks")
data class StudyDeck(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val materials: String,
    val summary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val front: String,
    val back: String,
    val isMastered: Boolean = false
)

@Entity(
    tableName = "practice_questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PracticeQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", or "D"
    val explanation: String,
    val userSelectedOption: String? = null
)

@Entity(
    tableName = "quiz_questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyDeck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deckId: Int,
    val question: String,
    val correctAnswer: Boolean, // true or false
    val explanation: String,
    val userAnswer: Boolean? = null
)
