/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
public
    class RawDie {

    private
        int x = 0;
    private
        int y = 0;
    private
        String dieType = null;

    public
        RawDie(int x, int y, String dieType) {
        this.x = x;
        this.y = y;
        this.dieType = dieType;
    }

    public
        int getX() {
        return x;
    }

    public
        int getY() {
        return y;
    }

    public
        String getDocValue() {
        if (this.dieType.equals(FieldType.MasterDie)) {
            return "," + FieldType.X0 + "=" + this.x
                + "," + FieldType.Y0 + "=" + this.y
                + "," + FieldType.DieType + "=" + this.dieType;
        }
        else {
            return "," + Config.watFormat.getUnit().getxCoordNode().getName() + "=" + this.x
                + "," + Config.watFormat.getUnit().getyCoordNode().getName() + "=" + this.y
                + "," + FieldType.DieType + "=" + this.dieType;
        }
    }

}
