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
    class TestItem {

    private
        int testNumber = 0;
    private
        float testValue = 0;

    /**
     *
     * @param testNumber
     * @param testValue
     */
    public
        TestItem(int testNumber, float testValue) {
        this.testNumber = testNumber;
        this.testValue = testValue;
    }

    public
        String getDocValue() {
        return "," + FieldType.TestNumber + "=" + this.testNumber
            + "," + FieldType.TestValue + "=" + this.testValue;

    }

    public
        int getTestNumber() {
        return testNumber;
    }

}
