/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.studies.family.content;

import org.phenotips.Constants;
import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Component
public class FamilyUtilsImpl implements org.phenotips.studies.family.FamilyUtils
{
    private final String PREFIX = "FAM";

    private final EntityReference FAMILY_REFERENCE =
        new EntityReference("FamilyReference", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    private final EntityReference RELATIVEREFERENCE =
        new EntityReference("RelativeClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    @Inject
    Provider<XWikiContext> provider;

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> referenceResolver;

    private XWikiDocument getDoc(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    /**
     * @return String could be null in case there is no pointer found
     */
    private EntityReference getFamilyReference(XWikiDocument doc) throws XWikiException
    {
        if (doc == null) {
            throw new IllegalArgumentException("Document reference for the patient was null");
        }
        BaseObject familyPointer = doc.getXObject(FAMILY_REFERENCE);
        if (familyPointer != null) {
            String familyDocName = familyPointer.getStringValue("reference");
            if (StringUtils.isNotBlank(familyDocName)) {
                return referenceResolver.resolve(familyDocName);
            }
        }
        return null;
    }

    // fixme make it throw exceptions
    public void processPatientPedigree(JSON json, String patientId) throws XWikiException
    {
        DocumentReference patientRef = referenceResolver.resolve(patientId);
        XWikiDocument patientDoc = getDoc(patientRef);
        XWikiDocument familyDoc = this.getFamilyDoc(patientDoc);
        this.storeFamilyRepresentation(familyDoc, json);
    }

    public XWikiDocument getFamilyDoc(XWikiDocument patient) throws XWikiException
    {
        EntityReference reference = getFamilyReference(patient);
        if (reference != null) {
            return getDoc(reference);
        }
        return null;
    }

    /**
     * @param doc which contains the JSON family object
     * @return The content of the family document, or a new blank family if there is no family or it was not found.
     */
    public JSONObject getFamilyRepresentation(XWikiDocument doc)
    {
        if (doc != null) {
            return JSONObject.fromObject(doc.getContent());
        }
        return createBlankFamily();
    }

    /**
     * Relatives are patients that are stored in the RelativeClass (old interface).
     *
     * @return collection of patient ids that the patient has links to on their report
     */
    public Collection<String> getRelatives(XWikiDocument patientDoc) throws XWikiException
    {
        if (patientDoc != null) {
            List<BaseObject> relativeObjects = patientDoc.getXObjects(RELATIVEREFERENCE);
            if (relativeObjects == null) {
                return Collections.emptySet();
            }
            Set<String> relativeIds = new HashSet<String>();
            for (BaseObject relative : relativeObjects) {
                String id = relative.getStringValue("relative_of");
                if (StringUtils.isNotBlank(id)) {
                    relativeIds.add(id);
                }
            }
            return relativeIds;
        }
        return Collections.emptySet();
    }

    /**
     * Will store the passed json in the document, or if the family document is null will create one
     */
    public void storeFamilyRepresentation(XWikiDocument family, JSON familyContents) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        family.setContent(familyContents.toString());
        wiki.saveDocument(family, context);
    }

    /** Creates a new family document and set that new document as the patients family, overwriting the existing
     * family. */
    public synchronized XWikiDocument createFamilyDoc(XWikiDocument patientDoc)
        throws NamingException, QueryException, XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        long nextId = getLastUsedId() + 1;
        String nextStringId = String.format("%s%07d", PREFIX, nextId);
        EntityReference nextRef = new EntityReference(nextStringId, EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument nextDoc = wiki.getDocument(nextRef, context);
        if (!nextDoc.isNew()) {
            throw new NamingException("The new family id was already taken.");
        } else {
            BaseObject pointer = patientDoc.getXObject(FAMILY_REFERENCE);
            BaseObject familyObject = nextDoc.newXObject(FAMILY_CLASS, context);
            familyObject.set("identifier", nextId, context);
            if (pointer == null) {
                pointer = patientDoc.newXObject(FAMILY_REFERENCE, context);
            }
            pointer.set("reference", nextStringId, context);
            wiki.saveDocument(nextDoc, context);
            wiki.saveDocument(patientDoc, context);
        }
        return nextDoc;
    }

    private JSONObject createBlankFamily()
    {
        JSONObject family = new JSONObject();
        family.put("list", new JSONArray());
        family.put("pedigree", new JSONArray());
        return family;
    }

    private long getLastUsedId() throws QueryException
    {
        long crtMaxID = 0;
        Query q =
            this.qm.createQuery(
                "select family.identifier from Document doc, doc.object(PhenoTips.FamilyClass) as family"
                    + " where family.identifier is not null order by family.identifier desc", Query.XWQL)
                .setLimit(1);
        List<Long> crtMaxIDList = q.execute();
        if (crtMaxIDList.size() > 0 && crtMaxIDList.get(0) != null) {
            crtMaxID = crtMaxIDList.get(0);
        }
        crtMaxID = Math.max(crtMaxID, 0);
        return crtMaxID;
    }
}
