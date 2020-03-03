package com.blog.blogify.actions

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import com.vladsch.flexmark.ext.yaml.front.matter.AbstractYamlFrontMatterVisitor
import com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension
import dev.alpas.*
import redis.clients.jedis.JedisPool
import java.io.File
import java.nio.file.Paths
import javax.sound.sampled.Line

class Blogs(
    private val env: Environment,
    private val resourceLoader: ResourceLoader,
    private val jedisPool: JedisPool
) {

    private fun blogsPath(page: String): String {
        val blogsStorageLink = env.storagePath(*RESOURCES_DIRS, "blog", page)
        return if (File(blogsStorageLink).exists()) {
            blogsStorageLink
        } else {
            Paths.get("blog", page).toString().mustStartWith("/")
        }
    }

    private val markdown by lazy { Markdown() }

    /*fun get(page: String): String {

        return if (env.isDev) {
            convert(page)
        } else {
            jedisPool.resource.use { jedis ->
                return jedis.hget("blog", page) ?: convert(page).also { jedis.hset("blog", page, it) }
            }
        }
    }*/


    fun convert(markData: String) = markdown.convert(markData)

    data class blogData(val content : String, val title : String, val author : String, val date : String, val image : String, val tags : String)


    fun readSource(content: String): blogData {

        var markData = content


        /*Node document = PARSER.parse(input);
        visitor.visit(document);
        println(theTitle)*/

        //JekyllFrontMatterExtension.create()
            /*resourceLoader.load(blogsPath("$page.md"))
            ?.readText()
            .orAbort("Page $page not found!")*/

        var stopper = 0
        val hasMeta = Regex(pattern = "---")
        val hasTitle = Regex(pattern = "Title:")
        var title = ""
        val hasAuthor = Regex(pattern = "Author:")
        var author = ""
        val hasDate = Regex(pattern = "Date:")
        var date = ""
        val hasImage = Regex(pattern = "heroimage:")
        var image = ""
        val hasTags = Regex(pattern = "tags:")
        var tags = ""

        fun trimMeta(data : String, key : Regex): String {
            return data.replace(key, "").trimStart()
        }

        for (line in content.lines()) {
            if (hasMeta.containsMatchIn(line)) {
                stopper = stopper + 1
            } else if (hasTitle.containsMatchIn(line)) {
                title = trimMeta(line, hasTitle)
            } else if (hasAuthor.containsMatchIn(line)) {
                author = trimMeta(line, hasAuthor)
            } else if (hasDate.containsMatchIn(line)) {
                date = trimMeta(line, hasDate)
            } else if (hasImage.containsMatchIn(line)) {
                image = trimMeta(line, hasImage)
            } else if (hasTags.containsMatchIn(line)) {
                tags = trimMeta(line, hasTags)
            } else { continue }

            markData = markData.removeRange(0, line.length+2)

            if (stopper == 2) { break }
        }

        return blogData(markData, title, author, date, image, tags)
    }

}

class Markdown {
    private val options by lazy {
        MutableDataSet().apply {
            set(Parser.EXTENSIONS, listOf(AutolinkExtension.create(), TablesExtension.create(), YamlFrontMatterExtension.create()))
            set(HtmlRenderer.HARD_BREAK, "<br />\n")
            set(HtmlRenderer.RENDER_HEADER_ID, true)
            set(TablesExtension.CLASS_NAME, "pure-table pure-table-striped")
        }
    }
    private val parser: Parser by lazy { Parser.builder(options).build() }
    private val renderer: HtmlRenderer by lazy { HtmlRenderer.builder(options).build() }

    internal fun convert(markdown: String?): String {
        val blog = parser.parse(markdown.orEmpty())
        return renderer.render(blog)
    }
}
