package ca.mcgill.science.tepid.client.ui.notification

sealed class NotificationModel(val title: String, val body: String)

class StateNotification(title: String,
                        body: String,
                        val color: Int,
                        val icon: String) : NotificationModel(title, body)

class TransitionNotification(title: String,
                             body: String,
                             val from: Int,
                             val to: Int) : NotificationModel(title, body)