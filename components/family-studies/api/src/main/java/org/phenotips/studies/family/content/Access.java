package org.phenotips.studies.family.content;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Collection of checks for checking if certain actions are allowed.
 * Needs to be split up really, but later.
 */
public class Access
{
    public boolean canAddToFamily(XWikiDocument patient, XWikiContext context) {
        return true;
    }
}
