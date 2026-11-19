package com.example.data.repository

import java.util.Calendar

data class MotivationalQuote(
    val text: String,
    val author: String
)

object QuoteProvider {
    private val quotes = listOf(
        MotivationalQuote("Small daily disciplines compound into extraordinary calm and mastery.", "Robin Sharma"),
        MotivationalQuote("You do not rise to the level of your goals; you fall to the level of your systems.", "James Clear"),
        MotivationalQuote("Focus is saying no to a hundred good ideas so the vital one can thrive.", "Steve Jobs"),
        MotivationalQuote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        MotivationalQuote("Amateurs sit and wait for inspiration; the rest of us just get up and go to work.", "Stephen King"),
        MotivationalQuote("How we spend our days is, of course, how we spend our lives.", "Annie Dillard"),
        MotivationalQuote("Consistency outperforms intensity when intensity only lasts a week.", "Unknown"),
        MotivationalQuote("Simplicity is about subtracting the obvious and adding the meaningful.", "John Maeda"),
        MotivationalQuote("A year from now you will wish you had started today.", "Karen Lamb"),
        MotivationalQuote("Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln"),
        MotivationalQuote("The secret of getting ahead is getting started.", "Mark Twain"),
        MotivationalQuote("Well begun is half done.", "Aristotle"),
        MotivationalQuote("Protect your morning hours; they set the rhythm for the entire day.", "Cal Newport"),
        MotivationalQuote("Rest is not idleness; it is the quiet fuel of sustainable focus.", "John Lubbock"),
        MotivationalQuote("Progress, not perfection, is what turns intentions into habits.", "Unknown"),
        MotivationalQuote("Clarity precedes mastery. Write the plan before you chase the clock.", "Robin Sharma")
    )

    fun quoteOfTheDay(offset: Int = 0): MotivationalQuote {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = Math.floorMod(dayOfYear + offset, quotes.size)
        return quotes[index]
    }
}
