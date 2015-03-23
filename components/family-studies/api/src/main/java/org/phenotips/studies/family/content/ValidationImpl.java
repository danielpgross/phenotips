package org.phenotips.studies.family.content;

import org.phenotips.data.Patient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.internal.access.ViewAccessLevel;
import org.phenotips.studies.family.FamilyUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

/**
 * Collection of checks for checking if certain actions are allowed. Needs to be split up really, but later.
 */
@Component
@Singleton
public class ValidationImpl
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private FamilyUtils familyUtils;

    @Inject
    private PatientAccess access;

    /**
     * Checks if the current {@link com.xpn.xwiki.XWikiContext}/user has sufficient access to this patent id and the
     * family to which the patient is being added to.
     */
    public boolean canAddToFamily(String familyAnchor, String patientId)
    {
        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        AccessLevel level = access.getAccessLevel(patientRef);
        boolean hasPatientAccess = level.compareTo(new ViewAccessLevel()) >= 0;
        if (hasPatientAccess) {

        }
        return false;
    }

    /**
     * Checks if the patient is already present within the family members list.
     *
     * @param familyAnchor a patient within the family in question
     */
    public boolean isInFamily(String familyAnchor, String otherId) throws XWikiException
    {
        // not checking for nulls, so that an exception will be thrown
        XWikiDocument familyDoc = this.getFamilyOfPatient(familyAnchor);
        BaseObject familyClass = familyDoc.getXObject(FamilyUtils.FAMILY_CLASS);
        DBStringListProperty members = (DBStringListProperty) familyClass.get("members");
        return members.getList().contains(otherId.trim());
    }

    /**
     * Does not check for nulls while retrieving the family document. Will throw an exception if any of the 'links in
     * the chain' are not present.
     */
    private XWikiDocument getFamilyOfPatient(String patientId) throws XWikiException
    {
        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = familyUtils.getDoc(patientRef);
        return familyUtils.getFamilyDoc(patientDoc);
    }
}
