/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import java.util.ArrayList;
import java.util.HashMap;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author wsilen
 */
public
    class WatDie {

    private
        RawDie masterDie = null;
    private
        int seq = 0;
    final private
        ArrayList<TestItem> testItems = new ArrayList();
    final private
        ArrayList<RawDie> dies = new ArrayList();
    private
        int docCnt = 0;
    private static
        StringBuilder docValue = new StringBuilder();

    public
        WatDie(int x, int y, int seq) {
        this.masterDie = new RawDie(x, y, FieldType.MasterDie);
        this.dies.add(masterDie);
        this.seq = seq;
        this.addSlaveDie();
    }

    public
        ArrayList<TestItem> getTestItems() {
        return testItems;
    }

    private
        void addSlaveDie() {
        int[] y_offset = {-1, 0, 1};
        int x = getValidX(this.masterDie.getX());
        for (int offset : y_offset) {
            int y = getValidY(this.masterDie.getY(), offset);
            this.dies.add(new RawDie(x, y, FieldType.SlaveDie));
        }
    }

    private
        int getValidX(int x0) {
        return ((7 - x0 > 0) && (7 - x0 < 14)) ? (7 - x0) : -1;
    }

    private
        int getValidY(int y0, int offset) {
        return ((3 * y0 + 16 + offset > 0) && (3 * y0 + 16 + offset < 31)) ? (3 * y0 + 16 + offset) : -1;
    }

    public
        StringBuilder getDocValue(String lotHead, HashMap<Integer, ParameterDesc> parameterDescs) {
        docValue.setLength(0);
        for (TestItem item : this.testItems) {
            String testItemDocValue = lotHead + item.getDocValue();
            ParameterDesc testDesc = parameterDescs.get(item.getTestNumber());
            if (testDesc != null) {
                testItemDocValue += testDesc.getDocValue();
            }
            for (RawDie die : this.dies) {
                String dieDocValue = testItemDocValue + die.getDocValue() + "\n";
                if (Reader.validateFullForamtString(dieDocValue)) {
                    docValue.append(dieDocValue);
                    docCnt++;
                }
            }
        }
        return docValue;
    }

    public
        int getDocCnt() {
        return docCnt;
    }

}
