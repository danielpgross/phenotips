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
import org.phenotips.studies.family.content.StatusResponse;

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
    public String getFamilyStatus(String id)
    {
        StatusResponse response = new StatusResponse();
        boolean isFamily = false;
        boolean hasFamily = false;
        try {
            XWikiDocument doc = utils.getFromDataSpace(id);
            XWikiDocument familyDoc = utils.getFamilyDoc(doc);
            hasFamily = familyDoc != null;
            if (hasFamily) {
                isFamily = familyDoc.getDocumentReference() == doc.getDocumentReference();
            }
            response.statusCode = 200;
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
            response.statusCode = 500;
        }
        return response.asFamilyStatus(isFamily, hasFamily);
    }

    /**
     * @return 200 if everything is ok, an error code if the patient is not linkable.
     */
    public String verifyLinkable(String thisId, String otherId)
    {
        int status = 200;
        try {
            if (validation.isInFamily(thisId, otherId)) {
                status = 208;
            } else {
                int canAddCode = validation.canAddToFamily(thisId, otherId);
                if (canAddCode == 1) {
                    status = 401;
                } else if (canAddCode == 2) {
                    // cannot add patients with existing pedigrees to a family
                    status = 501;
                }
            }
            // if thisId does not belong to a family still returns 200.
            status = 200;
        } catch (XWikiException ex) {
            status = 500;
        }
        StatusResponse response = new StatusResponse();
        response.statusCode = status;
        return response.asVerification();
    }

    public String processPedigree(String anchorId, String json, String image)
    {
        int status;
        try {
            status = this.processing.processPatientPedigree(anchorId, JSONObject.fromObject(json), image);
        } catch (Exception ex) {
            status = 500;
        }
        StatusResponse response = new StatusResponse();
        response.statusCode = status;
        return response.asProcessing();
    }
}
