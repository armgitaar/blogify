package com.blog.blogify.actions

import com.vladsch.flexmark.ext.autolink.AutolinkExtension
import com.vladsch.flexmark.ext.tables.TablesExtension
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.data.MutableDataSet
import dev.alpas.*

class ParseBlog(
    private val env: Environment
) {

    private val markdown by lazy { Markdown() }

    fun convert(markData: String) = markdown.convert(markData)

    data class blogData(val content : String, val title : String, val author : String, val date : String, val image : String, val tags : String)

    fun readSource(content: String): blogData {

        // Declare variables for MetaData define a Regex pattern so we can locate and store MetaData
        var markData = content
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

        // This operation works through the MetaData in the blog post and abstracts
        // information such as title, author, date, image, and tags. The info is then
        // removed from markData so that markData becomes the blog content sans MetaData
        // Note - flextmark extension has a Front Matter plugin which could be used
        // in place of this method
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

        // Now that we pulled MetaData and seperated from content, return back to the
        // function call
        return blogData(markData, title, author, date, image, tags)
    }

}

// This is where the markdown to html conversion happens
class Markdown {
    private val options by lazy {
        MutableDataSet().apply {
            set(Parser.EXTENSIONS, listOf(AutolinkExtension.create(), TablesExtension.create()))
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
