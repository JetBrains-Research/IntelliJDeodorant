package org.jetbrains.research.intellijdeodorant.core.clone.parsers;

import com.intellij.openapi.project.Project;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class NiCadOutputParser extends CloneDetectorOutputParser {

    private Document document;

    public NiCadOutputParser(Project iJavaProject, String cloneOutputFilePath) throws InvalidInputFileException {
        super(iJavaProject, cloneOutputFilePath);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file = new File(this.getToolOutputFilePath());
            this.document = builder.parse(file);
            NodeList classInfoNodeList = document.getElementsByTagName("classinfo");
            try {
                this.setCloneGroupCount(Integer.parseInt(classInfoNodeList.item(0).getAttributes().getNamedItem("nclasses").getNodeValue()));
            } catch (Exception nfe) {
                this.document = null;
                throw new InvalidInputFileException();
            }
        } catch (IOException | SAXException | ParserConfigurationException ioex) {
            ioex.printStackTrace();
        }
    }

    @Override
    public CloneGroupList readInputFile() throws InvalidInputFileException {

        if (this.document == null)
            throw new InvalidInputFileException();

        CloneGroupList cloneGroups = new CloneGroupList(getIJavaProject());

        NodeList classNodeList = document.getElementsByTagName("class");
        for (int cloneClassIndex = 0; cloneClassIndex < classNodeList.getLength(); cloneClassIndex++) {
            if (this.isOperationCanceled())
                break;
            Node classNode = classNodeList.item(cloneClassIndex);
            try {
                int cloneGroupID = Integer.parseInt(classNode.getAttributes().getNamedItem("classid").getNodeValue());
                CloneGroup cloneGroup = new CloneGroup(cloneGroupID);
                NodeList cloneInstancesNodeList = classNode.getChildNodes();
                for (int cloneInstanceIndex = 0; cloneInstanceIndex < cloneInstancesNodeList.getLength(); cloneInstanceIndex++) {
                    Node cloneInstanceNode = cloneInstancesNodeList.item(cloneInstanceIndex);
                    if ("source".equals(cloneInstanceNode.getNodeName())) {
                        NamedNodeMap cloneInstanceAttributes = cloneInstanceNode.getAttributes();
                        String filePath = cloneInstanceAttributes.getNamedItem("file").getNodeValue();
                        int startLine = Integer.parseInt(cloneInstanceAttributes.getNamedItem("startline").getNodeValue());
                        int endLine = Integer.parseInt(cloneInstanceAttributes.getNamedItem("endline").getNodeValue());
                        CloneInstance cloneInstance = getCloneInstance(filePath, cloneInstanceIndex, true, startLine, endLine, 0);
                        cloneGroup.addClone(cloneInstance);
                    }
                }
                if (cloneGroup.getCloneGroupSize() > 1)
                    cloneGroups.add(cloneGroup);
            } catch (NullPointerException | StringIndexOutOfBoundsException | NumberFormatException | ResourceInfo.ICompilationUnitNotFoundException e) {
                addExceptionHappenedDuringParsing(e);
            }
            progress(cloneClassIndex);
        }

        if (cloneGroups.getCloneGroupsCount() == 0)
            throw new InvalidInputFileException();

        return cloneGroups;
    }

}
