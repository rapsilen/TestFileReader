/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.kdf;

import com.amd.kdf.KDFFieldData;
import com.amd.kdf.collection.KDFCollectionFilter;
import com.amd.kdf.collection.KDFInstanceTree;
import com.amd.kdf.collection.KDFInstanceTree.Node;
import com.amd.kdf.collection.LiteKDFInstanceTree;
import com.amd.kdf.gui.KdfLoader;
import com.amd.kdf.gui.KdfTypes;
import com.amd.kdf.io.SeekableKDFInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.himalayas.filereader.util.Config;
import org.himalayas.filereader.util.DataFormat;
import org.himalayas.filereader.util.FieldType;
import org.himalayas.filereader.util.XmlNode;

/**
 *
 * @author ghfan
 */
public
    class KDFReader extends KdfLoader {

    private
        KDFCollectionFilter kdfFilter = null;
    private
        File file;
    private
        KDFInstanceTree tree;
    private
        DataFormat format = null;
//    private final
//        Document document = DocumentHelper.createDocument();
//    private
//        Element root = document.addElement("UnitData");
    private
        Config.FailureCase failType = null;
    private
        int unitCnt = 0;

    private
        String nodeContent = null;
    private
        String nodeHead = null;

    //add pinRefs
    private final
        HashMap<String, String> pinRefs = new HashMap();
    private final
        HashMap<String, String> patternRefs = new HashMap();
    private final
        HashMap<String, TestDesc> testDescRefs = new HashMap();
    private final
        HashMap<String, ComponentHash> comHashRefs = new HashMap();

    private
        String flowContextField = null;
    private
        String testResultFieldValue = null;
    private
        String subBaseClassField = null;
    private
        String comHashValue = null;
    private
        String fileOpenTime = "";
    private
        int testCntInFlow = 0;
    long jobStartTime = 0;
    boolean debugMode = false;

    private
        File testLogFile = null;
    private
        File mappingFile = null;
    private
        File doneArchiveFile = null;
    private
        File repeatArchiveFile = null;
    private
        File openErrorArchiveFile = null;
    private
        File badFormatArchiveFile = null;
    private
        File exceptionArchiveFile = null;
    private
        String kdfMonth = null;
    private
        String kdfDate = null;
    private
        String lotNumber = null;
    private
        String transferTime = null;
    private
        String kdfName = null;
    private
        String mfgStp = null;
    private
        int unitLevelDocCnt = 0;
    private
        int fileLevelDocCnt = 0;
//	private
//		ArrayList<String> allFields = new ArrayList();
    private
        String baseClass = null;
    private
        int slaveUnitCnt = 0;
    private
        int kdfDoneCnt = 0;

    public
        KDFReader() {
        super(null);
    }

    private
        void clearRefs() {
        this.patternRefs.clear();
        this.pinRefs.clear();
        this.testDescRefs.clear();
        this.comHashRefs.clear();

    }

    private
        void init() {
        //reset all the variables
        this.clearRefs();
        this.testLogFile = null;
        this.mappingFile = null;
        this.kdfMonth = null;
        this.kdfDate = null;
        this.lotNumber = null;
        this.doneArchiveFile = null;
        this.badFormatArchiveFile = null;
        this.exceptionArchiveFile = null;
        this.openErrorArchiveFile = null;
        this.repeatArchiveFile = null;
        this.fileOpenTime = null;
        this.transferTime = null;
        this.kdfName = null;
        this.mfgStp = null;
        this.fileLevelDocCnt = 0;
        this.unitLevelDocCnt = 0;
    }

    public
        boolean loadFile(File file) {
        debugMode = this.getFormat().isDebugMode();
        this.file = file;

        jobStartTime = System.currentTimeMillis();
        System.out.printf("\n%s: start proceed kdf %s\n", LocalDateTime.now(), file.getName());
        this.init();
        if (!this.preValidate()) {
            return false;
        }
        if (!this.validateFile(file)) {
            this.failType = Config.FailureCase.BadFormat;
            this.logBadFormatFileToES();
            this.renameOrArchiveKDF(this.badFormatArchiveFile, Config.KdfRename.badFormat);
            return false;
        }
        if (this.isRepeatFile()) {
            this.failType = Config.FailureCase.RepeatKDF;
            this.logRepeatFileToES();
            this.renameOrArchiveKDF(this.repeatArchiveFile, Config.KdfRename.skip);
            return false;
        }
        if (!validateKDFDate()) {
            return false;
        }

        try {
            this.addFile("", "");
        }
        catch (Exception ex) {
            Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
            this.failType = Config.FailureCase.OpenFailure;
            this.logOpenFailureToES();
            this.renameOrArchiveKDF(this.openErrorArchiveFile, Config.KdfRename.openErr);
            return false;
        }

        return this.readKDF();
    }

    @Override
    public
        boolean addFile(String fileName, String filter) throws Exception {
        kdfFilter = null;
        boolean useLite = true;
        boolean skipFile = false;
        if (!skipFile) {
            if (useLite) {
                SeekableKDFInputStream skis = new SeekableKDFInputStream(file);
                tree = new LiteKDFInstanceTree(skis, kdfFilter);
            }
        }

        return true;
    }

    private
        boolean validateKDFDate() {
        if (this.getFormat().getKdfStartDate() != null
            && this.kdfDate.compareTo(this.getFormat().getKdfStartDate()) < 0) {
            System.out.printf("Warnning: this kdf file is skipped, KDFDate = %s since KDFDate Start Date filter = %s\n", this.kdfDate, this.getFormat().getKdfStartDate());
            return false;
        }
        if (this.getFormat().getKdfEndData() != null
            && this.getFormat().getKdfEndData().compareTo(this.kdfDate) < 0) {
            System.out.printf("Warnning: this kdf file is skipped, KDFDate = %s since KDFDate End Date filter = %s\n", this.kdfDate, this.getFormat().getKdfEndData());
            return false;
        }
        return true;
    }

    private
        boolean validateFile(File kdfFile) {
        this.failType = null;

        String kdfLogName = kdfFile.getName();
        String names[] = kdfLogName.split("kdf.");
        if (names.length != 2) {
            return false;
        }
        if (!kdfFile.isFile()) {
            return false;
        }
        // the timestamp when the kdf is moved to this place
        if (names[names.length - 1].length() != 14 || (!names[names.length - 1].startsWith("20"))) {
            return false;
        }
        this.transferTime = this.formatTimeStr(names[1]);
        this.kdfName = names[0] + "kdf";

        boolean skip = false;
        for (String filter : this.getFormat().getFilters()) {
            if (kdfLogName.contains(filter)) {
                skip = true;
                break;
            }
        }
        if (skip) {
            return false;
        }

        for (String selector : this.getFormat().getSelectors()) {
            if (!kdfLogName.contains(selector)) {
                skip = true;
                break;
            }
        }
        if (skip) {
            return false;
        }

        names = kdfFile.getName().split("_");
        if (names.length != this.getFormat().getUnderLineCnt()) {
            System.out.println("Skip this kdf since underline cnt is not " + this.getFormat().getUnderLineCnt());
            return false;
        }

        if (!this.setKDFDate()) {
            System.out.println("Skip since bad Format of KDF date");
            return false;
        }
        if (names.length <= format.getLotNumberIndex()
            || names.length <= format.getMfgStepIndex()) {
            return false;
        }

        this.lotNumber = names[format.getLotNumberIndex()];
////		this.unitId = names[format.getUnitIdIndex()];
        this.mfgStp = names[format.getMfgStepIndex()];

        // archive file
        this.doneArchiveFile = new File(this.getFormat().getDoneArchivePath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName());

        this.badFormatArchiveFile = new File(this.getFormat().getBadFormatArchivePath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName());

        this.openErrorArchiveFile = new File(this.getFormat().getOpenErrorArchivePath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName());

        this.repeatArchiveFile = new File(this.getFormat().getRepeatArchivePath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName());
        this.exceptionArchiveFile = new File(this.getFormat().getExceptionArchivePath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName());

        // mapping file
        this.mappingFile = new File(this.getFormat().getMappingPath()
            + "/" + this.kdfDate
            + "/" + lotNumber
            + "/" + this.file.getName().split(".kdf")[0] + ".kdf");

        return true;
    }

    protected
        boolean preValidate() {

        if (this.file.getName().endsWith(Config.KdfRename.badFormat.name())
            || this.file.getName().endsWith(Config.KdfRename.done.name())
            || this.file.getName().endsWith(Config.KdfRename.exception.name())
            || this.file.getName().endsWith(Config.KdfRename.openErr.name())
            || this.file.getName().endsWith(Config.KdfRename.skip.name())) {
            System.out.println("Warning: this is a proceeded kdf!");
            return false;
        }
        return true;

    }

    private
        void logOpenFailureToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFOpenFailure,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.kdfName,
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp
        );
    }

    private
        void logRepeatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFRepeat,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.kdfName,
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp
        );
    }

    private
        void logBadFormatFileToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFBadFormat,
            FieldType.FileName, this.file.getName(),
            FieldType.DataType, this.getFormat().getDataType()
        );
    }

    private
        void logIoErrorToES(String func) {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.IOError,
            FieldType.Failure, func,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.kdfName,
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp
        );
    }

    public
        void logExceptionToES() {
        System.out.printf("%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s\n",
            FieldType.EventType, Config.EventType.KDFException,
            this.getFormat().getLotNumberNode().getName(), this.lotNumber,
            FieldType.KdfMonth, this.kdfMonth,
            FieldType.KdfDate, this.kdfDate,
            FieldType.TransferTime, this.transferTime,
            FieldType.KdfName, this.kdfName,
            FieldType.DataType, this.getFormat().getDataType(),
            this.getFormat().getOperationNode().getName(), this.mfgStp
        );
    }

    private
        void logKDFDoneToES() {
        if (!this.getFormat().getDataType().equals(Config.DataTypes.SLT)) {
            System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%d\n",
                FieldType.EventType, Config.EventType.KDFDone,
                FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
                FieldType.KdfName, this.kdfName,
                FieldType.UnitCnt, this.unitCnt,
                this.getFormat().getLotNumberNode().getName(), this.lotNumber,
                this.getFormat().getOperationNode().getName(), this.mfgStp,
                FieldType.KdfMonth, this.kdfMonth,
                FieldType.KdfDate, this.kdfDate,
                FieldType.TransferTime, this.transferTime,
                FieldType.DataType, this.getFormat().getDataType(),
                FieldType.DocCnt, this.fileLevelDocCnt
            );
        }
        else {
            System.out.printf("%s=%s,%s=%s,%s=%s,%s=%d,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%s,%s=%d,%s=%s\n",
                FieldType.EventType, Config.EventType.KDFDone,
                FieldType.DoneTime, ZonedDateTime.now().toOffsetDateTime(),
                this.getFormat().getUnit().getUnitIdNode().getName(), this.getFormat().getUnit().getUnitIdNode().getXmlValue(),
                FieldType.UnitCnt, this.unitCnt,
                this.getFormat().getLotNumberNode().getName(), this.lotNumber,
                this.getFormat().getOperationNode().getName(), this.mfgStp,
                FieldType.KdfMonth, kdfMonth,
                FieldType.KdfDate, this.kdfDate,
                FieldType.TransferTime, this.transferTime,
                FieldType.DataType, this.getFormat().getDataType(),
                FieldType.DocCnt, this.fileLevelDocCnt,
                FieldType.KdfName, this.kdfName
            );
        }

    }

    private
        String getAteFileKVStr() {

        String value = FieldType.Type + "=" + FieldType.File
            + "," + FieldType.DoneTime + "=" + ZonedDateTime.now().toOffsetDateTime()
            + "," + FieldType.KdfName + "=" + this.kdfName
            + "," + FieldType.UnitCnt + "=" + this.unitCnt
            + "," + this.getFormat().getLotNumberNode().getName() + "=" + this.lotNumber
            + "," + this.getFormat().getOperationNode().getName() + "=" + this.mfgStp
            + "," + FieldType.KdfMonth + "=" + this.kdfMonth
            + "," + FieldType.KdfDate + "=" + this.kdfDate
            + "," + FieldType.TransferTime + "=" + this.transferTime
            //			+ "," + FieldType.DataType + "=" + this.getFormat().getDataType()
            + "," + FieldType.DocCnt + "=" + this.fileLevelDocCnt
            + "," + FieldType.FileTime + "=" + this.formatTimeStr(this.fileOpenTime);
        value += this.getFormat().getFileDocTimeKVStr() + "\n";
        if (this.isDebugMode()) {
            System.out.println(value);
        }
        return value;
    }

    private
        boolean isRepeatFile() {
        if (this.mappingFile.exists()) {
            System.out.println("Skip since this is a repeat file");
            return true;
        }
        return false;
    }

    /**
     * read root
     */
    public
        void readRoot() {
        long startTime = System.currentTimeMillis();
        Node[] roots = tree.getRoots();
        if (roots != null && roots.length > 0) {
            for (Node rootNode : roots) {
                if (this.isDebugMode()) {
                    System.out.println("Root name is " + rootNode.getName());
                }
                for (XmlNode xmlNode : format.getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (rootNode.containsKey(fieldName)) {
                            xmlNode.setValue(rootNode.get(fieldName).toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.printf("reading %s time is : %d\n", "roots", (System.currentTimeMillis() - startTime));
            }
        }
    }

    public
        void readRootNode(String nodeType) {
        long startTime = System.currentTimeMillis();
        Node[] nodes = tree.getNodes(nodeType);
        if (nodes != null && nodes.length > 0) {
            for (Node node : nodes) {
                for (XmlNode xmlNode : format.getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (node.containsKey(fieldName)) {
                            xmlNode.setValue(node.get(fieldName).toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.printf("reading %s time is : %d\n", nodeType, (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read component
     */
    public
        void readComponement() {
        long startTime = System.currentTimeMillis();
        Node[] coms = tree.getNodes(KdfTypes.KDF_RT_COMPONENT);
        if (coms != null && coms.length > 0) {
            for (Node component : coms) {
                for (XmlNode xmlNode : format.getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (component.containsKey("componentType")
                            && component.get("componentType").toString().equals(fieldName)
                            && component.containsKey("componentId")) {
                            xmlNode.setValue(component.get("componentId").toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading component time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read Omi
     */
    private
        void readOMI() {
        long startTime = System.currentTimeMillis();
        Node[] omis = tree.getNodes(KdfTypes.KDF_RT_OMI);
        if (omis != null && omis.length > 0) {
            for (Node omi : omis) {
                for (XmlNode xmlNode : format.getLotHead().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (omi.get("name") != null
                            && omi.get("name").toString().equals(fieldName)
                            && omi.containsKey("value")) {
                            xmlNode.setValue(omi.get("value").toString());
                        }
                    }
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading omi time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    /**
     * read the base class the sub class the class is stored in the <testDescs>
     * the format is subClassName=subClassName,baseClass=baseClassName
     */
    private
        void readTestDesc() {
        long startTime = System.currentTimeMillis();
        Node[] descs = tree.getNodes(KdfTypes.KDF_RT_TESTDESC);
        if (descs != null && descs.length > 0) {
            for (Node des : descs) {
                String value = "";
                String testDescId = null;
                String baseClassName = null;
                String subClassName = null;
                for (String fieldName : des.keySet()) {
                    String fieldValue = des.get(fieldName).toString().trim();

                    if (fieldValue.isEmpty() || fieldName.isEmpty()) {
                        continue;
                    }
                    if (fieldName.endsWith(Config.subClass)) {
                        subClassName = fieldValue;
                        // base class and sub class filters need to apply them here since it needs the base and sub class
                        // to generate the generated test name
                        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)
                            && this.getFormat().getSubClassFilters().contains(fieldValue)) {
                            continue;
                        }
                    }
                    if (fieldName.endsWith(Config.baseClass)) {
                        baseClassName = fieldValue;
                        // base class and sub class filters need to apply them here since it needs the base and sub class
                        // to generate the generated test name
                        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)
                            && this.getFormat().getBaseClassFilters().contains(fieldValue)) {
                            continue;
                        }
                    }
                    if (fieldName.equals(Config.testDescId)) {
                        testDescId = fieldValue;
                        continue;
                    }
                    // apply only after the base and sub class and also the testDescId
                    if (this.getFormat().getTestDescFieldFilters().contains(fieldName)) {
                        continue;
                    }
                    if ((!this.getFormat().getTestDescFieldSelectors().isEmpty())
                        && (!this.getFormat().getTestDescFieldSelectors().contains(fieldName))) {
                        continue;
                    }

                    value += "," + fieldName.replace('.', '_') + "=" + fieldValue.replace(',', ' ').replace('=', ' ');

                }
                if (testDescId != null && (!value.isEmpty())) {
                    this.testDescRefs.put(testDescId, new TestDesc(baseClassName, subClassName, value));
                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading test desc time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    private
        void readBinDesc() {
        long startTime = System.currentTimeMillis();
        Node[] descs = tree.getNodes(KdfTypes.KDF_RT_BINDESC);
        if (descs != null && descs.length > 0) {
            for (Node binDesc : descs) {

                String binType = "";
                String binNumber = null;
                String binDescription = null;
                String flag = null;

                for (String key : binDesc.keySet()) {
                    if (key.equalsIgnoreCase("bintype")) {
                        binType = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binNumber")) {
                        binNumber = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binDescription")) {
                        Object o = binDesc.get(key).getValue();
                        if (o != null) {
                            binDescription = this.formatBinDesc(o.toString());
                        }
                    }
                    else if (key.equalsIgnoreCase("binDescFlag")) {
                        flag = binDesc.get(key).getValue().toString();
                    }
                }

                if (binType.equals("83") || binType.equals("S")) {
                    if (binNumber != null
                        && flag != null) {
                        format.getSoftBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
                    }

                }
                else if (binType.equals("72") || binType.equals("H")) {
                    if (binNumber != null
                        && flag != null) {
                        format.getHardBinDescs().putIfAbsent(binNumber, new Bin(binNumber, binDescription, flag));
                    }

                }

            }
            if (this.debugMode) {
                System.out.println("reading binDesc node time is : " + (System.currentTimeMillis() - startTime));
            }
        }
    }

    private
        void readRooT_Refs(String kdfType, HashMap<String, String> data) {
        long startTime = System.currentTimeMillis();
        Node[] miscs = tree.getNodes(kdfType);
        if (miscs != null && miscs.length > 0) {
            for (Node misc : miscs) {
                String key = null;
                String value = null;
                for (String fieldName : misc.keySet()) {
                    if (misc.get(fieldName).getType() == 5) {
                        key = misc.get(fieldName).getValue().toString();
                    }
                    else if (misc.get(fieldName).getType() == 11) {
                        value = misc.get(fieldName).getValue().toString();
                    }
                }
                if (key != null && (!key.isEmpty()) && value != null && (!value.isEmpty())) {
                    data.put(key, value.trim().replace(',', ' ').replace('=', ' '));

                }
            }
            if (this.isDebugMode()) {
                System.out.println("reading " + kdfType + " time is " + (System.currentTimeMillis() - startTime));
            }

        }
    }

    private
        void readRoot_ComHash() {
        long startTime = System.currentTimeMillis();
        Node[] rawComHashRefs = tree.getNodes(KdfTypes.KDF_RT_COMPGROUPDESC);
        if (rawComHashRefs != null && rawComHashRefs.length > 0) {
            for (Node comHashNode : rawComHashRefs) {
                KDFFieldData comNameField = comHashNode.get("componentName");
                if (comNameField != null) {
                    String comName = comNameField.getValue().toString().replace('.', '_');
                    String comClass = comHashNode.get("componentClass").getValue().toString().replace('.', '_');

                    String comHash = comHashNode.get("componentHash").getValue().toString();
                    String comInst = comHashNode.get("componentInst").getValue().toString();
                    this.comHashRefs.put(comHash, new ComponentHash(comClass, comName));
                }

            }
        }
        if (this.isDebugMode()) {
            System.out.println("reading component hash time is " + (System.currentTimeMillis() - startTime));
        }

    }

    private
        boolean readKDF() {
        this.unitCnt = 0;
        String waferNumber = "";

        format.clearAll();
        Node[] units = tree.getNodes(KdfTypes.KDF_RT_UNIT);
        if (units == null || units.length == 0) {
            System.out.println("Empty Uni Record: no unit record found in this kdf...");
            // generate the kdf mapping file and for repeat kdf file check
            if (this.getFormat().isGenerateMappingFile() && (!this.generateMapFile())) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailCreateMapFile");
            }
            else {
                this.logKDFDoneToES();
                // only rename or archive the kdf in production mode
                this.renameOrArchiveKDF(this.doneArchiveFile, Config.KdfRename.done);
            }
            return true;
        }
        else {
            unitCnt = units.length;
            if (this.isDebugMode()) {
                System.out.println("total unit cnt is: " + units.length);
            }
        }

        if (format.getDataType().equals(Config.DataTypes.WaferSort)) {
            Node[] wafers = tree.getNodes(KdfTypes.KDF_RT_WAFER);
            if (wafers != null && wafers.length > 1) {
                System.out.println("Error: we expect only one wafer in one kdf file...");
                this.failType = Config.FailureCase.Exception;
                return false;
            }
            waferNumber = Integer.valueOf(this.file.getName().split("_")[this.getFormat().getWaferNumberIndex()]).toString();
        }

        this.readRoot();

        /**
         * read info
         */
        this.readRootNode(KdfTypes.KDF_RT_INFO);

        /**
         * read lot
         */
        this.readRootNode(KdfTypes.KDF_RT_LOT);

        /**
         * read wafer
         */
        if (format.getDataType().equals(Config.DataTypes.WaferSort)) {
            this.readRootNode(KdfTypes.KDF_RT_WAFER);
        }

        /**
         * read mfg
         */
        this.readRootNode(RecordTypes.KDF_RT_MFG);

        this.readComponement();
        this.readOMI();
        this.readRooT_Refs(KdfTypes.KDF_RT_PINREF, this.pinRefs);
        this.readRooT_Refs(KdfTypes.KDF_RT_PATTERN, this.patternRefs);
        readTestDesc();
        readRoot_ComHash();
        /**
         * print and log head Data
         */
        format.resetHeadTimeFormat();
        if (this.isDebugMode()) {
            format.printHeadInfo();
        }
//		writeHeadData();

        testLogFile = new File(this.getFormat().getXmlPath() + "/" + this.file.getName());
        if (!this.removeTempLogFile()) {
            this.logIoErrorToES("FailDeleteLogFile");
            this.failType = Config.FailureCase.IOError;
            return false;
        }

        /**
         * here there are 3 types doc need to send to es A: Type=File, only
         * exist for ate since slt file is just unit B: Type=Unit, C:
         * Type=others for the details test items.
         *
         */
        // the lotHeadStr does't contains lot open/start/end time, we use the FieldType.FileTime as the timestamper
        String lotHeadStr = format.getLotHeadKVString() + "," + FieldType.FileTime + "=" + this.formatTimeStr(this.fileOpenTime);

        readBinDesc();
        System.out.printf("%s: opening kdf time is  %d\n", LocalDateTime.now(), (System.currentTimeMillis() - jobStartTime));

        int unitNo = 0;
        StringBuilder dataContent = new StringBuilder();
        for (Node unit : units) {
            unitNo++;
            slaveUnitCnt = 0;

            dataContent.setLength(0);
            format.clearUnitData();
            this.readUnit(unit);
            this.readUnitBin(unit);
            this.readUnitSerialNumber(unit);
            this.readOldKDFBinDesc(unit);

            format.calTestTime();
            format.resetUnitTimeFormat();
            format.setBinDesc();
            readFTSLTXY(waferNumber);

            /**
             * here we create the unit level doc <unitKVStr>
             * unitDocKVString contains the unit start and end timestamper, add
             * kdf file name in the unit level doc for ate only.
             */
            String unitKVStr = lotHeadStr + format.getUnitDocKVString() + getSlaveNodeKVString() + format.getX0Y0KVString();
            String slaveKVStr = this.getSlaveUnitKVString(lotHeadStr + format.getSlaveUnitDocKVString());

            if (this.getFormat().isAddFileName()) {
                unitKVStr += this.getFileNameKVStr();
            }

            if (validateFullForamtString(unitKVStr)) {
                dataContent.append(slaveKVStr).append(unitKVStr).append("\n");

            }
            else {
                System.err.println("Error Unit Doc: \n" + slaveKVStr + unitKVStr);
                System.err.flush();
                return false;
            }

            /**
             * here we create the head for each test time it combines the:
             * <lotHeadStr> which contains the lot info
             * <UnitHeadTestKVStr> which contains the unit level info and has no
             * the unit start/end time
             * <SlaveNodeKVString> which contains the slave die info for the
             * whole part
             */
            String testItemHeadStr = lotHeadStr + format.getUnitHeadTestKVStr() + format.getX0Y0KVString();
            if (this.getFormat().isAppendSlaveUnitId2Test()) {
                testItemHeadStr += getSlaveNodeKVString();
            }

            /**
             * print and log unit data
             */
            if (this.isDebugMode()) {
//				format.printUnitInfo();
                System.out.printf("Unit#%d KV String:\n%s\n", unitNo, unitKVStr);
            }
            this.unitLevelDocCnt = this.slaveUnitCnt + 1;

            for (Node item : unit.getChildren()) {

                String nodeName = item.getName();
                if (nodeName.equals(KdfTypes.KDF_RT_SERIAL)
                    || nodeName.equals(KdfTypes.KDF_RT_BIN)
                    || nodeName.equals(KdfTypes.KDF_RT_BINDESC)) {
                    continue;
                }
                this.nodeHead = testItemHeadStr;
//				this.nodeHead = "unit_no=" + unitNo;
                flowContextField = "";
                testResultFieldValue = "";
                subBaseClassField = "";
                comHashValue = "";
                String nodeKVString = printNodeInfo(item, 0);
                dataContent.append(nodeKVString);
                // log for debugging
                if (this.isDebugMode()) {
                    System.out.println(nodeKVString);
                }

            }
            this.fileLevelDocCnt += this.unitLevelDocCnt;

            // IO error and exit for this file
            if (!this.writeKVString(dataContent)) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailWriteLogFile");

                if (!this.removeTempLogFile()) {
                    this.logIoErrorToES("FailDeleteLogFile");
                }

                return false;
            }
        }
        // add the file level doc to es
        if (!this.writeFileKVString()) {
            return false;
        }

        // generate the kdf mapping file and for repeat kdf file check
        if (this.getFormat().isGenerateMappingFile() && (!this.generateMapFile())) {
            this.failType = Config.FailureCase.IOError;
            this.logIoErrorToES("FailCreateMapFile");
            if (!this.removeTempLogFile()) {
                this.logIoErrorToES("FailDeleteLogFile");
            }
            return false;
        }

        // only rename the log file in production mode
        if ((this.getFormat().isProductionMode()) && (!this.renameTempLogFile())) {
            this.failType = Config.FailureCase.IOError;
            this.logIoErrorToES("FailRenameLog");
            if (!this.removeTempLogFile()) {
                this.logIoErrorToES("FailDeleteLogFile");
            }

            if (!this.removeMapFile()) {
                this.logIoErrorToES("FailDeleteMapFile");
            }
            return false;
        }
        this.logKDFDoneToES();
        if (this.getFormat().getDataType().equals(Config.DataTypes.ATE)
            || this.getFormat().getDataType().equals(Config.DataTypes.SLT)
            || this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {

            String lotOperation = this.lotNumber + "_" + this.mfgStp;
            if (!this.getFormat().getLotOpertions().contains(lotOperation)) {
                this.getFormat().getLotOpertions().add(lotOperation);
            }
        }

        // only rename or archive the kdf in production mode
        this.renameOrArchiveKDF(this.doneArchiveFile, Config.KdfRename.done);

        System.out.printf("%s: successed to proceed kdf %s\n", LocalDateTime.now(), file.getName());
        System.out.printf("%s: total kdf reading time is : %d\n", LocalDateTime.now(), (System.currentTimeMillis() - jobStartTime));
        this.tree = null;
        kdfDoneCnt++;
        return true;
    }

    public
        boolean renameOrArchiveKDF(File destinationFile, Config.KdfRename rename) {
        if ((!Config.renameKDF) && destinationFile == null) {
            return false;
        }
        if (this.getFormat().isProductionMode()) {
            if ((!Config.renameKDF) && (!this.moveFileToArchive(destinationFile))) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailArchiveKDF");

                /**
                 * here we can rename the kdf if failed archive to avoid ....
                 */
                if (!this.renameKDFFile(rename)) {
                    this.failType = Config.FailureCase.IOError;
                    this.logIoErrorToES("FailRenameKDF");
                    return false;
                }
                else {
                    return true;
                }
            }

            if (!this.renameKDFFile(rename)) {
                this.failType = Config.FailureCase.IOError;
                this.logIoErrorToES("FailRenameKDF");
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * append the file level doc to es stream only for no-slt since slt
     * unit/file is the same
     *
     * @return
     */
    private
        boolean writeFileKVString() {
        // IO error and exit for this file
//		if(this.getFormat().getDataType().equals(Config.DataTypes.SLT)) {
//			return true;
//		}
        if (!this.writeKVString(this.getAteFileKVStr())) {
            this.failType = Config.FailureCase.IOError;
            this.logIoErrorToES("FailWriteLogFile");

            if (!this.removeTempLogFile()) {
                this.logIoErrorToES("FailDeleteLogFile");
            }

            return false;
        }
        return true;
    }

    private
        String getFileNameKVStr() {
        String value = "";
        if (this.getFormat().isAddFileName()) {
            value = "," + "FileName=" + this.kdfName;
        }
        return value;
    }

    private
        boolean removeMapFile() {
        if (this.mappingFile.exists()) {
            try {
                Files.delete(this.mappingFile.toPath());
            }
            catch (IOException ex) {
                Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;
    }

    private
        boolean renameTempLogFile() {
        File esTestFile = new File(this.testLogFile.getAbsolutePath() + ".log");
        return this.testLogFile.renameTo(esTestFile);
    }

    private
        boolean renameKDFFile(Config.KdfRename rename) {
        if (Config.renameKDF) {
            File kdfedFile = new File(this.file.getAbsolutePath() + "." + rename);
            return this.file.renameTo(kdfedFile);
        }
        return true;
    }

    /**
     * write the kv string in to log file
     *
     * @return
     */
    private
        boolean writeKVString(StringBuilder dataContent) {
        if (this.getFormat().isLogToFile()) {
            try {
                Files.write(testLogFile.toPath(), dataContent.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            }
            catch (IOException ex) {
                Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * write the kv string in to log file
     *
     * @return
     */
    private
        boolean writeKVString(String dataContent) {
        if (this.getFormat().isLogToFile()) {
            try {
                Files.write(testLogFile.toPath(), dataContent.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

            }
            catch (IOException ex) {
                Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            return true;
        }
        return true;
    }

    private
        boolean generateMapFile() {
        try {
            File parentFile = this.mappingFile.getParentFile();
            if (parentFile.exists() && parentFile.isDirectory()) {
            }
            else {
                if (!parentFile.mkdirs()) {
                    return false;
                }
            }
            return this.mappingFile.createNewFile();
        }
        catch (IOException ex) {
            Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private
        boolean removeTempLogFile() {
        if (this.testLogFile.exists()) {
            try {
                Files.delete(this.testLogFile.toPath());
            }
            catch (IOException ex) {
                Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        return true;

    }

    /**
     * H800A0010041704 read waferLotNumber, waferNumber, x and y from unit id
     *
     * @param waferNumber
     */
    private
        void readFTSLTXY(String waferNumber) {
        //H800A0010041704
        if (!this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            XmlNode waferNumberNode = this.getFormat().getUnit().getWaferNumberNode();
            XmlNode unitIdNode = this.getFormat().getUnit().getUnitIdNode();
            XmlNode xNode = this.getFormat().getUnit().getxCoordNode();
            XmlNode yNode = this.getFormat().getUnit().getyCoordNode();
            XmlNode waferLotNode = this.getFormat().getUnit().getWaferLotNode();

            if (unitIdNode.getXmlValue().length() > yNode.getEndIndex()) {
                waferNumberNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(waferNumberNode.getStartIndex(), waferNumberNode.getEndIndex())).toString());
                xNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(xNode.getStartIndex(), xNode.getEndIndex())).toString());
                yNode.setValue(Integer.valueOf(unitIdNode.getValue().substring(yNode.getStartIndex(), yNode.getEndIndex())).toString());
                waferLotNode.setValue(unitIdNode.getValue().substring(waferLotNode.getStartIndex(), waferLotNode.getEndIndex())
                    + "."
                    + unitIdNode.getValue().substring(waferLotNode.getEndIndex(), waferLotNode.getEndIndex() + 2));
            }
        }
        else {
            if (this.getFormat().getUnit().getWaferNumberNode().getValue() == null) {
                this.getFormat().getUnit().getWaferNumberNode().setValue(waferNumber);
            }
        }
    }

    private
        void readUnit(Node unit) {
        for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
            for (String fieldName : xmlNode.getFieldNames()) {
                if (unit.containsKey(fieldName)) {
                    xmlNode.setValue(unit.get(fieldName).toString());
                }
            }
        }
    }

    private
        void readUnitBin(Node unit) {
        Node[] bins = unit.getChildren(KdfTypes.KDF_RT_BIN);
        if (bins != null && bins.length > 0) {
            for (Node bin : bins) {
                for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (bin.containsKey(fieldName)) {
                            xmlNode.setValue(bin.get(fieldName).toString());
                        }
                    }
                }
            }
        }
    }

    private
        void readUnitSerialNumber(Node unit) {
        int dieCnt = 0;
        Node[] serialNumbers = unit.getChildren(KdfTypes.KDF_RT_SERIAL);
        if (serialNumbers != null && serialNumbers.length > 0) {
            for (Node serialNumber : serialNumbers) {
                for (XmlNode xmlNode : format.getUnit().getNodes().values()) {
                    for (String fieldName : xmlNode.getFieldNames()) {
                        if (serialNumber.containsKey(fieldName)) {

                            /**
                             * for slt and ate only slt and ate contains 4 die
                             * per device one master die and 3 slave die
                             */
                            if (xmlNode.isUnitIdNode()
                                && (!this.getFormat().getDataType().equals(Config.DataTypes.WaferSort))
                                && (!serialNumber.get("master").getValue().toString().equals("1"))) {
                                //add for slave unit id if unit id is not null
                                if (serialNumber.get(fieldName) != null) {
                                    this.getFormat().getUnit().getSlaveUnits().add(
                                        new SlaveUnit(serialNumber.get(fieldName).toString(), serialNumber.get("componentHash").toString())
                                    );
                                }
                                continue;

                            }

                            xmlNode.setValue(serialNumber.get(fieldName).toString());
                        }
                    }
                }
            }
        }
        /**
         *
         * here we have to generate a dummy unit id for the master die only
         * format:
         */
        // TODO
        if (this.getFormat().getUnit().getUnitIdNode().getXmlValue().isEmpty()) {

        }
    }

    private
        void readOldKDFBinDesc(Node unit) {
        // very very old kdf file store bin info in unit
        Node[] binDescs = unit.getChildren(KdfTypes.KDF_RT_BINDESC);
        if (binDescs != null && binDescs.length > 0) {
            for (Node binDesc : binDescs) {
                String binType = "";
                String binNumber = null;
                String binDescription = null;
                String flag = null;
                for (String key : binDesc.keySet()) {
                    if (key.equalsIgnoreCase("bintype")) {
                        binType = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binNumber")) {
                        binNumber = binDesc.get(key).getValue().toString();
                    }
                    else if (key.equalsIgnoreCase("binDescription")) {
                        Object o = binDesc.get(key).getValue();
                        if (o != null) {
                            binDescription = this.formatBinDesc(o.toString());
                        }
                    }
                    else if (key.equalsIgnoreCase("binDescFlag")) {
                        flag = binDesc.get(key).getValue().toString();
                    }
                }
                //SoftBin
                if (binNumber != null
                    && flag != null) {
                    Bin bin = new Bin(binNumber, binDescription, flag);
                    if (binType.equals("83") || binType.equals("S")) {
                        format.getUnit().setSoftBinDesc(bin.getBinDescription());
                        format.getUnit().getSoftBinNode().setValue(binNumber);
                        format.getUnit().setFlag(bin.getFlag());
                    } //HardBin
                    else if (binType.equals("72") || binType.equals("H")) {
                        format.getUnit().setHardBinDesc(binDescription);
                        format.getUnit().getHardBinNode().setValue(binNumber);
                        format.getUnit().setFlag(bin.getFlag());
                    }
                }
            }
        }
    }

    private
        String formatBinDesc(String binDesc) {
        int size = binDesc.length();
        String value = "";
        int i = 0;
        while (i < size && i < 128) {
            char chr = binDesc.charAt(i);
//			if(chr == ',' || chr == '=' || chr == 13 || chr == 10) {
//
//			}
            if ((chr >= 48 && chr <= 57)
                || (chr >= 65 && chr <= 90)
                || (chr >= 97 && chr <= 122)) {

            }
            else {
                chr = 32;
            }
            value += chr;
            i++;
        }
        return value;
    }

    private
        String printNodeInfo(Node node, int level) {

        String formatString = "";
        if (node == null) {
            return formatString;
        }
        String space = "";
        switch (level) {
            case 0:
                space = "--";
                break;
            case 1:
                space = "  |--";
                break;
            case 2:
                space = "    |--";
                break;
            case 3:
                space = "      |--";
                break;
            case 4:
                space = "        |--";
                break;
            case 5:
                space = "          |--";
                break;
        }

        space = "";
        String nodeName = node.getName();
        if (nodeName == null) {
            System.out.println("Warnning: empty node name node found: " + node.toString());
            return formatString;
        }
        boolean isFlow = false;
        if (nodeName.equals(KdfTypes.KDF_RT_FLOW)) {
            this.flowContextField = "";
            isFlow = true;
            baseClass = null;
        }

        if (nodeName.equals(KdfTypes.KDF_RT_TEST)) {
            // reset all the test related items here
            this.baseClass = null;
            this.testCntInFlow++;
            this.subBaseClassField = "";
            this.comHashValue = "";
            this.testResultFieldValue = "";

            String idClass = null;
            if (node.get("testDescId") != null) {
                idClass = node.get("testDescId").toString();
            }

            if (idClass != null && this.testDescRefs.containsKey(idClass)) {
                this.subBaseClassField = this.testDescRefs.get(idClass).getValue();

                // return immediately if validate failed
                if (!validateBaseSubClass(idClass)) {
                    return formatString;
                }
            }
            if (this.getFormat().isEnabledComponentHash()) {
                String comHash = null;
                if (node.get("componentHash") != null) {
                    comHash = node.get("componentHash").toString();
                }
                if (comHash != null && this.comHashRefs.containsKey(comHash)) {
                    this.comHashValue = this.comHashRefs.get(comHash).getKVString();
                }
            }

        }

        if (!validateNodeType(nodeName, node)) {
//			 skip those nodes
            if (this.isDebugMode()) {
                System.out.println("Skip this Node Type: " + node);
            }
            return formatString;
        }
        else {
            /**
             * handle the current node here
             */

            String formatNodeString = formateNode(node) + "\n";
            if (validatePartForamtString(formatNodeString)) {
                formatString += space + this.nodeHead + formatNodeString;
                this.unitLevelDocCnt++;
            }
            else {
                System.out.println("--------------------------------------------------------------------");
                System.out.println("FATAL Error: node format string validation failed");
                System.out.println("FATAL Error: es can not read this node string");
                System.out.println("Node=" + node);
                System.out.printf("formatString is:%s", formatNodeString);

                //System.exit(1);
            }

        }
        if (isFlow) {
            KDFFieldData flowContextData = node.get("context");
            if (flowContextData != null && (!flowContextData.isNull())) {
                String contextData = flowContextData.getValue().toString();
                if ((contextData != null) && (!contextData.isEmpty()) && (this.getFormat().getFlowContextFilters().contains(contextData))) {
                    return formatString;
                }
            }
        }

        // return immediately after slt test
        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT) && nodeName.equals(KdfTypes.KDF_RT_TEST)) {
            return formatString;
        }

        /**
         * handle the child node here
         */
        for (Node item : node.getChildren()) {
            if (item != null) {
                formatString += printNodeInfo(item, level + 1);
            }
        }
        return formatString;
    }

    /**
     * method to check if the logging is disabled to this node validate the node
     * name filter validate the log fail filter
     *
     * @return
     */
    private
        boolean validateNodeType(String nodeType, Node node) {
        if ((!this.getFormat().getNodeTypeFilters().isEmpty()) && this.getFormat().getNodeTypeFilters().contains(nodeType)) {
            return false;
        }
        if ((!this.getFormat().getNodeTypeSelectors().isEmpty()) && (!this.getFormat().getNodeTypeSelectors().contains(nodeType))) {
            return false;
        }

        /**
         * log fail filters A: fail node type selector B: base calss selector
         */
        if (this.getFormat().getLogOnlyFailNodes().contains(nodeType)
            && this.getFormat().getLogFailOnlyBaseClasses().contains(this.baseClass)) {
            KDFFieldData fieldData = node.get("result");
            if (fieldData == null) {
                return true;
            }
            String fieldValue = fieldData.toString().trim();
            if (this.isDebugMode()) {
                System.out.println();
            }
            return fieldValue.equals("0");
        }

        return true;
    }

    /**
     * this method is to filter and selector the base and sub class
     *
     * @param baseSubClass
     * @return
     */
    private
        boolean validateBaseSubClass(String idClass) {

        // slt class filters is in the readTestDesc method, slt TestDesc is different from ate
        if (this.getFormat().getDataType().equals(Config.DataTypes.SLT)) {
            return true;
        }
        //selector and filters

        String subClassName = this.testDescRefs.get(idClass).getSubClass();
        String baseClassName = this.testDescRefs.get(idClass).getBaseClass();
        this.baseClass = baseClassName;

        // validate the base class first
        if (!this.getFormat().getBaseClassFilters().isEmpty()) {
            // filter out
            if (this.getFormat().getBaseClassFilters().contains(baseClassName)) {
                return false;
            }
        }
        if (!this.getFormat().getBaseClassSelectors().isEmpty()) {
            // selector
            if (!this.getFormat().getBaseClassSelectors().contains(baseClassName)) {
                return false;
            }
        }

        // validate the sub class
        if (!this.getFormat().getSubClassFilters().isEmpty()) {
            // filter out
            if (this.getFormat().getSubClassFilters().contains(subClassName)) {
                return false;
            }
        }

        if (!this.getFormat().getSubClassSelectors().isEmpty()) {
            // selector
            if (!this.getFormat().getSubClassSelectors().contains(subClassName)) {
                return false;
            }
        }

        return true;
    }

    /**
     * validate a string return false if this string contains char ',' or '='
     * else return true
     *
     * @param formatString
     * @return
     */
    private
        boolean validatePartForamtString(String formatString) {
        int length = formatString.length();
        int commaCnt = 0;
        int equalityCnt = 0;
        while (length-- > 0) {
            char chr = formatString.charAt(length);
//			if(chr == '\n') {
//				System.out.println("Error: bad format string which contains LF," + formatString);
//				return false;
//			}
            if (chr == ',') {
                commaCnt++;
            }
            else if (chr == '=') {
                equalityCnt++;
            }
        }
        return (commaCnt == equalityCnt);

    }

    /**
     * validate a string return false if this string contains char ',' or '='
     * else return true
     *
     * @param formatString
     * @return
     */
    private
        boolean validateFullForamtString(String formatString) {
        int length = formatString.length();
        int commaCnt = 0;
        int equalityCnt = 0;
        while (length-- > 0) {
            char chr = formatString.charAt(length);
//			if(chr == '\n') {
//				System.out.println("Error: bad format string which contains LF," + formatString);
//				return false;
//			}
            if (chr == ',') {
                commaCnt++;
            }
            else if (chr == '=') {
                equalityCnt++;
            }
        }
        return (commaCnt + 1 == equalityCnt);

    }

    private
        String formatTestNode(Node node) {
        String value = "";
        String testResultField = this.getFieldKVStr(node, "result", "result");
        String startTimeField = this.getFieldKVStr(node, "startTimestamp", "startTimestamp");
        String endTimeField = this.getFieldKVStr(node, "endTimestamp", "endTimestamp");
        String alarmField = this.getFieldKVStr(node, "alarm", "alarm");

        if (!testResultField.isEmpty()) {
            value += "," + testResultField;
            this.testResultFieldValue = testResultField;
        }
        if (!this.getFormat().getFieldFiters().contains("startTimestamp")) {
            if (!startTimeField.isEmpty()) {
                value += "," + startTimeField;
            }
        }
        if (!this.getFormat().getFieldFiters().contains("endTimestamp")) {
            if (!endTimeField.isEmpty()) {
                value += "," + endTimeField;
            }
        }
        if (!this.getFormat().getFieldFiters().contains("alarm")) {
            if (!alarmField.isEmpty()) {
                value += "," + alarmField;
            }
        }

        // here add the test type node test time
        value += this.getTestTimeValue(node);

        return value;

    }

    private
        String getTestTimeValue(Node node) {
        String value = "";
        if (this.getFormat().getFieldFiters().contains("TestTime")) {
            return value;
        }
        // here add the test type node test time
        if (node.containsKey("startTimestamp")) {
            String startTimeValue = node.get("startTimestamp").toString().trim();
            if (node.containsKey("endTimestamp")) {
                String endTimeValue = node.get("endTimestamp").toString().trim();
                if (startTimeValue.length() == 13 && endTimeValue.length() == 13) {
                    value = ",TestTime=" + (Long.valueOf(endTimeValue) - Long.valueOf(startTimeValue)) / 1000.0;
                }
            }
        }
        return value;
    }

    /**
     * bug1: Type: StructureMeasure, Parent Type: Test, Data: [result=1,
     * highSpecLimit=20.0, structureName=RDIL0:Temp, paramRefPtr=,
     * componentHash=, lowSpecLimit=-20.0, units=, value=1.25] bug2: Type:
     * BitString, Parent Type: Test, Data: [bitValues=0000, paramRefPtr=,
     * componentHash=, segmentName=VAR:Rev_ID;EXPR:''0000'']
     *
     * @param node
     * @return
     */
    private
        String formateNode(Node node) {
        String value = "";
        String nodeType = node.getName().trim();

        if (!this.flowContextField.isEmpty()) {
            value += "," + this.flowContextField;
        }

        if (nodeType.equals(KdfTypes.KDF_RT_EVALUATION)) {
            value += "," + this.testResultFieldValue;
        }
        if (!this.subBaseClassField.isEmpty()) {
            value += this.subBaseClassField;
        }
        value += this.comHashValue;

        // add the node type
        if (!nodeType.isEmpty()) {
            value += "," + FieldType.Type + "=" + node.getName().trim();
        }

        if (nodeType.equals("Test")) {
            value += this.formatTestNode(node);
            return value;
        }

//			if (nodeType.equals(RecordTypes.KDF_RT_ULSD)) {
//				value += this.formatULSDNode(node);
//				return value;
//			}
        boolean isLog = true;
        if (nodeType.equals(RecordTypes.KDF_RT_ULSD)) {
            isLog = false;
        }

        for (String fieldName : node.keySet()) {
            Byte fieldType = node.get(fieldName).getType();

//			if ((!this.allFields.contains(fieldName)) && (fieldType == 11 || fieldType == 22 || fieldType == 33)) {
//				this.allFields.add(fieldName);
//			}
            String fieldValue = node.get(fieldName).toString().trim();

            if (fieldName != null && fieldName.trim().isEmpty()) {
                continue;
            }
            fieldName = fieldName.trim();
            if (this.getFormat().getFieldFiters().contains(fieldName)) {
                continue;
            }

            if (!fieldValue.isEmpty()) {
//                if (!this.isFormatField(fieldValue, fieldName, isLog)) {
//                    continue;
//
//                }
                if (fieldName.equals(FieldType.PinRefPtr)) {
                    String pinName = this.pinRefs.get(fieldValue);
                    if (fieldValue.contains(",")) {
                        System.out.println("Multip Pin found: " + fieldValue);
                    }
                    if (pinName != null) {
                        value += "," + FieldType.Pin + "=" + pinName;
                    }
                }
                else if (fieldName.equals(FieldType.PatternId)) {
                    String pattern = this.patternRefs.get(fieldValue);
                    if (fieldValue.contains(",")) {
                        System.out.println("Multip Patterns found: " + fieldValue);
                    }
                    if (pattern != null) {
                        value += "," + FieldType.Pattern + "=" + pattern;
                    }
                }
                else {
                    value += this.getFieldKVStr(fieldName, fieldValue);
//                    value += "," + fieldName.replace('.', '_') + "=" + fieldValue;

                }
            }
        }
        if (node.getName().equals(KdfTypes.KDF_RT_FLOW)) {
            // all the child nodes whose parent node is flow, add flow context and flow result into sub nodes
            this.flowContextField = this.getFieldKVStr(node, "context", "context");
//				this.testResultField += this.getFieldKVStr(node, "result", "flowResult");
            testCntInFlow = 0;
            value += this.getTestTimeValue(node);
        }
        return value;

    }

    /**
     * get the filed kv value return empty if key or value is empty
     *
     * @param node
     * @param fieldName
     * @return
     */
    private
        String getFieldKVStr(Node node, String fieldName, String aliasName) {
        String value = "";

        KDFFieldData fieldData = node.get(fieldName);
        if (fieldData == null) {
            return value;
        }
        String fieldValue = fieldData.toString().trim();
        if ((!fieldValue.isEmpty()) && this.isFormatField(fieldValue, fieldName, true)) {
            if (aliasName != null) {
                fieldName = aliasName;
            }
            value = fieldName + "=" + fieldValue;
        }

        return value;
    }

    /**
     *
     * @param fieldName
     * @param fieldValue
     * @return
     */
    private
        String getFieldKVStr(String fieldName, String fieldValue) {
        String value = "";
        if (fieldName.isEmpty() || fieldValue.isEmpty()) {
            return value;
        }
        int length = fieldValue.length();
        if (!fieldName.equals("bitValues")) {
            if (length > this.format.getFieldValueLengthLimit()) {
                if (this.isDebugMode()) {
                    System.out.printf("Too long Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                }
                return value;
            }
        }
        if (fieldName.equals("value")) {
            try {
                float numberValue = Float.valueOf(fieldValue);
                if (Float.isInfinite(numberValue)) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                    return value;
                }
            }
            catch (NumberFormatException e) {
                System.out.printf("Bad Format Field: value should be float, fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                return value;
            }
        }
        String formatValue = this.formatFieldValue(fieldValue);
        if (formatValue.isEmpty()) {
            return value;
        }
        value = "," + fieldName.replace('.', '_').replace('=', ':').replace(',', ';') + "=" + formatValue;
        return value;

    }

    private
        String formatFieldValue(String fieldValue) {
        int length = fieldValue.length();
        String value = "";
        int i = 0;
        while (i != length) {
            char chr = fieldValue.charAt(i);
            if (chr == ',') {
                value += ';';
            }
            else if (chr == '=') {
                value += ':';
            }
            else if (chr != 13 && chr != 10) {
                value += chr;
            }
            i++;
        }
        return value;

    }

    private
        boolean isFormatField(String fieldValue, String fieldName, boolean isLog) {
        int length = fieldValue.length();
        if (length > this.format.getFieldValueLengthLimit()) {
            if (this.isDebugMode()) {
                System.out.printf("Too long Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
            }
            return false;
        }

        while (length-- > 0) {
            char chr = fieldValue.charAt(length);
            if (chr == ',' || chr == '=' || chr == 13 || chr == 10) {
                if (isLog) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                }
                return false;
            }
        }

        if (fieldName.equals("value")) {
            try {
                float value = Float.valueOf(fieldValue);
                if (Float.isInfinite(value)) {
                    System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                    return false;
                }
            }
            catch (NumberFormatException e) {
                System.out.printf("Bad Format Field: fieldName=%s, fieldValue=%s\n", fieldName, fieldValue);
                return false;
            }
        }
        return true;

    }

    private
        String formatULSDNode(Node node) {
        //Type: ULSD, Parent Type: Unit, Data: [hostName=atdtsocket204.higon.com, queryTimeout=10000, totalQueryTime=2.045, recvTime=2.045, queriedTests=[[name='FUSE.LOG_MODE=SOFT'], [segmentName='Top.DIE3.DIE3FuseRam'], [segmentName='Top.DIE3.DIE3FuseRam'].bitValues]], portNumber=9999, timeout=0, queryType=3, connectTime=6.935, connectTimeout=5000, networkError=0, results=[[FT, DNXSP3XX1A0XFP0, F3, AANFNP, 000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000100000000000000000000000000000000000000000000000000000000100001000000000000000000000000000000000000000000000000000000000001000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001010011100000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000001000000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001100010110000000000000000000000000000000000000000000000000000000010000010000000000000000000000000000000000000000000000000000000011100000100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000010100001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001001000010100100010010010001000010010000100101101110010110111000100100111010010011001001001001110111111100110111110101110101111110101111110001110110111010110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001101000111010000000000000000000000000000000000000000000000000000001001100101011011010101011000100010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000101000010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001110110110000000000000000000000000101011110000000000000000000000000000011000101001000000000000000000000000011001001000000000000000000000000000011000100101000000000000000000000000010110010100110010000001100100000110001101010001111111000101111101010101111001110101110101101101101110111101101001111101100010010101001010000101000110110100111111110000000000000000000000000000000000000000000000000000000000000000000000000101111010110101111001010101111010000101111010010101110100110101110100101100000001111100000000101100000001001100000001011011110110111011110110100001110100000001110011000001110011010001110011010001101010010001101010010110111110101101111111001101110100010111100000110111101100010111100101100111101001100111101011110111101011010111100111110111101011000111100011000111101011110111100100110111101000000111101010110111100110000111100011000111100101010111100100110111100100100111100011010111100010110111100100000111100010110111100101100111100011100111000000011111000000110110010001111110110010101110111110011110111011001110011101001110111000000110110001001111000000000110010111101110101001111110111010011101100110100110110001000110001010000110100000010110011111100110101111010110110001010110100101100110110010100110010111010110101110110110100001100110110011100110101010110110110001000110110010110110110010010110101110010110110001100110100111100110110011100110101011000110110000100110110010100110101100110110100111010110101011010110101001000110101001000110100110100110100110000110101000000110100101100110101001010110100110010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100110010101001010100101110010000000000000000000000000000000000001111000000100001110010001001010100000000000000000000000001000011000010100000000000000000000000000000000000010010101101100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000111111111111111111111110000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010101011011001100100111010110100111101000000110001110010000000000000000000000000000000111]], status=Success]
        String value = FieldType.Type + "=" + RecordTypes.KDF_RT_ULSD;
        String names[] = node.toString().split(",");
        int size = names.length;
        while (size-- > 0) {
            if (!names[size].contains("[")) {
                if (names[size].contains("=")) {
                    value += "," + names[size].trim().replaceAll("]", "");
                }
            }
            else {
            }
        }
        return value;

    }

    private
        String genericFormat(Node node) {
        String value = "";
        String names[] = node.toString().split(",");
        int size = names.length;
        while (size-- > 0) {
            String content = names[size];
            if (!content.contains("=")) {
                String[] field = content.split(":");
                if (field.length == 2) {
                    value += field[0].replace('[', ' ').replace(']', ' ').trim() + "=" + field[1].replace('[', ' ').replace(']', ' ').trim();
                }
            }
            else {
                //

            }

        }
        return value;

    }

    /**
     * this method is to generate the kv string for the slave die format:
     * ,DIE.1=unitID_1,DIE_2=unitID_2
     *
     *
     * @return
     */
    private
        String getSlaveNodeKVString() {
        String value = "";
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            return value;
        }
        for (SlaveUnit slaveUnit : this.getFormat().getUnit().getSlaveUnits()) {
            if (slaveUnit.getComHash() != null) {
                ComponentHash componentHash = this.comHashRefs.get(slaveUnit.getComHash());

                if (componentHash != null && componentHash.getComName() != null && (!componentHash.getComName().isEmpty())) {
                    value += "," + componentHash.getComName() + "=" + slaveUnit.getUnitId();
                }
            }
        }
        return value;
    }

    private
        String getSlaveUnitKVString(String lotHeadStr) {
        String value = "";
        if (this.getFormat().getDataType().equals(Config.DataTypes.WaferSort)) {
            return value;
        }
        for (SlaveUnit slaveUnit : this.getFormat().getUnit().getSlaveUnits()) {
            if (slaveUnit.getComHash() != null) {
                ComponentHash componentHash = this.comHashRefs.get(slaveUnit.getComHash());

                if (componentHash != null && componentHash.getComName() != null && (!componentHash.getComName().isEmpty())) {
                    //value += "," + componentHash.getComName() + "=" + slaveUnit.getUnitId();
                    String tempStr = "";
                    XmlNode waferNumberNode = this.getFormat().getUnit().getWaferNumberNode();
                    XmlNode waferLotNode = this.getFormat().getUnit().getWaferLotNode();
                    XmlNode xNode = this.getFormat().getUnit().getxCoordNode();
                    XmlNode yNode = this.getFormat().getUnit().getyCoordNode();
//					tempStr += "," + FieldType.Type + "=" + FieldType.Unit;
                    tempStr += "," + this.getFormat().getUnit().getUnitIdNode().getName() + "=" + slaveUnit.getUnitId();
                    tempStr += "," + FieldType.FileName + "=" + this.file.getName();
                    tempStr += "," + FieldType.DieType + "=" + componentHash.getComName();
                    String masterDieId = this.getFormat().getUnit().getUnitIdNode().getXmlValue();
//                    if (!masterDieId.isEmpty()) {
//                        tempStr += "," + FieldType.MasterDieId + "=" + this.getFormat().getUnit().getUnitIdNode().getValue();
//                    }

                    if (slaveUnit.getUnitId().length() > this.getFormat().getUnit().getyCoordNode().getEndIndex()) {
                        tempStr += "," + waferNumberNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(waferNumberNode.getStartIndex(), waferNumberNode.getEndIndex())).toString());
                        tempStr += "," + xNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(xNode.getStartIndex(), xNode.getEndIndex())).toString());
                        tempStr += "," + yNode.getName() + "="
                            + (Integer.valueOf(slaveUnit.getUnitId().substring(yNode.getStartIndex(), yNode.getEndIndex())).toString());
                        tempStr += "," + waferLotNode.getName() + "="
                            + slaveUnit.getUnitId().substring(waferLotNode.getStartIndex(), waferLotNode.getEndIndex())
                            + "."
                            + slaveUnit.getUnitId().substring(waferLotNode.getEndIndex(), waferLotNode.getEndIndex() + 2);
                    }
                    tempStr += "\n";
////					if(validatePartForamtString(tempStr)) {
                    value += lotHeadStr + tempStr;
                    slaveUnitCnt++;
//					}

                }
            }
        }
        return value;
    }

    /*
     * private void writeHeadData() { this.fileOpenTime = ""; String[] names =
     * this.file.getName().split("_"); for (int index :
     * format.getFileOpenTimeIndex()) { if (names[index].contains(".")) {
     * names[index] = names[index].substring(0, names[index].indexOf('.')); }
     * fileOpenTime += names[index]; } if (fileOpenTime.length() == 12) {
     * fileOpenTime = "20" + fileOpenTime; } if (fileOpenTime.length() != 14) {
     * System.out.println("FATAL Error: failed to get file open time from the
     * file name"); System.exit(1); }
     *
     * XmlNode lotStartTimeNode = format.getLotStartTimeNode(); XmlNode
     * lotOpenTimeNode = format.getLotOpenTimeNode();
     * System.out.printf("fileOpenTime = %s, lotStartTime = %s, lotOpenTime =
     * %s\n", fileOpenTime, lotStartTimeNode.getValue(),
     * lotOpenTimeNode.getValue());
     *
     * if (lotStartTimeNode != null && lotStartTimeNode.getValue() != null) { if
     * (lotOpenTimeNode != null && lotOpenTimeNode.getValue() != null) {
     *
     * }
     * else { lotOpenTimeNode.forceValueTo(lotStartTimeNode.getValue()); } }
     * else {
     *
     * if (lotOpenTimeNode != null && lotOpenTimeNode.getValue() != null) {
     * lotStartTimeNode.forceValueTo(lotOpenTimeNode.getValue()); } else {
     * lotOpenTimeNode.forceValueTo(fileOpenTime);
     * lotStartTimeNode.forceValueTo(fileOpenTime); } }
     *
     * if (lotStartTimeNode.getValue().compareTo(fileOpenTime) < 0) {
     * lotStartTimeNode.forceValueTo(fileOpenTime); }
     *
     * root.clearContent(); for (XmlNode node : format.getLotHead().values()) {
     * root.addElement(node.getName()).setText(node.getXmlValue());
     *
     * }
     * root.addElement("src_to_xml_time").setText("1");
     * root.addElement("xml_file_generated_time").setText(LocalDateTime.now().toString());
     * root.addElement("kdf_file_name").setText(this.file.getName());
     *
     * }
     */
 /*
     * private void writeUnitData() { Element unit = root.addElement("Unit");
     * for (XmlNode node : format.getUnit().getNodes().values()) {
     * unit.addElement(node.getName()).setText(node.getXmlValue()); } //add
     * binDesc
     * unit.addElement("soft_bin_desc").setText(format.getUnit().getSoftBinDescValue());
     * unit.addElement("hard_bin_desc").setText(format.getUnit().getHardBinDescValue());
     * unit.addElement("test_result").setText(format.getUnit().getFlagValue());
     *
     * }
     *
     */
    private
        String getLotDate() {
        String[] names = this.file.getName().split("_");
        //String factoryName = names[0];
        String lotDate = "20" + names[7].substring(0, 2) + "-" + names[7].substring(2, 4) + "-" + names[7].substring(4, 6);
        return lotDate;
    }

    private
        boolean setKDFDate() {
        String[] names = this.file.getName().split("_");
        this.kdfMonth = names[format.getKdfMonthIndex()];
        switch (this.kdfMonth.length()) {
            case 4:
                //0604
                this.kdfMonth = "20" + kdfMonth;
                break;
            case 6:
                //180604
                this.kdfMonth = "20" + kdfMonth.substring(0, 4);
                break;
            case 8:
                this.kdfMonth = this.kdfMonth.substring(0, 6);
                break;
            default:
                return false;
        }

        this.fileOpenTime = "";
        // set file open time
        for (int index : format.getFileOpenTimeIndex()) {
            if (names[index].contains(".")) {
                names[index] = names[index].substring(0, names[index].indexOf('.'));
            }
            fileOpenTime += names[index];
        }
        if (fileOpenTime.length() == 12) {
            fileOpenTime = "20" + fileOpenTime;
        }
        if (fileOpenTime.length() != 14) {
            System.out.println("FATAL Error: failed to get file open time from the file name");
            return false;
        }
        this.kdfDate = fileOpenTime.substring(0, 8);
        return true;
    }

    private
        String formatTimeStr(String timeStr) {
        return timeStr.substring(0, 4) + "-" + timeStr.substring(4, 6) + "-" + timeStr.substring(6, 8)
            + "T" + timeStr.substring(8, 10) + ":" + timeStr.substring(10, 12) + ":"
            + timeStr.substring(12, 14) + ".00+08:00";
    }

    /**
     * judge the kdf type never change the judge order!!!
     */
    /**
     * public boolean getKDFType() { xmlDate =
     * LocalDate.now().format(DateTimeFormatter.ofPattern("uuuuMMdd")); String[]
     * names = this.file.getName().split("_"); if
     * (names[0].equals(KDFType.KDF_GFDRS) && names.length == 11) { factoryName
     * = names[0]; testerNumber = names[3].toLowerCase(); lotNumber = names[4];
     * mfgStep = names[5]; kdfMonth = "20" + names[9].substring(0, 4);
     * lotOpenTime = "20" + names[9] + names[10]; fileOpenTime = lotOpenTime;
     *
     * platform = names[2]; if (platform.equals("P")) { platform = "93k_data"; }
     * else if (platform.equals("S")) { platform = "sapphire_data"; } } else if
     * (names[0].equals(KDFType.KDF_SUZ) && names.length == KDFType.KDF_93K_FT)
     * { factoryName = names[0]; testerNumber = names[3].toLowerCase();
     * lotNumber = names[4]; mfgStep = names[5]; kdfMonth = "20" +
     * names[7].substring(0, 4); lotOpenTime = "20" + names[7] +
     * names[8].substring(0, 6); fileOpenTime = lotOpenTime;
     *
     * platform = names[2]; if (platform.equals("P")) { platform = "93k_data"; }
     * else if (platform.equals("S")) { platform = "sapphire_data"; } } else if
     * (names.length == KDFType.KDF_ASLT && names[KDFType.KDF_ASLT -
     * 3].toLowerCase().startsWith(KDFType.KDF_SUZASLT_Tester)) { factoryName =
     * KDFType.KDF_SUZ; testerNumber = names[KDFType.KDF_ASLT -
     * 3].toLowerCase(); lotNumber = names[0]; mfgStep = names[1]; kdfMonth =
     * names[6].substring(0, 4); lotOpenTime = names[6] + names[7].substring(0,
     * 6); fileOpenTime = lotOpenTime;
     *
     * platform = KDFType.KDF_SUZASLT_Tester; } else {
     * System.out.println("unsupportted kdf type found"); return false; } return
     * true; }
     *
     * @return
     */
    public
        DataFormat getFormat() {
        return format;
    }

    public
        void setTree(KDFInstanceTree tree) {
        this.tree = tree;
    }

    public
        void setFormat(DataFormat format) {
        this.format = format;
    }

    public
        Config.FailureCase getFailType() {
        return failType;
    }

    private
        boolean moveFileToArchive(File destinationFile) {
        try {
            if (destinationFile.exists()) {
                Files.delete(this.file.toPath());
            }
            else {
                if (!destinationFile.getParentFile().exists()) {
                    if (!destinationFile.getParentFile().mkdirs()) {
                        this.logIoErrorToES("FailMkDIR");
                        return false;
                    }
                }
            }
            Files.move(this.file.toPath(), destinationFile.toPath(), ATOMIC_MOVE);
        }
        catch (IOException ex) {
            System.out.println("EventType:ArchieveFailure");
            Logger.getLogger(KDFReader.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public
        boolean chooseFormat(File file) {
        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if (!dataFormat.isEnabled()) {
                continue;
            }
            if (dataFormat.getDataType().equals(Config.DataTypes.SMAP)
                || dataFormat.getDataType().equals(Config.DataTypes.WAT)) {
                continue;
            }
            String sourceType = file.getName().split("_")[dataFormat.getSourceTypeIndex()];
            if (sourceType.equals(dataFormat.getSourceType()) || sourceType.toLowerCase().contains(dataFormat.getSourceType().toLowerCase())) {
                this.setFormat(dataFormat);
                return true;
            }
        }
        return false;
    }

    public
        boolean isDebugMode() {
        return debugMode;
    }

    public
        File getExceptionArchiveFile() {
        return exceptionArchiveFile;
    }

    public
        int getKdfDoneCnt() {
        return kdfDoneCnt;
    }

    public static
        void main(String[] args) throws Exception {
        long startTime = System.currentTimeMillis();
        new Config("config/dataformat.xml");
        KDFReader loader = new KDFReader();

        for (DataFormat dataFormat : Config.dataFormats.values()) {
            if (dataFormat.getDataType().equals(Config.DataTypes.ATE)
                || dataFormat.getDataType().equals(Config.DataTypes.SLT)
                || dataFormat.getDataType().equals(Config.DataTypes.WaferSort)) {
                if (!dataFormat.isEnabled()) {
                    continue;
                }

                loader.setFormat(dataFormat);
                dataFormat.setProductionMode(false);

                File stageFile = new File(dataFormat.getKdfPath());
                for (File file : stageFile.listFiles()) {

                    loader.loadFile(file);

                }
            }
        }

        //System.out.println(loader.allFields.toString());
        System.out.println("total time = " + (System.currentTimeMillis() - startTime));
    }

}
