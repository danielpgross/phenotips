package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.internal.PhenoTipsPatient;
import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.data.permissions.PatientAccess;
import org.phenotips.data.permissions.PermissionsManager;
import org.phenotips.data.permissions.internal.DefaultPatientAccess;
import org.phenotips.data.permissions.internal.PatientAccessHelper;
import org.phenotips.data.permissions.internal.access.EditAccessLevel;
import org.phenotips.security.authorization.AuthorizationService;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

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
public class ValidationImpl implements Validation
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private FamilyUtils familyUtils;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private UserManager userManager;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private PatientAccessHelper patientAccessHelper;

    /**
     * Checks if the patient is already present within the family members list.
     *
     * @param familyAnchor a patient within the family in question
     */
    public boolean isInFamily(String familyAnchor, String otherId) throws XWikiException
    {
        // not checking for nulls, so that an exception will be thrown
        XWikiDocument familyDoc = familyUtils.getFamilyOfPatient(familyAnchor);
        if (familyDoc == null) {
            return false;
        }
        BaseObject familyClass = familyDoc.getXObject(FamilyUtils.FAMILY_CLASS);
        DBStringListProperty members = (DBStringListProperty) familyClass.get("members");
        return members.getList().contains(otherId.trim());
    }

    /**
     * Checks if the current {@link com.xpn.xwiki.XWikiContext}/user has sufficient access to this patent id and the
     * family to which the patient is being added to.
     * 1 - anchor does not belogn to a family, 2 - no access, 3 - pedigree exists for other true
     */
    public StatusResponse canAddToFamily(String familyAnchor, String patientId) throws XWikiException
    {
        StatusResponse response = new StatusResponse();

        DocumentReference familyAnchorRef = referenceResolver.resolve(familyAnchor, Patient.DEFAULT_DATA_SPACE);
        EntityReference familyRef = familyUtils.getFamilyReference(familyUtils.getDoc(familyAnchorRef));
        if (familyRef == null) {
            response.statusCode = 404;
            response.errorType = "noFamily";
            response.message = "Cannot link nodes. Anchor node does not belong to a family.";
            return response;
        }
        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = familyUtils.getDoc(patientRef);
        if (patientDoc == null) {
            response.statusCode = 404;
            response.errorType = "invalidId";
            response.message = "Could not find the patient document of the patient to be added to the family.";
            return response;
        }
        PatientAccess patientAccess =
            new DefaultPatientAccess(new PhenoTipsPatient(patientDoc), patientAccessHelper, permissionsManager);
        AccessLevel patientAccessLevel = patientAccess.getAccessLevel(patientRef);
        boolean hasPatientAccess = patientAccessLevel.compareTo(new EditAccessLevel()) >= 0;
        if (hasPatientAccess) {
            if (familyUtils.getPedigree(patientDoc).isEmpty()) {
                User currentUser = userManager.getCurrentUser();
                if (authorizationService.hasAccess(currentUser, Right.EDIT, new DocumentReference(familyRef))) {
                    response.statusCode = 200;
                    return response;
                }
                response.statusCode = 401;
                response.errorType = "permissions";
                response.message = "Insufficient permissions to edit the family record.";
                return response;
            } else {
                response.statusCode = 501;
                response.errorType = "existingPedigree";
                response.message = "The patient to be added to the family has an existing pedigree.";
                return response;
            }
        }
        response.statusCode = 401;
        response.errorType = "permissions";
        response.message = "Insufficient permissions to edit the patient record.";
        return response;
    }


    public boolean hasFamily(String id) throws XWikiException
    {
        return familyUtils.getFamilyOfPatient(id) != null;
    }

}
