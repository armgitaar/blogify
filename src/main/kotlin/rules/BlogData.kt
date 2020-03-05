package com.blog.blogify.rules

import dev.alpas.validation.ErrorMessage
import dev.alpas.validation.Rule
import dev.alpas.validation.ValidationGuard

class BlogData(private val markData: String, private val title: String, private val author: String, private val date: String, private val image: String, private val tags: String, private val message: ErrorMessage = null) : Rule() {
    override fun check(attribute: String, value: Any?): Boolean {

        val isValid = markData.isNotEmpty() && title.isNotEmpty() && author.isNotEmpty() && date.isNotEmpty() && image.isNotEmpty() && tags.isNotEmpty()

        if (!isValid) {

            error = message?.let { it(attribute, value) } ?: "Submission failed - be sure to fill in all metadata and blog content."
        }
        return isValid
    }
}

fun ValidationGuard.blogData(markData: String, title: String, author: String, date: String, image: String, tags: String, message: ErrorMessage = null): Rule {
    return rule(BlogData(markData, title, author, date, image, tags, message))
}