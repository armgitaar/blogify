package com.blog.blogify

import com.blog.blogify.controllers.BlogController
import dev.alpas.routing.RouteGroup
import dev.alpas.routing.Router

// https://alpas.dev/docs/routing
fun Router.addRoutes() = apply {
    group {
        webRoutesGroup()
    }.middlewareGroup("web")

    apiRoutes()
}

private fun RouteGroup.webRoutesGroup() {
    get("/", BlogController::index).name("welcome")
    get("/<page>", BlogController::pages).name("pages")
    get("blog/<id>/<page>", BlogController::show).name("blog.show")
    get("blog/new", BlogController::new).name("blog.new")
    post("blog/submit", BlogController::submit).name("blog.submit")
    get("blog/edit/<id>", BlogController::edit).name("blog.edit")
    patch("blog/edit/<id>", BlogController::update).name("blog.update")
    delete("blog/<id>", BlogController::delete).name("blog.delete")
}

private fun Router.apiRoutes() {
    // register API routes here
}

