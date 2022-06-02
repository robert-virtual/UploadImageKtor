package com.example.uploadimagektor

import io.ktor.client.*
import io.ktor.client.engine.cio.*

object KtorClient {
    val httpClient = HttpClient(CIO)
}