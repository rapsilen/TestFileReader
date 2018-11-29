/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template dateFile, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.reader.camstar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.himalayas.filereader.reader.Reader;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.XmlNode;

/**
 *
 * @author ghfan
 */
public
    class CamstarReader extends Reader {

    private
        Workbook workbook = null;
    private
        Sheet sheet = null;
    private final
        HashMap<String, Integer> nameColumns = new HashMap();
    private final
        ArrayList<Integer> validColumns = new ArrayList();
    private
        StringBuilder camDocs = new StringBuilder();
    private
        int goodLotCnt = 0;
    private
        int badLotCnt = 0;
    private final
        SimpleDateFormat fmtDateTime1 = new SimpleDateFormat("yyyyMMddHHmmss");
    private static
        CamstarReader instance = null;

    private
        CamstarReader(DataFormat format) {
        super(format);
    }

    public static
        CamstarReader getInstance() {
        if (instance == null && Config.camFormat != null && Config.camFormat.isEnabled()) {
            instance = new CamstarReader(Config.camFormat);
        }
        return instance;
    }

    @Override
    protected
        void init() {
        this.workbook = null;
        this.sheet = null;
        this.nameColumns.clear();
        this.validColumns.clear();
        this.camDocs.setLength(0);
        this.goodLotCnt = 0;
        this.badLotCnt = 0;

    }

    @Override
    protected
        boolean readFile() {
        try {
            this.workbook = WorkbookFactory.create(this.getFile());

            if (this.workbook == null) {
                return false;
            }

            this.sheet = workbook.getSheet("Sheet1");
            if (this.sheet == null) {
                System.out.println("Fatal Error: there's no default sheet1 in this file");
                return false;
            }
            this.readSheet();

        }
        catch (IOException | InvalidFormatException | EncryptedDocumentException ex) {
            java.util.logging.Logger.getLogger(CamstarReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    @Override
    protected
        boolean writeLogFile() {

        // write the file
        String docValue = FieldType.Type + "=" + FieldType.File
            + "," + FieldType.CamGoodCnt + "=" + this.goodLotCnt
            + "," + FieldType.CamBadCnt + "=" + this.badLotCnt
            + "," + FieldType.KdfName + "=" + this.getFileName()
            + "," + FieldType.DataType + "=" + this.getFormat().getDataType()
            + "," + FieldType.SourceType + "=" + this.getFormat().getSourceType()
            + "," + FieldType.KdfDate + "=" + this.getFileDate()
            + "," + FieldType.KdfMonth + "=" + this.getFileMonth()
            + "," + FieldType.TransferTime + "=" + this.getTransferTime()
            + "\n";
        return (this.writeKVString(camDocs.toString())
            && this.writeKVString(docValue));
    }

    private
        boolean readSheet() {
        int firstRowNo = this.sheet.getFirstRowNum();
        int lastRowNo = this.sheet.getLastRowNum();

        Row headRow = this.sheet.getRow(firstRowNo);

        int firstColNo = headRow.getFirstCellNum();
        int lastColNo = headRow.getLastCellNum() - 1;

        for (int colNo = firstColNo; colNo <= lastColNo; colNo++) {
            Cell cell = headRow.getCell(colNo);
            if (cell != null && ("STRING").equalsIgnoreCase(cell.getCellTypeEnum().name())) {
                String headName = cell.getStringCellValue().trim();
                if (!headName.isEmpty()) {
                    this.nameColumns.put(headName, colNo);
                    this.validColumns.add(colNo);
                }
            }
        }

        for (int rowNo = firstRowNo + 1; rowNo <= lastRowNo; rowNo++) {

            LinkedHashMap<String, String> line = new LinkedHashMap<>();
            boolean lineHasError = false;
            Row row = sheet.getRow(rowNo);

            for (XmlNode xmlNode : this.getFormat().getLotHead().values()) {
                if (xmlNode.isEnabled() && xmlNode.isEnabledLog() && xmlNode.getCamColumnName() != null) {
                    xmlNode.resetValue();
                    if (this.nameColumns.containsKey(xmlNode.getCamColumnName())) {
                        int colNo = this.nameColumns.get(xmlNode.getCamColumnName());
                        Cell cell = row.getCell(colNo);
                        if (cell != null) {
                            String cellValue = getCellValue(cell, rowNo, colNo, xmlNode.isTimeNode());
                            xmlNode.setValue(cellValue);
                        }
                    }
                    else {
                        System.out.printf("Warning: this camstar xml node %s is not in the=is xls report file\n", xmlNode.getCamColumnName());
                    }
                }
            }
            String camLot = this.getFormat().getLotNumberNode().getXmlValue();
            String camOper = this.getFormat().getOperationNode().getXmlValue();

            // invalid row whose camLot/camOper is empty
            if (camLot.isEmpty() || camOper.isEmpty()) {
                System.out.println("Warning: there's no camLot/camOper in this row, rowNo = " + rowNo);
                this.badLotCnt++;
                continue;
            }
            // add 2 additinal field camDate and camMonth
            // add camDate to terms by Date and camMonth to terms by Month
            // date is renamed to camTime
            String camDateKVString = "";
            String camMonthKVString = "";
            XmlNode camTimeNode = this.getFormat().getCamDateNode();
            String camTime = camTimeNode.getXmlValue();
            if ((!camTime.isEmpty()) && camTime.length() == 14) {
                String camDate = camTime.substring(0, 8);
                String camMonth = camTime.substring(0, 6);
                camDateKVString = "," + FieldType.CamDate + "=" + camDate;
                camMonthKVString = "," + FieldType.CamMonth + "=" + camMonth;
            }

            this.goodLotCnt++;
            String docIdKVString = "," + FieldType.Lot_Doc_id + "=" + this.getCamDocId(camLot, camOper);
            String docValue = this.generateLotHeadKVStr() + docIdKVString + camDateKVString + camMonthKVString + "\n";
            if (Reader.validateFullForamtString(docValue)) {
                this.camDocs.append(docValue);
            }
        }
        return true;
    }

    private
        String getCamDocId(String lotNumber, String oper) {
        return lotNumber + "_" + oper;
    }

    private
        String
        getCellValue(Cell cell, int rowNo, int colNo, boolean isTimeNode) {
        String cellValue = "";
        String cellType = cell.getCellTypeEnum().name().toUpperCase();

        if ("BLANK".equals(cellType)) {
            return cellValue;
        }

        switch (cellType) {
            case "STRING":
                cellValue = cell.getStringCellValue().trim();
                break;

            case "NUMERIC":
                if (isTimeNode) {
                    cellValue = fmtDateTime1.format(cell.getDateCellValue()).toString();
                }
                else {
                    cellValue = String.valueOf(cell.getNumericCellValue());
                    if (cellValue.endsWith(".0")) {
                        cellValue = cellValue.substring(0, cellValue.length() - 2);
                    }
                }
                break;

            default:
                System.out.println("Warning: unsupportted xls date type found: " + cellType);
                break;
        }
        return cellValue;
    }

    @Override
    protected
        void logRepeatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFRepeat,
            FieldType.KdfName, this.getFileName(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType(),
            FieldType.SourceType, this.getFormat().getSourceType()
        );
    }

    @Override
    protected
        void logOpenFailureToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFOpenFailure,
            FieldType.KdfName, this.getFileName(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType(),
            FieldType.SourceType, this.getFormat().getSourceType()
        );
    }

    @Override
    protected
        void logIoErrorToES(String error) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.IOError,
            FieldType.Failure, error,
            FieldType.KdfName, this.getFileName(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType(),
            FieldType.SourceType, this.getFormat().getSourceType()
        );
    }

    @Override
    protected
        void logExceptionToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFException,
            FieldType.KdfName, this.getFileName(),
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType(),
            FieldType.SourceType, this.getFormat().getSourceType()
        );
    }

    @Override
    protected
        void logFileDoneToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFDone,
            FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
            FieldType.KdfName, this.getFileName(),
            FieldType.CamGoodCnt, this.goodLotCnt,
            FieldType.CamBadCnt, this.badLotCnt,
            FieldType.KdfMonth, this.getFileMonth(),
            FieldType.KdfDate, this.getFileDate(),
            FieldType.TransferTime, this.getTransferTime(),
            FieldType.DataType, this.getFormat().getDataType(),
            FieldType.SourceType, this.getFormat().getSourceType()
        );
    }

    public static
        void main(String[] args) {
        test();
    }

    /**
     * @TODO sort camstar file by camstar file date
     */
    public static
        void test() {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        Config.camFormat.setProductionMode(false);

        File testDataFile = new File(Config.camFormat.getKdfPath());
        //TODO camstar

        for (File shiftFile : testDataFile.listFiles()) {
            for (File dateFile : shiftFile.listFiles()) {
                for (File xlsFile : dateFile.listFiles()) {
                    CamstarReader.getInstance().loadFile(xlsFile);
                }
            }
        }
        System.out.println("total time = " + (System.currentTimeMillis() - startTime));

    }

}
