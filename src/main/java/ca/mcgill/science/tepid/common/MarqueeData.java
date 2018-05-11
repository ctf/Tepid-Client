package ca.mcgill.science.tepid.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class is a port from the CUPPA system.
 * it is used in the ScreenSaver to encapsulate messages.
 */
@JsonIgnoreProperties({"_rev", "_id", "type"})
public class MarqueeData {
    @JsonProperty("title")
    public String title;        //the title to be displayed over the message
    @JsonProperty("entry")
    public List<String> entry;    //the message itself
}
