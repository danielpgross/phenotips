package org.phenotips.studies.family.content;

import net.sf.json.JSONObject;

/**
 * Passed around different functions to preserve important error information.
 */
public class StatusResponse
{
    public int statusCode = 200;

    public String message = "";

    public String errorType = "";

    public String asFamilyStatus(boolean isFamily, boolean hasFamily) {
        boolean isError = statusCode != 200;
        JSONObject json = basicJson();
        json.put("error", isError);
        json.put("isFamilyPage", isFamily);
        json.put("hasFamily", hasFamily);
        return json.toString();
    }

    public String asProcessing() {
        boolean isError = statusCode != 200;
        JSONObject json = basicJson();
        json.put("error", isError);
        return json.toString();
    }

    public String asVerification() {
        boolean valid = statusCode != 200;
        JSONObject json = basicJson();
        json.put("validLink", valid);
        return json.toString();
    }

    private JSONObject basicJson() {
        JSONObject json = new JSONObject();
        json.put("errorMessage", message);
        json.put("errorType", errorType);
        return json;
    }
}
