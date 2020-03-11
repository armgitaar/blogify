package com.blog.blogify.actions

import com.blog.blogify.entities.Blogs
import dev.alpas.ozone.latest
import me.liuwj.ktorm.dsl.count
import me.liuwj.ktorm.entity.map
import kotlin.math.ceil

class Pagination(val previousUrl: String?, val nextUrl: String?) {
    fun hasPrevious(): Boolean {
        return previousUrl != null
    }

    fun hasNext(): Boolean {
        return nextUrl != null
    }
}

// This function creates previous / next paging from one blog post to the next
fun pagination(currentPage: Int): Pagination {
    val blogs = Blogs.latest().map { it.id.toInt() }.toList()
    val index = blogs.indexOf(currentPage)
    val previousUrl = if (index < 1) null else blogs[index - 1].toString()
    val nextUrl = if (index >= blogs.size - 1) null else blogs[index + 1].toString()

    return Pagination(previousUrl, nextUrl)
}

// This function creates pagination at the top level
fun mainPagination(currentPage: Int): Pagination {
    val numPages = ceil(Blogs.count().toDouble() / 9)
    var previousUrl = if (currentPage <= 1) null else (currentPage - 1).toString()
    var nextUrl = if (currentPage.toDouble() == numPages) null else (currentPage + 1).toString()

    return Pagination(previousUrl, nextUrl)
}