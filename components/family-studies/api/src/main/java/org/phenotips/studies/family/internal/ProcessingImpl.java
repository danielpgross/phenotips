package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.data.PatientRepository;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Storage and retrieval.
 */
@Component
public class ProcessingImpl implements Processing
{
    @Inject
    PatientRepository patientRepository;

    @Inject
    FamilyUtils familyUtils;

    @Inject
    Provider<XWikiContext> provider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    private Validation validation;

    // fixme make it throw exceptions
    public StatusResponse processPatientPedigree(String anchorId, JSONObject json, String image) throws XWikiException
    {
        StatusResponse response = new StatusResponse();
        DocumentReference anchorRef = referenceResolver.resolve(anchorId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument anchorDoc = familyUtils.getDoc(anchorRef);
        XWikiDocument familyDoc = familyUtils.getFamilyDoc(anchorDoc);

        if (anchorDoc == null) {  // fixme must check for all conditions as in verify linkable
            response.statusCode = 404;
            response.errorType = "invalidId";
            response.message = String.format("The family/patient %s is invalid", anchorId);
            return response;
        }

        if (familyDoc != null) {
            List<String> members = familyUtils.getFamilyMembers(familyDoc);
            List<String> updatedMembers = this.extractIdsFromPedigree(json);

            StatusResponse duplicationStatus = this.checkForDuplicates(updatedMembers);
            if (duplicationStatus.statusCode != 200) {
                return duplicationStatus;
            }
            // sometimes pedigree passes in family document name as a member
            updatedMembers.remove(familyDoc.getDocumentReference().getName());
            members = Collections.unmodifiableList(members);
            updatedMembers = Collections.unmodifiableList(updatedMembers);

            // storing first, because pedigree depends on this.
            StatusResponse storingResponse = this.storeFamilyRepresentation(familyDoc, updatedMembers, json, image);
            if (storingResponse.statusCode != 200) {
                return storingResponse;
            }
            if (updatedMembers.size() < 1) {
                // the list of members should not be empty.
                response.statusCode = 412;
                response.errorType = "invalidUpdate";
                response.message = "The family has no members. Please specify at least one patient link.";
                return response;
            }

            this.removeMembersNotPresent(members, updatedMembers);
            this.addNewMembers(members, updatedMembers, familyDoc);
            // remove and add do not take care of modifying the 'members' property
            familyUtils.setFamilyMembers(familyDoc, updatedMembers);
        } else {
            if (!validation.hasPatientEditAccess(anchorDoc)) {
                return validation.createInsufficientPermissionsResponse(anchorId);
            }
            // when saving just a patient's pedigree that does not belong to a family
            XWikiContext context = provider.get();
            this.storePedigree(anchorDoc, json, image, context, context.getWiki());
        }

        response.statusCode = 200;
        return response;
    }

    private StatusResponse checkForDuplicates(List<String> updatedMembers)
    {
        StatusResponse response = new StatusResponse();
        List<String> duplicationCheck = new LinkedList<>();
        duplicationCheck.addAll(updatedMembers);
        for (String member : updatedMembers) {
            duplicationCheck.remove(member);
            if (duplicationCheck.contains(member)) {
                response.statusCode = 400;
                response.errorType = "duplicate";
                response.message = String.format("There is a duplicate link for patient %s", member);
                return response;
            }
        }

        response.statusCode = 200;
        return response;
    }

    private StatusResponse storeFamilyRepresentation(XWikiDocument family, List<String> updatedMembers,
        JSON familyContents, String image) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        // first check that the user has permissions for all patients
        for (String member : updatedMembers) {
            StatusResponse patientResponse = validation.canAddToFamily(family, member);
            if (patientResponse.statusCode != 200) {
                return patientResponse;
            }
        }
        // only then actually store the changes
        for (String member : updatedMembers) {
            XWikiDocument patientDoc = wiki.getDocument(patientRepository.getPatientById(member).getDocument(), context);
            this.storePedigree(patientDoc, familyContents, image, context, wiki);
        }
        // todo for now forgo family access check, because of inability to modify those permissions.
        // StatusResponse familyResponse = validation.checkFamilyAccessWithResponse(family);
        this.storePedigree(family, familyContents, image, context, wiki);

        StatusResponse familyResponse = new StatusResponse();
        familyResponse.statusCode = 200;
        return familyResponse;
    }

    /** Does not do permission checks. */
    private void storePedigree(XWikiDocument document, JSON pedigree, String image, XWikiContext context, XWiki wiki)
        throws XWikiException
    {
        BaseObject pedigreeObject = document.getXObject(FamilyUtils.PEDIGREE_CLASS);
        pedigreeObject.set("image", image, context);
        pedigreeObject.set("data", pedigree.toString(), context);
        wiki.saveDocument(document, context);
    }

    /**
     * @return all PhenoTips ids from pedigree nodes that have internal ids
     */
    private List<String> extractIdsFromPedigree(JSONObject pedigree)
    {
        List<String> extractedIds = new LinkedList<>();
        JSONArray gg = (JSONArray) pedigree.get("GG");
        // letting it throw a null exception on purpose
        for (Object nodeObj : gg) {
            JSONObject node = (JSONObject) nodeObj;
            JSONObject properties = (JSONObject) node.get("prop");
            if (properties == null) {
                continue;
            }
            Object id = properties.get("phenotipsId");
            if (id != null && StringUtils.isNotBlank(id.toString())) {
                extractedIds.add(id.toString());
            }
        }
        return extractedIds;
    }

    /**
     * Removes records from the family that are no longer in the updated family structure.
     */
    private void removeMembersNotPresent(List<String> currentMembers, List<String> updatedMembers) throws XWikiException
    {
        List<String> toRemove = new LinkedList<>();
        toRemove.addAll(currentMembers);
        toRemove.removeAll(updatedMembers);
        if (!toRemove.isEmpty()) {
            XWikiContext context = provider.get();
            XWiki wiki = context.getWiki();
            for (String oldMember : toRemove) {
                Patient patient = patientRepository.getPatientById(oldMember);
                if (patient != null) {
                    XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
                    BaseObject familyRefObj = patientDoc.getXObject(FamilyUtils.FAMILY_REFERENCE);
                    if (familyRefObj != null) {
                        patientDoc.removeXObject(familyRefObj);
                        wiki.saveDocument(patientDoc, context);
                    }
                }
            }
        }
    }

    private void addNewMembers(List<String> currentMembers, List<String> updatedMembers, XWikiDocument familyDoc)
        throws XWikiException
    {
        List<String> newMembers = new LinkedList<>();
        newMembers.addAll(updatedMembers);
        newMembers.removeAll(currentMembers);
        if (!newMembers.isEmpty()) {
            XWikiContext context = provider.get();
            XWiki wiki = context.getWiki();
            for (String newMember : newMembers) {
                Patient patient = patientRepository.getPatientById(newMember);
                if (patient != null) {
                    XWikiDocument patientDoc = wiki.getDocument(patient.getDocument(), context);
                    familyUtils.setFamilyReference(patientDoc, familyDoc, context);
                    wiki.saveDocument(patientDoc, context);
                }
            }
        }
    }
}
