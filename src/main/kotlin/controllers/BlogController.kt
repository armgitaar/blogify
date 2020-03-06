package com.blog.blogify.controllers

import dev.alpas.http.HttpCall
import dev.alpas.make
import dev.alpas.routing.Controller
import com.blog.blogify.actions.Blogs
import com.blog.blogify.rules.blogData
import dev.alpas.orAbort
import dev.alpas.ozone.create
import dev.alpas.ozone.latest
import dev.alpas.validation.required
import me.liuwj.ktorm.dsl.count
import me.liuwj.ktorm.dsl.delete
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.drop
import me.liuwj.ktorm.entity.take
import me.liuwj.ktorm.entity.filter
import me.liuwj.ktorm.entity.map
import me.liuwj.ktorm.dsl.update
import me.liuwj.ktorm.entity.toList
import java.io.File
import java.io.InputStream
import com.blog.blogify.entities.Blogs as BlogData

class BlogController : Controller() {

    fun index(call: HttpCall) {
        val pagination = mainPagination(1)
        val blogs = getBlogs(0, 1)

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }

    fun pages(call: HttpCall) {
        val page = call.stringParam("page") ?: "/"
        val pagination = mainPagination(page.toLong())
        var previous: Int = 0
        println(pagination)

        if (pagination.previousUrl != null) {
            previous = pagination.previousUrl.toInt()
        }

        val blogs = getBlogs(previous, page.toInt())

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }


    fun show(call: HttpCall) {
        val pageId = call.stringParam("id") ?: "/"
        val blog = call.make<Blogs>()
        val id: Long = pageId.toLong()
        val blogData = getBlog(id)
        val blogId = blogData[0].first
        val markBlog = blogData[0].second
        var (markData, title, author, date, image, tags) = blog.readSource(markBlog!!)
        var content = blog.convert(markData)
        val blogs = BlogData.latest().map { it.id }.toList()
        val pagination = pagination(blogs, id)

        call.render("blog", mapOf("blogId" to blogId, "markBlog" to markBlog, "content" to content, "title" to title,
            "author" to author, "date" to date, "image" to image, "tags" to tags, "pagination" to pagination))
    }

    fun edit(call: HttpCall) {

        val id = call.longParam("id").orAbort()
        val blogData = getBlog(id)
        val blogId = blogData[0].first
        val content = blogData[0].second

        call.render("editblog", mapOf("content" to content, "blogId" to blogId))
    }

    fun update(call: HttpCall) {
        val id = call.longParam("id").orAbort()
        BlogData.update {
            it.content to call.stringParam("content")
            where {
                it.id eq id
            }
            call.redirect().back()
        }

        flash("success", "Successfully Saved")

    }

    fun new(call: HttpCall) {
        val inputStream: InputStream = File("src/main/resources/templates/marked.peb").inputStream()
        val placeholder = inputStream.bufferedReader().use { it.readText() }
        call.render("new", mapOf("placeholder" to placeholder))
    }

    fun submit(call: HttpCall) {
        val content = call.stringParam(("content"))
        val blog = call.make<Blogs>()
        var (markData, title, author, date, image, tags) = blog.readSource(content!!)

        call.applyRules("content") {
            required()
            blogData(markData, title, author, date, image, tags)
        }.validate()

        BlogData.create() {
            it.content to content
        }

        call.redirect().toRouteNamed("welcome")
        flash("success", "Successfully posted")
    }

    fun delete(call: HttpCall) {
        val id = call.longParam("id").orAbort()
        println(id)
        BlogData.delete { it.id eq id }

        call.redirect().toRouteNamed("welcome")
        flash("success", "Successfully removed post")
    }

    private fun getBlog(id: Long): List<Pair<Long, String?>> {
        return BlogData.latest().filter { it.id eq id }.map { Pair(it.id, it.content) }
    }

    private fun getBlogs(previousPage: Int, currentPage: Int): MutableList<Any> {
        var previous = previousPage * 9
        var current = previous + 9

        //TODO - figure out why more than 9 results appear on page 2 when there are 19+ blog post entries...

        var blogs = BlogData.latest().drop(previous).take(current).toList()

        var blogsList = mutableListOf<Any>()

        for (x in blogs) {
            val blog = call.make<Blogs>()
            val id = x.id
            val content = x.content
            var (markData, title, author, date, image, tags) = blog.readSource(content!!)

            val re = Regex("#*")
            markData = re.replace(markData, "")

            val urlPattern = Regex(" ")
            val urlTitle = urlPattern.replace(title, "-")

            markData = markData.substring(0, Math.min(markData.length, 100)) + " ..."
            val list = mutableListOf<Any>(id, markData, title, author, date, image, tags, urlTitle)
            blogsList.add(list)
        }

        return blogsList
    }

}

private fun <E> MutableList<E>.subList(id: E, markDate: E, title: E, author: E, date: E, image: E, tags: E) {

}


private fun mainPagination(currentPage: Long): Pagination {
    val numPages = Math.ceil(BlogData.count().toDouble() / 9)
    println(numPages)

    var pages = mutableListOf<Long>()
    var stopper = 1
    while (numPages > stopper - 1) {
        pages.add(stopper.toLong())
        stopper = stopper + 1
    }

    return pagination(pages, currentPage)
}


private fun pagination(blogs: List<Long>, currentPage: Long): Pagination {
    val index = blogs.indexOf(currentPage)
    val previousUrl = if (index < 1) null else blogs[index - 1].toString()
    val nextUrl = if (index >= blogs.size - 1) null else blogs[index + 1].toString()
    return Pagination(previousUrl, nextUrl)
}

data class Pagination(val previousUrl: String?, val nextUrl: String?) {
    fun hasPrevious(): Boolean {
        return previousUrl != null
    }

    fun hasNext(): Boolean {
        return nextUrl != null
    }
}

