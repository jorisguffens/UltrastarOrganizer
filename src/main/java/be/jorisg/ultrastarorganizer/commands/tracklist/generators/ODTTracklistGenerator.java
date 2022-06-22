package be.jorisg.ultrastarorganizer.commands.tracklist.generators;

import be.jorisg.ultrastarorganizer.domain.TrackInfo;
import org.apache.xerces.dom.ParentNode;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.dom.OdfDocumentNamespace;
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
import org.odftoolkit.odfdom.dom.element.style.StyleMasterPageElement;
import org.odftoolkit.odfdom.dom.element.style.StyleTablePropertiesElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableCellElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableColumnElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableElement;
import org.odftoolkit.odfdom.dom.element.table.TableTableRowElement;
import org.odftoolkit.odfdom.dom.element.text.TextPElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.*;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStylePageLayout;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextHeading;
import org.odftoolkit.odfdom.pkg.OdfName;
import org.odftoolkit.odfdom.pkg.OdfXMLFactory;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ODTTracklistGenerator implements TracklistGenerator {

    @Override
    public void generate(File output, List<TrackInfo> tracks) throws Exception {
        // GENERATE STYLED DOCUMENT
        OdfTextDocument odt = OdfTextDocument.newTextDocument();

        OfficeTextElement root = odt.getContentRoot();
        clear(root);

        OdfContentDom dom = odt.getContentDom();
        OdfOfficeStyles styles = odt.getOrCreateDocumentStyles();

        // Page layout
        StyleMasterPageElement defaultPage = odt.getOfficeMasterStyles().getMasterPage("Standard");
        String pageLayoutName = defaultPage.getStylePageLayoutNameAttribute();
        OdfStylePageLayout pageLayoutStyle = defaultPage.getAutomaticStyles().getPageLayout(pageLayoutName);
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.Padding, "0");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginTop, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginRight, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginBottom, "1.3cm");
        pageLayoutStyle.setProperty(OdfPageLayoutProperties.MarginLeft, "1.3cm");


        styles.getDefaultStyle(OdfStyleFamily.Paragraph).setProperty(OdfTextProperties.FontFamily, "Arial");
        styles.getDefaultStyle(OdfStyleFamily.Table).setProperty(OdfTextProperties.FontFamily, "Arial");

        // create heading style
        OdfStyle titleStyle = styles.newStyle("Title", OdfStyleFamily.Paragraph);
        titleStyle.setStyleDisplayNameAttribute("Title");
        titleStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        titleStyle.setProperty(OdfTextProperties.FontSize, "28pt");
        titleStyle.setProperty(OdfParagraphProperties.TextAlign, "center");

        // create title
        OdfTextHeading title = new OdfTextHeading(dom);
        title.addStyledContent("Title", "Ultrastar");
        root.appendChild(title);

        odt.newParagraph();
        odt.newParagraph();

        // create table style
        OdfStyle tableStyle = styles.newStyle("Table", OdfStyleFamily.Table);
        tableStyle.setStyleDisplayNameAttribute("Table");
        tableStyle.setProperty(OdfTableProperties.Align, "center");
        tableStyle.setProperty(StyleTablePropertiesElement.Width, "6in");
        tableStyle.setProperty(StyleTablePropertiesElement.Align, "margins");

        // create table cell style
        OdfStyle tableCellStyle = styles.newStyle("Table_Cell", OdfStyleFamily.TableCell);
        tableCellStyle.setStyleDisplayNameAttribute("Table Cell");
        tableCellStyle.setProperty(OdfTableCellProperties.Border, "none");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingTop, "2pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingBottom, "2pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingLeft, "10pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingRight, "10pt");
        tableCellStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        tableCellStyle.setProperty(OdfTextProperties.FontSize, "10pt");

        // create odd table row style
        OdfStyle tableOddRowStyle = styles.newStyle("Table_Odd_Row", OdfStyleFamily.TableRow);
        tableOddRowStyle.setStyleDisplayNameAttribute("Table Odd Row");
        tableOddRowStyle.setProperty(OdfTableRowProperties.BackgroundColor, "#f6f6f6");

        // create table
        TableTableElement table = (TableTableElement) OdfXMLFactory.newOdfElement(dom,
                OdfName.newName(OdfDocumentNamespace.TABLE, "table"));
        table.setTableNameAttribute("Table1");

        // create columns
        TableTableColumnElement columns = (TableTableColumnElement) OdfXMLFactory.newOdfElement(dom,
                OdfName.newName(OdfDocumentNamespace.TABLE, "table-column"));
        columns.setTableNumberColumnsRepeatedAttribute(3);
        table.appendChild(columns);

        // create rows & cells
        for (int i = 0; i < tracks.size(); i++) {
            TrackInfo si = tracks.get(i);

            TableTableRowElement row = (TableTableRowElement) OdfXMLFactory.newOdfElement(dom,
                    OdfName.newName(OdfDocumentNamespace.TABLE, "table-row"));

            if ( i % 2 == 1 ) {
                row.setStyleName("Table_Odd_Row");
            }

            List<TextPElement> cells = new ArrayList<>();
            for (int j = 0; j < 3; j++) {
                TableTableCellElement cell = (TableTableCellElement) OdfXMLFactory.newOdfElement(dom,
                        OdfName.newName(OdfDocumentNamespace.TABLE, "table-cell"));
                cell.setStyleName("Table_Cell");

                TextPElement p = (TextPElement) OdfXMLFactory.newOdfElement(dom,
                        OdfName.newName(OdfDocumentNamespace.TEXT, "p"));
                cells.add(p);

                cell.appendChild(p);
                row.appendChild(cell);
            }

            cells.get(0).setTextContent((i + 1) + "");
            cells.get(1).setTextContent(si.artist());
            cells.get(2).setTextContent(si.title());

            table.appendChild(row);
        }
        root.appendChild(table);

        // save odt file
        odt.save(output);
    }

    private void clear(ParentNode parent) {
        Node childNode;
        while ((childNode = parent.getFirstChild()) != null) {
            parent.removeChild(childNode);
        }
    }

}
