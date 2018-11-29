/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.smap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
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
    class SmapReader extends Reader {

    private final
        ArrayList<String> pickBins = new ArrayList();
    private final
        ArrayList<Die> dies = new ArrayList();
    private static
        SmapReader instance = null;

    private
        SmapReader(DataFormat format) {
        super(format);
    }

    public static
        SmapReader getInstance() {
        if (instance == null && Config.camFormat != null && Config.smapFormat.isEnabled()) {
            instance = new SmapReader(Config.smapFormat);
        }
        return instance;
    }

    @Override
    protected
        boolean readFile() {
        try {
            List<String> lines = Files.readAllLines(this.getFile().toPath());
            if (lines.isEmpty()) {
                System.out.println("Warning: there's no data in this file");
                return true;
            }
            int lineCnt = lines.size();
            int lineNo = -1;
            boolean mapStart = false;
            int ycoord = 0;
            while (++lineNo < lineCnt) {
                String content = lines.get(lineNo).trim();
                if (this.isDebugMode()) {
                    System.out.printf("line %d:%s\n", lineNo, content);
                }
                if (content.startsWith("PICK")) {
                    String[] temp = content.split(":");
                    if (temp.length != 2) {
                        System.out.println("Warning: there's no pick bins for this file");
                        return true;
                    }
                    readBins(temp[1]);
                    if (this.pickBins.isEmpty()) {
                        System.out.println("Warning: there's no pick bins for this file");
                        return true;
                    }
                }
                if (!mapStart) {
                    if ((!this.pickBins.isEmpty()) && (content.startsWith("_"))) {
                        mapStart = true;
                    }
                }
                if (mapStart) {
                    readMap(content, ycoord);
                    ycoord++;
                }
            }
            if (this.isDebugMode()) {
                System.out.println();
            }
        }
        catch (IOException ex) {
            Logger.getLogger(SmapReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    @Override
    protected
        void init() {
        this.dies.clear();
        this.pickBins.clear();

    }

    @Override
    protected
        boolean writeLogFile() {
        this.setUnitCnt(this.dies.size());
        this.setDocCnt(this.dies.size());
        String lotHead = this.generateLotHeadKVStr();
        int pickUnit = 0;

        for (Die die : this.dies) {
            boolean isPick = this.isPickBin(die.getPickBin());
            if (isPick) {
                pickUnit++;
            }
            String dieKVStr = this.getFormat().getUnit().getxCoordNode().getName() + "=" + die.getX()
                + "," + this.getFormat().getUnit().getyCoordNode().getName() + "=" + die.getY()
                + "," + FieldType.Type + "=" + FieldType.Unit
                + "," + "PPBin=" + die.getPickBin()
                + "," + "Bin=" + this.convertBin(die.getPickBin())
                + "," + "Pick=" + isPick;
            String docValue = lotHead + "," + dieKVStr + "\n";
            if (this.isDebugMode()) {
                System.out.print(docValue);
            }
            if (Reader.validateFullForamtString(docValue) && (!this.writeKVString(docValue))) {
                return false;
            }
        }

        // write the file
        String docValue = lotHead
            + "," + FieldType.Type + "=" + FieldType.File
            + "," + FieldType.UnitCnt + "=" + this.dies.size()
            + "," + FieldType.DocCnt + "=" + this.dies.size()
            + "," + FieldType.IsCaled + "=N"
            + "," + FieldType.PickUnitCnt + "=" + pickUnit
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
        void readMap(String content, int ycoord) {
        String[] names = content.split(" ");
        int size = names.length;
        int xcoord = 0;
        for (; xcoord != size; xcoord++) {
            if (validatePickBin(names[xcoord])) {
                Die die = new Die(xcoord, ycoord, names[xcoord]);
                this.dies.add(die);
            }
        }
    }

    protected
        void readBins(String content) {
        String[] bins = content.split(";");
        for (String bin : bins) {
            String pickBin = bin.trim();
            if (validatePickBin(pickBin)) {
                this.pickBins.add(bin.trim());
            }
            else {
                System.out.println("Warning: there's an invalid pick bin found: " + pickBin);
            }
        }
    }

    private
        boolean validatePickBin(String bin) {
        int size = bin.length();
        for (int i = 0; i != size; i++) {
            char chr = bin.charAt(i);
            if ((chr >= '0' && chr <= '9') || (chr >= 'A' && chr <= 'F')) {
            }
            else {
                return false;
            }
        }
        return true;
    }

    private
        int convertBin(String bin) {
        int size = bin.length();
        int index = 0;
        int value = 0;
        for (; index != size; index++) {
            char chr = bin.charAt(index);
            int codeValue = 0;
            switch (chr) {
                case 'A':
                    codeValue = 10;
                    break;
                case 'B':
                    codeValue = 11;
                    break;
                case 'C':
                    codeValue = 12;
                    break;
                case 'D':
                    codeValue = 13;
                    break;
                case 'E':
                    codeValue = 14;
                    break;
                case 'F':
                    codeValue = 15;
                    break;
                default:
                    String code = bin.substring(index, index + 1);
                    codeValue = Integer.valueOf(code);

            }

            int temp = 1;
            switch (size - index - 1) {
                case 0:
                    temp = 1;
                    break;
                case 1:
                    temp = 16;
                    break;
                case 2:
                    temp = 256;
                    break;
                case 3:
                    temp = 4096;
                    break;
                default:
                    temp = (int) Math.pow(16, size - index - 1);
            }

            value += codeValue * temp;
        }
        return value;
    }

    private
        boolean isPickBin(String bin) {
        if (this.pickBins.contains(bin)) {
            return true;
        }
        else {
            for (String pickBin : this.pickBins) {
                if (convertBin(pickBin) == convertBin(bin)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static
        void main(String[] args) {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        Config.smapFormat.setProductionMode(false);

        File testDataFile = new File("./testdata/extend/hygon_source_data/SMAP");
        for (File lotFile : testDataFile.listFiles()) {
            for (File file : lotFile.listFiles()) {
                SmapReader.getInstance().loadFile(file);
            }
        }

        System.out.println("total time = " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

    }

}
