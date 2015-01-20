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
package org.phenotips.studies.family.api;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import javax.inject.Inject;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.XWikiContextProvider;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class Family
{
    private final EntityReference pointerReference = new EntityReference("FamilyPointer", EntityType.OBJECT);

    @Inject
    XWikiContextProvider provider;

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

    /**
     *
     * @param patient
     * @return The content of the family document, or null if there is no family or it was not found.
     * @throws XWikiException
     */
    public String getFamily(Patient patient) throws XWikiException
    {
        String pointer = getFamilyPointer(patient.getDocument());
        if (pointer != null) {
            EntityReference pointerRef = new EntityReference(pointer, EntityType.DOCUMENT);
            XWikiDocument familyDoc = getDoc(pointerRef);
            if (familyDoc != null) {
                return familyDoc.getContent();
            }
        }
        return null;
    }
}
