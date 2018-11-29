/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author wsilen
 */
public
    class ParameterDesc {

    private
        int paramId = 0;
    private
        String paramName = null;
    private
        String group = null;
    private
        String loLimit = null;
    private
        String hiLimit = null;
    private
        String unit = null;

    /**
     *
     * @param paramId
     * @param paramName
     * @param group
     * @param LSL
     * @param USL
     * @param unit
     */
    public
        ParameterDesc(int paramId, String paramName, String group, String LSL, String USL, String unit) {
        this.paramId = paramId;
        this.paramName = paramName;
        this.group = group;
        this.loLimit = LSL;
        this.hiLimit = USL;
        this.unit = unit;
    }

    public
        String getDocValue() {
        return "," + FieldType.TestName + "=" + this.paramName
            + "," + FieldType.Group + "=" + this.group
            + "," + FieldType.LoLimit + "=" + this.loLimit
            + "," + FieldType.HiLimit + "=" + this.hiLimit
            + "," + FieldType.Units + "=" + this.unit;
    }

}
