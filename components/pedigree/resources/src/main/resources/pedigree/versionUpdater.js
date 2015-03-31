/* 
 * VersionUpdater is responsible for updating pedigree JSON represenatation to the current version.
 */
VersionUpdater = Class.create( {
    initialize: function() {
        this.availableUpdates = [ { "comment":    "group node comment representation",
                                    "introduced": "May2014",
                                    "func":       "updateGroupNodeComments"},
                                  { "comment":    "adopted status",
                                    "introduced": "Nov2014",
                                    "func":       "updateAdoptedStatus"},
                                  { "comment":    "proband link",
                                    "introduced": "Mar 2015",
                                    "func":       "updateNode0ProbandLink"}
                                ];
    },

    updateToCurrentVersion: function(pedigreeJSON) {
        for (var i = 0; i < this.availableUpdates.length; i++) {
            var update = this.availableUpdates[i];
            
            var updateResult = this[update.func](pedigreeJSON);
            
            if (updateResult !== null) {
                console.log("[update #" + i + "] [updating to " + update.introduced + " version] - performing " + update.comment + " update");
                pedigreeJSON = updateResult;
            }
        }
        
        return pedigreeJSON;
    },

    /* - assumes input is in the pre-May-2014 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateGroupNodeComments: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];
            
            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("numPersons") && !node.prop.hasOwnProperty("comments") && node.prop.hasOwnProperty("fName") && node.prop.hasOwnProperty("fName") != "") {
                    node.prop["comments"] = node.prop.fName;
                    delete node.prop.fName;
                    change = true;
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);
    },

    /* - assumes input is in the pre-Nov-2014 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateAdoptedStatus: function(pedigreeJSON) {
        var change = false;
        var data = JSON.parse(pedigreeJSON);
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("isAdopted") ) {
                    if (node.prop.isAdopted) {
                        node.prop["adoptedStatus"] = "adoptedIn";
                    }
                    delete node.prop.isAdopted;
                    change = true;
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);
    },

    /* - assumes input is in the pre-Mar-2015 format
     * - returns null if there were no changes; returns new JSON if there was a change
     */
    updateNode0ProbandLink: function(pedigreeJSON) {
        // check if at least one node is linked to the current patient. 
        // Iff none are, assumenode 0 is the proband and link it to the patient

        if (editor.isFamilyPage()) {
            return null;
        }

        var currentPatient = XWiki.currentDocument.page;

        var data = JSON.parse(pedigreeJSON);

        //look through all person nodes for a node linked to the current patient
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.hasOwnProperty("prop")) {
                if (node.prop.hasOwnProperty("phenotipsId") ) {
                    if (node.prop.phenotipsId == currentPatient) {
                        return null;
                    }
                }
            }
        }

        var change = false;

        // assign node with id = 0 to be the proband
        for (var i = 0; i < data.GG.length; i++) {
            var node = data.GG[i];

            if (node.id == 0) {
                if (!node.hasOwnProperty("prop")) {
                    node.prop = {};
                }
                if (node.prop.hasOwnProperty("phenotipsId")) {
                    alert("Loaded pedigree is inconsistent - assumed proband node is linked to a different patient");
                } else {
                    node.prop.phenotipsId = currentPatient;
                    change = true;
                    break;
                }
            }
        }

        if (!change)
            return null;

        return JSON.stringify(data);
    }
});
