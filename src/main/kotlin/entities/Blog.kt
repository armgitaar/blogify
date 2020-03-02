package com.blog.blogify.entities

import dev.alpas.ozone.*
import java.time.Instant

interface Blog : OzoneEntity<Blog> {
    var id: Long
    var content: String?
    var createdAt: Instant?
    var updatedAt: Instant?

    companion object : OzoneEntity.Of<Blog>()
}

object Blogs : OzoneTable<Blog>("blogs") {
    val id by bigIncrements()
    val content by string("content").size(10000).nullable().bindTo { it.content }
    val createdAt by createdAt()
    val updatedAt by updatedAt()
}