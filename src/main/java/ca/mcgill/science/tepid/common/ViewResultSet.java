package ca.mcgill.science.tepid.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ViewResultSet<K, T> {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row<K, T> {
        @JsonProperty("key")
        public K key;
        @JsonProperty("value")
        public T value;
    }

    @JsonProperty("rows")
    public List<Row<K, T>> rows;
}
