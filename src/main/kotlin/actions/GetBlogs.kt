package com.blog.blogify.actions

import dev.alpas.http.HttpCall
import dev.alpas.ozone.latest
import dev.alpas.make
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.*

// This function gets a blog post's data using the ID that is passed to it
fun getBlog(id: Long): List<Pair<Long, String?>> {
    return com.blog.blogify.entities.Blogs.latest().filter { it.id eq id }.map { Pair(it.id, it.content) }
}

// This function is used for paging at the top level.
// 9 posts are displayed per page. It works by ignoring blog post entries up to the previous page * 9
// and the pulls the following 9 results to display on the page
fun getBlogs(currentPage: Int, call: HttpCall): MutableList<Any> {

    var drop = (currentPage - 1) * 9
    var blogs = com.blog.blogify.entities.Blogs.latest().drop(drop).take(9).toList()
    var blogsList = mutableListOf<Any>()

    // This portion loops through the results and extracts information from meta data to display; such as,
    // title, author, tags, data, and first 100 characters of blog content
    for (x in blogs) {
        val blog = call.make<ParseBlog>()
        val id = x.id
        val content = x.content
        var (markData, title, author, date, image, tags) = blog.readSource(content!!)

        // Since we aren't doing a full HTML conversion at this juncture, we just create a regex to search for
        // markdown characters that we do not want to display on the GUI
        val re = Regex("#*")
        markData = re.replace(markData, "")

        // This part creates a url friendly title to display in the url
        val urlPattern = Regex(" ")
        val urlTitle = urlPattern.replace(title, "-")

        // This part truncates the post content to 100 characters and then adds elipsis
        markData = markData.substring(0, markData.length.coerceAtMost(100)) + " ..."

        val list = mutableListOf<Any>(id, markData, title, author, date, image, tags, urlTitle)
        blogsList.add(list)
    }

    return blogsList
}