package com.example.network

import android.util.Log
import com.example.data.model.Flashcard
import com.example.data.model.PracticeQuestion
import com.example.data.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

object StudyGenerator {
    private const val TAG = "StudyGenerator"

    suspend fun generateFlashcards(deckId: Int, materialsText: String): List<Flashcard> {
        val systemInstruction = "You are an expert tutor creating study materials. You must output ONLY a valid JSON array of flashcards based on the provided material. Do not write any conversational text before or after the JSON."
        val prompt = """
            Analyze the following study materials and generate a JSON array of flashcards (aim for 5-10 high-quality cards). 
            Each flashcard must have a 'front' (the question, concept, or term to review) and 'back' (the detail, explanation, or definition). Make them educational, high-density, and clear.
            
            Study Materials:
            $materialsText
            
            Return ONLY a JSON array with this exact schema:
            [
              {
                "front": "What is the mitochondria?",
                "back": "Commonly known as the powerhouse of the cell, it generates chemical energy in the form of ATP."
              }
            ]
        """.trimIndent()

        val rawResponse = GeminiService.generateContent(prompt, systemInstruction, "application/json") ?: return emptyList()
        val cleanJson = GeminiService.cleanJsonBlock(rawResponse)
        
        val list = mutableListOf<Flashcard>()
        try {
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    Flashcard(
                        deckId = deckId,
                        front = obj.optString("front", "Concept ${i + 1}"),
                        back = obj.optString("back", "Definition of concept"),
                        isMastered = false
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing flashcards: ${e.message}", e)
            Log.d(TAG, "Raw cleanJson: $cleanJson")
        }
        return list
    }

    suspend fun generatePracticeQuestions(deckId: Int, materialsText: String): List<PracticeQuestion> {
        val systemInstruction = "You are an expert test builder. You must output ONLY a valid JSON array of multiple-choice questions based on the provided material. Do not write any conversational text."
        val prompt = """
            Analyze the following study materials and generate a JSON array of 4-6 custom multiple-choice practice questions.
            Each question must have exactly 4 selectable options labeled optionA, optionB, optionC, optionD, a 'correctAnswer' (which must be exactly "A", "B", "C", or "D"), and a helpful 'explanation' describing why that option is correct.
            
            Study Materials:
            $materialsText
            
            Return ONLY a JSON array with this exact schema:
            [
              {
                "question": "Which organelle is responsible for cellular respiration?",
                "optionA": "Ribosome",
                "optionB": "Mitochondria",
                "optionC": "Lysosome",
                "optionD": "Golgi apparatus",
                "correctAnswer": "B",
                "explanation": "Mitochondria convert glucose into ATP, which provides energy for cell functions."
              }
            ]
        """.trimIndent()

        val rawResponse = GeminiService.generateContent(prompt, systemInstruction, "application/json") ?: return emptyList()
        val cleanJson = GeminiService.cleanJsonBlock(rawResponse)
        
        val list = mutableListOf<PracticeQuestion>()
        try {
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PracticeQuestion(
                        deckId = deckId,
                        question = obj.optString("question", "Sample question?"),
                        optionA = obj.optString("optionA", "Option A"),
                        optionB = obj.optString("optionB", "Option B"),
                        optionC = obj.optString("optionC", "Option C"),
                        optionD = obj.optString("optionD", "Option D"),
                        correctAnswer = obj.optString("correctAnswer", "A").trim().uppercase(),
                        explanation = obj.optString("explanation", "Choice explanation."),
                        userSelectedOption = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing practice questions: ${e.message}", e)
        }
        return list
    }

    suspend fun generateQuizQuestions(deckId: Int, materialsText: String): List<QuizQuestion> {
        val systemInstruction = "You are an expert examiner. You must output ONLY a valid JSON array of True or False statements based on the provided material. Do not write conversational text."
        val prompt = """
            Analyze the following study materials and generate a JSON array of 5-8 True/False quick quiz statements.
            Each statement must contain a clear declarative 'question', a 'correctAnswer' boolean (true or false), and a short 'explanation' explaining the fact.
            
            Study Materials:
            $materialsText
            
            Return ONLY a JSON array with this exact schema:
            [
              {
                "question": "Mitochondria contain their own ribosomes and DNA.",
                "correctAnswer": true,
                "explanation": "In addition to chromosomal DNA, mitochondria contain their own hereditary genome which is circular."
              }
            ]
        """.trimIndent()

        val rawResponse = GeminiService.generateContent(prompt, systemInstruction, "application/json") ?: return emptyList()
        val cleanJson = GeminiService.cleanJsonBlock(rawResponse)
        
        val list = mutableListOf<QuizQuestion>()
        try {
            val jsonArray = JSONArray(cleanJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    QuizQuestion(
                        deckId = deckId,
                        question = obj.optString("question", "Sample statement"),
                        correctAnswer = obj.optBoolean("correctAnswer", true),
                        explanation = obj.optString("explanation", "Verification explanation."),
                        userAnswer = null
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing quiz questions: ${e.message}", e)
        }
        return list
    }

    suspend fun generateSummary(materialsText: String): String {
        val systemInstruction = "You are an expert narrator. Summarize study materials concisely and clearly."
        val prompt = """
            Create a rich, conversational summary of the following study materials. It will be used as a spoken summary so make sure it reads easily, covers the central concepts, definitions, and theories, and remains fluid. Length should be about 2-3 brief paragraphs.
            
            Study Materials:
            $materialsText
        """.trimIndent()

        return GeminiService.generateContent(prompt, systemInstruction) ?: "No summary generated."
    }
}
