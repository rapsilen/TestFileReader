/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.wat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;

/**
 *
 * @author ghfan
 */
final public
    class WatReader extends Reader {

    private final
        HashMap<Integer, ParameterDesc> parameterDescs = new HashMap<>();
    private final
        ArrayList<WatDie> dies = new ArrayList();
    private final
        String splitStr = "\\|";
    private
        boolean emptyDataFile = false;
    private
        WatDie currentDie = null;
    private static
        WatReader instance = null;

    private
        WatReader(DataFormat format) {
        super(format);
    }

    public static
        WatReader getInstance() {
        if (instance == null && Config.camFormat != null && Config.watFormat.isEnabled()) {
            instance = new WatReader(Config.watFormat);
        }
        return instance;
    }

    @Override
    protected
        void init() {
        this.dies.clear();
        this.parameterDescs.clear();
        this.emptyDataFile = false;
        this.currentDie = null;
    }

    @Override
    protected
        boolean readFile() {
        return this.readDataFile()
            && (this.emptyDataFile ? true : this.readLimitFile());
    }

    private
        boolean readDataFile() {
        try {
            List<String> lines = Files.readAllLines(this.getFile().toPath());
            if (lines.isEmpty()) {
                System.out.println("Warning: there's no data in this file");
                this.emptyDataFile = true;
                return true;
            }
            int lineCnt = lines.size();
            int lineNo = -1;
            boolean testStart = false;
            boolean testerStart = false;
            String[] currentTestPointStr = new String[3];
            while (++lineNo < lineCnt) {
                String content = lines.get(lineNo).trim();
                if (this.isDebugMode()) {
                    System.out.printf("line %d:%s\n", lineNo, content);
                }
                // Load HW info
                if (content.equals("START SECTION WET_EQUIPMENT")) {
                    testerStart = true;
                }
                else {
                    if (content.equals("FINISH SECTION WET_EQUIPMENT")) {
                        testerStart = false;
                    }
                }
                if (testerStart) {
                    if (this.validParamValContent(content)) {
                        String[] hwStr = content.split(splitStr);
                        Config.watFormat.getWatFieldNode()
                            .forEach(node -> {
                                if (node.getCamColumnName().equals(hwStr[1])) {
                                    node.resetValue();
                                    node.setValue(hwStr[0]);
                                }
                            });
                    }
                }

                // Load Test info
                if (content.equals("START SECTION WET_SITE")) {
                    testStart = true;
                    this.currentDie = null;
                }
                else {
                    if (content.equals("FINISH SECTION WET_SITE")) {
                        testStart = false;
                        this.currentDie = null;
                    }
                }
                if (testStart) {
                    this.readTestData(content);
                }
            }
        }
        catch (IOException ex) {
            Logger.getLogger(WatReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        this.setUnitCnt(this.dies.size());
        return true;
    }

    private
        void readTestData(String content) {
        String[] testItemStr = content.split(splitStr);
        try {
            if (testItemStr.length == 3) {
                // x/y/seq case
                this.currentDie = new WatDie(
                    Integer.valueOf(testItemStr[0]), // Xcoord
                    Integer.valueOf(testItemStr[1]), // Ycoord
                    Integer.valueOf(testItemStr[2]) // Seq
                );
                this.dies.add(currentDie);
            }
            else if (testItemStr.length == 2 && this.currentDie != null) {
                // paramId/testValue case
                int paramId = Integer.valueOf(testItemStr[0]);
                float testVal = Float.valueOf(testItemStr[1]);
                if (Float.isNaN(testVal) || Float.isInfinite(testVal)) {
                }
                else {
                    this.currentDie.getTestItems().add(new TestItem(paramId, testVal));
                }

            }
        }
        catch (NumberFormatException e) {
            System.out.println("Warning: failed to parse Number String: " + content);
        }

    }

    private
        boolean readLimitFile() {
        try {
            File limitFile = new File(this.getFile().getAbsolutePath().replaceAll("data.dis", "limit.dis"));
            if (!limitFile.exists()) {
                System.out.println("Warning: there's no limit file");
                return true;
            }
            List<String> lines = Files.readAllLines(limitFile.toPath());
            if (lines.isEmpty()) {
                System.out.println("Warning: there's no limit data in the limit file");
                return true;
            }

            int lineCnt = lines.size();
            int lineNo = -1;
            boolean mapStart = false;
            while (++lineNo < lineCnt) {
                String content = lines.get(lineNo).trim();
                if (this.isDebugMode()) {
                    System.out.printf("line %d:%s\n", lineNo, content);
                }
                if (content.equals("START SECTION WET_LIMIT")) {
                    mapStart = true;
                }
                if (mapStart) {
                    this.readTestDesc(content);
                }
            }
        }
        catch (IOException ex) {
            Logger.getLogger(WatReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private
        void readTestDesc(String content) {
        String[] descStrs = content.split(splitStr);
        if (descStrs.length == 12) {
            try {
                ParameterDesc paramDesc = new ParameterDesc(
                    Integer.valueOf(descStrs[0]), //ParamId
                    descStrs[1].replace('=', ':').replace(',', ';'), // ParamName
                    descStrs[2].replace('=', ':').replace(',', ';'), // Group
                    descStrs[3],
                    descStrs[4],
                    descStrs[9].replace('=', ':').replace(',', ';') // unit
                );
                this.parameterDescs.putIfAbsent(Integer.valueOf(descStrs[0]), paramDesc);
            }
            catch (NumberFormatException e) {
                System.out.println("Warning: failed to parse test desc Number String: " + content);
            }
        }

    }

    @Override
    protected
        boolean writeLogFile() {
        this.setUnitCnt(this.dies.size());
        String lotHead = this.generateLotHeadKVStr();

        for (WatDie die : this.dies) {
            if (!this.writeKVString(die.getDocValue(lotHead, parameterDescs).toString())) {
                return false;
            }
            else {
                this.setDocCnt(this.getDocCnt() + die.getDocCnt());
            }
        }

        // write the file
        String docValue = lotHead
            + "," + FieldType.Type + "=" + FieldType.File
            + "," + FieldType.UnitCnt + "=" + this.getUnitCnt()
            + "," + FieldType.DocCnt + "=" + this.getDocCnt()
            + "," + FieldType.DataType + "=" + this.getFormat().getDataType()
            + "," + FieldType.SourceType + "=" + this.getFormat().getSourceType()
            + "," + FieldType.TransferTime + "=" + this.getTransferTime()
            + "\n";
        if (Reader.validateFullForamtString(docValue)) {
            return this.writeKVString(docValue);
        }
        return true;

    }

    private
        boolean validParamValContent(String content) {
        return (content.split(splitStr).length == 2);
    }

    public static
        void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        Config.watFormat.setProductionMode(false);

        File testDataFile = new File(Config.watFormat.getKdfPath());
        for (File lotFile : testDataFile.listFiles()) {
            for (File file : lotFile.listFiles()) {
                WatReader.getInstance().loadFile(file);
            }
        }

        System.out.println("total time = " + (System.currentTimeMillis() - startTime));

    }

}
