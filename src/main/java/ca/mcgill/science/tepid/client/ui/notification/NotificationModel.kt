package ca.mcgill.science.tepid.client.ui.notification

import java.awt.image.BufferedImage

sealed class NotificationModel(val title: String, val body: String)

class StateNotification(title: String,
                        body: String,
                        val color: Int,
                        val icon: NotificationIcon) : NotificationModel(title, body)

class TransitionNotification(title: String,
                             body: String,
                             val from: Int,
                             val to: Int) : NotificationModel(title, body)

enum class NotificationIcon(private val fileName: String? = null) {
    NONE, COLOR, FAIL, NO_QUOTA("noquota"), RECEIVING, SENDING;

    fun image(): BufferedImage? =
            when (this) {
                NONE -> null
                else -> NotificationWindow.resourceImage("icons/${fileName ?: name.toLowerCase()}.png")
            }
}