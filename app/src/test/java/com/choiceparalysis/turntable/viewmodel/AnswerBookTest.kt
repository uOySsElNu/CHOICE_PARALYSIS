package com.choiceparalysis.turntable.viewmodel

import com.choiceparalysis.turntable.R
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerBookTest {

    // Answer pools for each scene (must match AnswerBook)
    private val foodAnswers = setOf(R.string.answer_food_1, R.string.answer_food_2, R.string.answer_food_3, R.string.answer_food_4)
    private val outingAnswers = setOf(R.string.answer_outing_1, R.string.answer_outing_2, R.string.answer_outing_3, R.string.answer_outing_4)
    private val shoppingAnswers = setOf(R.string.answer_shopping_1, R.string.answer_shopping_2, R.string.answer_shopping_3, R.string.answer_shopping_4)
    private val workAnswers = setOf(R.string.answer_work_1, R.string.answer_work_2, R.string.answer_work_3, R.string.answer_work_4)
    private val loveAnswers = setOf(R.string.answer_love_1, R.string.answer_love_2, R.string.answer_love_3, R.string.answer_love_4)
    private val generalAnswers = setOf(
        R.string.answer_general_1, R.string.answer_general_2, R.string.answer_general_3,
        R.string.answer_general_4, R.string.answer_general_5, R.string.answer_general_6,
        R.string.answer_general_7, R.string.answer_general_8, R.string.answer_general_9,
        R.string.answer_general_10,
    )
    private val allAnswers = foodAnswers + outingAnswers + shoppingAnswers + workAnswers + loveAnswers + generalAnswers

    @Test
    fun `blank question returns from general pool`() {
        val resId = AnswerBook.getAnswer("")
        assertTrue("Expected general pool answer, got $resId", resId in generalAnswers)
    }

    @Test
    fun `food keyword in chinese matches food scene`() {
        val resId = AnswerBook.getAnswer("今天吃什么好呢")
        assertTrue("Expected food answer, got $resId", resId in foodAnswers)
    }

    @Test
    fun `food keyword in english matches food scene`() {
        val resId = AnswerBook.getAnswer("What should I eat for lunch?")
        assertTrue("Expected food answer, got $resId", resId in foodAnswers)
    }

    @Test
    fun `travel keyword matches outing scene`() {
        val resId = AnswerBook.getAnswer("周末去哪里玩")
        assertTrue("Expected outing answer, got $resId", resId in outingAnswers)
    }

    @Test
    fun `shopping keyword matches shopping scene`() {
        val resId = AnswerBook.getAnswer("要不要买这个手机")
        assertTrue("Expected shopping answer, got $resId", resId in shoppingAnswers)
    }

    @Test
    fun `work keyword matches work scene`() {
        val resId = AnswerBook.getAnswer("该不该辞职")
        assertTrue("Expected work answer, got $resId", resId in workAnswers)
    }

    @Test
    fun `love keyword matches love scene`() {
        val resId = AnswerBook.getAnswer("要不要表白")
        assertTrue("Expected love answer, got $resId", resId in loveAnswers)
    }

    @Test
    fun `unrelated question returns general answer`() {
        // "今天天气真好" contains no scene keywords — should fall through to general pool
        val resId = AnswerBook.getAnswer("今天天气真好")
        assertTrue("Expected general answer, got $resId", resId in generalAnswers)
    }

    @Test
    fun `keyword matching is case insensitive`() {
        val resId = AnswerBook.getAnswer("WHAT SHOULD I EAT")
        assertTrue("Expected food answer for uppercase input, got $resId", resId in foodAnswers)
    }

    @Test
    fun `all returned ids are valid string resources`() {
        val questions = listOf("", "吃什么", "去哪里玩", "买手机", "辞职", "表白", "天气怎么样")
        questions.forEach { q ->
            val resId = AnswerBook.getAnswer(q)
            assertTrue("Answer for '$q' should be a valid resource", resId in allAnswers)
        }
    }

    @Test
    fun `multiple calls can return different answers`() {
        val answers = (1..20).map { AnswerBook.getAnswer("今天吃什么") }.toSet()
        assertTrue("Expected some variety in food answers, got ${answers.size} unique", answers.size > 1)
        assertTrue("All answers should be from food pool", answers.all { it in foodAnswers })
    }

    @Test
    fun `keyword at start of question matches`() {
        val resId = AnswerBook.getAnswer("吃什么呢")
        assertTrue("Expected food answer, got $resId", resId in foodAnswers)
    }

    @Test
    fun `keyword at end of question matches`() {
        val resId = AnswerBook.getAnswer("今天中午吃")
        assertTrue("Expected food answer, got $resId", resId in foodAnswers)
    }
}
