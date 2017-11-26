package ca.mcgill.science.tepid.client;

public class Pair<K, V> {

    public final K first;

    public final V second;

    public Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return first + "=" + second;
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 13 + (second == null ? 0 : second.hashCode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Pair) {
            Pair pair = (Pair) o;
            if (first != null ? !first.equals(pair.first) : pair.first != null) return false;
            if (second != null ? !second.equals(pair.second) : pair.second != null) return false;
            return true;
        }
        return false;
    }
}