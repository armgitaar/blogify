package com.blog.blogify.configs

import dev.alpas.Environment
import dev.alpas.queue.activemq.ActiveMQConnection
import dev.alpas.queue.passthrough.PassThroughQueueConnection
import dev.alpas.queue.QueueConfig as BaseConfig

// https://alpas.dev/docs/queues
@Suppress("unused")
class QueueConfig(env: Environment) : BaseConfig(env) {
    init {
        addConnection("activemq", lazy { ActiveMQConnection(env) })
        addConnection("passthrough", lazy { PassThroughQueueConnection() })
    }
}
