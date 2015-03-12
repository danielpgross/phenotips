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

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
@Named("family")
public class FamilyScriptService implements ScriptService
{
    @Inject
    FamilyUtils utils;

    @Inject
    Logger logger;

    /**
     *
     * @param thisPatient
     * @param otherId
     * @return true if the patients can be linked without additional input, false otherwise
     */
    public Boolean linkPatients(XWikiDocument thisPatient, String otherId)
    {
        return false;
    }

    public String createFamily(XWikiDocument patient)
    {
        try {
            return utils.createFamilyDoc(patient).getDocumentReference().getName();
        } catch (Exception ex) {
            logger.error("Could not create a new family document {}", ex.getMessage());
        }
        return "";
    }

    public String getPatientsFamily(XWikiDocument patient)
    {
        try {
            return utils.getFamilyDoc(patient).getDocumentReference().getName();
        } catch (XWikiException ex) {
            logger.error("Could not get patient's family {}", ex.getMessage());
        }
        return "";
    }
}
