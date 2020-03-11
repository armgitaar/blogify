package com.blog.blogify.rules

import com.blog.blogify.actions.ParseBlog
import com.blog.blogify.entities.Blog
import dev.alpas.http.HttpCall
import dev.alpas.make
import dev.alpas.validation.ErrorMessage
import dev.alpas.validation.Rule
import dev.alpas.validation.ValidationGuard

class BlogValidate(private val message: ErrorMessage = null) : Rule() {

    override fun check(attribute: String, call: HttpCall): Boolean {

        val content = call.stringParam(("content"))
        val parseBlog = call.make<ParseBlog>()
        var (markData, title, author, date, image, tags) = parseBlog.readSource(content.toString())

        val isValid = markData.isNotEmpty() && title.isNotEmpty() && author.isNotEmpty() && date.isNotEmpty() && image.isNotEmpty() && tags.isNotEmpty()

        if (!isValid) {
            error = message?.let { it(attribute, call) } ?: "Submission failed - be sure to fill in all metadata and blog content."
        }

        return isValid
    }
}

fun ValidationGuard.blogValidate(message: ErrorMessage = null): Rule {
    return rule(BlogValidate(message))
}