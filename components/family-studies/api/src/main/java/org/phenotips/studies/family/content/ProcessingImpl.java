package org.phenotips.studies.family.content;

import org.phenotips.Constants;
import org.phenotips.data.Patient;
import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSON;

/**
 * Storage and retrieval.
 */
@Component
public class ProcessingImpl implements Processing
{
    private final static EntityReference PEDIGREE_CLASS =
        new EntityReference("PedigreeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    FamilyUtils familyUtils;

    @Inject
    Provider<XWikiContext> provider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    // fixme make it throw exceptions
    public void processPatientPedigree(JSON json, String patientId) throws XWikiException
    {
        DocumentReference patientRef = referenceResolver.resolve(patientId, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument patientDoc = familyUtils.getDoc(patientRef);
        XWikiDocument familyDoc = familyUtils.getFamilyDoc(patientDoc);
        this.storeFamilyRepresentation(familyDoc, json);
    }

    /**
     * Will store the passed json in the document, or if the family document is null will create one
     */
    public void storeFamilyRepresentation(XWikiDocument family, JSON familyContents) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        BaseObject pedigreeObject = family.getXObject(PEDIGREE_CLASS);
        pedigreeObject.set("image", "", context);
        pedigreeObject.set("data", familyContents.toString(), context);
        wiki.saveDocument(family, context);
    }
}
