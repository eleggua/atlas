//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.09.22 at 12:06:36 PM BST 
//


package org.atlasapi.remotesite.pa.bindings;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "pictureUsage"
})
@XmlRootElement(name = "pictures")
public class Pictures {

    @XmlElement(required = true)
    protected List<PictureUsage> pictureUsage;

    /**
     * Gets the value of the pictureUsage property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the pictureUsage property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPictureUsage().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PictureUsage }
     * 
     * 
     */
    public List<PictureUsage> getPictureUsage() {
        if (pictureUsage == null) {
            pictureUsage = new ArrayList<PictureUsage>();
        }
        return this.pictureUsage;
    }

}
