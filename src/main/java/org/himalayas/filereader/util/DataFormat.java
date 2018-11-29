/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.util;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.dom4j.Element;
import org.himalayas.filereader.kdf.Bin;

public
    class DataFormat {

    private
        int latestDays = 0;

    private
        String fileOpenTimeFormat = null;

    private
        boolean lotFile = true;

    // add for camstar data type
    private
        boolean recalAll = false;
    private
        boolean reloadAll = false;
    private final
        HashMap<String, String> camstarOperMappings = new HashMap();

    private final
        ArrayList<String> lotOpertions = new ArrayList();
    private
        String fileType = null;

    private
        int fileLimit = 30;
    private
        String kdfStartDate = null;
    private
        String kdfEndData = null;

    private
        File doneArchivePath = null;

    private
        File badFormatArchivePath = null;
    private
        File openErrorArchivePath = null;
    private
        File exceptionArchivePath = null;
    private
        File repeatArchivePath = null;

    private
        String testIndexName = null;
    private
        String lotIndexName = null;

    private
        boolean appendSlaveUnitId2Test = false;
    private final
        ArrayList<String> logFailOnlyBaseClasses = new ArrayList();
    private final
        ArrayList<String> logOnlyFailNodes = new ArrayList();
    private final
        ArrayList<String> testDescFieldFilters = new ArrayList();
    private final
        ArrayList<String> testDescFieldSelectors = new ArrayList();
    private final
        ArrayList<String> flowContextFilters = new ArrayList();

    private
        boolean productionMode = true;

    private
        boolean generateMappingFile = true;

    private
        String kdfArchivePath = null;

    private
        String mappingPath = null;

    private
        boolean addFileName = false;

    private
        boolean logToFile = true;
    private
        boolean skipTestClass = false;

    private
        int fieldValueLengthLimit = 8192;

    private
        String factory = null;
    private
        String sourceType = null;
    private
        boolean enabled = false;
    private
        String customer = null;
    private
        String xmlPath = null;
    private
        String kdfPath = null;
    private
        Config.DataTypes dataType = null;
    private
        boolean debugMode = false;

    private final
        ArrayList<String> filters = new ArrayList();
    private final
        ArrayList<String> selectors = new ArrayList();

    private
        boolean ignoreEmptyValueField = true;
    private
        int underLineCnt = -1;
    private
        int lotNumberIndex = -1;
    private
        int testerNumberIndex = -1;
    private
        int mfgStepIndex = -1;
    private
        int waferNumberIndex = -1;
    private
        int testCodeIndex = -1;
    private
        int siteIndex = -1;
    private
        int kdfMonthIndex = -1;
    private
        int testerTypeIndex = -1;
    private
        int sourceTypeIndex = -1;
    private
        int unitIdIndex = -1;
    private
        boolean enabledComponentHash = false;
    private final
        ArrayList<Integer> fileOpenTimeIndex = new ArrayList();

    private final
        LinkedHashMap<String, XmlNode> lotHead = new LinkedHashMap();
    private final
        Unit unit = new Unit();

    private final
        HashMap<String, Bin> softBinDescs = new HashMap();
    private final
        HashMap<String, Bin> hardBinDescs = new HashMap();

    private final
        HashMap<String, String> testerTypes = new HashMap();

    // base class and sub class filters and selectors
    private final
        ArrayList<String> baseClassFilters = new ArrayList();
    private final
        ArrayList<String> baseClassSelectors = new ArrayList();
    private final
        ArrayList<String> subClassFilters = new ArrayList();
    private final
        ArrayList<String> subClassSelectors = new ArrayList();

    private final
        ArrayList<String> nodeTypeFilters = new ArrayList();
    private final
        ArrayList<String> nodeTypeSelectors = new ArrayList();
    private final
        ArrayList<String> fieldFiters = new ArrayList();
    private
        XmlNode lotNumberNode = null;
    private
        XmlNode operationNode = null;
    private
        XmlNode camDateNode = null;
    private
        XmlNode waferNumebrNode = null;

    public
        DataFormat(Element sourceData) {
        readData(sourceData);
    }

    private
        void readData(Element sourceData) {
        @SuppressWarnings("unused")

        List<Element> nodes = sourceData.elements();
        sourceType = sourceData.attributeValue("name");

        String fieldName = null;
        String fieldValue = null;

        if (!nodes.isEmpty()) {
            this.enabled = sourceData.attributeValue("enabled").trim().equals("1");
            for (Element node : nodes) {
                fieldName = node.getName().trim();
                fieldValue = node.getTextTrim();
                if (fieldValue.isEmpty() && (!fieldName.equals("KDFDate"))) {
                    continue;
                }

                switch (fieldName) {

                    case "LatestDays":
                        try {
                            this.latestDays = Integer.valueOf(fieldValue);
                            if (this.latestDays <= 0) {
                                System.out.println("Fatal Error: LatestDays should be a number grate than 0");
                                System.exit(1);
                            }
                        }
                        catch (NumberFormatException e) {
                            System.out.println("Fatal Error: LatestDays should be a number");
                            System.exit(1);
                        }
                        break;
                    case "FileOpenTimeFormat":
                        this.fileOpenTimeFormat = fieldValue;
                        break;
                    case "EnabledComponentHash":
                        this.enabledComponentHash = fieldValue.equals("1");
                        break;
                    case "AppendSlaveUnitId2Test":
                        this.appendSlaveUnitId2Test = fieldValue.equals("1");
                        break;
                    case "ReloadAll":
                        this.reloadAll = fieldValue.equals("1");
                        break;
                    case "RelcalAll":
                        this.recalAll = fieldValue.equals("1");
                        break;
                    case "IsLotFile":
                        this.lotFile = fieldValue.equals("1");
                        break;
                    case "OperMap":
                        for (String group : fieldValue.split(",")) {
                            String[] temp = group.split(":");
                            if (temp.length != 2) {
                                System.out.println("Error: the camstar operation to kdf mfg step mappings");
                                System.out.println("Please following the format: CamStartOper1:KDFMfgStep1,CamStartOper2:KDFMfgStep12...");
                                System.exit(1);
                            }
                            if (this.camstarOperMappings.containsKey(temp[0])) {
                                System.out.printf("Error: dupicate camstar oper found:%s\n", temp[0]);
                                System.exit(1);
                            }
                            this.camstarOperMappings.put(temp[0], temp[1]);
                        }
                        break;

                    case "GenerateMappingFile":
                        this.generateMappingFile = fieldValue.equals("1");
                        break;

                    case "ProductionMode":
                        this.productionMode = fieldValue.equals("1");
                        break;
                    case "LogToFile":
                        this.logToFile = fieldValue.equals("1");
                        break;
                    case "AddFileNameInUnit":
                        this.addFileName = fieldValue.equals("1");
                        break;
                    case "LogFailOnlyNodeSelector":
                        for (String name : fieldValue.split(",")) {
                            this.logOnlyFailNodes.add(name.trim());
                        }
                        break;
                    case "LogFailOnlyBaseClassSelector":
                        for (String name : fieldValue.split(",")) {
                            this.logFailOnlyBaseClasses.add(name.trim());
                        }
                        break;
                    case "KDFDate":
                        readKDFDate(node);
                        break;
                    case "Factory":
                        this.factory = fieldValue;
                        break;
                    case "FileType":
                        this.fileType = fieldValue;
                        break;
                    case "LotIndex":
                        this.lotIndexName = fieldValue;
                        break;
                    case "TestIndex":
                        this.testIndexName = fieldValue;
                        break;
                    case "Customer":
                        this.customer = fieldValue;
                        break;
                    case "KDFPath":
                        if (fieldValue.charAt(0) == '/') {
                            fieldValue = fieldValue.substring(1, fieldValue.length());
                        }
                        this.kdfPath = Config.sourcePath + "/" + fieldValue;
                        break;
                    case "KDFArchivePath":
                        if (fieldValue.charAt(0) == '/') {
                            fieldValue = fieldValue.substring(1, fieldValue.length());
                        }
                        this.kdfArchivePath = Config.archivePath + "/" + fieldValue;
                        break;
                    case "XmlPath":
                        if (fieldValue.charAt(0) == '/') {
                            fieldValue = fieldValue.substring(1, fieldValue.length());
                        }
                        this.xmlPath = Config.datalogPath + "/" + fieldValue;
                        break;
                    case "MappingPath":
                        if (fieldValue.charAt(0) == '/') {
                            fieldValue = fieldValue.substring(1, fieldValue.length());
                        }
                        this.mappingPath = Config.mappingPath + "/" + fieldValue;
                        break;
                    case "DataType":
                        this.dataType = Config.DataTypes.valueOf(fieldValue);
                        if (this.dataType == null) {
                            System.out.println("Fatal Error: DataType value must be one of " + Arrays.toString(Config.DataTypes.values()));
                        }
                        break;
                    case "DebugMode":
                        this.debugMode = fieldValue.equals("1");
                        break;
                    case "FilterOut":
                        for (String filter : fieldValue.split(",")) {
                            this.filters.add(filter.trim());
                        }
                        break;
                    case "Selector":
                        for (String filter : fieldValue.split(",")) {
                            this.selectors.add(filter.trim());
                        }
                        break;
                    case "TesterType":
                        for (String filter : fieldValue.split(",")) {
                            String[] temp = filter.split(":");
                            if (temp.length != 2) {
                                System.out.println("Error: incorrect selector found " + fieldValue);
                                System.exit(1);
                            }
                            testerTypes.put(temp[0], temp[1]);
                        }
                        break;
                    case "TesterTypeIndex":
                        this.testerTypeIndex = Integer.valueOf(fieldValue);
                        break;
                    case "FileLimit":
                        this.fileLimit = Integer.valueOf(fieldValue);
                        break;
                    case "UnitIdIndex":
                        this.unitIdIndex = Integer.valueOf(fieldValue);
                        break;

                    case "UnderLineCnt":
                        this.underLineCnt = Integer.valueOf(fieldValue);
                        break;
                    case "LotNumberIndex":
                        this.lotNumberIndex = Integer.valueOf(fieldValue);
                        break;
                    case "TesterNumberIndex":
                        this.testerNumberIndex = Integer.valueOf(fieldValue);
                        break;
                    case "MfgStepIndex":
                        this.mfgStepIndex = Integer.valueOf(fieldValue);
                        break;
                    case "TestCodeIndex":
                        this.testCodeIndex = Integer.valueOf(fieldValue);
                        break;
                    case "WaferNumberIndex":
                        this.waferNumberIndex = Integer.valueOf(fieldValue);
                        break;
                    case "SiteIndex":
                        this.siteIndex = Integer.valueOf(fieldValue);
                        break;
                    case "KDFMonthIndex":
                        this.kdfMonthIndex = Integer.valueOf(fieldValue);
                        break;
                    case "FieldValueLength":
                        this.fieldValueLengthLimit = Integer.valueOf(fieldValue);
                        break;
                    case "SourceTypeIndex":
                        this.sourceTypeIndex = Integer.valueOf(fieldValue);
                        break;

                    case "FileOpenTimeIndex":
                        int[] indices = new int[]{-1, -1, -1, -1};
                        int i = 0;
                        for (String var : fieldValue.split(",")) {
                            indices[i++] = Integer.valueOf(var.trim());
                        }
                        Arrays.sort(indices);
                        for (int j = 0; j != indices.length; j++) {
                            if (indices[j] == -1) {
                                continue;
                            }
                            this.fileOpenTimeIndex.add(indices[j]);
                        }

                        break;
                    case "BaseClasFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.baseClassFilters.add(filter.trim());
                        }
                        break;
                    case "BaseClasSelector":
                        for (String selector : fieldValue.split(",")) {
                            this.baseClassSelectors.add(selector.trim());
                        }
                        break;
                    case "SubClasFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.subClassFilters.add(filter.trim());
                        }
                        break;
                    case "SubClasSelector":
                        for (String selector : fieldValue.split(",")) {
                            this.subClassSelectors.add(selector.trim());
                        }
                        break;
                    case "NodeTypeFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.nodeTypeFilters.add(filter.trim());
                        }
                        break;
                    case "NodeTypeSelector":
                        for (String selector : fieldValue.split(",")) {
                            this.nodeTypeSelectors.add(selector.trim());
                        }
                        break;

                    case "FieldFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.fieldFiters.add(filter.trim());
                        }
                        break;
                    case "SkipTestClass":
                        this.skipTestClass = fieldValue.equals("1");
                        break;

                    case "TestDescFieldFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.testDescFieldFilters.add(filter.trim());
                        }
                        break;
                    case "TestDescFieldSelector":
                        for (String selector : fieldValue.split(",")) {
                            this.testDescFieldSelectors.add(selector.trim());
                        }
                        break;
                    case "FlowContextFilter":
                        for (String filter : fieldValue.split(",")) {
                            this.flowContextFilters.add(filter.trim());
                        }
                        break;
                    default:
                        System.out.printf("Error: this filed: %s is not supportted!\n", fieldName);
                        System.exit(1);
                        break;

                }
            }
        }
    }

    private
        void readKDFDate(Element element) {
        @SuppressWarnings("unused")
        List<Element> nodes = element.elements();
        if (!nodes.isEmpty()) {
            String startDate = null;
            String endDate = null;
            for (Element node : nodes) {
                String nodeName = node.getName().trim().toLowerCase();
                String value = node.getText().trim();
                if (nodeName.equals("start")) {
                    startDate = value;
                }
                else if (nodeName.equals("end")) {
                    endDate = value;
                }
                else {
                    System.out.println("Error: unsupportted KDFDate type found: " + nodeName);
                    System.exit(1);
                }
            }
            if (startDate.length() == 8 && startDate.startsWith("20")) {
                this.kdfStartDate = startDate;
            }
            if (endDate.length() == 8 && endDate.startsWith("20")) {
                if (this.kdfStartDate != null && endDate.compareTo(startDate) > 0) {
                    this.kdfEndData = endDate;
                }
                else {
                    System.out.printf("Error: stat data %s must be less than end date %s\n", startDate, endDate);
                    System.exit(1);
                }
            }
        }

    }

    public
        boolean validate() {
        System.out.printf("%s=%s, validation....\n", this.getSourceType(), this.isEnabled());
        if (this.getDataType().equals(Config.DataTypes.CAMSTAR)) {
            if (this.getCamDateNode() == null) {
                System.out.println("camstart date node can not be null");
                return false;
            }
            if (this.getDataType() == null) {
                System.out.println("DateType can not be null");
                return false;

            }
            if (this.getKdfPath() == null || (!new File(this.getKdfPath()).exists())) {
                System.out.println("KdfPath can not be " + this.getKdfPath());
                return false;
            }
            if ((this.generateMappingFile && this.mappingPath == null)
                || (this.generateMappingFile && (!new File(this.mappingPath).exists()) && (!new File(this.mappingPath).mkdirs()))) {
                System.out.println("mappingPath can not be " + this.mappingPath);
                return false;

            }
            if (this.xmlPath == null
                || ((!new File(this.xmlPath).exists()) && (!new File(this.xmlPath).mkdirs()))) {
                System.out.println("xmlPath can not be " + this.xmlPath);
                return false;

            }
            if (this.kdfArchivePath == null
                || ((!new File(this.kdfArchivePath).exists()) && (!new File(this.kdfArchivePath).mkdirs()))) {
                System.out.println("kdfArchivePath can not be " + this.kdfArchivePath);
                return false;

            }
            if (this.getLotNumberNode() == null) {
                System.out.println("LotNumberNode can not be null");
                return false;
            }
            if (this.getOperationNode() == null) {
                System.out.println("OperationNode can not be null");
                return false;
            }
            return checkArchive();
        }
        else if (this.getSourceType().equalsIgnoreCase(Config.DataTypes.SMAP.name())
            || this.getSourceType().equalsIgnoreCase(Config.DataTypes.WAT.name())) {
            if (this.getLotNumberIndex() == -1) {
                System.out.println("LotNumberIndex can not be null");
                return false;
            }
            if (this.getLotNumberNode() == null) {
                System.out.println("LotNumber Node can not be null");
                return false;
            }

            if (this.getWaferNumebrNode() == null) {
                System.out.println("WaferNumber Node can not be null");
                return false;
            }
            if (this.getDataType() == null) {
                System.out.println("DateType can not be null");
                return false;

            }
            if (this.getKdfMonthIndex() == -1) {
                System.out.println("KdfMonthIndex can not be null");
                return false;

            }
            if (this.getKdfPath() == null || (!new File(this.getKdfPath()).exists())) {
                System.out.println("KdfPath can not be " + this.getKdfPath());
                return false;
            }
            if (this.getUnderLineCnt() == -1) {
                System.out.println("UnderLineCnt can not be null");
                return false;
            }
            if ((this.generateMappingFile && this.mappingPath == null)
                || (this.generateMappingFile && (!new File(this.mappingPath).exists()) && (!new File(this.mappingPath).mkdirs()))) {
                System.out.println("mappingPath can not be " + this.mappingPath);
                return false;

            }
            if (this.xmlPath == null
                || ((!new File(this.xmlPath).exists()) && (!new File(this.xmlPath).mkdirs()))) {
                System.out.println("xmlPath can not be " + this.xmlPath);
                return false;

            }
            if (this.kdfArchivePath == null
                || ((!new File(this.kdfArchivePath).exists()) && (!new File(this.kdfArchivePath).mkdirs()))) {
                System.out.println("kdfArchivePath can not be " + this.kdfArchivePath);
                return false;

            }
            if (this.getUnit().getxCoordNode() == null) {
                System.out.println("Unit XcoordNode can not be null");
                return false;
            }
            if (this.getUnit().getyCoordNode() == null) {
                System.out.println("Unit YCoordNode can not be null");
                return false;
            }
            if (this.getWaferNumberIndex() == -1) {
                System.out.println("WaferNumberIndex can not be null for ");
                return false;

            }
            if (this.getUnit().getWaferNumberNode() == null) {
                System.out.println("Unit WaferNumberNode can not be null");
                return false;
            }
            for (XmlNode xmlNode : this.lotHead.values()) {
                if (this.getDataType().equals(Config.DataTypes.SMAP)) {
                    if (xmlNode.isEnabled() && xmlNode.isEnabledLog() && (xmlNode.getIndex() == -1 || xmlNode.getIndex() >= this.getUnderLineCnt())) {
                        System.out.printf("please setup a correct index for head node %s in %s\n", xmlNode.getName(), this.getSourceType());
                        return false;
                    }
                }
                else {
                    if (xmlNode.isEnabled() && xmlNode.isEnabledLog()) {
                        if (xmlNode.getCamColumnName() == null
                            && ((xmlNode.getIndex() == -1)
                            || (xmlNode.getIndex() >= this.getUnderLineCnt()))) {
                            System.out.printf("please setup a correct index or name for head node %s in %s\n", xmlNode.getName(), this.getSourceType());
                            return false;
                        }

                    }
                }
            }

            return checkArchive();
        }
        if (this.getCustomer() == null) {
            System.out.println("customer can not be null");
            return false;
        }
        if (this.getLotEndTimeNode() == null) {
            System.out.println("lotEndTimeNode can not be null");
            return false;
        }
        if (this.getLotOpenTimeNode() == null) {
            System.out.println("lotOpenTimeNode can not be null");
            return false;

        }
        if (this.getLotStartTimeNode() == null) {
            System.out.println("lotStartTimeNode can not be null");
            return false;
        }
        if (this.getFactory() == null) {
            System.out.println("factory can not be null");
            return false;
        }
        if (this.getDataType() == null) {
            System.out.println("DateType can not be null");
            return false;

        }
        if (this.getFileOpenTimeIndex().isEmpty()) {
            System.out.println("FileOpenTimeIndex can not be null");
            return false;

        }
        if (this.getKdfMonthIndex() == -1) {
            System.out.println("KdfMonthIndex can not be null");
            return false;

        }
        if (this.getKdfPath() == null || (!new File(this.getKdfPath()).exists())) {
            System.out.println("KdfPath can not be " + this.getKdfPath());
            return false;
        }
        if (this.getLotNumberIndex() == -1) {
            System.out.println("LotNumberIndex can not be null");
            return false;

        }
        if (this.getMfgStepIndex() == -1) {
            System.out.println("MfgStepIndex can not be null");
            return false;

        }
        if (this.getSourceTypeIndex() == -1) {
            System.out.println("SourceTypeIndex can not be null");
            return false;
        }
        if (this.getTestCodeIndex() == -1 && this.dataType.equals(Config.DataTypes.SLT)) {
            System.out.println("TestCodeIndex can not be null for SLT");
            return false;

        }
        if (this.getWaferNumberIndex() == -1 && this.dataType.equals(Config.DataTypes.WaferSort)) {
            System.out.println("WaferNumberIndex can not be null for wafer sort");
            return false;

        }
        if (this.getTesterNumberIndex() == -1) {
            System.out.println("TesterNumberIndex can not be null");
            return false;

        }
        if (this.getTesterTypes().isEmpty()) {
            System.out.println("TesterType can not be null");
            return false;
        }
        if (this.getUnderLineCnt() == -1) {
            System.out.println("UnderLineCnt can not be null");
            return false;
        }
        if (this.getTesterTypeIndex() == -1) {
            System.out.println("TesterTypeIndex can not be null");
            return false;

        }
        if ((this.generateMappingFile && this.mappingPath == null)
            || (this.generateMappingFile && (!new File(this.mappingPath).exists()) && (!new File(this.mappingPath).mkdirs()))) {
            System.out.println("mappingPath can not be " + this.mappingPath);
            return false;

        }
        if (this.xmlPath == null
            || ((!new File(this.xmlPath).exists()) && (!new File(this.xmlPath).mkdirs()))) {
            System.out.println("xmlPath can not be " + this.xmlPath);
            return false;

        }
        if (this.kdfArchivePath == null
            || ((!new File(this.kdfArchivePath).exists()) && (!new File(this.kdfArchivePath).mkdirs()))) {
            System.out.println("kdfArchivePath can not be " + this.kdfArchivePath);
            return false;

        }
        if (this.getUnit().getEndTimeNode() == null) {
            System.out.println("Unit EndTimeNode can not be null");
            return false;
        }
        if (this.getUnit().getHardBinNode() == null) {
            System.out.println("Unit HardBinNode can not be null");
            return false;
        }
        if (this.getUnit().getSoftBinNode() == null) {
            System.out.println("Unit SoftBinNode can not be null");
            return false;

        }
        if (this.getUnit().getStartTimeNode() == null) {
            System.out.println("Unit StartTimeNode can not be null");
            return false;

        }
        if (this.getUnit().getTestTimeNode() == null) {
            System.out.println("Unit TestTimeNode can not be null");
            return false;

        }
        if (this.getUnit().getUnitIdNode() == null) {
            System.out.println("Unit UnitIDNode can not be null");
            return false;
        }
        if (this.getUnit().getWaferNumberNode() == null) {
            System.out.println("Unit WaferNumberNode can not be null");
            return false;
        }
        if (this.getUnit().getxCoordNode() == null) {
            System.out.println("Unit XcoordNode can not be null");
            return false;
        }
        if (this.getUnit().getyCoordNode() == null) {
            System.out.println("Unit YCoordNode can not be null");
            return false;
        }
        if (this.getLotNumberNode() == null) {
            System.out.println("LotNumberNode can not be null");
            return false;
        }
        if (this.getOperationNode() == null) {
            System.out.println("OperationNode can not be null");
            return false;
        }
        if (this.dataType.equals(Config.DataTypes.SLT) && this.unitIdIndex == -1) {
            System.out.println("UniIdIndex of SLT can not be null");
            return false;
        }
        if ((!this.dataType.equals(Config.DataTypes.WaferSort)) && this.getUnit().getWaferLotNode() == null) {
            System.out.println("WaferLotNode of wafer sort can not be null");
            return false;
        }

        return checkArchive();
    }

    private
        boolean checkArchive() {
        if (!Config.renameKDF) {
            this.badFormatArchivePath = new File(this.kdfArchivePath + "/" + Config.EventType.KDFBadFormat);
            if (!this.mkArchivePath(this.badFormatArchivePath)) {
                return false;
            }

            this.openErrorArchivePath = new File(this.kdfArchivePath + "/" + Config.EventType.KDFOpenFailure);
            if (!this.mkArchivePath(this.openErrorArchivePath)) {
                return false;
            }

            this.doneArchivePath = new File(this.kdfArchivePath + "/" + Config.EventType.KDFDone);
            if (!this.mkArchivePath(this.doneArchivePath)) {
                return false;
            }

            this.exceptionArchivePath = new File(this.kdfArchivePath + "/" + Config.EventType.KDFException);
            if (!this.mkArchivePath(this.exceptionArchivePath)) {
                return false;
            }

            this.repeatArchivePath = new File(this.kdfArchivePath + "/" + Config.EventType.KDFRepeat);
            if (!this.mkArchivePath(this.repeatArchivePath)) {
                return false;
            }

            return true;
        }
        return true;
    }

    private
        boolean mkArchivePath(File file) {
        if ((!file.exists()) || file.isFile()) {
            if (file.mkdir()) {
                return true;
            }
            else {
                System.out.println("failed to mkdir for " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private
        void printConfig() {
        System.out.printf("\nsourceData:%s, customer:%s, dataType:%s\n"
            + "debugMode:%s, enabled:%s, xmlPath:%s, kdfPath:%s\n"
            + "testerTypes:%s,"
            + "filters:%s, selectors:%s\n"
            + "fileOpenTimeIndex:%s\n"
            + "kdfMonthIndex:%s, lotNumberIndex:%s, mfgStepIndex:%s\n"
            + "siteIndex:%s, testCodeIndex:%s, testerNumberIndex:%s,  "
            + "underLineCnt:%s, waferNumberIndex:%s\n\n",
            this.sourceType, this.customer, this.dataType,
            this.debugMode, this.enabled, this.xmlPath, this.kdfPath,
            this.testerTypes,
            Arrays.toString(this.filters.toArray()), Arrays.toString(this.selectors.toArray()),
            Arrays.toString(this.fileOpenTimeIndex.toArray()),
            this.kdfMonthIndex, this.lotNumberIndex, this.mfgStepIndex,
            this.siteIndex, this.testCodeIndex, this.testerNumberIndex,
            this.underLineCnt, this.waferNumberIndex);

    }

    @Override
    public
        String toString() {
        String value = "";
        for (XmlNode node : this.lotHead.values()) {
            value += node.toString();
        }
        value += "\n\n";

        for (XmlNode node : this.unit.getNodes().values()) {
            value += node.toString();
        }
        return value;
    }

    public
        void printHeadInfo() {
        String value = "";
        for (XmlNode node : this.lotHead.values()) {
            if (node.getValue() != null) {
                value += node.toString();
            }
        }
        value += "\n";
        for (XmlNode node : this.lotHead.values()) {
            if (node.getValue() == null) {
                value += node.toString();
            }
        }

        System.out.println(value);
    }

    public
        void printUnitInfo() {
        unit.printinfo();
    }

    public
        void clearUnitData() {
        unit.clear();

    }

    public
        void resetHeadTimeFormat() {
        for (XmlNode node : this.lotHead.values()) {
            node.resetTime();
        }
    }

    public
        void resetUnitTimeFormat() {
        for (XmlNode node : this.unit.getNodes().values()) {
            node.resetTime();
        }
    }

    public
        void calTestTime() {
        String startTime = null;
        String endTime = null;
        double testTime = 0;

        for (XmlNode node : this.unit.getNodes().values()) {
            if (node.isUnitTestTimeNode()
                && node.getValue() != null) {
                return;
            }
            else if (node.isStartTime()) {
                if (node.getValue() == null) {
                    return;
                }
                else {
                    startTime = node.getValue();
                }
            }
            else if (node.isEndTime()) {
                if (node.getValue() == null) {
                    return;
                }
                else {
                    endTime = node.getValue();
                }
            }
        }
        if (startTime == null) {
            return;
        }
        if (startTime.startsWith("1")
            && startTime.length() == 13
            && endTime.startsWith("1")
            && endTime.length() == 13) {
            testTime = Double.valueOf(Long.valueOf(endTime) - Long.valueOf(startTime)) / 1000.0;
            for (XmlNode node : this.unit.getNodes().values()) {
                if (node.isUnitTestTimeNode()) {
                    node.setValue(String.valueOf(testTime));
                }
            }
        }
    }

    public
        HashMap<String, Bin> getSoftBinDescs() {
        return softBinDescs;
    }

    public
        HashMap<String, Bin> getHardBinDescs() {
        return hardBinDescs;
    }

    public
        void setBinDesc() {
        setHardBinDesc();
        setSoftBinDesc();
    }

    private
        void setHardBinDesc() {
        XmlNode hardBinNode = this.getUnit().getHardBinNode();
        if (hardBinNode.getValue() != null
            && this.getUnit().getHardBinDesc() == null) {
            if (this.hardBinDescs.containsKey(hardBinNode.getValue())) {
                this.getUnit().setHardBinDesc(this.hardBinDescs.get(hardBinNode.getValue()).getBinDescription());
                this.getUnit().setFlag(this.hardBinDescs.get(hardBinNode.getValue()).getFlag());
            }
        }
    }

    private
        void setSoftBinDesc() {
        XmlNode softBinNode = this.getUnit().getSoftBinNode();
        if (softBinNode.getValue() != null
            && this.getUnit().getSoftBinDesc() == null) {
            if (this.softBinDescs.containsKey(softBinNode.getValue())) {
                this.getUnit().setSoftBinDesc(this.softBinDescs.get(softBinNode.getValue()).getBinDescription());
                this.getUnit().setFlag(this.softBinDescs.get(softBinNode.getValue()).getFlag());
            }
        }
    }

    public
        void clearAll() {
        for (XmlNode node : this.getLotHead().values()) {
            node.resetValue();
        }
        this.hardBinDescs.clear();
        this.softBinDescs.clear();
        unit.clear();

    }

    /**
     * get the lot head kv string here we skip the lot start/end/open time since
     * we use the FieldType.FileTime as the lot time timestamper
     *
     * @return
     */
    public
        String getLotHeadKVString() {
        boolean firstField = false;
        String value = "";
        for (XmlNode node : this.lotHead.values()) {
            if (!node.isEnabledLog()) {
                continue;
            }
            if (node.isLotEndTime() || node.isLotOpenTime() || node.isLotStartTime()) {
                continue;
            }
            if (node.getValue() != null && (!node.getValue().isEmpty())) {
                if (!firstField) {
                    value += node.toKVString();
                    firstField = true;
                }
                else {
                    value += "," + node.toKVString();
                }
            }
        }
        if (!this.ignoreEmptyValueField) {
            for (XmlNode node : this.lotHead.values()) {
                if (!node.isEnabledLog()) {
                    continue;
                }
                if (node.getValue() == null || node.getValue().isEmpty()) {
                    value += "," + node.toKVString();
                }
            }
        }

        return value;
    }

    public
        String getUnitDocKVString() {
        String value = "," + FieldType.Type + "=" + FieldType.Unit;
        for (XmlNode node : this.getUnit().getNodes().values()) {
            if (!node.isEnabledLog()) {
                continue;
            }
            if (node.getValue() != null && (!node.getValue().isEmpty())) {
                value += "," + node.toKVString();
            }
        }
        if (!this.ignoreEmptyValueField) {
            for (XmlNode node : this.lotHead.values()) {
                if (!node.isEnabledLog()) {
                    continue;
                }
                if (node.getValue() == null || node.getValue().isEmpty()) {
                    value += "," + node.toKVString();
                }
            }
        }
        value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
        value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
        value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();
        value += "," + FieldType.DieType + "=" + FieldType.MasterDie;
        if ((this.getDataType().equals(Config.DataTypes.ATE) || this.getDataType().equals(Config.DataTypes.SLT))
            && (!this.getUnit().getUnitIdNode().getXmlValue().isEmpty())) {
            value += "," + FieldType.MasterDieId + "=" + this.getUnit().getUnitIdNode().getValue();
        }

        return value;

    }

    public
        String getX0Y0KVString() {
        String value = "";
        String xValue = this.getUnit().getxCoordNode().getXmlValue().trim();
        String yValue = this.getUnit().getyCoordNode().getXmlValue().trim();
        if ((!xValue.isEmpty()) && (!yValue.isEmpty())) {
            try {
                value += "," + FieldType.X0 + "=" + (7 - Integer.valueOf(xValue));
                value += "," + FieldType.Y0 + "=" + ((Integer.valueOf(yValue) - 15) / 3);
            }
            catch (NumberFormatException e) {
                System.out.printf("Error: %s = %s, %s = %s, can not convert to integer\n",
                    this.getUnit().getxCoordNode().getName(),
                    xValue,
                    this.getUnit().getyCoordNode().getName(),
                    yValue);
                value = "";
            }
        }
        return value;

    }

    /**
     * skip all the master die info
     *
     * @return
     */
    public
        String getSlaveUnitDocKVString() {
        String value = "," + FieldType.Type + "=" + FieldType.Unit;
        for (XmlNode node : this.getUnit().getNodes().values()) {
            if (!node.isEnabledLog()) {
                continue;
            }

            if (node.isUnitIdNode() || node.isWaferNumberNode() || node.isWaferLotNode() || node.isxNode() || node.isyNode()) {
                continue;
            }
            if (node.getValue() != null && (!node.getValue().isEmpty())) {
                value += "," + node.toKVString();
            }
        }
        if (!this.ignoreEmptyValueField) {
            for (XmlNode node : this.lotHead.values()) {
                if (!node.isEnabledLog()) {
                    continue;
                }
                if (node.getValue() == null || node.getValue().isEmpty()) {
                    value += "," + node.toKVString();
                }
            }
        }
        value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
        value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
        value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();
        if ((this.getDataType().equals(Config.DataTypes.ATE) || this.getDataType().equals(Config.DataTypes.SLT))
            && (!this.getUnit().getUnitIdNode().getXmlValue().isEmpty())) {
            value += "," + FieldType.MasterDieId + "=" + this.getUnit().getUnitIdNode().getValue();
        }

        return value;

    }

    public
        String getUnitHeadTestKVStr() {
        String value = "";
        for (XmlNode node : this.getUnit().getNodes().values()) {
            if (!node.isEnabledLog()) {
                continue;
            }
            if (node.getValue() != null && (!node.getValue().isEmpty())) {
                // never log the unit start time and end time into test level data
                // never log the unit tets time to test level doc
                // readd startTestTime to all test level doc
                if (node.isTimeNode() || node.isUnitTestTimeNode()) {
                    if (!node.isStartTime()) {
                        continue;
                    }
                }

                value += "," + node.toKVString();
            }
        }
        if (!this.ignoreEmptyValueField) {
            for (XmlNode node : this.lotHead.values()) {
                if (!node.isEnabledLog()) {
                    continue;
                }
                if (node.getValue() == null || node.getValue().isEmpty()) {
                    value += "," + node.toKVString();
                }
            }
        }
        value += "," + FieldType.SoftBinDesc + "=" + this.getUnit().getSoftBinDescValue();
        value += "," + FieldType.HardBinDesc + "=" + this.getUnit().getHardBinDescValue();
        value += "," + FieldType.BinType + "=" + this.getUnit().getFlagIntValue();

        return value;

    }

    public
        String getFileDocTimeKVStr() {
        String value = "";
        XmlNode startTimeNode = this.getLotStartTimeNode();

        if (startTimeNode.getValue() != null) {
            value += "," + startTimeNode.toKVString();
        }
        XmlNode endTimeNode = this.getLotEndTimeNode();
        if (endTimeNode.getValue() != null) {
            value += "," + endTimeNode.toKVString();
        }

        if (startTimeNode.getValue() != null && endTimeNode.getValue() != null) {
            value += ("," + FieldType.GrossTime + "=" + (Long.valueOf(endTimeNode.getTimeLongValue()) - Long.valueOf(startTimeNode.getTimeLongValue())) / 1000);
        }
        return value;

    }

    public
        XmlNode getLotStartTimeNode() {
        for (XmlNode node : this.getLotHead().values()) {
            if (node.isLotStartTime()) {
                return node;
            }
        }
        return null;
    }

    public
        XmlNode getLotEndTimeNode() {
        for (XmlNode node : this.getLotHead().values()) {
            if (node.isLotEndTime()) {
                return node;
            }
        }
        return null;

    }

    public
        XmlNode getLotOpenTimeNode() {
        for (XmlNode node : this.getLotHead().values()) {
            if (node.isLotOpenTime()) {
                return node;
            }
        }
        return null;

    }

    public
        int getUnderLineCnt() {
        return underLineCnt;
    }

    public
        String getCustomer() {
        return customer;
    }

    public
        ArrayList<String> getSelectors() {
        return selectors;
    }

    public
        ArrayList<String> getFilters() {
        return filters;
    }

    public
        boolean isDebugMode() {
        return debugMode;
    }

    public
        String getXmlPath() {
        return xmlPath;
    }

    public
        void print() {
        System.out.println(this.toString());
    }

    public
        Unit getUnit() {
        return unit;
    }

    public
        LinkedHashMap<String, XmlNode> getLotHead() {
        return lotHead;
    }

    public
        boolean isEnabled() {
        return enabled;
    }

    public
        String getKdfPath() {
        return kdfPath;
    }

    public
        Config.DataTypes getDataType() {
        return dataType;
    }

    public
        int getLotNumberIndex() {
        return lotNumberIndex;
    }

    public
        int getTesterNumberIndex() {
        return testerNumberIndex;
    }

    public
        int getMfgStepIndex() {
        return mfgStepIndex;
    }

    public
        int getWaferNumberIndex() {
        return waferNumberIndex;
    }

    public
        int getTestCodeIndex() {
        return testCodeIndex;
    }

    public
        int getSiteIndex() {
        return siteIndex;
    }

    public
        int getKdfMonthIndex() {
        return kdfMonthIndex;
    }

    public
        ArrayList<Integer> getFileOpenTimeIndex() {
        return fileOpenTimeIndex;
    }

    public
        HashMap<String, String> getTesterTypes() {
        return testerTypes;
    }

    public
        String getFactory() {
        return factory;
    }

    public
        int getTesterTypeIndex() {
        return testerTypeIndex;
    }

    public
        ArrayList<String> getBaseClassFilters() {
        return baseClassFilters;
    }

    public
        ArrayList<String> getBaseClassSelectors() {
        return baseClassSelectors;
    }

    public
        ArrayList<String> getSubClassFilters() {
        return subClassFilters;
    }

    public
        ArrayList<String> getSubClassSelectors() {
        return subClassSelectors;
    }

    public
        ArrayList<String> getNodeTypeFilters() {
        return nodeTypeFilters;
    }

    public
        ArrayList<String> getNodeTypeSelectors() {
        return nodeTypeSelectors;
    }

    public
        int getFieldValueLengthLimit() {
        return fieldValueLengthLimit;
    }

    public
        boolean isLogToFile() {
        return logToFile;
    }

    public
        String getSourceType() {
        return sourceType;
    }

    public
        int getSourceTypeIndex() {
        return sourceTypeIndex;
    }

    public
        boolean isEnabledComponentHash() {
        return enabledComponentHash;
    }

    public
        ArrayList<String> getFieldFiters() {
        return fieldFiters;
    }

    public
        boolean isIgnoreEmptyValueField() {
        return ignoreEmptyValueField;
    }

    public
        boolean isAddFileName() {
        return addFileName;
    }

    public
        String getMappingPath() {
        return mappingPath;
    }

    public
        String getKdfArchivePath() {
        return kdfArchivePath;
    }

    public
        int getUnitIdIndex() {
        return unitIdIndex;
    }

    public
        boolean isGenerateMappingFile() {
        return generateMappingFile;
    }

    public
        boolean isProductionMode() {
        return productionMode;
    }

    public
        ArrayList<String> getTestDescFieldFilters() {
        return testDescFieldFilters;
    }

    public
        ArrayList<String> getTestDescFieldSelectors() {
        return testDescFieldSelectors;
    }

    public
        ArrayList<String> getFlowContextFilters() {
        return flowContextFilters;
    }

    public
        ArrayList<String> getLogOnlyFailNodes() {
        return logOnlyFailNodes;
    }

    public
        ArrayList<String> getLogFailOnlyBaseClasses() {
        return logFailOnlyBaseClasses;
    }

    public
        boolean isAppendSlaveUnitId2Test() {
        return appendSlaveUnitId2Test;
    }

    public
        void setLotNumberNode(XmlNode lotNumberNode) {
        this.lotNumberNode = lotNumberNode;
    }

    public
        void setOperationNode(XmlNode operationNode) {
        this.operationNode = operationNode;
    }

    public
        XmlNode getLotNumberNode() {
        return lotNumberNode;
    }

    public
        XmlNode getOperationNode() {
        return operationNode;
    }

    public
        String getLotIndexName() {
        return lotIndexName;
    }

    public
        File getBadFormatArchivePath() {
        return badFormatArchivePath;
    }

    public
        File getDoneArchivePath() {
        return doneArchivePath;
    }

    public
        File getExceptionArchivePath() {
        return exceptionArchivePath;
    }

    public
        File getOpenErrorArchivePath() {
        return openErrorArchivePath;
    }

    public
        File getRepeatArchivePath() {
        return repeatArchivePath;
    }

    public
        String getKdfEndData() {
        return kdfEndData;
    }

    public
        String getKdfStartDate() {
        return kdfStartDate;
    }

    public
        int getFileLimit() {
        return fileLimit;
    }

    public
        String getTestIndexName() {
        return testIndexName;
    }

    public
        String getFileType() {
        return fileType;
    }

    public
        ArrayList<String> getLotOpertions() {
        return lotOpertions;
    }

    public
        void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
    }

    public
        boolean isLotFile() {
        return lotFile;
    }

    public
        String getFileOpenTimeFormat() {
        return fileOpenTimeFormat;
    }

    public
        String getMinDateString() {
        String date = "1";
        if (this.latestDays > 0) {
            date = LocalDate.now().minusDays(this.latestDays).format(DateTimeFormatter.BASIC_ISO_DATE);
        }
        return date;
    }

    public
        ArrayList<XmlNode> getWatFieldNode() {
        ArrayList<XmlNode> watNode = new ArrayList();
        this.getLotHead().values().stream().filter((xmlNode) -> (xmlNode.getCamColumnName() != null)).forEachOrdered((xmlNode) -> {
            watNode.add(xmlNode);
        });
        return watNode;
    }

    public
        XmlNode getCamDateNode() {
        return camDateNode;
    }

    public
        void setCamDateNode(XmlNode camDateNode) {
        this.camDateNode = camDateNode;
    }

    public
        XmlNode getWaferNumebrNode() {
        return waferNumebrNode;
    }

    public
        void setWaferNumebrNode(XmlNode waferNumebrNode) {
        this.waferNumebrNode = waferNumebrNode;
    }

}
