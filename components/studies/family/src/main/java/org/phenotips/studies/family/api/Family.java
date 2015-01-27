package org.phenotips.studies.family.api;

import org.xwiki.component.annotation.Role;

import java.util.Collection;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

@Role
public interface Family
{
    XWikiDocument getFamilyDoc(XWikiDocument patient) throws XWikiException;

    JSONObject getFamily(XWikiDocument doc);

    Collection<String> getRelatives(XWikiDocument patient) throws XWikiException;

    void storeFamily(XWikiDocument family, JSON familyContents) throws XWikiException;

    XWikiDocument createFamilyDoc(XWikiDocument patient) throws Exception;

    public JSONObject createBlankFamily();
}
