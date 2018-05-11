package ca.mcgill.science.tepid.common;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"_id", "type", "currentCheckIn", "lateCheckIns", "lateCheckOuts"})
public class CheckedIn {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("currentCheckIn")
    private Map<String, String[]> currentCheckIn = new HashMap<String, String[]>();
    @JsonProperty("lateCheckIns")
    private Map<String, String[]> lateCheckIns = new HashMap<String, String[]>();
    @JsonProperty("lateCheckOuts")
    private Map<String, String[]> lateCheckOuts = new HashMap<String, String[]>();
    @JsonProperty("type")
    private String type;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     * @return The id
     */
    @JsonProperty("_id")
    public String getId() {
        return id;
    }

    /**
     * @param id The _id
     */
    @JsonProperty("_id")
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return The currentCheckIn
     */
    @JsonProperty("currentCheckIn")
    public Map<String, String[]> getCurrentCheckIn() {
        return currentCheckIn;
    }

    /**
     * @param currentCheckIn The currentCheckIn
     */
    @JsonProperty("currentCheckIn")
    public void setCurrentCheckIn(Map<String, String[]> currentCheckIn) {
        this.currentCheckIn = currentCheckIn;
    }

    /**
     * @return The lateCheckIns
     */
    @JsonProperty("lateCheckIns")
    public Map<String, String[]> getLateCheckIns() {
        return lateCheckIns;
    }

    /**
     * @param lateCheckIns The lateCheckIns
     */
    @JsonProperty("lateCheckIns")
    public void setLateCheckIns(Map<String, String[]> lateCheckIns) {
        this.lateCheckIns = lateCheckIns;
    }

    /**
     * @return The lateCheckOuts
     */
    @JsonProperty("lateCheckOuts")
    public Map<String, String[]> getLateCheckOuts() {
        return lateCheckOuts;
    }

    /**
     * @param lateCheckOuts The lateCheckOuts
     */
    @JsonProperty("lateCheckOuts")
    public void setLateCheckOuts(Map<String, String[]> lateCheckOuts) {
        this.lateCheckOuts = lateCheckOuts;
    }

    /**
     * @return The type
     */
    @JsonProperty("type")
    public String getType() {
        return type;
    }

    /**
     * @param type The type
     */
    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }


    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
