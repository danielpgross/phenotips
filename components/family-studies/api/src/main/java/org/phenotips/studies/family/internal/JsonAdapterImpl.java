package org.phenotips.studies.family.internal;

import org.phenotips.configuration.RecordConfigurationManager;

import org.xwiki.component.annotation.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import net.sf.json.JSONObject;

/**
 * Converts the JSON generated by the pedigree into the default format accepted by PhenoTips. {@link
 * org.phenotips.data.PatientDataController}.
 */
@Component
public class JsonAdapterImpl
{
    @Inject
    Logger logger;

    @Inject
    private RecordConfigurationManager configurationManager;

    public List<JSONObject> convert(@NotNull JSONObject toConvert)
    {
        if (toConvert.containsKey("JSON_version") &&
            !StringUtils.equalsIgnoreCase(toConvert.getString("JSON_version"), "1.0"))
        {
            logger.warn("The version of the pedigree JSON differs from the expected.");
        }

        DateFormat dateFormat =
            new SimpleDateFormat(this.configurationManager.getActiveConfiguration().getISODateFormat());
        List<JSONObject> convertedPatients = new LinkedList<>();
        List<JSONObject> patientJson = PedigreeUtils.extractPatientJSONPropertiesFromPedigree(toConvert);

        for (JSONObject singlePatient : patientJson) {

        }
        return convertedPatients;
    }

    private static JSONObject patientJsonToObject(JSONObject externalPatient, DateFormat dateFormat)
    {
        JSONObject internalPatient = new JSONObject();

        internalPatient = exchangeIds(externalPatient, internalPatient);
        internalPatient = exchangeBasicPatientData(externalPatient, internalPatient);
        internalPatient = exchangeDates(externalPatient, internalPatient, dateFormat);
                    
        return internalPatient;
    }

    private static JSONObject exchangeIds(JSONObject ex, JSONObject inter)
    {
        inter.put("id", ex.get("phenotipsId"));
        inter.put("external_id", ex.get("externalID"));
        return inter;
    }

    private static JSONObject exchangeBasicPatientData(JSONObject ex, JSONObject inter)
    {
        JSONObject name = new JSONObject();
        name.put("first_name", ex.get("fName"));
        name.put("last_name", ex.get("lName"));

        inter.put("sex", ex.get("gender"));
        inter.put("patient_name", name);
        return inter;
    }

    private static JSONObject exchangeDates(JSONObject ex, JSONObject inter, DateFormat format)
    {
        String dob = "dob";
        String dod = "dod";
        if (ex.containsKey(dob)) {
            inter.put("date_of_birth", JsonAdapterImpl.pedigreeDateToDate(ex.getJSONObject(dob)));
        }
        if (ex.containsKey(dod)) {
            inter.put("date_of_death", JsonAdapterImpl.pedigreeDateToDate(ex.getJSONObject(dod)));
        }
        return inter;
    }

    /**
     * Used for converting a pedigree date to a {@link Date}.
     *
     * @param pedigreeDate cannot be null. Must contain at least the decade field.
     */
    private static Date pedigreeDateToDate(JSONObject pedigreeDate)
    {
        String yearString = "year";
        String monthString = "month";
        String dayString = "day";
        DateTime jodaDate;
        if (pedigreeDate.containsKey(yearString)) {
            Integer year = Integer.parseInt(pedigreeDate.getString(yearString));
            Integer month =
                pedigreeDate.containsKey(monthString) ? Integer.parseInt(pedigreeDate.getString(monthString)) : 1;
            Integer day = pedigreeDate.containsKey(dayString) ? Integer.parseInt(pedigreeDate.getString(dayString)) : 1;
            jodaDate = new DateTime(year, month, day, 0, 0);
        } else {
            String decade = pedigreeDate.getString("decade").substring(0, 4);
            jodaDate = new DateTime(Integer.parseInt(decade), 1, 1, 0, 0);
        }
        return new Date(jodaDate.getMillis());
    }
//    public static JSONObject exchangeIds(JSONObject ex, JSONObject inter) {}
//    public static JSONObject exchangeIds(JSONObject ex, JSONObject inter) {}
//    public static JSONObject exchangeIds(JSONObject ex, JSONObject inter) {}
}
