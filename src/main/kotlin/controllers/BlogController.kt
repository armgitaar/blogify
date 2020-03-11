package com.blog.blogify.controllers

import dev.alpas.http.HttpCall
import dev.alpas.make
import dev.alpas.routing.Controller
import dev.alpas.orAbort
import dev.alpas.ozone.create
import dev.alpas.validation.required
import com.blog.blogify.actions.*
import com.blog.blogify.rules.*
import me.liuwj.ktorm.dsl.delete
import me.liuwj.ktorm.dsl.eq
import me.liuwj.ktorm.dsl.update
import java.io.File
import java.io.InputStream
import com.blog.blogify.entities.Blogs as BlogData

class BlogController : Controller() {

    // Index is the main landing page for the blog
    fun index(call: HttpCall) {

        // Pagination calls the main pagination function to see if there are more than 9 posts, if so, this will create
        // the need for top level paging too. This blog site will display 9 results per page.
        val pagination = mainPagination(1)
        val blogs = getBlogs(1, call)

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }

    // Since there will likely be multiple blog posts, we will incorporate ability to page between groups of results.
    // This route is used when there are more than 9 blog posts to necessitate navigation between multiple pages.
    fun pages(call: HttpCall) {
        val currentPage = call.stringParam("page")!!.toInt()
        val pagination = mainPagination(currentPage)
        val blogs = getBlogs(currentPage, call)

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }

    // The show function is essentially the blog post's dedicated page. We reference the page ID from the URL to determine which
    // post to pull from the database.
    fun show(call: HttpCall) {

        val pageId = call.stringParam("id")!!.toLong()
        val blog = call.make<ParseBlog>()
        val blogData = getBlog(pageId)
        val markBlog = blogData[0].second

        var (markData, title, author, date, image, tags) = blog.readSource(markBlog!!)
        var content = blog.convert(markData)
        val pagination = pagination(pageId.toInt())

        call.render("blog", mapOf("blogId" to pageId, "content" to content, "title" to title,
            "author" to author, "date" to date, "image" to image, "tags" to tags, "pagination" to pagination))
    }

    // This function will pull new post placeholder text from the marked.peb file and add to the new blog post
    // creation screen
    fun new(call: HttpCall) {
        val inputStream: InputStream = File("src/main/resources/templates/marked.peb").inputStream()
        val placeholder = inputStream.bufferedReader().use { it.readText() }
        call.render("new", mapOf("placeholder" to placeholder))
    }

    // Once the user has completed writing the content for their blog, this function will check to see if the meta data
    // content pass validation. If passes, then it will write the new blog post to the database.
    fun submit(call: HttpCall) {

        call.applyRules("content") {
            required()
            blogValidate()
        }.validate()

        var content = call.stringParam("content")

        // I have private val blogValContent by lazy { BlogValidate() } list before the function; the blogValidation
        // has the 'content' variable and saving this.content appears to save content. But when printing it shows as null
        BlogData.create() {
            it.content to content
        }

        call.redirect().toRouteNamed("welcome")
        flash("success", "Successfully posted")
    }

    // If user selects option to edit a blog post, they will be routed to an edit page where they can edit the post's
    // markdown.
    fun edit(call: HttpCall) {

        val id = call.longParam("id").orAbort()
        val blogData = getBlog(id)
        val blogId = blogData[0].first
        val content = blogData[0].second

        call.render("editblog", mapOf("content" to content, "blogId" to blogId))
    }

    // Once user has completed their edits, this function first checks to see if the updated content passes validation
    //  and if so, will update the blog post's entry in the database
    fun update(call: HttpCall) {
        val id = call.longParam("id").orAbort()

        call.applyRules("content") {
            required()
            blogValidate()
        }.validate()

        BlogData.update {
            it.content to call.stringParam("content")
            where {
                it.id eq id
            }
            call.redirect().back()
        }

        flash("success", "Successfully Saved")
    }

    // If the user decides to delete a blog post, this function will be called and will delete the blog post entry
    // from the database
    fun delete(call: HttpCall) {
        val id = call.longParam("id").orAbort()
        BlogData.delete { it.id eq id }

        call.redirect().toRouteNamed("welcome")
        flash("success", "Successfully removed post")
    }
}