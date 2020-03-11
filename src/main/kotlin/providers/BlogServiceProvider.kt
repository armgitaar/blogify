package com.blog.blogify.providers 

import dev.alpas.Application
import dev.alpas.ServiceProvider
import com.blog.blogify.actions.ParseBlog
import dev.alpas.make

class BlogServiceProvider() : ServiceProvider {
    override fun register(app: Application) {
        // register your bindings here like so: container.bind(MyApiService())
        // Do not ask for any dependencies here as they might not have been registered yet.
    }

    override fun boot(app: Application) {
        // do some initial setup here
        // Feel free to ask for any dependencies here as they should be all registered by now.
        app.bind(ParseBlog(app.make()))
    }
}