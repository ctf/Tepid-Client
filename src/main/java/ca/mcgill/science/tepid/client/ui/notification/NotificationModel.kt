package ca.mcgill.science.tepid.client.ui.notification

sealed class NotificationModel

class StateNotification(val title: String,
                        val body: String,
                        val color: Int,
                        val icon: String) : NotificationModel()

class TransitionNotification(val title: String,
                             val body: String,
                             val from: Int,
                             val to: Int) : NotificationModel()