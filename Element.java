package org.MDS;

// Элемент очереди
public class Element {
    
    private int itemID;
    private int groupID;

    public Element(int itemID, int groupID) {
        this.itemID = itemID;
        this.groupID = groupID;
    }

    public int getItemID() {
        return itemID;
    }

    public int getGroupID() {
        return groupID;
    }
}
