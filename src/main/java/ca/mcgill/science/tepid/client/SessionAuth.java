package ca.mcgill.science.tepid.client;

/**
 * User, password pair
 */
public class SessionAuth {

    public final String username, password;
    public static final SessionAuth INVALID = new SessionAuth("", "");

    public static SessionAuth create(String username, String password) {
        if (username == null || password == null)
            return INVALID;
        return new SessionAuth(username, password);
    }

    private SessionAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public int hashCode() {
        return username.hashCode() * 7 + password.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SessionAuth)) return false;
        SessionAuth other = (SessionAuth) obj;
        return username.equals(other.username) && password.equals(other.password);
    }

    @Override
    public String toString() {
        return String.format("SessionAuth[u=%s, p=%s]",
                username, Integer.toString(password.hashCode()));
    }
}
