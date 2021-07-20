package be.jorisg.ultrastarorganizer.commands;

import be.jorisg.ultrastarorganizer.entity.SongInfo;
import be.jorisg.ultrastarorganizer.utils.Utils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.odftoolkit.odfdom.doc.OdfTextDocument;
import org.odftoolkit.odfdom.doc.table.OdfTable;
import org.odftoolkit.odfdom.doc.table.OdfTableCell;
import org.odftoolkit.odfdom.doc.table.OdfTableRow;
import org.odftoolkit.odfdom.dom.OdfContentDom;
import org.odftoolkit.odfdom.dom.attribute.fo.FoTextAlignAttribute;
import org.odftoolkit.odfdom.dom.element.office.OfficeTextElement;
import org.odftoolkit.odfdom.dom.style.OdfStyleFamily;
import org.odftoolkit.odfdom.dom.style.props.*;
import org.odftoolkit.odfdom.incubator.doc.office.OdfOfficeStyles;
import org.odftoolkit.odfdom.incubator.doc.style.OdfStyle;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextHeading;
import org.odftoolkit.odfdom.incubator.doc.text.OdfTextParagraph;
import org.w3c.dom.Node;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "songlist",
        description = "Create a document with a list of all songs.")
public class SongList implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The ultrastar library.")
    private File directory;

    @Override
    public Integer call() throws Exception {

        List<SongInfo> songInfos = new ArrayList<>();
        for (File songDir : directory.listFiles()) {
            if (!songDir.isDirectory()) {
                continue;
            }

            try {
                SongInfo info = Utils.getMainInfoFile(songDir);
                songInfos.add(info);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // GENERATE ADMINISTRATIVE CSV
        File csvOutputFile = new File(FileSystems.getDefault().getPath(".").toFile(), "songlist.csv");
        if (!csvOutputFile.exists()) {
            csvOutputFile.createNewFile();
        }

        try (
                PrintWriter pw = new PrintWriter(csvOutputFile);
        ) {
            pw.write("SEP=,\n");

            CSVPrinter printer = new CSVPrinter(pw, CSVFormat.DEFAULT.withHeader(
                    "Artist", "Title", "Cover", "Background", "Video"));

            for (SongInfo info : songInfos) {
                printer.printRecord(info.getArtist(), info.getTitle(),
                        info.getCover() != null, info.getBackground() != null, info.getVideo() != null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Generated songlist csv for " + songInfos.size() + " songs!");

        // GENERATE STYLED DOCUMENT
        OdfTextDocument odt = OdfTextDocument.newTextDocument();

        OdfContentDom dom = odt.getContentDom();
        OfficeTextElement root = odt.getContentRoot();
        OdfOfficeStyles styles = odt.getOrCreateDocumentStyles();

        // cleanup
        Node childNode;
        childNode = root.getFirstChild();
        while (childNode != null) {
            root.removeChild(childNode);
            childNode = root.getFirstChild();
        }

        styles.getDefaultStyle(OdfStyleFamily.Paragraph).setProperty(OdfTextProperties.FontFamily, "Arial");
        styles.getDefaultStyle(OdfStyleFamily.Table).setProperty(OdfTextProperties.FontFamily, "Arial");

        // create heading style
        OdfStyle titleStyle = styles.newStyle("Title", OdfStyleFamily.Paragraph);
        titleStyle.setStyleDisplayNameAttribute("Title");
        titleStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        titleStyle.setProperty(OdfTextProperties.FontSize, "36pt");
        titleStyle.setProperty(OdfParagraphProperties.TextAlign, "center");

        // create title
        OdfTextHeading title = new OdfTextHeading(odt.getContentDom());
        title.addStyledContent("Title", "Ultrastar");
        root.appendChild(title);
        odt.newParagraph();

        // create table
        OdfTable table = OdfTable.newTable(odt, songInfos.size() + 1, 3);
        table.getOdfElement().setStyleName("Table");

        // create table header style
        OdfStyle tableHeaderStyle = styles.newStyle("Table_Header", OdfStyleFamily.TableCell);
        tableHeaderStyle.setStyleDisplayNameAttribute("Table Header");
        tableHeaderStyle.setProperty(OdfTableCellProperties.Border, "none");
        tableHeaderStyle.setProperty(OdfTextProperties.FontWeight, "bold");
        tableHeaderStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        tableHeaderStyle.setProperty(OdfParagraphProperties.TextAlign, "center");

        // table titles
        table.getCellByPosition(0, 0).getOdfElement().setStyleName("Table_Header");

        OdfTableCell col1 = table.getCellByPosition(1, 0);
        col1.setStringValue("ARTIST");
        col1.getOdfElement().setStyleName("Table_Header");

        OdfTableCell col2 = table.getCellByPosition(2, 0);
        col2.setStringValue("TITLE");
        col2.getOdfElement().setStyleName("Table_Header");

        // create table cell style
        OdfStyle tableCellStyle = styles.newStyle("Table_Cell", OdfStyleFamily.TableCell);
        tableCellStyle.setStyleDisplayNameAttribute("Table Cell");
        tableCellStyle.setProperty(OdfTableCellProperties.Border, "none");
        tableCellStyle.setProperty(OdfTextProperties.FontFamily, "Arial");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingTop, "3pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingBottom, "3pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingLeft, "10pt");
        tableCellStyle.setProperty(OdfTableCellProperties.PaddingRight, "10pt");

        // create odd table row style
        OdfStyle tableOddRowStyle = styles.newStyle("Table_Odd_Row", OdfStyleFamily.TableRow);
        tableOddRowStyle.setStyleDisplayNameAttribute("Table Odd Row");
        tableOddRowStyle.setProperty(OdfTableRowProperties.BackgroundColor, "#f6f6f6");

        // table contents
        for (int i = 0; i < songInfos.size(); i++) {
            SongInfo si = songInfos.get(i);
            table.getCellByPosition(0, i + 1).setStringValue(i + "");
            table.getCellByPosition(1, i + 1).setStringValue(si.getArtist());
            table.getCellByPosition(2, i + 1).setStringValue(si.getTitle());

            for ( int j = 0; j < table.getColumnCount(); j++ ) {
                table.getCellByPosition(j, i + 1).getOdfElement().setStyleName("Table_Cell");
            }

            if ( i % 2 == 1 ) {
                table.getRowByIndex(i + 1).getOdfElement().setStyleName("Table_Odd_Row");
            }
        }

        // save document to file
        File docOutputFile = new File(FileSystems.getDefault().getPath(".").toFile(), "songlist.odt");
        odt.save(docOutputFile);

        System.out.println("Generated songlist document for " + songInfos.size() + " songs!");

        return 0;
    }
}
