package com.blog.blogify.controllers

import dev.alpas.http.HttpCall
import dev.alpas.make
import dev.alpas.routing.Controller
import com.blog.blogify.actions.Blogs
import dev.alpas.orAbort
import dev.alpas.ozone.create
import dev.alpas.ozone.latest
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.entity.take
import me.liuwj.ktorm.entity.filter
import me.liuwj.ktorm.entity.map
import me.liuwj.ktorm.dsl.update
import me.liuwj.ktorm.entity.toList
import com.blog.blogify.entities.Blogs as BlogData

class BlogController : Controller() {

    fun index(call: HttpCall) {
        var blogsList = mutableListOf<Any>()
        val blogs = BlogData.latest().take(10).toList()
        for (x in blogs) {
            val blog = call.make<Blogs>()
            val id = x.id
            val content = x.content
            var (markData, title, author, date, image, tags) = blog.readSource(content!!)

            val re = Regex("#*")
            markData = re.replace(markData, "")

            markData = markData.substring(0, Math.min(markData.length, 100)) + " ..."
            val list = mutableListOf<Any>(id, markData, title, author, date, image, tags)
            blogsList.add(list)
        }
        call.render("welcome", mapOf("blogs" to blogsList))

    }

    fun show(call: HttpCall) {
        val page = call.stringParam("page") ?: "/"
        val blog = call.make<Blogs>()
        val id: Long = page.toLong()
        val blogData = getBlog(id)
        val blogId = blogData[0].first
        val markBlog = blogData[0].second
        var (markData, title, author, date, image, tags) = blog.readSource(markBlog!!)
        var content = blog.convert(markData)
        println(content)

        call.render("blog", mapOf("blogId" to blogId, "markBlog" to markBlog, "content" to content, "title" to title, "page" to page,
            "author" to author, "date" to date, "image" to image, "tags" to tags))
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
        call.render("new")
    }

    fun submit(call: HttpCall) {
        BlogData.create() {
            it.content to call.stringParam("content")
        }

        //flash("success", "Successfully added to-do")
        call.redirect().toRouteNamed("welcome")
        //call.render("new")
    }

    private fun getBlog(id: Long): List<Pair<Long, String?>> {
        return BlogData.latest().filter { it.id eq id }.map { Pair(it.id, it.content) }
    }

}

private fun <E> MutableList<E>.subList(id: E, markDate: E, title: E, author: E, date: E, image: E, tags: E) {

}


