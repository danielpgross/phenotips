/**
 * AbstractHoverbox is an abstract class for all the UI elements and graphics surrounding a node on the canvas (a Person
 * or a partnership). This includes the box that appears around the node when it's hovered by a mouse, as
 * well as the handles used for creating connections and creating new nodes.
 *
 * @class AbstractHoverbox
 * @constructor
 * @param {AbstractNode} node The node Person or Partnership for which the hoverbox is drawn
 * @param {Number} x The x coordinate for the hoverbox
 * @param {Number} y The y coordinate for the hoverbox
 * @param {Number} width The width in pixels
 * @param {Number} height The height in pixels
 * @param {Number} nodeX The x coordinate of the node for which the hoverbox is drawn
 * @param {Number} nodeY The y coordinate of the node for which the hoverbox is drawn
 * @param {Raphael.st} nodeShapes Raphaël set containing the graphical elements that make up the node
 */

var AbstractHoverbox = Class.create({

    initialize: function(node, shiftX, shiftY, width, height, nodeX, nodeY, nodeShapes) {
        //var timer = new Timer();        
        var me = this;
        this._node = node;
        this._relativeX = shiftX;
        this._relativeY = shiftY;
        this._nodeX = nodeX;
        this._nodeY = nodeY;
        this._hidden = true; 
        this._width = width;
        this._height = height;
        this._isHovered = false;
        this._orbs = editor.getPaper().set();
        this._connections = editor.getPaper().set();
        //var timer1 = new Timer();
        this._handles = this.generateHandles();
        //timer1.printSinceLast("=== abstract howerbox generate handles runtime: ");
        this._currentHandles = this._handles;
        this._buttons = this.generateButtons();
        //timer1.printSinceLast("=== abstract howerbox generate buttons runtime: ");
        this._boxOnHover = editor.getPaper().rect(this.getX(), this.getY(), this._width, this._height, 5).attr(PedigreeEditor.attributes.boxOnHover);
        this._backElements = editor.getPaper().set(this._boxOnHover, this._connections);
        this._backElements.insertBefore(nodeShapes.flatten());
        var mask = this._boxOnHover.clone().attr({fill: 'green', opacity: 0});
        this._frontElements = editor.getPaper().set().push(mask, this._buttons, this._orbs);
        this._frontElements.insertAfter(nodeShapes.flatten());
        this._frontElements.hover(function() {me.setHovered(true)}, function() {me.setHovered(false)});
        this.animateDrawHoverZone = this.animateDrawHoverZone.bind(this);
        this.animateHideHoverZone =  this.animateHideHoverZone.bind(this);
        this.hide();
        this.enable();
        //timer.printSinceLast("=== abstract howerbox runtime: ");
    },

    /**
     * Returns the x coordinate of the hoverbox
     *
     * @method getX
     * @return {Number} The x coordinate in pixels
     */
    getX: function() {        
        return this.getNodeX() + this._relativeX;
    },

    /**
     * Returns the y coordinate of the hoverbox
     *
     * @method getY
     * @return {Number} The y coordinate in pixels
     */
    getY: function() {
        return this.getNodeY() + this._relativeY;
    },

    /**
     * Returns the x coordinate of the attached node
     *
     * @method getNodeX
     * @return {Number} The x coordinate in pixels
     */
    getNodeX: function() {        
        // note: during construction getGraphics() isnot yet available, so need to store nodeX.
        //       however node may have been moved later, in which case we need to use current graphics X
        var nodeGraphics = this.getNode().getGraphics();
        if (nodeGraphics)
            this._nodeX = nodeGraphics.getX();        
        return this._nodeX;
    },

    /**
     * Returns the y coordinate of the attached node
     *
     * @method getNodeY
     * @return {Number} The y coordinate in pixels
     */
    getNodeY: function() {
        var nodeGraphics = this.getNode().getGraphics();
        if (nodeGraphics)
            this._nodeY = nodeGraphics.getY();          
        return this._nodeY;
    },

    /**
     * Returns the width of the hoverbox
     *
     * @method getWidth
     * @return {Number} The width in pixels
     */
    getWidth: function() {
        return this._width;
    },

    /**
     * Returns the height of the hoverbox
     *
     * @method getHeight
     * @return {Number} The height in pixels
     */
    getHeight: function() {
        return this._height;
    },

    /**
     * Returns the node for which the hoverbox is drawn
     *
     * @method getNode
     * @return {AbstractNode} Can be either a Partnership or a Person
     */
    getNode: function() {
        return this._node;
    },

    /**
     * Creates the buttons used in this hoverbox
     *
     * @method generateButtons
     * @return {Raphael.st} A set of buttons
     */
    generateButtons: function() {
        return editor.getPaper().set();
    },

    /**
     * Returns Raphael set of the buttons in this hoverbox
     *
     * @method getButtons
     * @return {Raphael.st} A set of buttons
     */
    getButtons: function() {
        return this._buttons;
    },

    /**
     * Creates the handles used in this hoverbox
     *
     * @method generateHandles
     * @return {Raphael.st} A set of handles
     */
    generateHandles: function() {
        return editor.getPaper().set();
    },

    /**
     * Returns a Raphael set of the currently visible handles
     *
     * @method getCurrentHandles
     * @return {Raphael.st} A set of handles
     */
    getCurrentHandles: function() {
        return this._currentHandles;
    },

    /**
     * Returns the a Raphael set containing the four draggable handles
     *
     * @method getHandles
     * @return {Raphael.st} A set of handles
     */
    getHandles: function() {
        return this._handles;
    },

    /**
     * Generates a button and places it on the hoverbox
     *
     * @method createButton
     * @param {Number} x The x coordinate of the button
     * @param {Number} y The y coordinate of the button
     * @param {String|Array} svgPath The svg path for the button
     * @param attributes The svg attributes
     * @param {Function} onClick Callback for the button
     * @param {String} className The class attribute for the button
     *
     * @return {Raphael.st} The generated button
     */
    createButton: function(x, y, svgPath, attributes, onClick, className, title) {
        var iconScale = PedigreeEditor.attributes.radius * 0.014,
            icon = editor.getPaper().path(svgPath).attr(attributes);

        icon.transform(["t" , x , y, "s", iconScale, iconScale, 0, 0]);      // <--- TODO: extremely SLOW        
        var mask = editor.getPaper().rect(icon.getBBox().x, icon.getBBox().y,
            icon.getBBox().width, icon.getBBox().height, 1);        
        mask.attr({fill: 'gray', opacity: 0, "stroke-width" : 0}).transform("s1.5");
        var button = editor.getPaper().set(mask, icon);
        var me = this;
        var clickFunct = function() {
            onClick && onClick();            
            if (me._hidden) {
                button.isClicked = false;
                return;
            }
            button.isClicked = !button.isClicked;            
            if(button.isClicked) {                
                mask.attr(PedigreeEditor.attributes.btnMaskClick);
            }
            else {
                mask.attr(PedigreeEditor.attributes.btnMaskHoverOn);
            }
        };
        button.click(clickFunct);
        button.mousedown(function(){mask.attr(PedigreeEditor.attributes.btnMaskClick)});
        button.hover(function() {
                mask.attr(PedigreeEditor.attributes.btnMaskHoverOn);
                if (title)
                    mask.attr({"title": title});
            },
            function() {
                mask.attr(PedigreeEditor.attributes.btnMaskHoverOff);
            });
        className && button.forEach(function(element) {
            element.node.setAttribute('class', className);
        });
        button.icon = icon;
        return button;
    },

    /**
     * Creates a show-menu button
     *
     * @method generateMenuBtn
     * @return {Raphael.st} The generated button
     */
    generateMenuBtn: function() {
        var me = this;
        var action = function() {
            me.toggleMenu(!me.isMenuToggled());
        };
        var path = "M2.021,9.748L2.021,9.748V9.746V9.748zM2.022,9.746l5.771,5.773l-5.772,5.771l2.122,2.123l7.894-7.895L4.143,7.623L2.022,9.746zM12.248,23.269h14.419V20.27H12.248V23.269zM16.583,17.019h10.084V14.02H16.583V17.019zM12.248,7.769v3.001h14.419V7.769H12.248z";
        var attributes = PedigreeEditor.attributes.menuBtnIcon;
        var x = this.getX() + this.getWidth() - 18 - this.getWidth()/40;
        var y = this.getY() + this.getHeight()/40;
        return this.createButton(x, y, path, attributes, action, "menu-trigger", "node properties");
    },
    /**
     * Creates and returns a delete button (big red X).
     *
     * @method generateDeleteBtn
     * @return {Raphael.st} the generated button
     */
    generateDeleteBtn: function() {
        var me = this;
        var action = function() {
            me.hide();
            var event = { "nodeID": me.getNode().getID() };
            document.fire("pedigree:node:remove", event);            
        };
        var path = "M24.778,21.419 19.276,15.917 24.777,10.415 21.949,7.585 16.447,13.087 10.945,7.585 8.117,10.415 13.618,15.917 8.116,21.419 10.946,24.248 16.447,18.746 21.948,24.248z";
        var attributes = PedigreeEditor.attributes.deleteBtnIcon;
        var x = this.getX() + this.getWidth()/40;
        var y = this.getY() + this.getHeight()/40;
        return this.createButton(x, y, path, attributes, action, "delete", "remove node");
    },

    /**
     * Returns the gray box that appears when the node is hovered
     *
     * @method getBoxOnHover
     * @return {Raphael.el} Raphael rectangle element
     */
    getBoxOnHover: function() {
        return this._boxOnHover;
    },

    /**
     * Returns true box if the hoverbox is currently hovered
     *
     * @method isHovered
     * @return {Boolean} Raphael rectangle element
     */
    isHovered: function() {
        return this._isHovered;
    },

    /**
     * Sets the hovered property to isHovered.
     * @method setHovered
     * @param {Boolean} isHovered Set to true if the box is hovered
     */
    setHovered: function(isHovered) {
        this._isHovered = isHovered;
    },
    
    /**
     * Enbales or disables the highlighting of the node
     * @method setHighlighted
     * @param {Boolean} isHighlighted Set to true enables green highlight box, false disables it
     */
    setHighlighted: function(isHighlighted) {
        // autoimaticaly highlight and unhighlight the node being dragged over
        if(isHighlighted) {
            this.getBoxOnHover().attr(PedigreeEditor.attributes.boxOnHover);                    
            this.getBoxOnHover().attr("fill", "green");
        }
        else {
            this.getBoxOnHover().attr(PedigreeEditor.attributes.boxOnHover).attr('opacity', 0);            
        }
    },    

    /**
     * Returns the invisible mask layer in front of the hoverbox
     *
     * @method getHoverZoneMask
     * @return {Raphael.el} Raphael rectangle
     */
    getHoverZoneMask: function() {
        return this.getFrontElements()[0];
    },

    /**
     * Returns a Raphael set containing all hoverbox elements that are layered
     * in front of the node graphics
     *
     * @method getFrontElements
     * @return {Raphael.st} set of Raphael elements
     */
    getFrontElements: function() {
        return this._frontElements;
    },

    /**
     * Returns a Raphael set containing all hoverbox elements that are layered
     * behind of the node graphics
     *
     * @method getBackElements
     * @return {Raphael.st} set of Raphael elements
     */
    getBackElements: function() {
        return this._backElements;
    },

    /**
     * Creates a handle with a blue orb from the center of the node and places it behind the node icon
     *
     * @method generateHandle
     * @param {String} type Should be 'parent', 'child' or 'partner'
     * @param {Number} orbX The x coordinate of the orb
     * @param {Number} orbY The y coordinate of the orb
     * @return {Raphael.st} Raphael set of elements that make up the handle
     */
    generateHandle: function(type, orbX, orbY, title) {
        var path = [["M", this.getNodeX(), this.getNodeY()],["L", orbX, orbY]];
        var connection   = editor.getPaper().path(path).attr({"stroke-width": 4, stroke: "gray"});
        connection.oPath = path;
        
        var orbRadius = PedigreeEditor.attributes.radius/7;
        var orbHue    = PedigreeEditor.attributes.orbHue;
        var orb       = generateOrb(editor.getPaper(), orbX, orbY, orbRadius*1.1, orbHue).attr("cursor", "pointer");
        
        //var adoptionPath = [["M", orbX - orbRadius/2, orbY + orbRadius*1.3],["L", (orbX - orbRadius * 1.3), orbY + orbRadius*1.3],["L", (orbX - orbRadius * 1.3), orbY - orbRadius*1.3],["L", (orbX - orbRadius / 2), orbY - orbRadius*1.3],["M", orbX + orbRadius/2, orbY + orbRadius*1.3],["L", (orbX + orbRadius * 1.3), orbY + orbRadius*1.3],["L", (orbX + orbRadius * 1.3), orbY - orbRadius*1.3],["L", (orbX + orbRadius / 2), orbY - orbRadius*1.3]],
        //var adoptionSymbol = editor.getPaper().path(adoptionPath).attr({"stroke-width": 2, stroke: "#484848", opacity: 1});
        //orb.push(adoptionSymbol);
        
        var handle  = editor.getPaper().set().push(connection, orb);
        handle.type = type;
        
        var me = this;                

        var start = function() {
        	console.log("handle: start");
            me.disable();
            me.getFrontElements().toFront();
            orb.ox = orb[0].attr("cx");
            orb.oy = orb[0].attr("cy");
            connection.ox = connection.oPath[1][1];
            connection.oy = connection.oPath[1][2];
            handle.isDragged = false;
            editor.getGraphicsSet().setCurrentDraggable(me.getNode().getID());
            // highlight valid targets (after a small delay - so that nothing gets annoyingly highlighted
            // and instantly un-highlighted if the person just clicks the orb without dragging)
            setTimeout(function() { if (editor.getGraphicsSet().getCurrentDraggable() !== null)
                                        editor.getGraphicsSet().enterHoverMode(me.getNode(), type);
                                  }, 100);
        };
        var move = function(dx, dy) {
        	//console.log("handle: move");
            dx = dx/editor.getWorkspace().zoomCoefficient;
            dy = dy/editor.getWorkspace().zoomCoefficient;
            orb.attr("cx", orb.ox + dx);
            orb.attr("cy", orb.oy + dy);
            connection.oPath[1][1] = connection.ox + dx;
            connection.oPath[1][2] = connection.oy + dy;
            connection.attr("path", connection.oPath);
            if(dx > 1 || dx < 1 || dy > 1 || dy < -1 ) {
                handle.isDragged = true;
            }            
            //console.log("currentHover: " + editor.getGraphicsSet()._currentHoveredNode + ", currentDrag: " + editor.getGraphicsSet()._currentDraggable);
        };
        var end = function() {
            var curHoveredId = editor.getGraphicsSet().getCurrentHoveredNode()
            
            editor.getGraphicsSet().setCurrentDraggable(null);
            editor.getGraphicsSet().exitHoverMode();            
            
            if(handle.isDragged)
                orb.animate({"cx": orb.ox, "cy": orb.oy}, + handle.isDragged * 1000, "elastic",
                            function() { me.animateHideHoverZone(); });
            
            me.enable();            
            console.log("handle.isDragged: " + handle.isDragged + ", currentHover: " + curHoveredId);                       
            connection.oPath[1][1] = connection.ox;
            connection.oPath[1][2] = connection.oy;
            connection.animate({"path": connection.oPath},1000, "elastic");
                        
            me.handleAction(handle.type, handle.isDragged, curHoveredId);
        };

        orb.drag(move, start, end);
        orb.hover(function() {
                orb[0].attr({fill: "r(.5,.9)hsb(" + (orbHue + .36) + ", 1, .75)-hsb(" + (orbHue + .36) + ", .5, .25)", stroke: "none"});
                //if (title)
                //    orb[0].attr({"title": title});                
                //orb[0].attr({fill: "r(.5,.9)hsb(" + orbHue +.7 + ", 1, .75)-hsb(" + orbHue + .7 + ", .5, .25)"})
            },
            function () {
                orb[0].attr({fill: "r(.5,.9)hsb(" + orbHue + ", 1, .75)-hsb(" + orbHue + ", .5, .25)"})
            });
        this._orbs.push(orb);
        this._connections.push(connection);

        handle.getType = function() {
            return type;
        };
        return handle;
    },

    /**
     * Hides the child handle
     *
     * @method hideChildHandle
     */
    hideChildHandle: function() {
        this.getCurrentHandles().exclude(this._downHandle.hide());
    },

    /**
     * Unhides the child handle
     *
     * @method unhideChildHandle
     */
    unhideChildHandle: function() {
        if(this.isHovered() || this.isMenuToggled()) {
            this._downHandle.show();
        }
        (!this.getCurrentHandles().contains(this._downHandle)) && this.getCurrentHandles().push(this._downHandle);
    },

    /*
     * Fades the hoverbox graphics in
     *
     * @method animateDrawHoverZone
     */
    animateDrawHoverZone: function() {     
        this._hidden = false;
        if (editor.getGraphicsSet().getCurrentDraggable()) return;
        
        this.getNode().getGraphics().setSelected(true);
        this.getBoxOnHover().stop().animate({opacity:0.7}, 300);
        this.getButtons().forEach(function(button) {
            button.icon.stop().animate({opacity:1}, 300);
        });
        this.getCurrentHandles().show();
    },

    /**
     * Fades the hoverbox graphics out
     *
     * @method animateHideHoverZone
     */
    animateHideHoverZone: function() {        
        if (editor.getGraphicsSet().getCurrentDraggable()) return;
        
        this.getNode().getGraphics().setSelected(false);
        this.getBoxOnHover().stop().animate({opacity:0}, 200);
        this.getButtons().forEach(function(button) {
            button.icon.stop().animate({opacity:0}, 200);
        });
        this.getCurrentHandles().hide();
    },

    /**
     * Hides the hoverbox's graphical elements
     *
     * @method hide
     */
    hide: function() {
        this._hidden = true;
        this.getBoxOnHover().attr({opacity:0});
        this.getButtons().forEach(function(button) {
            button.icon.attr({opacity:0});
        });
        this.getHandles().hide();
    },

    /**
     * Stops the hoverbox from responding to mouseovers
     *
     * @method disable
     */
    disable: function() {
        this.getFrontElements().unhover(this.animateDrawHoverZone, this.animateHideHoverZone);
    },

    /**
     * Attaches onMouseOver behavior to the hoverbox
     *
     * @method enable
     */
    enable: function() {
        this.getFrontElements().hover(this.animateDrawHoverZone, this.animateHideHoverZone);
    },

    /**
     * Deletes the hoverbox
     *
     * @method remove
     */
    remove: function() {
        this.disable();
        this.getBackElements().remove();
        this.getFrontElements().remove();
    },

    /**
     * Updates the hoverbox behavior after a widget (like the menu) is closed
     *
     * @method onWidgetHide
     */
    onWidgetHide: function() {
        this._isMenuToggled = false;
        !this.isHovered() && this.animateHideHoverZone();
        this.enable();
    },
    
    onWidgetShow: function() {
        this._isMenuToggled = true;
    }        
});
