package com.blog.blogify.database.migrations

import com.blog.blogify.entities.Blogs
import dev.alpas.ozone.migration.Migration

class CreateBlogsTable : Migration() {
    override var name = "2020_02_28_205021_create_blogs_table"
    
    override fun up() {
        createTable(Blogs)
    }
    
    override fun down() {
        dropTable(Blogs)
    }
}