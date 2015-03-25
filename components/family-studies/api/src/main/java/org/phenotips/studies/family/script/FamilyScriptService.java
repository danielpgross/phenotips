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
package org.phenotips.studies.family.script;

import org.phenotips.studies.family.FamilyUtils;
import org.phenotips.studies.family.Processing;
import org.phenotips.studies.family.Validation;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSONObject;

@Component
@Singleton
@Named("family")
public class FamilyScriptService implements ScriptService
{
    @Inject
    FamilyUtils utils;

    @Inject
    Processing processing;

    @Inject
    Validation validation;

    @Inject
    Logger logger;

    /** Can return null */
    public DocumentReference createFamily(String patientId)
    {
        try {
            XWikiDocument doc = utils.createFamilyDoc(patientId);
            return doc != null ? doc.getDocumentReference() : null;
        } catch (Exception ex) {
            logger.error("Could not create a new family document {}", ex.getMessage());
        }
        return null;
    }

    /** Can return null. */
    public DocumentReference getPatientsFamily(XWikiDocument patient)
    {
        try {
            XWikiDocument doc = utils.getFamilyDoc(patient);
            return doc != null ? doc.getDocumentReference() : null;
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
        }
        return null;
    }

    /**
     * @return 200 if everything is ok, an error code if the patient is not linkable.
     */
    public int verifyLinkable(String thisId, String otherId)
    {
        try {
            if (validation.isInFamily(thisId, otherId)) {
                return 208;
            } else {
                int canAddCode = validation.canAddToFamily(thisId, otherId);
                if (canAddCode == 1) {
                    return 401;
                } else if (canAddCode == 2) {
                    // cannot add patients with existing pedigrees to a family
                    return 501;
                }
            }
            // if thisId does not belong to a family still returns 200.
            return 200;
        } catch (XWikiException ex) {
            return 500;
        }
    }

    public int processPedigree(String familyMemberId, String json, String image)
    {
        try {
            // fixme remove
            familyMemberId = "P0000001";
            return this.processing.processPatientPedigree(familyMemberId, JSONObject.fromObject(json), image);
        } catch (Exception ex) {
            return 500;
        }
    }

    public JSONObject generateResponse(int statusCode, String message) {
        return new JSONObject();
    }
}
