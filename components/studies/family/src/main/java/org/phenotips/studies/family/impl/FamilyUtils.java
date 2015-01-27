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
package org.phenotips.studies.family.impl;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.api.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
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

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiContextProvider;
import com.xpn.xwiki.objects.BaseObject;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Component
public class FamilyUtils implements Family
{
    private final String prefix = "F";

    private final EntityReference pointerReference = new EntityReference("FamilyPointer", EntityType.OBJECT);

    private final EntityReference relativeReference = new EntityReference("RelativeClass", EntityType.OBJECT);

    @Inject
    XWikiContextProvider provider;

    /** Runs queries for finding families. */
    @Inject
    private QueryManager qm;

    private XWikiDocument getDoc(EntityReference docRef) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        return wiki.getDocument(docRef, context);
    }

    /**
     * @return String could be null in case there is no pointer found
     */
    private String getFamilyPointer(DocumentReference docRef) throws XWikiException
    {
        if (docRef == null) {
            throw new IllegalArgumentException("Document reference for the patient was null");
        }
        BaseObject familyPointer = getDoc(docRef).getXObject(pointerReference);
        return familyPointer.getStringValue("pointer");
    }

    public XWikiDocument getFamilyDoc(Patient patient) throws XWikiException
    {
        String pointer = getFamilyPointer(patient.getDocument());
        if (pointer != null) {
            EntityReference pointerRef = new EntityReference(pointer, EntityType.DOCUMENT);
            return getDoc(pointerRef);
        }
        return null;
    }

    /**
     * @param doc which contains the JSON family object
     * @return The content of the family document, or null if there is no family or it was not found.
     */
    public JSONObject getFamily(XWikiDocument doc)
    {
        if (doc != null) {
            return JSONObject.fromObject(doc.getContent());
        }
        return new JSONObject();
    }

    /**
     * Relatives are patients that are stored in the RelativeClass (old interface).
     *
     * @return collection of patient ids that the patient has links to on their report
     */
    public Collection<String> getRelatives(Patient patient) throws XWikiException
    {
        XWikiDocument patientDoc = getDoc(patient.getDocument());
        if (patientDoc != null) {
            List<BaseObject> relativeObjects = patientDoc.getXObjects(relativeReference);
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
    public void storeFamily(XWikiDocument family, JSON familyContents) throws XWikiException
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        family.setContent(familyContents.toString());
        wiki.saveDocument(family, context);
    }

    public synchronized XWikiDocument createFamilyDoc(Patient patient) throws Exception
    {
        XWikiContext context = provider.get();
        XWiki wiki = context.getWiki();
        String nextId = String.format("%s%7d", prefix, getLastUsedId() + 1);
        EntityReference nextRef = new EntityReference(nextId, EntityType.DOCUMENT, Patient.DEFAULT_DATA_SPACE);
        XWikiDocument nextDoc = wiki.getDocument(nextRef, context);
        if (!nextDoc.isNew()) {
            throw new Exception("The new family id was already taken.");
        } else {
            wiki.saveDocument(nextDoc, context);
            XWikiDocument patientDoc = getDoc(patient.getDocument());
            BaseObject pointer = patientDoc.getXObject(pointerReference);
            if (pointer == null) {
                pointer = patientDoc.newXObject(pointerReference, context);
            }
            pointer.set("pointer", nextId, context);
        }
        return nextDoc;
    }

    public JSONObject createBlankFamily()
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
