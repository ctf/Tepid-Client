package ca.mcgill.science.tepid.client.notifications;

public class NotificationEntry {
    final boolean quota;
    final String title, body, icon;
    final int color, from, to;

    public NotificationEntry(int color, String icon, String title, String body) {
        this.quota = false;
        this.color = color;
        this.icon = icon;
        this.title = title;
        this.body = body;
        this.from = 0;
        this.to = 1;
    }

    public NotificationEntry(int from, int to, String title, String body) {
        this.quota = true;
        this.from = from;
        this.to = to;
        this.title = title;
        this.body = body;
        this.icon = null;
        this.color = 0;
    }
}
