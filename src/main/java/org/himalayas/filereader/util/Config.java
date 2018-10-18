/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.himalayas.filereader.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.ElementHandler;
import org.dom4j.ElementPath;
import org.dom4j.io.SAXReader;

public
	class Config {

	public static
		boolean renameKDF = false;

	public static
		boolean convertTime = true;

	public static
		String lockFilePath = null;
	public static
		HashMap<String, DataFormat> dataFormats = new HashMap();

	public static
		enum DataTypes {
		WaferSort, ATE, SLT, WAT, SMAP
	};

	public static
		enum FailureCase {
		BadFormat, OpenFailure, Exception, RepeatKDF, IOError
	};

	public static
		enum KdfRename {
		exception, done, skip, openErr, badFormat
	}

	public static
		enum EventType {
		KDFDone, KDFRepeat, KDFBadFormat, KDFOpenFailure, IOError
	}

	public static
		String testDescId = null;
	public static
		String baseClass = null;
	public static
		String subClass = null;
	public static
		DataFormat watFormat = null;
	public static
		DataFormat smapFormat = null;

	public
		Config(String configFile) {
		if (!readDataFormat(configFile)) {
			System.out.println("please setup correct config file");
		}
		for (DataFormat dataFormat : dataFormats.values()) {
			if (!dataFormat.validate()) {
				System.exit(1);
			}
		}
	}

	private
		boolean readDataFormat(String configFile) {

		SAXReader reader = new SAXReader();
		Document document = null;

		reader.addHandler("/root/LockFilePath", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				lockFilePath = row.getTextTrim();
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/ConvertTime", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				convertTime = row.getTextTrim().equals("1");
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/RenameKDF", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				renameKDF = row.getTextTrim().equals("1");
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/TestDescId", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				testDescId = row.getTextTrim();
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/BaseClass", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				baseClass = row.getTextTrim();
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/SubClass", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element row = path.getCurrent();
				subClass = row.getTextTrim();
				row.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});

		reader.addHandler("/root/SourceType", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element sourceTypeNode = path.getCurrent();
				DataFormat dataFormat = new DataFormat(sourceTypeNode);
				sourceTypeNode.detach();
				if (Config.dataFormats.containsKey(dataFormat.getSourceType())) {
					System.out.println("Fatal Error: duplicate source data found " + dataFormat.getSourceType());
					System.exit(1);
				}
				if (dataFormat.getSourceType().equalsIgnoreCase("wat")) {
					Config.watFormat = dataFormat;
				}
				else if (dataFormat.getSourceType().equalsIgnoreCase("smap")) {
					Config.smapFormat = dataFormat;
				}
				Config.dataFormats.put(dataFormat.getSourceType(), dataFormat);
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/Head", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element head = path.getCurrent();
				List<Element> xmlHeadNodes = head.elements();
				for (Element xmlNode : xmlHeadNodes) {
					readHeadNode(xmlNode);
				}
				head.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		reader.addHandler("/root/Unit", new ElementHandler() {
			@Override
			public
				void onEnd(ElementPath path) {
				Element unit = path.getCurrent();
				List<Element> xmlHeadNodes = unit.elements();
				for (Element xmlNode : xmlHeadNodes) {
					readUnitNode(xmlNode);
				}
				unit.detach();
			}

			@Override
			public
				void onStart(ElementPath path) {
			}

		});
		try {
			document = reader.read(configFile);
			if (lockFilePath == null
				|| (!new File(lockFilePath).exists())
				|| testDescId == null) {
				System.err.println("please setup correct lockFilePath");
				return false;
			}
		}
		catch (DocumentException ex) {
			Logger.getLogger(DataFormat.class.getName()).log(Level.SEVERE, null, ex);
			return false;
		}
		return true;
	}

	private
		void readHeadNode(Element element) {
		@SuppressWarnings("unused")
		List<Element> nodes = element.elements();
		String xmlNodeName = null;
		boolean nodeEnabled = false;
		boolean logEnabled = false;
		if (!nodes.isEmpty()) {
			xmlNodeName = element.attributeValue("name").trim();

			ArrayList<String> fieldNames = new ArrayList();

			boolean isTime = false;
			boolean isLowerCase = false;

			boolean isLotStartTime = false;
			boolean isLotOpenTime = false;
			boolean isLotEndTime = false;
			boolean enabledLog = true;
			int index = -1;
			boolean isLotNumber = false;
			boolean isOperation = false;

			for (Element node : nodes) {
				String nodeName = node.getName().trim().toLowerCase();
				String value = node.getText().trim();
				if (value.isEmpty() && nodeName.equals("SourceType")) {
					System.out.printf("Fatal Error: please setup value for this tag %s in xml node: %s\n", nodeName, xmlNodeName);
					System.exit(1);
				}
				switch (nodeName) {
					case "lotnumber":
						isLotNumber = value.equals("1");
						break;
					case "operation":
						isOperation = value.equals("1");
						break;	
					case "time":
						isTime = value.equals("1");
						break;
					case "lowercase":
						isLowerCase = value.equals("1");
						break;
					case "lotstarttime":
						isLotStartTime = value.equals("1");
						break;
					case "lotendtime":
						isLotEndTime = value.equals("1");
						break;
					case "lotopentime":
						isLotOpenTime = value.equals("1");
						break;
					case "field":
						if (!fieldNames.contains(value)) {
							fieldNames.add(value);
						}
						break;
					case "index":
						if (value.isEmpty()) {
							System.out.printf("Fatal Error: please set the index for this head xml node %s\n", xmlNodeName);
							System.exit(1);
						}
						index = Integer.valueOf(value);
						if (index < 0) {
							System.out.printf("Fatal Error: the index must be grate or equals 0 for this head xml node %s\n", xmlNodeName);
							System.exit(1);
						}
						break;
					case "sourcetype":
						//source type must be initialized before head content
						String sourceType = node.attributeValue("name").trim();
						DataFormat dataFormat = null;
						if (Config.dataFormats.containsKey(sourceType)) {
							dataFormat = Config.dataFormats.get(sourceType);
						}
						else {
							System.out.printf("Fatal Error: please setup this source type: %s first\n", sourceType);
							System.exit(1);
						}

						XmlNode xmlNode = new XmlNode(xmlNodeName);
						nodeEnabled = node.attributeValue("enabled").trim().equals("1");
						enabledLog = node.elementTextTrim("EnabledLog").equals("1");
						if (node.elements("AliasName").size() > 0) {
							String aliasName = node.elementTextTrim("AliasName").trim();
							xmlNode.setName(aliasName);
						}
						if (dataFormat.getLotHead().containsKey(xmlNodeName)) {
							System.out.printf("Fatal Error: duplicate head xml node found %s\n", xmlNodeName);
							System.exit(1);
						}
						dataFormat.getLotHead().put(xmlNodeName, xmlNode);
						xmlNode.setEnabledLog(enabledLog);
						xmlNode.setEnabled(nodeEnabled);

						break;

					default:
						System.out.printf("FATAL ERROR: unsupportted head xml node property found: %s in head xml node: %s\n", nodeName, xmlNodeName);
						System.exit(1);
				}
			}

			for (DataFormat dataFormat : Config.dataFormats.values()) {
				XmlNode xmlNode = dataFormat.getLotHead().get(xmlNodeName);
				if (xmlNode == null) {
					System.out.printf("Fatal Error: please setup the head xml node: %s property for source type: %s\n", xmlNodeName, dataFormat.getSourceType());
					System.exit(1);
				}
				if (xmlNode.isEnabled()) {
					if (fieldNames.isEmpty()) {
						System.out.printf("Fatal Error: please add field for this head xml node %s\n", xmlNodeName);
						System.exit(1);
					}
					xmlNode.setFieldNames(fieldNames);
					xmlNode.setTimeNode(isTime);
					xmlNode.setToLowerCase(isLowerCase);
					xmlNode.setLotOpenTime(isLotOpenTime);
					xmlNode.setLotStartTime(isLotStartTime);
					xmlNode.setLotEndTime(isLotEndTime);
					xmlNode.setIndex(index);
					if(isLotNumber) {
						dataFormat.setLotNumberNode(xmlNode);
					}
					if(isOperation) {
						dataFormat.setOperationNode(xmlNode);
					}
					
				}
				else {
					dataFormat.getLotHead().remove(xmlNodeName);
				}

			}
		}
	}

	private
		void readUnitNode(Element element) {
		@SuppressWarnings("unused")
		List<Element> nodes = element.elements();
		String xmlNodeName = null;

		if (!nodes.isEmpty()) {
			xmlNodeName = element.attributeValue("name").trim();
			ArrayList<String> fieldNames = new ArrayList();

			boolean isTime = false;
			boolean isLowerCase = false;
			boolean isEndTime = false;
			boolean isStartTime = false;
			boolean isTestTime = false;

			boolean nodeEnabled = false;
			boolean enabledLog = true;
			boolean isWaferNumber = false;
			boolean isUnitId = false;
			boolean isXCoord = false;
			boolean isYCoord = false;
			boolean isHardBin = false;
			boolean isSoftBin = false;
			boolean isWaferLot = false;
			int isCnt = 0;
			String subString = null;

			for (Element node : nodes) {
				String nodeName = node.getName().trim().toLowerCase();
				String value = node.getText().trim();
				if (value.isEmpty() && nodeName.equals("SourceType")) {
					System.out.printf("Fatal Error: please setup value for this tag %s in xml node: %s\n", nodeName, xmlNodeName);
					System.exit(1);
				}
				switch (nodeName) {
					case "time":
						isTime = value.equals("1");
						break;
					case "lowercase":
						isLowerCase = value.equals("1");
						break;
					case "isstarttime":
						isStartTime = value.equals("1");
						isCnt++;
						break;
					case "isendtime":
						isEndTime = value.equals("1");
						isCnt++;
						break;
					case "istesttime":
						isTestTime = value.equals("1");
						isCnt++;
						break;
					case "field":
						if (!fieldNames.contains(value)) {
							fieldNames.add(value);
						}
						break;
					case "iswafernumber":
						isWaferNumber = value.endsWith("1");
						isCnt++;
						break;
					case "isunitid":
						isUnitId = value.endsWith("1");
						isCnt++;
						break;
					case "isxcoord":
						isXCoord = value.endsWith("1");
						isCnt++;
						break;
					case "isycoord":
						isYCoord = value.endsWith("1");
						isCnt++;
						break;
					case "ishardbin":
						isHardBin = value.endsWith("1");
						isCnt++;
						break;
					case "issoftbin":
						isSoftBin = value.endsWith("1");
						isCnt++;
						break;
					case "substring":
						subString = value;
						break;
					case "iswaferlot":
						isWaferLot = value.endsWith("1");
						isCnt++;
						break;
					case "sourcetype":
						String sourceType = node.attributeValue("name").trim();
						DataFormat dataFormat = null;
						if (Config.dataFormats.containsKey(sourceType)) {
							dataFormat = Config.dataFormats.get(sourceType);
						}
						else {
							System.out.printf("Fatal Error: please setup this source type: %s first\n", sourceType);
							System.exit(1);
						}

						XmlNode xmlNode = new XmlNode(xmlNodeName);
						nodeEnabled = node.attributeValue("enabled").trim().equals("1");
						enabledLog = node.elementTextTrim("EnabledLog").equals("1");
						if (dataFormat.getUnit().getNodes().containsKey(xmlNodeName)) {
							System.out.printf("Fatal Error: duplicate unit xml node found %s\n", xmlNodeName);
							System.exit(1);
						}
						dataFormat.getUnit().getNodes().put(xmlNodeName, xmlNode);
						xmlNode.setEnabledLog(enabledLog);
						xmlNode.setEnabled(nodeEnabled);

						break;
					default:
						System.out.printf("FATAL ERROR: unsupportted unit xml node property found: %s in unit xml node: %s\n", nodeName, xmlNodeName);
						System.exit(1);
				}
			}
			if (isCnt > 1) {
				System.out.printf("Fatal Error, only one of those properties can be set in unit xml node: %s\n",
					"IsStartTime, IsEndTime, IsTestTime, IsUnitId, IsWaferNumber, IsXCoord, IsYCoord, IsHardBin, IsSoftBin, IsWaferLot");
				System.exit(1);
			}
			for (DataFormat dataFormat : Config.dataFormats.values()) {
				XmlNode xmlNode = dataFormat.getUnit().getNodes().get(xmlNodeName);
				if (xmlNode == null) {
					System.out.printf("Fatal Error: please setup the unit xml node: %s property for source type: %s\n", xmlNodeName, dataFormat.getSourceType());
					System.exit(1);
				}
				if (xmlNode.isEnabled()) {
					if (fieldNames.isEmpty()) {
						System.out.printf("Fatal Error: please add field for this unit xml node %s\n", xmlNodeName);
						System.exit(1);
					}
					xmlNode.setFieldNames(fieldNames);
					xmlNode.setTimeNode(isTime);
					xmlNode.setToLowerCase(isLowerCase);
					xmlNode.setStartTime(isStartTime);
					xmlNode.setEndTime(isEndTime);
					xmlNode.setUnitTestTimeNode(isTestTime);
					if (isCnt == 1) {
						if (isUnitId) {
							dataFormat.getUnit().setUnitIdNode(xmlNode);
							xmlNode.setUnitIdNode(true);
						}
						else if (isWaferNumber) {
							dataFormat.getUnit().setWaferNumberNode(xmlNode);
							setUpIndex(xmlNode, subString);
							xmlNode.setWaferNumberNode(true);
						}
						else if (isXCoord) {
							dataFormat.getUnit().setxCoordNode(xmlNode);
							setUpIndex(xmlNode, subString);
							xmlNode.setxNode(true);
						}
						else if (isYCoord) {
							dataFormat.getUnit().setyCoordNode(xmlNode);
							setUpIndex(xmlNode, subString);
							xmlNode.setyNode(true);
						}
						else if (isHardBin) {
							dataFormat.getUnit().setHardBinNode(xmlNode);
						}
						else if (isSoftBin) {
							dataFormat.getUnit().setSoftBinNode(xmlNode);
						}
						else if (isStartTime) {
							dataFormat.getUnit().setStartTimeNode(xmlNode);
						}
						else if (isEndTime) {
							dataFormat.getUnit().setEndTimeNode(xmlNode);
						}
						else if (isTestTime) {
							dataFormat.getUnit().setTestTimeNode(xmlNode);
						}
						else if (isWaferLot) {
							if (dataFormat.getDataType().equals(Config.DataTypes.WaferSort)) {
								System.out.printf("Fatal Error: never setup wafer lot in data type: %s\n", Config.DataTypes.WaferSort);
								System.exit(1);
							}
							else {
								dataFormat.getUnit().setWaferLotNode(xmlNode);
								setUpIndex(xmlNode, subString);
								xmlNode.setWaferLotNode(true);
							}
						}
						else {
							System.out.println("Fatal Error: something woring for the Is property");
							System.exit(1);
						}
					}
				}
				else {
					dataFormat.getUnit().getNodes().remove(xmlNodeName);
				}

			}

		}

	}

	private
		void setUpIndex(XmlNode xmlNode, String subString) {
		if (subString == null) {
			System.out.printf("Fatal Error: please setup the sub string index to get the %s from unit id\n", xmlNode.getName());
			System.exit(1);
		}
		xmlNode.setStartIndex(Integer.valueOf(subString.split(",")[0].trim()));
		xmlNode.setEndIndex(Integer.valueOf(subString.split(",")[1].trim()));
		if (xmlNode.getStartIndex() > xmlNode.getEndIndex()) {
			System.out.printf("Fatal Error: please setup the sub string index to get the %s from unit id\n"
				+ "    the start index must less than end index\n", xmlNode.getName());
			System.exit(1);
		}
	}
	
	public static DataFormat getFTFormat(){
		for(DataFormat format: Config.dataFormats.values()) {
			if(format.getDataType().equals(Config.DataTypes.ATE)) {
				return format;
			}
		}
		return null;
	}
	public static DataFormat getSLTFormat(){
		for(DataFormat format: Config.dataFormats.values()) {
			if(format.getDataType().equals(Config.DataTypes.SLT)) {
				return format;
			}
		}
		return null;
	}

	public static
		void main(String[] args) {
		new Config("./config/dataformat.xml");
		System.out.println("");

	}
}