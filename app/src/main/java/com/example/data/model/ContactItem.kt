package com.example.data.model

data class ContactItem(
    val id: String,
    val name: String,
    val number: String,
    val photoUri: String? = null,
    val initials: String = name.take(2).uppercase()
)
