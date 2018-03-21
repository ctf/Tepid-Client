package ca.mcgill.science.tepid.client;

/**
 * User, password pair
 */
public class SessionAuth {

    public final String username, password;
    public final boolean valid;
    public static final SessionAuth INVALID = new SessionAuth(null, null);

    public SessionAuth(String username, String password) {
        this.valid = username != null && password != null;
        this.username = valid ? username : null;
        this.password = valid ? password : null;
    }

    @Override
    public int hashCode() {
        if (!valid) return 13;
        return username.hashCode() * 7 + password.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SessionAuth)) return false;
        SessionAuth other = (SessionAuth) obj;
        if (!valid) return !other.valid;
        return username.equals(other.username) && password.equals(other.password);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
