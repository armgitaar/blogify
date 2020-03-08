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

    // Index is the main landing page for the blog
    fun index(call: HttpCall) {

        // Pagination calls the main pagination function to see if there are more than 9 posts, if so, this will create
        // the need for top level paging too. This blog site will display 9 results per page.
        val pagination = mainPagination(1)
        val blogs = getBlogs(0, 1)

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }

    // Since there will likely be multiple blog posts, we will incorporate ability to page between groups of results.
    // This route is used when there are more than 9 blog posts to necessitate navigation between multiple pages.
    fun pages(call: HttpCall) {
        val page = call.stringParam("page") ?: "/"
        val pagination = mainPagination(page.toLong())
        var previous: Int = 0

        // This portion sees what page the user is on and if there is a previous page. This is to assist with setting the
        // range of blog post results to ignore and the range of results to include on the page.
        if (pagination.previousUrl != null) {
            previous = pagination.previousUrl.toInt()
        }

        val blogs = getBlogs(previous, page.toInt())

        call.render("welcome", mapOf("blogs" to blogs, "pagination" to pagination))
    }

    // The show function is essentially the blog post's dedicated page. We reference the page ID from the URL to determine which
    // post to pull from the database.
    fun show(call: HttpCall) {
        val pageId = call.stringParam("id") ?: "/"
        val id: Long = pageId.toLong()
        // This is where we call the service provider we created earlier
        val blog = call.make<Blogs>()
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

        val content = call.stringParam(("content"))
        val blog = call.make<Blogs>()
        var (markData, title, author, date, image, tags) = blog.readSource(content!!)

        call.applyRules("content") {
            required()
            blogData(markData, title, author, date, image, tags)
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
        println(id)
        BlogData.delete { it.id eq id }

        call.redirect().toRouteNamed("welcome")
        flash("success", "Successfully removed post")
    }

    // This function gets a blog post's data using the ID that is passed to it
    private fun getBlog(id: Long): List<Pair<Long, String?>> {
        return BlogData.latest().filter { it.id eq id }.map { Pair(it.id, it.content) }
    }

    // This function is used for paging at the top level.
    // 9 posts are displayed per page. It works by ignoring blog post entries up to the previous page * 9
    // and the pulls the following 9 results to display on the page
    private fun getBlogs(previousPage: Int, currentPage: Int): MutableList<Any> {
        var previous = previousPage * 9
        var current = previous + 9

        //TODO - figure out why more than 9 results appear on page 2 when there are 19+ blog post entries...

        var blogs = BlogData.latest().drop(previous).take(current).toList()

        var blogsList = mutableListOf<Any>()

        // This portion loops through the results and extracts information from meta data to display; such as,
        // title, author, tags, data, and first 100 characters of blog content
        for (x in blogs) {
            val blog = call.make<Blogs>()
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
            markData = markData.substring(0, Math.min(markData.length, 100)) + " ..."

            val list = mutableListOf<Any>(id, markData, title, author, date, image, tags, urlTitle)
            blogsList.add(list)
        }

        return blogsList
    }

}

// This function creates pagination at the top level
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

// This function creates previous / next paging from one blog post to the next
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

