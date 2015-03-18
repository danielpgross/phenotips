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
package org.phenotips.studies.family;

import org.phenotips.Constants;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryException;

import java.util.Collection;

import javax.naming.NamingException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

@Role
public interface FamilyUtils
{
    final EntityReference FAMILY_CLASS =
        new EntityReference("FamilyClass", EntityType.DOCUMENT, Constants.CODE_SPACE_REFERENCE);

    XWikiDocument getFamilyDoc(XWikiDocument patient) throws XWikiException;

    JSONObject getFamilyRepresentation(XWikiDocument doc);

    Collection<String> getRelatives(XWikiDocument patient) throws XWikiException;

//    void storeFamilyRepresentation(XWikiDocument family, JSON familyContents) throws XWikiException;

    XWikiDocument createFamilyDoc(String patientId) throws NamingException, QueryException, XWikiException;

    XWikiDocument createFamilyDoc(XWikiDocument patient) throws NamingException, QueryException, XWikiException;

    public void processPatientPedigree(JSON json, String patientId) throws XWikiException;
}
